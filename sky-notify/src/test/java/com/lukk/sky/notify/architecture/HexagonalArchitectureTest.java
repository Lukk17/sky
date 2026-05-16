package com.lukk.sky.notify.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * sky-notify has no REST controllers or JPA entities — it consumes Kafka and
 * pushes to WebSocket. Its hexagonal contract is narrower: inbound (Kafka) and
 * outbound (WebSocket) adapters with a thin domain coordinating between them.
 *
 * <p>Known tech-debt the rules deliberately tolerate:
 * <ul>
 *   <li>{@code domain.service.NotificationTransmissionServicePrimary} imports
 *       {@code adapters.outbound.NotificationPublisherPrimary} and
 *       {@code adapters.dto.WebsocketPayloadModel}. The orchestrator should live
 *       in {@code adapters.outbound} or talk only through ports — track as a
 *       follow-up refactor.</li>
 * </ul>
 */
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
    void domainPortsAreInterfaces() {
        classes()
                .that().resideInAPackage("..domain.ports..")
                .should().beInterfaces()
                .check(classes);
    }

    @Test
    @DisplayName("Domain has no Spring Web / Reactor / RestTemplate / WebClient dependencies")
    void domainHasNoWebStackDependencies() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.web.client..",
                        "org.springframework.web.reactive.function.client..",
                        "reactor.netty.."
                )
                .check(classes);
    }

    @Test
    @DisplayName("Kafka listeners live under adapters.inbound")
    void kafkaListenersInInboundAdapters() {
        classes()
                .that().haveSimpleNameContaining("Listener")
                .should().resideInAPackage("..adapters.inbound..")
                .check(classes);
    }
}
