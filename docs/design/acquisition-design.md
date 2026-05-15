# Detailed Design: Acquisition Module (`acquisition`)

이 문서는 이기종 PLC(현장 제어기기)로부터 데이터를 실시간으로 읽어오고, 제어 명령을 내리는 통신 모듈의 상세 설계를 정의합니다.

## 1. Class Architecture Overview

```mermaid
classDiagram
    class ModbusPollingScheduler {
        +pollPeriodically()
    }
    class ModbusClientAdapter {
        <<interface>>
        +readHoldingRegisters(address, length) short[]
        +writeSingleCoil(address, value) void
    }
    class J2ModAdapter {
        -TcpMaster master
        +connect()
        +readHoldingRegisters()
    }
    class RawDataParser {
        +parse(short[] rawData) Map
    }
    class SensorEventPublisher {
        +publish(Map parsedData)
    }

    ModbusPollingScheduler --> ModbusClientAdapter : uses
    ModbusClientAdapter <|-- J2ModAdapter : implements
    ModbusPollingScheduler --> RawDataParser : parses
    ModbusPollingScheduler --> SensorEventPublisher : delegates
```

## 2. Polling Lifecycle & Backoff Strategy

네트워크 불안정으로 인한 폴링 실패 및 자동 재연결(Backoff) 시나리오 설계입니다.

```mermaid
stateDiagram-v2
    [*] --> CONNECTING : 애플리케이션 시작
    CONNECTING --> POLLING : 소켓 연결 성공
    CONNECTING --> BACKOFF : 소켓 연결 실패
    
    POLLING --> POLLING : 1000ms 주기 Read 성공
    POLLING --> BACKOFF : Read Timeout 발생 (3회 누적)
    
    BACKOFF --> CONNECTING : 5초 대기 후 재시도
    
    note right of BACKOFF
      이 상태 진입 시
      "COMM_FAULT" 알람 이벤트를
      발행하여 시스템에 통보함
    end note
```

## 3. Data Parsing Algorithm (Scaling)

Modbus 프로토콜은 기본적으로 16비트 정수(`short` 또는 `INT16`)만 전송할 수 있으므로, 소수점을 포함한 아날로그 센서 데이터 처리를 위한 스케일링(Scaling) 알고리즘이 필요합니다.

### 3.1. Read (PLC -> SCADA)
* **PLC 메모리 값**: `255` (온도 25.5℃)
* **파싱 로직 (`RawDataParser`)**: 
  ```java
  float temperature = (float) rawRegisters[0] / 10.0f;
  ```

### 3.2. Write (SCADA -> PLC)
* **HMI 설정 요청값**: `26.5` (목표 온도)
* **변환 로직**: 
  ```java
  short plCTargetValue = (short) (requestedValue * 10.0f);
  // 이후 ModbusClientAdapter.writeSingleRegister() 호출
  ```

## 4. Threading Model
* 기본적으로 Spring `@Scheduled(fixedRate = 1000)`를 이용하여 별도의 스레드 풀(TaskScheduler)에서 동작합니다.
* 장비 대수가 늘어날 경우를 대비하여 폴링 메서드 전체에 타임아웃(Timeout) 방어 코드를 작성하고 비동기(Async) IO 확장을 고려한 인터페이스 구조를 채택했습니다.
