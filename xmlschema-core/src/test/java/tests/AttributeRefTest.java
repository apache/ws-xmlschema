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

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;

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
public class AttributeRefTest extends Assert {
    @Test
    public void testAttRefsWithNS() throws Exception {
        QName typeQName = new QName("http://tempuri.org/attribute", "TestAttributeReferenceType");

        InputStream is = new FileInputStream(Resources.asURI("attributref.xsd"));
        XmlSchemaCollection schema = new XmlSchemaCollection();
        XmlSchema s = schema.read(new StreamSource(is));

        XmlSchemaComplexType typeByName = (XmlSchemaComplexType)s.getTypeByName(typeQName);
        assertNotNull(typeByName);

        XmlSchemaAttribute item = (XmlSchemaAttribute)typeByName.getAttributes().get(0);
        QName qName = item.getRef().getTargetQName();
        assertNotNull(qName);

        String namspace = qName.getNamespaceURI();
        assertEquals("http://tempuri.org/attribute", namspace);

        for (XmlSchemaAttribute attribute : s.getAttributes().values()) {
            assertEquals("http://tempuri.org/attribute", attribute.getQName().getNamespaceURI());
        }

    }

}
