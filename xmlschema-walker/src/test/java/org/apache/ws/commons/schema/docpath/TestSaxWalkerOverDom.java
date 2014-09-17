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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.apache.ws.commons.schema.docpath.SaxWalkerOverDom;
import org.apache.ws.commons.schema.testutils.UtilsForTests;
import org.junit.Test;

/**
 * Reads the <code>src/test/resources/test_schema.xsd</code> file as an XML
 * document and confirms the correct events are triggered.
 */
public class TestSaxWalkerOverDom {

    private static class AttrInfo {

        static List<AttrInfo> getAttributesOf(Attributes attrs) {
            ArrayList<AttrInfo> newAttrs = new ArrayList<AttrInfo>();

            for (int a = 0; a < attrs.getLength(); ++a) {
                newAttrs.add(new AttrInfo(attrs.getURI(a), attrs.getLocalName(a), attrs.getQName(a), attrs
                    .getValue(a)));
            }

            return newAttrs;
        }

        AttrInfo(String ns, String ln, String qn, String v) {
            namespace = new String(ns);
            localName = new String(ln);
            qName = new String(qn);
            value = new String(v);
        }

        String namespace;
        String localName;
        String qName;
        String value;
    }

    private static class StackEntry {
        enum Type {
            ELEMENT, TEXT
        }

        StackEntry(String v) {
            type = Type.TEXT;
            value = v;
            namespace = null;
            localName = null;
            qName = null;
            attributes = null;
        }

        StackEntry(String ns, String ln, String qn, Attributes attrs) {
            type = Type.ELEMENT;
            namespace = ns;
            localName = ln;
            qName = qn;
            attributes = AttrInfo.getAttributesOf(attrs);
            value = null;
        }

        boolean equals(String v) {
            return stringsEqual(value, v);
        }

        boolean equals(String ns, String ln, String qn, Attributes attrs) {
            if (!stringsEqual(namespace, ns) || !stringsEqual(localName, ln) || !stringsEqual(qName, qn)
                || (attributes.size() > attrs.getLength())) {
                throw new IllegalStateException("Expected element [\"" + namespace + "\", \"" + localName
                                                + "\", \"" + qName + "\", " + attributes.size()
                                                + " attrs] does not match actual of [\"" + ns + "\", \"" + ln
                                                + "\", \"" + qn + "\", " + attrs.getLength() + " attrs].");
            }

            for (int index = 0; index < attributes.size(); ++index) {
                AttrInfo attribute = attributes.get(index);
                final String actual = attrs.getValue(attribute.qName);

                if (!attribute.value.equals(actual)) {
                    return false;
                }

                if (!attrs.getValue(attribute.namespace, attribute.localName).equals(actual)) {
                    System.err.println("Attribute [\"" + attribute.namespace + "\", \"" + attribute.localName
                                       + "\"] has a value of \"" + attribute.value
                                       + "\" which does not match the actual value of \"" + actual + "\".  ");
                    return false;
                }
            }

            return true;
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder(type.name());
            str.append(": ");

            if (type.equals(Type.ELEMENT)) {
                str.append("namespace=\"").append(namespace);
                str.append("\", localName=\"").append(localName);
                str.append("\", qName=\"").append(qName);
                str.append("\", attributes={ ");

                if (attributes != null) {
                    for (int index = 0; index < attributes.size(); ++index) {
                        AttrInfo attribute = attributes.get(index);
                        str.append("[Attr: namespace=\"").append(attribute.namespace);
                        str.append("\", localName=\"").append(attribute.localName);
                        str.append("\", qName=\"").append(attribute.qName);
                        str.append("\", value=\"").append(attribute.value).append("\"] ");
                    }
                }

                str.append('}');

            } else if (type.equals(Type.TEXT)) {
                str.append('\"').append(value).append('\"');
            }

            return str.toString();
        }

        private static boolean stringsEqual(String lhs, String rhs) {
            if (((lhs != null) && (rhs != null) && lhs.equals(rhs)) || ((lhs == null) && (rhs == null))) {

                return true;

            } else {
                throw new IllegalArgumentException("\"" + lhs + "\" does not match \"" + rhs + "\"");
            }
        }

        Type type;
        String namespace;
        String localName;
        String qName;
        String value;
        List<AttrInfo> attributes;
    }

    /**
     * Used to confirm the {@link SaxWalkerOverDom} walker generates the exact
     * same events as a real {@link SAXParser}.
     */
    private static class ContentValidator implements ContentHandler {

        ContentValidator(List<StackEntry> stack, Map<String, List<String>> prefixMappings) {

            this.stack = stack;
            this.prefixMappings = prefixMappings;
        }

        @Override
        public void startDocument() throws SAXException {
        }

