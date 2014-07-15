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
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaEnumerationFacet;
import org.apache.ws.commons.schema.XmlSchemaFacet;
import org.apache.ws.commons.schema.XmlSchemaFractionDigitsFacet;
import org.apache.ws.commons.schema.XmlSchemaLengthFacet;
import org.apache.ws.commons.schema.XmlSchemaMaxExclusiveFacet;
import org.apache.ws.commons.schema.XmlSchemaMaxInclusiveFacet;
import org.apache.ws.commons.schema.XmlSchemaMaxLengthFacet;
import org.apache.ws.commons.schema.XmlSchemaMinExclusiveFacet;
import org.apache.ws.commons.schema.XmlSchemaMinInclusiveFacet;
import org.apache.ws.commons.schema.XmlSchemaMinLengthFacet;
import org.apache.ws.commons.schema.XmlSchemaPatternFacet;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.XmlSchemaTotalDigitsFacet;
import org.apache.ws.commons.schema.XmlSchemaWhiteSpaceFacet;
import org.apache.ws.commons.schema.constants.Constants;

import org.junit.Assert;
import org.junit.Test;

public class FacetsTest extends Assert {

    /**
     * This method will test for the length facet.
     * 
     * @throws Exception Any exception encountered
     */
    @Test
    public void testLengthFacet() throws Exception {

        /*
         * <simpleType name="zipCode"> <restriction base="string"> <length value="5"/> <pattern
         * value="\d{5}"/> </restriction> </simpleType> <element name="myZipCode" type="tns:zipCode"/>
         */

        QName elementQName = new QName("http://soapinterop.org/types", "myZipCode");
        InputStream is = new FileInputStream(Resources.asURI("facets.xsd"));
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        schemaCol.read(new StreamSource(is));

        XmlSchemaElement elem = schemaCol.getElementByQName(elementQName);
        assertNotNull(elem);
        assertEquals("myZipCode", elem.getName());
        assertEquals(new QName("http://soapinterop.org/types", "myZipCode"), elem.getQName());
        assertEquals(new QName("http://soapinterop.org/types", "zipCode"), elem.getSchemaTypeName());

        XmlSchemaSimpleType simpleType = (XmlSchemaSimpleType)elem.getSchemaType();

        XmlSchemaSimpleTypeRestriction r = (XmlSchemaSimpleTypeRestriction)simpleType.getContent();
        assertEquals(new QName("http://www.w3.org/2001/XMLSchema", "string"), r.getBaseTypeName());

        XmlSchemaSimpleType xsst = r.getBaseType();
        assertNull(xsst);

        List<XmlSchemaFacet> collection = r.getFacets();
        assertEquals(2, collection.size());

        Set<String> s = new HashSet<String>();
        s.add(XmlSchemaLengthFacet.class.getName());
        s.add(XmlSchemaPatternFacet.class.getName());
        for (Iterator<XmlSchemaFacet> i = collection.iterator(); i.hasNext();) {
            Object o = i.next();
            assertTrue(s.remove(o.getClass().getName()));
            if (o instanceof XmlSchemaLengthFacet) {
                assertEquals("5", ((XmlSchemaLengthFacet)o).getValue());
                assertEquals(false, ((XmlSchemaLengthFacet)o).isFixed());
            } else if (o instanceof XmlSchemaPatternFacet) {
                assertEquals("\\d{5}", ((XmlSchemaPatternFacet)o).getValue());
                assertEquals(false, ((XmlSchemaPatternFacet)o).isFixed());
            } else {
                fail("Unexpected object encountered: " + o.getClass().getName());
            }
        }

        assertTrue("The set should have been empty, but instead contained: " + s + ".", s.isEmpty());

    }

