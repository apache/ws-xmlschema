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

import java.util.HashMap;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.constants.Constants;

/**
 * Represents the set of simple types defined by XML Schema, and conversions
 * between them and their respective {@link QName}s. This set is limited to only
 * <code>anyType</code>, <code>anySimpleType</code>, and the <a
 * href="http://www.w3.org/TR/xmlschema-2/#built-in-primitive-datatypes">
 * primitive datatypes</a> defined by XML Schema.
 */
public enum XmlSchemaBaseSimpleType {
    ANYTYPE(Constants.XSD_ANYTYPE), ANYSIMPLETYPE(Constants.XSD_ANYSIMPLETYPE), DURATION(
        Constants.XSD_DURATION), DATETIME(Constants.XSD_DATETIME), TIME(Constants.XSD_TIME), DATE(
        Constants.XSD_DATE), YEARMONTH(Constants.XSD_YEARMONTH), YEAR(Constants.XSD_YEAR), MONTHDAY(
        Constants.XSD_MONTHDAY), DAY(Constants.XSD_DAY), MONTH(Constants.XSD_MONTH), STRING(
        Constants.XSD_STRING), BOOLEAN(Constants.XSD_BOOLEAN), BIN_BASE64(Constants.XSD_BASE64), BIN_HEX(
        Constants.XSD_HEXBIN), FLOAT(Constants.XSD_FLOAT), DECIMAL(Constants.XSD_DECIMAL), DOUBLE(
        Constants.XSD_DOUBLE), ANYURI(Constants.XSD_ANYURI), QNAME(Constants.XSD_QNAME), NOTATION(
        Constants.XSD_NOTATION);

    private QName qName;

    private XmlSchemaBaseSimpleType(QName qName) {
        this.qName = qName;
    }

    /**
     * The corresponding {@link QName} that the
     * <code>XmlSchemaBaseSimpleType</code> represents in XML Schema.
     */
    public QName getQName() {
        return qName;
    }

    private static HashMap<QName, XmlSchemaBaseSimpleType> reverseMap = new HashMap<QName, XmlSchemaBaseSimpleType>();

    static {
        final XmlSchemaBaseSimpleType[] types = XmlSchemaBaseSimpleType.values();
        for (XmlSchemaBaseSimpleType type : types) {
            reverseMap.put(type.getQName(), type);
        }
    }

    /**
     * Returns the XML Schema base simple type for the provided {@link QName}.
     * If the <code>QName</code> represents <code>anyType</code>,
     * <code>anySimpleType</code>, or one of the <a
     * href="http://www.w3.org/TR/xmlschema-2/#built-in-primitive-datatypes">
     * primitive datatypes</a> defined by XML Schema, returns the corresponding
     * <code>XmlSchemaBaseSimpleType</code>. Otherwise, returns
     * <code>null</code>.
     *
     * @param qName The {@link QName} of an XML Schema base simple type.
     * @return The corresponding {@link XmlSchemaBaseSimpleType}.
     */
    public static XmlSchemaBaseSimpleType getBaseSimpleTypeFor(QName qName) {
        return reverseMap.get(qName);
    }

    /**
     * Returns <code>true</code> if the provided {@link QName} references XML
     * Schema's <code>anyType</code>, <code>anySimpleType</code>, or one of the
     * <a href="http://www.w3.org/TR/xmlschema-2/#built-in-primitive-datatypes">
     * primitive datatypes</a> defined by XML Schema. Otherwise, returns
     * <code>false</code>.
     *
     * @param qName The {@link QName} of an XML Schema type to check.
     * @return Whether that type is a XML Schema base simple type.
     */
    public static boolean isBaseSimpleType(QName qName) {
        return reverseMap.containsKey(qName);
    }
}