        @Override
        public void endDocument() throws SAXException {
            if (!stack.isEmpty()) {
                throw new SAXException("Reaced the end of the document early; expected " + stack.size()
                                       + " more elements.");
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts)
            throws SAXException {

            if (stack.isEmpty()) {
                throw new SAXException("Element " + toString(uri, localName, qName, atts)
                                       + " is not expected; stack is empty!");
            }

            StackEntry entry = stack.remove(0);

            if (entry.type != StackEntry.Type.ELEMENT) {
                throw new SAXException("Expected text of (" + entry + ") but received element of ("
                                       + toString(uri, localName, qName, atts) + ").");
            }

            if (!entry.equals(uri, localName, qName, atts)) {
                throw new SAXException("Expected element (" + entry + ") does not match actual ("
                                       + toString(uri, localName, qName, atts) + ").");
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {

            final String value = toString(ch, start, length);

            if (stack.isEmpty()) {
                throw new SAXException("Unexpected string \"" + value + "\"; stack is empty!");
            }

            StackEntry entry = stack.remove(0);

            if (!entry.type.equals(StackEntry.Type.TEXT)) {
                throw new SAXException("Unexpected string \"" + value + "\"; was expecting element (" + entry
                                       + ").");
            }

            if (!entry.equals(value)) {
                throw new SAXException("Expected string \"" + entry + "\" but received \"" + value + "\".");
            }
        }

        @Override
        public void startPrefixMapping(String prefix, String uri) throws SAXException {

            if (!prefixMappings.containsKey(prefix)) {
                throw new SAXException("Prefix \"" + prefix + "\" and URI \"" + uri + "\" are not expected.");
            }

            /*
             * This is a quick and dirty test; in reality the
             * prefix-to-namespace mapping is only valid if it exists inside the
             * current scope. That said, keeping track of the scope is a much
             * more difficult problem.
             */
            final List<String> namespaces = prefixMappings.get(prefix);
            boolean found = false;
            for (String namespace : namespaces) {
                if (namespace.equals(uri)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new SAXException("Prefix \"" + prefix + "\" does not have a namespace of \"" + uri
                                       + "\".");
            }
        }

        @Override
        public void endPrefixMapping(String prefix) throws SAXException {
            if (!prefixMappings.containsKey(prefix)) {
                throw new SAXException("Prefix \"" + prefix + "\" is not expected.");
            }
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            throw new UnsupportedOperationException("This should not be called.");
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            throw new UnsupportedOperationException("This should not be called.");
        }

        @Override
        public void processingInstruction(String target, String data) throws SAXException {
            throw new UnsupportedOperationException("This should not be called.");
        }

        @Override
        public void skippedEntity(String name) throws SAXException {
            throw new UnsupportedOperationException("This should not be called.");
        }

        private static String toString(String uri, String localName, String qName, Attributes attrs) {

            StringBuilder str = new StringBuilder("namespace=\"");
            str.append(uri).append("\", localName=\"").append(localName);
            str.append("\", qName=\"").append(qName).append("\", attributes={ ");

            for (int index = 0; index < attrs.getLength(); ++index) {
                str.append("[Attr: namespace=\"").append(attrs.getURI(index));
                str.append("\", localName=\"").append(attrs.getLocalName(index));
                str.append("\", qName=\"").append(attrs.getQName(index));
                str.append("\", value=\"").append(attrs.getValue(index));
                str.append("\"] ");
            }

            str.append('}');

            return str.toString();
        }

        private static String toString(char[] ch, int start, int length) {
            return new String(ch, start, length);
        }

        private List<StackEntry> stack;
        private Map<String, List<String>> prefixMappings;
    }

    /**
     * This is traversed by a SAX parser to retrieve the expected SAX events.
     */
    private static class StackBuilder extends DefaultHandler {

        public StackBuilder() {
            stack = new ArrayList<StackEntry>();
            prefixMappings = new HashMap<String, List<String>>();
        }

        public ArrayList<StackEntry> getStack() {
            return stack;
        }

        public Map<String, List<String>> getPrefixMappings() {
            return prefixMappings;
        }

        @Override
        public void startDocument() throws SAXException {
            stack.clear();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts)
            throws SAXException {

            stack.add(new StackEntry(uri, localName, qName, atts));
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {

            stack.add(new StackEntry(new String(ch, start, length)));
        }

        @Override
        public void startPrefixMapping(String prefix, String uri) throws SAXException {

            List<String> mappings = prefixMappings.get(prefix);
            if (mappings == null) {
                mappings = new ArrayList<String>(1);
                prefixMappings.put(prefix, mappings);
            }
            mappings.add(uri);
        }

        private final ArrayList<StackEntry> stack;
        private final Map<String, List<String>> prefixMappings;
    }

    @Test
    public void testRoot() throws Exception {
        final File xmlFile = UtilsForTests.buildFile("src", "test", "resources", "test1_root.xml");
        runTest(xmlFile);
    }

    @Test
    public void testChildren() throws Exception {
        final File xmlFile = UtilsForTests.buildFile("src", "test", "resources", "test2_children.xml");

        runTest(xmlFile);
    }

    @Test
    public void testGrandchildren() throws Exception {
        final File xmlFile = UtilsForTests.buildFile("src", "test", "resources", "test3_grandchildren.xml");

        runTest(xmlFile);
    }

    private static void runTest(File xmlFile) throws Exception {
        StackBuilder stackBuilder = new StackBuilder();

        // Parse the document using a real SAX parser
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();
        saxParser.parse(xmlFile, stackBuilder);

        // Parse the document using a DOM parser
        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        final Document doc = dBuilder.parse(xmlFile);

        /*
         * Walk the DOM, firing off SAX events, and confirm they match the real
         * SAX events.
         */
        final List<StackEntry> stack = stackBuilder.getStack();
        final int stackSize = stack.size();

        final SaxWalkerOverDom walker = new SaxWalkerOverDom(new ContentValidator(stack,
                                                                                  stackBuilder
                                                                                      .getPrefixMappings()));

        try {
            walker.walk(doc);
        } catch (Exception e) {
            throw new RuntimeException("Traversed through element " + (stackSize - stack.size())
                                       + " before failing.", e);
        }
    }
}