    /**
     * This method will test for the pattern facet.
     * 
     * @throws Exception Any Exception encountered
     */
    @Test
    public void testPatternFacet() throws Exception {

        /*
         * <simpleType name="creditCardNumber"> <restriction base="integer"> <pattern value="\d{15}"/>
         * </restriction> </simpleType> <element name="myCreditCardNumber" type="tns:creditCardNumber"/>
         */

        QName elementQName = new QName("http://soapinterop.org/types", "myCreditCardNumber");
        InputStream is = new FileInputStream(Resources.asURI("facets.xsd"));
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        schemaCol.read(new StreamSource(is));

        XmlSchemaElement elem = schemaCol.getElementByQName(elementQName);
        assertNotNull(elem);
        assertEquals("myCreditCardNumber", elem.getName());
        assertEquals(new QName("http://soapinterop.org/types", "myCreditCardNumber"), elem.getQName());
        assertEquals(new QName("http://soapinterop.org/types", "creditCardNumber"), elem.getSchemaTypeName());

        XmlSchemaSimpleType simpleType = (XmlSchemaSimpleType)elem.getSchemaType();

        XmlSchemaSimpleTypeRestriction r = (XmlSchemaSimpleTypeRestriction)simpleType.getContent();
        assertEquals(new QName("http://www.w3.org/2001/XMLSchema", "integer"), r.getBaseTypeName());

        XmlSchemaSimpleType xsst = r.getBaseType();
        assertNull(xsst);

        List<XmlSchemaFacet> collection = r.getFacets();
        assertEquals(1, collection.size());

        Set<String> s = new HashSet<String>();
        s.add(XmlSchemaPatternFacet.class.getName());
        for (Iterator<XmlSchemaFacet> i = collection.iterator(); i.hasNext();) {
            Object o = i.next();
            assertTrue(s.remove(o.getClass().getName()));
            if (o instanceof XmlSchemaPatternFacet) {
                assertEquals("\\d{15}", ((XmlSchemaPatternFacet)o).getValue());
                assertEquals(false, ((XmlSchemaPatternFacet)o).isFixed());
            } else {
                fail("Unexpected object encountered: " + o.getClass().getName());
            }
        }

        assertTrue("The set should have been empty, but instead contained: " + s + ".", s.isEmpty());

    }

    /**
     * This method will test the total digits facet.
     * 
     * @throws Exception Any exception encountered
     */
    @Test
    public void testTotalDigitsFacet() throws Exception {

        /*
         * <simpleType name="age"> <restriction base="decimal"> <totalDigits value="3"/> </restriction>
         * </simpleType> <element name="myAge" type="tns:age"/>
         */

        QName elementQName = new QName("http://soapinterop.org/types", "myAge");
        InputStream is = new FileInputStream(Resources.asURI("facets.xsd"));
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        schemaCol.read(new StreamSource(is));

        XmlSchemaElement elem = schemaCol.getElementByQName(elementQName);
        assertNotNull(elem);
        assertEquals("myAge", elem.getName());
        assertEquals(new QName("http://soapinterop.org/types", "myAge"), elem.getQName());
        assertEquals(new QName("http://soapinterop.org/types", "age"), elem.getSchemaTypeName());

        XmlSchemaSimpleType simpleType = (XmlSchemaSimpleType)elem.getSchemaType();

        XmlSchemaSimpleTypeRestriction r = (XmlSchemaSimpleTypeRestriction)simpleType.getContent();
        assertEquals(new QName("http://www.w3.org/2001/XMLSchema", "decimal"), r.getBaseTypeName());

        XmlSchemaSimpleType xsst = r.getBaseType();
        assertNull(xsst);

        List<XmlSchemaFacet> collection = r.getFacets();
        assertEquals(1, collection.size());

        Set<String> s = new HashSet<String>();
        s.add(XmlSchemaTotalDigitsFacet.class.getName());
        for (Iterator<XmlSchemaFacet> i = collection.iterator(); i.hasNext();) {
            Object o = i.next();
            assertTrue(s.remove(o.getClass().getName()));
            if (o instanceof XmlSchemaTotalDigitsFacet) {
                assertEquals("3", ((XmlSchemaTotalDigitsFacet)o).getValue());
                assertEquals(false, ((XmlSchemaTotalDigitsFacet)o).isFixed());
            } else {
                fail("Unexpected object encountered: " + o.getClass().getName());
            }
        }

        assertTrue("The set should have been empty, but instead contained: " + s + ".", s.isEmpty());

    }

