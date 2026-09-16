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
 * xmlschema-core does not check that references resolve, so a schema carrying a
 * dangling ref parses cleanly and only fails when the walker dereferences it. Those
 * failures must stay inside the documented XmlSchemaException surface rather than
 * escaping as NullPointerException or IllegalStateException.
 */
public class DanglingReferenceWalkerTest extends Assert {

    private static void walkRoot(String body) {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        collection.read(new StringReader(
            "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:tns=\"urn:dangling\""
            + " targetNamespace=\"urn:dangling\" elementFormDefault=\"qualified\">"
            + body
            + "</xs:schema>"));
        XmlSchemaElement root =
            collection.getElementByQName(new QName("urn:dangling", "root"));
        assertNotNull("the schema under test must declare a 'root' element", root);
        new XmlSchemaWalker(collection).walk(root);
    }

    private static void assertRejected(String body, String expectedFragment) {
        try {
            walkRoot(body);
            fail("expected the dangling reference to be rejected");
        } catch (XmlSchemaException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains(expectedFragment));
        }
    }

    @Test
    public void testDanglingAttributeGroupReferenceIsRejected() {
        assertRejected("<xs:complexType name=\"t\">"
                       + "<xs:attributeGroup ref=\"tns:missing\"/>"
                       + "</xs:complexType>"
                       + "<xs:element name=\"root\" type=\"tns:t\"/>",
                       "attribute group reference");
    }

    @Test
    public void testDanglingGroupReferenceIsRejected() {
        assertRejected("<xs:complexType name=\"t\"><xs:sequence>"
                       + "<xs:group ref=\"tns:missing\"/>"
                       + "</xs:sequence></xs:complexType>"
                       + "<xs:element name=\"root\" type=\"tns:t\"/>",
                       "group reference");
    }

    @Test
    public void testDanglingElementReferenceIsRejected() {
        assertRejected("<xs:complexType name=\"t\"><xs:sequence>"
                       + "<xs:element ref=\"tns:missing\"/>"
                       + "</xs:sequence></xs:complexType>"
                       + "<xs:element name=\"root\" type=\"tns:t\"/>",
                       "element reference");
    }

    @Test
    public void testDanglingAttributeReferenceIsRejected() {
        assertRejected("<xs:complexType name=\"t\">"
                       + "<xs:attribute ref=\"tns:missing\"/>"
                       + "</xs:complexType>"
                       + "<xs:element name=\"root\" type=\"tns:t\"/>",
                       "attribute reference");
    }

    @Test
    public void testDanglingTypeReferenceIsRejected() {
        assertRejected("<xs:element name=\"root\" type=\"tns:missing\"/>",
                       "has no type");
    }

    @Test
    public void testResolvableReferencesStillWalk() {
        walkRoot("<xs:attributeGroup name=\"ag\">"
                 + "<xs:attribute name=\"a\" type=\"xs:string\"/>"
                 + "</xs:attributeGroup>"
                 + "<xs:group name=\"g\"><xs:sequence>"
                 + "<xs:element name=\"inner\" type=\"xs:string\"/>"
                 + "</xs:sequence></xs:group>"
                 + "<xs:element name=\"global\" type=\"xs:string\"/>"
                 + "<xs:complexType name=\"t\"><xs:sequence>"
                 + "<xs:group ref=\"tns:g\"/><xs:element ref=\"tns:global\"/>"
                 + "</xs:sequence><xs:attributeGroup ref=\"tns:ag\"/></xs:complexType>"
                 + "<xs:element name=\"root\" type=\"tns:t\"/>");
    }
}
