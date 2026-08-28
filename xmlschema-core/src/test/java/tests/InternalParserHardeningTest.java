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

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaException;
import org.apache.ws.commons.schema.resolver.URIResolver;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.InputSource;

public class InternalParserHardeningTest extends Assert {

    private static final String PLAIN_SCHEMA =
        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\""
        + " targetNamespace=\"urn:hardening\">"
        + "<xs:element name=\"e\" type=\"xs:string\"/>"
        + "</xs:schema>";

    private static final String IMPORTING_SCHEMA =
        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\""
        + " targetNamespace=\"urn:importer\">"
        + "<xs:import namespace=\"urn:hardening\" schemaLocation=\"hostile.xsd\"/>"
        + "</xs:schema>";

    private static final String DOCTYPE_SCHEMA =
        "<!DOCTYPE xs:schema [<!ENTITY payload \"expanded\">]>" + PLAIN_SCHEMA;

    @Test
    public void testSchemaWithoutDoctypeStillParses() {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        XmlSchema schema = collection.read(new StringReader(PLAIN_SCHEMA));
        assertNotNull(schema);
        assertEquals("urn:hardening", schema.getTargetNamespace());
    }

    @Test(expected = XmlSchemaException.class)
    public void testSchemaWithDoctypeIsRejected() {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        collection.read(new StringReader(DOCTYPE_SCHEMA));
    }

    @Test(expected = XmlSchemaException.class)
    public void testImportedSchemaWithDoctypeIsRejected() {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        collection.setSchemaResolver(new URIResolver() {
            public InputSource resolveEntity(String targetNamespace, String schemaLocation,
                                            String baseUri) {
                InputSource source = new InputSource(new StringReader(DOCTYPE_SCHEMA));
                source.setSystemId(schemaLocation);
                return source;
            }
        });
        collection.read(new StringReader(IMPORTING_SCHEMA));
    }
}