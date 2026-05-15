# SCADA Server Development & Setup Guide

이 문서는 프로젝트에 새로 합류한 개발자가 자신의 로컬 PC(노트북)에 개발 환경을 세팅하고 애플리케이션을 구동하기까지의 과정을 설명하는 **Onboarding 매뉴얼**입니다.

## 1. 사전 요구 사항 (Prerequisites)

아래 도구들이 로컬 PC에 설치되어 있어야 합니다.
* **JDK 25**: Eclipse Temurin 또는 Oracle OpenJDK 배포판.
* **Docker Desktop**: 로컬 데이터베이스(TimescaleDB) 실행용.
* **Git**: 소스 코드 버전 관리.
* **IntelliJ IDEA (권장)**: Spring Boot 애플리케이션 개발에 최적화된 IDE.

## 2. 프로젝트 클론 및 빌드

```bash
# 1. 소스 코드 다운로드
git clone https://github.com/hvac-sys/scada-server.git
cd scada-server

# 2. Gradle 빌드 검증 (의존성 다운로드 및 테스트)
./gradlew clean build
```

## 3. 로컬 데이터베이스 환경 세팅 (Docker)

SCADA 서버 구동을 위해서는 반드시 TimescaleDB가 실행 중이어야 합니다. 프로젝트 루트에 제공된 `docker-compose.yml`을 사용하여 로컬 환경을 세팅합니다.

```bash
# 백그라운드로 TimescaleDB 구동
docker-compose up -d

# 구동 상태 확인
docker ps
```

* **DB 호스트**: `localhost:5432`
* **DB 이름**: `scadadb`
* **사용자명**: `postgres`
* **비밀번호**: (환경 변수 파일 `.env` 또는 yml 참조)

> **팁**: DB 툴(DBeaver, DataGrip)을 사용하여 로컬 컨테이너에 정상적으로 접속되는지 먼저 테스트하는 것을 권장합니다. 데이터베이스 스키마는 서버 기동 시 Flyway(또는 Hibernate ddl-auto)를 통해 자동 생성됩니다.

## 4. 로컬 서버 구동 (Running the Server)

IntelliJ IDEA에서 `scada-api` 모듈 내의 `@SpringBootApplication`이 붙은 메인 클래스를 실행하거나, 터미널에서 다음 명령어를 사용합니다.

```bash
# Spring Boot 서버 실행
./gradlew :api:bootRun
```
* 서버가 정상 구동되면 `http://localhost:8080` 포트로 접근 가능합니다.
* Swagger API 문서: `http://localhost:8080/swagger-ui/index.html`

## 5. PLC 시뮬레이터 연동 (선택 사항)

실제 LS산전 PLC나 장비가 없는 환경(카페, 자택 등)에서 수집 모듈(`acquisition`)을 테스트하려면 로컬에 Modbus TCP 시뮬레이터를 띄워야 합니다.

* **추천 도구**: ModRSsim2 (Windows) 또는 PyModbus 기반 스크립트.
* **설정 방법**: `application-local.yml` 파일의 `scada.modbus.ip` 값을 `127.0.0.1`로 변경 후 서버를 재시작합니다.
