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
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;

import org.junit.Assert;
import org.junit.Test;

public class ElementRefs2Test extends Assert {
    @Test
    public void testElementRefs() throws Exception {
        QName elementQName = new QName("http://soapinterop.org/types", "attTests");
        InputStream is = new FileInputStream(Resources.asURI("elementreferences2.xsd"));
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        XmlSchema schema = schemaCol.read(new StreamSource(is));

        XmlSchemaElement elem = schemaCol.getElementByQName(elementQName);

        assertNotNull(elem);

        XmlSchemaComplexType cmplxType = (XmlSchemaComplexType)elem.getSchemaType();
        List<XmlSchemaSequenceMember> items = ((XmlSchemaSequence)cmplxType.getParticle()).getItems();

        Iterator it = items.iterator();
        while (it.hasNext()) {
            XmlSchemaElement innerElement = (XmlSchemaElement)it.next();
            assertNotNull(innerElement.getRef().getTargetQName());
        }

        // test writing
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        schema.write(bos);

        // read this as a plain DOM and inspect our reference in question
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = documentBuilder.parse(new ByteArrayInputStream(bos.toByteArray()));

        // find the element with name="atttest" and test its type attribute
        // to see whether it includes a colon.
        NodeList elementList = document.getElementsByTagName("element");
        for (int i = 0; i < elementList.getLength(); i++) {
            Node n = elementList.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && ((Element)n).hasAttribute("type")) {
                assertTrue(((Element)n).getAttribute("type").indexOf(':') < 0);
            }
        }

    }

}
