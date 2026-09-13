package com.lukk.sky.message.architecture;

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

@DisplayName("Hexagonal Architecture: package dependency rules")
class HexagonalArchitectureTest {

    private static final String SPRING_DATA_REPOSITORY = "org.springframework.data.repository.Repository";

    private static final String REST_CONTROLLER_ADVICE =
            "org.springframework.web.bind.annotation.RestControllerAdvice";

    private static final String[] OWN_MONOREPO_PACKAGES = {
            "com.lukk.sky.message..",
            "com.lukk.sky.common.."};

    private static final String[] DOMAIN_ALLOWED_PACKAGES = {
            "com.lukk.sky.message.domain..",
            "com.lukk.sky.message.adapters.dto..",
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
            "org.springframework.transaction.annotation.."};

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
    @DisplayName("An inbound adapter never reaches a repository: a controller goes through a driving port")
    void inboundAdapterClasses_whenInspected_thenHaveNoDependencyOnARepository() {
        noClasses()
                .that().resideInAPackage("..adapters.inbound..")
                .should().dependOnClassesThat(areRepositories())
                .because("a controller calls a driving port under domain.ports.inbound, and reaching a repository"
                        + " skips the domain service that holds the rules")
                .check(classes);
    }

    @Test
    @DisplayName("Nothing imports another service: only sky-common is shared")
    void allClasses_whenInspected_thenHaveNoDependencyOnAnotherServiceModule() {
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
    void restControllerAdvice_whenInspected_thenReachesOnlyTheDomainAllowListPlusSpringWeb() {
        classes()
                .that().areAnnotatedWith(REST_CONTROLLER_ADVICE)
                .should().onlyDependOnClassesThat(areSanctionedDomainDependencies()
                        .or(JavaClass.Predicates.resideInAnyPackage(REST_ADVICE_EXTRA_PACKAGES)))
                .because("the advice maps domain exceptions onto HTTP, so it is the one class under domain that"
                        + " may reach Spring web, and it stays bound by the rest of the domain allow list")
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
}
