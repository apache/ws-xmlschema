/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ws.commons.schema.walker;

import java.io.StringReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchemaAnyAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaException;

import org.junit.Assert;
import org.junit.Test;

/**
 * A complex content extension combines the content model and attribute wildcard of its base
 * with its own. Doing so must not escape as a ClassCastException or NullPointerException.
 */
public class ComplexContentExtensionWalkerTest extends Assert {

    private static List<String> walkRoot(String body) {
        // Records the namespace of every attribute wildcard visited.
        final List<String> anyAttributeNamespaces = new ArrayList<String>();
        walk(body, (proxy, method, args) -> {
            if ("onVisitAnyAttribute".equals(method.getName())) {
                anyAttributeNamespaces.add(((XmlSchemaAnyAttribute)args[1]).getNamespace());
            }
            return null;
        });
        return anyAttributeNamespaces;
    }

    /**
     * Walks the root element and returns the name of every visitor callback,
     * in order.
     */
    private static List<String> walkEvents(String body) {
        final List<String> events = new ArrayList<String>();
        walk(body, (proxy, method, args) -> {
            events.add(method.getName());
            return null;
        });
        return events;
    }

    private static void walk(String body, InvocationHandler handler) {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        collection.read(new StringReader(
            "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:tns=\"urn:ext\""
            + " targetNamespace=\"urn:ext\" elementFormDefault=\"qualified\">"
            + body
            + "</xs:schema>"));
        XmlSchemaElement root = collection.getElementByQName(new QName("urn:ext", "root"));
        assertNotNull("the schema under test must declare a 'root' element", root);

        XmlSchemaVisitor visitor = (XmlSchemaVisitor)Proxy.newProxyInstance(
            XmlSchemaVisitor.class.getClassLoader(), new Class<?>[] {XmlSchemaVisitor.class}, handler);
        new XmlSchemaWalker(collection, visitor).walk(root);
    }

