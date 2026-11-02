package com.hoperefund.prediction.service;

import com.hoperefund.prediction.client.CalcClient;
import com.hoperefund.prediction.dto.CalculationResponseDTO;
import com.hoperefund.income.dto.IncomeResponseDTO;
import com.hoperefund.income.service.IncomeService;
import org.springframework.stereotype.Service;

/**
 * 계산 요청 (F-03-01)
 * 흐름: Controller → CalculationService → (IncomeService로 소득 조회) → CalcClient(계산) → 결과 반환
 * 결과를 DB(PREDICTION)에 저장하는 건 다음 단계(3단계)예요.
 */
@Service
public class CalculationService{

    private final IncomeService incomeService;
    private final CalcClient calcClient;

    // 생성자 주입: 스프링이 IncomeService와 CalcClient Bean을 찾아 넣어 줘요
    public CalculationService(IncomeService incomeService, CalcClient calcClient){
        this.incomeService = incomeService;
        this.calcClient = calcClient;
    }

    public CalculationResponseDTO calculate(long memberId, int taxYear){
        // 1. 본인(memberId) 소득만 읽어요 (SQL에 MEMBER_ID 조건 → 다른 회원 소득은 못 읽음)
        //    연도 검사 · 소득이 없을 때 오류도 IncomeService 규칙을 그대로 따라요
        IncomeResponseDTO income = incomeService.get(memberId, taxYear);

        // 2. 계산은 CalcClient에 맡겨요 (지금은 가짜 결과)
        return calcClient.calculate(taxYear, income);
    }
}
