# SCADA Server Java Style Guide

일관된 코드 품질 유지와 유지보수성 향상을 위해 프로젝트 전반에 적용되는 코딩 및 설계 스타일 가이드입니다. 기본적으로 **Google Java Style Guide**를 바탕으로 하며, 프로젝트 특성에 맞는 규칙을 추가했습니다.

## 1. Naming Convention (네이밍 규칙)

* **Class / Interface**: `PascalCase` 사용. (예: `SensorDataService`, `ModbusPollingTask`)
* **Method / Variable**: `camelCase` 사용. (예: `getAlarmHistory()`, `temperatureThreshold`)
* **Constant (상수)**: `UPPER_SNAKE_CASE` 사용. `static final`로 선언. (예: `MAX_RETRY_COUNT`)
* **Package**: 전체 소문자 사용. 언더스코어(`_`) 금지. (예: `com.scada.acquisition.polling`)

### 1.1. 특수 네이밍
* **Boolean 변수/메서드**: `is`, `has`, `can` 등의 접두사 사용. (예: `isActive`, `hasPermission()`)
* **Interface**: 구현체(`Impl`)와 구분하기 위해 `I` 접두사를 쓰지 않습니다. (X: `IUserService` / O: `UserService`)
* **DB Table**: 복수형을 사용하고 `snake_case`로 작성합니다. (예: `sensor_data`, `users`)

## 2. Code Structure (코드 구조)

### 2.1. 모듈 의존성 강제 규칙
Spring Modulith의 철학에 따라, 한 도메인 모듈이 다른 모듈의 내부 구현 패키지(`internal`)에 직접 접근하는 것을 금지합니다.
타 모듈과 통신할 때는 공통 `core` 모듈에 정의된 **Event(ApplicationEvent)** 객체를 발행/구독하는 방식을 최우선으로 합니다.

### 2.2. DTO (Data Transfer Object) 활용
* 클라이언트(웹/WPF)와 주고받는 데이터는 절대 Entity 클래스를 직접 사용하지 않습니다.
* 모든 API Request/Response는 별도의 DTO 클래스(또는 Java 14+ Record)를 생성하여 사용합니다.

## 3. Exception Handling (예외 처리)

* 로직 제어 흐름(Control Flow)을 위한 용도로 Exception을 사용하지 않습니다.
* `RuntimeException`을 직접 던지지 않고, 반드시 `core` 모듈에 정의된 `ScadaApplicationException` 또는 그 하위 커스텀 예외를 상속받아 던집니다.
* 에러 메시지 하드코딩을 피하고 `ErrorCode` Enum을 사용하여 관리합니다.

```java
// Bad
if (value > max) {
    throw new RuntimeException("온도 초과");
}

// Good
if (value > max) {
    throw new ScadaApplicationException(ErrorCode.TEMPERATURE_OUT_OF_RANGE);
}
```

## 4. Logging (로깅)

운영 환경(SRE 관점)에서 장애를 빠르고 정확하게 추적하기 위해 `System.out.println` 사용을 엄격히 금지하며, SLF4J/Logback을 사용합니다.

* `ERROR`: 즉각적인 시스템 수정이나 개입이 필요한 장애 (예: DB 연결 실패)
* `WARN`: 기능은 동작하나 잠재적 문제가 있는 경우 (예: PLC 재연결 시도 1회차)
* `INFO`: 시스템의 주요 상태 변화 (예: 서버 시작, 관리자 로그인 성공)
* `DEBUG` / `TRACE`: 개발 및 트러블슈팅을 위한 상세 데이터 (예: 수신된 Raw Modbus Byte 배열)

> **MDC(Mapped Diagnostic Context)** 활용: 분산 환경이나 비동기 이벤트 처리 로그를 추적하기 위해 모든 로그 라인에 고유한 `traceId`를 남기는 것을 권장합니다.
