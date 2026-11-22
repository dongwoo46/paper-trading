# Workflow — 작은 단위 작업과 완료 기준 (paper-trading)
<!-- devkit:pointer
path: ^(backend|frontend|scripts)/
command: ^git\s+add\s+(-A|--all)\b
why: 코드를 바꾸고 시험·커밋하는 작업 흐름과 완료 판정
-->
<!-- devkit:scope narrows WF1, CS15 -->
<!-- devkit:form v2 -->

이 파일은 코드를 바꾸고 커밋하는 단위와 완료 판정을 정한다. 관계없는 파일이 커밋에 섞이거나 검증 없이 완료로 넘어가는 것을 막는다.

## 조항

<!-- gt:PTW1 -->
- 커밋할 파일은 하나씩 이름으로 stage 하고 `git add -A` 를 쓰지 않는다.
  Why: 한꺼번에 stage 하면 다른 세션·도구가 남긴 파일(`.DS_Store`, findings 조각, 남의 미커밋 작업)이 커밋에 섞인다(devkit git-conventions «관련 파일만 stage»). — enforce: review
<!-- gt:PTW2 -->
- 작은 단위로 구현 → 시험 → 린트 → 커밋을 되풀이한다.
  Why: 커밋 하나에 논리 변경 하나여야 사람이 읽고 승인할 수 있고, 실패한 단계를 그 단위로 되돌릴 수 있다(devkit git-conventions). — enforce: review
<!-- gt:PTW3 -->
- 빌드·시험·린트와 포맷이 컴파일·import 오류 없이 모두 통과하고, 바꾼 파일에 풀리지 않은 TODO·FIXME 가 없어야 완료다.
  Why: devkit WF1(검증 증거 없이 완료를 선언하지 않음)의 이 레포 판정이다. 바꾼 파일의 TODO·FIXME 는 devkit CS15(링크 단 TODO 허용)를 좁혀 남기지 않는다. — enforce: review
