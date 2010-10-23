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
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * TestElementForm
 */
public class TestElementForm extends Assert {
    static final String NS = "http://unqualified-elements.example.com";
    static final QName UNQUAL = new QName(NS, "unQualifiedLocals");
    private XmlSchemaCollection schema;

    @Before
    public void setUp() throws Exception {
        InputStream is = new FileInputStream(Resources.asURI("elementForm.xsd"));
        schema = new XmlSchemaCollection();
        schema.read(new StreamSource(is));
    }

    @Test
    public void testLocalElements() throws Exception {
        XmlSchemaElement element = schema.getElementByQName(UNQUAL);
        assertNotNull("Couldn't find unQualifiedLocals element", element);
        XmlSchemaComplexType type = (XmlSchemaComplexType)element.getSchemaType();
        XmlSchemaSequence seq = (XmlSchemaSequence)type.getParticle();
        List<XmlSchemaSequenceMember> items = seq.getItems();
        XmlSchemaElement subElement;
        subElement = (XmlSchemaElement)items.get(0);
        QName qname = subElement.getWireName();
        assertEquals("Namespace on unqualified element", "", qname.getNamespaceURI());
        subElement = (XmlSchemaElement)items.get(1);
        qname = subElement.getWireName();
        assertEquals("Bad namespace on qualified element", NS, qname.getNamespaceURI());
    }
}
