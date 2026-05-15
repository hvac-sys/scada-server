# SCADA WebSocket Module (`websocket`)

이 모듈은 SCADA 서버에서 발생하는 실시간 센서 데이터와 알람 이벤트를 HMI 클라이언트(WPF)로 실시간 푸시(Push)하는 역할을 담당합니다.

## 1. 주요 책임 (Responsibilities)

- STOMP 기반의 WebSocket 연결 관리 및 브로커(Broker) 구성.
- `acquisition` 모듈에서 수집된 데이터를 구독(Subscribe) 중인 클라이언트로 1초 주기로 브로드캐스트.
- `alarm` 모듈에서 발생/복구된 알람 이벤트를 실시간으로 푸시.

## 2. 모듈 의존성 (Dependencies)

* `core` (공통 DTO 및 이벤트 페이로드)
* `security` (WebSocket 연결 시 토큰 검증)
* *Spring WebSocket, STOMP*

## 3. 실시간 통신 명세 (STOMP Specification)

클라이언트는 STOMP 프로토콜을 사용하여 서버에 연결(Connect)한 후, 필요한 Topic을 구독(Subscribe)합니다.

### 3.1. Connection
* **Endpoint**: `/ws-scada`
* **Handshake Protocol**: HTTP Upgrade
* **Authentication**: Connection Header에 JWT Token 포함

### 3.2. Subscribe Topics (서버 -> 클라이언트)

| Topic Path | 발행 주기 | 메시지 내용 | 페이로드(JSON) 예시 |
|---|---|---|---|
| `/topic/sensors` | 1초 | 전체 장비의 실시간 센서값 및 가동 상태 | `{"timestamp": "2026-...", "temp": 22.5, "hum": 55.0, ...}` |
| `/topic/alarms` | 이벤트 발생 시 | 알람 발생, 확인(Ack), 복구 이벤트 | `{"alarmId": 123, "type": "CRITICAL", "status": "OCCURRED", "message": "온도 상한 초과"}` |

### 3.3. Publish Destinations (클라이언트 -> 서버)

클라이언트에서 서버로 메시지를 보낼 때 사용합니다. (REST API를 대신하여 빠른 응답이 필요한 제어용)

| Destination | 역할 | 페이로드(JSON) 예시 |
|---|---|---|
| `/app/control/mode` | 수동/자동 모드 변경 | `{"mode": "MANUAL"}` |
| `/app/alarm/ack` | 알람 인지(Acknowledge) 처리 | `{"alarmId": 123, "userId": "admin"}` |

## 4. 내부 동작 흐름 (Internal Flow)

1. `acquisition` 모듈이 PLC 데이터를 읽어 Spring `ApplicationEvent` 발행.
2. `websocket` 모듈 내 리스너(`@EventListener`)가 이벤트를 캐치.
3. 데이터를 WebSocket DTO로 변환 후 `SimpMessagingTemplate`을 사용하여 `/topic/sensors`로 전송.
