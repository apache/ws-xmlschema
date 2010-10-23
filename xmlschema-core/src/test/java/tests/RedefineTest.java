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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroup;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroupMember;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaContentModel;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaExternal;
import org.apache.ws.commons.schema.XmlSchemaFacet;
import org.apache.ws.commons.schema.XmlSchemaGroup;
import org.apache.ws.commons.schema.XmlSchemaGroupRef;
import org.apache.ws.commons.schema.XmlSchemaMaxInclusiveFacet;
import org.apache.ws.commons.schema.XmlSchemaMinInclusiveFacet;
import org.apache.ws.commons.schema.XmlSchemaRedefine;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.XmlSchemaType;

import org.junit.Assert;
import org.junit.Test;

public class RedefineTest extends Assert {

    /**
     * This method will test a complex type redefine.
     * 
     * @throws Exception Any exception encountered
     */
    @Test
    public void testComplexTypeRedefine() throws Exception {

        /*
         * redefine1.xsd ----------------- <schema xmlns="http://www.w3.org/2001/XMLSchema"
         * xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:tns="http://soapinterop.org/types"
         * targetNamespace="http://soapinterop.org/types"> <complexType name="person"> <sequence> <element
         * name="firstname" type="string"/> <element name="lastname" type="string"/> </sequence>
         * </complexType> <element name="customer" type="tns:person"/> </schema> redefine2.xsd
         * ----------------- <schema xmlns="http://www.w3.org/2001/XMLSchema"
         * xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:tns="http://soapinterop.org/types"
         * targetNamespace="http://soapinterop.org/types"> <redefine
         * schemaLocation="src/test/test-resources/redefine1.xsd"> <complexType name="person">
         * <complexContent> <extension base="tns:person"> <sequence> <element name="id" type="string"/>
         * </sequence> </extension> </complexContent> </complexType> </redefine> <element name="vip"
         * type="tns:person"/> </schema>
         */

        InputStream is = new FileInputStream(Resources.asURI("redefine2.xsd"));
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        XmlSchema schema = schemaCol.read(new StreamSource(is));

        Map<QName, XmlSchemaElement> xsot = schema.getElements();
        assertEquals(1, xsot.size());

        XmlSchemaElement xse = null;
        Iterator<XmlSchemaElement> ei = xsot.values().iterator();
        while (ei.hasNext()) {
            xse = ei.next();
        }
        assertEquals("vip", xse.getName());
        assertEquals(new QName("http://soapinterop.org/types", "person"), xse.getSchemaTypeName());

        List<XmlSchemaExternal> xsoc = schema.getExternals();
        assertEquals(1, xsoc.size());

        XmlSchemaRedefine xsr = (XmlSchemaRedefine)xsoc.get(0);
        Map<QName, XmlSchemaType> redefTypes = xsr.getSchemaTypes();
        assertEquals(1, redefTypes.size());

        for (Iterator<QName> i = redefTypes.keySet().iterator(); i.hasNext();) {
            QName qname = i.next();
            assertEquals(new QName("http://soapinterop.org/types", "person"), qname);
        }

        XmlSchemaComplexType xsct = null;
        for (Iterator<XmlSchemaType> i = redefTypes.values().iterator(); i.hasNext();) {
            xsct = (XmlSchemaComplexType)i.next();
        }
        assertNotNull(xsct);

        XmlSchemaContentModel xscm = xsct.getContentModel();
        assertNotNull(xscm);

        XmlSchemaComplexContentExtension xscce = (XmlSchemaComplexContentExtension)xscm.getContent();
        assertEquals(new QName("http://soapinterop.org/types", "person"), xscce.getBaseTypeName());

        XmlSchemaSequence xsp = (XmlSchemaSequence)xscce.getParticle();
        assertNotNull(xsp);

        List<XmlSchemaSequenceMember> c = xsp.getItems();
        assertEquals(1, c.size());

        xse = null;
        for (int i = 0; i < c.size(); i++) {
            xse = (XmlSchemaElement)c.get(i);
        }
        assertEquals("id", xse.getName());
        assertEquals(new QName("http://www.w3.org/2001/XMLSchema", "string"), xse.getSchemaTypeName());

    }

