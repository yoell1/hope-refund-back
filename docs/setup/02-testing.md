# 02. Testing 세팅 (JUnit 5)

> 이 문서는 **AI 코딩 에이전트가 읽고 그대로 실행**하기 위한 작업 지시서다.
> 사용자는 개발 입문자다. 각 단계에서 무엇을 했는지 쉬운 말로 짧게 설명해 준다.
> 선행 조건: `01-linter.md` 완료 (`./mvnw spotless:check` 통과 상태)

## 목표

`./mvnw test` 한 줄로 테스트를 실행할 수 있고, **예시 테스트 3개 이상이 통과**하는 상태를 만든다.

## 배경 (사용자 설명용)

- 테스트는 "내가 만든 메서드가 **의도대로 동작하는지** 자동으로 검산"하는 코드다.
- 나중에 코드를 고쳐도(특히 AI가 고쳐도) 기존 기능이 안 깨졌는지 바로 알려 준다.
- Spring Boot는 `spring-boot-starter-test`에 JUnit 5가 이미 들어 있어서 따로 설치할 게 없다.

## 작업 순서

1. **의존성 확인** — `pom.xml`에 `spring-boot-starter-test`(scope test)가 있는지 확인한다. 없으면 추가한다.

2. **예시 코드 + 테스트 작성**
   - `src/main/java/com/hoperefund/common/util/MoneyFormat.java`에 숫자를 `12,000원` 형태로 바꾸는 `format(long amount)` 메서드를 만든다.
   - 같은 패키지의 `src/test/java/com/hoperefund/common/util/MoneyFormatTest.java`에 테스트를 만든다.
   - 테스트 작성 규칙:
     - **테스트 하나 = 확인 하나** (`@Test` 하나에 검증 하나)
     - 본문은 `// Given` `// When` `// Then` 주석으로 3단 구분
     - 메서드 이름 위에 `@DisplayName("12000을 \"12,000원\"으로 표시한다")`처럼 한글 설명
     - 최소 케이스: `0 → "0원"`, `12000 → "12,000원"`, `1000000 → "1,000,000원"`

   ```java
   @Test
   @DisplayName("12000을 \"12,000원\"으로 표시한다")
   void format12000() {
       // Given
       long amount = 12000L;
       // When
       String result = MoneyFormat.format(amount);
       // Then
       assertEquals("12,000원", result);
   }
   ```

3. **DB가 필요한 테스트는 지금 만들지 않는다** — 예시 테스트는 DB 없이 도는 순수 메서드만 테스트한다. (DB 연결 테스트는 CI에서 DB가 없어 실패한다)
   - Spring Initializr가 만든 기본 테스트(`...ApplicationTests`의 `contextLoads`)는 서버 전체를 띄우면서 DB에 접속하려 해서, DB 설정을 넣은 뒤에는 CI에서 실패한다. 실패하면 사용자에게 알리고 지울지 상의한다. (`@Disabled`로 숨기지 않는다)

4. **실패하는 걸 한 번 보여주기** — `format`에서 쉼표 처리를 잠깐 지워 `./mvnw test`가 **실패**하는 것을 사용자에게 보여준 뒤, 원래대로 되돌려 다시 통과시킨다.

## 완료 기준 (모두 확인 후 결과를 보여줄 것)

- [ ] `./mvnw test` → 모든 테스트 통과 (최소 3개)
- [ ] `./mvnw spotless:check` → 여전히 위반 0개 (테스트 파일 포함)
- [ ] 일부러 깨뜨렸을 때 실패하는 것을 확인하고 원상 복구함

## 마지막 보고

만든 파일, `./mvnw test` 최종 출력, 기억할 명령어(`./mvnw test`)를 3~5줄로 정리한다.
