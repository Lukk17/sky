package com.lukk.sky.offer.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@DisplayName("Hexagonal Architecture Tests: enforce package dependency rules for sky-offer")
class HexagonalArchitectureTest {

    private static final Set<String> HAND_WRITTEN_QUERY_TYPES = Set.of(
            "org.springframework.data.jpa.repository.Query",
            "org.springframework.data.jpa.repository.NativeQuery",
            "jakarta.persistence.EntityManager",
            "jakarta.persistence.EntityManagerFactory",
            "jakarta.persistence.PersistenceContext",
            "jakarta.persistence.Query",
            "jakarta.persistence.TypedQuery",
            "jakarta.persistence.NamedQuery",
            "jakarta.persistence.NamedNativeQuery",
            "org.hibernate.Session",
            "org.hibernate.SessionFactory");

    private static final String HIBERNATE_QUERY_PACKAGE = "org.hibernate.query";

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.lukk.sky.offer");
    }

    @Test
    @DisplayName("Entities (domain.model) do not import any adapter type")
    void domainModel_whenCheckedForDependencies_thenDoesNotImportAdapterClasses() {
        noClasses()
                .that().resideInAPackage("..domain.model..")
                .should().dependOnClassesThat().resideInAPackage("..adapters..")
                .check(classes);
    }

    @Test
    @DisplayName("Adapters do not import domain service implementations, only the ports")
    void adapters_whenCheckedForDependencies_thenDoNotImportDomainServiceImplementations() {
        noClasses()
                .that().resideInAPackage("..adapters..")
                .should().dependOnClassesThat().resideInAPackage("..domain.service..")
                .check(classes);
    }

    @Test
    @DisplayName("Domain calls no outbound HTTP client directly")
    void domainClasses_whenCheckedForHttpClients_thenHaveNoOutboundClientImports() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework.web.client..")
                .check(classes);
    }

    @Test
    @DisplayName("sky-offer is a blocking MVC service: nothing depends on Reactor or WebFlux")
    void allClasses_whenCheckedForReactiveTypes_thenHaveNoReactorOrWebFluxImports() {
        noClasses()
                .that().resideInAPackage("com.lukk.sky.offer..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "reactor..",
                        "org.springframework.web.reactive..")
                .check(classes);
    }

    @Test
    @DisplayName("Controllers live under adapters.inbound.api")
    void restControllers_whenPackageChecked_thenResideInAdaptersInboundApiPackage() {
        classes()
                .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .or().areAnnotatedWith("org.springframework.stereotype.Controller")
                .should().resideInAPackage("..adapters.inbound.api..")
                .check(classes);
    }

    @Test
    @DisplayName("JPA entities live under domain.model")
    void jpaEntities_whenPackageChecked_thenResideInDomainModelPackage() {
        classes()
                .that().areAnnotatedWith("jakarta.persistence.Entity")
                .should().resideInAPackage("..domain.model..")
                .check(classes);
    }

    @Test
    @DisplayName("Repository interfaces live under domain.ports.outbound")
    void repositories_whenPackageChecked_thenResideInDomainPortsOutboundPackage() {
        classes()
                .that().areAssignableTo("org.springframework.data.jpa.repository.JpaRepository")
                .or().haveSimpleNameEndingWith("Repository")
                .and().areInterfaces()
                .should().resideInAPackage("..domain.ports.outbound..")
                .check(classes);
    }

    @Test
    @DisplayName("No main source writes a query by hand: no @Query, no native query, no EntityManager")
    void mainSources_whenCheckedForHandWrittenQueries_thenNoneUsesQueryAnnotationNativeQueryOrEntityManager() {
        noClasses()
                .should().dependOnClassesThat(areHandWrittenQueryApi())
                .because("every query must be a Spring Data Specification passed to JpaSpecificationExecutor"
                        + " (findAll, findBy) or a derived query method: replace @Query, @NativeQuery,"
                        + " EntityManager.createQuery, EntityManager.createNativeQuery and the Hibernate query"
                        + " API with a Specification built in an adapter under adapters.outbound.persistence")
                .check(classes);
    }

    private static DescribedPredicate<JavaClass> areHandWrittenQueryApi() {
        return new DescribedPredicate<>("the @Query annotation, a native query, an EntityManager or a Hibernate query API") {
            @Override
            public boolean test(JavaClass javaClass) {
                return HAND_WRITTEN_QUERY_TYPES.contains(javaClass.getFullName())
                        || javaClass.getPackageName().startsWith(HIBERNATE_QUERY_PACKAGE);
            }
        };
    }
}
