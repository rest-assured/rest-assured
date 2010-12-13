package com.jayway.restassured;

import org.apache.commons.jxpath.CompiledExpression;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathNotFoundException;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

import static org.apache.commons.jxpath.JXPathContext.newContext;

public class JsonXPath {
    private final CompiledExpression compiledExpression;
    private String xpathString;

    public JsonXPath(String xpath) {
        xpathString = xpath;
        this.compiledExpression = JXPathContext.compile(xpath);
    }

    public Object getValue(String src) {
        try {
            return compiledExpression.getValue(getJXPathContext(src));
        } catch (JsonProcessingException e) {
            return null;
        } catch (JXPathNotFoundException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JXPathContext getJXPathContext(String src)
            throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Object root = mapper.readValue(src, Object.class);
        JXPathContext context = newContext(root);
        return context;
    }
}