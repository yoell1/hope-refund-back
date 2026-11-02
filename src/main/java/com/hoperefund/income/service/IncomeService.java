package com.hoperefund.income.service;

import com.hoperefund.common.exception.BadRequestException;
import com.hoperefund.common.exception.NotFoundException;
import com.hoperefund.income.dto.IncomeRequest;
import com.hoperefund.income.dto.IncomeResponse;
import com.hoperefund.income.mapper.IncomeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncomeService {

    /** 기준값이 있는 귀속 연도 (DB 기준값 2025 · 2026). 기준값이 늘면 여기도 늘려요. */
    static final int MIN_TAX_YEAR = 2025;
    static final int MAX_TAX_YEAR = 2026;

    private final IncomeMapper incomeMapper;

    public IncomeService(IncomeMapper incomeMapper) {
        this.incomeMapper = incomeMapper;
    }

    @Transactional(readOnly = true)
    public IncomeResponse get(long memberId, int taxYear) {
        checkTaxYear(taxYear);
        IncomeResponse income = incomeMapper.findByMemberAndYear(memberId, taxYear);
        if (income == null) {
            throw new NotFoundException(taxYear + "년 소득 정보가 아직 없어요");
        }
        return income;
    }

    @Transactional
    public IncomeResponse save(long memberId, int taxYear, IncomeRequest req) {
        checkTaxYear(taxYear);
        if (req.getChildren() > req.getDependents()) {
            throw new BadRequestException("20세 이하 자녀 수는 부양가족 수보다 많을 수 없어요");
        }
        incomeMapper.upsert(memberId, taxYear, req);
        return incomeMapper.findByMemberAndYear(memberId, taxYear);
    }

    private void checkTaxYear(int taxYear) {
        if (taxYear < MIN_TAX_YEAR || taxYear > MAX_TAX_YEAR) {
            throw new BadRequestException("해당 연도 기준값이 없어요");
        }
    }
}
