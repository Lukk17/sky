package com.lukk.sky.message.db;

import com.lukk.sky.common.config.CommonConfigPropertiesAutoConfiguration;
import com.lukk.sky.message.TestcontainersConfiguration;
import com.lukk.sky.message.config.propertyBind.SpringConfigProperties;
import com.lukk.sky.message.domain.model.Message;
import com.lukk.sky.message.domain.ports.outbound.MessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@ImportAutoConfiguration(CommonConfigPropertiesAutoConfiguration.class)
@EnableConfigurationProperties(SpringConfigProperties.class)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.flyway.locations=classpath:db/migration,classpath:db/demo")
@DisplayName("Demo seed: the migration set the local profile runs")
class DemoSeedMigrationTest {

    private static final String DEMO_OWNER = "owner@sky.dev";
    private static final String DEMO_USER = "user@sky.dev";

    @Autowired
    private MessageRepository messageRepository;

    @Test
    @DisplayName("seeds three demo messages exchanged between the demo owner and the demo user")
    void demoSeed_whenFlywayRunsWithTheDemoLocation_thenThreeMessagesArePersisted() {
        // when
        List<Message> messages = messageRepository.findAll();

        // then
        assertEquals(3, messages.size());
        assertTrue(messages.stream().allMatch(message -> message.getId() != null));
        assertTrue(messages.stream().allMatch(message -> message.getCreatedTime() != null));
    }

    @Test
    @DisplayName("addresses two seeded messages to the demo owner and one to the demo user")
    void demoSeed_whenFlywayRunsWithTheDemoLocation_thenMessagesAreAddressedToTheDemoAccounts() {
        // when
        List<Message> messages = messageRepository.findAll();

        // then
        assertEquals(2, messages.stream().filter(message -> DEMO_OWNER.equals(message.getReceiverEmail())).count());
        assertEquals(1, messages.stream().filter(message -> DEMO_USER.equals(message.getReceiverEmail())).count());
        assertTrue(messages.stream().allMatch(message -> isDemoAccount(message.getSenderEmail())));
    }

    @Test
    @DisplayName("gives every seeded message a distinct identifier, so a re-run cannot duplicate a row")
    void demoSeed_whenFlywayRunsWithTheDemoLocation_thenEveryMessageHasADistinctId() {
        // when
        List<Message> messages = messageRepository.findAll();

        // then
        assertNotNull(messages);
        assertEquals(messages.size(), messages.stream().map(Message::getId).distinct().count());
    }

    private static boolean isDemoAccount(String email) {
        return DEMO_OWNER.equals(email) || DEMO_USER.equals(email);
    }
}
