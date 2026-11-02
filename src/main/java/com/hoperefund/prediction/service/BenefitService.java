package com.hoperefund.prediction.service;

import com.hoperefund.common.exception.NotFoundException;
import com.hoperefund.income.dto.IncomeResponseDTO;
import com.hoperefund.income.service.IncomeService;
import com.hoperefund.prediction.client.CalcClient;
import com.hoperefund.prediction.dto.BenefitResponseDTO;
import com.hoperefund.prediction.dto.MemberQualificationDTO;
import com.hoperefund.prediction.mapper.PredictionMapper;
import org.springframework.stereotype.Service;

/**
 * 청년 혜택 자격 진단 (F-03-02 · 분담표 F-12 · S-05)
 * 흐름: 본인 소득(총급여) + 본인 회원 자격 정보 → 계산 서버 /benefit → 혜택별 해당/비해당 + 이유 + 근거
 * 판정 조건은 DB BENEFIT_CONDITION에 있고, 판정은 계산 서버가 해요.
 */
@Service
public class BenefitService{

    private final IncomeService incomeService;
    private final CalcClient calcClient;
    private final PredictionMapper predictionMapper;

    public BenefitService(IncomeService incomeService, CalcClient calcClient, PredictionMapper predictionMapper){
        this.incomeService = incomeService;
        this.calcClient = calcClient;
        this.predictionMapper = predictionMapper;
    }

    public BenefitResponseDTO diagnose(long memberId, int taxYear){
        IncomeResponseDTO income = incomeService.get(memberId, taxYear);  // 연도 검사 · 소득 없음 오류 포함
        MemberQualificationDTO member = predictionMapper.findMemberQualification(memberId);
        if(member == null){
            throw new NotFoundException("회원 정보를 찾을 수 없어요");
        }
        return calcClient.diagnose(taxYear, income.getTotalSalary(), member);
    }
}
