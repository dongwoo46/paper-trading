package com.papertrading.api.characterization.kis

import com.papertrading.api.infrastructure.kis.KisExecutionWebSocketConnectionRequest
import com.papertrading.api.infrastructure.kis.KisExecutionWebSocketTransport
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.util.concurrent.CopyOnWriteArrayList

// 실제 웹소켓 연결 대신 받은 연결 요청만 쌓는 대역: 시험이 요청의 onMessage 에 통지 원문을 넣어 전송 경계를 흉내 냄
class KisFakeExecutionWebSocketTransport : KisExecutionWebSocketTransport {
    private val requests = CopyOnWriteArrayList<KisExecutionWebSocketConnectionRequest>()

    override fun connect(request: KisExecutionWebSocketConnectionRequest) {
        requests.add(request)
    }

    fun lastRequest(mode: String): KisExecutionWebSocketConnectionRequest {
        val request = requests.lastOrNull { it.mode == mode }
        return requireNotNull(request) { "no websocket connect request for mode=$mode" }
    }
}

@TestConfiguration
class KisFakeExecutionWebSocketTransportConfig {
    @Bean
    @Primary
    fun kisFakeExecutionWebSocketTransport() = KisFakeExecutionWebSocketTransport()
}
