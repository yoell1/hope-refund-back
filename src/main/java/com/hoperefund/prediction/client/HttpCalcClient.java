package com.hoperefund.prediction.client;

import com.hoperefund.common.exception.CalcUnavailableException;
import com.hoperefund.common.exception.NoCriteriaException;
import com.hoperefund.income.dto.IncomeResponseDTO;
import com.hoperefund.prediction.dto.BenefitResponseDTO;
import com.hoperefund.prediction.dto.CalculationResponseDTO;
import com.hoperefund.prediction.dto.CardSpendDTO;
import com.hoperefund.prediction.dto.MemberQualificationDTO;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 파이썬 계산 서버를 HTTP로 불러요 (hope.calc.mode=http 일 때만 Bean 등록)
 * - POST {CALC_URL}/calculate · /benefit (JSON 이름은 camelCase · 「동석님 전달」과 같음)
 * - 헤더 X-Calc-Token: 계산 서버의 CALC_API_TOKEN과 같은 값 (.env)
 * - 계산 서버가 NO_CRITERIA를 주면 그대로 NO_CRITERIA, 연결이 안 되면 503 안내
 */
@Component
@ConditionalOnProperty(name = "hope.calc.mode", havingValue = "http")
public class HttpCalcClient implements CalcClient{

    private final RestClient restClient;

    public HttpCalcClient(@Value("${hope.calc.url}") String baseUrl,
                          @Value("${hope.calc.token:}") String token){
        // 연결 3초 · 응답 10초 제한: 계산 서버 주소가 틀리거나 멈춰도 요청이 무한정 기다리지 않게
        // HTTP/1.1 고정: 계산 서버(uvicorn)가 HTTP/2 전환 요청을 거절해 남기던 경고(Unsupported upgrade request) 없애기
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(10));
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl).requestFactory(factory);
        if(!token.isBlank()){
            builder.defaultHeader("X-Calc-Token", token);
        }
        this.restClient = builder.build();
    }

    @Override
    public CalculationResponseDTO calculate(int taxYear, IncomeResponseDTO income, String homelessHeadYn,
                                            List<CardSpendDTO> cardSpends){
        // 계산 서버가 받는 칸만 골라 보내요 (그 서버는 모르는 칸이 오면 400으로 막아요)
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("taxYear", taxYear);
        body.put("totalSalary", income.getTotalSalary());
        body.put("withheldTax", income.getWithheldTax());
        body.put("nationalPension", income.getNationalPension());
        body.put("healthInsurance", income.getHealthInsurance());
        body.put("dependents", income.getDependents());
        body.put("children", income.getChildren());
        body.put("monthlyRent", income.getMonthlyRent());
        body.put("housingSaving", income.getHousingSaving());
        body.put("pensionSaving", income.getPensionSaving());
        body.put("irpSaving", income.getIrpSaving());
        body.put("smeReductionYn", income.getSmeReductionYn());
        body.put("homelessHeadYn", homelessHeadYn);
        body.put("cardSpends", cardSpends);
        return post("/calculate", body, CalculationResponseDTO.class);
    }

    @Override
    public BenefitResponseDTO diagnose(int taxYear, long totalSalary, MemberQualificationDTO member){
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("taxYear", taxYear);
        body.put("totalSalary", totalSalary);
        body.put("birthDate", member.getBirthDate() == null ? null : member.getBirthDate().toString());
        body.put("militaryMonths", member.getMilitaryMonths() == null ? 0 : member.getMilitaryMonths());
        body.put("hireDate", member.getHireDate() == null ? null : member.getHireDate().toString());
        body.put("companySize", member.getCompanySize());
        body.put("homelessHeadYn", member.getHomelessHeadYn());
        return post("/benefit", body, BenefitResponseDTO.class);
    }

    private <T> T post(String path, Map<String, Object> body, Class<T> type){
        try{
            return restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(type);
        }catch(HttpClientErrorException e){
            if(e.getResponseBodyAsString().contains("\"NO_CRITERIA\"")){
                throw new NoCriteriaException();
            }
            throw new CalcUnavailableException("계산 서버 요청 오류: " + e.getStatusCode(), e);
        }catch(RestClientException e){
            throw new CalcUnavailableException("계산 서버 연결 실패", e);
        }
    }
}
