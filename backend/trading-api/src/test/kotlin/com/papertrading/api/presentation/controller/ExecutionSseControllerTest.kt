package com.papertrading.api.presentation.controller

import com.papertrading.api.infrastructure.sse.ExecutionSseRegistry
import io.mockk.justRun
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@WebMvcTest(ExecutionSseController::class)
class ExecutionSseControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @TestConfiguration
    class TestConfig {
        @Bean
        fun executionSseRegistry(): ExecutionSseRegistry {
            val registry = mockk<ExecutionSseRegistry>()
            justRun { registry.register(any(), any<SseEmitter>()) }
            return registry
        }
    }

    @Test
    fun `GET stream returns 200 with text event-stream content type`() {
        mockMvc.perform(get("/api/v1/executions/stream"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
    }
}
