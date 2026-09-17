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

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.resolver.URIResolver;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.InputSource;

/**
 * A document reached through a schemaLocation is whatever was at that URL, and any XML root
 * parses as a schema. Its foreign children must not be captured into the model, or an
 * embedding that republishes resolved schemas hands the fetched file's contents back.
 */
public class ForeignTopLevelContentTest extends Assert {

    private static final String SECRET = "TOP-SECRET-VALUE";

    private static final String NOT_A_SCHEMA =
        "<config targetNamespace=\"urn:internal\" xmlns=\"urn:internal\">"
        + "<dbPassword>" + SECRET + "</dbPassword>"
        + "</config>";

    private static String republish(XmlSchema schema) throws Exception {
        StringWriter writer = new StringWriter();
        schema.write(writer);
        return writer.toString();
    }

    @Test
    public void testForeignChildrenOfTheRootAreNotRepublished() throws Exception {
        XmlSchema schema = new XmlSchemaCollection().read(new StringReader(NOT_A_SCHEMA));
        assertFalse("the fetched document's content must not reach the serializer",
                    republish(schema).contains(SECRET));
    }

    @Test
    public void testForeignChildrenOfAnImportedDocumentAreNotRepublished() throws Exception {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        collection.setSchemaResolver(new URIResolver() {
            public InputSource resolveEntity(String namespace, String schemaLocation, String baseUri) {
                InputSource source = new InputSource(new StringReader(NOT_A_SCHEMA));
                source.setSystemId("http://example.invalid/" + schemaLocation);
                return source;
            }
        });
        collection.read(new StringReader(
            "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:a\">"
            + "<xs:import namespace=\"urn:internal\" schemaLocation=\"config.xml\"/>"
            + "</xs:schema>"));

        XmlSchema imported = collection.schemaForNamespace("urn:internal");
        assertNotNull(imported);
        assertFalse(republish(imported).contains(SECRET));
    }

    /** Foreign attributes on xs:schema are legal, and are still captured. */
    @Test
    public void testForeignAttributesOnTheRootAreStillCaptured() throws Exception {
        XmlSchema schema = new XmlSchemaCollection().read(new StringReader(
            "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\""
            + " xmlns:ext=\"http://customattrib.org\" ext:stamp=\"kept\""
            + " targetNamespace=\"urn:a\">"
            + "<xs:element name=\"e\" type=\"xs:string\"/></xs:schema>"));
        assertNotNull("a foreign attribute on xs:schema must still be captured",
                      schema.getMetaInfoMap());
    }

    /** Foreign children of a complexType or element are legal extension points and still work. */
    @Test
    public void testForeignChildrenOfOtherComponentsAreStillCaptured() throws Exception {
        XmlSchema schema = new XmlSchemaCollection().read(new StringReader(
            "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\""
            + " xmlns:ext=\"http://customattrib.org\" targetNamespace=\"urn:a\">"
            + "<xs:element name=\"e\" type=\"xs:string\">"
            + "<ext:customElt prefix=\"ext\" suffix=\"elt\"/>"
            + "</xs:element></xs:schema>"));
        assertNotNull(schema.getElementByName("e"));
        assertNotNull("extension elements on an xs:element must still be captured",
                      schema.getElementByName("e").getMetaInfoMap());
    }
}
