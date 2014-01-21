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

package tests.xmlschema30;

import com.google.common.testing.EqualsTester;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaGroup;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.constants.Constants;

import org.junit.BeforeClass;
import org.junit.Test;

import tests.Resources;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * This test checks implementations of <strong>equals</strong> and <strong>hashCode</strong> methods on the following
 * classes:
 * <ul>
 *     <li>{@link org.apache.ws.commons.schema.utils.XmlSchemaNamedImpl XmlSchemaNamedImpl}</li>
 *     <li>{@link org.apache.ws.commons.schema.utils.XmlSchemaNamedWithFormImpl XmlSchemaNamedWithFormImpl}</li>
 *     <li>{@link org.apache.ws.commons.schema.XmlSchemaType XmlSchemaType}</li>
 *     <li>{@link org.apache.ws.commons.schema.XmlSchemaGroup XmlSchemaGroup}</li>
 *     <li>{@link org.apache.ws.commons.schema.XmlSchemaElement XmlSchemaElement}</li>
 * </ul>
 */
public class NamedItemsEqualityTest {

    static private final String CUSTOM_SCHEMA_NS = "http://example.com/test";
    static private XmlSchema BASE_SCHEMA;
    static private XmlSchema CUSTOM_SCHEMA;

    @BeforeClass
    static public void setUp() throws FileNotFoundException {
        InputStream is = new FileInputStream(Resources.asURI("XMLSCHEMA-30/test.xsd"));
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        schemaCol.read(new StreamSource(is));
        BASE_SCHEMA = schemaCol.schemaForNamespace(Constants.URI_2001_SCHEMA_XSD);
        CUSTOM_SCHEMA = schemaCol.schemaForNamespace(CUSTOM_SCHEMA_NS);
    }

    @Test
    public void testXmlSchemaTypes() {
        XmlSchemaType stringSimpleType = BASE_SCHEMA.getTypeByName( Constants.XSD_STRING );
        XmlSchemaType decimalSimpleType = BASE_SCHEMA.getTypeByName( Constants.XSD_DECIMAL );

        XmlSchemaType customSimpleType = CUSTOM_SCHEMA.getTypeByName("customSimpleType");
        XmlSchemaType customComplexType = CUSTOM_SCHEMA.getTypeByName("customComplexTypeType");

        new EqualsTester()
                .addEqualityGroup(
                    stringSimpleType,
                    stringSimpleType)
                .addEqualityGroup(
                    decimalSimpleType,
                    decimalSimpleType)
                .addEqualityGroup(
                    customSimpleType,
                    customSimpleType)
                .addEqualityGroup(
                    customComplexType,
                    customComplexType)
                .testEquals();

    }

    @Test
    public void testXmlSchemaElements() {
        XmlSchemaElement customSimpleElem = CUSTOM_SCHEMA.getElementByName("customTopSimpleElement");
        XmlSchemaElement customComplexElement = CUSTOM_SCHEMA.getElementByName("customTopComplexElement");

        new EqualsTester()
                .addEqualityGroup(
                    customSimpleElem,
                    customSimpleElem)
                .addEqualityGroup(
                    customComplexElement,
                    customComplexElement)
                .testEquals();
    }

    @Test
    public void testXmlSchemaGroup() {
        final QName grp1QName = new QName(CUSTOM_SCHEMA_NS, "customGroup1");
        final QName grp2QName = new QName(CUSTOM_SCHEMA_NS, "customGroup2");
        XmlSchemaGroup grp1 = CUSTOM_SCHEMA.getGroupByName(grp1QName);
        XmlSchemaGroup grp2 = CUSTOM_SCHEMA.getGroupByName(grp2QName);

        new EqualsTester()
                .addEqualityGroup(
                        grp1,
                        grp1)
                .addEqualityGroup(
                        grp2,
                        grp2)
                .testEquals();
    }

}
