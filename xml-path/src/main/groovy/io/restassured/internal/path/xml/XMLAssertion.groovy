/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.restassured.internal.path.xml

import groovy.util.slurpersupport.*
import io.restassured.internal.common.assertion.Assertion
import io.restassured.internal.path.xml.NodeChildrenImpl
import io.restassured.internal.path.xml.NodeImpl
import io.restassured.path.xml.element.PathElement
import org.apache.commons.lang3.StringUtils

import static io.restassured.internal.common.assertion.AssertionSupport.*

class XMLAssertion implements Assertion {
    private static final String DOT = "."
    private static final String EXPLICIT_LIST_CONVERSION = 'list()'
    String key;
    Map<String, Object> params;

    /* Matches fragment such as children() or size(2) */
    private def isInvocationFragment = ~/.*\(\d*\)|.*(\{|\}).*/

    private def fragments

    /**
     * @param object The object to get the result from
     * @param shouldConvertToJavaObject Should the result be converted to Java Object or should we retain the returned Groovy object
     * @param rootEvaluation True if we're evaluating from a root, false if start node is a child node.
     */
    def Object getResult(Object object, boolean shouldConvertToJavaObject, boolean rootEvaluation) {
        def objectToUse = object instanceof groovy.util.slurpersupport.Node ? new NodeChild(object, null, null) : object
        if (rootEvaluation) {
            key = key?.startsWith(DOT) ? key.substring(1) : key
        }
        key = escapePath(key, hyphen(), attributeGetter(), doubleStar(), colon(), classKeyword())
        def indexOfDot = key.indexOf(".")
        def evaluationString
        def isRootOnly = rootEvaluation ? (indexOfDot < 0) : false
        if (!isRootOnly) {
            fragments = key.split("\\.");
            def firstFragment = fragments[0];
            if (isDoubleStarFragment(firstFragment) || !isPathFragment(firstFragment)) {
                evaluationString = key.startsWith(DOT) ?: DOT + key; // Add a dot if needed because the first path fragment is actually a method invocation
            } else if (rootEvaluation) {
                evaluationString = key.substring(indexOfDot);
            } else {
                evaluationString = key.startsWith(".") ? key : "." + key;
            }
        } else {
            evaluationString = "";
        }

        def result;
        def rootObjectVariableName = "restAssuredXmlRootObject"
        try {
            result = eval(rootObjectVariableName, objectToUse, "$rootObjectVariableName$evaluationString")
        } catch (MissingPropertyException e) {
            // This means that a param was used that was not defined
            String error = String.format("The parameter \"%s\" was used but not defined. Define parameters using the XmlPath.params(...) function", e.property);
            throw new IllegalArgumentException(error, e);
        } catch (Exception e) {
            def errorMessage = e.getMessage();
            if (errorMessage.startsWith("No signature of method:")) {
                errorMessage = "Path $key is invalid."
            } else {
                def boolean hasRootObjectVariableName = StringUtils.contains(errorMessage, rootObjectVariableName)
                def replacement = rootEvaluation ? fragments[0].toString() + "." : ""
                errorMessage = e.getMessage().replace("startup failed:", "Invalid path:").replace("Script1.groovy: 1: ", "").
                        replace(rootObjectVariableName+".", replacement).replace("Object.", replacement)

                // Since we've replaced "Object" with root node name we need to move the ^ indicator.
                def originalIndicatorIndex = errorMessage.indexOf("^");
                if (originalIndicatorIndex > 0) {
                    def int lhs = rootEvaluation ? replacement.length() : 0
                    def int indicatorDelta = lhs - (hasRootObjectVariableName ? rootObjectVariableName.length() + 1 : "Object.".length())
                    if (indicatorDelta > 0) {
                        errorMessage = StringUtils.replace(errorMessage, "^", generateWhitespace(indicatorDelta) + "^")
                    } else if (indicatorDelta < 0) {
                        errorMessage = StringUtils.replace(errorMessage, generateWhitespace(Math.abs(indicatorDelta)) + "^", "^")
                    }
                }

            }
            throw new IllegalArgumentException(errorMessage);
        }
        def convertedObject = shouldConvertToJavaObject ? convertToJavaObject(result) : result
        return preventTreatingRootObjectAsAList(convertedObject)
    }

