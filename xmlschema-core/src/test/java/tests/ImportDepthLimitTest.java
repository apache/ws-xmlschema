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
import java.io.FileWriter;
import java.io.Writer;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaException;

import org.junit.Assert;
import org.junit.Test;

import org.xml.sax.InputSource;

/**
 * The total work of a single read must be bounded: an include chain
 * deeper than the configured maximum depth is rejected with the documented
 * XmlSchemaException rather than recursing until the thread stack overflows.
 */
public class ImportDepthLimitTest extends Assert {

    private File createIncludeChain(File dir, int depth) throws Exception {
        for (int i = 0; i < depth; i++) {
            File f = new File(dir, "chain" + i + ".xsd");
            Writer out = new FileWriter(f);
            try {
                out.write("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"");
                out.write(" targetNamespace=\"urn:deep\">");
                if (i + 1 < depth) {
                    out.write("<xs:include schemaLocation=\"chain" + (i + 1) + ".xsd\"/>");
                }
                out.write("</xs:schema>");
            } finally {
                out.close();
            }
        }
        return new File(dir, "chain0.xsd");
    }

    private File newTempDir() {
        File dir = new File(System.getProperty("java.io.tmpdir"),
                            "xmlschema-depth-" + System.nanoTime());
        assertTrue(dir.mkdir());
        return dir;
    }

    @Test
    public void testShallowIncludeChainStillParses() throws Exception {
        File dir = newTempDir();
        File root = createIncludeChain(dir, 5);

        XmlSchemaCollection collection = new XmlSchemaCollection();
        XmlSchema schema = collection.read(new InputSource(root.toURI().toString()));
        assertNotNull(schema);
    }

    @Test
    public void testIncludeChainDeeperThanLimitIsRejected() throws Exception {
        File dir = newTempDir();
        File root = createIncludeChain(dir, 5000);

        XmlSchemaCollection collection = new XmlSchemaCollection();
        try {
            collection.read(new InputSource(root.toURI().toString()));
            fail("An include chain deeper than the maximum depth should be rejected.");
        } catch (XmlSchemaException expected) {
            assertTrue(expected.getMessage().contains("depth"));
        }
    }
}