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

public class OccursParsingTest extends Assert {

    private XmlSchema read(String particleAttributes) {
        String schema =
            "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\""
            + " targetNamespace=\"urn:occurs\">"
            + "<xs:element name=\"root\"><xs:complexType><xs:sequence>"
            + "<xs:element name=\"e\" type=\"xs:string\" " + particleAttributes + "/>"
            + "</xs:sequence></xs:complexType></xs:element>"
            + "</xs:schema>";
        return new XmlSchemaCollection().read(new StringReader(schema));
    }

    private XmlSchemaElement childElement(XmlSchema schema) {
        XmlSchemaElement root = schema.getElementByName(new QName("urn:occurs", "root"));
        XmlSchemaComplexType type = (XmlSchemaComplexType)root.getSchemaType();
        XmlSchemaSequence sequence = (XmlSchemaSequence)type.getParticle();
        return (XmlSchemaElement)sequence.getItems().get(0);
    }

    private void assertRejected(String particleAttributes, String message) {
        try {
            read(particleAttributes);
            fail(message);
        } catch (XmlSchemaException expected) {
            // Expected documented failure surface.
        }
    }

    @Test
    public void testValidOccursValuesStillParse() {
        XmlSchemaElement element = childElement(read("minOccurs=\"0\" maxOccurs=\"unbounded\""));
        assertEquals(0, element.getMinOccurs());
        assertEquals(Long.MAX_VALUE, element.getMaxOccurs());
    }

    @Test
    public void testNonNumericMaxOccursIsRejected() {
        assertRejected("maxOccurs=\"18446744073709551616\"",
                       "An overflowing maxOccurs value should be rejected, not coerced to 1.");
    }

    @Test
    public void testUnboundedMinOccursIsRejected() {
        assertRejected("minOccurs=\"unbounded\"",
                       "minOccurs=unbounded should be rejected.");
    }

    @Test
    public void testNegativeMinOccursIsRejected() {
        assertRejected("minOccurs=\"-1\"",
                       "A negative minOccurs should be rejected.");
    }

    @Test
    public void testNegativeMaxOccursIsRejected() {
        assertRejected("maxOccurs=\"-1\"",
                       "A negative maxOccurs should be rejected.");
    }

    @Test
    public void testMaxValueLiteralMaxOccursIsRejected() {
        assertRejected("maxOccurs=\"9223372036854775807\"",
                       "The maxOccurs sentinel literal should be rejected.");
    }

    @Test
    public void testMaxValueLiteralMinOccursIsRejected() {
        assertRejected("minOccurs=\"9223372036854775807\"",
                       "The minOccurs sentinel literal should be rejected.");
    }

    @Test
    public void testSentinelMinOccursDoesNotSerializeSilently() throws Exception {
        XmlSchema schema = read("minOccurs=\"0\"");
        childElement(schema).setMinOccurs(Long.MAX_VALUE);
        try {
            schema.getSchemaDocument();
            fail("A sentinel minOccurs must be rejected during serialization.");
        } catch (XmlSchemaException expected) {
            // Expected documented failure surface.
        }
    }
}