    /**
     * This method will test the Min and Max Inclusive facets.
     * 
     * @throws Exception Any Exception encountered
     */
    @Test
    public void testMinMaxInclusiveFacets() throws Exception {

        /*
         * <simpleType name="distance"> <restriction base="integer"> <maxInclusive value="100" fixed="true"/>
         * <minInclusive value="0"/> </restriction> </simpleType> <element name="myDistance"
         * type="tns:distance"/>
         */

        QName elementQName = new QName("http://soapinterop.org/types", "myDistance");
        InputStream is = new FileInputStream(Resources.asURI("facets.xsd"));
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        schemaCol.read(new StreamSource(is));

        XmlSchemaElement elem = schemaCol.getElementByQName(elementQName);
        assertNotNull(elem);
        assertEquals("myDistance", elem.getName());
        assertEquals(new QName("http://soapinterop.org/types", "myDistance"), elem.getQName());
        assertEquals(new QName("http://soapinterop.org/types", "distance"), elem.getSchemaTypeName());

        XmlSchemaSimpleType simpleType = (XmlSchemaSimpleType)elem.getSchemaType();

        XmlSchemaSimpleTypeRestriction r = (XmlSchemaSimpleTypeRestriction)simpleType.getContent();
        assertEquals(new QName("http://www.w3.org/2001/XMLSchema", "integer"), r.getBaseTypeName());

        XmlSchemaSimpleType xsst = r.getBaseType();
        assertNull(xsst);

        List<XmlSchemaFacet> collection = r.getFacets();
        assertEquals(2, collection.size());

        Set<String> s = new HashSet<String>();
        s.add(XmlSchemaMaxInclusiveFacet.class.getName());
        s.add(XmlSchemaMinInclusiveFacet.class.getName());
        for (Iterator<XmlSchemaFacet> i = collection.iterator(); i.hasNext();) {
            Object o = i.next();
            assertTrue(s.remove(o.getClass().getName()));
            if (o instanceof XmlSchemaMaxInclusiveFacet) {
                assertEquals("100", ((XmlSchemaMaxInclusiveFacet)o).getValue());
                assertEquals(true, ((XmlSchemaMaxInclusiveFacet)o).isFixed());
            } else if (o instanceof XmlSchemaMinInclusiveFacet) {
                assertEquals("0", ((XmlSchemaMinInclusiveFacet)o).getValue());
                assertEquals(false, ((XmlSchemaMinInclusiveFacet)o).isFixed());
            } else {
                fail("Unexpected object encountered: " + o.getClass().getName());
            }
        }

        assertTrue("The set should have been empty, but instead contained: " + s + ".", s.isEmpty());

    }

    /**
     * This method will test the Min and Max Exclusive facets.
     * 
     * @throws Exception Any Exception encountered
     */
    @Test
    public void testMinMaxExlusiveFacets() throws Exception {

        /*
         * <simpleType name="weight"> <restriction base="integer"> <maxExclusive value="200"/> <minExclusive
         * value="1"/> </restriction> </simpleType> <element name="myWeight" type="tns:weight"/>
         */

        QName elementQName = new QName("http://soapinterop.org/types", "myWeight");
        InputStream is = new FileInputStream(Resources.asURI("facets.xsd"));
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        schemaCol.read(new StreamSource(is));

        XmlSchemaElement elem = schemaCol.getElementByQName(elementQName);
        assertNotNull(elem);
        assertEquals("myWeight", elem.getName());
        assertEquals(new QName("http://soapinterop.org/types", "myWeight"), elem.getQName());
        assertEquals(new QName("http://soapinterop.org/types", "weight"), elem.getSchemaTypeName());

        XmlSchemaSimpleType simpleType = (XmlSchemaSimpleType)elem.getSchemaType();

        XmlSchemaSimpleTypeRestriction r = (XmlSchemaSimpleTypeRestriction)simpleType.getContent();
        assertEquals(new QName("http://www.w3.org/2001/XMLSchema", "integer"), r.getBaseTypeName());

        XmlSchemaSimpleType xsst = r.getBaseType();
        assertNull(xsst);

        List<XmlSchemaFacet> collection = r.getFacets();
        assertEquals(2, collection.size());

        Set<String> s = new HashSet<String>();
        s.add(XmlSchemaMaxExclusiveFacet.class.getName());
        s.add(XmlSchemaMinExclusiveFacet.class.getName());
        for (Iterator<XmlSchemaFacet> i = collection.iterator(); i.hasNext();) {
            Object o = i.next();
            assertTrue(s.remove(o.getClass().getName()));
            if (o instanceof XmlSchemaMaxExclusiveFacet) {
                assertEquals("200", ((XmlSchemaMaxExclusiveFacet)o).getValue());
                assertEquals(false, ((XmlSchemaMaxExclusiveFacet)o).isFixed());
            } else if (o instanceof XmlSchemaMinExclusiveFacet) {
                assertEquals("1", ((XmlSchemaMinExclusiveFacet)o).getValue());
                assertEquals(false, ((XmlSchemaMinExclusiveFacet)o).isFixed());
            } else {
                fail("Unexpected object encountered: " + o.getClass().getName());
            }
        }

        assertTrue("The set should have been empty, but instead contained: " + s + ".", s.isEmpty());

    }

