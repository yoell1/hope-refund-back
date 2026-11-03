# 작업 이력

> 기능 구현 · 리팩터링 · 버그 수정을 마칠 때마다 아래 형식으로 기록한다. 최신 작업이 위로 오게 쌓고, 지난 기록은 지우지 않는다.

<!--
### [YYYY-MM-DD] 작업 요약 (작성자 · 브랜치)
- 작업 내용:
- 변경 사유:
- 검증 결과: (실행한 명령과 결과)
-->

### [2026-11-03] 보안 점검 반영: 서버 로그 · 동시 저장 409 · 계산 서버 입력 거부 구분 (이우진 · feature/security-fix)
- **작업 내용**: 서버 로그 추가(소득 저장 · 계산 요청/완료 · 걸린 시간 · predictionId · 감면 Y→N 전환 · 혜택 진단 결과 코드 · 400/404/401 경고) · 소득 첫 저장이 동시에 겹치면(UQ_INCOME_MEMBER_YEAR) 500 대신 409 `CONFLICT` · 계산 서버가 입력을 거부(400)하면 503 대신 400 `CALC_REJECTED` · 계산 서버 빈 응답 · 진단 결과 목록 없음 처리(감면 없이) · `IncomeRequestDTO` 모든 @NotNull · @Max에 한글 안내 문구
- **변경 사유**: 보안약점 점검(행정안전부 「소프트웨어 개발보안 가이드」 7개 분류 · OWASP Top 10:2025 A09 · A10) — 정상 처리 로그가 없어 추적 불가(CWE-778) · 동시 요청 시 500(시간 및 상태) · Null Pointer 가능(코드오류) · 계산 서버 거부를 「연결 불가」로 안내(에러처리)
- **로그 규칙**: 회원번호 · 연도 · predictionId · 혜택 코드 · 결과만 남기고 금액 · 생년월일 · 입사일 · 토큰은 남기지 않음(CWE-532)
- **검증 결과**: 미검증 (작업 환경에서 Maven 저장소 접근 불가 → 로컬 `mvnw clean test` 필요) · 테스트 2개 추가(동시 저장 → ConflictException · 진단 결과 없음 → 감면 N)
- **미검증**: 실제 Oracle에서 동시 저장 시 DuplicateKeyException으로 바뀌는지 · DB 접근 기록(ACCESS_LOG)은 팀과 방식 정한 뒤

### [2026-11-02] 중소기업 감면은 진단 「해당」일 때만 · 계산 연도는 DB 기준값으로 (이우진 · feature/sme-check)
- **작업 내용**: 소득 입력의 감면 신청이 Y여도 혜택 진단(F-03-02) SME_YOUTH가 「해당」이 아니면(비해당 · 정보 부족) 감면 없이 계산하고 입력값도 N으로 저장 · 계산 가능 연도를 코드 상수(2025~2026) 대신 DEDUCTION_TYPE 행 유무로 확인(`IncomeMapper.countCriteria`) · TODO 주석 「세션」 → 「Spring Security」 · 혜택 진단 주석에 명세서 번호(F-03-02) 같이 적음
- **변경 사유**: 자격 없는 회원이 감면 Y를 고르면 90% 감면이 그대로 들어가 환급액이 부풀던 문제(기능명세서 F-03-02 ④) · 새 연도 기준값을 넣어도 back이 먼저 막던 문제
- **검증 결과**: 미검증 (작업 환경에서 Maven 저장소 접근 불가 → 로컬 `mvnw clean test` 필요) · 테스트 3개 추가(해당 → Y · 비해당 → N · 정보 부족 → N)
- **미검증**: 실제 계산 서버로 감면 Y 회원 계산