    def Object getResult(object, config) {
        return getResult(object, true, true)
    }

    def Object getChildResultAsJavaObject(Object object) {
        return getResult(object, true, false)
    }

    private def isDoubleStarFragment(String fragment) {
        def trimmed = fragment.trim()
        return trimmed == "**" || trimmed == "'**'"
    }

    private def preventTreatingRootObjectAsAList(javaObject) {
        if (javaObject instanceof List && javaObject.size() == 1 && fragments[-1] != EXPLICIT_LIST_CONVERSION) {
            javaObject = javaObject.get(0)
        }
        return javaObject
    }

    boolean isPathFragment(String fragment) {
        return !isInvocationFragment.matcher(fragment).matches()
    }

    private def convertToJavaObject(result) {
        def returnValue;
        if (Attributes.class.isAssignableFrom(result.getClass())) {
            returnValue = toJavaObject(result, true, false)
        } else if (result instanceof Node) {
            returnValue = nodeToJavaObject(result)
        } else if (result instanceof FilteredNodeChildren) {
            returnValue = toJavaObject(result, false, true)
        } else if (result instanceof NodeChild) {
            def object = toJavaObject(result, false, false)
            if (object instanceof PathElement) {
                returnValue = object.get(0)
            } else {
                returnValue = object
            }
        } else if (result instanceof GPathResult) {
            returnValue = toJavaObject(result, false, false)
        } else if (result instanceof List) {
            returnValue = handleList(result)
        } else {
            returnValue = result;
        }

        return returnValue
    }

    private def handleList(List result) {
        if (result.size() == 1 && fragments[-1] != EXPLICIT_LIST_CONVERSION) {
            return convertToJavaObject(result.get(0))
        } else {
            for (int i = 0; i < result.size(); i++) {
                result.set(i, convertToJavaObject(result.get(i)))
            }
        }

        result
    }

    private def nodeToJavaObject(node) {
        def nodeImpl = new NodeImpl(name: node.name(), groovyNode: node)
        addAttributes(nodeImpl, node)
        for (Object child : node.children()) {
            if (child instanceof Node) {
                def object = convertToJavaObject(child)
                nodeImpl.children << object
            } else {
                nodeImpl.value = child
            }
        }
        nodeImpl
    }

    private def addAttributes(nodeImpl, node) {
        def attributes = node.attributes();
        nodeImpl.attributes = convertToJavaObject(attributes)
    }

    private def toJavaObject(nodes, isAttributes, forceList) {
        if (nodes.size() == 1 && !hasChildren(nodes, isAttributes)) {
            return nodes.text()
        } else {
            return toJavaList(nodes, isAttributes, forceList)
        }
    }

    private boolean hasChildren(nodes, isAttributes) {
        if (isAttributes) {
            return false;
        }
        return !nodes.children().isEmpty()
    }

    private def toJavaList(nodes, isAttributes, forceList) {
        def nodeList
        if (forceList) {
            nodeList = []
        } else if (nodes instanceof NodeChild) {
            def nodeImpl = new NodeImpl(name: nodes.name(), groovyNode: nodes)
            addAttributes(nodeImpl, nodes)
            nodeList = nodeImpl
        } else {
            nodeList = new NodeChildrenImpl(groovyNodes: nodes)
        }

        if (isAttributes) {
            def temp
            if(nodes.isEmpty()) {
                temp = null;
            } else {
                temp = []
            }

            nodes.each {
                CharArrayWriter caw = new CharArrayWriter();
                it.writeTo(caw);
                caw.close();
                temp << caw.toString()
            }
            return temp
        } else {
            nodes.nodeIterator().each {
                def object = convertToJavaObject(it)
                nodeList << object
            }
        }
        nodeList
    }

    private def eval(root, object, expr) {
        Map<String, Object> newParams;
        // Create parameters from given ones
        if(params!=null) {
            newParams=new HashMap<>(params);
        } else {
            newParams=new HashMap<>();
        }
        // Add object to evaluate
        newParams.put(root, object);
        // Create shell with variables set
        GroovyShell sh = new GroovyShell(new Binding(newParams));
        // Run
        def res = sh.evaluate(expr)
        sh.resetLoadedClasses()
        return res;
    }

    def String description() {
        return "XML path"
    }
}

class XmlEntity {
    def children
    def attributes

}
