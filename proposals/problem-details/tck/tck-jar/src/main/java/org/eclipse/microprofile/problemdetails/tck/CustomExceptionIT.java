package org.eclipse.microprofile.problemdetails.tck;

import org.eclipse.microprofile.problemdetails.LogLevel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.eclipse.microprofile.problemdetails.Constants.PROBLEM_DETAIL_JSON;
import static org.eclipse.microprofile.problemdetails.LogLevel.INFO;
import static org.eclipse.microprofile.problemdetails.LogLevel.OFF;
import static org.eclipse.microprofile.problemdetails.tck.ContainerLaunchingExtension.testPost;
import static org.eclipse.microprofile.problemdetails.tck.ContainerLaunchingExtension.thenLogged;
import static org.eclipse.microprofile.problemdetails.tck.ContainerLaunchingExtension.thenNothingLoggedTo;
import static org.eclipse.microprofile.problemdetails.tck.ContainerLaunchingExtension.withDisabledExceptionMessageAsDetail;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;

@ExtendWith(ContainerLaunchingExtension.class)
class CustomExceptionIT {

    @Test void shouldMapCustomRuntimeException() {
        testPost("/custom/runtime-exception")
            .hasStatus(BAD_REQUEST)
            .hasContentType(PROBLEM_DETAIL_JSON)
            .hasType("urn:problem-type:custom")
            .hasTitle("Custom")
            .hasNoDetail()
            .hasUuidInstance();
    }

    @Test void shouldMapCustomIllegalArgumentException() {
        testPost("/custom/illegal-argument-exception")
            .hasStatus(BAD_REQUEST)
            .hasContentType(PROBLEM_DETAIL_JSON)
            .hasType("urn:problem-type:custom")
            .hasTitle("Custom")
            .hasNoDetail()
            .hasUuidInstance();
    }

    @Test void shouldMapExplicitType() {
        testPost("/custom/explicit-type")
            .hasStatus(BAD_REQUEST)
            .hasContentType(PROBLEM_DETAIL_JSON)
            .hasType("http://error-codes.org/out-of-memory")
            .hasTitle("Some")
            .hasNoDetail()
            .hasUuidInstance();
    }

    @Test void shouldMapExplicitTitle() {
        testPost("/custom/explicit-title")
            .hasStatus(BAD_REQUEST)
            .hasContentType(PROBLEM_DETAIL_JSON)
            .hasType("urn:problem-type:some")
            .hasTitle("Some Title")
            .hasNoDetail()
            .hasUuidInstance();
    }

    @Test void shouldMapExplicitStatus() {
        testPost("/custom/explicit-status")
            .hasStatus(FORBIDDEN)
            .hasContentType(PROBLEM_DETAIL_JSON)
            .hasType("urn:problem-type:something-forbidden")
            .hasTitle("Something Forbidden")
            .hasNoDetail()
            .hasUuidInstance();
    }

    @Test void shouldMapCustomExceptionWithMessageButDisableExceptionMessageAsDetail() {
        withDisabledExceptionMessageAsDetail(() -> testPost("/custom/forbidden-with-message")
            .hasStatus(FORBIDDEN)
            .hasContentType(PROBLEM_DETAIL_JSON)
            .hasType("urn:problem-type:something-forbidden")
            .hasTitle("SomethingForbidden")
            .hasNoDetail()
            .hasUuidInstance());
    }

    @Nested class ExplicitDetail {
        @Test void shouldMapDetailMethod() {
            testPost("/custom/public-detail-method")
                .hasStatus(BAD_REQUEST)
                .hasContentType(PROBLEM_DETAIL_JSON)
                .hasType("urn:problem-type:some-message")
                .hasTitle("Some Message")
                .hasDetail("some detail")
                .hasUuidInstance();
        }

        @Test void shouldMapPrivateDetailMethod() {
            testPost("/custom/private-detail-method")
                .hasStatus(BAD_REQUEST)
                .hasContentType(PROBLEM_DETAIL_JSON)
                .hasType("urn:problem-type:some-message")
                .hasTitle("Some Message")
                .hasDetail("some detail")
                .hasUuidInstance();
        }

        @Test void shouldMapFailingDetailMethod() {
            testPost("/custom/failing-detail-method")
                .hasStatus(BAD_REQUEST)
                .hasContentType(PROBLEM_DETAIL_JSON)
                .hasType("urn:problem-type:failing-detail")
                .hasTitle("Failing Detail")
                .hasDetail("could not invoke FailingDetailException.failingDetail: java.lang.RuntimeException: inner")
                .hasUuidInstance();
        }

        @Test void shouldMapPublicDetailFieldOverridingMessage() {
            testPost("/custom/public-detail-field")
                .hasStatus(BAD_REQUEST)
                .hasContentType(PROBLEM_DETAIL_JSON)
                .hasType("urn:problem-type:some-message")
                .hasTitle("Some Message")
                .hasDetail("some detail")
                .hasUuidInstance();
        }

