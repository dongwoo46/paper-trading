# 레포 규칙 4차 추가 — 변경 증거 (2026-11-15)

대상: `rules/backend-api.md` 새 절 «계층과 포트 (trading-api)»(PTB7~PTB9)와 포인터 signal 확장. 조항 추가만이고 지운 줄은 0이다. 팀 규칙 모순 대조 표는 1차 증거 문서(`2026-11-15-repo-rules-spring-ddd.md`)의 4차 행에 있다.

사람 결정(2026-11-15 팝업): 클린 아키텍처(포트·어댑터), 출력 포트는 `application/<컨텍스트>/port`, application 의 Spring 은 `@Service`·`@Transactional` 만 허용.

실측 근거(2026-11-15, origin/main e2904c8, `backend/trading-api/src/main/kotlin/com/papertrading/api` 아래 `^import` 줄 집계): application→infrastructure 28파일, domain→jakarta.persistence 28파일, domain→application 1파일, infrastructure→presentation 2파일, presentation→infrastructure 1파일. 포트는 `domain/port` 5개, `application/*/port` 3개.

## 공식 근거 대조

근거 문서: devkit `docs/research/ai-doc-official-guidance.md` §1~§4.

- 이유(rationale) 보존: 새 조항 3개 모두 `Why:` 줄을 두고, 각 Why 에 실패 메커니즘(판정 기준 부재, 단위 시험이 DB 없이 못 돎, 영속 예외가 업무 분기로 샘)과 사람 결정 날짜를 적었다.
- 강조 서식: 굵게, IMPORTANT, CRITICAL 을 쓰지 않았다. 평문 명령형 한 줄이다.
- 골격(헤딩·불릿): 관심사 헤딩 «계층과 포트 (trading-api)» 아래 `gt:` 앵커와 불릿 한 줄 조항으로 썼다(form v2). 금지 방향은 열거로 적어 판정 가능하게 했다.
- 분량 판정: «의존은 안쪽으로» 같은 원칙 설명은 팀 규칙에 있어 다시 쓰지 않고, 이 레포 패키지 이름과 허용 목록처럼 모델이 코드만 보고는 알 수 없는 값만 넣었다. 강제는 ArchUnit 구조 시험(별도 PR)이 맡는다.
