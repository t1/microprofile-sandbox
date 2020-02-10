package org.eclipse.microprofile.problemdetails.tck;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.assertj.core.api.BDDAssertions;
import org.eclipse.microprofile.problemdetails.tck.ContainerLaunchingExtension.ProblemDetailAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.singletonList;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.fail;
import static org.eclipse.microprofile.problemdetails.Constants.PROBLEM_DETAIL_JSON;
import static org.eclipse.microprofile.problemdetails.tck.ContainerLaunchingExtension.target;
import static org.eclipse.microprofile.problemdetails.tck.ContainerLaunchingExtension.thenProblemDetail;

@ExtendWith(ContainerLaunchingExtension.class)
class ValidationFailedExceptionMappingIT {
    @NoArgsConstructor @AllArgsConstructor
    public static @Data class Address {
        String street;
        int zipCode;
        String city;
    }

    @NoArgsConstructor @AllArgsConstructor
    public static @Data class Person {
        String firstName;
        String lastName;
        LocalDate born;
        List<Address> address;
    }

    @Test void shouldMapAnnotatedValidationFailedException() {
        Person person = new Person(null, "", LocalDate.now().plusDays(3),
            singletonList(new Address(null, -1, null)));
        WebTarget target = target("/validation");

        Response response = target.request(APPLICATION_JSON_TYPE)
            .post(entity(person, APPLICATION_JSON_TYPE));

        thenValidationFailed(response);
    }

    private void thenValidationFailed(Response response) {
        ProblemDetailAssert<ValidationProblemDetail> it = thenProblemDetail(response, ValidationProblemDetail.class)
            .hasStatus(BAD_REQUEST)
            .hasContentType(PROBLEM_DETAIL_JSON)
            .hasType("urn:problem-type:validation-failed")
            .hasTitle("Validation Failed")
            .hasUuidInstance();

        String mustNotBeNull = mustOrMay(it) + " not be null";

        switch (it.getDetail()) {
            case "2 violations failed": // rest-easy has a different mode to validate fields
                it.check(detail -> BDDAssertions.then(detail.violations).containsOnly(
                    entry("post.person.firstName", mustNotBeNull),
                    entry("post.person.lastName", "must not be empty")
                ));
                break;
            case "6 violations failed":
                it.check(detail -> BDDAssertions.then(detail.violations).containsOnly(
                    entry("firstName", mustNotBeNull),
                    entry("lastName", "must not be empty"),
                    entry("born", "must be a past date"),
                    entry("address[0].street", mustNotBeNull),
                    entry("address[0].zipCode", "must be greater than 0"),
                    entry("address[0].city", mustNotBeNull)
                ));
                break;
            default:
                fail("unexpected detail: [" + it.getDetail() + "]");
        }
    }

    /** some validators say 'may' others say 'must' :( */
    private String mustOrMay(ProblemDetailAssert<ValidationProblemDetail> it) {
        AtomicBoolean containsMust = new AtomicBoolean(true);
        it.check(detail -> {
            if (detail.violations != null) {
                containsMust.set(detail.violations.containsValue("must not be null"));
            }
        });
        return containsMust.get() ? "must" : "may";
    }

    @EqualsAndHashCode(callSuper = true)
    public static @Data class ValidationProblemDetail extends ProblemDetail {
        private Map<String, String> violations;
    }
}
