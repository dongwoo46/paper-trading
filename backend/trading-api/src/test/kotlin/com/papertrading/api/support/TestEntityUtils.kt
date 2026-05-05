package com.papertrading.api.support

fun <T : Any> T.withId(id: Long): T = apply {
    javaClass.getDeclaredField("id").also {
        it.isAccessible = true
        it.set(this, id)
    }
}