    /**
     * This method will test a simple type redefine.
     * 
     * @throws Exception Any exception encountered
     */
    @Test
    public void testSimpleTypeRedefine() throws Exception {
        /*
         * redefine3.xsd ----------------- <schema xmlns="http://www.w3.org/2001/XMLSchema"
         * xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:tns="http://soapinterop.org/types"
         * targetNamespace="http://soapinterop.org/types"> <simpleType name="drinksize"> <restriction
         * base="integer"/> </simpleType> <element name="size" type="tns:drinksize"/> </schema> redefine4.xsd
         * ----------------- <schema xmlns="http://www.w3.org/2001/XMLSchema"
         * xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:tns="http://soapinterop.org/types"
         * targetNamespace="http://soapinterop.org/types"> <redefine
         * schemaLocation="test-resources/redefine3.xsd"> <simpleType name="drinksize"> <restriction
         * base="tns:drinksize"> <minInclusive value="1"/> <maxInclusive value="3"/> </restriction>
         * </simpleType> </redefine> <element name="childsizedrink" type="tns:drinksize"/> </schema>
         */

        InputStream is = new FileInputStream(Resources.asURI("redefine4.xsd"));
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        XmlSchema schema = schemaCol.read(new StreamSource(is));

        Map<QName, XmlSchemaElement> elements = schema.getElements();
        assertEquals(1, elements.size());

        XmlSchemaElement xse = null;
        Iterator<XmlSchemaElement> ei = elements.values().iterator();
        while (ei.hasNext()) {
            xse = ei.next();
        }
        assertEquals("childsizedrink", xse.getName());
        assertEquals(new QName("http://soapinterop.org/types", "drinksize"), xse.getSchemaTypeName());

        List<XmlSchemaExternal> xsoc = schema.getExternals();
        assertEquals(1, xsoc.size());

        XmlSchemaRedefine xsr = (XmlSchemaRedefine)xsoc.get(0);
        Map<QName, XmlSchemaType> xsot = xsr.getSchemaTypes();
        assertEquals(1, xsot.size());

        for (Iterator<QName> i = xsot.keySet().iterator(); i.hasNext();) {
            QName qname = i.next();
            assertEquals(new QName("http://soapinterop.org/types", "drinksize"), qname);
        }

        XmlSchemaSimpleType xsst = null;
        for (Iterator<XmlSchemaType> i = xsot.values().iterator(); i.hasNext();) {
            xsst = (XmlSchemaSimpleType)i.next();
        }
        assertNotNull(xsst);

        XmlSchemaSimpleTypeRestriction xsstr = (XmlSchemaSimpleTypeRestriction)xsst.getContent();
        assertEquals(new QName("http://soapinterop.org/types", "drinksize"), xsstr.getBaseTypeName());

        List<XmlSchemaFacet> facets = xsstr.getFacets();

        Set<String> s = new HashSet<String>();
        s.add(XmlSchemaMinInclusiveFacet.class.getName());
        s.add(XmlSchemaMaxInclusiveFacet.class.getName());
        for (Iterator<XmlSchemaFacet> i = facets.iterator(); i.hasNext();) {
            Object o = i.next();
            assertTrue(s.remove(o.getClass().getName()));
            if (o instanceof XmlSchemaMinInclusiveFacet) {
                assertEquals("1", ((XmlSchemaMinInclusiveFacet)o).getValue());
            } else if (o instanceof XmlSchemaMaxInclusiveFacet) {
                assertEquals("3", ((XmlSchemaMaxInclusiveFacet)o).getValue());
            } else {
                fail("Unexpected object encountered: " + o.getClass().getName());
            }
        }

        assertTrue("The set should have been empty, but instead contained: " + s + ".", s.isEmpty());

    }

