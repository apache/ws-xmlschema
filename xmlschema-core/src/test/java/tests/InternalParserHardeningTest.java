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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaDocumentation;
import org.apache.ws.commons.schema.XmlSchemaException;
import org.apache.ws.commons.schema.resolver.URIResolver;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * The internal parser must not fetch external DTD or external entity content, but it must
 * still accept the DOCTYPE declarations that real-world schema documents carry.
 */
public class InternalParserHardeningTest extends Assert {

    private static final String PLAIN_SCHEMA =
        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\""
        + " targetNamespace=\"urn:hardening\">"
        + "<xs:element name=\"e\" type=\"xs:string\"/>"
        + "</xs:schema>";

    /**
     * The shape used by the W3C's own normative schemas (XML Signature, XML Encryption,
     * XKMS): a DOCTYPE whose internal subset declares the target namespace as an entity,
     * alongside an external subset that must not be fetched.
     */
    private static final String W3C_STYLE_SCHEMA =
        "<!DOCTYPE xs:schema PUBLIC \"-//W3C//DTD XMLSCHEMA 200102//EN\""
        + " \"http://www.w3.org/2001/XMLSchema.dtd\" ["
        + "<!ENTITY ns 'urn:hardening'>"
        + "]>"
        + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"&ns;\">"
        + "<xs:element name=\"e\" type=\"xs:string\"/>"
        + "<xs:complexType name=\"t\">"
        + "<xs:annotation><xs:documentation>ns is &ns;</xs:documentation></xs:annotation>"
        + "<xs:sequence/>"
        + "</xs:complexType>"
        + "</xs:schema>";

    private static final String IMPORTING_SCHEMA =
        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\""
        + " targetNamespace=\"urn:importer\">"
        + "<xs:import namespace=\"urn:hardening\" schemaLocation=\"imported.xsd\"/>"
        + "</xs:schema>";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testSchemaWithoutDoctypeStillParses() {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        XmlSchema schema = collection.read(new StringReader(PLAIN_SCHEMA));
        assertNotNull(schema);
        assertEquals("urn:hardening", schema.getTargetNamespace());
    }

    @Test
    public void testSchemaWithInternalDtdSubsetIsAccepted() {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        XmlSchema schema = collection.read(new StringReader(W3C_STYLE_SCHEMA));
        assertNotNull(schema);
        assertEquals("urn:hardening", schema.getTargetNamespace());
    }

    /**
     * Internal entities must be expanded, not left in the model as entity references.
     */
    @Test
    public void testInternalEntitiesAreExpanded() {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        XmlSchema schema = collection.read(new StringReader(W3C_STYLE_SCHEMA));
        assertEquals("ns is urn:hardening", markupOf(schema).trim());
    }

    @Test
    public void testExternalGeneralEntityIsNotResolved() throws Exception {
        File secret = writeSecretFile();
        String schemaText =
            "<!DOCTYPE xs:schema [<!ENTITY xxe SYSTEM \"" + secret.toURI() + "\">]>"
            + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:hardening\">"
            + "<xs:complexType name=\"t\">"
            + "<xs:annotation><xs:documentation>&xxe;</xs:documentation></xs:annotation>"
            + "<xs:sequence/>"
            + "</xs:complexType>"
            + "</xs:schema>";

        XmlSchemaCollection collection = new XmlSchemaCollection();
        XmlSchema schema = collection.read(new StringReader(schemaText));
        assertNotNull(schema);
        assertFalse("external entity content leaked into the schema model",
                    markupOf(schema).contains("TOP-SECRET"));
    }

    @Test
    public void testExternalDtdSubsetIsNotResolved() throws Exception {
        File secret = writeSecretFile();
        File dtd = tempFolder.newFile("evil.dtd");
        write(dtd, "<!ENTITY xxe SYSTEM \"" + secret.toURI() + "\">");

        String schemaText =
            "<!DOCTYPE xs:schema SYSTEM \"" + dtd.toURI() + "\">"
            + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:hardening\">"
            + "<xs:complexType name=\"t\">"
            + "<xs:annotation><xs:documentation>text</xs:documentation></xs:annotation>"
            + "<xs:sequence/>"
            + "</xs:complexType>"
            + "</xs:schema>";

        XmlSchemaCollection collection = new XmlSchemaCollection();
        XmlSchema schema = collection.read(new StringReader(schemaText));
        assertNotNull(schema);
        assertFalse("external DTD content leaked into the schema model",
                    markupOf(schema).contains("TOP-SECRET"));
    }

