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
package org.apache.ws.commons.schema.walker;

import java.io.StringReader;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaException;
import org.apache.ws.commons.schema.docpath.XmlSchemaStateMachineGenerator;

import org.junit.Assert;
import org.junit.Test;

/**
 * An acyclic schema can nest elements, model groups, substitution group
 * members, type derivations or attribute group references deeply enough to
 * exhaust the thread stack while it is walked. Each of these is bounded by
 * the walker's maximum depth, and exceeding it is reported as an
 * XmlSchemaException.
 */
public class WalkerDepthLimitTest extends Assert {

    private static final String NS = "urn:depth";

    /** Far beyond the default limit, and deep enough to overflow a default stack without it. */
    private static final int DEEP = 5000;

    /** Comfortably within the default limit. */
    private static final int SHALLOW = 100;

    private static void walkRoot(String body) {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        collection.read(new StringReader(
            "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:tns=\"" + NS + "\""
            + " targetNamespace=\"" + NS + "\" elementFormDefault=\"qualified\">"
            + body
            + "</xs:schema>"));
        XmlSchemaElement root = collection.getElementByQName(new QName(NS, "root"));
        assertNotNull("the schema under test must declare a 'root' element", root);
        new XmlSchemaWalker(collection, new XmlSchemaStateMachineGenerator()).walk(root);
    }

    private static void assertRejected(String body) {
        try {
            walkRoot(body);
            fail("expected the deeply nested schema to be rejected");
        } catch (XmlSchemaException expected) {
            assertTrue(expected.getMessage(),
                       expected.getMessage().contains(XmlSchemaWalker.MAX_DEPTH_PROPERTY));
        }
    }

    /** root, with e0 substituting for root, e1 for e0, and so on. */
    private static String substitutionGroupChain(int length) {
        StringBuilder body = new StringBuilder("<xs:element name=\"root\" type=\"xs:string\"/>");
        String head = "root";
        for (int i = 0; i < length; i++) {
            body.append("<xs:element name=\"e").append(i).append("\" type=\"xs:string\"")
                .append(" substitutionGroup=\"tns:").append(head).append("\"/>");
            head = "e" + i;
        }
        return body.toString();
    }

    /** root contains a reference to e1, which contains a reference to e2, and so on. */
    private static String elementReferenceChain(int length) {
        return elementReferenceChain(length, null);
    }

