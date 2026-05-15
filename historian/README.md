# SCADA Historian Module (`historian`)

이 모듈은 SCADA 시스템의 시계열 데이터(센서 수집값)와 알람 이력을 데이터베이스(TimescaleDB)에 영속화(Persist)하고, 효율적으로 조회하는 책임을 가집니다.

## 1. 주요 책임 (Responsibilities)

- 초당 대량으로 발생하는 센서 데이터를 병목 없이 TimescaleDB Hypertable에 저장.
- 시계열 데이터의 장기 보관을 위한 파티셔닝(Chunk) 및 연속 집계(Continuous Aggregates) 관리.
- 알람 발생, Acknowledge, 복구 이력을 기록.
- HMI 클라이언트에서 기간별 트렌드 차트를 그리기 위한 데이터 제공 로직 구현.

## 2. 모듈 의존성 (Dependencies)

* `core` (공통 DTO)
* *Spring Data JPA, Hibernate*
* *TimescaleDB (PostgreSQL)*

## 3. 데이터베이스 스키마 명세 (TimescaleDB Schema)

### 3.1. 시계열 센서 데이터 (`sensor_data`)

초당 대량 적재되는 데이터이므로 TimescaleDB의 **Hypertable**로 구성합니다.

| Column Name | Data Type | PK/Idx | Description |
|---|---|---|---|
| `time` | `TIMESTAMPTZ` | PK | 데이터 수집 시각. (Hypertable 시간 파티션 키) |
| `equipment_id` | `VARCHAR(50)` | PK | 장비 식별자 (FK 없음 - 삽입 성능 최적화) |
| `temperature` | `FLOAT` | | 현재 온도 측정값 |
| `humidity` | `FLOAT` | | 현재 습도 측정값 |
| `status_code` | `INT` | | 장비 현재 상태(운전/정지/비상 등) 코드 |

> **Hypertable 정책**: `time` 컬럼 기준으로 1일(1 day) 단위 Chunk 자동 생성 (`chunk_time_interval` 설정).

### 3.2. 알람 이력 테이블 (`alarm_history`)

| Column Name | Data Type | PK/Idx | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | 이력 고유 ID |
| `equipment_id` | `VARCHAR(50)` | Idx | 알람 발생 장비 식별자 |
| `severity` | `VARCHAR(20)` | | 알람 등급 (WARNING, CRITICAL 등) |
| `message` | `VARCHAR(255)`| | 알람 발생 사유 / 메시지 |
| `occurred_at` | `TIMESTAMPTZ` | Idx | 알람 발생 시각 |
| `acked_at` | `TIMESTAMPTZ` | | 관리자 확인(Ack) 시각 |
| `cleared_at` | `TIMESTAMPTZ` | | 조건 해소(복구) 시각 |

### 3.3. 연속 집계 (Continuous Aggregates) - *도입 예정*

대시보드에서 1주일, 1달 트렌드를 조회할 때 수백만 건을 조회하는 것을 방지하기 위해 Materialized View를 사용합니다.
- `sensor_data_1m_agg`: 1분 단위 평균값
- `sensor_data_1h_agg`: 1시간 단위 평균, 최소, 최대값
