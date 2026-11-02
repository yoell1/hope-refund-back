package com.hoperefund.prediction.client;

import com.hoperefund.prediction.dto.CalculationResponseDTO;
import com.hoperefund.prediction.dto.DeductionDetailDTO;
import com.hoperefund.income.dto.IncomeResponseDTO;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 가짜 계산 결과 (파이썬 서버 연결 전까지 화면 연결용)
 * - 세액을 실제로 계산하지 않아요. 과세표준·산출세액·결정세액·공제액은 모두 0이에요.
 * - 대상 금액(baseAmount)에는 입력한 소득 값만 그대로 넣어요.
 * - 항목 코드·이름·종류·근거는 DB DEDUCTION_TYPE(2026) 기준이에요.
 *
 * TODO(파이썬 서버 연결 후): HTTP로 부르는 CalcClient 구현을 만들고 이 클래스는 지우기
 */
@Component // 스프링 Bean으로 등록 → CalculationService 생성자에 자동으로 들어가요
public class MockCalcClient implements CalcClient{

    private static final String CARD_SOURCE = "국세청 신고안내 132p";

    @Override
    public CalculationResponseDTO calculate(int taxYear, IncomeResponseDTO income){
        CalculationResponseDTO result = new CalculationResponseDTO();
        result.setTaxYear(taxYear);
        result.setTotalSalary(income.getTotalSalary());
        result.setWithheldTax(income.getWithheldTax());
        result.setTaxableIncome(0L);
        result.setCalculatedTax(0L);
        result.setDecidedTax(0L);
        // 환급액 = 기납부세액 - 결정세액 (가짜라 결정세액이 0 → 기납부세액 전부가 환급으로 보여요)
        result.setExpectedRefund(income.getWithheldTax() - result.getDecidedTax());

        // 연금계좌 대상 금액 = 연금저축 + IRP (DB는 두 칸 · v4.1)
        long pensionTotal = income.getPensionSaving() + income.getIrpSaving();

        result.setDetails(List.of(
            new DeductionDetailDTO("EARNED_DEDUCTION", "근로소득공제", "INCOME_DEDUCTION",
                income.getTotalSalary(), 0L, "국세청 신고안내 94p"),
            new DeductionDetailDTO("NATIONAL_PENSION", "국민연금보험료 공제", "INCOME_DEDUCTION",
                income.getNationalPension(), 0L, "국세청 신고안내 103p"),
            new DeductionDetailDTO("HEALTH_INSURANCE", "건강·고용보험료 공제", "INCOME_DEDUCTION",
                income.getHealthInsurance(), 0L, "국세청 신고안내 107p"),
            // 카드 공제 6개 행 (카드 내역 연결 전이라 대상 금액 0)
            new DeductionDetailDTO("CARD_CREDIT", "신용카드(일반)", "CARD", 0L, 0L, CARD_SOURCE),
            new DeductionDetailDTO("CARD_CHECK", "체크카드(일반)", "CARD", 0L, 0L, CARD_SOURCE),
            new DeductionDetailDTO("CARD_CASH", "현금영수증(일반)", "CARD", 0L, 0L, CARD_SOURCE),
            new DeductionDetailDTO("CARD_TRANSIT", "대중교통", "CARD", 0L, 0L, CARD_SOURCE),
            new DeductionDetailDTO("CARD_MARKET", "전통시장", "CARD", 0L, 0L, CARD_SOURCE),
            new DeductionDetailDTO("CARD_CULTURE", "문화체육", "CARD", 0L, 0L, CARD_SOURCE),
            new DeductionDetailDTO("EARNED_CREDIT", "근로소득세액공제", "TAX_CREDIT",
                0L, 0L, "국세청 신고안내 162p"),
            new DeductionDetailDTO("PENSION", "연금계좌", "TAX_CREDIT",
                pensionTotal, 0L, "국세청 신고안내 요약표"),
            new DeductionDetailDTO("STANDARD_CREDIT", "표준세액공제(근로자)", "TAX_CREDIT",
                0L, 0L, "국세청 신고안내 170p")
        ));
        return result;
    }
}
