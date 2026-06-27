package com.lukk.sky.message.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@DisplayName("Hexagonal Architecture — package dependency rules")
class HexagonalArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.lukk.sky.message");
    }

    @Test
    @DisplayName("Entities (domain.model) do not import any adapter type")
    void domainModelClasses_whenInspected_thenHaveNoDependencyOnAdapters() {
        noClasses()
                .that().resideInAPackage("..domain.model..")
                .should().dependOnClassesThat().resideInAPackage("..adapters..")
                .check(classes);
    }

    @Test
    @DisplayName("Domain has no Spring Web / Reactor / RestTemplate / WebClient dependencies")
    void domainClasses_whenInspected_thenHaveNoWebStackDependencies() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .and().areNotAnnotatedWith("org.springframework.web.bind.annotation.RestControllerAdvice")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.web.client..",
                        "org.springframework.web.reactive.function.client..",
                        "reactor.netty.."
                )
                .check(classes);
    }

    @Test
    @DisplayName("Controllers live under adapters.inbound.api")
    void controllers_whenInspected_thenResideInAdaptersInboundApiPackage() {
        classes()
                .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .or().areAnnotatedWith("org.springframework.stereotype.Controller")
                .should().resideInAPackage("..adapters.inbound.api..")
                .check(classes);
    }

    @Test
    @DisplayName("JPA entities live under domain.model")
    void jpaEntities_whenInspected_thenResideInDomainModelPackage() {
        classes()
                .that().areAnnotatedWith("jakarta.persistence.Entity")
                .should().resideInAPackage("..domain.model..")
                .check(classes);
    }

    @Test
    @DisplayName("Repositories live under domain.ports.repository")
    void repositories_whenInspected_thenResideInDomainPortsRepositoryPackage() {
        classes()
                .that().areAssignableTo("org.springframework.data.jpa.repository.JpaRepository")
                .or().haveSimpleNameEndingWith("Repository")
                .and().areInterfaces()
                .should().resideInAPackage("..domain.ports.repository..")
                .check(classes);
    }
}
