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

import java.io.StringReader;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.walker.XmlSchemaWalker;

import org.junit.Assert;
import org.junit.Test;

/**
 * The walker reports "previously visited" by type identity while the state
 * machine generator historically kept its books by element QName. These
 * tests cover the legal schema shapes where the two disagree.
 */
public class TestStateMachineSharedType extends Assert {

    private static final String SHARED_TYPE_SCHEMA =
        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\""
        + " xmlns:tns=\"urn:shared\" targetNamespace=\"urn:shared\">"
        + "<xs:complexType name=\"T\"><xs:sequence>"
        + "<xs:element name=\"leaf\" type=\"xs:string\"/>"
        + "</xs:sequence></xs:complexType>"
        + "<xs:element name=\"root\"><xs:complexType><xs:sequence>"
        + "<xs:element name=\"a\" type=\"tns:T\"/>"
        + "<xs:element name=\"b\" type=\"tns:T\"/>"
        + "</xs:sequence></xs:complexType></xs:element>"
        + "</xs:schema>";

    /*
     * Two same-QName local declarations (dup) with different named types,
     * in different scopes (legal: the Element Declarations Consistent
     * constraint applies per content model). The third wrapper revisits
     * type A *after* the second declaration overwrote dup's QName entry.
     */
    private static final String SAME_QNAME_SCHEMA =
        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\""
        + " xmlns:tns=\"urn:dup\" targetNamespace=\"urn:dup\">"
        + "<xs:complexType name=\"A\"><xs:sequence>"
        + "<xs:element name=\"x\" type=\"xs:string\"/>"
        + "</xs:sequence></xs:complexType>"
        + "<xs:complexType name=\"B\"><xs:sequence>"
        + "<xs:element name=\"y\" type=\"xs:string\"/>"
        + "</xs:sequence></xs:complexType>"
        + "<xs:element name=\"root\"><xs:complexType><xs:sequence>"
        + "<xs:element name=\"w1\"><xs:complexType><xs:sequence>"
        + "<xs:element name=\"dup\" type=\"tns:A\"/>"
        + "</xs:sequence></xs:complexType></xs:element>"
        + "<xs:element name=\"w2\"><xs:complexType><xs:sequence>"
        + "<xs:element name=\"dup\" type=\"tns:B\"/>"
        + "</xs:sequence></xs:complexType></xs:element>"
        + "<xs:element name=\"w3\"><xs:complexType><xs:sequence>"
        + "<xs:element name=\"dup\" type=\"tns:A\"/>"
        + "</xs:sequence></xs:complexType></xs:element>"
        + "</xs:sequence></xs:complexType></xs:element>"
        + "</xs:schema>";

    /*
     * A global element with an anonymous type, referenced twice. The walker
     * hands the generator a fresh element copy and a fresh XmlSchemaTypeInfo
     * on the second reference, so this shape exercises the validated-QName
     * fast path (schema-type reference equality).
     */
    private static final String REFERENCED_ANONYMOUS_SCHEMA =
        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\""
        + " xmlns:tns=\"urn:refanon\" targetNamespace=\"urn:refanon\">"
        + "<xs:element name=\"e\"><xs:complexType><xs:sequence>"
        + "<xs:element name=\"inner\" type=\"xs:string\"/>"
        + "</xs:sequence></xs:complexType></xs:element>"
        + "<xs:element name=\"root\"><xs:complexType><xs:sequence>"
        + "<xs:element ref=\"tns:e\"/>"
        + "<xs:element ref=\"tns:e\"/>"
        + "</xs:sequence></xs:complexType></xs:element>"
        + "</xs:schema>";

