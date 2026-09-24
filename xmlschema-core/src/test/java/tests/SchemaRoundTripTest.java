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
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaDerivationMethod;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaIdentityConstraint;
import org.apache.ws.commons.schema.XmlSchemaKeyref;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeUnion;

import org.junit.Assert;
import org.junit.Test;

/**
 * A schema read, written and read again must mean what it meant the first time.
 */
public class SchemaRoundTripTest extends Assert {

    private static XmlSchema read(String schema) {
        return new XmlSchemaCollection().read(new StringReader(schema));
    }

    private static XmlSchema roundTrip(XmlSchema schema) throws Exception {
        StringWriter written = new StringWriter();
        schema.write(written);
        return read(written.toString());
    }

    /**
     * block was written as the type's toString(), and so read back as no constraint at all.
     */
    @Test
    public void testComplexTypeBlockIsKept() throws Exception {
        XmlSchema schema = roundTrip(read(
            "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:t\">"
            + "<xs:complexType name=\"Account\" block=\"#all\"/>"
            + "<xs:complexType name=\"Partial\" block=\"extension\"/>"
            + "</xs:schema>"));

        XmlSchemaDerivationMethod all =
            ((XmlSchemaComplexType)schema.getTypeByName(new QName("urn:t", "Account"))).getBlock();
        assertTrue(all.isAll());
        XmlSchemaDerivationMethod partial =
            ((XmlSchemaComplexType)schema.getTypeByName(new QName("urn:t", "Partial"))).getBlock();
        assertTrue(partial.isExtension());
        assertFalse(partial.isRestriction());
    }

    private static XmlSchemaSimpleTypeUnion union(XmlSchema schema) {
        return (XmlSchemaSimpleTypeUnion)((XmlSchemaSimpleType)schema
            .getTypeByName(new QName("urn:t", "u"))).getContent();
    }

    /**
     * A named simple type inside a union was added to memberTypes as well as being an inline
     * member: with no memberTypes attribute write() then threw a NullPointerException, and with
     * one the member was written back twice.
     */
    @Test
    public void testNamedInlineUnionMemberWithoutMemberTypes() throws Exception {
        XmlSchema schema = read(
            "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:t\">"
            + "<xs:simpleType name=\"u\"><xs:union>"
            + "<xs:simpleType name=\"n\"><xs:restriction base=\"xs:string\"/></xs:simpleType>"
            + "</xs:union></xs:simpleType></xs:schema>");
        assertNull(union(schema).getMemberTypesSource());

        XmlSchemaSimpleTypeUnion reread = union(roundTrip(schema));
        assertNull(reread.getMemberTypesQNames());
        assertEquals(1, reread.getBaseTypes().size());
    }

    @Test
    public void testNamedInlineUnionMemberWithMemberTypes() throws Exception {
        XmlSchema schema = read(
            "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:t\">"
            + "<xs:simpleType name=\"u\"><xs:union memberTypes=\"xs:int\">"
            + "<xs:simpleType name=\"n\"><xs:restriction base=\"xs:string\"/></xs:simpleType>"
            + "</xs:union></xs:simpleType></xs:schema>");

        XmlSchemaSimpleTypeUnion reread = union(roundTrip(schema));
        assertArrayEquals(new QName[] {new QName("http://www.w3.org/2001/XMLSchema", "int")},
                          reread.getMemberTypesQNames());
        assertEquals(1, reread.getBaseTypes().size());
    }

    private static QName refer(XmlSchema schema) {
        XmlSchemaElement root = schema.getElementByName(new QName("urn:B", "root"));
        for (XmlSchemaIdentityConstraint constraint : root.getConstraints()) {
            if (constraint instanceof XmlSchemaKeyref) {
                return ((XmlSchemaKeyref)constraint).getRefer();
            }
        }
        fail("no keyref");
        return null;
    }

    private static String keyrefSchema(String outerDeclaration) {
        return "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"" + outerDeclaration
               + " targetNamespace=\"urn:B\">"
               + "<xs:element name=\"root\"><xs:complexType><xs:sequence>"
               + "<xs:element name=\"i\" type=\"xs:string\" maxOccurs=\"unbounded\"/>"
               + "</xs:sequence></xs:complexType>"
               + "<xs:key name=\"k\"><xs:selector xpath=\"i\"/><xs:field xpath=\".\"/></xs:key>"
               + "<xs:keyref name=\"r\" refer=\"p:k\" xmlns:p=\"urn:B\">"
               + "<xs:selector xpath=\"i\"/><xs:field xpath=\".\"/></xs:keyref>"
               + "</xs:element></xs:schema>";
    }

    /**
     * refer is resolved against the keyref's own namespace declarations; it used to be resolved
     * again against the enclosing element's, which here bind p to a different namespace.
     */
    @Test
    public void testKeyrefReferUsesItsOwnDeclarations() throws Exception {
        XmlSchema schema = read(keyrefSchema(" xmlns:p=\"urn:A\""));
        assertEquals(new QName("urn:B", "k"), refer(schema));
        assertEquals(new QName("urn:B", "k"), refer(roundTrip(schema)));
    }

    @Test
    public void testKeyrefReferDeclaredOnlyOnTheKeyref() throws Exception {
        assertEquals(new QName("urn:B", "k"), refer(read(keyrefSchema(""))));
    }
}
