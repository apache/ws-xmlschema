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
import java.io.StringWriter;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeUnion;

import org.junit.Assert;
import org.junit.Test;

/**
 * Walking a schema must not change it. The walker once added a union's named member types to
 * the union's own list of inline members, so the list grew on every walk, and within one walk
 * on every visit to an anonymous union.
 */
public class UnionMembersUnchangedByWalkTest extends Assert {

    private static final String NS = "urn:union";

    private static final String SCHEMA =
        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:tns=\"urn:union\""
        + " targetNamespace=\"urn:union\">"
        + "<xs:simpleType name=\"U\"><xs:union memberTypes=\"xs:int xs:boolean\">"
        + "<xs:simpleType><xs:restriction base=\"xs:string\"/></xs:simpleType>"
        + "</xs:union></xs:simpleType>"
        + "<xs:attribute name=\"a\"><xs:simpleType>"
        + "<xs:union memberTypes=\"xs:int xs:date\"/></xs:simpleType></xs:attribute>"
        + "<xs:complexType name=\"T1\"><xs:attribute ref=\"tns:a\"/></xs:complexType>"
        + "<xs:complexType name=\"T2\"><xs:attribute ref=\"tns:a\"/></xs:complexType>"
        + "<xs:complexType name=\"T3\"><xs:attribute ref=\"tns:a\"/></xs:complexType>"
        + "<xs:element name=\"root\"><xs:complexType><xs:sequence>"
        + "<xs:element name=\"u\" type=\"tns:U\"/>"
        + "<xs:element name=\"t1\" type=\"tns:T1\"/>"
        + "<xs:element name=\"t2\" type=\"tns:T2\"/>"
        + "<xs:element name=\"t3\" type=\"tns:T3\"/>"
        + "</xs:sequence></xs:complexType></xs:element>"
        + "</xs:schema>";

    @Test
    public void testRepeatedWalksLeaveUnionMembersUnchanged() {
        XmlSchemaCollection collection = read(SCHEMA);
        for (int i = 0; i < 3; i++) {
            walk(collection);
            assertEquals(1, namedUnion(collection).getBaseTypes().size());
            assertEquals(2, namedUnion(collection).getMemberTypesQNames().length);
        }
    }

    @Test
    public void testOneWalkLeavesAnonymousUnionMembersUnchanged() {
        XmlSchemaCollection collection = read(SCHEMA);
        walk(collection);
        XmlSchemaSimpleTypeUnion union = (XmlSchemaSimpleTypeUnion)collection
            .getAttributeByQName(new QName(NS, "a")).getSchemaType().getContent();
        assertEquals(0, union.getBaseTypes().size());
        assertEquals(2, union.getMemberTypesQNames().length);
    }

    @Test
    public void testSchemaWrittenAfterAWalkReadsBackTheSame() throws Exception {
        XmlSchemaCollection collection = read(SCHEMA);
        walk(collection);
        walk(collection);
        StringWriter out = new StringWriter();
        for (XmlSchema schema : collection.getXmlSchema(null)) {
            if (NS.equals(schema.getTargetNamespace())) {
                schema.write(out);
            }
        }
        XmlSchemaSimpleTypeUnion reread = namedUnion(read(out.toString()));
        assertEquals(1, reread.getBaseTypes().size());
        assertEquals(2, reread.getMemberTypesQNames().length);
    }

    private static XmlSchemaCollection read(String schema) {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        collection.read(new StringReader(schema));
        return collection;
    }

    private static void walk(XmlSchemaCollection collection) {
        XmlSchemaElement root = collection.getElementByQName(new QName(NS, "root"));
        assertNotNull(root);
        new XmlSchemaWalker(collection).walk(root);
    }

    private static XmlSchemaSimpleTypeUnion namedUnion(XmlSchemaCollection collection) {
        XmlSchemaSimpleType type = (XmlSchemaSimpleType)collection.getTypeByQName(new QName(NS, "U"));
        return (XmlSchemaSimpleTypeUnion)type.getContent();
    }
}
