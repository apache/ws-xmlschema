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

package org.apache.ws.commons.schema.docpath;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.docpath.XmlSchemaNamespaceContext;
import org.junit.Test;

public class TestXmlSchemaNamespaceContext {

    @Test
    public void test() {
        XmlSchemaNamespaceContext nsContext = new XmlSchemaNamespaceContext();

        assertEquals(2, nsContext.getDeclaredPrefixes().length);
        assertArrayEquality(new String[] {Constants.XML_NS_PREFIX, Constants.XMLNS_ATTRIBUTE},
                            nsContext.getDeclaredPrefixes());

        Map<String, String> expPrefixToNsMap = new HashMap<String, String>();
        expPrefixToNsMap.put(Constants.XML_NS_PREFIX, Constants.XML_NS_URI);
        expPrefixToNsMap.put(Constants.XMLNS_ATTRIBUTE, Constants.XMLNS_ATTRIBUTE_NS_URI);

        Map<String, String[]> expNsToPrefixMap = new HashMap<String, String[]>();

        expNsToPrefixMap.put(Constants.XML_NS_URI, new String[] {Constants.XML_NS_PREFIX});

        expNsToPrefixMap.put(Constants.XMLNS_ATTRIBUTE_NS_URI, new String[] {Constants.XMLNS_ATTRIBUTE});

        assertEquality(expPrefixToNsMap, expNsToPrefixMap, nsContext);

        // "avro" -> "http://avro.apache.org/AvroTest"
        final String avroPrefix = "avro";
        final String avroNs = "http://avro.apache.org/AvroTest";

        nsContext.addNamespace(avroPrefix, avroNs);
        expPrefixToNsMap.put(avroPrefix, avroNs);
        expNsToPrefixMap.put(avroNs, new String[] {avroPrefix});

        assertEquality(expPrefixToNsMap, expNsToPrefixMap, nsContext);

        // "dei" -> "http://xbrl.sec.gov/dei/2012-01-31"
        final String deiPrefix = "dei";
        final String deiNs = "http://xbrl.sec.gov/dei/2012-01-31";

        nsContext.addNamespace(deiPrefix, deiNs);
        expPrefixToNsMap.put(deiPrefix, deiNs);
        expNsToPrefixMap.put(deiNs, new String[] {deiPrefix});

        assertEquality(expPrefixToNsMap, expNsToPrefixMap, nsContext);

        // "iso4217" -> "http://www.xbrl.org/2003/iso4217"
        final String iso4217Prefix = "iso4217";
        final String iso4217Ns = "http://www.xbrl.org/2003/iso4217";

        nsContext.addNamespace(iso4217Prefix, iso4217Ns);
        expPrefixToNsMap.put(iso4217Prefix, iso4217Ns);
        expNsToPrefixMap.put(iso4217Ns, new String[] {iso4217Prefix});

        assertEquality(expPrefixToNsMap, expNsToPrefixMap, nsContext);

        // Attach "dei" to "http://www.xbrl.org/2003/iso4217"
        nsContext.addNamespace(deiPrefix, iso4217Ns);
        expPrefixToNsMap.put(deiPrefix, iso4217Ns);
        expNsToPrefixMap.put(iso4217Ns, new String[] {iso4217Prefix, deiPrefix});
        expNsToPrefixMap.remove(deiNs);

        assertEquality(expPrefixToNsMap, expNsToPrefixMap, nsContext);

        // Attach "iso4217" to "http://xbrl.sec.gov/dei/2012-01-31"
        nsContext.addNamespace(iso4217Prefix, deiNs);
        expPrefixToNsMap.put(iso4217Prefix, deiNs);
        expNsToPrefixMap.put(deiNs, new String[] {iso4217Prefix});
        expNsToPrefixMap.put(iso4217Ns, new String[] {deiPrefix});

        assertEquality(expPrefixToNsMap, expNsToPrefixMap, nsContext);

        // Remove "dei" to "http://www.xbrl.org/2003/iso4217" mapping.
        nsContext.removeNamespace(deiPrefix);
        expPrefixToNsMap.put(deiPrefix, deiNs);
        expNsToPrefixMap.put(deiNs, new String[] {iso4217Prefix, deiPrefix});
        expNsToPrefixMap.remove(iso4217Ns);

        assertEquality(expPrefixToNsMap, expNsToPrefixMap, nsContext);

        // Remove "iso4217" to "http://xbrl.sec.gov/dei/2012-01-31" mapping.
        nsContext.removeNamespace(iso4217Prefix);
        expPrefixToNsMap.put(iso4217Prefix, iso4217Ns);
        expNsToPrefixMap.put(deiNs, new String[] {deiPrefix});
        expNsToPrefixMap.put(iso4217Ns, new String[] {iso4217Prefix});

        assertEquality(expPrefixToNsMap, expNsToPrefixMap, nsContext);

        // Remove "iso4217" to "http://www.xbrl.org/2003/iso4217" mapping.
        nsContext.removeNamespace(iso4217Prefix);
        expPrefixToNsMap.remove(iso4217Prefix);
        expNsToPrefixMap.remove(iso4217Ns);

        assertEquality(expPrefixToNsMap, expNsToPrefixMap, nsContext);

        // Remove "dei" to "http:///xbrl.sec.gov/dei/2012-01-31"
        nsContext.removeNamespace(deiPrefix);
        expPrefixToNsMap.remove(deiPrefix);
        expNsToPrefixMap.remove(deiNs);

        assertEquality(expPrefixToNsMap, expNsToPrefixMap, nsContext);

        // Try to remove a dead prefix.
        try {
            nsContext.removeNamespace(deiPrefix);
            fail("Should not have been able to remove " + deiPrefix);
        } catch (IllegalStateException ise) {
            // Passes.
        }

        // Add the dei namespace back in under the avro namespace.
        nsContext.addNamespace(deiPrefix, avroNs);
        expPrefixToNsMap.put(deiPrefix, avroNs);
        expNsToPrefixMap.put(avroNs, new String[] {deiPrefix, avroPrefix});

        assertEquality(expPrefixToNsMap, expNsToPrefixMap, nsContext);

        // Remove the "avro" to "http://avro.apache.org/AvroTest" mapping.
        nsContext.removeNamespace(avroPrefix);
        expPrefixToNsMap.remove(avroPrefix);
        expNsToPrefixMap.put(avroNs, new String[] {deiPrefix});

        assertEquality(expPrefixToNsMap, expNsToPrefixMap, nsContext);

        nsContext.removeNamespace(deiPrefix);
        expPrefixToNsMap.remove(deiPrefix);
        expNsToPrefixMap.remove(avroNs);

        assertEquality(expPrefixToNsMap, expNsToPrefixMap, nsContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullPrefixForNamespace() {
        XmlSchemaNamespaceContext nsContext = new XmlSchemaNamespaceContext();
        nsContext.getNamespaceURI(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullNamespaceForPrefix() {
        XmlSchemaNamespaceContext nsContext = new XmlSchemaNamespaceContext();
        nsContext.getPrefix(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddNullNamespaceAndPrefix() {
        XmlSchemaNamespaceContext nsContext = new XmlSchemaNamespaceContext();
        nsContext.addNamespace(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddNullNamespaceWithPrefix() {
        XmlSchemaNamespaceContext nsContext = new XmlSchemaNamespaceContext();
        nsContext.addNamespace("avro", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddEmptyNamespaceWithPrefix() {
        XmlSchemaNamespaceContext nsContext = new XmlSchemaNamespaceContext();
        nsContext.addNamespace("avro", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPrefixesWithNullNamespace() {
        XmlSchemaNamespaceContext nsContext = new XmlSchemaNamespaceContext();
        nsContext.getPrefixes(null);
    }

    @Test
    public void testNamespaceNotFound() {
        XmlSchemaNamespaceContext nsContext = new XmlSchemaNamespaceContext();
        assertNull(nsContext.getPrefix("urn:avro:test"));
        assertEquals(Constants.NULL_NS_URI, nsContext.getNamespaceURI("avro"));
    }

    static void assertEquality(Map<String, String> expPrefixToNamespace, Map<String, String[]> expNsToPrefix,
                               XmlSchemaNamespaceContext actual) {

        final Set<String> pfxKeys = expPrefixToNamespace.keySet();
        final String[] prefixes = pfxKeys.toArray(new String[pfxKeys.size()]);

        assertArrayEquality(prefixes, actual.getDeclaredPrefixes());

        for (String prefix : prefixes) {
            final String expNamespace = expPrefixToNamespace.get(prefix);

            assertEquals(expNamespace, actual.getNamespaceURI(prefix));
        }

        for (String expNamespace : expNsToPrefix.keySet()) {
            final String[] expPrefixes = expNsToPrefix.get(expNamespace);

            assertArrayEquality(expPrefixes, actual.getPrefixes(expNamespace));

            final String firstPrefix = actual.getPrefix(expNamespace);
            boolean found = false;
            for (int pfxIdx = 0; pfxIdx < expPrefixes.length; ++pfxIdx) {
                if (expPrefixes[pfxIdx].equals(firstPrefix)) {
                    found = true;
                    break;
                }
            }

            assertTrue("Could not find \"" + firstPrefix + "\" in the expected prefix list.", found);

        }
    }

    static void assertArrayEquality(String[] expected, String[] actual) {
        assertEquals(expected.length, actual.length);

        int found = 0;
        for (int expIndex = 0; expIndex < expected.length; ++expIndex) {
            for (int actIndex = 0; actIndex < actual.length; ++actIndex) {
                if (expected[expIndex].equals(actual[actIndex])) {
                    ++found;
                    break;
                }
            }
        }
        assertEquals(expected.length, found);
    }

    static void assertArrayEquality(String[] expected, Iterator actual) {
        HashSet<String> expSet = new HashSet<String>();
        for (String exp : expected) {
            expSet.add(exp);
        }

        while (actual.hasNext()) {
            final String next = actual.next().toString();
            assertTrue("Could not find " + next + " in the expected set.", expSet.contains(next));
            expSet.remove(next);
        }

        if (!expSet.isEmpty()) {
            fail("The expected list has more elements than the actual.");
        }
    }
}
