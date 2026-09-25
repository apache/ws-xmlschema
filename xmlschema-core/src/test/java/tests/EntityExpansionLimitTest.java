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

import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * The JDK parser unwinds nested entities recursively, so a chain of internal entities each
 * referring to the next must be refused by the entity-expansion limit before it can exhaust the
 * thread stack.
 */
public class EntityExpansionLimitTest extends Assert {
    private static final String ENTITY_EXPANSION_LIMIT = "jdk.xml.entityExpansionLimit";

    private String savedLimit;

    @Before
    public void saveLimit() {
        savedLimit = System.getProperty(ENTITY_EXPANSION_LIMIT);
        System.clearProperty(ENTITY_EXPANSION_LIMIT);
    }

    @After
    public void restoreLimit() {
        if (savedLimit == null) {
            System.clearProperty(ENTITY_EXPANSION_LIMIT);
        } else {
            System.setProperty(ENTITY_EXPANSION_LIMIT, savedLimit);
        }
    }

    /**
     * On a 256 KB stack a chain of 3,000 overflowed inside the JDK parser on JDK 8 to 25; at the
     * default stack size a chain of about 30,000 did.
     */
    @Test
    public void testEntityChainIsRefusedOnASmallStack() throws Exception {
        final Throwable[] thrown = new Throwable[1];
        Thread reader = new Thread(null, new Runnable() {
            public void run() {
                try {
                    new XmlSchemaCollection().read(new StringReader(chainedSchema(3000)));
                } catch (Throwable t) {
                    thrown[0] = t;
                }
            }
        }, "entity-chain", 256 * 1024);
        reader.start();
        reader.join();
        assertTrue("Expected XmlSchemaException, got " + thrown[0],
                   thrown[0] instanceof XmlSchemaException);
        assertTrue(thrown[0].getMessage(), thrown[0].getMessage().contains("too many entities"));
    }

    @Test
    public void testEntityChainWithinTheLimitStillParses() {
        assertNotNull(new XmlSchemaCollection().read(new StringReader(chainedSchema(900))));
    }

    @Test
    public void testLowerEntityExpansionLimitIsKept() {
        System.setProperty(ENTITY_EXPANSION_LIMIT, "5");
        try {
            new XmlSchemaCollection().read(new StringReader(chainedSchema(10)));
            fail("Ten expansions should be refused under a limit of five.");
        } catch (XmlSchemaException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("too many entities"));
        }
    }

    private static String chainedSchema(int length) {
        StringBuilder schema = new StringBuilder("<!DOCTYPE xs:schema [");
        for (int i = 1; i < length; i++) {
            schema.append("<!ENTITY e").append(i).append(" '&e").append(i + 1).append(";'>");
        }
        schema.append("<!ENTITY e").append(length).append(" 'x'>]>")
            .append("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">")
            .append("<xs:annotation><xs:documentation>&e1;</xs:documentation></xs:annotation>")
            .append("</xs:schema>");
        return schema.toString();
    }
}
