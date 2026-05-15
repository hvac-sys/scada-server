# SCADA API Module (`api`)

이 모듈은 SCADA 시스템의 메인 진입점(`@SpringBootApplication`)이자, 타 시스템(WPF HMI Client 등)과의 HTTP REST 연동을 담당합니다. 

## 1. 주요 책임 (Responsibilities)

- HMI 클라이언트에서 요청하는 REST API 엔드포인트 제공.
- 타 모듈(`historian`, `alarm`, `acquisition` 등)의 Service 계층을 조합하여 Facade 패턴으로 API 응답 생성.
- 글로벌 예외 처리(`@RestControllerAdvice`)를 통한 표준 Error Response 반환.

## 2. 모듈 의존성 (Dependencies)

* `core` (공통 DTO 및 유틸)
* `security` (API 엔드포인트 보호)
* `historian` (이력 데이터 조회)
* `alarm` (알람 설정 변경)
* *WebMVC, Validation, Swagger (OpenAPI 3.0)*

## 3. REST API 명세 (Endpoint Specifications)

> 상세 명세는 Swagger UI (`/swagger-ui/index.html`)를 통해 자동화하여 제공할 예정이며, 아래는 주요 기능에 대한 설계 초안입니다.

### 3.1. 이력 조회 (Historian)

| Method | Endpoint | Description | Request | Response |
|---|---|---|---|---|
| `GET` | `/api/v1/history/sensors` | 특정 기간/장비 센서 데이터 조회 | Query Params: `start`, `end`, `equipmentId` | `200 OK` (SensorDataDTO List) |
| `GET` | `/api/v1/history/alarms` | 알람 이력 조회 | Query Params: `start`, `end`, `severity` | `200 OK` (AlarmHistoryDTO List) |
| `GET` | `/api/v1/history/export` | 이력 데이터 CSV/Excel 다운로드 | Query Params: `type`, `start`, `end` | `200 OK` (File Stream) |

### 3.2. 제어 및 설정 (Control & Settings)

| Method | Endpoint | Description | Request | Response |
|---|---|---|---|---|
| `POST` | `/api/v1/control/equipment/{id}` | 장비 수동 제어(기동/정지) 명령 | Body: `{"command": "START"}` | `200 OK` |
| `PUT` | `/api/v1/settings/sp` | 목표 온도/습도 설정값(SP) 변경 | Body: `{"tempSp": 22.5, "humSp": 50.0}` | `200 OK` |
| `PUT` | `/api/v1/settings/alarm-rules` | 알람 임계값 룰 변경 | Body: (AlarmRuleDTO) | `200 OK` |

### 3.3. 공통 응답 포맷 (Common Response Format)

정상 및 에러 응답은 `core` 모듈에 정의된 표준 봉투(Envelope) 포맷을 사용합니다.

```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```
