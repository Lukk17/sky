package com.lukk.sky.booking.domain.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.lukk.sky.booking.TestSecurityConfig;
import com.lukk.sky.booking.TestcontainersConfiguration;
import com.lukk.sky.booking.adapters.outbound.persistence.EventSpecifications;
import com.lukk.sky.booking.domain.model.Booking;
import com.lukk.sky.booking.domain.model.Event;
import com.lukk.sky.booking.domain.model.EventType;
import com.lukk.sky.booking.domain.ports.outbound.BookingEventStore;
import com.lukk.sky.booking.domain.ports.outbound.BookingRepository;
import com.lukk.sky.booking.domain.ports.outbound.EventSourceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, BookingEventAppendConcurrencyTest.IsolatedContainers.class})
@DisplayName("Appending booking events concurrently: every sequence number is used exactly once")
class BookingEventAppendConcurrencyTest {

    private static final int WRITERS = 4;
    private static final int APPENDS_PER_WRITER = 5;
    private static final int TOTAL_APPENDS = WRITERS * APPENDS_PER_WRITER;
    private static final String CONFLICT_LOG_MESSAGE = "event_append_conflict";

    @Autowired
    private EventSourceService eventSourceService;

    @Autowired
    private EventSourceRepository eventSourceRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoSpyBean
    private BookingEventStore bookingEventStore;

    private ListAppender<ILoggingEvent> retryLog;

    @BeforeEach
    void captureRetryLog() {
        retryLog = new ListAppender<>();
        retryLog.start();
        retryLogger().addAppender(retryLog);
    }

    @AfterEach
    void releaseRetryLog() {
        retryLogger().detachAppender(retryLog);
        retryLog.stop();
    }

    @Test
    @DisplayName("concurrent writers on one booking all succeed and produce the sequence numbers 1 to N exactly once")
    void saveEvent_whenWritersAppendForTheSameBookingAtOnce_thenSequenceNumbersAreOneToNWithoutGapOrDuplicate()
            throws Exception {
        // given
        Booking booking = booking(UUID.randomUUID());
        CyclicBarrier allWritersReady = new CyclicBarrier(WRITERS);

        // when
        List<Future<?>> appends = new ArrayList<>();
        try (ExecutorService writers = Executors.newFixedThreadPool(WRITERS)) {
            for (int writer = 0; writer < WRITERS; writer++) {
                appends.add(writers.submit(() -> {
                    allWritersReady.await(30, TimeUnit.SECONDS);

                    for (int append = 0; append < APPENDS_PER_WRITER; append++) {
                        eventSourceService.saveEvent(booking, EventType.BOOKED);
                    }

                    return null;
                }));
            }

            for (Future<?> append : appends) {
                append.get(120, TimeUnit.SECONDS);
            }
        }

        // then
        assertThat(sequenceNumbersOf(booking.getId()))
                .isEqualTo(IntStream.rangeClosed(1, TOTAL_APPENDS).boxed().toList());
        assertThat(loggedConflicts())
                .as("unique constraint violations the writers had to retry, proving they did not serialise")
                .isPositive();
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("a conflict inside a caller transaction is retried and leaves that transaction able to commit")
    void saveEvent_whenCalledInsideATransactionThatLosesTheRace_thenRetrySucceedsAndTheCallerStillCommits() {
        // given
        AtomicBoolean raceInjected = new AtomicBoolean();
        doAnswer(invocation -> {
            UUID bookingId = invocation.getArgument(0);
            Optional<Integer> lastSequenceNumber = (Optional<Integer>) invocation.callRealMethod();

            if (raceInjected.compareAndSet(false, true)) {
                commitCompetingEventOnAnotherThread(bookingId, lastSequenceNumber.orElse(0) + 1);
            }

            return lastSequenceNumber;
        }).when(bookingEventStore).findLastSequenceNumber(any(UUID.class));

        // when
        UUID bookingId = new TransactionTemplate(transactionManager).execute(status -> {
            Booking saved = bookingRepository.save(booking(null));
            eventSourceService.saveEvent(saved, EventType.BOOKED);

            return saved.getId();
        });

        // then
        assertThat(raceInjected).isTrue();
        assertThat(loggedConflicts()).isEqualTo(1);
        assertThat(bookingRepository.findById(bookingId))
                .as("the caller transaction committed its own write despite the conflict")
                .isPresent();
        assertThat(sequenceNumbersOf(bookingId)).isEqualTo(List.of(1, 2));
    }

    private void commitCompetingEventOnAnotherThread(UUID bookingId, int sequenceNumber) {
        CompletableFuture.runAsync(() -> eventSourceRepository.saveAndFlush(Event.builder()
                        .bookingId(bookingId)
                        .sequenceNumber(sequenceNumber)
                        .eventType(EventType.RESERVED)
                        .payload("{}")
                        .timestamp(Instant.now())
                        .build()))
                .join();
    }

    private List<Integer> sequenceNumbersOf(UUID bookingId) {
        return eventSourceRepository.findAll(EventSpecifications.hasBookingId(bookingId))
                .stream()
                .map(Event::getSequenceNumber)
                .sorted()
                .toList();
    }

    private long loggedConflicts() {
        return retryLog.list.stream()
                .filter(event -> event.getFormattedMessage().contains(CONFLICT_LOG_MESSAGE))
                .count();
    }

    private static Logger retryLogger() {
        return (Logger) LoggerFactory.getLogger(EventSourceServicePrimary.class);
    }

    private static Booking booking(UUID id) {
        return Booking.builder()
                .id(id)
                .offerId(UUID.randomUUID())
                .bookedDate(LocalDate.now().plusDays(1))
                .bookingUser("user@booking.com")
                .ownerEmail("owner@booking.com")
                .build();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class IsolatedContainers {

        @Bean
        @ServiceConnection
        PostgreSQLContainer unsharedPostgresContainer() {
            return new PostgreSQLContainer(TestcontainersConfiguration.POSTGRES_IMAGE).withReuse(false);
        }

        @Bean
        @ServiceConnection
        ConfluentKafkaContainer kafkaContainer() {
            return new ConfluentKafkaContainer(TestcontainersConfiguration.KAFKA_IMAGE).withReuse(true);
        }
    }
}
