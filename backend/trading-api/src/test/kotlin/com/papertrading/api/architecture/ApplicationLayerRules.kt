package com.papertrading.api.architecture

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.base.DescribedPredicate.not
import com.tngtech.archunit.core.domain.JavaAnnotation
import com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage
import com.tngtech.archunit.core.domain.JavaEnumConstant
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionalEventListener

private const val APPLICATION_PACKAGE = "com.papertrading.api.application.."

// phase 를 안 적은 @TransactionalEventListener 도 Spring 기본값이 AFTER_COMMIT 이라 포함
private val afterCommitListenerAnnotation: DescribedPredicate<JavaAnnotation<*>> =
    DescribedPredicate.describe("@TransactionalEventListener(phase = AFTER_COMMIT)") { annotation ->
        annotation.rawType.isEquivalentTo(TransactionalEventListener::class.java) &&
            annotation
                .get("phase")
                .map { (it as JavaEnumConstant).name() == "AFTER_COMMIT" }
                .orElse(true)
    }

/** PTB9 application 이 쓰는 Spring 타입 허용 목록, 동결 없는 규칙 본체(시험 픽스처 검증용) */
internal fun applicationUsesOnlyAllowedSpringTypesRule(): ArchRule = noClasses()
    .that()
    .resideInAPackage(APPLICATION_PACKAGE)
    .should()
    .dependOnClassesThat(
        resideInAPackage("org.springframework..")
            .and(not(equivalentTo(Service::class.java)))
            .and(not(equivalentTo(Async::class.java)))
            .and(
                not(
                    resideInAnyPackage(
                        "org.springframework.transaction.annotation..",
                        "org.springframework.transaction.event..",
                        "org.springframework.context.event..",
                    ),
                ),
            ),
    )

/** PTB9 application 의 @Async 는 AFTER_COMMIT 리스너 메서드에만 허용(메서드 단위로 판정) */
internal fun asyncOnlyOnAfterCommitListenerMethodsRule(): ArchRule = methods()
    .that()
    .areDeclaredInClassesThat()
    .resideInAPackage(APPLICATION_PACKAGE)
    .and()
    .areAnnotatedWith(Async::class.java)
    .should()
    .beAnnotatedWith(afterCommitListenerAnnotation)

/** PTB9 application 의 클래스 수준 @Async 금지, 모든 메서드에 걸려 메서드 조건 우회 */
internal fun applicationClassesAreNotAsyncRule(): ArchRule = noClasses()
    .that()
    .resideInAPackage(APPLICATION_PACKAGE)
    .should()
    .beAnnotatedWith(Async::class.java)
