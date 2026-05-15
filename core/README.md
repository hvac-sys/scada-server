# SCADA Core Module (`core`)

이 모듈은 SCADA 애플리케이션 전반에서 공통으로 사용되는 모델, DTO, 예외 처리, 유틸리티, 그리고 모듈 간 통신을 위한 이벤트 정의를 모아둔 기반 모듈입니다.

> **주의**: `core` 모듈은 다른 어떤 도메인 모듈(api, alarm, 등)도 의존해서는 안 되며, 반대로 다른 모든 도메인 모듈이 `core` 모듈을 의존합니다.

## 1. 주요 책임 (Responsibilities)

- 전역 공통 에러 코드 및 커스텀 예외(`CustomException`) 클래스 정의.
- 표준 API 응답 포맷(`ApiResponse<T>`) 봉투 클래스.
- 도메인 모듈 간의 느슨한 결합(Loose Coupling)을 위한 Spring `ApplicationEvent` 페이로드 정의.
- 로깅, 날짜/시간 유틸리티 등 공통 도구 제공.

## 2. 주요 패키지 및 클래스 명세

### 2.1. `com.scada.core.event` (이벤트 페이로드)
다른 모듈 간의 통신은 직접 호출을 피하고 이벤트 기반으로 동작합니다.
- `SensorDataCollectedEvent`: `acquisition` 모듈이 PLC에서 데이터를 수집한 직후 발행하는 이벤트. (`alarm`, `historian`, `websocket`이 리스닝)
- `AlarmStatusChangedEvent`: `alarm` 모듈이 알람 상태(발생/복구)를 판정했을 때 발행하는 이벤트. (`historian`, `websocket`이 리스닝)

### 2.2. `com.scada.core.exception` (공통 예외 관리)
모든 에러를 체계적으로 관리합니다.
- `ErrorCode` Enum: `MODBUS_CONN_FAIL(5001, "PLC 통신 실패")`, `INVALID_SP_RANGE(4001, "설정값 범위 초과")` 등 에러 코드와 메시지 정의.
- `ScadaApplicationException`: 에러 코드를 매개변수로 받는 최상위 커스텀 예외.

### 2.3. `com.scada.core.dto` (표준 응답)
REST API 요청 시 항상 아래의 형태로 JSON이 응답되도록 강제합니다.
```java
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private ErrorResponse error;
}
```
