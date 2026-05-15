# ADR 0001: 멀티 모듈 및 Spring Modulith 도입 결정

**Date**: 2026-05-13  
**Status**: Accepted  

## 1. Context (배경)
스마트 항온항습 SCADA 시스템은 실시간 데이터 수집(Acquisition), 알람 판정(Alarm), 이력 관리(Historian), 그리고 클라이언트 API 제공(API) 등 성격이 매우 다른 여러 도메인 책임을 동시에 수행해야 한다. 
단일 프로젝트(Monolith)에 모든 패키지를 섞어 넣을 경우, 코드베이스가 커짐에 따라 객체 간 강결합(Spaghetti Code)이 발생하여 유지보수성이 급격히 하락할 위험이 있다.

## 2. Decision (의사 결정)
우리는 프로젝트 아키텍처를 분리하기 위해 **물리적 Gradle 멀티 모듈**과 **Spring Modulith**를 결합하여 도입하기로 결정했다.

1. **Gradle Multi-Module (물리적 분리)**
   * `core`, `api`, `acquisition`, `historian`, `alarm`, `websocket`, `security`로 폴더와 `build.gradle.kts`를 물리적으로 나눈다.
   * 이를 통해 한 모듈이 다른 모듈의 클래스를 무분별하게 `import`하는 것을 컴파일 단계에서 원천 차단한다.
2. **Spring Modulith (논리적 보완 및 이벤트 기반 아키텍처 강제)**
   * 모듈 간 통신은 직접 메서드 호출(Direct Method Call)이 아닌 Spring의 `@ApplicationModuleListener`를 이용한 이벤트 기반 통신으로 구현한다.
   * `Spring Modulith`의 테스트 기능을 활용해 아키텍처 규칙(예: `historian`이 `api`를 의존하지 않음)이 지켜지고 있는지 빌드 타임에 검증한다.

## 3. Consequences (결과 및 영향)
* **장점 (Pros)**
  * 각 도메인(수집, 저장, 알람)이 완벽히 분리되어, 한 모듈의 코드를 수정해도 다른 모듈에 미치는 사이드 이펙트가 최소화된다.
  * 향후 시스템 부하가 커져 특정 모듈(예: `acquisition`)만 독립된 마이크로서비스(MSA)로 분리해야 할 때 코드를 거의 수정하지 않고 즉시 분리할 수 있다.
  * 이벤트 기반 통신을 통해 비동기 처리(Async)를 손쉽게 적용할 수 있다.
* **단점/주의사항 (Cons)**
  * 초기에 각 모듈을 세팅하고 이벤트 객체(DTO)를 매번 만들어야 하므로 보일러플레이트(Boilerplate) 코드가 증가한다.
  * 이벤트 기반의 비동기 흐름은 코드를 순차적으로 따라가기 어려워 디버깅 난이도가 올라간다. (로그 추적을 위해 Trace ID 도입을 고려해야 함).
