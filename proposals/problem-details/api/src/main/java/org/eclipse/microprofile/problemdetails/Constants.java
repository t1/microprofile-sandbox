package org.eclipse.microprofile.problemdetails;

import static java.lang.Boolean.TRUE;

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

    private static final String CONFIG_PREFIX = Constants.class.getPackage().getName() + ".";

    /**
     * The config option name to not fall back to use the messages of exceptions for the `detail` field.
     * Useful if you are not sure that they never contain security critical information in an existing
     * code base.
     */
    public static final String EXCEPTION_MESSAGE_AS_DETAIL = CONFIG_PREFIX + "exceptionMessageAsDetail";

    /**
     * The default value for {@link #EXCEPTION_MESSAGE_AS_DETAIL}
     */
    public static final Boolean EXCEPTION_MESSAGE_AS_DETAIL_DEFAULT = TRUE;
}
