# Contributing to SCADA Server

이 가이드라인은 "스마트 항온항습 SCADA 서버" 프로젝트에 참여하는 모든 개발자가 지켜야 할 협업 규칙을 정의합니다. 일관된 코드 품질과 효율적인 협업을 위해 아래 규칙을 반드시 준수해 주시기 바랍니다.

## 1. Branch Strategy (브랜치 전략)

이 프로젝트는 빠르고 유연한 배포를 위해 **GitHub Flow** 기반의 간소화된 브랜치 전략을 사용합니다.

* **`main`**: 언제든지 Production에 배포 가능한 상태를 유지하는 메인 브랜치입니다. 직접적인 Commit은 엄격히 금지됩니다.
* **Feature Branch**: 새로운 기능 개발이나 버그 수정을 진행하는 브랜치입니다. 반드시 `main` 브랜치에서 파생되어야 합니다.

**브랜치 네이밍 컨벤션:**
* `feature/기능명` (예: `feature/modbus-polling`)
* `bugfix/버그명` (예: `bugfix/alarm-delay-error`)
* `refactor/리팩토링대상` (예: `refactor/event-dto`)

## 2. Commit Message Convention (커밋 메시지 규약)

Git 커밋 히스토리를 깔끔하게 관리하고 자동화된 릴리즈 노트를 생성하기 위해 **Conventional Commits** 표준을 사용합니다.

### 2.1. 포맷
```text
<type>(<scope>): <subject>

<body> (선택 사항)

<footer> (선택 사항 - 관련 이슈 번호 등)
```

### 2.2. Type 종류
* `feat`: 새로운 기능 추가
* `fix`: 버그 수정
* `docs`: 문서 수정 (README, 주석 등)
* `style`: 코드 포맷팅, 세미콜론 누락, 코드 변경이 없는 경우
* `refactor`: 코드 리팩토링 (기능 변화 없음)
* `test`: 테스트 코드 추가/수정
* `chore`: 빌드 업무 수정, 패키지 매니저 설정 등 (프로덕션 코드 변경 없음)

**예시**:
> `feat(acquisition): add Modbus TCP reconnection logic`
> `fix(alarm): resolve threshold hysteresis calculation error`

## 3. Pull Request (PR) 프로세스

작업이 완료되면 `main` 브랜치로 병합하기 위해 PR을 생성합니다.

1. **Self-Review**: PR을 생성하기 전 스스로 코드를 한 번 리뷰하고 불필요한 주석이나 콘솔 로그(`System.out.println`)를 제거합니다.
2. **PR Template**: 제공된 PR 템플릿의 항목(작업 내용, 테스트 여부 등)을 성실히 작성합니다.
3. **Reviewers**: 최소 1명 이상의 동료에게 코드 리뷰를 요청합니다.
4. **Merge**: 모든 리뷰어의 `Approve`를 받은 후, CI/CD 파이프라인(빌드/테스트)이 통과하면 Merge합니다. (Squash and Merge 권장)

## 4. Code of Conduct (행동 강령)
우리는 개방적이고 환영받는 커뮤니티를 지향합니다. 코드 리뷰 시 공격적인 언행을 삼가고, 사람(Who)이 아닌 코드(What) 자체에 집중하여 건설적인 피드백을 제공해 주시기 바랍니다.
