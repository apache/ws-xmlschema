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

import javax.xml.bind.ValidationException;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaException;
import org.apache.ws.commons.schema.walker.XmlSchemaWalker;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.InputSource;

/**
 * A failure during the document walk must stay distinguishable from an internal error, which
 * reporting everything as a bare RuntimeException did not allow.
 */
public class TestPathFinderFailureTypes extends Assert {

    private static final String MAX_DECISION_POINTS_PROPERTY =
        "org.apache.ws.commons.schema.walker.maxDecisionPoints";

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

    private static Throwable rootCause(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    @Test
    public void testFacetViolationIsReportedAsXmlSchemaException() throws Exception {
        try {
            walk("<xs:simpleType name=\"s\"><xs:restriction base=\"xs:string\">"
                 + "<xs:maxLength value=\"2\"/></xs:restriction></xs:simpleType>"
                 + "<xs:element name=\"root\" type=\"t:s\"/>",
                 "<root xmlns=\"urn:t\">toolong</root>");
            fail("expected the facet violation to be reported");
        } catch (XmlSchemaException expected) {
            assertTrue("the ValidationException must be kept as the cause",
                       expected.getCause() instanceof ValidationException);
        }
    }

    @Test
    public void testMalformedFacetIsReportedAsXmlSchemaException() throws Exception {
        try {
            walk("<xs:simpleType name=\"s\"><xs:restriction base=\"xs:string\">"
                 + "<xs:maxLength value=\"bogus\"/></xs:restriction></xs:simpleType>"
                 + "<xs:element name=\"root\" type=\"t:s\"/>",
                 "<root xmlns=\"urn:t\">hello</root>");
            fail("expected the malformed facet to be reported");
        } catch (XmlSchemaException expected) {
            assertTrue("the NumberFormatException must survive in the cause chain",
                       rootCause(expected) instanceof NumberFormatException);
        }
    }

    @Test
    public void testDecisionPointBudgetSurvivesUnwrapped() throws Exception {
        System.setProperty(MAX_DECISION_POINTS_PROPERTY, "1");
        try {
            StringBuilder schema = new StringBuilder(
                "<xs:element name=\"root\"><xs:complexType><xs:sequence>");
            StringBuilder xml = new StringBuilder("<root xmlns=\"urn:t\">");
            for (int i = 0; i < 12; i++) {
                schema.append("<xs:choice minOccurs=\"0\">")
                      .append("<xs:element name=\"a\" type=\"xs:string\"/>")
                      .append("<xs:sequence><xs:element name=\"a\" type=\"xs:string\"/>")
                      .append("<xs:element name=\"b\" type=\"xs:string\" minOccurs=\"0\"/>")
                      .append("</xs:sequence></xs:choice>");
                xml.append("<a>x</a>");
            }
            schema.append("<xs:element name=\"end\" type=\"xs:string\"/>")
                  .append("</xs:sequence></xs:complexType></xs:element>");
            xml.append("</root>");

            walk(schema.toString(), xml.toString());
            fail("expected the decision point budget to be reported");
        } catch (XmlSchemaException expected) {
            assertTrue(expected.getMessage(),
                       expected.getMessage().contains("decision points"));
            assertNull("the budget exception must not be wrapped", expected.getCause());
        } finally {
            System.clearProperty(MAX_DECISION_POINTS_PROPERTY);
        }
    }

    @Test
    public void testValidDocumentStillWalks() throws Exception {
        walk("<xs:simpleType name=\"s\"><xs:restriction base=\"xs:string\">"
             + "<xs:maxLength value=\"16\"/></xs:restriction></xs:simpleType>"
             + "<xs:element name=\"root\" type=\"t:s\"/>",
             "<root xmlns=\"urn:t\">short</root>");
    }
}
