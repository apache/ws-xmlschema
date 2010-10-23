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

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA. User: ajith
 */
public class EnumValueTest extends Assert {
    @Test
    public void testValue() throws Exception {
        // create a DOM document
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        Document doc = documentBuilderFactory.newDocumentBuilder().parse(Resources.asURI("enum.xsd"));

        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        XmlSchema s = schemaCol.read(doc.getDocumentElement());

        assertNotNull(s);
    }
}
