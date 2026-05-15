# SCADA Security Module (`security`)

이 모듈은 SCADA 서버의 모든 API 접근을 보호하고, 사용자 인증(Authentication) 및 인가(Authorization) 처리를 담당합니다.

## 1. 주요 책임 (Responsibilities)

- Spring Security를 기반으로 JWT(JSON Web Token) 발급 및 검증 로직 구현.
- 운전원(Operator)과 관리자(Admin)의 권한 분리 및 API 엔드포인트별 접근 제어.
- HMI 클라이언트에서 넘어오는 WebSocket(STOMP) 연결 요청에 대한 토큰 기반 인증 처리.
- CORS 및 CSRF 정책 관리.

## 2. 모듈 의존성 (Dependencies)

* `core` (에러 처리, 공통 예외)
* *Spring Security*
* *io.jsonwebtoken (jjwt)*

## 3. 인증 및 권한 정책 (Auth Policies)

### 3.1. 인증 방식 (JWT)
산업용 모니터링 시스템(HMI)은 브라우저 뿐만 아니라 데스크톱 앱(WPF)에서도 접속하므로, 쿠키 기반 세션보다는 **Stateless한 JWT 방식**을 채택합니다.
- `Access Token`: 유효기간 1시간 (만료 시 401 반환)
- `Refresh Token`: 유효기간 7일 (만료 전 재발급 API 호출 필요)

### 3.2. 사용자 권한 등급 (Role Matrix)

시스템 보안을 위해 두 가지 Role로 분리하여 운영합니다.

| Role | 대상 | 허용되는 작업 | 접근 불가 작업 |
|---|---|---|---|
| `ROLE_OPERATOR` | 현장 운전원 | 실시간 모니터링 시청, 알람 인지(Ack) 처리, 장비 운전(수동 모드 기동/정지) | 타 계정 권한 부여, 온도/습도 등 목표 설정값(SP) 변경 불가 |
| `ROLE_ADMIN` | 시스템 관리자 | OPERATOR의 모든 권한 | 온도/습도 설정값(SP) 변경, 알람 임계값 룰 변경, 사용자 계정 생성/삭제 |

> **설정값(SP) 변경 보안 로직**: HMI에서 설정값을 변경할 때는 반드시 관리자의 권한이 필요합니다. WPF 화면에서 설정 버튼 클릭 시 권한이 없으면 비밀번호 확인 다이얼로그를 띄워 임시로 관리자 토큰을 발급받아 요청해야 합니다.

## 4. 데이터베이스 연동

사용자 정보와 권한 해시 암호화(Bcrypt)된 비밀번호는 PostgreSQL의 `users`와 `user_roles` 테이블에 저장되어 관리됩니다. 로그인 시 해당 테이블을 조회하여 UserDetails 객체를 생성합니다.
