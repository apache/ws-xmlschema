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

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAnnotation;
import org.apache.ws.commons.schema.XmlSchemaAnnotationItem;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaDocumentation;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaEnumerationFacet;
import org.apache.ws.commons.schema.XmlSchemaFacet;
import org.apache.ws.commons.schema.XmlSchemaNotation;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;

import org.junit.Assert;
import org.junit.Test;

/*
 * Copyright 2004,2007 The Apache Software Foundation.
 * Copyright 2006 International Business Machines Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * @author Brent Ulbricht 
 */
public class NotationTest extends Assert {

    /**
     * This method will test the notation.
     * 
     * @throws Exception Any exception encountered
     */
    @Test
    public void testNotation() throws Exception {

        /*
         * <schema xmlns="http://www.w3.org/2001/XMLSchema" xmlns:xsd="http://www.w3.org/2001/XMLSchema"
         * xmlns:tns="http://soapinterop.org/types" targetNamespace="http://soapinterop.org/types"> <notation
         * name="teamLogo" system="com/team/graphics/teamLogo" public="http://www.team.com/graphics/teamLogo"
         * id="notation.teamLogo"> <annotation> <documentation xml:lang="en">Location of the corporate
         * logo.</documentation> </annotation> </notation> <notation name="teamMascot"
         * system="com/team/graphics/teamMascot" public="http://www.team.com/graphics/teamMascot"
         * id="notation.teamMascot"> <annotation> <documentation xml:lang="en">Location of the corporate
         * mascot.</documentation> </annotation> </notation> <element name="demoNotation"> <simpleType>
         * <restriction base="NOTATION"> <enumeration value="tns:teamLogo"/> <enumeration
         * value="tns:teamMascot"/> </restriction> </simpleType> </element> </schema>
         */

        QName elementQName = new QName("http://soapinterop.org/types", "demoNotation");
        QName notationName = new QName("http://soapinterop.org/types", "teamLogo");

        InputStream is = new FileInputStream(Resources.asURI("notation.xsd"));
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        XmlSchema schema = schemaCol.read(new StreamSource(is));

        testSimpleRestrictions(elementQName, notationName, schemaCol, schema);

        Map<QName, XmlSchemaNotation> notations = schema.getNotations();
        assertEquals(2, notations.size());

        Set<String> s = new HashSet<String>();
        s.add("teamMascot");
        s.add("teamLogo");
        for (Map.Entry<QName, XmlSchemaNotation> e : notations.entrySet()) {
            String name = e.getKey().getLocalPart();
            if (!("teamLogo".equals(name) || "teamMascot".equals(name))) {
                fail("An unexpected name of \"" + name + "\" was found.");
            }
            assertTrue(s.remove(name));
        }
        assertTrue("The set should have been empty, but instead contained: " + s + ".", s.isEmpty());

        s.clear();
        s.add("teamMascot");
        s.add("teamLogo");
        for (Map.Entry<QName, XmlSchemaNotation> e : notations.entrySet()) {
            XmlSchemaNotation xsn = e.getValue();
            String name = xsn.getName();
            XmlSchemaAnnotation xsa = xsn.getAnnotation();
            List<XmlSchemaAnnotationItem> col = xsa.getItems();
            assertEquals(1, col.size());
            XmlSchemaDocumentation xsd = null;
            for (int k = 0; k < col.size(); k++) {
                xsd = (XmlSchemaDocumentation)col.get(k);
            }
            if ("teamMascot".equals(name)) {
                assertEquals("http://www.team.com/graphics/teamMascot", xsn.getPublic());
                assertEquals("com/team/graphics/teamMascot", xsn.getSystem());
                assertEquals("notation.teamMascot", xsn.getId());
                assertEquals("en", xsd.getLanguage());
                NodeList nl = xsd.getMarkup();
                for (int j = 0; j < nl.getLength(); j++) {
                    Node n = nl.item(j);
                    if (n.getNodeType() == Node.TEXT_NODE) {
                        assertEquals("Location of the corporate mascot.", n.getNodeValue());
                    }
                }
            } else if ("teamLogo".equals(name)) {
                assertEquals("http://www.team.com/graphics/teamLogo", xsn.getPublic());
                assertEquals("com/team/graphics/teamLogo", xsn.getSystem());
                assertEquals("notation.teamLogo", xsn.getId());
                assertEquals("en", xsd.getLanguage());
                NodeList nl = xsd.getMarkup();
                for (int j = 0; j < nl.getLength(); j++) {
                    Node n = nl.item(j);
                    if (n.getNodeType() == Node.TEXT_NODE) {
                        assertEquals("Location of the corporate logo.", n.getNodeValue());
                    }
                }
            } else {
                fail("An unexpected name of \"" + name + "\" was found.");
            }
            assertTrue(s.remove(name));
        }
        assertTrue("The set should have been empty, but instead contained: " + s + ".", s.isEmpty());

    }
    
    private void testSimpleRestrictions(QName elementQName, QName notationName, XmlSchemaCollection schemaCol,
                                  XmlSchema schema) {
        Map<QName, XmlSchemaNotation> notations = schema.getNotations();
        assertNotNull(notations.get(notationName));

        XmlSchemaElement elem = schemaCol.getElementByQName(elementQName);
        assertNotNull(elem);
        assertEquals("demoNotation", elem.getName());
        assertEquals(new QName("http://soapinterop.org/types", "demoNotation"), elem.getQName());

        XmlSchemaSimpleType type = (XmlSchemaSimpleType)elem.getSchemaType();
        assertNotNull(type);

        XmlSchemaSimpleTypeRestriction xsstc = (XmlSchemaSimpleTypeRestriction)type.getContent();
        assertEquals(new QName("http://www.w3.org/2001/XMLSchema", "NOTATION"), xsstc.getBaseTypeName());

        List<XmlSchemaFacet> xsoc = xsstc.getFacets();
        assertEquals(2, xsoc.size());
        Set<String> s = new HashSet<String>();
        s.add("tns:teamLogo");
        s.add("tns:teamMascot");
        for (int i = 0; i < xsoc.size(); i++) {
            XmlSchemaEnumerationFacet xsef = (XmlSchemaEnumerationFacet)xsoc.get(i);
            String value = (String)xsef.getValue();
            if (!("tns:teamLogo".equals(value) || "tns:teamMascot".equals(value))) {
                fail("An unexpected value of \"" + value + "\" was found.");
            }
            assertTrue(s.remove(value));
        }
        assertTrue("The set should have been empty, but instead contained: " + s + ".", s.isEmpty());
    }

}