    /**
     * This method will test a group redefine.
     * 
     * @throws Exception Any exception encountered
     */
    @Test
    public void testGroupRedefine() throws Exception {

        /*
         * redefine5.xsd ----------------- <schema xmlns="http://www.w3.org/2001/XMLSchema"
         * xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:tns="http://soapinterop.org/types"
         * targetNamespace="http://soapinterop.org/types"> <group name="PrologGroup"> <sequence> <element
         * name="date" type="string"/> <element name="author" type="string"/> <element name="defect"
         * type="integer"/> </sequence> </group> </schema> redefine6.xsd ----------------- <schema
         * xmlns="http://www.w3.org/2001/XMLSchema" xmlns:xsd="http://www.w3.org/2001/XMLSchema"
         * xmlns:tns="http://soapinterop.org/types" targetNamespace="http://soapinterop.org/types"> <redefine
         * schemaLocation="redefine5.xsd"> <group name="PrologGroup"> <sequence> <group
         * ref="tns:PrologGroup"/> <element name="description" type="string"/> </sequence> </group>
         * </redefine> </schema>
         */

        InputStream is = new FileInputStream(Resources.asURI("redefine6.xsd"));
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        XmlSchema schema = schemaCol.read(new StreamSource(is));

        List<XmlSchemaExternal> xsoc = schema.getExternals();
        assertEquals(1, xsoc.size());

        XmlSchemaRedefine xsr = (XmlSchemaRedefine)xsoc.get(0);
        Map<QName, XmlSchemaGroup> xsot = xsr.getGroups();
        assertEquals(1, xsot.size());

        for (Iterator<QName> i = xsot.keySet().iterator(); i.hasNext();) {
            assertEquals("PrologGroup", i.next().getLocalPart());
        }

        XmlSchemaGroup xsg = null;
        for (Iterator<XmlSchemaGroup> i = xsot.values().iterator(); i.hasNext();) {
            xsg = i.next();
        }

        XmlSchemaSequence xss = (XmlSchemaSequence)xsg.getParticle();

        List<XmlSchemaSequenceMember> sequenceItems = xss.getItems();
        assertEquals(2, sequenceItems.size());

        Set<String> s = new HashSet<String>();
        s.add(XmlSchemaGroupRef.class.getName());
        s.add(XmlSchemaElement.class.getName());
        for (Iterator i = sequenceItems.iterator(); i.hasNext();) {
            Object o = i.next();
            assertTrue(s.remove(o.getClass().getName()));
            if (o instanceof XmlSchemaGroupRef) {
                assertEquals(new QName("http://soapinterop.org/types", "PrologGroup"), ((XmlSchemaGroupRef)o)
                    .getRefName());
            } else if (o instanceof XmlSchemaElement) {
                assertEquals("description", ((XmlSchemaElement)o).getName());
            } else {
                fail("Unexpected object encountered: " + o.getClass().getName());
            }
        }

        assertTrue("The set should have been empty, but instead contained: " + s + ".", s.isEmpty());

    }

    /**
     * This method will test a attribute group redefine.
     * 
     * @throws Exception Any exception encountered
     */
    @Test
    public void testAttributeGroupRedefine() throws Exception {

        /*
         * redefine7.xsd ----------------- <schema xmlns="http://www.w3.org/2001/XMLSchema"
         * xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:tns="http://soapinterop.org/types"
         * targetNamespace="http://soapinterop.org/types"> <attributeGroup name="AttribGroup"> <attribute
         * name="type" type="string"/> <attribute name="units" type="string"/> <attribute name="serialId"
         * type="string"/> </attributeGroup> </schema> redefine8.xsd ----------------- <schema
         * xmlns="http://www.w3.org/2001/XMLSchema" xmlns:xsd="http://www.w3.org/2001/XMLSchema"
         * xmlns:tns="http://soapinterop.org/types" targetNamespace="http://soapinterop.org/types"> <redefine
         * schemaLocation="redefine7.xsd"> <attributeGroup name="AttribGroup"> <attribute name="type"
         * type="string"/> <attribute name="units" type="string"/> </attributeGroup> </redefine> </schema>
         */

        InputStream is = new FileInputStream(Resources.asURI("redefine8.xsd"));
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        XmlSchema schema = schemaCol.read(new StreamSource(is));

        List<XmlSchemaExternal> xsoc = schema.getExternals();
        assertEquals(1, xsoc.size());

        XmlSchemaRedefine xsr = (XmlSchemaRedefine)xsoc.get(0);
        Map<QName, XmlSchemaAttributeGroup> xsot = xsr.getAttributeGroups();
        assertEquals(1, xsot.size());

        for (Iterator<QName> i = xsot.keySet().iterator(); i.hasNext();) {
            assertEquals("AttribGroup", (i.next()).getLocalPart());
        }

        XmlSchemaAttributeGroup xsag = null;
        for (Iterator<XmlSchemaAttributeGroup> i = xsot.values().iterator(); i.hasNext();) {
            xsag = (XmlSchemaAttributeGroup)i.next();
        }

        assertNotNull(xsag);
        assertEquals("AttribGroup", xsag.getName());
        List<XmlSchemaAttributeGroupMember> attributes = xsag.getAttributes();

        Set<String> s = new HashSet<String>();
        s.add("type");
        s.add("units");
        for (Iterator i = attributes.iterator(); i.hasNext();) {
            XmlSchemaAttribute xsa = (XmlSchemaAttribute)i.next();
            assertTrue(s.remove(xsa.getName()));
        }

        assertTrue("The set should have been empty, but instead contained: " + s + ".", s.isEmpty());

    }

