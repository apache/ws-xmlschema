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
import org.apache.ws.commons.schema.XmlSchemaNotation;

import org.junit.Assert;
import org.junit.Test;

/**
 * read() builds an xs:notation into the model, so write() has to put it back.
 */
public class NotationRoundTripTest extends Assert {

    private static final String SCHEMA =
        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:n\">"
        + "<xs:notation name=\"jpeg\" public=\"image/jpeg\" system=\"viewer.exe\">"
        + "<xs:annotation><xs:documentation>a picture</xs:documentation></xs:annotation>"
        + "</xs:notation>"
        + "<xs:element name=\"e\" type=\"xs:string\"/>"
        + "</xs:schema>";

    private static XmlSchema read(String text) {
        return new XmlSchemaCollection().read(new StringReader(text));
    }

    @Test
    public void testNotationSurvivesARoundTrip() throws Exception {
        XmlSchema before = read(SCHEMA);
        assertEquals(1, before.getNotations().size());

        StringWriter writer = new StringWriter();
        before.write(writer);

        XmlSchema after = read(writer.toString());
        assertEquals("the notation must not be dropped by write()",
                     before.getNotations().size(), after.getNotations().size());

        XmlSchemaNotation notation =
            after.getNotationByName(new QName("urn:n", "jpeg"));
        assertNotNull(notation);
        assertEquals("image/jpeg", notation.getPublic());
        assertEquals("viewer.exe", notation.getSystem());
        assertNotNull("the notation's annotation must survive too", notation.getAnnotation());
    }

    @Test
    public void testOtherTopLevelComponentsStillSurvive() throws Exception {
        XmlSchema before = read(
            "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:n\">"
            + "<xs:element name=\"el\" type=\"xs:string\"/>"
            + "<xs:attribute name=\"at\" type=\"xs:string\"/>"
            + "<xs:complexType name=\"ct\"><xs:sequence/></xs:complexType>"
            + "<xs:group name=\"g\"><xs:sequence>"
            + "<xs:element name=\"x\" type=\"xs:string\"/></xs:sequence></xs:group>"
            + "<xs:attributeGroup name=\"ag\">"
            + "<xs:attribute name=\"y\" type=\"xs:string\"/></xs:attributeGroup>"
            + "<xs:notation name=\"n\" public=\"urn:p\"/>"
            + "</xs:schema>");

        StringWriter writer = new StringWriter();
        before.write(writer);
        XmlSchema after = read(writer.toString());

        assertEquals(before.getElements().size(), after.getElements().size());
        assertEquals(before.getAttributes().size(), after.getAttributes().size());
        assertEquals(before.getSchemaTypes().size(), after.getSchemaTypes().size());
        assertEquals(before.getGroups().size(), after.getGroups().size());
        assertEquals(before.getAttributeGroups().size(), after.getAttributeGroups().size());
        assertEquals(before.getNotations().size(), after.getNotations().size());
    }

    /** getTypeByQName was the only QName lookup that threw on null. */
    @Test
    public void testQNameLookupsTolerateNull() {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        assertNull(collection.getTypeByQName(null));
        assertNull(collection.getElementByQName(null));
        assertNull(collection.getAttributeByQName(null));
    }
}
