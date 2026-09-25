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

package org.apache.ws.commons.schema.docpath;

import java.io.StringReader;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaException;
import org.apache.ws.commons.schema.walker.XmlSchemaWalker;

import org.junit.Assert;
import org.junit.Test;

/**
 * Element declarations the schema reader accepts although XML Schema forbids them are refused by
 * the walker with its documented exception, not a NullPointerException from the state machine
 * generator.
 */
public class TestStateMachineOddDeclarations extends Assert {

    private static final String PREFIX =
        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\""
        + " xmlns:tns=\"urn:odd\" targetNamespace=\"urn:odd\">"
        + "<xs:element name=\"target\" type=\"xs:string\"/>"
        + "<xs:element name=\"root\"><xs:complexType><xs:sequence>";

    private static final String SUFFIX = "</xs:sequence></xs:complexType></xs:element></xs:schema>";

    /*
     * ref and type together, as a W3C test schema has it. The reader keeps only the type.
     */
    @Test
    public void testElementWithRefAndTypeIsRefused() {
        assertRefused("<xs:element ref=\"tns:target\" type=\"xs:int\"/>");
    }

    @Test
    public void testElementWithNeitherNameNorRefIsRefused() {
        assertRefused("<xs:element type=\"xs:string\"/>");
    }

    @Test
    public void testElementWithRefStillWalks() {
        final XmlSchemaCollection collection = read("<xs:element ref=\"tns:target\"/>");
        final XmlSchemaStateMachineGenerator generator = new XmlSchemaStateMachineGenerator();
        new XmlSchemaWalker(collection, generator).walk(root(collection));
        assertNotNull(generator.getStateMachineNodesByQName().get(new QName("urn:odd", "target")));
    }

    private static void assertRefused(String particles) {
        final XmlSchemaCollection collection = read(particles);
        try {
            new XmlSchemaWalker(collection, new XmlSchemaStateMachineGenerator()).walk(root(collection));
            fail("The declaration names no element and should be refused.");
        } catch (XmlSchemaException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("neither a name nor a ref"));
        }
    }

    private static XmlSchemaCollection read(String particles) {
        final XmlSchemaCollection collection = new XmlSchemaCollection();
        collection.read(new StringReader(PREFIX + particles + SUFFIX));
        return collection;
    }

    private static XmlSchemaElement root(XmlSchemaCollection collection) {
        final XmlSchemaElement root = collection.getElementByQName(new QName("urn:odd", "root"));
        assertNotNull(root);
        return root;
    }
}