    /**
     * This will test the whiteSpace facet.
     * 
     * @throws Exception Any Exception encountered
     */
    @Test
    public void testWhiteSpaceFacet() throws Exception {

        /*
         * <simpleType name="noWhiteSpace"> <restriction base="integer"> <whiteSpace value="collapse"/>
         * </restriction> </simpleType> <element name="myWhiteSpace" type="tns:noWhiteSpace"/>
         */

        QName elementQName = new QName("http://soapinterop.org/types", "myWhiteSpace");
        InputStream is = new FileInputStream(Resources.asURI("facets.xsd"));
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        schemaCol.read(new StreamSource(is));

        XmlSchemaElement elem = schemaCol.getElementByQName(elementQName);
        assertNotNull(elem);
        assertEquals("myWhiteSpace", elem.getName());
        assertEquals(new QName("http://soapinterop.org/types", "myWhiteSpace"), elem.getQName());
        assertEquals(new QName("http://soapinterop.org/types", "noWhiteSpace"), elem.getSchemaTypeName());

        XmlSchemaSimpleType simpleType = (XmlSchemaSimpleType)elem.getSchemaType();

        XmlSchemaSimpleTypeRestriction r = (XmlSchemaSimpleTypeRestriction)simpleType.getContent();
        assertEquals(new QName("http://www.w3.org/2001/XMLSchema", "normalizedString"), r.getBaseTypeName());

        XmlSchemaSimpleType xsst = r.getBaseType();
        assertNull(xsst);

        List<XmlSchemaFacet> collection = r.getFacets();
        assertEquals(1, collection.size());

        Set<String> s = new HashSet<String>();
        s.add(XmlSchemaWhiteSpaceFacet.class.getName());
        for (Iterator<XmlSchemaFacet> i = collection.iterator(); i.hasNext();) {
            Object o = i.next();
            assertTrue(s.remove(o.getClass().getName()));
            if (o instanceof XmlSchemaWhiteSpaceFacet) {
                assertEquals("collapse", ((XmlSchemaWhiteSpaceFacet)o).getValue());
                assertEquals(false, ((XmlSchemaWhiteSpaceFacet)o).isFixed());
            } else {
                fail("Unexpected object encountered: " + o.getClass().getName());
            }
        }

        assertTrue("The set should have been empty, but instead contained: " + s + ".", s.isEmpty());

    }