    /**
     * The import re-parse is the path a caller cannot harden themselves: fetched bytes
     * re-enter this collection's own parser, and a DTD-level fetch never consults the
     * URIResolver. External entity content must not be resolved there either.
     */
    @Test
    public void testExternalGeneralEntityIsNotResolvedOnImportReparse() throws Exception {
        File secret = writeSecretFile();
        final String hostileImport =
            "<!DOCTYPE xs:schema [<!ENTITY xxe SYSTEM \"" + secret.toURI() + "\">]>"
            + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:hardening\">"
            + "<xs:complexType name=\"t\">"
            + "<xs:annotation><xs:documentation>&xxe;</xs:documentation></xs:annotation>"
            + "<xs:sequence/>"
            + "</xs:complexType>"
            + "</xs:schema>";

        XmlSchemaCollection collection = new XmlSchemaCollection();
        collection.setSchemaResolver(new URIResolver() {
            public InputSource resolveEntity(String targetNamespace, String schemaLocation,
                                            String baseUri) {
                InputSource source = new InputSource(new StringReader(hostileImport));
                source.setSystemId(schemaLocation);
                return source;
            }
        });
        collection.read(new StringReader(IMPORTING_SCHEMA));

        XmlSchema imported = collection.schemaForNamespace("urn:hardening");
        assertNotNull(imported);
        assertFalse("external entity content leaked through the import re-parse",
                    markupOf(imported).contains("TOP-SECRET"));
    }

    @Test
    public void testImportedSchemaWithInternalDtdSubsetIsAccepted() {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        collection.setSchemaResolver(new URIResolver() {
            public InputSource resolveEntity(String targetNamespace, String schemaLocation,
                                            String baseUri) {
                InputSource source = new InputSource(new StringReader(W3C_STYLE_SCHEMA));
                source.setSystemId(schemaLocation);
                return source;
            }
        });
        XmlSchema schema = collection.read(new StringReader(IMPORTING_SCHEMA));
        assertNotNull(schema);
        assertNotNull(collection.schemaForNamespace("urn:hardening"));
    }

    /**
     * Accepting a DOCTYPE means accepting an internal DTD subset, so the limits that
     * make that safe must stay in force: FEATURE_SECURE_PROCESSING bounds entity
     * expansion by count, which stops nested "billion laughs" expansion.
     */
    @Test
    public void testNestedEntityExpansionIsBounded() {
        StringBuilder subset = new StringBuilder("<!ENTITY a 'aaaaaaaaaa'>");
        for (int i = 1; i < 9; i++) {
            subset.append("<!ENTITY a").append(i).append(" '");
            for (int j = 0; j < 10; j++) {
                subset.append("&").append(i == 1 ? "a" : "a" + (i - 1)).append(";");
            }
            subset.append("'>");
        }
        try {
            new XmlSchemaCollection().read(new StringReader(schemaWithSubset(subset.toString(), "&a8;")));
            fail("expected the entity expansion limit to refuse a billion-laughs schema");
        } catch (XmlSchemaException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("entity expansions"));
        }
    }

    /**
     * ... and by accumulated size, which stops a flat entity referenced enough times to
     * stay under the expansion count but still blow up the heap.
     */
    @Test
    public void testFlatEntityExpansionIsBoundedBySize() {
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 64 * 1024; i++) {
            big.append('A');
        }
        StringBuilder refs = new StringBuilder();
        for (int i = 0; i < 60000; i++) {
            refs.append("&big;");
        }
        try {
            new XmlSchemaCollection().read(new StringReader(
                schemaWithSubset("<!ENTITY big '" + big + "'>", refs.toString())));
            fail("expected the accumulated entity size limit to refuse the schema");
        } catch (XmlSchemaException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("accumulated size"));
        }
    }

    private static String schemaWithSubset(String internalSubset, String documentation) {
        return "<!DOCTYPE xs:schema [" + internalSubset + "]>"
            + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:hardening\">"
            + "<xs:complexType name=\"t\">"
            + "<xs:annotation><xs:documentation>" + documentation + "</xs:documentation></xs:annotation>"
            + "<xs:sequence/>"
            + "</xs:complexType>"
            + "</xs:schema>";
    }

    private File writeSecretFile() throws Exception {
        File secret = tempFolder.newFile("secret.txt");
        write(secret, "TOP-SECRET-CONTENT");
        return secret;
    }

    private void write(File file, String content) throws Exception {
        Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
        try {
            writer.write(content);
        } finally {
            writer.close();
        }
    }

    private String markupOf(XmlSchema schema) {
        XmlSchemaComplexType type = (XmlSchemaComplexType)schema.getTypeByName("t");
        if (type == null || type.getAnnotation() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object item : type.getAnnotation().getItems()) {
            XmlSchemaDocumentation doc = (XmlSchemaDocumentation)item;
            NodeList markup = doc.getMarkup();
            for (int i = 0; markup != null && i < markup.getLength(); i++) {
                sb.append(markup.item(i).getTextContent());
            }
        }
        return sb.toString();
    }
}
