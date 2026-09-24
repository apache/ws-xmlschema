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
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;

import org.junit.Assert;
import org.junit.Test;

import org.w3c.dom.Attr;

/**
 * An unrecognised attribute on xs:attribute is kept, and when its value looks like a QName the
 * declaration of its prefix is kept with it.
 */
public class UnhandledAttributeTest extends Assert {

    private static XmlSchemaAttribute readAttribute(String value) {
        XmlSchema schema = new XmlSchemaCollection().read(new StringReader(schemaWith(value)));
        return schema.getAttributeByName(new QName("urn:t", "a"));
    }

    private static String schemaWith(String value) {
        return "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:t=\"urn:t\""
               + " xmlns=\"urn:default\" targetNamespace=\"urn:t\">"
               + "<xs:attribute name=\"a\" foo=\"" + value + "\"/></xs:schema>";
    }

    private static String declarationOf(XmlSchemaAttribute attribute, String prefix) {
        for (Attr attr : attribute.getUnhandledAttributes()) {
            if (("xmlns:" + prefix).equals(attr.getName())) {
                return attr.getValue();
            }
        }
        return null;
    }

    /**
     * A value starting with ':' has no prefix. It used to be read as the empty prefix, which
     * resolved to the default namespace and threw a DOMException creating an "xmlns:" attribute.
     */
    @Test
    public void testValueStartingWithAColonIsKeptAsIs() throws Exception {
        XmlSchemaAttribute attribute = readAttribute(":x");
        assertEquals(1, attribute.getUnhandledAttributes().length);
        assertEquals(":x", attribute.getUnhandledAttributes()[0].getValue());

        StringWriter written = new StringWriter();
        new XmlSchemaCollection().read(new StringReader(schemaWith(":x"))).write(written);
        XmlSchema reread = new XmlSchemaCollection().read(new StringReader(written.toString()));
        assertNotNull(reread.getAttributeByName(new QName("urn:t", "a")));
    }

    @Test
    public void testPrefixedValueStillKeepsItsDeclaration() throws Exception {
        XmlSchemaAttribute attribute = readAttribute("t:x");
        assertEquals("urn:t", declarationOf(attribute, "t"));
    }
}
