package com.papertrading.api.infrastructure.toss

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

// 토스 전송 코드에 부동소수점이 들어오지 않는지 소스를 결정적으로 훑음 (RULE-09, D13)
class TossFloatingPointScanTest {

    private val sourceRoot: Path = Paths.get("src/main/kotlin/com/papertrading/api/infrastructure/toss")
    private val forbidden = Regex(
        """\b(Double|Float|toDouble|toFloat)\b|\d\.\d+[fF]\b|\bMap<String,\s*Any\??>|\bJsonNode\b""",
    )

    private fun kotlinFiles(): List<Path> = Files.walk(sourceRoot).use { stream ->
        stream.filter { it.toString().endsWith(".kt") }.toList()
    }

    // 주석 안의 설명 글자는 검사 대상이 아님, 블록 주석과 줄 주석을 뺌
    private fun codeOnly(source: String): String = source
        .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
        .lines()
        .joinToString("\n") { it.substringBefore("//") }

    @Test
    fun `토스 전송 코드에 Double Float toDouble toFloat Map JsonNode 가 없다`() {
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
