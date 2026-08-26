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

import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaException;

import org.junit.Assert;
import org.junit.Test;

public class ExceptionContractTest extends Assert {

    private void assertRejectedWithXmlSchemaException(String schemaBody) {
        String schema =
            "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\""
            + " targetNamespace=\"urn:contract\">" + schemaBody + "</xs:schema>";
        XmlSchemaCollection collection = new XmlSchemaCollection();
        try {
            collection.read(new StringReader(schema));
            fail("The crafted schema should have been rejected: " + schemaBody);
        } catch (XmlSchemaException expected) {
            // Expected documented failure surface.
        }
    }

    @Test
    public void testUndeclaredPrefixInTypeReference() {
        assertRejectedWithXmlSchemaException("<xs:element name=\"a\" type=\"nope:T\"/>");
    }

    @Test
    public void testReferOnKeyConstraint() {
        assertRejectedWithXmlSchemaException(
            "<xs:element name=\"a\" type=\"xs:string\">"
            + "<xs:key name=\"k\" refer=\"xs:b\">"
            + "<xs:selector xpath=\".\"/><xs:field xpath=\"@id\"/>"
            + "</xs:key></xs:element>");
    }

    @Test
    public void testInvalidFormAttributeValue() {
        assertRejectedWithXmlSchemaException(
            "<xs:element name=\"a\"><xs:complexType>"
            + "<xs:attribute name=\"x\" form=\"bogus\"/>"
            + "</xs:complexType></xs:element>");
    }

    @Test
    public void testInvalidUseAttributeValue() {
        assertRejectedWithXmlSchemaException(
            "<xs:element name=\"a\"><xs:complexType>"
            + "<xs:attribute name=\"x\" use=\"bogus\"/>"
            + "</xs:complexType></xs:element>");
    }

    @Test
    public void testInvalidProcessContentsValue() {
        assertRejectedWithXmlSchemaException(
            "<xs:element name=\"a\"><xs:complexType><xs:sequence>"
            + "<xs:any processContents=\"bogus\"/>"
            + "</xs:sequence></xs:complexType></xs:element>");
    }
}