package org.eclipse.microprofile.problemdetails.tck;

import com.github.t1.testcontainers.jee.AddLibMod;
import com.github.t1.testcontainers.jee.JeeContainer;
import com.github.t1.testcontainers.jee.Mod;
import lombok.Builder;
import lombok.Singular;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Condition;
import org.eclipse.microprofile.problemdetails.LogLevel;
import org.eclipse.microprofile.problemdetails.tck.ContainerLaunchingExtension.LoggedAssert.LoggedAssertBuilder;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.output.OutputFrame;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
public class ContainerLaunchingExtension implements Extension, BeforeAllCallback, BeforeEachCallback, AfterEachCallback {
    protected static URI BASE_URI = null;

    private static StringBuffer LOGS = null;
    private static LoggedAssertBuilder LOGGED_ASSERT_BUILDER = null;
    private static boolean CAN_CHECK_LOGGING = true;

    /**
     * Stopping is done by the ryuk container... see
     * https://www.testcontainers.org/test_framework_integration/manual_lifecycle_control/#singleton-containers
     */
    @Override public void beforeAll(ExtensionContext context) {
        if (System.getProperty(runningProperty()) != null) {
            CAN_CHECK_LOGGING = false;
            BASE_URI = URI.create(System.getProperty(runningProperty()));
        } else if (BASE_URI == null) {
            CAN_CHECK_LOGGING = true;
            JeeContainer container = buildJeeContainer()
                .withLogConsumer(ContainerLaunchingExtension::consumeLog);
            container.start();
            BASE_URI = container.baseUri();
        }
    }

    protected String runningProperty() { return "problemdetails-tck-running"; }

    protected JeeContainer buildJeeContainer() {
        String[] libs = System.getProperty("problemdetails-tck-libs", "").split("(\\s|,)");
        return buildJeeContainer(Stream.of(libs));
    }

    public static JeeContainer buildJeeContainer(Stream<String> libs) {
        Mod[] mods = libs
            .filter(uri -> !uri.isEmpty())
            .map(AddLibMod::addLib)
            .toArray(Mod[]::new);
        return JeeContainer.create()
            // TODO get the version, maybe from the manifest
            .withDeployment("urn:mvn:io.microprofile.sandbox:problem-details.tck-war:1.0.0-SNAPSHOT:war", mods);
    }

    protected static void consumeLog(OutputFrame outputFrame) {
        if (LOGS == null) {
            LOGS = new StringBuffer();
        }
        LOGS.append(outputFrame.getUtf8String());
    }

    @Override public void beforeEach(ExtensionContext context) {
        LOGS = null;
    }

    @Override public void afterEach(ExtensionContext context) {
        if (LOGGED_ASSERT_BUILDER != null) {
            LOGGED_ASSERT_BUILDER = null; // so the next test can run
            throw new IllegalStateException("LoggedAssertBuilder without check()");
        }
    }


    public static void withDisabledExceptionMessageAsDetail(Runnable runnable) {
        try {
            setExceptionMessageAsDetail(false);
            runnable.run();
        } finally {
            setExceptionMessageAsDetail(true);
        }
    }

    private static void setExceptionMessageAsDetail(boolean enabled) {
        Response response = target("/backdoors/message-as-detail").request()
            .post(Entity.form(new Form("enabled", Boolean.toString(enabled))));
        then(response.getStatusInfo())
            .describedAs(response.hasEntity() ? response.readEntity(String.class) : "")
            .isIn(OK, NO_CONTENT);
    }


    public static ProblemDetailAssert<ProblemDetail> testPost(String path) {
        return thenProblemDetail(target(path).request(APPLICATION_JSON_TYPE).post(null));
    }

    public static <T extends ProblemDetail> ProblemDetailAssert<T> testPost(String path, Class<T> type) {
        return thenProblemDetail(target(path).request(APPLICATION_JSON_TYPE).post(null), type);
    }

