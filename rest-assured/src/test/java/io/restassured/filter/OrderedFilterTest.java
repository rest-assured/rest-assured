package io.restassured.filter;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class OrderedFilterTest {

    private static List<Filter> filters;

    public static class FilterImpl implements Filter {
        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec,
                FilterContext ctx) {
            return null;
        }
    }

    public static class OrderedFilterImpl implements OrderedFilter {

        private int priority;

        OrderedFilterImpl(int priority) {
            this.priority = priority;
        }

        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec,
                FilterContext ctx) {
            return null;
        }

        public int getPriority() {
            return priority;
        }

    }

    @BeforeClass
    public static void beforeClass() {
        filters = RestAssured.filters();
        RestAssured.replaceFiltersWith(new LinkedList<Filter>());
    }

    @AfterClass
    public static void afterClass() {
        RestAssured.replaceFiltersWith(filters);
        filters = null;
    }

    @After
    public void afterTest() {
        RestAssured.replaceFiltersWith(new LinkedList<Filter>());
    }

    @Test
    public void testRegularFilterOrder() {
        // Ensures that existing behavior is not changed
        Filter f1 = new FilterImpl();
        Filter f2 = new FilterImpl();
        Filter f3 = new FilterImpl();

        RestAssured.filters(f1, f2);
        RestAssured.filters(Collections.singletonList(f3));

        assertThat(RestAssured.filters().get(0), is(f1));
        assertThat(RestAssured.filters().get(1), is(f2));
        assertThat(RestAssured.filters().get(2), is(f3));

        RestAssured.replaceFiltersWith(f1, f2, f3);

        assertThat(RestAssured.filters().get(0), is(f1));
        assertThat(RestAssured.filters().get(1), is(f2));
        assertThat(RestAssured.filters().get(2), is(f3));
    }

    @Test
    public void testOrderedFilterOrder() {
        Filter f1 = new OrderedFilterImpl(1);
        Filter f2 = new OrderedFilterImpl(2);
        Filter f3 = new OrderedFilterImpl(3);

        RestAssured.filters(f2, f3);
        RestAssured.filters(Collections.singletonList(f1));

        assertThat(RestAssured.filters().get(0), is(f1));
        assertThat(RestAssured.filters().get(1), is(f2));
        assertThat(RestAssured.filters().get(2), is(f3));

        RestAssured.replaceFiltersWith(f1, f2, f3);

        assertThat(RestAssured.filters().get(0), is(f1));
        assertThat(RestAssured.filters().get(1), is(f2));
        assertThat(RestAssured.filters().get(2), is(f3));

    }

    @Test
    public void testMixedFilterOrder() {
        Filter f1 = new OrderedFilterImpl(1);
        Filter f2 = new OrderedFilterImpl(2);
        Filter f3 = new OrderedFilterImpl(3);

        Filter f4 = new FilterImpl();
        Filter f5 = new FilterImpl();
        Filter f6 = new FilterImpl();

        Filter f7 = new OrderedFilterImpl(1001);
        Filter f8 = new OrderedFilterImpl(1002);
        Filter f9 = new OrderedFilterImpl(1003);

        RestAssured.filters(f3, f9, f4, f7, f2, f5, f1, f6, f8);

        assertThat(RestAssured.filters().get(0), is(f1));
        assertThat(RestAssured.filters().get(1), is(f2));
        assertThat(RestAssured.filters().get(2), is(f3));
        assertThat(RestAssured.filters().get(3), is(f4));
        assertThat(RestAssured.filters().get(4), is(f5));
        assertThat(RestAssured.filters().get(5), is(f6));
        assertThat(RestAssured.filters().get(6), is(f7));
        assertThat(RestAssured.filters().get(7), is(f8));
        assertThat(RestAssured.filters().get(8), is(f9));
    }

}
