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
| PTB5 생성 시 불변식(2차 추가) | DD10, DD13 | 같은 방향(값 객체·애그리거트 생성 방식) |
| PTT1 실제 엔진 시험(2차 추가) | 팀 tests.md | 팀 규칙에 시험 엔진 조항 없음. 단위 시험은 포트 가짜를 쓰도록 예외로 남겨 «가짜는 경계에서만» 원칙과 부딪치지 않음 |
| PTF1 원장 추가 전용(2차 추가) | 팀 규칙에 없음, CLAUDE.md 감사 추적 | 충돌 없음. devkit 제안으로도 올림 |
| PTF2 대사(2차 추가) | 팀 규칙에 없음 | 충돌 없음. devkit 제안으로도 올림 |
| PTF3 불확정 응답(2차 추가) | DA9 | DA9 의 결과 반영 단계를 강화(좁히기). devkit 제안으로도 올림 |

| PTF4 계산 스냅샷(3차) | 팀 규칙에 없음 | 충돌 없음. devkit 제안 #2471 2차에 포함 |
| PTF5 반열림 구간(3차) | 팀 time.md 에 해당 조항 없음 | 충돌 없음(KST naive 원칙과 독립). devkit 제안 #2471 2차에 포함 |
| PTF6 예수금 홀드 요청 경로(3차) | DA10, CS11 | 같은 방향. 큐는 알림에만 써서 CS11(부수효과 통로)과 맞음 |
| PTB6 컬렉션 노출, copy 얕은 복사(3차) | DD10 | 같은 방향(Kotlin 형태) |
| PTB7 계층 지도(4차) | backend-api 잠금 원칙(의존은 안쪽으로), DD14 | 같은 방향. DD14 가 말하는 «계층»을 이 레포 패키지 이름으로 정한 것. «No over-engineering»(계층 수를 복잡도에 맞춤)과는 돈·상태 컨텍스트가 있는 trading-api 에만 적용해 부딪치지 않음 |
| PTB8 출력 포트 위치(4차) | DD9 | 같은 방향(유스케이스가 ORM·외부 SDK 를 직접 참조하지 않게 하는 구조) |
| PTB9 application 의 Spring 허용 범위(4차) | DD9 | 같은 방향. 팀 규칙은 프레임워크를 «바깥 세부»로 두되 금지 목록은 ORM·HTTP·SDK 라, 트랜잭션 어노테이션 허용은 모순 아님(사람 결정) |

2차 추가 근거: 국내 테크 기업 공개 기술 블로그 조사(토스, 우아한형제들, 카카오페이, 강남언니 등). 요약본 기반이라 인용의 글자 대조는 하지 않았다.

## 공식 근거 대조

근거 문서: devkit `docs/research/ai-doc-official-guidance.md` §1~§4, §7.

- 2차 추가(PTB5, PTT1, PTF1~PTF3) 뒤 조항은 16개, 다섯 파일이며 아래 판정은 2차 추가분에도 같게 적용했다(Why 줄 16개, 강조 0, 분량 합계 약 140줄).
- 이유(rationale) 보존: 1차 조항 11개 모두 `Why:` 줄을 둔다(파일별 4·5·2개, 조항 수와 같음). §1 「explaining the why behind constraints」와 devkit 실측(근거를 지운 판본 준수 90% → 10%)을 따랐다.
- 강조 서식: 세 파일에 굵게, IMPORTANT, CRITICAL, MUST 가 0개다(grep 확인). §2 「piling on emphasis mostly makes Claude over-apply the rule」에 맞춰 강조 없이 평문 명령형으로 썼다.
- 골격(헤딩·불릿): 파일마다 관심사별 헤딩 아래 `gt:` 앵커와 불릿 한 줄 조항으로 썼다(form v2). §3 「group related instructions under markdown headers and bullets」를 따랐다.
- 분량 판정: 세 파일 23·31·38줄, 합계 92줄로 200줄 상한 아래다. §4 의 「Does Claude really need this explanation?」 판별로, 모델이 이미 아는 일반 설명(트랜잭션 정의 등)은 넣지 않고 컴파일은 통과하지만 조용히 틀리는 함정만 남겼다. 내용을 줄였다기보다 처음부터 함정만 골랐다.
- 모순: §4 「if two instructions contradict each other, Claude may pick one arbitrarily」에 따라 위 표로 팀 규칙과 대조했고, 발견한 2건(PTB1, PTD4)은 고친 뒤 커밋한다.
