package com.hoperefund.prediction.service;

import com.hoperefund.common.exception.NotFoundException;
import com.hoperefund.income.dto.IncomeResponseDTO;
import com.hoperefund.income.service.IncomeService;
import com.hoperefund.prediction.client.CalcClient;
import com.hoperefund.prediction.dto.CalculationResponseDTO;
import com.hoperefund.prediction.dto.CardSpendDTO;
import com.hoperefund.prediction.mapper.PredictionMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 계산 요청 (F-03-01)
 * 흐름: Controller → CalculationService → (소득 · 무주택 · 카드 합계 조회) → CalcClient(계산) → PredictionSaveService(저장) → 결과 반환
 *
 * 계산 서버 호출은 트랜잭션 밖에서 해요. 저장할 때만 PredictionSaveService가 트랜잭션을 열어요.
 * 저장은 hope.prediction.save-enabled가 true이고 hope.calc.mode가 http일 때만 해요.
 * mock(가짜 계산)일 때는 저장을 켜 두어도 저장하지 않아요 → 가짜 결과(세액 0)가 공용 DB에 쌓이지 않게.
 */
@Service
public class CalculationService{

    private static final Logger log = LoggerFactory.getLogger(CalculationService.class);

    private final IncomeService incomeService;
    private final CalcClient calcClient;
    private final PredictionSaveService predictionSaveService;
    private final PredictionMapper predictionMapper;
    private final boolean saveEnabled;

    // 생성자 주입: @Value는 application.yml의 설정 값을 읽어 넣어 줘요
    public CalculationService(IncomeService incomeService, CalcClient calcClient,
                              PredictionSaveService predictionSaveService, PredictionMapper predictionMapper,
                              @Value("${hope.prediction.save-enabled:false}") boolean saveEnabled,
                              @Value("${hope.calc.mode:mock}") String calcMode){
        this.incomeService = incomeService;
        this.calcClient = calcClient;
        this.predictionSaveService = predictionSaveService;
        this.predictionMapper = predictionMapper;
        boolean realCalc = "http".equals(calcMode);
        if(saveEnabled && !realCalc){
            log.warn("CALC_MODE가 http가 아니라서(현재 {}) SAVE_ENABLED=true여도 계산 결과를 저장하지 않아요", calcMode);
        }
        this.saveEnabled = saveEnabled && realCalc;
    }

    public CalculationResponseDTO calculate(long memberId, int taxYear){
        // 1. 본인(memberId) 데이터만 읽어요 · 연도 검사 · 소득 없음 오류는 IncomeService 규칙 그대로
        IncomeResponseDTO income = incomeService.get(memberId, taxYear);
        String homelessHeadYn = predictionMapper.findHomelessHeadYn(memberId);
        if(homelessHeadYn == null){
            throw new NotFoundException("회원 정보를 찾을 수 없어요");
        }
        List<CardSpendDTO> cardSpends = predictionMapper.findCardSpends(memberId, taxYear);

        // 2. 계산은 CalcClient에 맡겨요 (mock 또는 파이썬 서버) · 트랜잭션 밖
        CalculationResponseDTO result = calcClient.calculate(taxYear, income, homelessHeadYn, cardSpends);

        // 3. 저장 (설정이 켜져 있고 실제 계산일 때만) · 여기서만 트랜잭션
        if(saveEnabled){
            long predictionId = predictionSaveService.save(memberId, taxYear, income, homelessHeadYn, result);
            result.setPredictionId(predictionId);
        }
        return result;
    }
}
