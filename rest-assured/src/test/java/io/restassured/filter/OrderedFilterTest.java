package io.restassured.filter;

import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

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

        public OrderedFilterImpl(int priority) {
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
        RestAssured.filters(Arrays.asList(f3));

        assertThat(RestAssured.filters().get(0), Matchers.is(f1));
        assertThat(RestAssured.filters().get(1), Matchers.is(f2));
        assertThat(RestAssured.filters().get(2), Matchers.is(f3));

        RestAssured.replaceFiltersWith(f1, f2, f3);

        assertThat(RestAssured.filters().get(0), Matchers.is(f1));
        assertThat(RestAssured.filters().get(1), Matchers.is(f2));
        assertThat(RestAssured.filters().get(2), Matchers.is(f3));
    }

    @Test
    public void testOrderedFilterOrder() {
        Filter f1 = new OrderedFilterImpl(1);
        Filter f2 = new OrderedFilterImpl(2);
        Filter f3 = new OrderedFilterImpl(3);

        RestAssured.filters(f2, f3);
        RestAssured.filters(Arrays.asList(f1));

        assertThat(RestAssured.filters().get(0), Matchers.is(f1));
        assertThat(RestAssured.filters().get(1), Matchers.is(f2));
        assertThat(RestAssured.filters().get(2), Matchers.is(f3));

        RestAssured.replaceFiltersWith(f1, f2, f3);

        assertThat(RestAssured.filters().get(0), Matchers.is(f1));
        assertThat(RestAssured.filters().get(1), Matchers.is(f2));
        assertThat(RestAssured.filters().get(2), Matchers.is(f3));

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

        assertThat(RestAssured.filters().get(0), Matchers.is(f1));
        assertThat(RestAssured.filters().get(1), Matchers.is(f2));
        assertThat(RestAssured.filters().get(2), Matchers.is(f3));
        assertThat(RestAssured.filters().get(3), Matchers.is(f4));
        assertThat(RestAssured.filters().get(4), Matchers.is(f5));
        assertThat(RestAssured.filters().get(5), Matchers.is(f6));
        assertThat(RestAssured.filters().get(6), Matchers.is(f7));
        assertThat(RestAssured.filters().get(7), Matchers.is(f8));
        assertThat(RestAssured.filters().get(8), Matchers.is(f9));
    }

}