    /*
     * A recursive named type re-entered under a different element QName
     * while its own children are still being walked: the transition copy
     * must be deferred until the first-visit node's content model is
     * complete.
     */
    private static final String RECURSIVE_SHARED_SCHEMA =
        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\""
        + " xmlns:tns=\"urn:recur\" targetNamespace=\"urn:recur\">"
        + "<xs:complexType name=\"R\"><xs:sequence>"
        + "<xs:element name=\"mid\" type=\"xs:string\"/>"
        + "<xs:element name=\"next\" type=\"tns:R\" minOccurs=\"0\"/>"
        + "</xs:sequence></xs:complexType>"
        + "<xs:element name=\"root\"><xs:complexType><xs:sequence>"
        + "<xs:element name=\"first\" type=\"tns:R\"/>"
        + "</xs:sequence></xs:complexType></xs:element>"
        + "</xs:schema>";

    @Test
    public void testTwoElementsSharingOneNamedComplexType() {
        final XmlSchemaStateMachineGenerator generator =
            walk(SHARED_TYPE_SCHEMA, new QName("urn:shared", "root"));

        assertNotNull(generator.getStartNode());

        final Map<QName, XmlSchemaStateMachineNode> nodes = generator.getStateMachineNodesByQName();
        final XmlSchemaStateMachineNode nodeA = nodes.get(new QName("urn:shared", "a"));
        final XmlSchemaStateMachineNode nodeB = nodes.get(new QName("urn:shared", "b"));
        assertNotNull(nodeA);
        assertNotNull(nodeB);

        /*
         * The legal document <root><a><leaf/></a><b><leaf/></b></root> must
         * be navigable: the walker skips b's children as previously visited,
         * so b's node only has a content model if the transitions were
         * copied from a's node. Non-null alone is not enough -- b's
         * possibleNextStates must match a's.
         */
        final List<XmlSchemaStateMachineNode> aNext = nodeA.getPossibleNextStates();
        final List<XmlSchemaStateMachineNode> bNext = nodeB.getPossibleNextStates();
        assertFalse("a's node should have a content model", aNext.isEmpty());
        assertEquals("b's node must carry the shared type's content model", aNext.size(),
                     bNext.size());
        for (int i = 0; i < aNext.size(); ++i) {
            assertEquals(aNext.get(i).getNodeType(), bNext.get(i).getNodeType());
            if (XmlSchemaStateMachineNode.Type.ELEMENT.equals(aNext.get(i).getNodeType())) {
                assertEquals(aNext.get(i).getElement().getQName(),
                             bNext.get(i).getElement().getQName());
            }
        }

        final QName leaf = new QName("urn:shared", "leaf");
        assertNotNull(findElement(nodeA, leaf));
        assertNotNull("leaf must be reachable through b's content model", findElement(nodeB, leaf));
    }

    @Test
    public void testSameQNameDistinctDeclarationsDoNotCrossBind() {
        final XmlSchemaStateMachineGenerator generator =
            walk(SAME_QNAME_SCHEMA, new QName("urn:dup", "root"));

        final XmlSchemaStateMachineNode start = generator.getStartNode();
        assertNotNull(start);

        final QName dup = new QName("urn:dup", "dup");
        final QName x = new QName("urn:dup", "x");
        final QName y = new QName("urn:dup", "y");

        final XmlSchemaStateMachineNode w2 = findElement(start, new QName("urn:dup", "w2"));
        final XmlSchemaStateMachineNode w3 = findElement(start, new QName("urn:dup", "w3"));
        assertNotNull(w2);
        assertNotNull(w3);

        // w2/dup is declared with type B: it leads to <y/>, never <x/>.
        final XmlSchemaStateMachineNode dupUnderW2 = findElement(w2, dup);
        assertNotNull(dupUnderW2);
        assertNotNull(findElement(dupUnderW2, y));
        assertNull(findElement(dupUnderW2, x));

        /*
         * w3/dup is declared with type A. Resolving the revisit through the
         * (overwritten) QName entry would silently bind it to type B's state
         * machine; it must lead to <x/>, never <y/>.
         */
        final XmlSchemaStateMachineNode dupUnderW3 = findElement(w3, dup);
        assertNotNull(dupUnderW3);
        assertNotNull("w3/dup must carry type A's content model", findElement(dupUnderW3, x));
        assertNull("w3/dup must not cross-bind to type B", findElement(dupUnderW3, y));
    }

