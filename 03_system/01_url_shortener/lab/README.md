# Java Lab

독립 Java 코드와 Snowflake 동시성 비교 코드를 실행하는 Gradle Java 프로젝트입니다.
Spring Boot 애플리케이션과 분리되어 있으며 Java 17을 사용합니다.

## 프로젝트 구조

```text
lab/
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
└── src/
    ├── main/java/
    └── test/java/
```

## 실행

```powershell
cd lab
.\gradlew.bat run
```

기본 실행 클래스는 `com.example.urlshortener.lab.SnowflakeComparisonRunner`입니다.
운영 코드에서 옮긴 CAS 구현과 비원자 구현에 다음 조건을 동일하게 적용합니다.

- worker ID
- 고정 timestamp
- 스레드 수
- 스레드당 ID 생성 수
- 시작 시점과 첫 상태 읽기 배리어

```powershell
.\gradlew.bat run
```

기본값은 64개 스레드에서 스레드당 16개씩, 총 1,024개 ID를 생성합니다.
스레드 수와 스레드당 생성 수는 실행 인자로 변경할 수 있습니다.

```powershell
.\gradlew.bat run --args="8 16"
```

고정 timestamp를 사용하므로 총 생성 수는 한 시각의 시퀀스 범위인 2,048개 이하여야 합니다.

출력 항목:

- `total`: 메서드가 반환한 전체 ID 수
- `unique`: 고유 ID 수
- `duplicates`: 중복 ID 수
- `retries`: CAS 재시도 수
- `calls/s`: 중복을 포함한 메서드 반환 처리량
- `unique-ids/s`: 실제 고유 ID 처리량

처리시간은 일반 Java 러너에서 확인하는 참고값입니다. 정식 마이크로벤치마크 결과로 사용하지 않습니다.
