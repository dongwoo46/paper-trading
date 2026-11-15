package com.papertrading.api.common

private const val VISIBLE_TAIL_LENGTH = 4
private const val MASK = "****"

/**
 * 계좌 식별값을 로그·toString 에 남길 때 쓰는 유일한 마스킹 (RULE-05), 끝 4자리만 보이고 앞은 `****`
 * 4자리 이하는 일부라도 보이면 값 전체가 드러나므로 전부 가림, null 은 «없음»이라 none
 * 스프링과 계층에 의존하지 않아 모든 계층이 import 함
 */
fun maskAllButLastFour(value: String?): String = when {
    value == null -> "none"
    value.length <= VISIBLE_TAIL_LENGTH -> MASK
    else -> MASK + value.takeLast(VISIBLE_TAIL_LENGTH)
}
