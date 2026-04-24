# Step 1: 기능 명세·API 스펙
담당 에이전트: Service Planner

## 작업 경로
.worktrees/trading-api-position-service

## 읽어야 할 파일
- CLAUDE.md
- docs/ADR.md
- docs/PRD.md
- docs/TODO.md

## 배경 및 요구사항

trading-api의 Position 애플리케이션 서비스 계층을 구현한다.

### 기능 범위 (docs/TODO.md 기준)
- PositionCommandService: 포지션 생성·업데이트·청산
- PositionQueryService: 포지션 조회, 평균단가, 평가손익 계산
- PositionResponseDto + PositionController 응답 연결
- 체결(Execution) 이벤트 → 포지션 자동 업데이트

### 기술 스택
- Kotlin / Spring Boot 3 / JPA / PostgreSQL / Redis
- 아키텍처: presentation → application → domain ← infrastructure
- 금액·수량: BigDecimal만 사용 (double/float 금지)
- DTO ↔ Entity 혼용 금지

### 기존 코드 파악 지시
작업 경로 `.worktrees/trading-api-position-service/backend/trading-api/` 아래에서:
1. 기존 도메인 모델(Position, Order, Execution, Account 등) 파악
2. 기존 서비스 패턴(AccountCommandService 등) 참고
3. Execution → Position 업데이트 연계 지점(QuoteEventListener, 체결 엔진 등) 확인

## 작업
1. 기존 코드 구조 파악 (도메인, 인프라, 기존 서비스 패턴)
2. DDD 모델 확정 (Position Entity/VO, Aggregate 경계, Domain Event)
3. API 스펙 설계 (엔드포인트, Request/Response, 에러 케이스)
4. DB 스키마 설계 (positions 테이블, 인덱스)
5. spec.md 작성 (`.worktrees/trading-api-position-service/docs/phase/trading-api/position-service/spec.md`)
6. step-2.md ~ step-5.md 생성 (구체적 구현 지시, 파일 경로, 클래스 시그니처 포함)

## Acceptance Criteria
- spec.md 생성 완료 (DDD 모델, API 스펙, DB 스키마 포함)
- step-2.md ~ step-5.md 생성 완료
- 각 step 파일에 worktree 경로, 읽어야 할 파일 목록, 구체적 작업 내용 명시
- 사용자 승인 완료