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
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.constants.Constants;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author alex $Revision$
 */
public class WSCOMMONS377Test extends Assert {
    @Test
    public void testSchemaImport() throws Exception {
        DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
        fac.setNamespaceAware(true);

        String strUri = Resources.asURI("WSCOMMONS-377/importing.wsdl");
        Document doc = fac.newDocumentBuilder().parse(strUri);

        XmlSchemaCollection xsColl = new XmlSchemaCollection();
        xsColl.setBaseUri(Resources.TEST_RESOURCES + "/WSCOMMONS-377");

        NodeList nodesSchema = doc.getElementsByTagNameNS(Constants.URI_2001_SCHEMA_XSD, "schema");
        XmlSchema[] schemas = new XmlSchema[nodesSchema.getLength()];

        String systemIdBase = "urn:schemas";
        for (int i = 0; i < nodesSchema.getLength(); i++) {
            schemas[i] = xsColl.read((Element)nodesSchema.item(i), systemIdBase + i);
        }
    }
}
