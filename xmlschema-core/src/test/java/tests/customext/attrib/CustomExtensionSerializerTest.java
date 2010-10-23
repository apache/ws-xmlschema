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
package tests.customext.attrib;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.constants.Constants;

import org.junit.Assert;
import org.junit.Test;

import tests.Resources;

/**
 * Test class to do a full parsing run with the extensions
 */
public class CustomExtensionSerializerTest extends Assert {

    @Test
    public void testSerialization() throws Exception {
        // set the system property for the custom extension registry
        System.setProperty(Constants.SystemConstants.EXTENSION_REGISTRY_KEY, CustomExtensionRegistry.class
            .getName());

        // create a DOM document
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);

        // Skip test in JDK1.4 as it uses crimson parser and an old DOM implementation
        if (documentBuilderFactory.getClass().toString().indexOf("crimson") != -1) {
            return;
        }
        Document doc1 = documentBuilderFactory.newDocumentBuilder()
            .parse(Resources.asURI("/external/externalAnnotations.xsd"));

        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        XmlSchema schema = schemaCol.read(doc1, null);
        assertNotNull(schema);

        // now serialize it to a byte stream
        // and build a new document out of it
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        schema.write(baos);

        Document doc2 = documentBuilderFactory.newDocumentBuilder().parse(
                                                                          new ByteArrayInputStream(baos
                                                                              .toByteArray()));

        schemaCol = new XmlSchemaCollection();
        schema = schemaCol.read(doc2, null);
        assertNotNull(schema);

        // get the elements and check whether their annotations are properly
        // populated
        for (XmlSchemaElement elt : schema.getElements().values()) {
            Map metaInfoMap = elt.getMetaInfoMap();
            assertNotNull(metaInfoMap);

            CustomAttribute customAttrib = (CustomAttribute)metaInfoMap
                .get(CustomAttribute.CUSTOM_ATTRIBUTE_QNAME);
            assertNotNull(customAttrib);

        }

        // remove our system property
        System.getProperties().remove(Constants.SystemConstants.EXTENSION_REGISTRY_KEY);

    }

}