        @Test void shouldMapPrivateDetailField() {
            testPost("/custom/private-detail-field")
                .hasStatus(BAD_REQUEST)
                .hasContentType(PROBLEM_DETAIL_JSON)
                .hasType("urn:problem-type:some-message")
                .hasTitle("Some Message")
                .hasDetail("some detail")
                .hasUuidInstance();
        }

        @Test void shouldMapMultipleDetailFields() {
            testPost("/custom/multi-detail-fields")
                .hasStatus(BAD_REQUEST)
                .hasContentType(PROBLEM_DETAIL_JSON)
                .hasType("urn:problem-type:some-message")
                .hasTitle("Some Message")
                .hasDetail("detail a detail b")
                .hasUuidInstance();
        }

        @Test void shouldMapDetailMethodAndTwoFields() {
            testPost("/custom/mixed-details")
                .hasStatus(BAD_REQUEST)
                .hasContentType(PROBLEM_DETAIL_JSON)
                .hasType("urn:problem-type:some-message")
                .hasTitle("Some Message")
                .hasDetail("detail a detail b detail c")
                .hasUuidInstance();
        }

        @Test void shouldFailToMapDetailMethodTakingAnArgument() {
            testPost("/custom/detail-method-arg")
                .hasStatus(BAD_REQUEST)
                .hasContentType(PROBLEM_DETAIL_JSON)
                .hasType("urn:problem-type:some-message")
                .hasTitle("Some Message")
                .hasDetail("could not invoke SomeMessageException.detail: expected no args but got 1")
                .hasUuidInstance();
        }
    }

    @Nested class ExplicitInstance {
        @Test void shouldMapInstanceMethod() {
            testPost("/custom/explicit-instance-method")
                .hasStatus(BAD_REQUEST)
                .hasContentType(PROBLEM_DETAIL_JSON)
                .hasType("urn:problem-type:some")
                .hasTitle("Some")
                .hasNoDetail()
                .hasInstance("foobar");
        }

        @Test void shouldMapInstanceField() {
            testPost("/custom/explicit-instance-field")
                .hasStatus(BAD_REQUEST)
                .hasContentType(PROBLEM_DETAIL_JSON)
                .hasType("urn:problem-type:some")
                .hasTitle("Some")
                .hasNoDetail()
                .hasInstance("foobar");
        }

        @Test void shouldMapNullInstanceMethod() {
            testPost("/custom/null-instance-method")
                .hasStatus(BAD_REQUEST)
                .hasContentType(PROBLEM_DETAIL_JSON)
                .hasType("urn:problem-type:some")
                .hasTitle("Some")
                .hasNoDetail()
                .hasNoInstance();
        }

        @Test void shouldMapNullInstanceField() {
            testPost("/custom/null-instance-field")
                .hasStatus(BAD_REQUEST)
                .hasContentType(PROBLEM_DETAIL_JSON)
                .hasType("urn:problem-type:some")
                .hasTitle("Some")
                .hasNoDetail()
                .hasNoInstance();
        }

        @Test void shouldMapTwoInstanceFieldsOneNull() {
            testPost("/custom/two-instance-fields-one-null")
                .hasStatus(BAD_REQUEST)
                .hasContentType(PROBLEM_DETAIL_JSON)
                .hasType("urn:problem-type:some")
                .hasTitle("Some")
                .hasNoDetail()
                .hasInstance("foobar");
        }

        @Test void shouldMapTwoInstanceMethodsOneNull() {
            testPost("/custom/two-instance-methods-one-null")
                .hasStatus(BAD_REQUEST)
                .hasContentType(PROBLEM_DETAIL_JSON)
                .hasType("urn:problem-type:some")
                .hasTitle("Some")
                .hasNoDetail()
                .hasInstance("foobar");
        }
    }

    @Nested class Logging {
        @Test void shouldLogInstanceField() {
            testPost("/custom/explicit-instance-field");
            thenLogged(INFO, "org.eclipse.microprofile.problemdetails.tckapp.CustomExceptionBoundary$4SomeException")
                .type("urn:problem-type:some")
                .title("Some")
                .status("400")
                .instance("foobar")
                .noStackTrace()
                .check();
        }

        @EnumSource(value = LogLevel.class, mode = EXCLUDE, names = {
            "AUTO",
            "DEBUG" // might be disabled or not
        })
        @ParameterizedTest void shouldLogLevel(LogLevel logLevel) {
            testPost("/custom/log-level/" + logLevel.name());
            String camel = logLevel.name().substring(0, 1) + logLevel.name().substring(1).toLowerCase();
            String logCategory = "org.eclipse.microprofile.problemdetails.tckapp.CustomExceptionBoundary$1" + camel + "LogException";
            if (logLevel == OFF)
                thenNothingLoggedTo(logCategory);
            else
                thenLogged(logLevel, logCategory)
                    .type("urn:problem-type:" + logLevel.name().toLowerCase() + "-log")
                    .title(camel + " Log")
                    .status("400")
                    .noStackTrace()
                    .check();
        }
    }
}