    public static ProblemDetailAssert<ProblemDetail> testPost(String path, String accept) {
        return thenProblemDetail(target(path).request(MediaType.valueOf(accept)).post(null));
    }

    public static ProblemDetailAssert<ProblemDetail> testPost(String path, String accept1, String accept2) {
        return thenProblemDetail(target(path).request(MediaType.valueOf(accept1), MediaType.valueOf(accept2)).post(null));
    }

    public static <T> ResponseAssert<T> testPost(String path, String accept, Class<T> type) {
        Response response = target(path).request(MediaType.valueOf(accept)).post(null);
        return new ResponseAssert<>(response, type);
    }

    public static WebTarget target(String path) {
        WebTarget target = target().path(path);
        log.info("target: {}", target.getUri());
        return target;
    }

    private static final Client CLIENT = ClientBuilder.newClient();

    public static WebTarget target() {
        return CLIENT.target(BASE_URI);
    }

    public static ProblemDetailAssert<ProblemDetail> thenProblemDetail(Response response) {
        return thenProblemDetail(response, ProblemDetail.class);
    }

    public static <T extends ProblemDetail> ProblemDetailAssert<T> thenProblemDetail(Response response, Class<T> type) {
        return new ProblemDetailAssert<>(response, type);
    }

    public static class ProblemDetailAssert<T extends ProblemDetail> extends ResponseAssert<T> {
        public ProblemDetailAssert(Response response, Class<T> type) { super(response, type); }

        @Override public ProblemDetailAssert<T> hasStatus(Status status) {
            super.hasStatus(status);
            assertThat(entity.getStatus()).describedAs("problem-detail.status")
                .isEqualTo(status.getStatusCode());
            return this;
        }

        @Override public ProblemDetailAssert<T> hasContentType(String contentType) {
            super.hasContentType(contentType);
            return this;
        }

        @Override public ProblemDetailAssert<T> hasContentType(MediaType contentType) {
            super.hasContentType(contentType);
            return this;
        }


        public ProblemDetailAssert<T> hasType(String type) {
            assertThat(entity.getType()).describedAs("problem-detail.type")
                .isEqualTo(URI.create(type));
            return this;
        }

        public ProblemDetailAssert<T> hasTitle(String title) {
            assertThat(entity.getTitle()).describedAs("problem-detail.title")
                .isEqualTo(title);
            return this;
        }

        public ProblemDetailAssert<T> hasDetail(String detail) {
            assertThat(getDetail()).describedAs("problem-detail.detail")
                .isEqualTo(detail);
            return this;
        }

        public ProblemDetailAssert<T> hasNoDetail() {
            assertThat(getDetail()).describedAs("problem-detail.detail")
                .isNull();
            return this;
        }

        public String getDetail() {
            return entity.getDetail();
        }

        public ProblemDetailAssert<T> hasUuidInstance() {
            assertThat(entity.getInstance()).describedAs("problem-detail.instance")
                .has(new Condition<>(instance -> instance.toString().startsWith("urn:uuid:"), "some uuid urn"));
            return this;
        }

        @SuppressWarnings("UnusedReturnValue")
        public ProblemDetailAssert<T> hasInstance(String instance) {
            assertThat(entity.getInstance()).describedAs("problem-detail.instance")
                .isEqualTo(URI.create(instance));
            return this;
        }

        @SuppressWarnings("UnusedReturnValue")
        public ProblemDetailAssert<T> hasNoInstance() {
            assertThat(entity.getInstance()).describedAs("problem-detail.instance")
                .isNull();
            return this;
        }

        public void check(Consumer<T> consumer) {
            consumer.accept(entity);
        }
    }

    public static class ResponseAssert<T> {
        protected final Response response;
        protected final T entity;

        public ResponseAssert(Response response, Class<T> type) {
            this.response = response;
            assertThat(this.response.hasEntity())
                .describedAs("response has entity")
                .isTrue();
            this.entity = this.response.readEntity(type);
        }