    @Test
    public void testExtendingAnAllGroupWithMoreContentIsRejected() {
        try {
            walkRoot("<xs:complexType name=\"base\"><xs:all>"
                     + "<xs:element name=\"a\" type=\"xs:string\"/>"
                     + "</xs:all></xs:complexType>"
                     + "<xs:complexType name=\"derived\"><xs:complexContent>"
                     + "<xs:extension base=\"tns:base\"><xs:sequence>"
                     + "<xs:element name=\"b\" type=\"xs:string\"/>"
                     + "</xs:sequence></xs:extension></xs:complexContent></xs:complexType>"
                     + "<xs:element name=\"root\" type=\"tns:derived\"/>");
            fail("expected an xs:all combined with other content to be rejected");
        } catch (XmlSchemaException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("xs:all"));
        }
    }

    /**
     * An empty particle adds no content, so XML Schema allows it alongside an
     * xs:all: the xs:all is then the whole content model.
     */
    @Test
    public void testExtendingAnAllGroupWithAnEmptySequenceWalks() {
        assertAllGroupWalked("<xs:complexType name=\"base\"><xs:all>"
                             + "<xs:element name=\"a\" type=\"xs:string\"/>"
                             + "<xs:element name=\"b\" type=\"xs:string\"/>"
                             + "</xs:all></xs:complexType>"
                             + "<xs:complexType name=\"derived\"><xs:complexContent>"
                             + "<xs:extension base=\"tns:base\"><xs:sequence/>"
                             + "<xs:attribute name=\"id\" type=\"xs:string\"/>"
                             + "</xs:extension></xs:complexContent></xs:complexType>"
                             + "<xs:element name=\"root\" type=\"tns:derived\"/>");
    }

    @Test
    public void testExtendingAnAllGroupWithAChoiceThatCannotOccurWalks() {
        assertAllGroupWalked("<xs:complexType name=\"base\"><xs:all>"
                             + "<xs:element name=\"a\" type=\"xs:string\"/>"
                             + "</xs:all></xs:complexType>"
                             + "<xs:complexType name=\"derived\"><xs:complexContent>"
                             + "<xs:extension base=\"tns:base\">"
                             + "<xs:choice minOccurs=\"0\" maxOccurs=\"0\"/>"
                             + "</xs:extension></xs:complexContent></xs:complexType>"
                             + "<xs:element name=\"root\" type=\"tns:derived\"/>");
    }

    @Test
    public void testExtendingAnEmptySequenceWithAnAllGroupWalks() {
        assertAllGroupWalked("<xs:complexType name=\"base\"><xs:sequence/>"
                             + "<xs:attribute name=\"id\" type=\"xs:string\"/>"
                             + "</xs:complexType>"
                             + "<xs:complexType name=\"derived\"><xs:complexContent>"
                             + "<xs:extension base=\"tns:base\"><xs:all>"
                             + "<xs:element name=\"a\" type=\"xs:string\"/>"
                             + "</xs:all></xs:extension></xs:complexContent></xs:complexType>"
                             + "<xs:element name=\"root\" type=\"tns:derived\"/>");
    }

    @Test
    public void testExtendingANonEmptySequenceWithAnAllGroupIsRejected() {
        try {
            walkRoot("<xs:complexType name=\"base\"><xs:sequence>"
                     + "<xs:element name=\"a\" type=\"xs:string\"/>"
                     + "</xs:sequence></xs:complexType>"
                     + "<xs:complexType name=\"derived\"><xs:complexContent>"
                     + "<xs:extension base=\"tns:base\"><xs:all>"
                     + "<xs:element name=\"b\" type=\"xs:string\"/>"
                     + "</xs:all></xs:extension></xs:complexContent></xs:complexType>"
                     + "<xs:element name=\"root\" type=\"tns:derived\"/>");
            fail("expected an xs:all combined with other content to be rejected");
        } catch (XmlSchemaException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("xs:all"));
        }
    }

    private static void assertAllGroupWalked(String body) {
        List<String> events = walkEvents(body);
        assertTrue(events.toString(), events.contains("onEnterAllGroup"));
        assertFalse(events.toString(), events.contains("onEnterSequenceGroup"));
    }

    @Test
    public void testExtendingASequenceWithASequenceStillWalks() {
        walkRoot("<xs:complexType name=\"base\"><xs:sequence>"
                 + "<xs:element name=\"a\" type=\"xs:string\"/>"
                 + "</xs:sequence></xs:complexType>"
                 + "<xs:complexType name=\"derived\"><xs:complexContent>"
                 + "<xs:extension base=\"tns:base\"><xs:sequence>"
                 + "<xs:element name=\"b\" type=\"xs:string\"/>"
                 + "</xs:sequence></xs:extension></xs:complexContent></xs:complexType>"
                 + "<xs:element name=\"root\" type=\"tns:derived\"/>");
    }

    /**
     * An xs:anyAttribute with no namespace attribute allows any namespace, and so does its union
     * with any other wildcard.
     */
    @Test
    public void testAnyAttributeWithoutANamespaceIsAnyNamespace() {
        List<String> namespaces =
            walkRoot("<xs:complexType name=\"base\"><xs:sequence/>"
                     + "<xs:anyAttribute/></xs:complexType>"
                     + "<xs:complexType name=\"derived\"><xs:complexContent>"
                     + "<xs:extension base=\"tns:base\">"
                     + "<xs:anyAttribute namespace=\"urn:other\" processContents=\"lax\"/>"
                     + "</xs:extension></xs:complexContent></xs:complexType>"
                     + "<xs:element name=\"root\" type=\"tns:derived\"/>");
        assertEquals(1, namespaces.size());
        assertEquals("##any", namespaces.get(0));
    }

    @Test
    public void testAnyAttributeNamespacesAreStillUnited() {
        List<String> namespaces =
            walkRoot("<xs:complexType name=\"base\"><xs:sequence/>"
                     + "<xs:anyAttribute namespace=\"urn:a\"/></xs:complexType>"
                     + "<xs:complexType name=\"derived\"><xs:complexContent>"
                     + "<xs:extension base=\"tns:base\">"
                     + "<xs:anyAttribute namespace=\"urn:b\"/>"
                     + "</xs:extension></xs:complexContent></xs:complexType>"
                     + "<xs:element name=\"root\" type=\"tns:derived\"/>");
        assertEquals(1, namespaces.size());
        String[] united = namespaces.get(0).trim().split(" ");
        Arrays.sort(united);
        assertArrayEquals(new String[] {"urn:a", "urn:b"}, united);
    }
}
