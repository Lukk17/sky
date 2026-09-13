package com.lukk.sky.booking.architecture;

import com.lukk.sky.booking.SkyBookingApplication;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Enforces hexagonal layering for sky-booking. Rules tailored to the current
 * package layout. They pass today and act as a regression net.
 *
 * <p>Known tech-debt the rules deliberately tolerate (track in follow-up changes):
 * <ul>
 *   <li>{@code domain.service.*} imports {@code adapters.dto.BookingDTO}:
 *       DTOs should split into request/response (adapter) vs command/result (domain).</li>
 * </ul>
 */
@DisplayName("Hexagonal architecture enforcement tests")
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

    private static final String[] DOMAIN_ALLOWED_PACKAGES = {
            "com.lukk.sky.booking.domain..",
            "com.lukk.sky.booking.adapters.dto..",
            "com.lukk.sky.common..",
            "java..",
            "jakarta.persistence..",
            "jakarta.validation..",
            "lombok..",
            "org.slf4j..",
            "org.springframework.context.annotation..",
            "org.springframework.data.domain..",
            "org.springframework.data.jpa.repository..",
            "org.springframework.stereotype..",
            "org.springframework.transaction.annotation..",
            "org.springframework.util..",
            "tools.jackson.databind.."};

    private static final Set<String> DOMAIN_ALLOWED_TYPES = Set.of("org.hibernate.Hibernate");

    private static final String[] REST_ADVICE_EXTRA_PACKAGES = {
            "org.springframework.http..",
            "org.springframework.web",
            "org.springframework.web.bind.annotation.."};

    private static final String REST_CONTROLLER_ADVICE =
            "org.springframework.web.bind.annotation.RestControllerAdvice";

    private static final String SPRING_DATA_REPOSITORY = "org.springframework.data.repository.Repository";

    private static final String[] OWN_MONOREPO_PACKAGES = {
            "com.lukk.sky.booking..",
            "com.lukk.sky.common.."};

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importUrl(productionClasses());

        assertThat(classes)
                .as("production classes available to the architecture rules")
                .isNotEmpty();
    }

    private static URL productionClasses() {
        return SkyBookingApplication.class.getProtectionDomain().getCodeSource().getLocation();
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
    @DisplayName("Adapters never reach into domain.service, only into the ports")
    void adapters_whenInspected_thenDependOnPortsRatherThanDomainServiceImplementations() {
        noClasses()
                .that().resideInAPackage("..adapters..")
                .should().dependOnClassesThat().resideInAPackage("..domain.service..")
                .check(classes);
    }

    @Test
    @DisplayName("An inbound adapter never reaches a repository: a controller goes through a driving port")
    void inboundAdapters_whenInspected_thenDependOnNoRepository() {
        noClasses()
                .that().resideInAPackage("..adapters.inbound..")
                .should().dependOnClassesThat(areRepositories())
                .because("a controller calls a driving port under domain.ports.inbound, and reaching a repository"
                        + " skips the domain service that holds the rules")
                .check(classes);
    }

    @Test
    @DisplayName("Nothing imports another service: only sky-common is shared")
    void allClasses_whenInspected_thenImportNoOtherServiceModule() {
        noClasses()
                .should().dependOnClassesThat(areOtherServiceModules())
                .because("sky-common is the only module a service may share code through, and a second"
                        + " project dependency in the build file is what makes the import compile in the first place")
                .check(classes);
    }

    @Test
    @DisplayName("Domain depends only on the packages the architecture specification sanctions")
    void domainClasses_whenInspected_thenDependOnlyOnSanctionedPackages() {
        classes()
                .that().resideInAPackage("..domain..")
                .and().areNotAnnotatedWith(REST_CONTROLLER_ADVICE)
                .should().onlyDependOnClassesThat(areSanctionedDomainDependencies())
                .because("the domain carries its own persistence mapping, so the allow list names every framework"
                        + " it may reach and nothing else: a new one needs a specification change first")
                .check(classes);
    }

    @Test
    @DisplayName("The exception advice reaches Spring web, and nothing else the domain may not reach")
    void restControllerAdvice_whenInspected_thenDependsOnlyOnDomainPackagesPlusSpringWeb() {
        classes()
                .that().areAnnotatedWith(REST_CONTROLLER_ADVICE)
                .should().onlyDependOnClassesThat(areSanctionedDomainDependencies()
                        .or(JavaClass.Predicates.resideInAnyPackage(REST_ADVICE_EXTRA_PACKAGES)))
                .because("the advice maps domain exceptions onto HTTP, so it is the one class under domain that"
                        + " may reach Spring web, and it stays bound by the rest of the domain allow list")
                .check(classes);
    }

    @Test
    @DisplayName("Domain has no outbound HTTP client dependency")
    void domainClasses_whenInspected_thenHaveNoHttpClientDependencies() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .and().areNotAnnotatedWith("org.springframework.web.bind.annotation.RestControllerAdvice")
                .should().dependOnClassesThat().resideInAPackage("org.springframework.web.client..")
                .check(classes);
    }

    @Test
    @DisplayName("The whole service is off the reactive stack: no Reactor and no WebFlux anywhere")
    void allClasses_whenInspected_thenHaveNoReactiveStackDependencies() {
        noClasses()
                .should().dependOnClassesThat().resideInAnyPackage(
                        "reactor..",
                        "org.springframework.web.reactive..",
                        "org.springframework.http.client.reactive..")
                .check(classes);
    }

    @Test
    @DisplayName("Controllers live under adapters.inbound.api")
    void controllers_whenAnnotatedWithRestController_thenResideInAdaptersInboundApi() {
        classes()
                .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .or().areAnnotatedWith("org.springframework.stereotype.Controller")
                .should().resideInAPackage("..adapters.inbound.api..")
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
    @DisplayName("Repositories live under domain.ports.outbound")
    void repositories_whenImplementingJpaRepository_thenResideInDomainPortsOutbound() {
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

    private static DescribedPredicate<JavaClass> areRepositories() {
        return new DescribedPredicate<>("are a Spring Data repository or a Repository-suffixed interface") {
            @Override
            public boolean test(JavaClass javaClass) {
                return javaClass.isAssignableTo(SPRING_DATA_REPOSITORY)
                        || (javaClass.isInterface() && javaClass.getSimpleName().endsWith("Repository"));
            }
        };
    }

    private static DescribedPredicate<JavaClass> areOtherServiceModules() {
        return JavaClass.Predicates.resideInAPackage("com.lukk.sky..")
                .and(DescribedPredicate.not(JavaClass.Predicates.resideInAnyPackage(OWN_MONOREPO_PACKAGES)))
                .as("belong to another module of this monorepo");
    }

    private static DescribedPredicate<JavaClass> areSanctionedDomainDependencies() {
        DescribedPredicate<JavaClass> allowedPackages =
                JavaClass.Predicates.resideInAnyPackage(DOMAIN_ALLOWED_PACKAGES);

        return new DescribedPredicate<>("are sanctioned by the domain allow list") {
            @Override
            public boolean test(JavaClass javaClass) {
                JavaClass target = javaClass.isArray() ? javaClass.getBaseComponentType() : javaClass;

                return target.isPrimitive()
                        || DOMAIN_ALLOWED_TYPES.contains(target.getFullName())
                        || allowedPackages.test(target);
            }
        };
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
