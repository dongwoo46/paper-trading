# Architecture — trading-web FSD 의존 방향 (paper-trading)
<!-- devkit:pointer
path: ^frontend/trading-web/src/.+\.(ts|tsx)$
why: trading-web 의 pages·features·entities·shared 계층 import
-->
<!-- devkit:scope narrows AR2 -->
<!-- devkit:form v2 -->

이 파일은 trading-web 계층 사이의 import 방향을 정한다. 아래 계층이 위 계층을 불러 계층을 바꿀 때 화면이 함께 깨지는 일을 막는다.

## 조항

<!-- gt:PTR1 -->
- trading-web 은 `pages` → `features` → `entities` → `shared` 방향으로만 import 하고, 아래 계층에서 위 계층을 import 하지 않는다.
  Why: devkit AR2(층 방향은 한쪽으로만)를 이 레포 FSD 계층 이름으로 적은 것이다. 역방향 import 가 하나 생기면 아래 계층을 고칠 때 위 계층이 함께 깨진다. — enforce: review
