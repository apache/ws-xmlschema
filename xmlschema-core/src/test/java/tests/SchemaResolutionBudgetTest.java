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

import java.io.StringReader;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaException;
import org.apache.ws.commons.schema.resolver.URIResolver;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.InputSource;

/**
 * maxSchemaResolutions is meant to bound the number of documents one read may pull in. The
 * bound has to hold however the import graph is shaped: a deep chain is already caught by
 * maxImportDepth, so the case that needs this budget is the shallow, wide one.
 */
public class SchemaResolutionBudgetTest extends Assert {

    /** The documented default of org.apache.ws.commons.schema.maxSchemaResolutions. */
    private static final int MAX_RESOLUTIONS = 1000;

    /** Counts how many documents a read actually pulled in. */
    private static final class CountingResolver implements URIResolver {
        private final AtomicInteger count = new AtomicInteger();

        public InputSource resolveEntity(String namespace, String schemaLocation, String baseUri) {
            int id = count.incrementAndGet();
            InputSource source = new InputSource(new StringReader(
                "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\""
                + namespace + "\"><xs:element name=\"e\" type=\"xs:string\"/></xs:schema>"));
            source.setSystemId("http://example.invalid/leaf" + id + ".xsd");
            return source;
        }
    }

    /** Each read needs its own namespaces, or the second one collides in the collection. */
    private static String rootImporting(String tag, int leaves) {
        StringBuilder root = new StringBuilder(
            "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:root"
            + tag + "\">");
        for (int i = 0; i < leaves; i++) {
            root.append("<xs:import namespace=\"urn:leaf").append(tag).append('_').append(i)
                .append("\" schemaLocation=\"http://example.invalid/leaf").append(tag)
                .append('_').append(i).append(".xsd\"/>");
        }
        return root.append("</xs:schema>").toString();
    }

    private static CountingResolver read(int leaves) {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        CountingResolver resolver = new CountingResolver();
        collection.setSchemaResolver(resolver);
        collection.read(new StringReader(rootImporting("a", leaves)));
        return resolver;
    }

    /**
     * XMLSCHEMA-XXX: the budget was reset whenever the in-progress stack was empty, which it is
     * between two sibling imports of the top-level document, so a wide graph never reached it.
     */
    @Test
    public void testWideImportGraphIsBounded() {
        CountingResolver resolver;
        try {
            resolver = read(MAX_RESOLUTIONS * 5);
            fail("expected the resolution budget to stop a graph of "
                 + (MAX_RESOLUTIONS * 5) + " imports, but it resolved "
                 + resolver.count.get());
        } catch (XmlSchemaException expected) {
            assertTrue(expected.getMessage(),
                       expected.getMessage().contains("schema documents were resolved"));
        }
    }

    /** A graph inside the budget must still resolve in full. */
    @Test
    public void testGraphWithinBudgetStillResolves() {
        CountingResolver resolver = read(MAX_RESOLUTIONS / 2);
        assertEquals(MAX_RESOLUTIONS / 2, resolver.count.get());
    }

    /** The budget is per top-level read, so a second read starts from zero. */
    @Test
    public void testBudgetIsPerTopLevelRead() {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        CountingResolver resolver = new CountingResolver();
        collection.setSchemaResolver(resolver);
        for (int i = 0; i < 3; i++) {
            collection.read(new StringReader(rootImporting("r" + i, MAX_RESOLUTIONS / 2)));
        }
        assertTrue("a later read must not inherit an earlier read's spend",
                   resolver.count.get() > MAX_RESOLUTIONS);
    }
}
