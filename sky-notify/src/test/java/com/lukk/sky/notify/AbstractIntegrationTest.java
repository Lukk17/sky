package com.lukk.sky.notify;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.TopicExistsException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.lukk.sky.notify.config.Constants.KAFKA_BOOKING_TOPIC;
import static com.lukk.sky.notify.config.Constants.KAFKA_OFFER_TOPIC;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final short SINGLE_REPLICA = 1;
    private static final int SINGLE_PARTITION = 1;
    private static final int TOPIC_CREATION_TIMEOUT_SECONDS = 30;

    // One broker for the whole JVM, started here rather than through @Testcontainers: a @Container
    // field is started and stopped per test class, while the Spring context that binds to its address
    // is cached across classes, so the second class would talk to the address of a broker it no longer owns.
    protected static final ConfluentKafkaContainer KAFKA =
            new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"))
                    .withReuse(true);

    static {
        KAFKA.start();
        createTopics();
    }

    // sky-notify builds its own ConsumerFactory/ProducerFactory in KafkaConsumerConfig from
    // the spring.kafka.bootstrap-servers property (not Spring Boot's auto-config), so a
    // KafkaConnectionDetails bean alone does not reach it. Feed the broker into that property explicitly.
    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    // The broker does not auto-create topics and the service runs with spring.kafka.admin.auto-create=false,
    // so the topics have to exist before the listener containers subscribe.
    private static void createTopics() {
        Map<String, Object> adminConfig = Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());

        try (Admin admin = Admin.create(adminConfig)) {
            admin.createTopics(List.of(
                            new NewTopic(KAFKA_OFFER_TOPIC, SINGLE_PARTITION, SINGLE_REPLICA),
                            new NewTopic(KAFKA_BOOKING_TOPIC, SINGLE_PARTITION, SINGLE_REPLICA)))
                    .all()
                    .get(TOPIC_CREATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException ex) {
            if (!(ex.getCause() instanceof TopicExistsException)) {
                throw new IllegalStateException("Could not create the sky-notify Kafka topics", ex);
            }
        } catch (TimeoutException ex) {
            throw new IllegalStateException("Timed out creating the sky-notify Kafka topics", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException("Interrupted while creating the sky-notify Kafka topics", ex);
        }
    }
}
