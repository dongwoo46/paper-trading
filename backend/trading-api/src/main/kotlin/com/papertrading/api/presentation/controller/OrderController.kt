package com.papertrading.api.presentation.controller

import com.papertrading.api.application.order.OrderCommandService
import com.papertrading.api.application.order.OrderQueryService
import com.papertrading.api.application.order.command.CancelOrderCommand
import com.papertrading.api.application.order.command.PlaceOrderCommand
import com.papertrading.api.presentation.dto.order.ExecutionResponse
import com.papertrading.api.presentation.dto.order.OrderResponse
import com.papertrading.api.presentation.dto.order.PlaceOrderRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/orders")
class OrderController(
    private val orderCommandService: OrderCommandService,
    private val orderQueryService: OrderQueryService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun placeOrder(
        @PathVariable accountId: Long,
        @Valid @RequestBody request: PlaceOrderRequest,
    ): OrderResponse {
        val order = orderCommandService.placeOrder(
            accountId,
            PlaceOrderCommand(
                ticker = request.ticker.trim().uppercase(),
                marketType = request.marketType,
                orderType = request.orderType,
                orderSide = request.orderSide,
                orderCondition = request.orderCondition,
                quantity = request.quantity,
                limitPrice = request.limitPrice,
                expireAt = request.expireAt,
                idempotencyKey = request.idempotencyKey,
                strategyId = request.strategyId,
                signalId = request.signalId,
            )
        )
        return OrderResponse.from(order)
    }

    @GetMapping
    fun listOrders(@PathVariable accountId: Long): List<OrderResponse> =
        orderQueryService.listOrders(accountId).map { OrderResponse.from(it) }

    @GetMapping("/{orderId}")
    fun getOrder(
        @PathVariable accountId: Long,
        @PathVariable orderId: Long,
    ): OrderResponse = OrderResponse.from(orderQueryService.getOrder(accountId, orderId))

    @DeleteMapping("/{orderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun cancelOrder(
        @PathVariable accountId: Long,
        @PathVariable orderId: Long,
    ) {
        orderCommandService.cancelOrder(accountId, orderId, CancelOrderCommand())
    }

    @GetMapping("/{orderId}/executions")
    fun listExecutions(
        @PathVariable accountId: Long,
        @PathVariable orderId: Long,
    ): List<ExecutionResponse> =
        orderQueryService.listExecutions(accountId, orderId).map { ExecutionResponse.from(it) }
}
