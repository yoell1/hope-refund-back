package com.hoperefund.prediction.client;

import com.hoperefund.prediction.dto.CalculationResponseDTO;
import com.hoperefund.income.dto.IncomeResponseDTO;

/**
 * 파이썬 서버(세액 계산)에 계산을 맡기는 창구 (인터페이스)
 *
 * 왜 인터페이스로 두나요?
 * - Service는 "계산해 줘"라고만 부르고, 실제로 누가 계산하는지는 몰라도 돼요.
 * - 지금은 가짜(MockCalcClient), 파이썬 서버가 생기면 HTTP로 부르는 구현으로 바꿔 끼우기만 하면 돼요.
 *   (스프링이 이 인터페이스를 구현한 @Component를 찾아 Service에 주입해 줘요 = DI)
 */
public interface CalcClient{

    CalculationResponseDTO calculate(int taxYear, IncomeResponseDTO income);
}
