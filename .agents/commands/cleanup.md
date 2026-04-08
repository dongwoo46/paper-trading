cleanup — 브랜치 정리

실행 순서
1. git status, git log --oneline -10, git diff main...HEAD 확인
2. WIP 커밋 있으면 squash 여부 사용자 확인
3. PR 초안 작성 후 사용자 확인 대기
4. 확인 후 gh pr create 실행

PR 형식
제목: [타입]: [변경 요약]
본문: Summary(변경 내용) / Test plan(체크리스트)