    /**
     * This will test the fractionDigits facet.
     * 
     * @throws Exception Any Exception encountered
     */
    @Test
    public void testFractionDigitsFacet() throws Exception {

        /*
         * <simpleType name="height"> <restriction base="decimal"> <totalDigits value="3"/> <fractionDigits
         * value="2"/> </restriction> </simpleType> <element name="myHeight" type="tns:height"/>
         */

        QName elementQName = new QName("http://soapinterop.org/types", "myHeight");
        InputStream is = new FileInputStream(Resources.asURI("facets.xsd"));
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        schemaCol.read(new StreamSource(is));

        XmlSchemaElement elem = schemaCol.getElementByQName(elementQName);
        assertNotNull(elem);
        assertEquals("myHeight", elem.getName());
        assertEquals(new QName("http://soapinterop.org/types", "myHeight"), elem.getQName());
        assertEquals(new QName("http://soapinterop.org/types", "height"), elem.getSchemaTypeName());

        XmlSchemaSimpleType simpleType = (XmlSchemaSimpleType)elem.getSchemaType();

        XmlSchemaSimpleTypeRestriction r = (XmlSchemaSimpleTypeRestriction)simpleType.getContent();
        assertEquals(new QName("http://www.w3.org/2001/XMLSchema", "decimal"), r.getBaseTypeName());

        XmlSchemaSimpleType xsst = r.getBaseType();
        assertNull(xsst);

        List<XmlSchemaFacet> collection = r.getFacets();
        assertEquals(2, collection.size());

        Set<String> s = new HashSet<String>();
        s.add(XmlSchemaFractionDigitsFacet.class.getName());
        s.add(XmlSchemaTotalDigitsFacet.class.getName());
        for (Iterator<XmlSchemaFacet> i = collection.iterator(); i.hasNext();) {
            Object o = i.next();
            assertTrue(s.remove(o.getClass().getName()));
            if (o instanceof XmlSchemaFractionDigitsFacet) {
                assertEquals("2", ((XmlSchemaFractionDigitsFacet)o).getValue());
                assertEquals(false, ((XmlSchemaFractionDigitsFacet)o).isFixed());
            } else if (o instanceof XmlSchemaTotalDigitsFacet) {
                assertEquals("3", ((XmlSchemaTotalDigitsFacet)o).getValue());
            } else {
                fail("Unexpected object encountered: " + o.getClass().getName());
            }
        }

        assertTrue("The set should have been empty, but instead contained: " + s + ".", s.isEmpty());

    }

    /**
     * This method will test the Min and Max Length facets.
     * 
     * @throws Exception Any Exception encountered
     */
    @Test
    public void testMinMaxLengthFacets() throws Exception {

        /*
         * <simpleType name="yardLength"> <restriction base="nonNegativeInteger"> <minLength value="45"/>
         * <maxLength value="205"/> </restriction> </simpleType> <element name="myYardLength"
         * type="tns:yardLength"/>
         */

        QName elementQName = new QName("http://soapinterop.org/types", "myYardLength");
        InputStream is = new FileInputStream(Resources.asURI("facets.xsd"));
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        schemaCol.read(new StreamSource(is));

        XmlSchemaElement elem = schemaCol.getElementByQName(elementQName);
        assertNotNull(elem);
        assertEquals("myYardLength", elem.getName());
        assertEquals(new QName("http://soapinterop.org/types", "myYardLength"), elem.getQName());
        assertEquals(new QName("http://soapinterop.org/types", "yardLength"), elem.getSchemaTypeName());

        XmlSchemaSimpleType simpleType = (XmlSchemaSimpleType)elem.getSchemaType();

        XmlSchemaSimpleTypeRestriction r = (XmlSchemaSimpleTypeRestriction)simpleType.getContent();
        assertEquals(new QName("http://www.w3.org/2001/XMLSchema", "nonNegativeInteger"), 
                     r.getBaseTypeName());

        XmlSchemaSimpleType xsst = r.getBaseType();
        assertNull(xsst);

        List<XmlSchemaFacet> collection = r.getFacets();
        assertEquals(2, collection.size());

        Set<String> s = new HashSet<String>();
        s.add(XmlSchemaMinLengthFacet.class.getName());
        s.add(XmlSchemaMaxLengthFacet.class.getName());
        for (Iterator<XmlSchemaFacet> i = collection.iterator(); i.hasNext();) {
            Object o = i.next();
            assertTrue(s.remove(o.getClass().getName()));
            if (o instanceof XmlSchemaMinLengthFacet) {
                assertEquals("45", ((XmlSchemaMinLengthFacet)o).getValue());
                assertEquals(false, ((XmlSchemaMinLengthFacet)o).isFixed());
            } else if (o instanceof XmlSchemaMaxLengthFacet) {
                assertEquals("205", ((XmlSchemaMaxLengthFacet)o).getValue());
                assertEquals(false, ((XmlSchemaMaxLengthFacet)o).isFixed());
            } else {
                fail("Unexpected object encountered: " + o.getClass().getName());
            }
        }

        assertTrue("The set should have been empty, but instead contained: " + s + ".", s.isEmpty());

    }

