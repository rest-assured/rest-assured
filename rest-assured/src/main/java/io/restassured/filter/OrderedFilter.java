package io.restassured.filter;

/**
 * Implement this filter if you need to control the order of the filter.
 * A higher priority will apply the filter earlier in the chain.
 */
public interface OrderedFilter extends Filter {

    /**
     * The default priority for any filter
     */
    int DEFAULT_PRIORITY = 1000;

    /**
     * @return priority of the filter
     */
    int getPriority();
}