### [2026-11-02] 코드 검토 반영: 트랜잭션 분리 · 연결 타임아웃 · mock 저장 막기 (이우진 · feature/review-fix)
- **작업 내용**: 저장을 `PredictionSaveService`(@Transactional)로 분리해 계산 서버 호출을 트랜잭션 밖으로 · `HttpCalcClient` 연결 3초 제한 + HTTP/1.1 고정 · `CALC_MODE`가 http가 아니면 `SAVE_ENABLED=true`여도 저장 안 함(경고 로그) · `CalculationController` 틀린 주석 수정
- **변경 사유**: 계산 서버가 느릴 때 DB 연결을 붙잡지 않게 · 계산 서버 주소가 틀려도 오래 기다리지 않게 · uvicorn 「Unsupported upgrade request」 경고 · 설정 실수로 가짜 결과(세액 0)가 공용 DB에 쌓이는 것 방지
- **검증 결과**: 미검증 (작업 환경에서 Maven 저장소 접근 불가 → `mvnw clean test`는 로컬에서 실행 필요) · 테스트 1개 추가(가짜 계산 모드에서는 저장 안 함)
- **미검증**: 실제 계산 서버 연결 상태에서 경고가 사라지는지

### [2026-11-02] 파이썬 계산 서버 연결 · 카드 합계 · 혜택 진단 API · Spotless (이우진 · feature/calc-connect)
- 작업 내용: `HttpCalcClient`(POST /calculate · /benefit · X-Calc-Token · 10초 제한 · 연결 실패 503 CALC_UNAVAILABLE) · `hope.calc.mode`로 mock/http 선택 · 계산 때 무주택 여부와 귀속 연도 카드 합계(EXPENSE 분류+결제수단별)를 같이 넘김 · `GET /api/benefits/{taxYear}`(F-12 · MEMBER 자격 정보 → 계산 서버 진단) · 설정은 .env(CALC_MODE · CALC_URL · CALC_API_TOKEN · SAVE_ENABLED) · Spotless(안 쓰는 import 제거 · 줄 끝 공백 · 마지막 줄바꿈만 · 중괄호 모양은 그대로) · 중복 설정 `application.yaml` 삭제
- 변경 사유: TT-10 [BE] 세액 계산·진단 연결
- 검증 결과: CalculationServiceTest 9개 · BenefitServiceTest 3개 (DB 없이 가짜 매퍼) · HttpCalcClient와 실제 DB 저장은 미검증 (계산 서버 실행 후 확인)

### [2026-11-02] 계산 결과 저장 (이우진 · feature/prediction-save)
- 작업 내용: 계산 요청 때 PREDICTION · PREDICTION_INPUT · PREDICTION_DETAIL을 한 트랜잭션으로 저장 · 무주택 세대주는 MEMBER에서 읽어 PREDICTION_INPUT에 복사 · 상세의 TYPE_ID는 연도 + 코드 + 총급여 구간으로 찾음 (0개·여러 개면 저장 중단) · 응답에 `predictionId` 추가 · 저장은 설정 `hope.prediction.save-enabled`가 true일 때만 (지금 false)
- 변경 사유: TT-10 [BE] 3단계 · 지난 결과 다시 열기 · 파이썬 서버 연결 전 가짜 결과가 공용 DB에 쌓이지 않게 저장은 꺼 둠
- 검증 결과: CalculationServiceTest 9개 (DB 없이 가짜 매퍼) · 실제 DB 저장은 미검증 (파이썬 서버 연결 후 save-enabled: true로 확인 예정)

### [2026-11-02] DTO 이름 규칙 · 계산 폴더 위치 · NO_CRITERIA 오류 (이우진 · feature/rename-dto)
- 작업 내용: `IncomeRequest` → `IncomeRequestDTO` · `IncomeResponse` → `IncomeResponseDTO` · `CalculationResultDTO` → `CalculationResponseDTO` · 계산 코드를 `calculation/`에서 `prediction/`으로 옮김 (AGENTS.md 폴더 구조 기준) · 기준값 없는 연도 오류의 error 값을 `NO_CRITERIA`로 · 중괄호 모양을 교안 방식(`){`)으로 통일 · AGENTS.md에 DTO 이름 규칙 추가
- 변경 사유: 교안의 DTO 이름 방식 + 받는 값/돌려주는 값 분리(요청으로 번호·작성일을 못 바꾸게) · 「동석님 전달」 오류 모양과 맞추기
- 검증 결과: IncomeServiceTest 4개 · CalculationServiceTest 4개 · JSON 칸 이름은 그대로라 화면 쪽 변경 없음

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
