package com.papertrading.api.architecture

import com.papertrading.api.application.archfixture.AfterCommitAsyncListenerFixture
import com.papertrading.api.application.archfixture.AfterRollbackAsyncListenerFixture
import com.papertrading.api.application.archfixture.BeforeCommitAsyncListenerFixture
import com.papertrading.api.application.archfixture.ClassLevelAsyncFixture
import com.papertrading.api.application.archfixture.DefaultPhaseAsyncListenerFixture
import com.papertrading.api.application.archfixture.PlainAsyncMethodFixture
import com.papertrading.api.application.archfixture.ScheduledMethodFixture
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** PTB9 application 의 @Async 허용 범위(AFTER_COMMIT 리스너 메서드만)를 픽스처 클래스로 시험, 본 규칙 시험은 테스트 클래스를 제외해서 따로 둔다 */
class ApplicationLayerRulesTest {
    private fun import(vararg types: Class<*>): JavaClasses = ClassFileImporter().importClasses(*types)

    @Test
    fun `AFTER_COMMIT 리스너의 Async 는 Spring 타입 허용 목록을 통과한다`() {
        applicationUsesOnlyAllowedSpringTypesRule().check(import(AfterCommitAsyncListenerFixture::class.java))
    }

    @Test
    fun `Async 외 scheduling 타입은 계속 막는다`() {
        assertThrows<AssertionError> {
            applicationUsesOnlyAllowedSpringTypesRule().check(import(ScheduledMethodFixture::class.java))
        }
    }

    @Test
    fun `AFTER_COMMIT 리스너 메서드의 Async 는 통과한다`() {
        asyncOnlyOnAfterCommitListenerMethodsRule().check(
            import(AfterCommitAsyncListenerFixture::class.java, DefaultPhaseAsyncListenerFixture::class.java),
        )
    }

    @Test
    fun `AFTER_COMMIT 이 아닌 리스너의 Async 는 막는다`() {
        assertThrows<AssertionError> {
            asyncOnlyOnAfterCommitListenerMethodsRule().check(import(BeforeCommitAsyncListenerFixture::class.java))
        }
        assertThrows<AssertionError> {
            asyncOnlyOnAfterCommitListenerMethodsRule().check(import(AfterRollbackAsyncListenerFixture::class.java))
        }
    }

    @Test
    fun `리스너가 아닌 메서드의 Async 는 막는다`() {
        assertThrows<AssertionError> {
            asyncOnlyOnAfterCommitListenerMethodsRule().check(import(PlainAsyncMethodFixture::class.java))
        }
    }

    @Test
    fun `클래스 수준 Async 는 막는다`() {
        assertThrows<AssertionError> {
            applicationClassesAreNotAsyncRule().check(import(ClassLevelAsyncFixture::class.java))
        }
    }
}
