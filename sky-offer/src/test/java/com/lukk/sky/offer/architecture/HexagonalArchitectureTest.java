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

    private static final String SPRING_DATA_REPOSITORY = "org.springframework.data.repository.Repository";

    private static final String REST_CONTROLLER_ADVICE =
            "org.springframework.web.bind.annotation.RestControllerAdvice";

    private static final String[] OWN_MONOREPO_PACKAGES = {
            "com.lukk.sky.offer..",
            "com.lukk.sky.common.."};

    private static final String[] DOMAIN_ALLOWED_PACKAGES = {
            "com.lukk.sky.offer.domain..",
            "com.lukk.sky.offer.adapters.dto..",
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
    @DisplayName("An inbound adapter never reaches a repository: a controller goes through a driving port")
    void inboundAdapters_whenCheckedForRepositories_thenDependOnNone() {
        noClasses()
                .that().resideInAPackage("..adapters.inbound..")
                .should().dependOnClassesThat(areRepositories())
                .because("a controller calls a driving port under domain.ports.inbound, and reaching a repository"
                        + " skips the domain service that holds the rules")
                .check(classes);
    }

    @Test
    @DisplayName("An inbound adapter never reaches a driven port: a controller publishes no event of its own")
    void inboundAdapters_whenCheckedForOutboundPorts_thenDependOnNone() {
        noClasses()
                .that().resideInAPackage("..adapters.inbound..")
                .should().dependOnClassesThat().resideInAPackage("..domain.ports.outbound..")
                .because("a controller calls a driving port under domain.ports.inbound, and reaching a driven port"
                        + " skips the domain service that owns whether the side effect happens at all")
                .check(classes);
    }

    @Test
    @DisplayName("Nothing imports another service: only sky-common is shared")
    void allClasses_whenCheckedForSiblingModules_thenImportNoOtherServiceModule() {
        noClasses()
                .should().dependOnClassesThat(areOtherServiceModules())
                .because("sky-common is the only module a service may share code through, and a second"
                        + " project dependency in the build file is what makes the import compile in the first place")
                .check(classes);
    }

    @Test
    @DisplayName("Domain depends only on the packages the architecture specification sanctions")
    void domainClasses_whenCheckedForDependencies_thenDependOnlyOnSanctionedPackages() {
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
    void restControllerAdvice_whenCheckedForDependencies_thenReachesOnlyTheDomainAllowListPlusSpringWeb() {
        classes()
                .that().areAnnotatedWith(REST_CONTROLLER_ADVICE)
                .should().onlyDependOnClassesThat(areSanctionedDomainDependencies()
                        .or(JavaClass.Predicates.resideInAnyPackage(REST_ADVICE_EXTRA_PACKAGES)))
                .because("the advice maps domain exceptions onto HTTP, so it is the one class under domain that"
                        + " may reach Spring web, and it stays bound by the rest of the domain allow list")
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
