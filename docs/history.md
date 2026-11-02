# 작업 이력

> 기능 구현 · 리팩터링 · 버그 수정을 마칠 때마다 아래 형식으로 기록한다. 최신 작업이 위로 오게 쌓고, 지난 기록은 지우지 않는다.

<!--
### [YYYY-MM-DD] 작업 요약 (작성자 · 브랜치)
- 작업 내용:
- 변경 사유:
- 검증 결과: (실행한 명령과 결과)
-->

### [2026-11-02] 계산 요청 API · 가짜 결과 · 오류 응답 보안 (이우진 · feature/calculation)
- 작업 내용: `POST /api/calculations/{taxYear}` · 저장된 본인 소득을 읽어 계산 창구(CalcClient)에 넘김 · 지금은 MockCalcClient가 「동석님 전달」 모양의 가짜 결과를 돌려줌 (세액·공제액 0 · 대상 금액만 입력값)
- 보안: 오류 응답에 스택 트레이스가 그대로 나가던 문제 수정 (DB 주소 노출) → application.yml `server.error.include-stacktrace: never` · GlobalExceptionHandler에 형식 오류(400)·없는 주소(404)·방식 오류(405)·그 밖의 오류(500, 원인은 로그에만) 추가
- 변경 사유: TT-10 [BE] 2단계 · 파이썬 서버 연결 전에 화면이 결과 모양을 먼저 붙여 볼 수 있게
- 남은 위험: 회원번호를 X-Member-Id 헤더로 받음 (개발용) → 로그인 연결 때 세션으로 바꿔야 함
- 검증 결과: CalculationServiceTest 4개 (DB 없이 가짜 매퍼)

### [2026-11-02] 소득 입력 저장·조회 API (이우진 · feature/income)
- 작업 내용: `GET/PUT /api/incomes/{taxYear}` · INCOME 조회·저장(MERGE) · 입력 검사(@Valid · 자녀 수 ≤ 부양가족 수 · 귀속 연도 2025~2026) · 공통 오류 응답 {"error","message"} · DB 접속은 .env
- 변경 사유: TT-10 [BE] 세액 계산·진단 1단계 (소득 입력이 있어야 계산 가능) · v4.1 연금저축/IRP 두 칸 반영
- 검증 결과: IncomeServiceTest 4개 (DB 없이 가짜 매퍼) · 회원번호는 로그인 연결 전까지 X-Member-Id 헤더
