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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.walker.XmlSchemaWalker;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.InputSource;

/**
 * Documents that are valid against their schema - each checked against the JDK's own validator -
 * must be walked without error, and ones that are not must still be refused.
 */
public class TestPathFinderValidDocuments extends Assert {

    private DocumentBuilder docBuilder;

    @Before
    public void setUp() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        docBuilder = factory.newDocumentBuilder();
    }

    private void walk(String schemaBody, String xml) throws Exception {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        collection.read(new StringReader(
            "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:t=\"urn:t\""
            + " targetNamespace=\"urn:t\" elementFormDefault=\"qualified\">"
            + schemaBody + "</xs:schema>"));

        XmlSchemaStateMachineGenerator generator = new XmlSchemaStateMachineGenerator();
        new XmlSchemaWalker(collection, generator)
            .walk(collection.getElementByQName(new QName("urn:t", "root")));

        XmlSchemaPathFinder<Object, Object> pathFinder =
            new XmlSchemaPathFinder<Object, Object>(generator.getStartNode());
        new SaxWalkerOverDom(pathFinder)
            .walk(docBuilder.parse(new InputSource(new StringReader(xml))));
    }

    private void assertRefused(String schemaBody, String xml) throws Exception {
        try {
            walk(schemaBody, xml);
        } catch (RuntimeException expected) {
            return;
        }
        fail("expected the document to be refused: " + xml);
    }

    private static final String LOCAL_WILDCARD =
        "<xs:element name=\"root\"><xs:complexType><xs:sequence>"
        + "<xs:element name=\"a\" type=\"xs:string\"/>"
        + "<xs:any namespace=\"##local\" processContents=\"skip\"/>"
        + "</xs:sequence></xs:complexType></xs:element>";

    /**
     * xmlns="" undeclares the default namespace, which the namespace context used to refuse.
     */
    @Test
    public void testUndeclaredDefaultNamespace() throws Exception {
        walk(LOCAL_WILDCARD, "<root xmlns=\"urn:t\"><a>x</a><x xmlns=\"\"/></root>");
    }

    @Test
    public void testLocalWildcardMatchesAnElementInNoNamespace() throws Exception {
        walk(LOCAL_WILDCARD, "<t:root xmlns:t=\"urn:t\"><t:a>x</t:a><x/></t:root>");
    }

    @Test
    public void testLocalWildcardRefusesAnElementInANamespace() throws Exception {
        assertRefused(LOCAL_WILDCARD, "<root xmlns=\"urn:t\"><a>x</a><x xmlns=\"urn:other\"/></root>");
    }

    @Test
    public void testWildcardAsTheFirstChildOfTheRoot() throws Exception {
        walk("<xs:element name=\"root\"><xs:complexType><xs:sequence>"
             + "<xs:any processContents=\"skip\"/>"
             + "</xs:sequence></xs:complexType></xs:element>",
             "<root xmlns=\"urn:t\"><x/></root>");
    }

    private static final String EMPTY_BASE_EXTENDED =
        "<xs:complexType name=\"base\"><xs:sequence/>"
        + "<xs:attribute name=\"id\" type=\"xs:string\"/></xs:complexType>"
        + "<xs:complexType name=\"derived\"><xs:complexContent><xs:extension base=\"t:base\">"
        + "<xs:sequence><xs:element name=\"a\" type=\"xs:int\"/></xs:sequence>"
        + "</xs:extension></xs:complexContent></xs:complexType>"
        + "<xs:element name=\"root\" type=\"t:derived\"/>";

    @Test
    public void testEmptyBaseSequenceExtendedWithContent() throws Exception {
        walk(EMPTY_BASE_EXTENDED, "<root xmlns=\"urn:t\" id=\"1\"><a>1</a></root>");
    }

    @Test
    public void testEmptyBaseSequenceExtendedWithContentStillChecksIt() throws Exception {
        assertRefused(EMPTY_BASE_EXTENDED, "<root xmlns=\"urn:t\" id=\"1\"><a>notint</a></root>");
    }

    /**
     * A required group whose content can match nothing - here a base type whose elements are all
     * optional - must not stop the document moving on to what follows it.
     */
    @Test
    public void testBaseOfOptionalElementsExtendedWithContent() throws Exception {
        walk("<xs:complexType name=\"base\"><xs:sequence>"
             + "<xs:element name=\"a\" type=\"xs:string\" minOccurs=\"0\"/>"
             + "</xs:sequence></xs:complexType>"
             + "<xs:complexType name=\"derived\"><xs:complexContent><xs:extension base=\"t:base\">"
             + "<xs:sequence><xs:element name=\"b\" type=\"xs:string\"/></xs:sequence>"
             + "</xs:extension></xs:complexContent></xs:complexType>"
             + "<xs:element name=\"root\" type=\"t:derived\"/>",
             "<root xmlns=\"urn:t\"><b>x</b></root>");
    }

    @Test
    public void testChoiceWithAnEmptyOption() throws Exception {
        String schema = "<xs:element name=\"root\"><xs:complexType><xs:sequence>"
                        + "<xs:choice><xs:sequence/><xs:element name=\"a\" type=\"xs:string\"/></xs:choice>"
                        + "<xs:element name=\"b\" type=\"xs:string\"/>"
                        + "</xs:sequence></xs:complexType></xs:element>";
        walk(schema, "<root xmlns=\"urn:t\"><b>x</b></root>");
        walk(schema, "<root xmlns=\"urn:t\"><a>x</a><b>x</b></root>");
        assertRefused(schema, "<root xmlns=\"urn:t\"><c>x</c></root>");
    }

    @Test
    public void testChoiceWithAnOptionalOption() throws Exception {
        walk("<xs:element name=\"root\"><xs:complexType><xs:sequence>"
             + "<xs:choice><xs:element name=\"a\" type=\"xs:string\" minOccurs=\"0\"/>"
             + "<xs:element name=\"c\" type=\"xs:string\"/></xs:choice>"
             + "<xs:element name=\"b\" type=\"xs:string\"/>"
             + "</xs:sequence></xs:complexType></xs:element>",
             "<root xmlns=\"urn:t\"><b>x</b></root>");
    }

    /**
     * Ending an element used to stop at the nearest element of the same name, which in a
     * recursive type is the one just ended rather than its parent.
     */
    @Test
    public void testSelfRecursiveType() throws Exception {
        String schema = "<xs:complexType name=\"T0\"><xs:sequence>"
                        + "<xs:element name=\"c\" type=\"t:T0\" minOccurs=\"0\" maxOccurs=\"3\"/>"
                        + "</xs:sequence></xs:complexType>"
                        + "<xs:element name=\"root\" type=\"t:T0\"/>";
        walk(schema, "<root xmlns=\"urn:t\"><c><c/></c></root>");
        walk(schema, "<root xmlns=\"urn:t\"><c><c><c/></c></c><c><c/></c><c/></root>");
        assertRefused(schema, "<root xmlns=\"urn:t\"><c/><c/><c/><c/></root>");
    }
}
