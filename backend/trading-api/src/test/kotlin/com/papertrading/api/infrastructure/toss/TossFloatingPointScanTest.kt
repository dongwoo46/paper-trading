package com.papertrading.api.infrastructure.toss

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

// 토스 전송 코드와 브로커 잔고 포트·결과·토스 유스케이스에 부동소수점이 들어오지 않는지 소스를 결정적으로 훑음 (RULE-09, D13)
class TossFloatingPointScanTest {

    private val apiRoot: Path = Paths.get("src/main/kotlin/com/papertrading/api")
    private val sourceRoot: Path = apiRoot.resolve("infrastructure/toss")
    private val forbidden = Regex(
        """\b(Double|Float|toDouble|toFloat)\b|\d\.\d+[fF]\b|\bMap<String,\s*Any\??>|\bJsonNode\b""",
    )

    private fun walkKotlin(root: Path): List<Path> = Files.walk(root).use { stream ->
        stream.filter { it.toString().endsWith(".kt") }.toList()
    }

    // 폴더 바로 아래의 Broker 로 시작하는 파일만 (application/account/port/Broker*, application/account/result/Broker*)
    private fun brokerFiles(relativeDir: String): List<Path> = Files.list(apiRoot.resolve(relativeDir)).use { stream ->
        stream.filter { it.fileName.toString().startsWith("Broker") && it.toString().endsWith(".kt") }.toList()
    }

    // 컨트롤러 폴더 바로 아래의 Toss 로 시작하는 파일만 (presentation/controller/Toss*)
    private fun tossControllerFiles(): List<Path> {
        val controllerDir = apiRoot.resolve("presentation/controller")
        return Files.list(controllerDir).use { stream ->
            stream.filter { it.fileName.toString().startsWith("Toss") && it.toString().endsWith(".kt") }.toList()
        }
    }

    private fun kotlinFiles(): List<Path> {
        val groups = listOf(
            walkKotlin(sourceRoot),
            brokerFiles("application/account/port"),
            brokerFiles("application/account/result"),
            walkKotlin(apiRoot.resolve("application/toss")),
            // step 7: 시세 응답 DTO 와 컨트롤러도 대상, 가격·수량은 BigDecimal 에서 JSON 문자열로만 나감
            walkKotlin(apiRoot.resolve("presentation/dto/toss")),
            tossControllerFiles(),
            // 2단계 §10.6: 공통 주문 포트·접수기·KIS 주문 어댑터 (step 14 QA Q-03)
            walkKotlin(apiRoot.resolve("application/order/port")),
            listOf(apiRoot.resolve("application/order/BrokerOrderSubmitter.kt")),
            listOf(apiRoot.resolve("infrastructure/kis/KisOrderAdapter.kt")),
        )
        groups.forEachIndexed { index, files -> assertTrue(files.isNotEmpty(), "검사 대상 묶음 $index 이 비어 있음") }
        return groups.flatten()
    }

    // 주석 안의 설명 글자는 검사 대상이 아님, 블록 주석과 줄 주석을 뺌
    private fun codeOnly(source: String): String = source
        .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
        .lines()
        .joinToString("\n") { it.substringBefore("//") }

    @Test
    fun `토스 전송 코드와 브로커 잔고 포트 결과 토스 유스케이스에 Double Float toDouble toFloat Map JsonNode 가 없다`() {
        val files = kotlinFiles()
        assertTrue(files.size >= 8, "검사 대상이 너무 적음: ${files.size}")

        val hits = files.flatMap { file ->
            codeOnly(Files.readString(file)).lines().withIndex()
                .filter { (_, line) -> forbidden.containsMatchIn(line) }
                .map { (index, line) -> "${file.fileName}:${index + 1} ${line.trim()}" }
        }

        assertEquals(emptyList<String>(), hits)
    }

    @Test
    fun `검사 정규식은 금지 표기를 실제로 잡는다`() {
        val samples = listOf(
            "val a: Double",
            "x.toDouble()",
            "val f: Float = 1.5f",
            "val m: Map<String, Any>",
            "val n: JsonNode",
        )
        samples.forEach { assertTrue(forbidden.containsMatchIn(it), it) }
        assertTrue(!forbidden.containsMatchIn("val price: BigDecimal"))
    }
}
