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

package tests.w3c;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * 
 */
public final class W3CTestCaseCollector {
    private static final String[] TEST_SUITE_PATHS = 
    {"w3c/xmlschema2006-11-06/nistMeta/NISTXMLSchemaDatatypes.testSet"
    };
    
    private W3CTestCaseCollector() {
    }
    
    /**
     * Return all the tests to be used in automated testing.
     * @return a list of SchemaTest objects.
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws URISyntaxException 
     */
    public static List<SchemaCase> getSchemaTests() 
        throws ParserConfigurationException, SAXException, IOException, URISyntaxException {
        List<SchemaCase> results = new ArrayList<SchemaCase>();
        for (String path : TEST_SUITE_PATHS) {
            results.addAll(getSchemaTests(path));
        }
        return results;
    }
    
    /**
     * Return a list of tests as specified by some specific XML file.
     * @param testSet
     * @return
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws URISyntaxException 
     */
    public static List<SchemaCase> getSchemaTests(String testSet) throws ParserConfigurationException,
        SAXException, 
        IOException, URISyntaxException {
        URL setURL = W3CTestCaseCollector.class.getClassLoader().getResource(testSet);
        InputStream setXmlStream = setURL.openStream();
        File basePath = new File(setURL.toURI()).getParentFile();
        List<SchemaCase> schemaTests = new ArrayList<SchemaCase>();
        Document doc = getDocument(new InputSource(setXmlStream));
        NodeList testGroups = doc.getElementsByTagName("testGroup");
        for (int i = 0; i < testGroups.getLength(); i++) {
            Node testGroup = testGroups.item(i);
            NodeList testGroupChildren = testGroup.getChildNodes();
            Element schemaTestElem = null;
            for (int j = 0; j < testGroupChildren.getLength(); j++) {
                Node n = testGroupChildren.item(j);
                if (!(n instanceof Element)) {
                    continue;
                }
                schemaTestElem = (Element)n;
                if (schemaTestElem.getNodeName().equals("schemaTest")) {
                    break;
                }
            }
            if (schemaTestElem != null) {
                try {

                    SchemaCase schemaTest = new SchemaCase(schemaTestElem);
                    schemaTest.setBaseFilePathname(basePath);
                    if (schemaTest.getSchemaDocumentLink() != null) {
                        schemaTests.add(schemaTest);
                    }
                } catch (Exception e) {
                    // ignore errors?
                }
            }
        }

        return schemaTests;
    }

    /**
     * Returns a DOM Document of the file passed in as the inputsource parameter
     * 
     * @param inputSource input to read in as DOM Document
     * @return DOM Document of the input source
     * @throws Exception can be IOException or SAXException
     */
    private static Document getDocument(InputSource inputSource) throws ParserConfigurationException,
        SAXException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);

        return dbf.newDocumentBuilder().parse(inputSource);
    }
}