    private static String elementReferenceChain(int length, String lastType) {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < length; i++) {
            body.append("<xs:element name=\"").append((i == 0) ? "root" : "e" + i).append('"');
            if (i + 1 < length) {
                body.append("><xs:complexType><xs:sequence><xs:element ref=\"tns:e").append(i + 1)
                    .append("\"/></xs:sequence></xs:complexType></xs:element>");
            } else {
                body.append(" type=\"").append((lastType == null) ? "xs:string" : lastType).append("\"/>");
            }
        }
        return body.toString();
    }

    /** root is of type T0, which contains an element of type T1, and so on. */
    private static String namedTypeChain(int length) {
        StringBuilder body = new StringBuilder("<xs:element name=\"root\" type=\"tns:T0\"/>");
        for (int i = 0; i < length; i++) {
            body.append("<xs:complexType name=\"T").append(i).append("\"><xs:sequence>");
            if (i + 1 < length) {
                body.append("<xs:element name=\"c\" type=\"tns:T").append(i + 1).append("\"/>");
            }
            body.append("</xs:sequence></xs:complexType>");
        }
        return body.toString();
    }

    /** T1 extends T0, T2 extends T1, and so on; T0 references the given attribute group, if any. */
    private static String complexExtensionChain(int length, String attributeGroup) {
        StringBuilder body = new StringBuilder("<xs:complexType name=\"T0\"><xs:sequence>"
                                               + "<xs:element name=\"a0\" type=\"xs:string\"/>"
                                               + "</xs:sequence>");
        if (attributeGroup != null) {
            body.append("<xs:attributeGroup ref=\"tns:").append(attributeGroup).append("\"/>");
        }
        body.append("</xs:complexType>");
        for (int i = 1; i < length; i++) {
            body.append("<xs:complexType name=\"T").append(i).append("\"><xs:complexContent>")
                .append("<xs:extension base=\"tns:T").append(i - 1).append("\"><xs:sequence>")
                .append("<xs:element name=\"a").append(i).append("\" type=\"xs:string\"/>")
                .append("</xs:sequence></xs:extension></xs:complexContent></xs:complexType>");
        }
        return body.toString();
    }

    /** S1 restricts S0, S2 restricts S1, and so on. */
    private static String simpleRestrictionChain(int length) {
        StringBuilder body = new StringBuilder("<xs:element name=\"root\" type=\"tns:S")
            .append(length - 1).append("\"/>")
            .append("<xs:simpleType name=\"S0\"><xs:restriction base=\"xs:string\"/></xs:simpleType>");
        for (int i = 1; i < length; i++) {
            body.append("<xs:simpleType name=\"S").append(i).append("\"><xs:restriction base=\"tns:S")
                .append(i - 1).append("\"/></xs:simpleType>");
        }
        return body.toString();
    }

    /** g0 references g1, which references g2, and so on. */
    private static String groupReferenceChain(int length) {
        StringBuilder body = new StringBuilder("<xs:element name=\"root\"><xs:complexType><xs:sequence>"
                                               + "<xs:group ref=\"tns:g0\"/>"
                                               + "</xs:sequence></xs:complexType></xs:element>");
        for (int i = 0; i < length; i++) {
            body.append("<xs:group name=\"g").append(i).append("\"><xs:sequence>");
            if (i + 1 < length) {
                body.append("<xs:group ref=\"tns:g").append(i + 1).append("\"/>");
            } else {
                body.append("<xs:element name=\"leaf\" type=\"xs:string\"/>");
            }
            body.append("</xs:sequence></xs:group>");
        }
        return body.toString();
    }

    /** ag0 references ag1, which references ag2, and so on. */
    private static String attributeGroupChain(int length) {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < length; i++) {
            body.append("<xs:attributeGroup name=\"ag").append(i).append("\">");
            if (i + 1 < length) {
                body.append("<xs:attributeGroup ref=\"tns:ag").append(i + 1).append("\"/>");
            } else {
                body.append("<xs:attribute name=\"a\" type=\"xs:string\"/>");
            }
            body.append("</xs:attributeGroup>");
        }
        return body.toString();
    }

    private static String rootWithAttributeGroupChain(int length) {
        return "<xs:element name=\"root\"><xs:complexType><xs:attributeGroup ref=\"tns:ag0\"/>"
               + "</xs:complexType></xs:element>" + attributeGroupChain(length);
    }

    private static String rootWithComplexExtensionChain(int length) {
        return "<xs:element name=\"root\" type=\"tns:T" + (length - 1) + "\"/>"
               + complexExtensionChain(length, null);
    }

    @Test
    public void testDeepSubstitutionGroupChainIsRejected() {
        assertRejected(substitutionGroupChain(DEEP));
    }

    @Test
    public void testDeepElementReferenceChainIsRejected() {
        assertRejected(elementReferenceChain(DEEP));
    }

    @Test
    public void testDeepNamedTypeChainIsRejected() {
        assertRejected(namedTypeChain(DEEP));
    }

    @Test
    public void testDeepGroupReferenceChainIsRejected() {
        assertRejected(groupReferenceChain(DEEP));
    }

    @Test
    public void testDeepComplexExtensionChainIsRejected() {
        assertRejected(rootWithComplexExtensionChain(DEEP));
    }

    @Test
    public void testDeepSimpleRestrictionChainIsRejected() {
        assertRejected(simpleRestrictionChain(DEEP));
    }

    @Test
    public void testDeepAttributeGroupChainIsRejected() {
        assertRejected(rootWithAttributeGroupChain(DEEP));
    }

    @Test
    public void testShallowChainsStillWalk() {
        walkRoot(substitutionGroupChain(SHALLOW));
        walkRoot(elementReferenceChain(SHALLOW));
        walkRoot(namedTypeChain(SHALLOW));
        walkRoot(groupReferenceChain(SHALLOW));
        walkRoot(rootWithComplexExtensionChain(SHALLOW));
        walkRoot(simpleRestrictionChain(SHALLOW));
        walkRoot(rootWithAttributeGroupChain(SHALLOW));
    }

    /*
     * Type derivation and attribute group references are resolved while the
     * element walk is already at depth, so the stack must hold the element
     * walk and either chain at their limits at once. A complex type's
     * content is walked as one nested model group per level of extension,
     * so the deepest derivation that does not also nest content is a simple
     * type's.
     */
    @Test
    public void testAllChainsJustWithinTheLimitsTogetherStillWalk() {
        final int limit = XmlSchemaWalker.DEFAULT_MAX_DEPTH;
        // Each element level counts itself and its sequence.
        final int elementLevels = limit / 2;
        // The leaf type and the built-in xs:string at the base of the
        // chain each count one level of derivation too.
        final int simpleDerivations = limit - 2;
        walkRoot(elementReferenceChain(elementLevels, "tns:Leaf")
                 + "<xs:complexType name=\"Leaf\"><xs:simpleContent>"
                 + "<xs:extension base=\"tns:S" + (simpleDerivations - 1) + "\">"
                 + "<xs:attributeGroup ref=\"tns:ag0\"/>"
                 + "</xs:extension></xs:simpleContent></xs:complexType>"
                 + simpleRestrictionChain(simpleDerivations).replaceFirst("<xs:element [^>]*/>", "")
                 + attributeGroupChain(limit));
    }

    @Test
    public void testLimitIsConfigurable() {
        final String previous = System.getProperty(XmlSchemaWalker.MAX_DEPTH_PROPERTY);
        System.setProperty(XmlSchemaWalker.MAX_DEPTH_PROPERTY, "10");
        try {
            assertRejected(substitutionGroupChain(20));
            assertRejected(rootWithComplexExtensionChain(20));
            assertRejected(rootWithAttributeGroupChain(20));
            walkRoot(substitutionGroupChain(5));
        } finally {
            if (previous == null) {
                System.clearProperty(XmlSchemaWalker.MAX_DEPTH_PROPERTY);
            } else {
                System.setProperty(XmlSchemaWalker.MAX_DEPTH_PROPERTY, previous);
            }
        }
    }
}
