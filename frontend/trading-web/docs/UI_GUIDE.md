# UI_GUIDE — trading-web

## 색상 팔레트

| 용도 | 값 |
| --- | --- |
| 배경 (메인) | `#06070a` |
| 배경 (사이드바) | `#0a0c12` |
| 텍스트 (기본) | `#ffffff` |
| 텍스트 (보조) | `#cbd5e1` |
| 텍스트 (흐림) | `#94a3b8` |
| 브랜드 블루 | `#3b82f6` |
| 브랜드 그린 | `#10b981` |

그라디언트(`--grad-primary`)는 강조 포인트에만, 배경 전체 사용 금지.

## 레이아웃
- `.feature-grid` / `.card-grid` 로 그리드 구성
- 1024px 미만: 세로 스택
- 테이블은 `.scroll-container` 래핑 (overflow 방지)

## 인터랙션
- 클릭 가능한 카드/버튼: `:hover { transform: translateY(-2px) }`
- 페이지/패널 진입: `.fade-in` 애니메이션

## AI 슬롭 안티패턴 (금지)
- Glass morphism 전면 남용 (카드 전체에 blur 도배)
- 보라색 그라디언트 텍스트 (`background-clip: text`)
- 네온 글로우 (`box-shadow: 0 0 20px`)
- 무의미한 애니메이션 (로딩 스피너 과다, 파티클 효과)
- 다크 배경에 낮은 대비 텍스트 (`#64748b` 이하 단독 사용)
