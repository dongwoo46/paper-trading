# Step 1: Feature Spec and Component Design
Assigned agent: Service Planner

## Working Directory
.worktrees/front-order-management-ui

## Context
Main repo root: C:\Users\dw\Desktop\paper-trading

## Files to Read
- CLAUDE.md (main repo root)
- docs/ADR.md (if exists)
- docs/PRD.md (if exists)
- frontend/trading-web/src structure (explore to understand existing patterns)
- backend/trading-api graph or key files to understand available APIs (orders, executions)

## Tasks
1. Analyze existing frontend architecture (routing, state management, component patterns).
2. Identify all trading-api endpoints relevant to order-management:
   - POST /orders (주문 생성: 종목 검색, 수량/가격 입력, 매수/매도, 모드 선택)
   - GET /orders (주문 내역: 상태별 필터)
   - DELETE /orders/{id} (주문 취소 버튼)
   - Any related endpoints
3. Design component tree for order-management page:
   - 주문 생성 폼 (종목 검색, 수량/가격 입력, 매수/매도 선택, LOCAL/KIS 모드)
   - 주문 내역 테이블 (상태 필터, 취소 버튼)
4. Define TypeScript interfaces for API request/response types.
5. Write spec.md.
6. Generate step-2.md ~ step-5.md with concrete implementation directives.
   - Explicitly list worktree path (.worktrees/front-order-management-ui) in each step.
   - Explicitly list all required file paths in each step's "Files to Read" section.

## Acceptance Criteria
- spec.md created with component tree, API mapping, TypeScript interfaces.
- step-2.md ~ step-5.md created.
- Each step file includes the "## Agent Return Protocol" section.
- User approval received.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences>
- Files modified: <list relative to main repo root>
- Blockers: <none | description>
---
