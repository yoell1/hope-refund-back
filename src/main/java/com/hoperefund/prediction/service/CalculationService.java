package com.hoperefund.prediction.service;

import com.hoperefund.common.exception.NotFoundException;
import com.hoperefund.income.dto.IncomeResponseDTO;
import com.hoperefund.income.service.IncomeService;
import com.hoperefund.prediction.client.CalcClient;
import com.hoperefund.prediction.dto.CalculationResponseDTO;
import com.hoperefund.prediction.dto.DeductionDetailDTO;
import com.hoperefund.prediction.mapper.PredictionMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계산 요청 (F-03-01)
 * 흐름: Controller → CalculationService → (IncomeService로 소득 조회) → CalcClient(계산) → 결과 저장 → 결과 반환
 *
 * 저장은 설정 hope.prediction.save-enabled가 true일 때만 해요.
 * 파이썬 서버 연결 전에는 false → 가짜 결과(세액 0)가 공용 DB에 쌓이지 않게 해요.
 */
@Service
public class CalculationService{

    private final IncomeService incomeService;
    private final CalcClient calcClient;
    private final PredictionMapper predictionMapper;
    private final boolean saveEnabled;

    // 생성자 주입: @Value는 application.yml의 설정 값을 읽어 넣어 줘요 (없으면 false)
    public CalculationService(IncomeService incomeService, CalcClient calcClient, PredictionMapper predictionMapper,
                              @Value("${hope.prediction.save-enabled:false}") boolean saveEnabled){
        this.incomeService = incomeService;
        this.calcClient = calcClient;
        this.predictionMapper = predictionMapper;
        this.saveEnabled = saveEnabled;
    }

    /**
     * @Transactional: 이 메서드 안의 INSERT가 하나라도 실패하면 전부 취소(rollback)돼요.
     * → PREDICTION만 저장되고 상세가 빠지는 "반쪽 결과"가 남지 않아요.
     */
    @Transactional
    public CalculationResponseDTO calculate(long memberId, int taxYear){
        // 1. 본인(memberId) 소득만 읽어요 · 연도 검사 · 소득 없음 오류는 IncomeService 규칙 그대로
        IncomeResponseDTO income = incomeService.get(memberId, taxYear);

        // 2. 계산은 CalcClient에 맡겨요
        CalculationResponseDTO result = calcClient.calculate(taxYear, income);

        // 3. 저장 (설정이 켜져 있을 때만)
        if(saveEnabled){
            long predictionId = save(memberId, taxYear, income, result);
            result.setPredictionId(predictionId);
        }
        return result;
    }

    private long save(long memberId, int taxYear, IncomeResponseDTO income, CalculationResponseDTO result){
        // 계산 결과의 연도가 요청 연도와 다르면 저장하지 않아요 (기준값 연도 = 계산 연도 규칙)
        if(result.getTaxYear() == null || result.getTaxYear() != taxYear){
            throw new IllegalStateException("계산 결과 연도가 요청 연도와 달라요: " + result.getTaxYear() + " / " + taxYear);
        }

        // 무주택 세대주 여부는 회원 정보(MEMBER)에서 읽어 입력값 기록에 복사해요
        String homelessHeadYn = predictionMapper.findHomelessHeadYn(memberId);
        if(homelessHeadYn == null){
            throw new NotFoundException("회원 정보를 찾을 수 없어요");
        }

        long predictionId = predictionMapper.nextPredictionId();
        predictionMapper.insertPrediction(predictionId, memberId, result);
        predictionMapper.insertPredictionInput(predictionId, income, homelessHeadYn);

        for(DeductionDetailDTO detail : result.getDetails()){
            long typeId = findTypeId(taxYear, detail.getTypeCode(), income.getTotalSalary());
            predictionMapper.insertPredictionDetail(predictionId, typeId,
                    detail.getBaseAmount(), detail.getDeductionAmount());
        }
        return predictionId;
    }

    /** 연도 + 코드 + 총급여 구간으로 정확히 1개를 찾아요. 0개나 여러 개면 기준값이 잘못된 거라 저장을 멈춰요. */
    private long findTypeId(int taxYear, String typeCode, long totalSalary){
        List<Long> typeIds = predictionMapper.findTypeIds(taxYear, typeCode, totalSalary);
        if(typeIds.size() != 1){
            throw new IllegalStateException("기준값 항목을 하나로 찾지 못했어요: " + taxYear + " " + typeCode
                    + " (찾은 개수 " + typeIds.size() + ")");
        }
        return typeIds.get(0);
    }
}
