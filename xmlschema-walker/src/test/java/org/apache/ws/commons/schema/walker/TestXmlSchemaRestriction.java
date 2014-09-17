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

package org.apache.ws.commons.schema.walker;

import static org.junit.Assert.*;

import java.util.ArrayList;

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
import org.apache.ws.commons.schema.XmlSchemaTotalDigitsFacet;
import org.apache.ws.commons.schema.XmlSchemaWhiteSpaceFacet;
import org.apache.ws.commons.schema.walker.XmlSchemaRestriction;
import org.apache.ws.commons.schema.walker.XmlSchemaScope;
import org.junit.Test;

public class TestXmlSchemaRestriction {

    @Test
    public void testWhitespaceCollapseFixed() {
        XmlSchemaWhiteSpaceFacet collapseFixed = new XmlSchemaWhiteSpaceFacet("collapse", true);

        assertEquality(collapseFixed, XmlSchemaRestriction.Type.WHITESPACE,
                       new XmlSchemaRestriction(collapseFixed));
    }

    @Test
    public void tesetWhitespaceCollapseNotFixed() {
        XmlSchemaWhiteSpaceFacet collapseNotFixed = new XmlSchemaWhiteSpaceFacet("collapse", false);

        assertEquality(collapseNotFixed, XmlSchemaRestriction.Type.WHITESPACE,
                       new XmlSchemaRestriction(collapseNotFixed));

    }

    @Test
    public void testWhitespacePreserveFixed() {
        XmlSchemaWhiteSpaceFacet preserveFixed = new XmlSchemaWhiteSpaceFacet("preserve", true);

        assertEquality(preserveFixed, XmlSchemaRestriction.Type.WHITESPACE,
                       new XmlSchemaRestriction(preserveFixed));
    }

    @Test
    public void testWhitespacePreserveNotFixed() {
        XmlSchemaWhiteSpaceFacet preserveNotFixed = new XmlSchemaWhiteSpaceFacet("preserve", false);

        assertEquality(preserveNotFixed, XmlSchemaRestriction.Type.WHITESPACE,
                       new XmlSchemaRestriction(preserveNotFixed));
    }

    @Test
    public void testWhitespaceReplaceFixed() {

        XmlSchemaWhiteSpaceFacet replaceFixed = new XmlSchemaWhiteSpaceFacet("replace", true);

        assertEquality(replaceFixed, XmlSchemaRestriction.Type.WHITESPACE,
                       new XmlSchemaRestriction(replaceFixed));
    }

    @Test
    public void testWhitespaceReplaceNotFixed() {
        XmlSchemaWhiteSpaceFacet replaceNotFixed = new XmlSchemaWhiteSpaceFacet("replace", false);

        assertEquality(replaceNotFixed, XmlSchemaRestriction.Type.WHITESPACE,
                       new XmlSchemaRestriction(replaceNotFixed));
    }

    @Test
    public void testEnumerationFacetFixed() {
        XmlSchemaEnumerationFacet facet = new XmlSchemaEnumerationFacet("123", true);

        assertEquality(facet, XmlSchemaRestriction.Type.ENUMERATION, new XmlSchemaRestriction(facet));
    }

    @Test
    public void testEnumerationFacetNotFixed() {
        XmlSchemaEnumerationFacet facet = new XmlSchemaEnumerationFacet("avro", false);

        assertEquality(facet, XmlSchemaRestriction.Type.ENUMERATION, new XmlSchemaRestriction(facet));
    }

    @Test
    public void testExclusiveMaxFixed() {
        XmlSchemaMaxExclusiveFacet facet = new XmlSchemaMaxExclusiveFacet(new Integer(1234), true);

        assertEquality(facet, XmlSchemaRestriction.Type.EXCLUSIVE_MAX, new XmlSchemaRestriction(facet));
    }

    @Test
    public void testExclusiveMaxNotFixed() {
        XmlSchemaMaxExclusiveFacet facet = new XmlSchemaMaxExclusiveFacet(new Integer(1234), false);

        assertEquality(facet, XmlSchemaRestriction.Type.EXCLUSIVE_MAX, new XmlSchemaRestriction(facet));
    }