    /**
     * This method will test the enumeration facet.
     * 
     * @throws Exception Any Exception encountered
     */
    @Test
    public void testEnumerationFacet() throws Exception {

        /*
         * <simpleType name="layoutComponentType"> <restriction base="string"> <enumeration value="Field"/>
         * <enumeration value="Separator"/> </restriction> </simpleType> <element name="layoutComponent"
         * type="tns:layoutComponentType"/>
         */

        QName elementQName = new QName("http://soapinterop.org/types", "layoutComponent");
        InputStream is = new FileInputStream(Resources.asURI("facets.xsd"));
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        schemaCol.read(new StreamSource(is));

        XmlSchemaElement elem = schemaCol.getElementByQName(elementQName);
        assertNotNull(elem);
        assertEquals("layoutComponent", elem.getName());
        assertEquals(new QName("http://soapinterop.org/types", "layoutComponent"), elem.getQName());
        assertEquals(new QName("http://soapinterop.org/types", "layoutComponentType"), elem
            .getSchemaTypeName());

        XmlSchemaSimpleType simpleType = (XmlSchemaSimpleType)elem.getSchemaType();

        XmlSchemaSimpleTypeRestriction r = (XmlSchemaSimpleTypeRestriction)simpleType.getContent();
        assertEquals(new QName("http://www.w3.org/2001/XMLSchema", "string"), r.getBaseTypeName());

        XmlSchemaSimpleType xsst = r.getBaseType();
        assertNull(xsst);

        List<XmlSchemaFacet> collection = r.getFacets();
        assertEquals(2, collection.size());

        Set<String> s = new HashSet<String>();
        s.add("Field");
        s.add("Separator");
        for (Iterator<XmlSchemaFacet> i = collection.iterator(); i.hasNext();) {
            XmlSchemaEnumerationFacet xsef = (XmlSchemaEnumerationFacet)i.next();
            String value = (String)xsef.getValue();
            assertTrue("Atempted to remove an enumeration with the value of " + "\"" + value
                       + "\", but the value was not in the set.", s.remove(value));
        }
        assertTrue("The set should have been empty, but instead contained: " + s + ".", s.isEmpty());

    }

