# 05. 테스트 · 보안 검증 절차 (JUnit · SpotBugs · 제출 전 점검)

> 이 문서는 **AI 코딩 에이전트와 팀원이 읽고 그대로 실행**하기 위한 작업 지시서다.
> 사용자는 개발 입문자다. 각 단계에서 무엇을 했는지 쉬운 말로 짧게 설명해 준다.
> 선행 조건: `01`~`04` 완료 · DB 접속 정보 · 토큰 값은 이 문서에 적지 않는다 (`.env`에만).

## 1. 자가 채점 (JUnit)

- STS: 테스트 클래스 → 메서드 이름 더블클릭 → 우클릭 → Run As → JUnit Test
  (아직 안 푼 문제가 있으면 클래스 전체 대신 해당 메서드만)
- cmd:
  ```
  cd /d C:\workspace\hope-refund-back
  set JAVA_HOME=C:\dev\jdk-17.0.19
  mvnw.cmd test -Dtest=<테스트클래스명>
  ```
- 통과 기준: `Tests run: N, Failures: 0, Errors: 0` + `BUILD SUCCESS`
- 실패 시 `[ERROR] Failures:` 아래 `expected: <...> but was: <...>` 줄로 원인을 본다.
- `BUILD SUCCESS`가 "답이 맞다"는 뜻은 아니다 (compile = 문법만 · test = 결과 검증).

## 2. 정적 분석 (SpotBugs + Find Security Bugs · `sast` 프로필)

- STS: 프로젝트 우클릭 → Run As → Maven build... → Goals `compile spotbugs:spotbugs` · Profiles `sast` → Run
  (화면으로 보기: Goals `spotbugs:gui` · Profiles `sast`)
- cmd: `mvnw.cmd -Psast compile spotbugs:spotbugs` · `mvnw.cmd -Psast spotbugs:gui`
- 리포트: `target/spotbugsXml.xml`
- 정리 순서: 분류 · 건수 · 심각도(빨강 > 노랑 > 회색) → 경고마다 앞뒤 코드를 보고 **진짜 / 오탐** 판정 → `docs/history.md`에 기록
- 오탐 제외는 `spotbugs-exclude.xml`에 **이유와 함께 좁게만** (메서드 단위로 넓게 빼지 않는다 → 나중에 같은 곳에 생긴 진짜 문제를 놓치지 않게)
- 한계: 컴파일된 Java만 본다 → MyBatis XML(`${}`) · IDOR · XSS · CSRF 같은 로직 문제는 코드 리뷰로 따로 확인한다.

## 3. STS와 cmd 결과가 다를 때 (폴더 확인)

- Maven 로그가 `Nothing to compile`인데 결과가 원본 값 → cmd가 다른 폴더를 보고 있다.
- `findstr /n "<찾을 문자열>" <파일경로>`로 디스크 파일 내용을 직접 확인한다.
- STS 파일 우클릭 → Properties → Resource → Location 경로와 cmd의 `cd` 경로를 비교한다.
- 탭 이름 앞 `*` (저장 안 됨)도 확인한다.

## 4. 제출(push) 전 점검

- 비밀 값(DB 비밀번호 · 토큰 · 키)이 `application.yml` · 코드에 남아 있지 않은지 (`.env`에만)
- 수정한 파일에 오타 · 불필요한 문자(줄 끝 따옴표 등)가 없는지 — 테스트가 안 거치는 코드는 통과해도 놓칠 수 있다.
- `target` 폴더는 제출하지 않는다 (빌드 결과물 · 예전 사본이 남아 있을 수 있음).
- 제출할 폴더가 수정본이 있는 폴더인지 경로부터 확인한다.

## 5. 11/03 실행 결과 (back)

