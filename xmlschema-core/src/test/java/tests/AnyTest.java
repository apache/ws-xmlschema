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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.InputSource;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAny;
import org.apache.ws.commons.schema.XmlSchemaAnyAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaContentProcessing;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;
import org.apache.ws.commons.schema.XmlSchemaType;

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
 */
public class AnyTest extends Assert {

    /**
     * This method will test the any.
     * 
     * @throws Exception Any exception encountered
     */
    @Test
    public void testAny() throws Exception {

        /*
         * <schema xmlns="http://www.w3.org/2001/XMLSchema" xmlns:xsd="http://www.w3.org/2001/XMLSchema"
         * xmlns:tns="http://soapinterop.org/types" targetNamespace="http://soapinterop.org/types"
         * elementFormDefault="qualified"> <element name="department"> <complexType> <sequence> <element
         * name="id" type="xsd:integer"/> <element name="name" type="xsd:string"/> <any minOccurs="5"
         * maxOccurs="10"/> </sequence> </complexType> </element> </schema>
         */

        QName elementQName = new QName("http://soapinterop.org/types", "department");
        InputStream is = new FileInputStream(Resources.asURI("any.xsd"));
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        schemaCol.read(new StreamSource(is));

        verifyAccuracy(elementQName, schemaCol, 5L, 10L);

    }

    @Test
    public void testAnyAttribute() throws Exception {
        InputStream is = new FileInputStream(Resources.asURI("anyAttribute.xsd"));
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        schemaCol.read(new StreamSource(is));

        XmlSchema[] schemas = schemaCol.getXmlSchemas();
        XmlSchema schema = null;
        for (XmlSchema schema2 : schemas) {
            if ("http://soapinterop.org/types".equals(schema2.getTargetNamespace())) {
                schema = schema2;
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        schema.write(baos);
        baos.close();
        byte[] bytes = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        schemaCol = new XmlSchemaCollection();
        schemaCol.read(new InputSource(bais));
        XmlSchemaType type = schemaCol.getTypeByQName(new QName("http://soapinterop.org/types",
                                                                "OccuringStructWithAnyAttribute"));
        XmlSchemaComplexType complexType = (XmlSchemaComplexType)type;
        XmlSchemaAnyAttribute aa = complexType.getAnyAttribute();
        assertNotNull(aa);
    }

    @Test
    public void testAnyZeroOccurs() throws Exception {

        /*
         * <schema xmlns="http://www.w3.org/2001/XMLSchema" xmlns:xsd="http://www.w3.org/2001/XMLSchema"
         * xmlns:tns="http://soapinterop.org/types" targetNamespace="http://soapinterop.org/types"
         * elementFormDefault="qualified"> <element name="department"> <complexType> <sequence> <element
         * name="id" type="xsd:integer"/> <element name="name" type="xsd:string"/> <any minOccurs="5"
         * maxOccurs="10"/> </sequence> </complexType> </element> </schema>
         */

        QName elementQName = new QName("http://soapinterop.org/types", "department");
        InputStream is = new FileInputStream(Resources.asURI("anyZero.xsd"));
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        XmlSchema schema = schemaCol.read(new StreamSource(is));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        schema.write(baos);

        XmlSchemaCollection schemaCol2 = new XmlSchemaCollection();
        schemaCol2.read(new StreamSource(new ByteArrayInputStream(baos.toByteArray())));

        verifyAccuracy(elementQName, schemaCol2, 0, 0);

    }

    private void verifyAccuracy(QName elementQName, XmlSchemaCollection schemaCol, long minCount,
                                long maxCount) {
        XmlSchemaElement elem = schemaCol.getElementByQName(elementQName);
        assertNotNull(elem);
        assertEquals("department", elem.getName());
        assertEquals(new QName("http://soapinterop.org/types", "department"), elem.getQName());

        XmlSchemaComplexType type = (XmlSchemaComplexType)elem.getSchemaType();
        assertNotNull(type);

        XmlSchemaSequence xss = (XmlSchemaSequence)type.getParticle();
        assertNotNull(xss);

        List<XmlSchemaSequenceMember> c = xss.getItems();
        assertEquals(3, c.size());

        Set<String> s = new HashSet<String>();
        s.add("id");
        s.add("name");
        Object o = null;
        for (int i = 0; i < c.size(); i++) {
            o = c.get(i);
            if (o instanceof XmlSchemaElement) {
                String name = ((XmlSchemaElement)o).getName();
                if ("id".equals(name)) {
                    assertEquals(new QName("http://www.w3.org/2001/XMLSchema", "integer"),
                                 ((XmlSchemaElement)o).getSchemaTypeName());
                } else if ("name".equals(name)) {
                    assertEquals(new QName("http://www.w3.org/2001/XMLSchema", "string"),
                                 ((XmlSchemaElement)o).getSchemaTypeName());
                }
                s.remove(name);
            } else if (o instanceof XmlSchemaAny) {
                XmlSchemaContentProcessing xscp = ((XmlSchemaAny)o).getProcessContent();
                assertEquals("none", xscp.toString());
                assertEquals(minCount, ((XmlSchemaAny)o).getMinOccurs());
                assertEquals(maxCount, ((XmlSchemaAny)o).getMaxOccurs());
            }
        }

        assertTrue("The set should have been empty, but instead contained: " + s + ".", s.isEmpty());
    }

}
