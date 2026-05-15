# Detailed Design: Core Module (`core`)

이 문서는 SCADA 시스템의 근간이 되는 `core` 모듈의 내부 클래스 및 인터페이스 구조를 상세히 정의합니다.

## 1. Class Architecture Overview

`core` 모듈은 타 도메인 모듈(acquisition, alarm 등)에 직접적인 의존성을 가지지 않으며, 모든 모듈이 공유해야 하는 **추상화(Abstraction)와 공통 규약**을 제공합니다.

### 1.1. Package Structure
```text
com.scada.core
├── exception/        # 공통 에러 및 커스텀 예외 클래스
├── dto/              # API 표준 응답(Response) 래퍼 클래스
├── event/            # 모듈 간 통신(Pub/Sub)을 위한 이벤트 Payload
└── util/             # 전역 유틸리티 (DateTime, Scaling 등)
```

## 2. Event Payload Design (이벤트 기반 통신)

Spring Modulith 환경에서 도메인 간의 결합도를 낮추기 위해 `ApplicationEvent`를 사용합니다.

### 2.1. `SensorDataCollectedEvent`
* **발행처**: `acquisition` 모듈 (Modbus 폴링 성공 시)
* **구독처**: `historian` (DB 저장), `alarm` (임계값 평가), `websocket` (HMI 푸시)
* **Fields**:
  * `String equipmentId`: 장비 식별자 (예: "AHU-01")
  * `LocalDateTime timestamp`: 수집 시각
  * `Map<String, Object> data`: 센서 데이터 맵 (예: `{"temperature": 25.5, "humidity": 50.0}`)
  * `int statusCode`: PLC Run/Stop/Error 상태 코드

### 2.2. `AlarmStatusChangedEvent`
* **발행처**: `alarm` 모듈 (알람 룰 평가 후 상태 변경 시)
* **구독처**: `historian` (이력 저장), `websocket` (알람 팝업 푸시)
* **Fields**:
  * `String equipmentId`: 장비 식별자
  * `AlarmSeverity severity`: 알람 등급 (INFO, WARN, CRITICAL, FAULT)
  * `AlarmState previousState`: 이전 상태
  * `AlarmState currentState`: 변경된 현재 상태 (NORMAL, OCCURRED, ACKED, CLEARED)
  * `String message`: 알람 발생 사유

## 3. Exception Handling Design

### 3.1. `ErrorCode` (Enum)
시스템의 모든 예외 상황을 코드로 관리합니다. HTTP 상태 코드와 비즈니스 코드를 매핑합니다.
* `INVALID_SP_VALUE(400, "SCADA-4001", "설정값 범위를 초과했습니다.")`
* `MODBUS_CONN_TIMEOUT(503, "SCADA-5001", "PLC 통신 응답 지연")`

### 3.2. `ScadaApplicationException`
`RuntimeException`을 상속받는 최상위 커스텀 예외 클래스입니다.
* **Fields**: `ErrorCode errorCode`, `String customMessage`
* 모든 도메인 예외는 이를 상속받거나 던져야 하며, `api` 모듈의 `@RestControllerAdvice`에서 이를 캐치하여 일관된 JSON으로 변환합니다.

## 4. Standard Response DTO

REST API 응답 시 항상 동일한 봉투(Envelope) 패턴을 유지하기 위한 공통 제네릭 레코드(Record)입니다.

```java
public record ApiResponse<T>(
    boolean success,
    T data,
    ErrorDetail error
) {
    public static <T> ApiResponse<T> ok(T data) { ... }
    public static <T> ApiResponse<T> fail(ErrorDetail error) { ... }
}
```
