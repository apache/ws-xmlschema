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

package tests;

import java.io.StringReader;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaException;
import org.apache.ws.commons.schema.XmlSchemaSequence;

import org.junit.Assert;
import org.junit.Test;

/**
 * A form attribute on an element or attribute declaration with no name, such as one using ref,
 * used to throw IllegalArgumentException out of read() while computing a wire name from a null
 * local name.
 */
public class FormWithoutNameTest extends Assert {

    private static final String HEADER =
        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:t=\"urn:t\""
        + " targetNamespace=\"urn:t\">";

    /**
     * The schemas below are not all valid, but read() must report a problem with one only
     * through XmlSchemaException.
     */
    private static XmlSchema read(String body) {
        try {
            return new XmlSchemaCollection().read(new StringReader(HEADER + body + "</xs:schema>"));
        } catch (XmlSchemaException e) {
            return null;
        }
    }

    @Test
    public void testGlobalElementWithoutName() {
        read("<xs:element form=\"unqualified\"/>");
    }

    @Test
    public void testLocalAttributeWithoutName() {
        read("<xs:complexType name=\"c\"><xs:attribute form=\"unqualified\"/></xs:complexType>");
    }

    @Test
    public void testAttributeRefWithForm() {
        read("<xs:attribute name=\"a\" type=\"xs:string\"/>"
             + "<xs:attributeGroup name=\"g\"><xs:attribute ref=\"t:a\" form=\"unqualified\"/>"
             + "</xs:attributeGroup>");
    }

    @Test
    public void testElementRefWithFormKeepsTheReferencedName() {
        XmlSchema schema = read(
            "<xs:element name=\"e\" type=\"xs:string\"/>"
            + "<xs:complexType name=\"c\"><xs:sequence>"
            + "<xs:element ref=\"t:e\" form=\"unqualified\"/>"
            + "</xs:sequence></xs:complexType>");
        assertNotNull(schema);
        XmlSchemaComplexType c = (XmlSchemaComplexType)schema.getTypeByName(new QName("urn:t", "c"));
        XmlSchemaElement ref = (XmlSchemaElement)((XmlSchemaSequence)c.getParticle()).getItems().get(0);
        assertEquals(new QName("urn:t", "e"), ref.getWireName());
    }

    @Test
    public void testNamedElementsStillGetTheirWireName() {
        XmlSchema schema = read(
            "<xs:complexType name=\"c\"><xs:sequence>"
            + "<xs:element name=\"u\" type=\"xs:string\" form=\"unqualified\"/>"
            + "<xs:element name=\"q\" type=\"xs:string\" form=\"qualified\"/>"
            + "</xs:sequence></xs:complexType>");
        XmlSchemaComplexType c = (XmlSchemaComplexType)schema.getTypeByName(new QName("urn:t", "c"));
        XmlSchemaSequence seq = (XmlSchemaSequence)c.getParticle();
        assertEquals(new QName("", "u"), ((XmlSchemaElement)seq.getItems().get(0)).getWireName());
        assertEquals(new QName("urn:t", "q"), ((XmlSchemaElement)seq.getItems().get(1)).getWireName());
    }
}