    /**
     * This method will test a complex type redefine. Similar to the first test but now there are multiple
     * layers of includes
     * 
     * @throws Exception Any exception encountered
     */
    @Test
    public void testComplexTypeRedefineWithRelativeImports() throws Exception {

        /*
         * redefine-import2.xsd <schema xmlns="http://www.w3.org/2001/XMLSchema"
         * xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:tns="http://soapinterop.org/types"
         * targetNamespace="http://soapinterop.org/types"> <complexType name="person"> <sequence> <element
         * name="firstname" type="string"/> <element name="lastname" type="string"/> </sequence>
         * </complexType> <element name="customer" type="tns:person"/> </schema>
         */

        /*
         * redefine-import1.xsd <schema xmlns="http://www.w3.org/2001/XMLSchema"
         * xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:tns="http://soapinterop.org/types"
         * targetNamespace="http://soapinterop.org/types"> <!-- relative import to this location -->
         * <xsd:include schemaLocation="redefine-import2.xsd"></xsd:include> </schema>
         */
        /*
         * redefine9.xsd ----------------- <schema xmlns="http://www.w3.org/2001/XMLSchema"
         * xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:tns="http://soapinterop.org/types"
         * targetNamespace="http://soapinterop.org/types"> <!-- Relative import --> <xsd:include
         * schemaLocation="redefine-include/redefine-import1.xsd"></xsd:include> </schema> redefine10.xsd
         * ----------------- <schema xmlns="http://www.w3.org/2001/XMLSchema"
         * xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:tns="http://soapinterop.org/types"
         * targetNamespace="http://soapinterop.org/types"> <redefine
         * schemaLocation="src/test/test-resources/redefine9.xsd"> <complexType name="person">
         * <complexContent> <extension base="tns:person"> <sequence> <element name="id" type="string"/>
         * </sequence> </extension> </complexContent> </complexType> </redefine> <element name="vip"
         * type="tns:person"/> </schema>
         */

        InputStream is = new FileInputStream(Resources.asURI("redefine10.xsd"));
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        XmlSchema schema = schemaCol.read(new StreamSource(is));

        Map<QName, XmlSchemaElement> elements = schema.getElements();
        assertEquals(1, elements.size());

        XmlSchemaElement xse = null;
        Iterator<XmlSchemaElement> ei = elements.values().iterator();
        while (ei.hasNext()) {
            xse = ei.next();
        }
        assertEquals("vip", xse.getName());
        assertEquals(new QName("http://soapinterop.org/types", "person"), xse.getSchemaTypeName());

        List<XmlSchemaExternal> xsoc = schema.getExternals();
        assertEquals(1, xsoc.size());

        XmlSchemaRedefine xsr = (XmlSchemaRedefine)xsoc.get(0);
        Map<QName, XmlSchemaType> xsot = xsr.getSchemaTypes();
        assertEquals(1, xsot.size());

        for (Iterator<QName> i = xsot.keySet().iterator(); i.hasNext();) {
            QName qname = i.next();
            assertEquals(new QName("http://soapinterop.org/types", "person"), qname);
        }

        XmlSchemaComplexType xsct = null;
        for (Iterator<XmlSchemaType> i = xsot.values().iterator(); i.hasNext();) {
            xsct = (XmlSchemaComplexType)i.next();
        }
        assertNotNull(xsct);

        XmlSchemaContentModel xscm = xsct.getContentModel();
        assertNotNull(xscm);

        XmlSchemaComplexContentExtension xscce = (XmlSchemaComplexContentExtension)xscm.getContent();
        assertEquals(new QName("http://soapinterop.org/types", "person"), xscce.getBaseTypeName());

        XmlSchemaSequence xsp = (XmlSchemaSequence)xscce.getParticle();
        assertNotNull(xsp);

        List<XmlSchemaSequenceMember> c = xsp.getItems();
        assertEquals(1, c.size());

        xse = null;
        for (int i = 0; i < c.size(); i++) {
            xse = (XmlSchemaElement)c.get(i);
        }
        assertEquals("id", xse.getName());
        assertEquals(new QName("http://www.w3.org/2001/XMLSchema", "string"), xse.getSchemaTypeName());

    }
}
