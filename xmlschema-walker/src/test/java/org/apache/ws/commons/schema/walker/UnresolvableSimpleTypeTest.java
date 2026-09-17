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

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaException;

import org.junit.Assert;
import org.junit.Test;

/**
 * Where a schema names a type that must be simple, the core parser checks neither that the
 * name resolves nor that what it resolves to is simple, so both reach the walker.
 */
public class UnresolvableSimpleTypeTest extends Assert {

    private static final String COMPLEX_TYPE =
        "<xs:complexType name=\"ct\"><xs:sequence/></xs:complexType>";

    private static void walk(String body) {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        collection.read(new StringReader(
            "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:t=\"urn:t\""
            + " targetNamespace=\"urn:t\" elementFormDefault=\"qualified\">"
            + body + "</xs:schema>"));
        XmlSchemaElement root = collection.getElementByQName(new QName("urn:t", "root"));
        assertNotNull(root);
        new XmlSchemaWalker(collection).walk(root);
    }

    private static void assertRejected(String body, String expectedFragment) {
        try {
            walk(body);
            fail("expected the unusable type reference to be rejected");
        } catch (XmlSchemaException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains(expectedFragment));
        }
    }

    @Test
    public void testListItemTypeThatDoesNotResolve() {
        assertRejected("<xs:simpleType name=\"l\"><xs:list itemType=\"t:missing\"/></xs:simpleType>"
                       + "<xs:element name=\"root\" type=\"t:l\"/>",
                       "does not resolve");
    }

    @Test
    public void testListItemTypeThatIsComplex() {
        assertRejected(COMPLEX_TYPE
                       + "<xs:simpleType name=\"l\"><xs:list itemType=\"t:ct\"/></xs:simpleType>"
                       + "<xs:element name=\"root\" type=\"t:l\"/>",
                       "complex type");
    }

    @Test
    public void testListOfListIsRejected() {
        assertRejected("<xs:simpleType name=\"l1\"><xs:list itemType=\"xs:int\"/></xs:simpleType>"
                       + "<xs:simpleType name=\"l2\"><xs:list itemType=\"t:l1\"/></xs:simpleType>"
                       + "<xs:element name=\"root\" type=\"t:l2\"/>",
                       "atomic or a union");
    }

    @Test
    public void testUnionMemberThatDoesNotResolve() {
        assertRejected("<xs:simpleType name=\"u\"><xs:union memberTypes=\"t:missing\"/></xs:simpleType>"
                       + "<xs:element name=\"root\" type=\"t:u\"/>",
                       "does not resolve");
    }

    @Test
    public void testUnionMemberThatIsComplex() {
        assertRejected(COMPLEX_TYPE
                       + "<xs:simpleType name=\"u\"><xs:union memberTypes=\"xs:int t:ct\"/></xs:simpleType>"
                       + "<xs:element name=\"root\" type=\"t:u\"/>",
                       "complex type");
    }

    @Test
    public void testRestrictionBaseThatDoesNotResolve() {
        assertRejected("<xs:simpleType name=\"s\"><xs:restriction base=\"t:missing\"/></xs:simpleType>"
                       + "<xs:element name=\"root\" type=\"t:s\"/>",
                       "does not resolve");
    }

    @Test
    public void testRestrictionBaseThatIsComplex() {
        assertRejected(COMPLEX_TYPE
                       + "<xs:simpleType name=\"s\"><xs:restriction base=\"t:ct\"/></xs:simpleType>"
                       + "<xs:element name=\"root\" type=\"t:s\"/>",
                       "complex type");
    }

    @Test
    public void testAttributeTypedByAComplexType() {
        assertRejected(COMPLEX_TYPE
                       + "<xs:element name=\"root\"><xs:complexType>"
                       + "<xs:attribute name=\"a\" type=\"t:ct\"/></xs:complexType></xs:element>",
                       "complex type");
    }

    @Test
    public void testSimpleContentRestrictionOfAComplexBase() {
        assertRejected("<xs:complexType name=\"b\"><xs:complexContent>"
                       + "<xs:restriction base=\"xs:anyType\"><xs:sequence/></xs:restriction>"
                       + "</xs:complexContent></xs:complexType>"
                       + "<xs:complexType name=\"ct2\"><xs:simpleContent>"
                       + "<xs:restriction base=\"t:b\"/></xs:simpleContent></xs:complexType>"
                       + "<xs:element name=\"root\" type=\"t:ct2\"/>",
                       "Cannot restrict");
    }

    @Test
    public void testWellFormedSimpleTypesStillWalk() {
        walk("<xs:simpleType name=\"u\"><xs:union memberTypes=\"xs:int xs:string\"/></xs:simpleType>"
             + "<xs:simpleType name=\"l\"><xs:list itemType=\"t:u\"/></xs:simpleType>"
             + "<xs:simpleType name=\"r\"><xs:restriction base=\"xs:string\">"
             + "<xs:maxLength value=\"8\"/></xs:restriction></xs:simpleType>"
             + "<xs:element name=\"root\"><xs:complexType><xs:sequence>"
             + "<xs:element name=\"a\" type=\"t:l\"/><xs:element name=\"b\" type=\"t:r\"/>"
             + "</xs:sequence><xs:attribute name=\"c\" type=\"t:u\"/></xs:complexType></xs:element>");
    }
}
