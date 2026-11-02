# AGENTS.md

이 파일은 이 저장소에서 일하는 AI 코딩 에이전트(Claude Code, Codex, Cursor 등)를 위한 온보딩 문서다. 작업 전에 반드시 읽는다.

## 프로젝트 개요

희망환급: 청년 근로자가 소득·카드 내역을 넣으면 연말정산 예상 환급액을 계산하고, 절세 방법을 비교·안내받는 웹 서비스의 **API 서버(Spring Boot)** 저장소다. 회원 · 소득 · 카드 업로드 · 결과 저장 · 문의 · 관리자 기능과 DB 저장을 맡는다.

- 함께 쓰는 저장소: `hope-refund-front`(React 화면) · `hope-refund-calc`(Python 세액 계산 · 카드 파일 정리·분류)
- 세액 계산은 calc가 하고, 결과 저장은 이 저장소의 `prediction/`이 한다 (calc 호출 → 결과 · 입력값 · 항목별 내역을 한 트랜잭션으로 저장).
- 범위 밖: 결제 · 카드 내역 직접 입력(파일 업로드만) · 실명 수집

## 명령어

빌드 도구는 **Maven**(`./mvnw`)만 사용한다. Java 17 · Spring Boot 4.0.9 (MyBatis 스타터가 4.0.x를 요구해서 정함 · 팀원 모두 같은 버전).
(Windows 명령 프롬프트(cmd)에서는 `./mvnw` 대신 `mvnw`로 쓴다. PowerShell·Git Bash는 `./mvnw` 그대로)

```bash
./mvnw spring-boot:run   # 서버 실행
./mvnw test              # 테스트 (JUnit)
./mvnw package           # 빌드
./mvnw spotless:apply    # 코드 모양 자동 정리
./mvnw spotless:check    # 코드 모양 검사
```

## 기술 스택

Java 17 · Spring Boot 4.0.9 · Maven · MyBatis · Oracle · Spotless(코드 모양) · JUnit 5(테스트)

## 폴더 구조

```text
src/main/java/com/hoperefund/
├── member/       # MEMBER · PASSWORD_RESET
├── income/       # INCOME
├── expense/      # CARD_UPLOAD · EXPENSE · SPEND_CATEGORY (calc가 정리·분류한 결과 저장)
├── prediction/   # PREDICTION · PREDICTION_INPUT · PREDICTION_DETAIL (calc 호출 → 저장)
├── statistics/   # GROUP_STAT · STAT_FILE
├── chat/         # CHAT_SESSION · CHAT_MESSAGE · RECOMMENDATION (대화 저장)
├── inquiry/      # INQUIRY
├── admin/        # DEDUCTION_TYPE · TAX_BRACKET · BENEFIT_CONDITION 수정
└── common/       # ACCESS_LOG · 보안 · 예외 처리
    (각 폴더 안: controller / service / mapper / dto)
src/main/resources/mapper/   # MyBatis SQL (기능별 폴더)
```

## 규칙

- 모든 조회·수정·삭제 SQL에 로그인한 회원번호(`MEMBER_ID`) 조건을 넣는다. 관리자 API는 `ROLE = 'ADMIN'`을 확인한다.
- 회원정보 수정에서 `ROLE` · `STATUS`는 바꾸지 않는다.
- 기준값(`DEDUCTION_TYPE`)은 번호(`TYPE_ID`)가 아니라 `TAX_YEAR` + `TYPE_CODE`(+ 총급여 구간)로 찾는다. 공용 DB와 새 계정의 번호가 다르다.
- 기본값이 있는 칸은 INSERT에서 빼고 넣는다 (NULL을 넣으면 오류).
- 금액은 원 단위 정수(`long`)로 다룬다.
- DB 오류 코드는 기능명세서 「7_오류 메시지」 시트의 문구로 바꿔 응답한다.
- 새 기능에는 `src/test/java`에 같은 패키지로 `*Test.java`를 함께 작성한다.

## 팀 공통 규칙 (희망연봉2조)

- 브랜치: `main`(최종본) · `develop`(기능이 끝나면 머지) · `feature/기능명`(기능 브랜치 → develop으로 PR). `main`·`develop`에 직접 커밋하지 않는다. (예외: 저장소를 처음 만들 때 `docs/setup` 01~04 초기 세팅은 형상 관리 담당이 `main`에서 진행)
- 커밋 메시지: `종류: 무엇을 했는지` (feat · fix · chore · docs · test · refactor · ci)
- 작업을 마칠 때마다 `docs/history.md`에 기록한다.
- 내 기능 폴더만 수정한다. 다른 기능 폴더나 공통 폴더를 고쳐야 하면 먼저 사용자에게 묻는다.
- DB 접속 정보 · API 키는 `.env`에만 쓴다. `.env.example`에는 이름만 적는다.
- DB 테이블·칸은 바꾸지 않는다. 필요하면 노션 「요청·전달」에 DB 변경 요청을 남긴다 (DB 담당: 이우진).
- 기능 사양은 기능명세서(구글 시트)를 기준으로 한다: https://docs.google.com/spreadsheets/d/1qW5xjOhPqrWkCn9KLUUbylKfsFJCacfOAgBiXm27v9w/edit
- 테이블·칸은 ERD(ERDCloud)가 기준이다: https://www.erdcloud.com/d/mL4eEnZLQ3QjgbWF4
- 사용자에게 보이는 문구는 한글로 쓴다.
- 요청받지 않은 외부 라이브러리를 설치하지 않는다. 필요하면 먼저 이유를 말하고 묻는다.

## 작업 완료 조건

작업을 "끝났다"고 말하기 전에 아래를 실행하고, 모두 통과한 결과를 보여준다.

```bash
./mvnw spotless:check && ./mvnw -B verify
```

## 하지 말 것

- `.env*` 파일 내용 출력·커밋 금지.
- 사용자 확인 없이 `git push`, 브랜치 삭제, 강제 푸시(`git push -f`) 금지.
- 안 쓰는 import·변수를 남긴 채 커밋하지 않는다.
- 린터 규칙을 끄거나 주석으로 숨기지 않는다. 꼭 필요하면 이유를 말하고 허락을 받는다.

## 참고 문서 (필요할 때만 읽는다)

- 개발 환경 세팅 절차: `docs/setup/` (01-linter → 02-testing → 03-github → 04-github-actions)
- 작업 이력: `docs/history.md`