        public ResponseAssert<T> hasStatus(Status status) {
            assertThat(response.getStatusInfo()).describedAs("response status")
                .isEqualTo(status);
            return this;
        }

        public ResponseAssert<T> hasContentType(String contentType) {
            return hasContentType(MediaType.valueOf(contentType));
        }

        public ResponseAssert<T> hasContentType(MediaType contentType) {
            assertThat(response.getMediaType().isCompatible(contentType))
                .describedAs("response content type [" + response.getMediaType() + "] "
                    + "is not compatible with [" + contentType + "]").isTrue();
            return this;
        }

        @SuppressWarnings("UnusedReturnValue") public ResponseAssert<T> hasBody(T entity) {
            assertThat(this.entity).isEqualTo(entity);
            return this;
        }
    }


    @SneakyThrows(InterruptedException.class)
    public static void thenNothingLoggedTo(String logCategory) {
        Thread.sleep(50); // this is not nice
        if (LOGS != null) {
            then(LOGS).doesNotContain(logCategory);
        }
    }

    public static LoggedAssertBuilder thenLogged(LogLevel logLevel, String logCategory) {
        if (LOGGED_ASSERT_BUILDER != null)
            throw new IllegalStateException("thenLogged without check()");
        assumeThat(CAN_CHECK_LOGGING)
            .describedAs("can't grab logging from running container")
            .isTrue();
        LOGGED_ASSERT_BUILDER = LoggedAssert.builder().logLevel(logLevel).logCategory(logCategory);
        return LOGGED_ASSERT_BUILDER;
    }

    @Builder
    public static class LoggedAssert {
        private final LogLevel logLevel;
        private final String logCategory;
        private final String type;
        private final String title;
        private final String status;
        private final String detail;
        private final String instance;
        private final @Singular List<String> extensions;
        private final String stackTrace;

        public static class LoggedAssertBuilder {
            public void check() {
                if (LOGGED_ASSERT_BUILDER != this) {
                    throw new IllegalStateException("LoggedAssertBuilder not this");
                }
                LOGGED_ASSERT_BUILDER = null;
                build().check();
            }

            public LoggedAssertBuilder noStackTrace() {
                stackTrace = null;
                return this;
            }
        }

        public void check() {
            String logLevel = this.logLevel.toString();
            if ("ERROR".equals(logLevel) && usesJavaUtilLogging()) {
                logLevel = "SEVERE";
            }
            thenLogsContain(logLevel);
            thenLogsContain(logCategory);
            thenLogsContainIfNotNull(type);
            thenLogsContainIfNotNull(title);
            thenLogsContainIfNotNull(status);
            thenLogsContainIfNotNull(detail);
            thenLogsContainIfNotNull(instance);
            for (String extension : extensions) {
                thenLogsContainIfNotNull(extension);
            }
            thenLogsContainIfNotNull(stackTrace);
        }
    }

    private static boolean usesJavaUtilLogging() {
        return System.getProperty("jee-testcontainer", "")
            .matches("(open-liberty.*|payara.*)");
    }

    private static void thenLogsContainIfNotNull(String field) {
        if (field != null) {
            if (usesJavaUtilLogging()) {
                field = field.replace("/", "\\/"); // jee-testcontainer open-liberty logs as json
            }
            thenLogsContain(field);
        }
    }

    @SneakyThrows(InterruptedException.class)
    private static void thenLogsContain(String value) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() < start + 1000) {
            if (LOGS != null && LOGS.toString().contains(value)) {
                return;
            }
            Thread.sleep(10);
        }
        if (!value.endsWith("\n")) {
            value += "\n";
        }
        fail("\n" +
            "---------------------- logs\n" +
            LOGS +
            "---------------------- should contain\n" +
            value +
            "---------------------- but didn't within 1 second");
    }
}
