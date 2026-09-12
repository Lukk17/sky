package com.lukk.sky.notify.architecture;

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
}
