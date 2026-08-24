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

public class CyclicSchemaWalkerTest extends Assert {

    private static final String XSD_HEADER =
        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\""
        + " xmlns:tns=\"urn:cyclic\" targetNamespace=\"urn:cyclic\">";

    private XmlSchemaElement load(String schemaBody, String elementName,
                                  XmlSchemaCollection collection) {
        collection.read(new StringReader(XSD_HEADER + schemaBody + "</xs:schema>"));
        XmlSchemaElement element =
            collection.getElementByQName(new QName("urn:cyclic", elementName));
        assertNotNull(element);
        return element;
    }

    @Test
    public void testRecursiveContentModelThroughElementWalks() {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        XmlSchemaElement element =
            load("<xs:group name=\"content\"><xs:sequence>"
                 + "<xs:element ref=\"tns:section\"/>"
                 + "</xs:sequence></xs:group>"
                 + "<xs:element name=\"section\"><xs:complexType><xs:sequence>"
                 + "<xs:group ref=\"tns:content\" minOccurs=\"0\"/>"
                 + "</xs:sequence></xs:complexType></xs:element>"
                 + "<xs:element name=\"root\"><xs:complexType><xs:sequence>"
                 + "<xs:group ref=\"tns:content\"/>"
                 + "</xs:sequence></xs:complexType></xs:element>",
                 "root", collection);

        new XmlSchemaWalker(collection).walk(element);
    }

    @Test
    public void testNestedValidSubstitutionGroupReferenceWalks() {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        XmlSchemaElement element =
            load("<xs:complexType name=\"HeadType\"><xs:sequence>"
                 + "<xs:element name=\"leaf\" type=\"xs:string\" minOccurs=\"0\"/>"
                 + "</xs:sequence></xs:complexType>"
                 + "<xs:complexType name=\"BranchType\"><xs:complexContent>"
                 + "<xs:extension base=\"tns:HeadType\"><xs:sequence>"
                 + "<xs:element ref=\"tns:node\" minOccurs=\"0\"/>"
                 + "</xs:sequence></xs:extension>"
                 + "</xs:complexContent></xs:complexType>"
                 + "<xs:element name=\"node\" type=\"tns:HeadType\"/>"
                 + "<xs:element name=\"branch\" type=\"tns:BranchType\""
                 + " substitutionGroup=\"tns:node\"/>",
                 "node", collection);

        new XmlSchemaWalker(collection).walk(element);
    }

    @Test
    public void testSelfSubstitutionGroupIsRejected() {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        XmlSchemaElement element =
            load("<xs:element name=\"a\" type=\"xs:string\""
                 + " substitutionGroup=\"tns:a\"/>", "a", collection);

        assertThrowsXmlSchemaException(element, collection);
    }

    @Test
    public void testMutualSubstitutionGroupCycleIsRejected() {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        XmlSchemaElement element =
            load("<xs:element name=\"a\" type=\"xs:string\""
                 + " substitutionGroup=\"tns:b\"/>"
                 + "<xs:element name=\"b\" type=\"xs:string\""
                 + " substitutionGroup=\"tns:a\"/>", "a", collection);

        assertThrowsXmlSchemaException(element, collection);
    }

    @Test
    public void testCyclicGroupReferencesAreRejected() {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        XmlSchemaElement element =
            load("<xs:group name=\"g1\"><xs:sequence><xs:group ref=\"tns:g2\"/>"
                 + "</xs:sequence></xs:group>"
                 + "<xs:group name=\"g2\"><xs:sequence><xs:group ref=\"tns:g1\"/>"
                 + "</xs:sequence></xs:group>"
                 + "<xs:element name=\"root\"><xs:complexType><xs:sequence>"
                 + "<xs:group ref=\"tns:g1\"/>"
                 + "</xs:sequence></xs:complexType></xs:element>",
                 "root", collection);

        assertThrowsXmlSchemaException(element, collection);
    }

    @Test
    public void testCyclicTypeDerivationIsRejected() {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        XmlSchemaElement element =
            load("<xs:complexType name=\"A\"><xs:complexContent>"
                 + "<xs:extension base=\"tns:A\"/>"
                 + "</xs:complexContent></xs:complexType>"
                 + "<xs:element name=\"e\" type=\"tns:A\"/>", "e", collection);

        assertThrowsXmlSchemaException(element, collection);
    }

    @Test
    public void testCyclicAttributeGroupReferencesAreRejected() {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        XmlSchemaElement element =
            load("<xs:attributeGroup name=\"ag1\"><xs:attributeGroup ref=\"tns:ag2\"/>"
                 + "</xs:attributeGroup>"
                 + "<xs:attributeGroup name=\"ag2\"><xs:attributeGroup ref=\"tns:ag1\"/>"
                 + "</xs:attributeGroup>"
                 + "<xs:complexType name=\"C\"><xs:attributeGroup ref=\"tns:ag1\"/>"
                 + "</xs:complexType>"
                 + "<xs:element name=\"e\" type=\"tns:C\"/>", "e", collection);

        assertThrowsXmlSchemaException(element, collection);
    }

    private void assertThrowsXmlSchemaException(XmlSchemaElement element,
                                                XmlSchemaCollection collection) {
        try {
            new XmlSchemaWalker(collection).walk(element);
            fail("Expected cyclic schema expansion to be rejected.");
        } catch (XmlSchemaException expected) {
            // Expected: malformed schema expansion is bounded by the walker.
        }
    }
}
