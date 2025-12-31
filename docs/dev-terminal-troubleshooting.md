# 개발 환경 트러블슈팅: 테스트(터미널) 출력이 비어 보이는 문제

## 증상
- IDE/자동 실행에서 Maven/PowerShell 명령을 실행해도 출력이 비어 보임
- 실제로는 명령이 수행되며, 파일 리다이렉트(`> log.txt 2>&1`)로 보면 로그가 생성됨

## 원인(확인된 패턴)
1) PowerShell에서 `-Dspring.profiles.active=test` 같은 인자가 **`.`(dot)** 때문에 잘못 전달되는 케이스가 있음
   - 결과적으로 Maven이 `Unknown lifecycle phase ".profiles.active=test"` 같은 오류를 냄
2) 현재 환경에서는 표준 출력 캡처가 정상적으로 표시되지 않는 경우가 있어, 로그 파일로 확인하는 편이 확실함

## 해결 방법
### 1) Spring 프로필 값을 Maven에 전달할 때는 반드시 따옴표로 감싸기(권장)

```powershell
.\mvnw.cmd "-Dtest=ReviewPhoneVerifiedP0Test" "-Dspring.profiles.active=test" test -DtrimStackTrace=false
```

### 2) stdout 캡처가 비어도 로그로 확인하기

```powershell
.\mvnw.cmd "-Dtest=ReviewPhoneVerifiedP0Test" "-Dspring.profiles.active=test" test -DtrimStackTrace=false > _tmp_test.log 2>&1
Get-Content _tmp_test.log -Tail 200
```

### 3) 테스트 클래스에서 프로필을 고정하는 방법(보조)
- `@ActiveProfiles("test")`
- 필요하면 `@TestPropertySource(properties = { ... })`로 datasource를 H2로 강제

## 참고
- 본 프로젝트의 `application.yml`은 기본이 PostgreSQL이며, 테스트 프로필에서 H2를 사용하도록 분리되어 있음
- 따라서 테스트는 **반드시 `test` 프로필로 뜨는지**가 핵심

