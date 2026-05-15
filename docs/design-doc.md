# Design Doc: Smart Climate Control SCADA Server

**Author:** SCADA Backend Team  
**Status:** Approved  
**Last Updated:** 2026-05-13  

## 1. Objective (목표)
스마트 항온항습 자동 제어 시스템의 핵심 백엔드인 SCADA(Supervisory Control and Data Acquisition) 서버를 구축한다. 이 서버는 이기종 PLC(LS산전, 미쓰비시)에서 발생하는 센서 데이터를 중앙 집중식으로 수집하여 영속화하고, 이상 징후를 감지(Alarm)하며, 원격 HMI(WPF) 클라이언트로 실시간 데이터를 배포하는 관제탑 역할을 수행한다.

## 2. Non-Goals (비목표)
이 프로젝트에서 **하지 않을 것**을 명확히 정의한다.
* 현장 설비(인버터, 서보모터)를 직접 하드웨어 I/O 통신으로 제어하지 않는다. (모든 제어는 PLC 래더 로직에 위임하며, SCADA는 PLC로 설정값(SP)만 전달한다.)
* 자체적인 UI/UX 웹 프론트엔드를 개발하지 않는다. (화면 처리는 별도 리포지토리의 C# WPF HMI 클라이언트에서 전담한다.)
* 머신러닝 기반의 이상 탐지나 예측 정비(Predictive Maintenance)는 1차 릴리즈 대상에서 제외한다.

## 3. Background (배경)
기존 시스템은 HMI 터치패널이 PLC와 직접 통신(Modbus)하는 2-Tier 구조였다. 이는 현장 운전원에게는 충분하지만, 장기적인 데이터의 로깅, 다수 클라이언트의 동시 접속, 중앙 알람 통합 관리가 불가능하다. 이를 해결하기 위해 HMI와 PLC 사이에 3-Tier 아키텍처의 미들웨어인 SCADA 서버를 도입한다.

## 4. Architecture (아키텍처)

### 4.1. High-Level Design
시스템은 도메인 주도 설계(DDD)를 따르는 **멀티 모듈 스프링 부트(Spring Boot) 애플리케이션**으로 구성된다.

* **Data Acquisition**: 1초 단위 Modbus TCP 폴링 -> `SensorDataCollectedEvent` 발행.
* **Alarm Engine**: 이벤트 리스닝 -> 임계값/Hysteresis/Delay 평가 -> `AlarmStatusChangedEvent` 발행.
* **Historian**: 이벤트 리스닝 -> TimescaleDB 영속화 (비동기 처리).
* **WebSocket**: 이벤트 리스닝 -> STOMP 브로드캐스트.

### 4.2. Alternatives Considered (대안 평가)

**1) 데이터베이스: MongoDB vs TimescaleDB**
* *대안*: JSON 기반의 유연한 확장을 위해 MongoDB 시계열 컬렉션 고려.
* *결정*: **TimescaleDB (PostgreSQL)** 선택.
* *이유*: 사용자 권한(Spring Security) 등 관계형 메타데이터와 시계열 센서 데이터를 단일 DB 인스턴스에서 조인(JOIN) 쿼리할 수 있어 운영 복잡도가 낮아지며, JPA/Hibernate 생태계를 그대로 활용할 수 있기 때문.

**2) 실시간 통신: gRPC vs WebSocket(STOMP)**
* *대안*: 성능 극대화를 위해 구글의 gRPC 고려.
* *결정*: **WebSocket (STOMP)** 선택.
* *이유*: 초당 1회 수준의 브로드캐스트는 WebSocket으로 충분하며, .NET(WPF) 클라이언트와의 Pub/Sub 연동 시 STOMP 표준을 사용하는 것이 개발 생산성 측면에서 훨씬 유리함.

**3) 마이크로서비스(MSA) vs 멀티 모듈 (Spring Modulith)**
* *대안*: `acquisition`, `historian` 등을 각각 별도의 컨테이너로 분리(MSA).
* *결정*: **Spring Modulith (단일 JVM 내 논리적 격리)** 선택.
* *이유*: 초기 프로젝트 규모 상 분산 트랜잭션과 네트워크 레이턴시 관리 비용이 이점보다 큼. Modulith를 통해 패키지 간 강결합만 막아두고, 향후 트래픽 증가 시 분리하는 것이 합리적임.

## 5. Security (보안)
* **인증**: JWT(JSON Web Token) 기반 Stateless 인증.
* **인가**: Role-Based Access Control (RBAC).
  * `ROLE_OPERATOR`: 조회 및 단순 확인(Ack).
  * `ROLE_ADMIN`: SP(목표 온도/습도) 변경 및 알람 룰 변경 권한.
* **네트워크**: 향후 Production 배포 시 SCADA 서버와 HMI 간 통신은 TLS/SSL(WSS, HTTPS)로 암호화한다.