| 항목 | 결과 |
| --- | --- |
| JUnit | 전체 통과 (JDK 17.0.19 · 학원 PC) |
| SpotBugs 1차 | 경고 23건 (높음 0 · 보통 10 · 낮음 13) |
| 판정 | 로그 CRLF 9건 오탐 · API 목록 4건 = 알고 있는 X-Member-Id 문제 · DTO/생성자 9건 오탐 · 박싱 1건 진짜(수정) |
| SpotBugs 2차 | 경고 13건 (모두 판정 기록 있음 · 이보다 늘면 새 경고) |
| 코드 리뷰로 보완 | MyBatis 전부 `#{}` 바인딩 · 오류 응답에 스택 트레이스/SQL 없음 · 계산 서버 토큰 `hmac.compare_digest` · 계산 서버 DB는 읽기 전용 계정 |

## 6. 발표용 요약 (1장)

**"코드 리뷰 + 도구, 두 겹으로 보안을 점검했어요"**

- 기준: 행정안전부 「소프트웨어 개발보안 가이드」 7개 분류 · OWASP Top 10:2025 · CWE 번호
- 도구: SpotBugs + Find Security Bugs (정적 분석) · JUnit (결과 검증)
- 결과: 경고 23건 → 하나씩 판정 → 진짜 1건 수정 · 오탐 9건 이유 기록 후 제외 → 13건 (모두 판정 완료 · 높음 0)
- 도구가 못 보는 부분은 코드 리뷰로: SQL 삽입(바인딩) · 오류 정보 노출 · 최소 권한(계산 서버 읽기 전용 DB 계정) · 로그에 금액 · 생년월일 · 토큰 남기지 않기
- 남은 과제: 회원 확인을 `X-Member-Id` 헤더 → Spring Security 로그인 정보로 교체 (IDOR · CWE-639)

## 7. 전체 흐름 확인 (화면 → back → calc → DB)

단위 테스트는 부분만 본다. 발표 · 검수 전에는 세 서버를 모두 켜고 화면에서 끝까지 한 번 돌려 본다.

1. calc: `cd C:\workspace\hope-refund-calc` → `.venv\Scripts\activate` → `uvicorn app.main:app --port 8000`
2. back: `.env`에 `CALC_MODE=http` · `CALC_URL=http://localhost:8000` · `CALC_API_TOKEN`(calc `.env`와 같은 값) · `SAVE_ENABLED=true` → `mvnw spring-boot:run`
   - `CALC_MODE`가 없으면 가짜 계산(mock) · `SAVE_ENABLED`가 없으면 결과가 DB에 저장되지 않음
3. front(`feature/calculation-api` 브랜치): `frontend/.env`에 `VITE_DEV_MEMBER_ID=DB에 있는 회원번호` → `npm run dev`
4. 화면 `http://localhost:5173/#expenses` → 6단계 입력(중소기업 감면 예/아니요 선택) → 계산 → `#dashboard`
5. 성공 기준: 대시보드 「계산 결과 · 계산값」 표시(「데모값」 없음) · AI 가이드 숫자가 대시보드와 같음 · `PREDICTION`에 같은 결정세액 · 환급액 행

| 화면 알림 | 원인 |
| --- | --- |
| 예시 계산으로 보여 줘요 | back이 꺼져 있음 |
| 로그인이 필요해요 | front `.env` 회원번호 없음 · `.env` 수정 후 `npm run dev` 재시작 안 함 |
| 회원 정보를 찾을 수 없어요 | DB에 없는 회원번호 |
| 계산 서버에 잠시 연결할 수 없어요 | calc 꺼짐 · 두 `.env`의 토큰 값이 다름 |

### 11/03 결과 (학원 PC · 회원 81 · 2026 귀속)

- 입력: 총급여 36,000,000 · 기납부세액 1,523,000 · 나머지 0 · 중소기업 감면 「예」
- 화면 · DB(`PREDICTION`) 모두 결정세액 **1,471,500** · 환급 **51,500**
- 손 계산과 일치: 근로소득공제 10,650,000 · 인적공제 1,500,000 → 과세표준 23,850,000 → 산출세액 2,317,500 − 근로소득세액공제 716,000 − 표준세액공제 130,000 = 1,471,500
- 감면 미적용은 정상: 혜택 진단이 「해당」일 때만 감면 (회원 81은 회사 규모 등 자격 정보 없음)
- 아직 연결 안 됨: 시뮬레이션(저장하지 않는 계산 API 필요) · 카드 사용액(업로드 내역 서버 저장 전)
