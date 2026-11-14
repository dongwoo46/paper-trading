package com.papertrading.api.architecture

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.base.DescribedPredicate.not
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import com.tngtech.archunit.library.freeze.FreezingArchRule
import org.springframework.stereotype.Service

// infrastructure 클래스가 구현하는 인터페이스 = 출력 포트 판정
private val implementedByInfrastructure: DescribedPredicate<JavaClass> =
    DescribedPredicate.describe("are implemented by infrastructure classes") { javaClass ->
        javaClass.allSubclasses.any { it.packageName.startsWith("com.papertrading.api.infrastructure") }
    }

/** trading-api 계층·포트 규칙(PTB1·PTB7~PTB9), 기존 위반 archunit_store 동결·새 위반만 실패, 갱신 방법은 archunit.properties */
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
                .that()
                .resideInAPackage("com.papertrading.api.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("jakarta.persistence..", "org.hibernate..", "org.springframework.."),
        )

    @ArchTest
    @JvmField
    val domainDoesNotDependOnOuterLayers: ArchRule =
        FreezingArchRule.freeze(
            noClasses()
                .that()
                .resideInAPackage("com.papertrading.api.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
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
                .that()
                .resideInAPackage("com.papertrading.api.application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                    "com.papertrading.api.infrastructure..",
                    "com.papertrading.api.presentation..",
                ),
        )

    @ArchTest
    @JvmField
    val applicationUsesOnlyAllowedSpringTypes: ArchRule =
        FreezingArchRule.freeze(
            noClasses()
                .that()
                .resideInAPackage("com.papertrading.api.application..")
                .should()
                .dependOnClassesThat(
                    resideInAPackage("org.springframework..")
                        .and(not(equivalentTo(Service::class.java)))
                        .and(
                            not(
                                resideInAnyPackage(
                                    "org.springframework.transaction.annotation..",
                                    "org.springframework.transaction.event..",
                                    "org.springframework.context.event..",
                                ),
                            ),
                        ),
                ),
        )

    @ArchTest
    @JvmField
    val presentationDoesNotDependOnInfrastructure: ArchRule =
        FreezingArchRule.freeze(
            noClasses()
                .that()
                .resideInAPackage("com.papertrading.api.presentation..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.papertrading.api.infrastructure.."),
        )

    @ArchTest
    @JvmField
    val infrastructureDoesNotDependOnPresentation: ArchRule =
        FreezingArchRule.freeze(
            noClasses()
                .that()
                .resideInAPackage("com.papertrading.api.infrastructure..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.papertrading.api.presentation.."),
        )

    @ArchTest
    @JvmField
    val presentationDependsOnDomainOnlyThroughEnumsAndEvents: ArchRule =
        FreezingArchRule.freeze(
            noClasses()
                .that()
                .resideInAPackage("com.papertrading.api.presentation..")
                .should()
                .dependOnClassesThat(
                    resideInAPackage("com.papertrading.api.domain..").and(
                        not(
                            resideInAnyPackage(
                                "com.papertrading.api.domain.enums..",
                                "com.papertrading.api.domain.event..",
                            ),
                        ),
                    ),
                ),
        )

    @ArchTest
    @JvmField
    val infrastructureDependsOnApplicationOnlyThroughPorts: ArchRule =
        FreezingArchRule.freeze(
            noClasses()
                .that()
                .resideInAPackage("com.papertrading.api.infrastructure..")
                .should()
                .dependOnClassesThat(
                    resideInAPackage("com.papertrading.api.application..")
                        .and(not(resideInAPackage("..port.."))),
                ),
        )

    @ArchTest
    @JvmField
    val commonDoesNotDependOnJpaOrSpring: ArchRule =
        FreezingArchRule.freeze(
            noClasses()
                .that()
                .resideInAPackage("com.papertrading.api.common..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("jakarta.persistence..", "org.springframework.."),
        )

    @ArchTest
    @JvmField
    val commonDoesNotDependOnLayers: ArchRule =
        FreezingArchRule.freeze(
            noClasses()
                .that()
                .resideInAPackage("com.papertrading.api.common..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                    "com.papertrading.api.domain..",
                    "com.papertrading.api.application..",
                    "com.papertrading.api.infrastructure..",
                    "com.papertrading.api.presentation..",
                ),
        )

    @ArchTest
    @JvmField
    val outputPortsResideInApplicationPortPackage: ArchRule =
        FreezingArchRule.freeze(
            classes()
                .that()
                .areInterfaces()
                .and()
                .resideInAnyPackage("com.papertrading.api.domain..", "com.papertrading.api.application..")
                .and(implementedByInfrastructure)
                .should()
                .resideInAPackage("com.papertrading.api.application.*.port"),
        )

    @ArchTest
    @JvmField
    val everyClassResidesInRootOrLayerPackage: ArchRule =
        FreezingArchRule.freeze(
            classes()
                .should()
                .resideInAnyPackage(
                    "com.papertrading.api",
                    "com.papertrading.api.presentation..",
                    "com.papertrading.api.application..",
                    "com.papertrading.api.domain..",
                    "com.papertrading.api.infrastructure..",
                    "com.papertrading.api.common..",
                ),
        )

    @ArchTest
    @JvmField
    val applicationContextsAreFreeOfCycles: ArchRule =
        FreezingArchRule.freeze(
            slices()
                .matching("com.papertrading.api.application.(*)..")
                .should()
                .beFreeOfCycles(),
        )
}
