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
import org.apache.ws.commons.schema.docpath.XmlSchemaStateMachineGenerator;
import org.apache.ws.commons.schema.docpath.XmlSchemaStateMachineNode;

import org.junit.Assert;
import org.junit.Test;

/**
 * An element declared without a type, but with a substitution group, takes
 * the type of its substitution group head.
 */
public class SubstitutionGroupTypeTest extends Assert {

    private static final String NS = "urn:substtype";

    private static XmlSchemaStateMachineGenerator walkRoot(String body) {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        collection.read(new StringReader(
            "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:tns=\"" + NS + "\""
            + " targetNamespace=\"" + NS + "\" elementFormDefault=\"qualified\">"
            + body
            + "</xs:schema>"));
        XmlSchemaElement root = collection.getElementByQName(new QName(NS, "root"));
        assertNotNull("the schema under test must declare a 'root' element", root);

        XmlSchemaStateMachineGenerator generator = new XmlSchemaStateMachineGenerator();
        XmlSchemaWalker walker = new XmlSchemaWalker(collection, generator);
        walker.walk(root);
        return generator;
    }

    private static void assertRejected(String body, String expectedFragment) {
        try {
            walkRoot(body);
            fail("expected the substitution group to be rejected");
        } catch (XmlSchemaException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains(expectedFragment));
        }
    }

    @Test
    public void testUntypedMemberInheritsSimpleTypeOfHead() {
        XmlSchemaStateMachineGenerator generator =
            walkRoot("<xs:element name=\"head\" type=\"xs:boolean\" abstract=\"true\"/>"
                     + "<xs:element name=\"title\" substitutionGroup=\"tns:head\"/>"
                     + "<xs:element name=\"root\"><xs:complexType><xs:sequence>"
                     + "<xs:element ref=\"tns:head\"/>"
                     + "</xs:sequence></xs:complexType></xs:element>");

        XmlSchemaStateMachineNode title =
            generator.getStateMachineNodesByQName().get(new QName(NS, "title"));
        assertNotNull("the substitute element must be walked", title);
        assertEquals(XmlSchemaTypeInfo.Type.ATOMIC, title.getElementType().getType());
        assertEquals(XmlSchemaBaseSimpleType.BOOLEAN, title.getElementType().getBaseType());
    }

    @Test
    public void testUntypedMemberInheritsComplexTypeThroughChain() {
        XmlSchemaStateMachineGenerator generator =
            walkRoot("<xs:complexType name=\"HeadType\"><xs:sequence>"
                     + "<xs:element name=\"leaf\" type=\"xs:string\"/>"
                     + "</xs:sequence><xs:attribute name=\"id\" type=\"xs:string\"/></xs:complexType>"
                     + "<xs:element name=\"head\" type=\"tns:HeadType\" abstract=\"true\"/>"
                     + "<xs:element name=\"middle\" substitutionGroup=\"tns:head\" abstract=\"true\"/>"
                     + "<xs:element name=\"title\" substitutionGroup=\"tns:middle\"/>"
                     + "<xs:element name=\"root\"><xs:complexType><xs:sequence>"
                     + "<xs:element ref=\"tns:head\"/>"
                     + "</xs:sequence></xs:complexType></xs:element>");

        XmlSchemaStateMachineNode title =
            generator.getStateMachineNodesByQName().get(new QName(NS, "title"));
        assertNotNull("the substitute element must be walked", title);
        assertEquals(XmlSchemaTypeInfo.Type.COMPLEX, title.getElementType().getType());
        assertEquals(1, title.getAttributes().size());
        assertNotNull("the head's content model must be walked for the substitute",
                      generator.getStateMachineNodesByQName().get(new QName(NS, "leaf")));
    }

    @Test
    public void testUntypedMemberInheritsAnonymousTypeOfHead() {
        XmlSchemaStateMachineGenerator generator =
            walkRoot("<xs:element name=\"head\"><xs:complexType><xs:sequence>"
                     + "<xs:element name=\"leaf\" type=\"xs:string\"/>"
                     + "<xs:element ref=\"tns:head\" minOccurs=\"0\"/>"
                     + "</xs:sequence></xs:complexType></xs:element>"
                     + "<xs:element name=\"title\" substitutionGroup=\"tns:head\"/>"
                     + "<xs:element name=\"root\"><xs:complexType><xs:sequence>"
                     + "<xs:element ref=\"tns:head\"/>"
                     + "</xs:sequence></xs:complexType></xs:element>");

        XmlSchemaStateMachineNode head =
            generator.getStateMachineNodesByQName().get(new QName(NS, "head"));
        XmlSchemaStateMachineNode title =
            generator.getStateMachineNodesByQName().get(new QName(NS, "title"));
        assertNotNull("the head element must be walked", head);
        assertNotNull("the substitute element must be walked", title);
        assertSame(head.getElementType(), title.getElementType());
    }

    @Test
    public void testLongUntypedChainResolvesEachHeadOnce() {
        StringBuilder body = new StringBuilder("<xs:element name=\"root\" type=\"xs:string\"/>");
        String previous = "root";
        for (int i = 0; i < 2000; i++) {
            body.append("<xs:element name=\"e").append(i).append("\" substitutionGroup=\"tns:")
                .append(previous).append("\"/>");
            previous = "e" + i;
        }
        XmlSchemaStateMachineGenerator generator = walkRoot(body.toString());

        XmlSchemaStateMachineNode last =
            generator.getStateMachineNodesByQName().get(new QName(NS, previous));
        assertNotNull("the last element of the chain must be walked", last);
        assertEquals(XmlSchemaBaseSimpleType.STRING, last.getElementType().getBaseType());
    }

    @Test
    public void testMissingSubstitutionGroupHeadIsRejected() {
        assertRejected("<xs:element name=\"root\" substitutionGroup=\"tns:missing\"/>",
                       "does not resolve to an element");
    }

    @Test
    public void testCyclicUntypedSubstitutionGroupIsRejected() {
        assertRejected("<xs:element name=\"a\" substitutionGroup=\"tns:b\"/>"
                       + "<xs:element name=\"root\" substitutionGroup=\"tns:a\"/>"
                       + "<xs:element name=\"b\" substitutionGroup=\"tns:root\"/>",
                       "Cyclic substitution group");
    }
}
