package com.lukk.sky.notify.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@DisplayName("Hexagonal architecture constraints for sky-notify")
class HexagonalArchitectureTest {

    private static final String[] OWN_MONOREPO_PACKAGES = {
            "com.lukk.sky.notify..",
            "com.lukk.sky.common.."};

    private static final String[] DOMAIN_ALLOWED_PACKAGES = {
            "com.lukk.sky.notify.domain..",
            "com.lukk.sky.common..",
            "java..",
            "lombok..",
            "org.slf4j..",
            "org.springframework.context.annotation..",
            "org.springframework.stereotype.."};

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.lukk.sky.notify");
    }

    @Test
    @DisplayName("Domain ports are interfaces")
    void domainPorts_whenInspected_thenAllAreInterfaces() {
        classes()
                .that().resideInAPackage("..domain.ports..")
                .should().beInterfaces()
                .check(classes);
    }

    @Test
    @DisplayName("Domain never depends on an adapter")
    void domain_whenInspected_thenDoesNotDependOnAdapters() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..adapters..")
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
    void domain_whenInspected_thenDependsOnlyOnSanctionedPackages() {
        classes()
                .that().resideInAPackage("..domain..")
                .should().onlyDependOnClassesThat(areSanctionedDomainDependencies())
                .because("this service stores nothing, so its domain needs no persistence API and the allow list"
                        + " is shorter than the one the three stateful services carry")
                .check(classes);
    }

    @Test
    @DisplayName("Domain never depends on transport or serialization frameworks")
    void domain_whenInspected_thenHasNoTransportOrSerializationDependencies() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.web..",
                        "org.springframework.messaging..",
                        "org.springframework.kafka..",
                        "jakarta.servlet..",
                        "tools.jackson..",
                        "com.fasterxml.jackson..",
                        "com.google.gson.."
                )
                .check(classes);
    }

    @Test
    @DisplayName("Kafka listeners live under adapters.inbound")
    void kafkaListeners_whenInspected_thenResideInInboundAdaptersPackage() {
        classes()
                .that().haveSimpleNameContaining("Listener")
                .should().resideInAPackage("..adapters.inbound..")
                .check(classes);
    }

    @Test
    @DisplayName("WebSocket publishing stays in adapters.outbound")
    void webSocketPublishing_whenInspected_thenResidesInOutboundAdaptersPackage() {
        noClasses()
                .that().resideOutsideOfPackage("..adapters.outbound..")
                .should().dependOnClassesThat().haveFullyQualifiedName(
                        "org.springframework.messaging.simp.SimpMessagingTemplate")
                .check(classes);
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

                return target.isPrimitive() || allowedPackages.test(target);
            }
        };
    }
}
