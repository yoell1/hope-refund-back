# 01. Linter 세팅 (Spotless · Maven)

> 이 문서는 **AI 코딩 에이전트가 읽고 그대로 실행**하기 위한 작업 지시서다.
> 사용자는 개발 입문자다. 각 단계에서 무엇을 했는지 쉬운 말로 짧게 설명해 준다.

## 목표

`./mvnw spotless:check` 한 줄로 프로젝트 전체 코드 모양을 검사할 수 있고, **현재 위반 0개인 깨끗한 상태(baseline)**를 만든다.

## 배경 (사용자 설명용)

- Spotless는 들여쓰기 · 줄바꿈 · import 순서 같은 **코드 모양**을 팀 전체가 똑같이 맞춰 주는 도구다.
- `spotless:apply`는 자동으로 고쳐 주고, `spotless:check`는 맞는지 검사만 한다. (React의 `lint:fix` / `lint`와 비슷)
- 6명이 같이 작업해도 코드 모양 때문에 생기는 쓸데없는 변경(diff)이 사라진다.

## 작업 순서

1. **현재 상태 확인**
   - `pom.xml`의 `<java.version>`이 **17**인지 확인한다.
   - 프로젝트 루트에 `mvnw`(Windows는 `mvnw.cmd`)가 있는지 확인한다. 없으면 사용자에게 알리고 멈춘다.

2. **플러그인 추가** — `pom.xml`의 `<build><plugins>` 안에 아래를 추가한다.
   `<version>`은 Maven Central에서 `com.diffplug.spotless:spotless-maven-plugin`의 **최신 안정 버전**을 확인해 넣고, 넣은 버전을 사용자에게 알려 준다.

   ```xml
   <plugin>
     <groupId>com.diffplug.spotless</groupId>
     <artifactId>spotless-maven-plugin</artifactId>
     <version><!-- 최신 안정 버전 --></version>
     <configuration>
       <java>
         <googleJavaFormat />
         <removeUnusedImports />
       </java>
     </configuration>
   </plugin>
   ```

3. **에디터 연동 (권장)** — 팀원이 IntelliJ/VS Code에서 저장할 때도 같은 모양이 되도록, 사용하는 편집기에 맞는 Java 포맷터 설정 방법을 사용자에게 짧게 안내한다.

4. **실행하고 baseline 만들기**
   - `./mvnw spotless:apply` 실행 → 전체 코드 모양 정리
   - `./mvnw spotless:check` 실행 → 위반 0개 확인
   - **설정을 빼거나 검사를 건너뛰는 옵션(`-Dspotless.check.skip`)으로 숨기지 않는다.**

## 완료 기준 (모두 확인 후 결과를 보여줄 것)

- [ ] `./mvnw spotless:check` 위반 0개
- [ ] `pom.xml`에 Spotless 플러그인 존재
- [ ] 일부러 들여쓰기를 망가뜨리면 `spotless:check`가 잡아내는지 확인한 뒤, `spotless:apply`로 되돌린다

## 마지막 보고

무엇을 추가/수정했는지, `spotless:check` 최종 출력, 기억할 명령어(`./mvnw spotless:apply`, `./mvnw spotless:check`)를 3~5줄로 정리한다.
