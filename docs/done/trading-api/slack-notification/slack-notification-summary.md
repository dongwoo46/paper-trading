# Slack Notification Phase Summary

## 구현된 기능 요약
- Slack 알림 기능을 이벤트 기반으로 통합하고 운영 API(조회/업데이트/테스트 전송)를 추가했다.
- 초기 구현의 동기 재시도/대기(`Thread.sleep`) 구조를 제거하고 Outbox 기반 비동기 재시도 구조로 재설계했다.
- Delivery 로그를 영속화하여 실패 재시도와 상태 추적을 분리했고, 스케줄러가 PENDING 건을 주기적으로 재처리한다.

## 최종 컴포넌트 목록
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/enums/NotificationEventType.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/enums/DeliveryStatus.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/event/SlackNotificationRequestedEvent.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/NotificationDeliveryLog.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/repository/NotificationDeliveryLogRepository.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/notification/SlackNotificationEventPublisher.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/notification/SlackNotificationService.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/config/AsyncConfig.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/NotificationSender.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationProperties.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackWebhookNotifier.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationEventHandler.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationScheduler.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/SlackNotificationController.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/notification/SlackNotificationDtos.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/exception/GlobalExceptionHandler.kt`

## 주요 설계 결정 및 근거
- `Thread.sleep` 제거: 요청 스레드를 점유하는 대기/재시도는 처리량 저하와 장애 전파 위험이 있어 제거했다.
- Outbox 도입: 알림 전송과 도메인 처리의 결합을 낮추고, 실패 케이스를 별도 저장/재처리하여 신뢰성과 관측 가능성을 높였다.
- 스케줄러 재시도 + 유계 ThreadPool: 무제한 동시성/블로킹 확산을 방지하고 운영 제어 지점을 명확히 했다.

## 알려진 한계 및 추후 개선
- 재시도 백오프/최대 시도 횟수 정책은 단순 주기 기반이므로 지수 백오프 및 DLQ 정책으로 확장 여지가 있다.
- 멱등 키/중복 방지 규칙은 현재 이벤트/로그 설계에 의존하므로 webhook 수신 측 계약 기반 보강이 필요하다.
- 알림 템플릿/라우팅 규칙은 운영 API 확장으로 채널 다변화(예: 다중 webhook, severity별 라우팅) 가능하다.
