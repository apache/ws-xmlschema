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

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaException;

import org.junit.Assert;
import org.junit.Test;

import org.w3c.dom.Document;

import org.xml.sax.InputSource;

/**
 * read(Source) with a DOMSource must route to the pre-parsed read paths
 * (previously it threw ClassCastException for every DOMSource), and must
 * reject DOMSources wrapping anything but a Document or Element.
 */
public class ReadDomSourceTest extends Assert {

    private static final String SCHEMA =
        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\""
        + " targetNamespace=\"urn:domsource\">"
        + "<xs:element name=\"e\" type=\"xs:string\"/>"
        + "</xs:schema>";

    private Document parse() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(SCHEMA)));
    }

    @Test
    public void testDomSourceWrappingDocument() throws Exception {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        XmlSchema schema = collection.read(new DOMSource(parse()));
        assertNotNull(schema);
        assertEquals("urn:domsource", schema.getTargetNamespace());
    }

    @Test
    public void testDomSourceWrappingElement() throws Exception {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        XmlSchema schema = collection.read(new DOMSource(parse().getDocumentElement()));
        assertNotNull(schema);
        assertEquals("urn:domsource", schema.getTargetNamespace());
    }

    @Test
    public void testDomSourceSystemIdIsPreserved() throws Exception {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        DOMSource source = new DOMSource(parse());
        source.setSystemId("urn:domsource:system-id");
        XmlSchema schema = collection.read(source);
        assertEquals("urn:domsource:system-id", schema.getSourceURI());
    }

    @Test
    public void testDomSourceWrappingOtherNodeIsRejected() throws Exception {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        Document doc = parse();
        try {
            collection.read(new DOMSource(doc.createTextNode("not a schema")));
            fail("A DOMSource wrapping a text node should be rejected.");
        } catch (XmlSchemaException expected) {
            // documented failure surface (previously ClassCastException)
        }
    }
}