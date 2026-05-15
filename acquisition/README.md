# SCADA Acquisition Module (`acquisition`)

이 모듈은 산업 현장의 제어기기(PLC 등)와 직접 통신하여 센서 데이터를 수집하고, 제어 명령을 하달하는 SCADA의 핵심 엔진 역할을 합니다.

## 1. 주요 책임 (Responsibilities)

- Modbus TCP(또는 RTU over TCP) 프로토콜을 이용한 현장 PLC 데이터 폴링.
- 통신 끊김(Timeout), 재연결(Reconnect) 등 통신 세션 관리.
- 원시 데이터(Raw Byte Array)를 애플리케이션에서 사용하는 도메인 모델(Float, Int 등)로 변환 (엔디안 처리, 스케일링 적용).
- HMI에서 들어온 수동 제어 및 설정값(SP) 변경 명령을 PLC에 Write.
- 수집된 데이터를 Spring `ApplicationEvent`로 발행하여 다른 모듈(`historian`, `alarm`, `websocket`)로 전파.

## 2. 모듈 의존성 (Dependencies)

* `core` (이벤트 DTO)
* *j2mod* 또는 *modbus4j* (Modbus TCP 라이브러리)

## 3. PLC 통신 명세 (Communication Spec)

### 3.1. 연결 정보
* **프로토콜**: Modbus TCP
* **IP Address**: `192.168.1.10` (설정 파일 주입)
* **Port**: `502`
* **Slave ID (Unit ID)**: `1`
* **폴링 주기 (Polling Rate)**: `1000ms` (1초)

### 3.2. Modbus 메모리 맵 매핑표 (Memory Map)

LS산전 XBC PLC와의 통신을 위한 주소 매핑 초안입니다. (Modbus 주소 체계 적용)

| 구분 | PLC 주소 | Modbus Address | Function Code | Data Type | Read/Write | 내용 |
|---|---|---|---|---|---|---|
| **상태** | MW100 | `40101` (Holding Reg) | 03 (Read) | INT16 | Read | 현재 온도 (소수점 1자리 10배수 값. ex: 25.5℃ -> 255) |
| **상태** | MW101 | `40102` (Holding Reg) | 03 (Read) | INT16 | Read | 현재 습도 (소수점 1자리 10배수 값. ex: 50.0% -> 500) |
| **상태** | MW102 | `40103` (Holding Reg) | 03 (Read) | INT16 | Read | 인버터 현재 출력 주파수 (Hz) |
| **설정** | MW110 | `40111` (Holding Reg) | 03 / 06 | INT16 | R/W | 목표 온도 (SP) 설정값 |
| **설정** | MW111 | `40112` (Holding Reg) | 03 / 06 | INT16 | R/W | 목표 습도 (SP) 설정값 |
| **제어** | MX000 | `00001` (Coil) | 01 / 05 | BOOL | R/W | 시스템 전체 RUN (1) / STOP (0) |
| **제어** | MX001 | `00002` (Coil) | 01 / 05 | BOOL | R/W | 제어 모드 수동 (1) / 자동 (0) |
| **알람** | MX100 | `00101` (Coil) | 01 (Read) | BOOL | Read | E-Stop(비상정지) 눌림 상태 |

> **주의**: LS산전 PLC의 워드 메모리(MW)는 통상적으로 Modbus Holding Register(4x)에 매핑됩니다. 정확한 Offset은 XG5000 설정에 따라 1 차이가 날 수 있습니다 (0-based vs 1-based).

## 4. 데이터 스케일링 로직

PLC는 소수점을 전송하지 못하므로, 정수로 변환하여 송수신합니다. 이 모듈에서 변환을 수행합니다.
- `Read`: 수신된 INT16 값 / 10.0 -> Float (애플리케이션 전송)
- `Write`: 전달받은 Float 값 * 10.0 -> INT16 (PLC 전송)
