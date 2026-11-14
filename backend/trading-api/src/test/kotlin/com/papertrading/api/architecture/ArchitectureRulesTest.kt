package com.papertrading.api.architecture

import com.tngtech.archunit.base.DescribedPredicate.not
import com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.library.freeze.FreezingArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.springframework.stereotype.Service

/**
 * trading-api 계층·포트 규칙 (rules/backend-api.md PTB1, PTB7~PTB9).
 * 계층: presentation → application → domain, infrastructure 는 안쪽 포트를 구현만 한다.
 * 기존 위반은 src/test/resources/archunit_store 에 동결돼 있고, 새 위반만 실패한다.
 */
@AnalyzeClasses(
    packages = ["com.papertrading.api"],
    importOptions = [ImportOption.DoNotIncludeTests::class],
)
class ArchitectureRulesTest {
    @ArchTest
    @JvmField
    val domainDoesNotDependOnJpaOrSpring: ArchRule =
        FreezingArchRule.freeze(
            noClasses()
                .that().resideInAPackage("com.papertrading.api.domain..")
                .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.springframework.."),
        )

    @ArchTest
    @JvmField
    val domainDoesNotDependOnOuterLayers: ArchRule =
        FreezingArchRule.freeze(
            noClasses()
                .that().resideInAPackage("com.papertrading.api.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                    "com.papertrading.api.application..",
                    "com.papertrading.api.infrastructure..",
                    "com.papertrading.api.presentation..",
                ),
        )

    @ArchTest
    @JvmField
    val applicationDoesNotDependOnInfrastructureOrPresentation: ArchRule =
        FreezingArchRule.freeze(
            noClasses()
                .that().resideInAPackage("com.papertrading.api.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                    "com.papertrading.api.infrastructure..",
                    "com.papertrading.api.presentation..",
                ),
        )

    @ArchTest
    @JvmField
    val applicationUsesOnlyServiceAndTransactionalFromSpring: ArchRule =
        FreezingArchRule.freeze(
            noClasses()
                .that().resideInAPackage("com.papertrading.api.application..")
                .should().dependOnClassesThat(
                    resideInAPackage("org.springframework..")
                        .and(not(equivalentTo(Service::class.java)))
                        .and(not(resideInAPackage("org.springframework.transaction.annotation.."))),
                ),
        )

    @ArchTest
    @JvmField
    val presentationDoesNotDependOnInfrastructure: ArchRule =
        FreezingArchRule.freeze(
            noClasses()
                .that().resideInAPackage("com.papertrading.api.presentation..")
                .should().dependOnClassesThat().resideInAPackage("com.papertrading.api.infrastructure.."),
        )

    @ArchTest
    @JvmField
    val infrastructureDoesNotDependOnPresentation: ArchRule =
        FreezingArchRule.freeze(
            noClasses()
                .that().resideInAPackage("com.papertrading.api.infrastructure..")
                .should().dependOnClassesThat().resideInAPackage("com.papertrading.api.presentation.."),
        )

    @ArchTest
    @JvmField
    val outputPortsResideInApplicationPortPackage: ArchRule =
        FreezingArchRule.freeze(
            classes()
                .that().areInterfaces().and().haveSimpleNameEndingWith("Port")
                .should().resideInAPackage("com.papertrading.api.application.*.port"),
        )

    @ArchTest
    @JvmField
    val applicationContextsAreFreeOfCycles: ArchRule =
        FreezingArchRule.freeze(
            slices()
                .matching("com.papertrading.api.application.(*)..")
                .should().beFreeOfCycles(),
        )
}