    /**
     * This method verifies the XML Schema's built-in
     * types have their facets defined correctly.
     */
    @Test
    public void testBuiltinFacets() {
    	XmlSchemaCollection collection = new XmlSchemaCollection();

    	// anySimpleType
    	List<XmlSchemaFacet> anySimpleTypeFacets = getFacetsForQName(collection, Constants.XSD_ANYSIMPLETYPE);
    	assertEquals(0, anySimpleTypeFacets.size());

    	// Any-Simple-Type-Based Facets
    	QName[] anySimpleTypeBasedQNames = {
    			Constants.XSD_DURATION,
    			Constants.XSD_DATETIME,
    			Constants.XSD_TIME,
    			Constants.XSD_DATE,
    			Constants.XSD_YEARMONTH,
    			Constants.XSD_YEAR,
    			Constants.XSD_MONTHDAY,
    			Constants.XSD_DAY,
    			Constants.XSD_MONTH,
    			Constants.XSD_BOOLEAN,
    			Constants.XSD_BASE64,
    			Constants.XSD_HEXBIN,
    			Constants.XSD_FLOAT,
    			Constants.XSD_DOUBLE,
    			Constants.XSD_ANYURI,
    			Constants.XSD_QNAME,
    			Constants.XSD_NOTATION,
    			Constants.XSD_DECIMAL
    		};

    	for (QName qName : anySimpleTypeBasedQNames) {
    		testWhiteSpaceFacet(
    				qName,
    				getFacetsForQName(collection, qName),
    				"collapse",
    				true);
    	}

    	// Numeric Type Facets

    	// integer
    	List<XmlSchemaFacet> integerFacets = getFacetsForQName(collection, Constants.XSD_INTEGER);
    	assertEquals(2, integerFacets.size());
    	XmlSchemaFractionDigitsFacet integerFractionDigitsFacet = null;
    	XmlSchemaPatternFacet integerPatternFacet = null;
    	for (XmlSchemaFacet facet : integerFacets) {
    		if (facet instanceof XmlSchemaPatternFacet) {
    			integerPatternFacet = (XmlSchemaPatternFacet) facet;
    		} else if (facet instanceof XmlSchemaFractionDigitsFacet) {
    			integerFractionDigitsFacet = (XmlSchemaFractionDigitsFacet) facet;
    		} else {
    			fail("Simple Type " + Constants.XSD_INTEGER + " should not have a facet of type " + facet.getClass().getName());
    		}
    	}
    	assertEquals(new Integer(0), integerFractionDigitsFacet.getValue());
    	assertTrue(integerFractionDigitsFacet.isFixed());
    	assertEquals("[\\-+]?[0-9]+", integerPatternFacet.getValue());
    	assertFalse(integerPatternFacet.isFixed());

    	// nonPositiveInteger
    	testMaxInclusiveFacet(
    			Constants.XSD_NONPOSITIVEINTEGER,
    			getFacetsForQName(collection, Constants.XSD_NONPOSITIVEINTEGER),
    			new Integer(0));

    	// negativeInteger
    	testMaxInclusiveFacet(
    			Constants.XSD_NEGATIVEINTEGER,
    			getFacetsForQName(collection, Constants.XSD_NEGATIVEINTEGER),
    			new Integer(-1));

    	// long
    	testNumericRange(
    			Constants.XSD_LONG,
    			getFacetsForQName(collection, Constants.XSD_LONG),
    			new Long(-9223372036854775808L),
    			new Long(9223372036854775807L));

    	// int
    	testNumericRange(
    			Constants.XSD_INT,
    			getFacetsForQName(collection, Constants.XSD_INT),
    			new Integer(-2147483648),
    			new Integer(2147483647));

    	// short
    	testNumericRange(
    			Constants.XSD_SHORT,
    			getFacetsForQName(collection, Constants.XSD_SHORT),
    			new Short((short) -32768),
    			new Short((short)  32767));

    	// byte
    	testNumericRange(
    			Constants.XSD_BYTE,
    			getFacetsForQName(collection, Constants.XSD_BYTE),
    			new Byte((byte) -128),
    			new Byte((byte)  127));

    	// nonNegativeInteger
    	testMinInclusiveFacet(
    			Constants.XSD_NONNEGATIVEINTEGER,
    			getFacetsForQName(collection, Constants.XSD_NONNEGATIVEINTEGER),
    			new Integer(0));

    	// positiveInteger
    	testMinInclusiveFacet(
    			Constants.XSD_POSITIVEINTEGER,
    			getFacetsForQName(collection, Constants.XSD_POSITIVEINTEGER),
    			new Integer(1));

    	// unsignedLong
    	testMaxInclusiveFacet(
    			Constants.XSD_UNSIGNEDLONG,
    			getFacetsForQName(collection, Constants.XSD_UNSIGNEDLONG),
    			new BigInteger("18446744073709551615"));

    	// unsignedInt
    	testMaxInclusiveFacet(
    			Constants.XSD_UNSIGNEDINT,
    			getFacetsForQName(collection, Constants.XSD_UNSIGNEDINT),
    			new Long(4294967295L));

    	// unsignedShort
    	testMaxInclusiveFacet(
    			Constants.XSD_UNSIGNEDSHORT,
    			getFacetsForQName(collection, Constants.XSD_UNSIGNEDSHORT),
    			new Integer(65535));

    	// unsignedByte
    	testMaxInclusiveFacet(
    			Constants.XSD_UNSIGNEDBYTE,
    			getFacetsForQName(collection, Constants.XSD_UNSIGNEDBYTE),
    			new Short((short) 255));

    	// String Type Facets

    	// string
    	testWhiteSpaceFacet(
    			Constants.XSD_STRING,
    			getFacetsForQName(collection, Constants.XSD_STRING),
    			"preserve",
    			false);

    	// normalizedString
    	testWhiteSpaceFacet(
    			Constants.XSD_NORMALIZEDSTRING,
    			getFacetsForQName(collection, Constants.XSD_NORMALIZEDSTRING),
    			"replace",
    			false);

    	// token
    	testWhiteSpaceFacet(
    			Constants.XSD_TOKEN,
    			getFacetsForQName(collection, Constants.XSD_TOKEN),
    			"collapse",
    			false);

    	// language
    	testPatternFacet(
    			Constants.XSD_LANGUAGE,
    			getFacetsForQName(collection, Constants.XSD_LANGUAGE),
    			"[a-zA-Z]{1,8}(-[a-zA-Z0-9]{1,8})*");

    	// NMTOKEN
    	testPatternFacet(
    			Constants.XSD_NMTOKEN,
    			getFacetsForQName(collection, Constants.XSD_NMTOKEN),
    			"\\c+");

    	// Name
    	testPatternFacet(
    			Constants.XSD_NAME,
    			getFacetsForQName(collection, Constants.XSD_NAME),
    			"\\i\\c*");

    	// NCName
    	testPatternFacet(
    			Constants.XSD_NCNAME,
    			getFacetsForQName(collection, Constants.XSD_NCNAME),
    			"[\\i-[:]][\\c-[:]]*");

    	// ID
    	assertTrue(
    			Constants.XSD_ID.toString(),
    			getFacetsForQName(collection, Constants.XSD_ID).isEmpty());

    	// IDREF
    	assertTrue(
    			Constants.XSD_IDREF.toString(),
    			getFacetsForQName(collection, Constants.XSD_IDREF).isEmpty());

    	// ENTITY
    	assertTrue(
    			Constants.XSD_ENTITY.toString(),
    			getFacetsForQName(collection, Constants.XSD_ENTITY).isEmpty());
    }

