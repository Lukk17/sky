package com.lukk.sky.message.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@DisplayName("Hexagonal Architecture: package dependency rules")
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
    @DisplayName("No class uses Reactor or WebFlux: sky-message is a servlet MVC service")
    void allClasses_whenInspected_thenHaveNoReactiveStackDependencies() {
        noClasses()
                .should().dependOnClassesThat().resideInAnyPackage(
                        "reactor..",
                        "org.reactivestreams..",
                        "org.springframework.web.reactive..",
                        "org.springframework.http.client.reactive.."
                )
                .because("sky-message runs on blocking Spring MVC and declares no reactive dependency")
                .check(classes);
    }

    @Test
    @DisplayName("Domain does not call out over HTTP: outbound calls belong to an adapter")
    void domainClasses_whenInspected_thenHaveNoHttpClientDependencies() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework.web.client..")
                .check(classes);
    }

    @Test
    @DisplayName("Adapters depend on ports, never on a domain service implementation")
    void adapterClasses_whenInspected_thenHaveNoDependencyOnDomainServiceImplementations() {
        noClasses()
                .that().resideInAPackage("..adapters..")
                .should().dependOnClassesThat().resideInAPackage("..domain.service..")
                .check(classes);
    }

    @Test
    @DisplayName("No class acts as an OAuth2 client: sky-message only validates tokens it is given")
    void allClasses_whenInspected_thenHaveNoOAuth2ClientDependencies() {
        noClasses()
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework.security.oauth2.client..")
                .because("sky-message reaches Keycloak for signing keys alone, so it holds no client credential")
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
    @DisplayName("Repositories live under domain.ports.outbound")
    void repositories_whenInspected_thenResideInDomainPortsOutboundPackage() {
        classes()
                .that().areAssignableTo("org.springframework.data.jpa.repository.JpaRepository")
                .or().haveSimpleNameEndingWith("Repository")
                .and().areInterfaces()
                .should().resideInAPackage("..domain.ports.outbound..")
                .check(classes);
    }
}
