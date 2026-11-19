package com.bachelorprojekt_backend.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bachelorprojekt_backend.alarm.HighActivityAlarm;
import com.bachelorprojekt_backend.types.AlarmType;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AlarmServiceTest {
    @Test
    void logsOnlyAfterCommitNotRollback() {
        Logger logger = (Logger) LoggerFactory.getLogger(AlarmService.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try (var context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            var service = context.getBean(AlarmService.class);
            var transactions = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            var alarm = new HighActivityAlarm(AlarmType.HIGH_ACTIVITY, UUID.randomUUID(), UUID.randomUUID(),
                    UUID.randomUUID(), "High activity", Instant.parse("2026-01-01T12:00:00Z"), 5, 10);

            transactions.executeWithoutResult(status -> {
                service.send(alarm);
                assertTrue(appender.list.isEmpty());
                status.setRollbackOnly();
            });
            assertTrue(appender.list.isEmpty());

            transactions.executeWithoutResult(status -> {
                service.send(alarm);
                assertTrue(appender.list.isEmpty());
            });
            assertEquals(1, appender.list.size());
            assertEquals(Level.WARN, appender.list.getFirst().getLevel());
            assertTrue(appender.list.getFirst().getFormattedMessage().contains("HIGH_ACTIVITY"));
            assertTrue(appender.list.getFirst().getFormattedMessage().contains(alarm.tenantId().toString()));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {
        @Bean
        AlarmService alarmService(org.springframework.context.ApplicationEventPublisher events) {
            return new AlarmService(events);
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                @Override protected Object doGetTransaction() { return new Object(); }
                @Override protected void doBegin(@NonNull Object transaction, @NonNull TransactionDefinition definition) {
                    // no-op
                }
                @Override protected void doCommit(@NonNull DefaultTransactionStatus status) {
                    // no-op
                }
                @Override protected void doRollback(@NonNull DefaultTransactionStatus status) {
                    // no-op
                }
            };
        }
    }
}
