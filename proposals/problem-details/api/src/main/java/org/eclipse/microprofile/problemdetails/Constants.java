package org.eclipse.microprofile.problemdetails;

public class Constants {
    /**
     * The JSON formatted details body of a failing http request.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7807">RFC-7807</a>
     */
    public static final String PROBLEM_DETAIL_JSON = "application/problem+json";

    /**
     * The XML formatted details body of a failing http request.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7807">RFC-7807</a>
     */
    public static final String PROBLEM_DETAIL_XML = "application/problem+xml";
}