    @Test
    public void testExclusiveMinFixed() {
        XmlSchemaMinExclusiveFacet facet = new XmlSchemaMinExclusiveFacet(new Integer(1234), true);

        assertEquality(facet, XmlSchemaRestriction.Type.EXCLUSIVE_MIN, new XmlSchemaRestriction(facet));
    }

    @Test
    public void testExclusiveMinNotFixed() {
        XmlSchemaMinExclusiveFacet facet = new XmlSchemaMinExclusiveFacet(new Integer(1234), false);

        assertEquality(facet, XmlSchemaRestriction.Type.EXCLUSIVE_MIN, new XmlSchemaRestriction(facet));
    }

    @Test
    public void testInclusiveMinFixed() {
        XmlSchemaMinInclusiveFacet facet = new XmlSchemaMinInclusiveFacet(new Integer(1234), true);

        assertEquality(facet, XmlSchemaRestriction.Type.INCLUSIVE_MIN, new XmlSchemaRestriction(facet));
    }

    @Test
    public void testInclusiveMinNotFixed() {
        XmlSchemaMinInclusiveFacet facet = new XmlSchemaMinInclusiveFacet(new Integer(1234), false);

        assertEquality(facet, XmlSchemaRestriction.Type.INCLUSIVE_MIN, new XmlSchemaRestriction(facet));
    }

    @Test
    public void testInclusiveMaxFixed() {
        XmlSchemaMaxInclusiveFacet facet = new XmlSchemaMaxInclusiveFacet(new Integer(1234), true);

        assertEquality(facet, XmlSchemaRestriction.Type.INCLUSIVE_MAX, new XmlSchemaRestriction(facet));
    }

    @Test
    public void testInclusiveMaxNotFixed() {
        XmlSchemaMaxInclusiveFacet facet = new XmlSchemaMaxInclusiveFacet(new Integer(1234), false);

        assertEquality(facet, XmlSchemaRestriction.Type.INCLUSIVE_MAX, new XmlSchemaRestriction(facet));
    }

    @Test
    public void testDigitsFractionFixed() {
        XmlSchemaFractionDigitsFacet facet = new XmlSchemaFractionDigitsFacet(new Integer(0), true);

        assertEquality(facet, XmlSchemaRestriction.Type.DIGITS_FRACTION, new XmlSchemaRestriction(facet));
    }

    @Test
    public void testDigitsFractionNotFixed() {
        XmlSchemaFractionDigitsFacet facet = new XmlSchemaFractionDigitsFacet(new Integer(0), false);

        assertEquality(facet, XmlSchemaRestriction.Type.DIGITS_FRACTION, new XmlSchemaRestriction(facet));
    }

    @Test
    public void testDigitsTotalFixed() {
        XmlSchemaTotalDigitsFacet facet = new XmlSchemaTotalDigitsFacet(new Integer(0), true);

        assertEquality(facet, XmlSchemaRestriction.Type.DIGITS_TOTAL, new XmlSchemaRestriction(facet));
    }

    @Test
    public void testDigitsTotalNotFixed() {
        XmlSchemaTotalDigitsFacet facet = new XmlSchemaTotalDigitsFacet(new Integer(0), false);

        assertEquality(facet, XmlSchemaRestriction.Type.DIGITS_TOTAL, new XmlSchemaRestriction(facet));
    }

    @Test
    public void testPatternFacetFixed() {
        XmlSchemaPatternFacet facet = new XmlSchemaPatternFacet("[a-zA-Z]{1,8}(-[a-zA-Z0-9]{1,8})*", true);

        assertEquality(facet, XmlSchemaRestriction.Type.PATTERN, new XmlSchemaRestriction(facet));
    }

    @Test
    public void testPatternFacetNotFixed() {
        XmlSchemaPatternFacet facet = new XmlSchemaPatternFacet("[\\i-[:]][\\c-[:]]*", false);

        assertEquality(facet, XmlSchemaRestriction.Type.PATTERN, new XmlSchemaRestriction(facet));
    }

