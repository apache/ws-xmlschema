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

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaExternal;
import org.apache.ws.commons.schema.XmlSchemaImport;

import org.junit.Assert;
import org.junit.Test;

public class ImportTest extends Assert {
    @Test
    public void testSchemaImport() throws Exception {
        // create a DOM document
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        Document doc = documentBuilderFactory.newDocumentBuilder().parse(Resources.asURI("importBase.xsd"));

        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        schemaCol.setBaseUri(Resources.TEST_RESOURCES);
        XmlSchema schema = schemaCol.read(doc, null);
        assertNotNull(schema);

        // attempt with slash now
        schemaCol = new XmlSchemaCollection();
        schemaCol.setBaseUri(Resources.TEST_RESOURCES + "/");
        schema = schemaCol.read(doc, null);
        assertNotNull(schema);
    }

    /**
     * variation of above don't set the base uri.
     *
     * @throws Exception
     */
    @Test
    public void testSchemaImport2() throws Exception {
        File file = new File(Resources.asURI("importBase.xsd"));
        // create a DOM document
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        Document doc = documentBuilderFactory.newDocumentBuilder().parse(file.toURI().toURL().toString());

        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        XmlSchema schema = schemaCol.read(doc, file.toURI().toURL().toString());
        assertNotNull(schema);

    }

    /**
     * see whether we can reach the types of the imported schemas.
     *
     * @throws Exception
     */
    @Test
    public void testSchemaImport3() throws Exception {
        File file = new File(Resources.asURI("importBase.xsd"));
        // create a DOM document
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        Document doc = documentBuilderFactory.newDocumentBuilder().parse(file.toURI().toURL().toString());

        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        XmlSchema schema = schemaCol.read(doc, file.toURI().toURL().toString());
        assertNotNull(schema);

        assertNotNull(schema.getTypeByName(new QName("http://soapinterop.org/xsd2", "SOAPStruct")));
        assertNotNull(schema.getElementByName(new QName("http://soapinterop.org/xsd2", "SOAPWrapper")));
    } 
    
    /**
     * see whether we can parse the imported schemas using relative 
     * directories and paths.
     *
     * @throws Exception
     */
    @Test
    public void testSchemaImport4() throws Exception {
        //No base works
        File f = new File(Resources.asURI("XMLSCHEMA-8/a/b/c/s1.xsd"));
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        FileInputStream is = new FileInputStream(f);
        XmlSchema schema = schemaCol.read(new StreamSource(is, f.toURI().toString()));
        is.close();
        assertNotNull(schema);
        
        //XMLSCHEMA-8
        schemaCol = new XmlSchemaCollection();
        String base = f.getParentFile().toString();
        schemaCol.setBaseUri(base);
        is = new FileInputStream(f);
        schema = schemaCol.read(new StreamSource(is, f.toURI().toString()));
        is.close();
        assertNotNull(schema);
    }
    
    /**
     * Tests that imports without <tt>schemaLocation</tt> are resolved if the corresponding schemas
     * have been registered using {@link XmlSchemaCollection#getKnownNamespaceMap()}.
     */
    @Test
    public void testImportWithKnownNamespace() {
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        schemaCol.getKnownNamespaceMap().put("http://www.w3.org/XML/1998/namespace",
                new XmlSchemaCollection().read(new InputSource(Resources.asURI("xml.xsd"))));
        XmlSchema schema = schemaCol.read(new InputSource(Resources.asURI("knownNamespace.xsd")));
        List<XmlSchemaExternal> externals = schema.getExternals();
        assertEquals(1, externals.size());
        XmlSchemaImport schemaImport = (XmlSchemaImport)externals.get(0);
        assertEquals("http://www.w3.org/XML/1998/namespace", schemaImport.getNamespace());
        XmlSchema schema2 = schemaImport.getSchema();
        assertNotNull(schema2);
    }
    
    /**
     * Tests that imports are properly resolved when loading a schema from a JAR (as will generally
     * be the case when loading a schema from the classpath).
     * 
     * @throws Exception
     */
    @Test
    public void testImportWithJARURL() throws Exception {
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = ".";
        }
        URL jarUrl = new File(basedir, "target/test-zip.zip").toURL();
        URL schemaUrl = new URL("jar:" + jarUrl + "!/test-dir/importBase.xsd");
        XmlSchemaCollection schemaCol = new XmlSchemaCollection();
        XmlSchema schema = schemaCol.read(new InputSource(schemaUrl.toExternalForm()));
        assertNotNull(schema);
    }
}
