# 코드 리뷰 2회차 (재리뷰) — trading-web-shadcn-rebuild

- 범위: `52331d6..e8bacfe` (고침 커밋 `e8bacfe` 하나). 앞 회차 입력: `review/panel.md`
- 프로필: consumer (rule-pointer `--list`). 규칙 목록: `rules/dev-team/tests.md` 외 `--list` 반환분
- `fix-round-brief check`: 실행 못 함 — `review/` 에 `*.brief.json` 이 없습니다. 읽기 범위는 고침 커밋이 바꾼 파일 5개로 잡았습니다.
- 범위 밖으로 연 파일(사유 한 줄씩)
  - `.github/workflows/full-test.yml`: CI 에서 시험이 backend 소스를 읽을 수 있는지 확인(요청 항목)
  - `main:frontend/trading-web/vite.config.ts`: P9 의 비교 기준인 옛 프록시 표 대조
  - `docker-compose.yml`: 옛 표의 `/api-ai`·`/api-collector`·`/api-research` 가 같은 종류 누락인지 판단
  - `backend/{trading-api,collector-api}/src/main/**/*.kt`: 새 시험의 입력. 독립 스캔으로만 읽음

## 지난 막음 행 판정

| 지난 ID | 판정 | 근거 |
|---|---|---|
| P9 | 고침 | 1. `proxy.config.ts:26-29,44` 에 접두사 5개 추가. 2. 독립 스캔(시험 코드와 별개 스크립트, Vite 실제 매칭 = 표 삽입 순서대로 첫 `startsWith`): trading-api 컨트롤러 12개·collector-api 19개의 모든 경로가 자기 서비스로 감, 잘못 가는 경로 0. `.java` 0개, 줄 중간 매핑 애너테이션 0, 경로 여러 개 애너테이션 0, context-path·addPathPrefix·함수형 라우터·WebSocket 엔드포인트 등록 0. 3. 격리 사본에서 `vitest run src/test/proxy-table.test.ts` 9/9 통과. 4. `test-suite-audit guards-fix --base e8bacfe~1 --head e8bacfe`: 잡음(caught) — `proxy.config.ts` 를 되돌리면 새 시험 2개가 단언 실패로 빨개짐(로드 실패 아님) |

같은 종류(TT54) 잔여: P9 범위에선 없습니다. trading-api·collector-api 는 컨트롤러 소스 대조로 바뀌었고, quant-worker 는 목록이 아니라 접두사 하나(`/api-quant`)라 해당이 없습니다. 옛 표의 `/api-ai`(8083)·`/api-collector`(8082)·`/api-research`(8084) 가 빠진 건 D10 결정문에 세 서비스만 적혀 있고 `docker-compose.yml` 에도 세 서비스뿐이라 누락이 아니라 설계 결정으로 봤습니다.

## 고침이 만든 문제

| ID | file:line | 근거 조항 | 무엇이 틀렸나 | 등급 | 출처 | 회귀 |
|---|---|---|---|---|---|---|
| R2-01 | frontend/trading-web/src/test/proxy-table.test.ts:119 | TT54 | `covers()` 는 `/` 경계로 맞추지만 Vite 는 표 삽입 순서(trading 먼저)로 경계 없는 `startsWith` 첫 일치를 씁니다. 예: collector 에 `/api/kis/accounts-x` 가 생기면 Vite 는 trading `/api/kis/account` 로 보내는데 시험은 «겹침 없음»으로 통과합니다. 기존 겹침 시험(57행)도 완전 일치만 봐서 못 잡습니다. 시험의 판정 모형이 실제 라우터와 다름. 지금 소스엔 해당 경로 0 | should-fix | 이번 수정 | 아니오 |
| R2-02 | frontend/trading-web/src/test/proxy-table.test.ts:97 | TT54 | 매핑 읽기가 조용히 놓치는 모양 두 가지: 1. `@PreAuthorize(..) @GetMapping("/x")` 처럼 줄 중간 애너테이션은 `annotations`·`looseCount` 둘 다 안 세어 대조가 통과함. 2. `@GetMapping("/a", "/b")`(Kotlin 가변 인자)는 109행이 첫 경로만 읽음. 클래스 경로가 `/api` 처럼 넓은 컨트롤러에서 빠진 경로가 새어 나갑니다. 지금 소스엔 두 모양 모두 0 | nit | 이번 수정 | 아니오 |

## 1. 규칙 준수
- TT54: P9 의 «표끼리만 대조»는 해소. 남은 건 위 R2-01·R2-02(판정 모형·읽기 범위의 빈틈, 현재 미발현).
- TT(시험 먼저): 작성자 증거 Red 2 → Green 9 를 guards-fix 로 독립 확인했습니다.

## 2. 부작용
- 프런트 시험이 backend 소스에 묶입니다. backend 에 컨트롤러 경로를 더하면 프런트 CI 가 빨개지는데, README·D10 에 적힌 의도된 결합이라 결함으로 보지 않았습니다.
- CI: `full-test.yml` 의 trading-web 잡은 `actions/checkout@v4` 기본값(저장소 전체, sparse 아님)이고 트리거는 `[pull_request, push]`(경로 필터 없음)라 backend 경로를 읽을 수 있고 backend 만 바뀐 PR 에서도 돕니다. Node 20 에서 `import.meta.dirname` 은 Vite 모듈 러너가 채움(로컬 vitest 4.1.12 통과). CI 실측은 네트워크 차단으로 확인 불가.

## 3. 버그·장애 위험
해당 없음 — 이번 고침은 개발 서버 프록시 표와 시험만 바꿨고 런타임 코드 경로는 그대로입니다.

## 병합 판정
병합 가능 — P9 고침, 이번 수정 출처 blocker 0, 회귀 예 0. R2-01·R2-02 는 후속 후보(이번 회차 고침 요청에 넣지 않음).

<!-- devkit:standard-list hex:7b22636861696e223a22646f63732f66656174757265732f72756e6e696e672f74726164696e672d7765622d73686164636e2d72656275696c642f7265766965772f70616e656c2e6d64222c22726f756e6473223a7b2231223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2235323333316436353138363434386534386137653163633433343939613432663434333566336430227d2c2232223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2265656436343866383335383132383734393536366462623135366664636164333264386561666565227d7d7d -->
