package io.restassured.filter;

/**
 * Implement this filter if you need to control the order of the filter.
 * Higher values are interpreted as lower priority. As a consequence, the object with the lowest value has the highest priority (somewhat analogous to Servlet load-on-startup values).
 * Same order values will result in arbitrary sort positions for the affected objects.
 */
public interface OrderedFilter extends Filter {

    /**
     * The default precedence
     */
    int DEFAULT_PRECEDENCE = 1000;

    /**
     * Useful constant for the highest precedence value.
     *
     * @see Integer#MIN_VALUE
     */
    int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;

    /**
     * Useful constant for the lowest precedence value.
     *
     * @see Integer#MAX_VALUE
     */
    int LOWEST_PRECEDENCE = Integer.MAX_VALUE;

    /**
     * @return The order of the filter
     */
    int getOrder();
}
