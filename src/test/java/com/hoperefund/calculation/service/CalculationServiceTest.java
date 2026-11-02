package com.hoperefund.calculation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hoperefund.calculation.client.MockCalcClient;
import com.hoperefund.calculation.dto.CalculationResultDTO;
import com.hoperefund.common.exception.BadRequestException;
import com.hoperefund.common.exception.NotFoundException;
import com.hoperefund.income.dto.IncomeRequest;
import com.hoperefund.income.dto.IncomeResponse;
import com.hoperefund.income.mapper.IncomeMapper;
import com.hoperefund.income.service.IncomeService;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** DB 없이 계산 요청 흐름만 확인해요 (가짜 매퍼 + 가짜 계산). */
class CalculationServiceTest{

    /** 메모리에만 저장하는 가짜 IncomeMapper (회원번호-연도로 찾아요) */
    static class FakeIncomeMapper implements IncomeMapper{
        final Map<String, IncomeResponse> rows = new HashMap<>();

        @Override
        public IncomeResponse findByMemberAndYear(long memberId, int taxYear){
            return rows.get(memberId + "-" + taxYear);
        }

        @Override
        public int upsert(long memberId, int taxYear, IncomeRequest req){
            return 0;
        }
    }

    private FakeIncomeMapper mapper;
    private CalculationService service;

    @BeforeEach
    void setUp(){
        mapper = new FakeIncomeMapper();
        service = new CalculationService(new IncomeService(mapper), new MockCalcClient());
    }

    private void saveIncome(long memberId, int taxYear){
        IncomeResponse income = new IncomeResponse();
        income.setTaxYear(taxYear);
        income.setTotalSalary(36_000_000L);
        income.setWithheldTax(1_523_000L);
        income.setNationalPension(1_620_000L);
        income.setHealthInsurance(1_276_000L);
        income.setPensionSaving(6_000_000L);
        income.setIrpSaving(3_000_000L);
        mapper.rows.put(memberId + "-" + taxYear, income);
    }

    @Test
    void 소득이_있으면_기납부세액이_결과에_들어간다(){
        // Given
        saveIncome(1L, 2026);
        // When
        CalculationResultDTO result = service.calculate(1L, 2026);
        // Then
        assertEquals(1_523_000L, result.getWithheldTax().longValue());
    }

    @Test
    void 카드_공제는_6개_행으로_나온다(){
        // Given
        saveIncome(1L, 2026);
        // When
        CalculationResultDTO result = service.calculate(1L, 2026);
        // Then
        assertEquals(6, result.getDetails().stream().filter(d -> "CARD".equals(d.getKind())).count());
    }

    @Test
    void 다른_회원의_소득으로는_계산하지_않는다(){
        // Given: 1번 회원 소득만 있음
        saveIncome(1L, 2026);
        // When · Then: 2번 회원이 요청하면 1번 소득을 읽지 않고 없음 오류
        assertThrows(NotFoundException.class, () -> service.calculate(2L, 2026));
    }

    @Test
    void 기준값이_없는_연도는_계산하지_않는다(){
        // When · Then
        assertThrows(BadRequestException.class, () -> service.calculate(1L, 2024));
    }
}
