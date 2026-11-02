package com.hoperefund.income.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hoperefund.common.exception.BadRequestException;
import com.hoperefund.common.exception.NotFoundException;
import com.hoperefund.income.dto.IncomeRequest;
import com.hoperefund.income.dto.IncomeResponse;
import com.hoperefund.income.mapper.IncomeMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** DB 없이 서비스 규칙만 확인해요 (가짜 매퍼 사용). */
class IncomeServiceTest {

    /** 메모리에만 저장하는 가짜 IncomeMapper */
    static class FakeIncomeMapper implements IncomeMapper {
        final Map<String, IncomeResponse> rows = new HashMap<>();
        int upsertCount = 0;

        @Override
        public IncomeResponse findByMemberAndYear(long memberId, int taxYear) {
            return rows.get(memberId + "-" + taxYear);
        }

        @Override
        public int upsert(long memberId, int taxYear, IncomeRequest req) {
            upsertCount++;
            IncomeResponse r = new IncomeResponse();
            r.setTaxYear(taxYear);
            r.setTotalSalary(req.getTotalSalary());
            r.setPensionSaving(req.getPensionSaving());
            r.setIrpSaving(req.getIrpSaving());
            rows.put(memberId + "-" + taxYear, r);
            return 1;
        }
    }

    private FakeIncomeMapper mapper;
    private IncomeService service;

    @BeforeEach
    void setUp() {
        mapper = new FakeIncomeMapper();
        service = new IncomeService(mapper);
    }

    private IncomeRequest request(int dependents, int children) {
        IncomeRequest req = new IncomeRequest();
        req.setTotalSalary(36_000_000L);
        req.setWithheldTax(1_523_000L);
        req.setDependents(dependents);
        req.setChildren(children);
        req.setPensionSaving(6_000_000L);
        req.setIrpSaving(3_000_000L);
        return req;
    }

    @Test
    void 저장하면_연금저축과_IRP가_따로_저장된다() {
        // Given
        IncomeRequest req = request(1, 0);
        // When
        IncomeResponse saved = service.save(1L, 2026, req);
        // Then
        assertEquals(3_000_000L, saved.getIrpSaving().longValue());
    }

    @Test
    void 자녀_수가_부양가족_수보다_많으면_저장하지_않는다() {
        // Given
        IncomeRequest req = request(1, 2);
        // When
        assertThrows(BadRequestException.class, () -> service.save(1L, 2026, req));
        // Then
        assertEquals(0, mapper.upsertCount);
    }

    @Test
    void 기준값이_없는_연도는_저장하지_않는다() {
        // Given
        IncomeRequest req = request(0, 0);
        // When · Then
        assertThrows(BadRequestException.class, () -> service.save(1L, 2024, req));
    }

    @Test
    void 저장한_적_없는_연도를_조회하면_없음_오류() {
        // When · Then
        assertThrows(NotFoundException.class, () -> service.get(1L, 2026));
    }
}
