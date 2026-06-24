package com.lukk.sky.booking.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces hexagonal layering for sky-booking. Rules tailored to the current
 * package layout — they pass today and act as a regression net.
 *
 * <p>Known tech-debt the rules deliberately tolerate (track in follow-up changes):
 * <ul>
 *   <li>{@code domain.ports.service.*} imports {@code adapters.dto.BookingDTO} —
 *       DTOs should split into request/response (adapter) vs command/result (domain).</li>
 * </ul>
 */
@DisplayName("Hexagonal architecture enforcement tests")
class HexagonalArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.lukk.sky.booking");
    }

    @Test
    @DisplayName("Entities (domain.model) do not import any adapter type")
    void entitiesDoNotImportAdapters_whenDomainModelClassesExist_thenNoDependencyOnAdapters() {
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
    @DisplayName("Controllers live under adapters.api")
    void controllers_whenAnnotatedWithRestController_thenResideInAdaptersApi() {
        classes()
                .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .or().areAnnotatedWith("org.springframework.stereotype.Controller")
                .should().resideInAPackage("..adapters.api..")
                .check(classes);
    }

    @Test
    @DisplayName("JPA entities live under domain.model")
    void jpaEntities_whenAnnotatedWithEntity_thenResideInDomainModel() {
        classes()
                .that().areAnnotatedWith("jakarta.persistence.Entity")
                .should().resideInAPackage("..domain.model..")
                .check(classes);
    }

    @Test
    @DisplayName("Repositories live under domain.ports.repository")
    void repositories_whenImplementingJpaRepository_thenResideInDomainPortsRepository() {
        classes()
                .that().areAssignableTo("org.springframework.data.jpa.repository.JpaRepository")
                .or().haveSimpleNameEndingWith("Repository")
                .and().areInterfaces()
                .should().resideInAPackage("..domain.ports.repository..")
                .check(classes);
    }
}