    private List<XmlSchemaFacet> getFacetsForQName(XmlSchemaCollection collection, QName qname) {
    	XmlSchemaSimpleType type = (XmlSchemaSimpleType) collection.getTypeByQName(qname);
    	return ((XmlSchemaSimpleTypeRestriction) type.getContent()).getFacets();
    }

    private void testWhiteSpaceFacet(QName qName, List<XmlSchemaFacet> facets, String value, boolean isFixed) {
    	assertEquals(qName.toString(), 1, facets.size());
    	assertTrue(qName.toString(), facets.get(0) instanceof XmlSchemaWhiteSpaceFacet);
    	assertEquals(qName.toString(), value, facets.get(0).getValue().toString());
    	assertEquals(qName.toString(), isFixed, facets.get(0).isFixed());
    }

    private void testMinInclusiveFacet(QName qName, List<XmlSchemaFacet> facets, Number minInclusiveValue) {
    	assertEquals(qName.toString(), 1, facets.size());
    	assertTrue(qName.toString(), facets.get(0) instanceof XmlSchemaMinInclusiveFacet);
    	assertEquals(qName.toString(), minInclusiveValue, facets.get(0).getValue());
    	assertFalse(qName.toString(), facets.get(0).isFixed());
    }

    private void testMaxInclusiveFacet(QName qName, List<XmlSchemaFacet> facets, Number maxInclusiveValue) {
    	assertEquals(qName.toString(), 1, facets.size());
    	assertTrue(qName.toString(), facets.get(0) instanceof XmlSchemaMaxInclusiveFacet);
    	assertEquals(qName.toString(), maxInclusiveValue, facets.get(0).getValue());
    	assertFalse(qName.toString(), facets.get(0).isFixed() );
    }

    private void testNumericRange(QName qName, List<XmlSchemaFacet> facets, Number min, Number max) {
    	XmlSchemaMinInclusiveFacet minFacet = null;
    	XmlSchemaMaxInclusiveFacet maxFacet = null;

    	assertEquals(qName.toString(), 2, facets.size());
    	for (XmlSchemaFacet facet : facets) {
    		if (facet instanceof XmlSchemaMinInclusiveFacet) {
    			minFacet = (XmlSchemaMinInclusiveFacet) facet;
    		} else if (facet instanceof XmlSchemaMaxInclusiveFacet) {
    			maxFacet = (XmlSchemaMaxInclusiveFacet) facet;
    		} else {
    			fail("Numeric Simple Type " + qName + " should not have a facet of type " + facet.getClass().getName());
    		}
    	}

    	assertEquals(qName.toString(), min, minFacet.getValue());
    	assertEquals(qName.toString(), max, maxFacet.getValue());

    	assertFalse(qName.toString(), minFacet.isFixed());
    	assertFalse(qName.toString(), maxFacet.isFixed());
    }

    private void testPatternFacet(QName qName, List<XmlSchemaFacet> facets, String pattern) {
    	assertEquals(qName.toString(), 1, facets.size());
    	assertTrue(qName.toString(), facets.get(0) instanceof XmlSchemaPatternFacet);
    	assertEquals(qName.toString(), pattern, facets.get(0).getValue());
    	assertFalse(qName.toString(), facets.get(0).isFixed());
    }
}
