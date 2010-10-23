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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroup;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroupMember;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroupRef;
import org.apache.ws.commons.schema.XmlSchemaAttributeOrGroupRef;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;

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
public class AttributeGroupTest
    extends Assert {

    /**
     * This method will test the list.
     * 
     * @throws Exception Any exception encountered
     */
    @Test
    public void testAttributeGroup() throws Exception {

        /*
         * <schema xmlns="http://www.w3.org/2001/XMLSchema" xmlns:xsd="http://www.w3.org/2001/XMLSchema"
         * xmlns:tns="http://soapinterop.org/types" targetNamespace="http://soapinterop.org/types"
         * attributeFormDefault="qualified" > <attributeGroup name="department"> <attribute name="name"
         * type="string"/> <attribute name="id" type="integer"/> </attributeGroup> <element name="member">
         * <complexType> <attributeGroup ref="tns:department"/> </complexType> </element> </schema>
         */

        QName elementQName = new QName("http://soapinterop.org/types", "member");
        InputStream is = new FileInputStream(Resources.asURI("attributegroup.xsd"));
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        XmlSchema schema = schemaCol.read(new StreamSource(is));

        XmlSchemaElement elem = schemaCol.getElementByQName(elementQName);
        assertNotNull(elem);
        assertEquals("member", elem.getName());
        assertEquals(new QName("http://soapinterop.org/types", "member"), elem.getQName());

        XmlSchemaComplexType t = (XmlSchemaComplexType)elem.getSchemaType();
        assertNotNull(t);

        List<XmlSchemaAttributeOrGroupRef> c = t.getAttributes();
        for (Iterator<XmlSchemaAttributeOrGroupRef> i = c.iterator(); i.hasNext();) {
            XmlSchemaAttributeGroupRef agrn = (XmlSchemaAttributeGroupRef)i.next();
            assertEquals(new QName("http://soapinterop.org/types", "department"), agrn.getRef()
                .getTargetQName());
        }

        Map<QName, XmlSchemaAttributeGroup> attG = schema.getAttributeGroups();
        assertNotNull(attG);
        assertEquals(1, attG.size());

        for (QName name : attG.keySet()) {
            assertEquals("department", name.getLocalPart());
        }

        for (XmlSchemaAttributeGroup group : attG.values()) {
            assertEquals("department", group.getName());
            List<XmlSchemaAttributeGroupMember> attributes = group.getAttributes();
            assertNotNull(attributes);
            assertEquals(2, attributes.size());
            for (Iterator j = attributes.iterator(); j.hasNext();) {
                XmlSchemaAttribute obj2 = (XmlSchemaAttribute)j.next();
                String name = obj2.getName();
                if ("id".equals(name)) {
                    assertEquals(new QName("http://soapinterop.org/types", "id"), obj2.getQName());
                    assertEquals(new QName("http://www.w3.org/2001/XMLSchema", "integer"), obj2
                        .getSchemaTypeName());
                } else if ("name".equals(name)) {
                    assertEquals(new QName("http://soapinterop.org/types", "name"), obj2.getQName());
                    assertEquals(new QName("http://www.w3.org/2001/XMLSchema", "string"), obj2
                        .getSchemaTypeName());
                } else {
                    fail("The name \"" + name + "\" should not have been found " + "for an attribute.");

                }
            }
        }

    }
}
