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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Assert;
import org.junit.Test;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.xml.sax.helpers.DefaultHandler;

/**
 * SaxWalkerOverDom must use constant JVM stack space for document nesting.
 */
public class TestSaxWalkerOverDomDeepNesting extends Assert {

    private static final int DEPTH = 50000;

    @Test
    public void testDeeplyNestedDocumentDoesNotOverflowTheStack() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();

        Element current = document.createElement("a");
        document.appendChild(current);
        for (int index = 1; index < DEPTH; ++index) {
            Element child = document.createElement("a");
            current.appendChild(child);
            current = child;
        }
        current.appendChild(document.createTextNode("deepest"));

        final int[] counts = new int[2];
        SaxWalkerOverDom walker = new SaxWalkerOverDom(new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName,
                                     org.xml.sax.Attributes attributes) {
                ++counts[0];
            }

            @Override
            public void endElement(String uri, String localName, String qName) {
                ++counts[1];
            }
        });

        walker.walk(document);

        assertEquals(DEPTH, counts[0]);
        assertEquals(DEPTH, counts[1]);
    }
}