    @Test
    public void testLengthFacetFixed() {
        XmlSchemaLengthFacet facet = new XmlSchemaLengthFacet(new Integer(1), true);

        assertEquality(facet, XmlSchemaRestriction.Type.LENGTH, new XmlSchemaRestriction(facet));
    }

    @Test
    public void testLengthFacetNotFixed() {
        XmlSchemaLengthFacet facet = new XmlSchemaLengthFacet(new Integer(1000), false);

        assertEquality(facet, XmlSchemaRestriction.Type.LENGTH, new XmlSchemaRestriction(facet));
    }

    @Test
    public void testLengthMinFacetFixed() {
        XmlSchemaMinLengthFacet facet = new XmlSchemaMinLengthFacet(new Integer(1), true);

        assertEquality(facet, XmlSchemaRestriction.Type.LENGTH_MIN, new XmlSchemaRestriction(facet));
    }

    @Test
    public void testLengthMinFacetNotFixed() {
        XmlSchemaMinLengthFacet facet = new XmlSchemaMinLengthFacet(new Integer(10), false);

        assertEquality(facet, XmlSchemaRestriction.Type.LENGTH_MIN, new XmlSchemaRestriction(facet));
    }

    @Test
    public void testLengthMaxFacetFixed() {
        XmlSchemaMaxLengthFacet facet = new XmlSchemaMaxLengthFacet(new Integer(256), true);

        assertEquality(facet, XmlSchemaRestriction.Type.LENGTH_MAX, new XmlSchemaRestriction(facet));
    }

    @Test
    public void testLengthMaxFacetNotFixed() {
        XmlSchemaMaxLengthFacet facet = new XmlSchemaMaxLengthFacet(new Integer(128), false);

        assertEquality(facet, XmlSchemaRestriction.Type.LENGTH_MAX, new XmlSchemaRestriction(facet));
    }

    @Test
    public void testTypeConstructor() {
        XmlSchemaRestriction rstr = new XmlSchemaRestriction(XmlSchemaRestriction.Type.DIGITS_TOTAL);

        assertEquals(XmlSchemaRestriction.Type.DIGITS_TOTAL, rstr.getType());
        assertNull(rstr.getValue());
        assertFalse(rstr.isFixed());
    }

    @Test
    public void testFullConstructor() {
        XmlSchemaRestriction rstr = new XmlSchemaRestriction(XmlSchemaRestriction.Type.PATTERN,
                                                             "[\\-+]?[0-9]+", true);

        assertEquals(XmlSchemaRestriction.Type.PATTERN, rstr.getType());
        assertEquals("[\\-+]?[0-9]+", rstr.getValue());
        assertTrue(rstr.isFixed());
    }

    @Test
    public void testSetters() {
        XmlSchemaRestriction rstr = new XmlSchemaRestriction(XmlSchemaRestriction.Type.WHITESPACE,
                                                             "collapse", true);

        assertEquals(XmlSchemaRestriction.Type.WHITESPACE, rstr.getType());
        assertEquals("collapse", rstr.getValue());
        assertTrue(rstr.isFixed());

        rstr.setValue("replace");

        assertEquals(XmlSchemaRestriction.Type.WHITESPACE, rstr.getType());
        assertEquals("replace", rstr.getValue());
        assertTrue(rstr.isFixed());

        rstr.setFixed(false);

        assertEquals(XmlSchemaRestriction.Type.WHITESPACE, rstr.getType());
        assertEquals("replace", rstr.getValue());
        assertFalse(rstr.isFixed());
    }

