package io.restassured.filter;

public interface OrderedFilter extends Filter {

    /**
     * The default priority for any filter
     */
    public static final int DEFAULT_PRIORITY = 1000;

    /**
     * @return priority of the filter
     */
    public int getPriority();

}