    @Test
    public void testElementWithAnonymousTypeReferencedTwice() {
        final XmlSchemaStateMachineGenerator generator =
            walk(REFERENCED_ANONYMOUS_SCHEMA, new QName("urn:refanon", "root"));

        final XmlSchemaStateMachineNode start = generator.getStartNode();
        assertNotNull(start);

        final XmlSchemaStateMachineNode nodeE = findElement(start, new QName("urn:refanon", "e"));
        assertNotNull(nodeE);
        assertFalse(nodeE.getPossibleNextStates().isEmpty());
        assertNotNull(findElement(nodeE, new QName("urn:refanon", "inner")));
    }

    @Test
    public void testRecursiveTypeSharedUnderDifferentName() {
        final XmlSchemaStateMachineGenerator generator =
            walk(RECURSIVE_SHARED_SCHEMA, new QName("urn:recur", "root"));

        assertNotNull(generator.getStartNode());

        final Map<QName, XmlSchemaStateMachineNode> nodes = generator.getStateMachineNodesByQName();
        final XmlSchemaStateMachineNode nodeFirst = nodes.get(new QName("urn:recur", "first"));
        final XmlSchemaStateMachineNode nodeNext = nodes.get(new QName("urn:recur", "next"));
        assertNotNull(nodeFirst);
        assertNotNull(nodeNext);

        /*
         * "next" re-entered type R while "first" was still being walked; the
         * deferred copy must still deliver the complete content model.
         */
        assertFalse(nodeFirst.getPossibleNextStates().isEmpty());
        assertEquals(nodeFirst.getPossibleNextStates().size(),
                     nodeNext.getPossibleNextStates().size());

        final QName mid = new QName("urn:recur", "mid");
        final QName next = new QName("urn:recur", "next");
        assertNotNull(findElement(nodeNext, mid));
        assertNotNull(findElement(nodeNext, next));
    }

    private static XmlSchemaStateMachineGenerator walk(String schema, QName rootQName) {
        final XmlSchemaCollection collection = new XmlSchemaCollection();
        collection.read(new StringReader(schema));

        final XmlSchemaElement root = collection.getElementByQName(rootQName);
        assertNotNull(root);

        final XmlSchemaStateMachineGenerator generator = new XmlSchemaStateMachineGenerator();
        final XmlSchemaWalker walker = new XmlSchemaWalker(collection, generator);

        // Throws IllegalStateException without the type-identity handling.
        walker.walk(root);

        return generator;
    }

    /**
     * Breadth-first search of a node's content model for an element with the
     * given QName. Does not descend through nested elements (an element
     * boundary starts a nested content model) and is cycle-safe for
     * recursive types.
     */
    private static XmlSchemaStateMachineNode findElement(XmlSchemaStateMachineNode start,
                                                         QName qName) {
        final Map<XmlSchemaStateMachineNode, Boolean> visited =
            new IdentityHashMap<XmlSchemaStateMachineNode, Boolean>();
        final LinkedList<XmlSchemaStateMachineNode> queue =
            new LinkedList<XmlSchemaStateMachineNode>(start.getPossibleNextStates());

        while (!queue.isEmpty()) {
            final XmlSchemaStateMachineNode node = queue.removeFirst();
            if (visited.containsKey(node)) {
                continue;
            }
            visited.put(node, Boolean.TRUE);

            if (XmlSchemaStateMachineNode.Type.ELEMENT.equals(node.getNodeType())) {
                if (qName.equals(node.getElement().getQName())) {
                    return node;
                }
            } else {
                queue.addAll(node.getPossibleNextStates());
            }
        }

        return null;
    }
}