    /**
     * Enumerations are the only restriction where the value and fixedness
     * factor into the equality check and hash code generation. When building
     * {@link XmlSchemaScope}s, only {@link XmlSchemaEnumerationFacet}s are
     * allowed to have multiple values.
     */
    @Test
    public void testEnumerationEqualsAndHashCode() {
        XmlSchemaRestriction enum1 = new XmlSchemaRestriction(XmlSchemaRestriction.Type.ENUMERATION);

        XmlSchemaRestriction enum2 = new XmlSchemaRestriction(XmlSchemaRestriction.Type.ENUMERATION, "avro",
                                                              false);

        assertFalse(enum1.equals(enum2));
        assertFalse(enum1.hashCode() == enum2.hashCode());

        enum1.setValue("avro");
        assertTrue(enum1.equals(enum2));
        assertTrue(enum1.hashCode() == enum2.hashCode());

        enum2.setFixed(true);

        assertFalse(enum1.equals(enum2));
        assertFalse(enum1.hashCode() == enum2.hashCode());

        enum1.setFixed(true);

        assertTrue(enum1.equals(enum2));
        assertTrue(enum1.hashCode() == enum2.hashCode());
    }

    public void testPatternEqualsAndHashCode() {
        XmlSchemaRestriction pattern1 = new XmlSchemaRestriction(XmlSchemaRestriction.Type.PATTERN);

        XmlSchemaRestriction pattern2 = new XmlSchemaRestriction(XmlSchemaRestriction.Type.PATTERN,
                                                                 "\\i\\c*", false);

        assertFalse(pattern1.equals(pattern2));
        assertFalse(pattern1.hashCode() == pattern2.hashCode());

        pattern1.setValue("\\i\\c*");
        assertTrue(pattern1.equals(pattern2));
        assertTrue(pattern1.hashCode() == pattern2.hashCode());

        pattern2.setFixed(true);

        assertFalse(pattern1.equals(pattern2));
        assertFalse(pattern1.hashCode() == pattern2.hashCode());

        pattern1.setFixed(true);

        assertTrue(pattern1.equals(pattern2));
        assertTrue(pattern1.hashCode() == pattern2.hashCode());
    }

    @Test
    public void testAllOtherEqualsAndHashCode() {
        ArrayList<XmlSchemaRestriction.Type> allOtherTypes = new ArrayList<XmlSchemaRestriction.Type>(11);

        allOtherTypes.add(XmlSchemaRestriction.Type.EXCLUSIVE_MAX);
        allOtherTypes.add(XmlSchemaRestriction.Type.EXCLUSIVE_MIN);
        allOtherTypes.add(XmlSchemaRestriction.Type.INCLUSIVE_MAX);
        allOtherTypes.add(XmlSchemaRestriction.Type.INCLUSIVE_MIN);
        allOtherTypes.add(XmlSchemaRestriction.Type.DIGITS_FRACTION);
        allOtherTypes.add(XmlSchemaRestriction.Type.DIGITS_TOTAL);
        allOtherTypes.add(XmlSchemaRestriction.Type.WHITESPACE);
        allOtherTypes.add(XmlSchemaRestriction.Type.LENGTH);
        allOtherTypes.add(XmlSchemaRestriction.Type.LENGTH_MIN);
        allOtherTypes.add(XmlSchemaRestriction.Type.LENGTH_MAX);

        for (XmlSchemaRestriction.Type type : allOtherTypes) {
            XmlSchemaRestriction rstr1 = new XmlSchemaRestriction(type);

            XmlSchemaRestriction rstr2 = new XmlSchemaRestriction(type, "1234", false);

            assertTrue(rstr1.equals(rstr2));
            assertTrue(rstr1.hashCode() == rstr2.hashCode());

            rstr1.setValue(new Integer(0));

            assertTrue(rstr1.equals(rstr2));
            assertTrue(rstr1.hashCode() == rstr2.hashCode());
        }
    }

    private static void assertEquality(XmlSchemaFacet expFacet, XmlSchemaRestriction.Type expType,
                                       XmlSchemaRestriction actual) {

        assertEquals("Expected Type: " + expType + "; Actual Type: " + actual.getType(), expType,
                     actual.getType());

        assertEquals(expType.toString(), expFacet.getValue(), actual.getValue());
        assertEquals(expType.toString(), expFacet.isFixed(), actual.isFixed());
    }
}
