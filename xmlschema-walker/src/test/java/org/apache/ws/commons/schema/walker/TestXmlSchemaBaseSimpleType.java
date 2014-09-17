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
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.walker.XmlSchemaBaseSimpleType;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestXmlSchemaBaseSimpleType {

    private static List<QName> nonBaseTypes;

    @BeforeClass
    public static void setUpNonBaseTypes() {
        nonBaseTypes = new ArrayList<QName>(28);

        nonBaseTypes.add(Constants.XSD_ANY);
        nonBaseTypes.add(Constants.XSD_BYTE);
        nonBaseTypes.add(Constants.XSD_ENTITIES);
        nonBaseTypes.add(Constants.XSD_ENTITY);
        nonBaseTypes.add(Constants.XSD_ID);
        nonBaseTypes.add(Constants.XSD_IDREF);
        nonBaseTypes.add(Constants.XSD_IDREFS);
        nonBaseTypes.add(Constants.XSD_INT);
        nonBaseTypes.add(Constants.XSD_INTEGER);
        nonBaseTypes.add(Constants.XSD_LANGUAGE);
        nonBaseTypes.add(Constants.XSD_LONG);
        nonBaseTypes.add(Constants.XSD_NAME);
        nonBaseTypes.add(Constants.XSD_NCNAME);
        nonBaseTypes.add(Constants.XSD_NEGATIVEINTEGER);
        nonBaseTypes.add(Constants.XSD_NMTOKEN);
        nonBaseTypes.add(Constants.XSD_NMTOKENS);
        nonBaseTypes.add(Constants.XSD_NONNEGATIVEINTEGER);
        nonBaseTypes.add(Constants.XSD_NONPOSITIVEINTEGER);
        nonBaseTypes.add(Constants.XSD_NORMALIZEDSTRING);
        nonBaseTypes.add(Constants.XSD_POSITIVEINTEGER);
        nonBaseTypes.add(Constants.XSD_SCHEMA);
        nonBaseTypes.add(Constants.XSD_SHORT);
        nonBaseTypes.add(Constants.XSD_TOKEN);
        nonBaseTypes.add(Constants.XSD_UNSIGNEDBYTE);
        nonBaseTypes.add(Constants.XSD_UNSIGNEDINT);
        nonBaseTypes.add(Constants.XSD_UNSIGNEDLONG);
        nonBaseTypes.add(Constants.XSD_UNSIGNEDSHORT);
    }

    @Test
    public void testMappings() {
        assertEquals(Constants.XSD_ANYTYPE, XmlSchemaBaseSimpleType.ANYTYPE.getQName());

        assertEquals(Constants.XSD_ANYSIMPLETYPE, XmlSchemaBaseSimpleType.ANYSIMPLETYPE.getQName());

        assertEquals(Constants.XSD_DURATION, XmlSchemaBaseSimpleType.DURATION.getQName());

        assertEquals(Constants.XSD_DATETIME, XmlSchemaBaseSimpleType.DATETIME.getQName());

        assertEquals(Constants.XSD_TIME, XmlSchemaBaseSimpleType.TIME.getQName());

        assertEquals(Constants.XSD_DATE, XmlSchemaBaseSimpleType.DATE.getQName());

        assertEquals(Constants.XSD_YEARMONTH, XmlSchemaBaseSimpleType.YEARMONTH.getQName());

        assertEquals(Constants.XSD_YEAR, XmlSchemaBaseSimpleType.YEAR.getQName());

        assertEquals(Constants.XSD_MONTHDAY, XmlSchemaBaseSimpleType.MONTHDAY.getQName());

        assertEquals(Constants.XSD_DAY, XmlSchemaBaseSimpleType.DAY.getQName());

        assertEquals(Constants.XSD_MONTH, XmlSchemaBaseSimpleType.MONTH.getQName());

        assertEquals(Constants.XSD_STRING, XmlSchemaBaseSimpleType.STRING.getQName());

        assertEquals(Constants.XSD_BOOLEAN, XmlSchemaBaseSimpleType.BOOLEAN.getQName());

        assertEquals(Constants.XSD_BASE64, XmlSchemaBaseSimpleType.BIN_BASE64.getQName());

        assertEquals(Constants.XSD_HEXBIN, XmlSchemaBaseSimpleType.BIN_HEX.getQName());

        assertEquals(Constants.XSD_FLOAT, XmlSchemaBaseSimpleType.FLOAT.getQName());

        assertEquals(Constants.XSD_DECIMAL, XmlSchemaBaseSimpleType.DECIMAL.getQName());

        assertEquals(Constants.XSD_DOUBLE, XmlSchemaBaseSimpleType.DOUBLE.getQName());

        assertEquals(Constants.XSD_ANYURI, XmlSchemaBaseSimpleType.ANYURI.getQName());

        assertEquals(Constants.XSD_QNAME, XmlSchemaBaseSimpleType.QNAME.getQName());

        assertEquals(Constants.XSD_NOTATION, XmlSchemaBaseSimpleType.NOTATION.getQName());
    }

    @Test
    public void testReverseMappings() {
        assertEquals(XmlSchemaBaseSimpleType.ANYTYPE,
                     XmlSchemaBaseSimpleType.getBaseSimpleTypeFor(Constants.XSD_ANYTYPE));

        assertEquals(XmlSchemaBaseSimpleType.ANYSIMPLETYPE,
                     XmlSchemaBaseSimpleType.getBaseSimpleTypeFor(Constants.XSD_ANYSIMPLETYPE));

        assertEquals(XmlSchemaBaseSimpleType.DURATION,
                     XmlSchemaBaseSimpleType.getBaseSimpleTypeFor(Constants.XSD_DURATION));

        assertEquals(XmlSchemaBaseSimpleType.DATETIME,
                     XmlSchemaBaseSimpleType.getBaseSimpleTypeFor(Constants.XSD_DATETIME));

        assertEquals(XmlSchemaBaseSimpleType.TIME,
                     XmlSchemaBaseSimpleType.getBaseSimpleTypeFor(Constants.XSD_TIME));

        assertEquals(XmlSchemaBaseSimpleType.DATE,
                     XmlSchemaBaseSimpleType.getBaseSimpleTypeFor(Constants.XSD_DATE));

        assertEquals(XmlSchemaBaseSimpleType.YEARMONTH,
                     XmlSchemaBaseSimpleType.getBaseSimpleTypeFor(Constants.XSD_YEARMONTH));

        assertEquals(XmlSchemaBaseSimpleType.YEAR,
                     XmlSchemaBaseSimpleType.getBaseSimpleTypeFor(Constants.XSD_YEAR));

        assertEquals(XmlSchemaBaseSimpleType.MONTHDAY,
                     XmlSchemaBaseSimpleType.getBaseSimpleTypeFor(Constants.XSD_MONTHDAY));

        assertEquals(XmlSchemaBaseSimpleType.DAY,
                     XmlSchemaBaseSimpleType.getBaseSimpleTypeFor(Constants.XSD_DAY));

        assertEquals(XmlSchemaBaseSimpleType.MONTH,
                     XmlSchemaBaseSimpleType.getBaseSimpleTypeFor(Constants.XSD_MONTH));

        assertEquals(XmlSchemaBaseSimpleType.STRING,
                     XmlSchemaBaseSimpleType.getBaseSimpleTypeFor(Constants.XSD_STRING));

        assertEquals(XmlSchemaBaseSimpleType.BOOLEAN,
                     XmlSchemaBaseSimpleType.getBaseSimpleTypeFor(Constants.XSD_BOOLEAN));

        assertEquals(XmlSchemaBaseSimpleType.BIN_BASE64,
                     XmlSchemaBaseSimpleType.getBaseSimpleTypeFor(Constants.XSD_BASE64));

        assertEquals(XmlSchemaBaseSimpleType.BIN_HEX,
                     XmlSchemaBaseSimpleType.getBaseSimpleTypeFor(Constants.XSD_HEXBIN));

        assertEquals(XmlSchemaBaseSimpleType.FLOAT,
                     XmlSchemaBaseSimpleType.getBaseSimpleTypeFor(Constants.XSD_FLOAT));

        assertEquals(XmlSchemaBaseSimpleType.DECIMAL,
                     XmlSchemaBaseSimpleType.getBaseSimpleTypeFor(Constants.XSD_DECIMAL));

        assertEquals(XmlSchemaBaseSimpleType.DOUBLE,
                     XmlSchemaBaseSimpleType.getBaseSimpleTypeFor(Constants.XSD_DOUBLE));

        assertEquals(XmlSchemaBaseSimpleType.ANYURI,
                     XmlSchemaBaseSimpleType.getBaseSimpleTypeFor(Constants.XSD_ANYURI));

        assertEquals(XmlSchemaBaseSimpleType.QNAME,
                     XmlSchemaBaseSimpleType.getBaseSimpleTypeFor(Constants.XSD_QNAME));

        assertEquals(XmlSchemaBaseSimpleType.NOTATION,
                     XmlSchemaBaseSimpleType.getBaseSimpleTypeFor(Constants.XSD_NOTATION));

        for (QName nonBaseType : nonBaseTypes) {
            final XmlSchemaBaseSimpleType simpleType = XmlSchemaBaseSimpleType
                .getBaseSimpleTypeFor(nonBaseType);
            assertNull(nonBaseType + " -> " + simpleType, simpleType);
        }
    }

    @Test
    public void testIsBaseSimpleType() {
        assertTrue(XmlSchemaBaseSimpleType.isBaseSimpleType(Constants.XSD_ANYTYPE));

        assertTrue(XmlSchemaBaseSimpleType.isBaseSimpleType(Constants.XSD_ANYSIMPLETYPE));

        assertTrue(XmlSchemaBaseSimpleType.isBaseSimpleType(Constants.XSD_DURATION));

        assertTrue(XmlSchemaBaseSimpleType.isBaseSimpleType(Constants.XSD_DATETIME));

        assertTrue(XmlSchemaBaseSimpleType.isBaseSimpleType(Constants.XSD_TIME));

        assertTrue(XmlSchemaBaseSimpleType.isBaseSimpleType(Constants.XSD_DATE));

        assertTrue(XmlSchemaBaseSimpleType.isBaseSimpleType(Constants.XSD_YEARMONTH));

        assertTrue(XmlSchemaBaseSimpleType.isBaseSimpleType(Constants.XSD_YEAR));

        assertTrue(XmlSchemaBaseSimpleType.isBaseSimpleType(Constants.XSD_MONTHDAY));

        assertTrue(XmlSchemaBaseSimpleType.isBaseSimpleType(Constants.XSD_DAY));

        assertTrue(XmlSchemaBaseSimpleType.isBaseSimpleType(Constants.XSD_MONTH));

        assertTrue(XmlSchemaBaseSimpleType.isBaseSimpleType(Constants.XSD_STRING));

        assertTrue(XmlSchemaBaseSimpleType.isBaseSimpleType(Constants.XSD_BOOLEAN));

        assertTrue(XmlSchemaBaseSimpleType.isBaseSimpleType(Constants.XSD_BASE64));

        assertTrue(XmlSchemaBaseSimpleType.isBaseSimpleType(Constants.XSD_HEXBIN));

        assertTrue(XmlSchemaBaseSimpleType.isBaseSimpleType(Constants.XSD_FLOAT));

        assertTrue(XmlSchemaBaseSimpleType.isBaseSimpleType(Constants.XSD_DECIMAL));

        assertTrue(XmlSchemaBaseSimpleType.isBaseSimpleType(Constants.XSD_DOUBLE));

        assertTrue(XmlSchemaBaseSimpleType.isBaseSimpleType(Constants.XSD_ANYURI));

        assertTrue(XmlSchemaBaseSimpleType.isBaseSimpleType(Constants.XSD_QNAME));

        assertTrue(XmlSchemaBaseSimpleType.isBaseSimpleType(Constants.XSD_NOTATION));

        for (QName nonBaseType : nonBaseTypes) {
            assertFalse(nonBaseType.toString(), XmlSchemaBaseSimpleType.isBaseSimpleType(nonBaseType));
        }
    }
}
