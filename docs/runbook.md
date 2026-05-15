# SRE Runbook: SCADA Server Operations

이 문서는 SCADA 서버 운영 중 발생할 수 있는 주요 장애 상황(Incidents)에 대한 원인 분석 및 대응 매뉴얼(Playbook)입니다. SRE(Site Reliability Engineering) 원칙에 따라 신속한 복구를 목적으로 합니다.

---

## 🚨 Incident 1: PLC 통신 단절 (Modbus Timeout Error)

**증상**
* 에러 로그: `java.net.SocketTimeoutException: Read timed out` 또는 `ModbusConnectionException`
* 현상: HMI 대시보드의 센서 데이터가 갱신되지 않으며, SCADA 서버에 "COMM_FAULT" 알람 발생.

**대응 절차 (Troubleshooting Steps)**
1. **Ping 테스트**: SCADA 서버 쉘에서 PLC IP(예: `192.168.1.10`)로 `ping`을 날려 네트워크 단절 여부를 확인한다.
   * 실패 시: 물리적 랜선, 스위치 허브 전원, 노트북 USB 랜카드 인식을 점검한다.
2. **포트(502) 상태 확인**: `telnet 192.168.1.10 502` 명령어로 TCP 커넥션이 맺어지는지 확인한다.
   * 거부(Connection Refused) 시: PLC 측 Modbus TCP 서버 설정(XG5000 FEnet 모듈)이 정상인지, 타 클라이언트가 이미 포트를 점유 중인지 확인한다. (일부 PLC는 동시 접속 수 제한이 있음)
3. **재연결 로직 모니터링**: `acquisition` 모듈의 재연결 백오프(Backoff) 로직이 동작하여 5초 단위로 재시도를 수행하는지 로그를 확인한다.

---

## 🚨 Incident 2: 데이터베이스 용량 초과 경고 (DB Disk Space > 85%)

**증상**
* 서버 모니터링 경고: `Disk utilization exceeds 85% on /var/lib/postgresql/data`
* 현상: 새로운 센서 데이터 INSERT 속도 저하 또는 DB 쓰기 실패 예외 발생.

**원인**
* TimescaleDB의 `sensor_data` Hypertable에 데이터가 압축/삭제 없이 장기간 보관되어 스토리지 여유 공간 고갈.

**대응 절차 (Troubleshooting Steps)**
1. **공간 확보 (오래된 Chunk 삭제)**: 중요도가 떨어지는 6개월 이전의 Raw 데이터를 Drop한다.
   ```sql
   SELECT drop_chunks('sensor_data', INTERVAL '6 months');
   ```
2. **압축(Compression) 활성화 확인**: TimescaleDB의 기본 압축 정책이 제대로 돌고 있는지 확인한다.
   ```sql
   SELECT * FROM timescaledb_information.compression_settings;
   ```
3. **근본 대책**: `historian` 모듈에 연속 집계(Continuous Aggregates)를 적용하여 1시간/1일 단위 통계만 남기고 Raw 데이터는 1달 후 자동 삭제되도록 Data Retention Policy를 설정한다.

---

## 🚨 Incident 3: SCADA 서버 메모리 부족 (OOM: OutOfMemoryError)

**증상**
* 에러 로그: `java.lang.OutOfMemoryError: Java heap space` 발생 후 프로세스 종료.

**원인 파악**
* 대량의 알람 발생 시 WebSocket 브로드캐스트 큐(Queue)에 메시지가 적체되거나, HMI에서 너무 넓은 기간(예: 1년치)의 센서 이력을 조회하여 List가 Heap을 과점했을 가능성이 높다.

**대응 절차 (Troubleshooting Steps)**
1. **임시 복구**: 프로세스를 즉시 재시작하여 시스템을 복구한다. (`systemctl restart scada-server`)
2. **Heap Dump 분석**: 서버 재시작 시 자동으로 남겨진 `.hprof` 덤프 파일을 Eclipse MAT 또는 IntelliJ Profiler로 분석하여 메모리 릭(Memory Leak) 지점을 찾는다.
3. **쿼리 제한**: `api` 모듈의 이력 조회 API에서 단일 쿼리로 가져올 수 있는 최대 데이터 범위를 1주일 등으로 제한(Validation)하는 패치를 배포한다.
