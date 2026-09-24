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
import java.io.StringWriter;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexContent;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaFacet;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;

import org.junit.Assert;
import org.junit.Test;

/**
 * xs:boolean attributes accept "1" and surrounding whitespace as well as "true".
 */
public class BooleanAttributeTest extends Assert {

    private static XmlSchema read(String value) {
        return new XmlSchemaCollection().read(new StringReader(
            "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:t=\"urn:t\""
            + " targetNamespace=\"urn:t\">"
            + "<xs:element name=\"e\" type=\"xs:string\" abstract=\"" + value + "\""
            + " nillable=\"" + value + "\"/>"
            + "<xs:complexType name=\"C\" abstract=\"" + value + "\" mixed=\"" + value + "\"/>"
            + "<xs:complexType name=\"D\"><xs:complexContent mixed=\"" + value + "\">"
            + "<xs:extension base=\"t:C\"/></xs:complexContent></xs:complexType>"
            + "<xs:simpleType name=\"S\"><xs:restriction base=\"xs:string\">"
            + "<xs:maxLength value=\"5\" fixed=\"" + value + "\"/></xs:restriction></xs:simpleType>"
            + "</xs:schema>"));
    }

    private static void assertAll(XmlSchema schema, boolean expected) {
        XmlSchemaElement e = schema.getElementByName(new QName("urn:t", "e"));
        assertEquals("element abstract", expected, e.isAbstract());
        assertEquals("element nillable", expected, e.isNillable());

        XmlSchemaComplexType c = (XmlSchemaComplexType)schema.getTypeByName(new QName("urn:t", "C"));
        assertEquals("complexType abstract", expected, c.isAbstract());
        assertEquals("complexType mixed", expected, c.isMixed());

        XmlSchemaComplexType d = (XmlSchemaComplexType)schema.getTypeByName(new QName("urn:t", "D"));
        assertEquals("complexContent mixed", expected, ((XmlSchemaComplexContent)d.getContentModel()).isMixed());

        XmlSchemaSimpleType s = (XmlSchemaSimpleType)schema.getTypeByName(new QName("urn:t", "S"));
        XmlSchemaFacet facet = ((XmlSchemaSimpleTypeRestriction)s.getContent()).getFacets().get(0);
        assertEquals("facet fixed", expected, facet.isFixed());
    }

    private static XmlSchema roundTrip(XmlSchema schema) throws Exception {
        StringWriter written = new StringWriter();
        schema.write(written);
        return new XmlSchemaCollection().read(new StringReader(written.toString()));
    }

    @Test
    public void testOneIsTrue() throws Exception {
        XmlSchema schema = read("1");
        assertAll(schema, true);
        assertAll(roundTrip(schema), true);
    }

    @Test
    public void testSurroundingWhitespaceIsIgnored() throws Exception {
        assertAll(read(" true "), true);
    }

    @Test
    public void testTrueStillTrue() throws Exception {
        assertAll(read("true"), true);
    }

    @Test
    public void testZeroAndFalseAreFalse() throws Exception {
        assertAll(read("0"), false);
        assertAll(read("false"), false);
    }
}
