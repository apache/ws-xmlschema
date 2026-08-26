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
import java.io.StringReader;
import java.io.Writer;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.xml.sax.InputSource;

/**
 * The structural descent of the schema build must be bounded: a deeply
 * nested schema document must fail with the documented XmlSchemaException
 * instead of exhausting the thread stack. The bound is shared across the
 * documents of one read: every nested include/import/redefine resolution is
 * built by a fresh SchemaBuilder but on the same thread stack, so the depth
 * carries across resolveXmlSchema hops instead of resetting per document,
 * and a chain of distinct single-hop documents is bounded too.
 */
public class NestingDepthLimitTest extends Assert {
    private static final String MAX_ELEMENT_DEPTH = "jdk.xml.maxElementDepth";

    private String savedMaxElementDepth;

    /**
     * Secure processing makes the JDK parser enforce its own element-depth
     * limit (100 by default as of JDK 25), which would reject these
     * documents before the schema build ever descends. Lift it so the bound
     * under test is the library's own.
     */
    @Before
    public void liftParserElementDepthLimit() {
        savedMaxElementDepth = System.getProperty(MAX_ELEMENT_DEPTH);
        System.setProperty(MAX_ELEMENT_DEPTH, "0");
    }

    @After
    public void restoreParserElementDepthLimit() {
        if (savedMaxElementDepth == null) {
            System.clearProperty(MAX_ELEMENT_DEPTH);
        } else {
            System.setProperty(MAX_ELEMENT_DEPTH, savedMaxElementDepth);
        }
    }

    @Test
    public void testReasonablyNestedSchemaStillParses() throws Exception {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        XmlSchema schema = collection.read(new StringReader(buildNestedSchema(50)));
        assertNotNull(schema);
    }

    @Test
    public void testDeeplyNestedSchemaIsRejected() throws Exception {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        try {
            collection.read(new StringReader(buildNestedSchema(600)));
            fail("A schema document nested 600 particle levels deep should be rejected.");
        } catch (XmlSchemaException expected) {
            assertTrue(expected.getMessage().contains("nested"));
        }
    }

    /**
     * The counter must not reset across include hops: a chain of distinct
     * documents, each individually trivial, must still hit the shared bound
     * instead of adding stack frames per hop until the thread stack
     * overflows. (Exact-key cycle detection never fires here because every
     * schemaLocation is distinct.)
     */
    @Test
    public void testIncludeChainOfDistinctDocumentsIsRejected() throws Exception {
        File root = createIncludeChain(newTempDir(), 600, 0);
        XmlSchemaCollection collection = new XmlSchemaCollection();
        try {
            collection.read(new InputSource(root.toURI().toString()));
            fail("An include chain of 600 distinct documents should be rejected, not recursed.");
        } catch (XmlSchemaException expected) {
            assertTrue(expected.getMessage().contains("depth"));
        }
    }

    @Test
    public void testShallowIncludeChainWithNestedLeafStillParses() throws Exception {
        File root = createIncludeChain(newTempDir(), 3, 50);
        XmlSchemaCollection collection = new XmlSchemaCollection();
        XmlSchema schema = collection.read(new InputSource(root.toURI().toString()));
        assertNotNull(schema);
    }

    @Test
    public void testCollectionIsReusableAfterRejection() throws Exception {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        try {
            collection.read(new StringReader(buildNestedSchema(600)));
            fail("A schema document nested 600 particle levels deep should be rejected.");
        } catch (XmlSchemaException expected) {
            // expected; the counter must have unwound to zero.
        }

        XmlSchema schema = collection.read(new StringReader(buildNestedSchema(50, "urn:after-rejection")));
        assertNotNull(schema);
    }

    private String buildNestedSchema(int depth) {
        return buildNestedSchema(depth, "urn:nesting");
    }

    private String buildNestedSchema(int depth, String namespace) {
        StringBuilder schema = new StringBuilder();
        schema.append("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"")
            .append(" targetNamespace=\"").append(namespace).append("\">");
        schema.append("<xs:element name=\"root\">");
        for (int i = 0; i < depth; i++) {
            schema.append("<xs:complexType><xs:sequence><xs:element name=\"e\">");
        }
        schema.append("<xs:complexType/>");
        for (int i = 0; i < depth; i++) {
            schema.append("</xs:element></xs:sequence></xs:complexType>");
        }
        schema.append("</xs:element></xs:schema>");
        return schema.toString();
    }

    private File newTempDir() {
        File dir = new File(System.getProperty("java.io.tmpdir"),
                            "xmlschema-nesting-" + System.nanoTime());
        assertTrue(dir.mkdir());
        return dir;
    }

    /**
     * Writes an include chain of the given length; every document is
     * structurally trivial, and the last one carries the given nested
     * payload (or none).
     */
    private File createIncludeChain(File dir, int hops, int leafNestingDepth) throws Exception {
        for (int i = 0; i < hops; i++) {
            File f = new File(dir, "hop" + i + ".xsd");
            Writer out = new FileWriter(f);
            try {
                out.write("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\""
                          + " targetNamespace=\"urn:nesting\">");
                if (i + 1 < hops) {
                    out.write("<xs:include schemaLocation=\"hop" + (i + 1) + ".xsd\"/>");
                } else if (leafNestingDepth > 0) {
                    String leaf = buildNestedSchema(leafNestingDepth);
                    int start = leaf.indexOf("<xs:element");
                    out.write(leaf.substring(start, leaf.lastIndexOf("</xs:schema>")));
                }
                out.write("</xs:schema>");
            } finally {
                out.close();
            }
        }
        return new File(dir, "hop0.xsd");
    }
}
