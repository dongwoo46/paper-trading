# 레포 규칙 세 문서 추가 — 변경 증거 (2026-11-15)

대상: `rules/backend-api.md`(PTB1~PTB4), `rules/data-access.md`(PTD1~PTD5), `rules/async-external.md`(PTA1~PTA2). 새 파일 추가만이다.

## 삭제 줄 대조

`rule-change-gate` 판정: 지운 줄 0개(no-net-deletion 통과). 대조할 삭제 줄이 없어 소실 0이다.

## 전후 준수 비교

해당 없음. 지운 줄이 없어 뜻을 잃을 수 없다(rule-change-verify «Additions only = not applicable»). 새 조항이 실제로 지켜지는지는 이후 전수 감사(quality-audit)와 리팩터링 리뷰에서 잰다.

## 팀 규칙과 모순 대조

devkit 0.95.0 `rules/dev-team` 의 원문과 조항마다 대조했다. 판정 도구가 없어 사람이 읽는 대조다.

| 레포 조항 | 맞닿는 팀 조항 | 판정 |
|---|---|---|
| PTB1 도메인/영속 분리 | DD9 | 같은 방향. 초안(엔티티를 애그리거트로 씀)은 DD9 와 반대라 사람 결정으로 분리 쪽으로 고쳤다. 단순 CRUD 예외는 팀 규칙 «No over-engineering» 인용이지 새 예외가 아니다 |
| PTB2 private set | DD10, DD11 | 같은 방향(Kotlin 형태) |
| PTB3 id 참조 | DD12, 제안 중인 DD15(BiocomKR/devkit#2471) | 같은 방향. DD15 머지 시 지운다고 조항에 적음 |
| PTB4 BigDecimal | 루트 CLAUDE.md Financial Safety | 같은 방향 |
| PTD1, PTD2 | DA1, DA9 | 같은 방향 |
| PTD3 | DA10 | 같은 방향 |
| PTD4 리스너 역할 | DA6, DA7 | 초안(커밋 뒤 리스너에서 REQUIRES_NEW 로 쓰기)이 DA6 문장과 반대로 읽혀 고쳤다. 원자적 쓰기는 트랜잭션 안 리스너(DA6), 커밋 뒤 리스너는 DA7 부수효과로 나눔 |
| PTD5 | 해당 팀 조항 없음 | 충돌 없음 |
| PTA1 | LA1 | 같은 방향 |
| PTA2 | 제안 중인 EV2 | 같은 방향 |

## 공식 근거 대조

근거 문서: devkit `docs/research/ai-doc-official-guidance.md` §1~§4, §7.

- 이유(rationale) 보존: 조항 11개 모두 `Why:` 줄을 둔다(파일별 4·5·2개, 조항 수와 같음). §1 「explaining the why behind constraints」와 devkit 실측(근거를 지운 판본 준수 90% → 10%)을 따랐다.
- 강조 서식: 세 파일에 굵게, IMPORTANT, CRITICAL, MUST 가 0개다(grep 확인). §2 「piling on emphasis mostly makes Claude over-apply the rule」에 맞춰 강조 없이 평문 명령형으로 썼다.
- 골격(헤딩·불릿): 파일마다 관심사별 헤딩 아래 `gt:` 앵커와 불릿 한 줄 조항으로 썼다(form v2). §3 「group related instructions under markdown headers and bullets」를 따랐다.
- 분량 판정: 세 파일 23·31·38줄, 합계 92줄로 200줄 상한 아래다. §4 의 「Does Claude really need this explanation?」 판별로, 모델이 이미 아는 일반 설명(트랜잭션 정의 등)은 넣지 않고 컴파일은 통과하지만 조용히 틀리는 함정만 남겼다. 내용을 줄였다기보다 처음부터 함정만 골랐다.
- 모순: §4 「if two instructions contradict each other, Claude may pick one arbitrarily」에 따라 위 표로 팀 규칙과 대조했고, 발견한 2건(PTB1, PTD4)은 고친 뒤 커밋한다.
