package com.lukk.sky.common.web;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.dao.DataAccessException;
import org.springframework.data.core.PropertyReferenceException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RestExceptionHandlerAutoConfiguration")
class RestExceptionHandlerAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestExceptionHandlerAutoConfiguration.class));

    private ListAppender<ILoggingEvent> logAppender;
    private Logger introspectionFailureLogger;

    @BeforeEach
    void setUp() {
        introspectionFailureLogger = (Logger) LoggerFactory.getLogger(MergedAnnotation.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        introspectionFailureLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        introspectionFailureLogger.detachAppender(logAppender);
    }

    @Test
    @DisplayName("registersBothHandlers_whenSpringDataIsOnTheClasspath")
    void registersBothHandlers_whenSpringDataIsOnTheClasspath() {
        runner.run(context -> assertThat(context)
                .hasSingleBean(SkyRestExceptionHandler.class)
                .hasSingleBean(SpringDataExceptionHandler.class));
    }

    @Test
    @DisplayName("registersOnlyTheFrameworkHandler_whenSpringDataIsFilteredOffTheClasspath")
    void registersOnlyTheFrameworkHandler_whenSpringDataIsFilteredOffTheClasspath() {
        runner.withClassLoader(new FilteredClassLoader(PropertyReferenceException.class))
                .run(context -> assertThat(context)
                        .hasSingleBean(SkyRestExceptionHandler.class)
                        .as("the Spring Data handler cannot exist on a classpath without Spring Data")
                        .doesNotHaveBean(SpringDataExceptionHandler.class));
    }

    @Test
    @DisplayName("registersOnlyTheFrameworkHandler_whenTheDaoExceptionHierarchyIsFilteredOffTheClasspath")
    void registersOnlyTheFrameworkHandler_whenTheDaoExceptionHierarchyIsFilteredOffTheClasspath() {
        runner.withClassLoader(new FilteredClassLoader(DataAccessException.class))
                .run(context -> assertThat(context)
                        .hasSingleBean(SkyRestExceptionHandler.class)
                        .as("the handler translates a DataAccessException, so it cannot exist without that hierarchy")
                        .doesNotHaveBean(SpringDataExceptionHandler.class));
    }

    @Test
    @DisplayName("logsNoIntrospectionFailure_whenSpringDataCannotBeResolved")
    void logsNoIntrospectionFailure_whenSpringDataCannotBeResolved() throws ClassNotFoundException {
        Class<?> withoutSpringData = new SpringDataHiddenClassLoader()
                .redefine(RestExceptionHandlerAutoConfiguration.class);

        List<Method> beanMethods = Arrays.stream(withoutSpringData.getDeclaredMethods())
                .filter(method -> MergedAnnotations.from(method).isPresent(Bean.class))
                .toList();

        assertThat(beanMethods)
                .as("the scan has to read bean annotations, or the log assertion below proves nothing")
                .isNotEmpty();
        assertThat(logAppender.list)
                .as("a bean method must not make Spring resolve a type the consumer does not have")
                .isEmpty();
    }

    private static final class SpringDataHiddenClassLoader extends ClassLoader {

        private static final String HIDDEN_PACKAGE_PREFIX = "org.springframework.data.";

        private SpringDataHiddenClassLoader() {
            super(SpringDataHiddenClassLoader.class.getClassLoader());
        }

        private Class<?> redefine(Class<?> type) throws ClassNotFoundException {
            String classFileName = type.getName().replace('.', '/') + ".class";

            try (InputStream classFile = getParent().getResourceAsStream(classFileName)) {
                if (classFile == null) {
                    throw new ClassNotFoundException(type.getName());
                }

                byte[] bytecode = classFile.readAllBytes();

                return defineClass(type.getName(), bytecode, 0, bytecode.length);
            } catch (IOException ex) {
                throw new ClassNotFoundException(type.getName(), ex);
            }
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith(HIDDEN_PACKAGE_PREFIX)) {
                throw new ClassNotFoundException(name);
            }

            return super.loadClass(name, resolve);
        }
    }
}
