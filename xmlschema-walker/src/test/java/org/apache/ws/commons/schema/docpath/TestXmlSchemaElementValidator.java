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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.ValidationException;
import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaUse;
import org.apache.ws.commons.schema.docpath.XmlSchemaElementValidator;
import org.apache.ws.commons.schema.docpath.XmlSchemaNamespaceContext;
import org.apache.ws.commons.schema.docpath.XmlSchemaStateMachineNode;
import org.apache.ws.commons.schema.walker.XmlSchemaAttrInfo;
import org.apache.ws.commons.schema.walker.XmlSchemaBaseSimpleType;
import org.apache.ws.commons.schema.walker.XmlSchemaRestriction;
import org.apache.ws.commons.schema.walker.XmlSchemaTypeInfo;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.Attributes;

public class TestXmlSchemaElementValidator {

    private static final String NAMESPACE = "urn:avro:test";
    private static final String PREFIX = "avro";

    private static final String PROHIBITED = "prohibited";
    private static final String REQUIRED = "required";
    private static final String OPTIONAL = "optional";

    private static XmlSchema xmlSchema;
    private static XmlSchemaElement xmlElement;
    private static XmlSchemaAttribute prohibitedAttribute;
    private static XmlSchemaAttribute optionalAttribute;
    private static XmlSchemaAttribute requiredAttribute;
    private static XmlSchemaNamespaceContext nsContext;

    private static class SaxAttribute {

        SaxAttribute(String localName, String val) {
            qName = new QName(NAMESPACE, localName);
            qualifiedName = PREFIX + ':' + localName;
            value = val;
        }

        final QName qName;
        final String qualifiedName;
        final String value;
    }

    private static class SaxAttributes implements Attributes {

        SaxAttributes() {
            attributes = new ArrayList<SaxAttribute>();
            attrsByQualifiedName = new HashMap<String, SaxAttribute>();
            attrsByQName = new HashMap<QName, SaxAttribute>();

            indexByQualifiedName = new HashMap<String, Integer>();
            indexByQName = new HashMap<QName, Integer>();
        }

        void add(SaxAttribute attr) {
            attrsByQualifiedName.put(attr.qualifiedName, attr);
            attrsByQName.put(attr.qName, attr);

            indexByQualifiedName.put(attr.qualifiedName, attributes.size());
            indexByQName.put(attr.qName, attributes.size());

            attributes.add(attr);
        }

        @Override
        public int getLength() {
            return attributes.size();
        }

        @Override
        public String getURI(int index) {
            if (attributes.size() <= index) {
                return null;
            } else {
                return attributes.get(index).qName.getNamespaceURI();
            }
        }

        @Override
        public String getLocalName(int index) {
            if (attributes.size() <= index) {
                return null;
            } else {
                return attributes.get(index).qName.getLocalPart();
            }
        }

        @Override
        public String getQName(int index) {
            if (attributes.size() <= index) {
                return null;
            } else {
                return attributes.get(index).qualifiedName;
            }
        }

        @Override
        public String getType(int index) {
            if (attributes.size() <= index) {
                return null;
            } else {
                return "CDATA"; // We do not know the type information.
            }
        }

        @Override
        public String getValue(int index) {
            if (attributes.size() <= index) {
                return null;
            } else {
                return attributes.get(index).value;
            }
        }

        @Override
        public int getIndex(String uri, String localName) {
            if ((uri == null) || (localName == null)) {
                return -1;
            }

            final QName qName = new QName(uri, localName);
            final Integer index = indexByQName.get(qName);

            if (index == null) {
                return -1;
            } else {
                return index;
            }
        }

        @Override
        public int getIndex(String qName) {
            if (qName == null) {
                return -1;
            }

            final Integer index = indexByQualifiedName.get(qName);
            if (index == null) {
                return -1;
            } else {
                return index;
            }
        }

        @Override
        public String getType(String uri, String localName) {
            if ((uri == null) || (localName == null)) {
                return null;
            } else {
                final SaxAttribute attr = attrsByQName.get(new QName(uri, localName));
                return (attr == null) ? null : "CDATA";
            }
        }

        @Override
        public String getType(String qName) {
            if (qName == null) {
                return null;
            } else {
                final SaxAttribute attr = attrsByQualifiedName.get(qName);
                return (attr == null) ? null : "CDATA";
            }
        }

        @Override
        public String getValue(String uri, String localName) {
            if ((uri == null) || (localName == null)) {
                return null;
            } else {
                final SaxAttribute attr = attrsByQName.get(new QName(uri, localName));
                return (attr == null) ? null : attr.value;
            }
        }

        @Override
        public String getValue(String qName) {
            if (qName == null) {
                return null;
            } else {
                final SaxAttribute attr = attrsByQualifiedName.get(qName);
                return (attr == null) ? null : attr.value;
            }
        }

        private final List<SaxAttribute> attributes;
        private final Map<String, SaxAttribute> attrsByQualifiedName;
        private final Map<QName, SaxAttribute> attrsByQName;
        private final Map<String, Integer> indexByQualifiedName;
        private final Map<QName, Integer> indexByQName;
    }

    @Test
    public void testSaxAttributes() {
        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(OPTIONAL, "123.45"));
        saxAttributes.add(new SaxAttribute(PROHIBITED, "hello"));
        saxAttributes.add(new SaxAttribute(REQUIRED, "true"));
        saxAttributes.add(new SaxAttribute("test", "-73"));

        assertEquals(4, saxAttributes.getLength());

        for (int i = 0; i < 4; ++i) {
            assertEquals(NAMESPACE, saxAttributes.getURI(i));
            assertEquals("CDATA", saxAttributes.getType(i));
        }
        assertEquals(null, saxAttributes.getURI(5));
        assertEquals(null, saxAttributes.getType(5));

        assertEquals(OPTIONAL, saxAttributes.getLocalName(0));
        assertEquals(PREFIX + ':' + OPTIONAL, saxAttributes.getQName(0));
        assertEquals("123.45", saxAttributes.getValue(0));
        assertEquals(0, saxAttributes.getIndex(NAMESPACE, OPTIONAL));
        assertEquals(0, saxAttributes.getIndex(PREFIX + ':' + OPTIONAL));
        assertEquals("CDATA", saxAttributes.getType(NAMESPACE, OPTIONAL));
        assertEquals("CDATA", saxAttributes.getType(PREFIX + ':' + OPTIONAL));
        assertEquals("123.45", saxAttributes.getValue(NAMESPACE, OPTIONAL));
        assertEquals("123.45", saxAttributes.getValue(PREFIX + ':' + OPTIONAL));

        assertEquals(PROHIBITED, saxAttributes.getLocalName(1));
        assertEquals(PREFIX + ':' + PROHIBITED, saxAttributes.getQName(1));
        assertEquals("hello", saxAttributes.getValue(1));
        assertEquals(1, saxAttributes.getIndex(NAMESPACE, PROHIBITED));
        assertEquals(1, saxAttributes.getIndex(PREFIX + ':' + PROHIBITED));
        assertEquals("CDATA", saxAttributes.getType(NAMESPACE, PROHIBITED));
        assertEquals("CDATA", saxAttributes.getType(PREFIX + ':' + PROHIBITED));
        assertEquals("hello", saxAttributes.getValue(NAMESPACE, PROHIBITED));
        assertEquals("hello", saxAttributes.getValue(PREFIX + ':' + PROHIBITED));

        assertEquals(REQUIRED, saxAttributes.getLocalName(2));
        assertEquals(PREFIX + ':' + REQUIRED, saxAttributes.getQName(2));
        assertEquals("true", saxAttributes.getValue(2));
        assertEquals(2, saxAttributes.getIndex(NAMESPACE, REQUIRED));
        assertEquals(2, saxAttributes.getIndex(PREFIX + ':' + REQUIRED));
        assertEquals("CDATA", saxAttributes.getType(NAMESPACE, REQUIRED));
        assertEquals("CDATA", saxAttributes.getType(PREFIX + ':' + REQUIRED));
        assertEquals("true", saxAttributes.getValue(NAMESPACE, REQUIRED));
        assertEquals("true", saxAttributes.getValue(PREFIX + ':' + REQUIRED));

        assertEquals("test", saxAttributes.getLocalName(3));
        assertEquals(PREFIX + ":test", saxAttributes.getQName(3));
        assertEquals("-73", saxAttributes.getValue(3));
        assertEquals(3, saxAttributes.getIndex(NAMESPACE, "test"));
        assertEquals(3, saxAttributes.getIndex(PREFIX + ":test"));
        assertEquals("CDATA", saxAttributes.getType(NAMESPACE, "test"));
        assertEquals("CDATA", saxAttributes.getType(PREFIX + ":test"));
        assertEquals("-73", saxAttributes.getValue(NAMESPACE, "test"));
        assertEquals("-73", saxAttributes.getValue(PREFIX + ":test"));
    }

    @BeforeClass
    public static void setUpXmlSchema() {
        xmlSchema = new XmlSchema();
        xmlSchema.setTargetNamespace(NAMESPACE);

        xmlElement = new XmlSchemaElement(xmlSchema, false);
        xmlElement.setName("elem");

        prohibitedAttribute = new XmlSchemaAttribute(xmlSchema, false);
        prohibitedAttribute.setUse(XmlSchemaUse.PROHIBITED);
        prohibitedAttribute.setName(PROHIBITED);

        optionalAttribute = new XmlSchemaAttribute(xmlSchema, false);
        optionalAttribute.setUse(XmlSchemaUse.OPTIONAL);
        optionalAttribute.setName(OPTIONAL);

        requiredAttribute = new XmlSchemaAttribute(xmlSchema, false);
        requiredAttribute.setUse(XmlSchemaUse.REQUIRED);
        requiredAttribute.setName(REQUIRED);

        nsContext = new XmlSchemaNamespaceContext();
        nsContext.addNamespace(PREFIX, NAMESPACE);
    }

    @Test(expected = ValidationException.class)
    public void testAttributesWithNothing() throws Exception {
        XmlSchemaElementValidator.validateAttributes(null, null, null);
    }

    @Test(expected = ValidationException.class)
    public void testAttributesWithNoState() throws Exception {
        XmlSchemaElementValidator.validateAttributes(null, new SaxAttributes(), nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testAttributesWithNoAttributes() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.BOOLEAN));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                null, nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testAttributesWithNoNamespaceContext() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.BOOLEAN));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                new SaxAttributes(), null);
    }

    @Test
    public void testElementWithNoAttributes() throws Exception {
        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null,
                                                                               new XmlSchemaTypeInfo(true));

        XmlSchemaElementValidator.validateAttributes(stateMachine, new SaxAttributes(), nsContext);

        stateMachine = new XmlSchemaStateMachineNode(xmlElement, Collections.<XmlSchemaAttrInfo> emptyList(),
                                                     new XmlSchemaTypeInfo(true));

        XmlSchemaElementValidator.validateAttributes(stateMachine, new SaxAttributes(), nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testNotAnElement() throws Exception {
        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(
                                                                               XmlSchemaStateMachineNode.Type.SUBSTITUTION_GROUP,
                                                                               1, 1);

        XmlSchemaElementValidator.validateAttributes(stateMachine, new SaxAttributes(), nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testContentWithNothing() throws Exception {
        XmlSchemaElementValidator.validateAttributes(null, null, null);
    }

    @Test(expected = ValidationException.class)
    public void testContentWithNoState() throws Exception {
        XmlSchemaElementValidator.validateContent(null, "hello", nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testContentWithNoNamespaceContext() throws Exception {
        XmlSchemaElementValidator
            .validateContent(new XmlSchemaStateMachineNode(
                                                           xmlElement,
                                                           null,
                                                           new XmlSchemaTypeInfo(
                                                                                 XmlSchemaBaseSimpleType.STRING)),
                             "hello", null);
    }

    @Test(expected = ValidationException.class)
    public void testContentNotAnElement() throws Exception {
        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(
                                                                               XmlSchemaStateMachineNode.Type.SUBSTITUTION_GROUP,
                                                                               1, 1);

        XmlSchemaElementValidator.validateContent(stateMachine, "hello", nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testInvalidUse() throws Exception {
        XmlSchemaAttribute attr = new XmlSchemaAttribute(xmlSchema, false);
        attr.setUse(XmlSchemaUse.NONE);
        attr.setName("none");

        XmlSchemaTypeInfo attrType = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING);

        List<XmlSchemaAttrInfo> attrs = Collections
            .<XmlSchemaAttrInfo> singletonList(new XmlSchemaAttrInfo(attr, attrType));

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, attrs,
                                                                               new XmlSchemaTypeInfo(true));

        XmlSchemaElementValidator.validateAttributes(stateMachine, new SaxAttributes(), nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testProhibitedAttribute() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.BOOLEAN));

        SaxAttributes saxAttrs = new SaxAttributes();

        saxAttrs.add(new SaxAttribute(PROHIBITED, "true"));
        saxAttrs.add(new SaxAttribute(REQUIRED, "true"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttrs, nsContext);
    }

    @Test
    public void testEmptyProhibitedAttribute() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.BOOLEAN));

        SaxAttributes saxAttrs = new SaxAttributes();

        saxAttrs.add(new SaxAttribute(PROHIBITED, ""));
        saxAttrs.add(new SaxAttribute(REQUIRED, "true"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttrs, nsContext);
    }

    @Test
    public void testOptionalAttribute() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = new ArrayList<XmlSchemaAttrInfo>(1);

        attrs.add(new XmlSchemaAttrInfo(optionalAttribute,
                                        new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DOUBLE)));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                new SaxAttributes(), nsContext);
    }

    @Test
    public void testRequiredAttribute() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = new ArrayList<XmlSchemaAttrInfo>(1);

        attrs.add(new XmlSchemaAttrInfo(requiredAttribute,
                                        new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DOUBLE)));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "123.45"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testUnsatisfiedRequiredAttribute() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = new ArrayList<XmlSchemaAttrInfo>(1);

        attrs.add(new XmlSchemaAttrInfo(requiredAttribute,
                                        new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DOUBLE)));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                new SaxAttributes(), nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testEmptyRequiredAttribute() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = new ArrayList<XmlSchemaAttrInfo>(1);

        attrs.add(new XmlSchemaAttrInfo(requiredAttribute,
                                        new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DOUBLE)));

        SaxAttributes saxAttrs = new SaxAttributes();
        saxAttrs.add(new SaxAttribute(REQUIRED, ""));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttrs, nsContext);
    }

    @Test
    public void onlyOneAttrRequired() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DOUBLE));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "123.45"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test(expected = ValidationException.class)
    public void oneUnsatisfiedRequiredAttr() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DOUBLE));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                new SaxAttributes(), nsContext);
    }

    @Test(expected = ValidationException.class)
    public void optionalAttrsSetRequiredNot() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DOUBLE));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(OPTIONAL, "123.45"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test(expected = ValidationException.class)
    public void attributeHasComplexType() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(true));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "hi"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test
    public void testValidDuration() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(
                                                                              XmlSchemaBaseSimpleType.DURATION));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "P1D"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testInvalidDuration() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(
                                                                              XmlSchemaBaseSimpleType.DURATION));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "fail!"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test
    public void testValidDateTime() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(
                                                                              XmlSchemaBaseSimpleType.DATETIME));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "2014-07-27T12:47:30"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testInvalidDateTime() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(
                                                                              XmlSchemaBaseSimpleType.DATETIME));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "fail!"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test
    public void testValidTime() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.TIME));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "12:47:30"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testInvalidTime() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.TIME));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "fail!"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test
    public void testValidDate() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DATE));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "2014-07-27"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testInvalidDate() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DATE));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "fail!"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test
    public void testValidYearMonth() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(
                                                                              XmlSchemaBaseSimpleType.YEARMONTH));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "2014-07"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testInvalidYearMonth() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(
                                                                              XmlSchemaBaseSimpleType.YEARMONTH));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "fail!"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test
    public void testValidYear() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.YEAR));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "2014"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testInvalidYear() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.YEAR));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "fail!"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test
    public void testValidMonthDay() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(
                                                                              XmlSchemaBaseSimpleType.MONTHDAY));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "--07-27"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testInvalidMonthDay() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(
                                                                              XmlSchemaBaseSimpleType.MONTHDAY));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "fail!"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test
    public void testValidDay() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DAY));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "---27"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testInvalidDay() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DAY));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "fail!"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test
    public void testValidMonth() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.MONTH));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "--07"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testInvalidMonth() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.MONTH));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "fail!"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test
    public void testValidBooleanTrue() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.BOOLEAN));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "true"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test
    public void testValidBooleanFalse() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.BOOLEAN));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "false"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testInvalidBoolean() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.BOOLEAN));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "fail!"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test
    public void testValidBase64() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(
                                                                              XmlSchemaBaseSimpleType.BIN_BASE64));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "fail!"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test
    public void testValidHexadecimal() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.BIN_HEX));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "0F00"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testInvalidHexadecimal() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.BIN_HEX));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "fail!"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test
    public void testValidFloat() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.FLOAT));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "12.34"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testInvalidFloat() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.FLOAT));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "fail!"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test
    public void testValidDecimalWithEmptyRangeFacets() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = new HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>>();

        facets.put(XmlSchemaRestriction.Type.EXCLUSIVE_MAX, Collections.<XmlSchemaRestriction> emptyList());

        facets.put(XmlSchemaRestriction.Type.EXCLUSIVE_MIN, Collections.<XmlSchemaRestriction> emptyList());

        facets.put(XmlSchemaRestriction.Type.INCLUSIVE_MAX, Collections.<XmlSchemaRestriction> emptyList());

        facets.put(XmlSchemaRestriction.Type.INCLUSIVE_MIN, Collections.<XmlSchemaRestriction> emptyList());

        List<XmlSchemaAttrInfo> attrs = Collections
            .singletonList(new XmlSchemaAttrInfo(requiredAttribute,
                                                 new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL,
                                                                       facets)));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "123456789.123456789"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testInvalidDecimal() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "fail!"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test
    public void testValidDouble() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DOUBLE));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "12345.12345"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testInvalidDouble() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DOUBLE));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "fail!"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test
    public void testValidQName() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.QNAME));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "avro:hi"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testInvalidQName() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.QNAME));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "test:failure"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test
    public void testValidNotation() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(
                                                                              XmlSchemaBaseSimpleType.NOTATION));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "avro:one avro:two"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testInvalidNotation() throws Exception {
        ArrayList<XmlSchemaAttrInfo> attrs = buildAttrs(new XmlSchemaTypeInfo(
                                                                              XmlSchemaBaseSimpleType.NOTATION));

        SaxAttributes saxAttributes = new SaxAttributes();
        saxAttributes.add(new SaxAttribute(REQUIRED, "test:fails test:fails"));

        XmlSchemaElementValidator
            .validateAttributes(new XmlSchemaStateMachineNode(
                                                              xmlElement,
                                                              attrs,
                                                              new XmlSchemaTypeInfo(
                                                                                    XmlSchemaBaseSimpleType.STRING)),
                                saxAttributes, nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testInvalidNonMixedContentType() throws Exception {
        XmlSchemaTypeInfo typeInfo = new XmlSchemaTypeInfo(false);
        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, typeInfo);

        XmlSchemaElementValidator.validateContent(stateMachine, "fail", nsContext);
    }

    @Test
    public void testNullNonMixedContentType() throws Exception {
        XmlSchemaTypeInfo typeInfo = new XmlSchemaTypeInfo(false);
        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, typeInfo);

        XmlSchemaElementValidator.validateContent(stateMachine, null, nsContext);
    }

    @Test
    public void testEmptyNonMixedContentType() throws Exception {
        XmlSchemaTypeInfo typeInfo = new XmlSchemaTypeInfo(false);
        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, typeInfo);

        XmlSchemaElementValidator.validateContent(stateMachine, "", nsContext);
    }

    @Test
    public void testMixedContentType() throws Exception {
        XmlSchemaTypeInfo typeInfo = new XmlSchemaTypeInfo(true);
        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, typeInfo);

        XmlSchemaElementValidator.validateContent(stateMachine, "", nsContext);
        XmlSchemaElementValidator.validateContent(stateMachine, null, nsContext);
        XmlSchemaElementValidator.validateContent(stateMachine, "hi!", nsContext);
    }

    @Test
    public void testContentOfValidExclusiveNumericRange() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = getExclusiveNumericFacets();

        XmlSchemaTypeInfo typeInfo = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL, facets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, typeInfo);

        XmlSchemaElementValidator.validateContent(stateMachine, "125", nsContext);
        XmlSchemaElementValidator.validateContent(stateMachine, "-45", nsContext);
        XmlSchemaElementValidator.validateContent(stateMachine, "0", nsContext);
        XmlSchemaElementValidator.validateContent(stateMachine, "1", nsContext);
        XmlSchemaElementValidator.validateContent(stateMachine, "-1", nsContext);
        XmlSchemaElementValidator.validateContent(stateMachine, "127", nsContext);
        XmlSchemaElementValidator.validateContent(stateMachine, "-127", nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testContentOfTooLowExclusiveNumericRange() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = getExclusiveNumericFacets();

        XmlSchemaTypeInfo typeInfo = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL, facets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, typeInfo);

        XmlSchemaElementValidator.validateContent(stateMachine, "-150", nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testContentOfLowBorderExclusiveNumericRange() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = getExclusiveNumericFacets();

        XmlSchemaTypeInfo typeInfo = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL, facets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, typeInfo);

        XmlSchemaElementValidator.validateContent(stateMachine, "-128", nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testContentOfTooHighExclusiveNumericRange() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = getExclusiveNumericFacets();

        XmlSchemaTypeInfo typeInfo = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL, facets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, typeInfo);

        XmlSchemaElementValidator.validateContent(stateMachine, "150", nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testContentOfHighBorderExclusiveNumericRange() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = getExclusiveNumericFacets();

        XmlSchemaTypeInfo typeInfo = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL, facets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, typeInfo);

        XmlSchemaElementValidator.validateContent(stateMachine, "128", nsContext);
    }

    @Test
    public void testContentOfValidInclusiveNumericRange() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = getInclusiveNumericFacets();

        XmlSchemaTypeInfo typeInfo = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL, facets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, typeInfo);

        XmlSchemaElementValidator.validateContent(stateMachine, "128", nsContext);
        XmlSchemaElementValidator.validateContent(stateMachine, "-45", nsContext);
        XmlSchemaElementValidator.validateContent(stateMachine, "0", nsContext);
        XmlSchemaElementValidator.validateContent(stateMachine, "1", nsContext);
        XmlSchemaElementValidator.validateContent(stateMachine, "-1", nsContext);
        XmlSchemaElementValidator.validateContent(stateMachine, "97", nsContext);
        XmlSchemaElementValidator.validateContent(stateMachine, "-128", nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testContentOfTooLowInclusiveNumericRange() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = getInclusiveNumericFacets();

        XmlSchemaTypeInfo typeInfo = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL, facets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, typeInfo);

        XmlSchemaElementValidator.validateContent(stateMachine, "-150", nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testContentOfTooHighInclusiveNumericRange() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = getInclusiveNumericFacets();

        XmlSchemaTypeInfo typeInfo = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL, facets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, typeInfo);

        XmlSchemaElementValidator.validateContent(stateMachine, "150", nsContext);
    }

    @Test
    public void testFloatAndStringBasedRangeCheck() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = new HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>>();

        ArrayList<XmlSchemaRestriction> minValueInclRestr = new ArrayList<XmlSchemaRestriction>(1);

        minValueInclRestr.add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.INCLUSIVE_MIN,
                                                       new Float(-128.0), false));

        ArrayList<XmlSchemaRestriction> maxValueInclRestr = new ArrayList<XmlSchemaRestriction>(1);

        maxValueInclRestr
            .add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.INCLUSIVE_MAX, "128", false));

        facets.put(XmlSchemaRestriction.Type.INCLUSIVE_MIN, minValueInclRestr);
        facets.put(XmlSchemaRestriction.Type.INCLUSIVE_MAX, maxValueInclRestr);

        XmlSchemaTypeInfo typeInfo = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL, facets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, typeInfo);

        XmlSchemaElementValidator.validateContent(stateMachine, "128", nsContext);
        XmlSchemaElementValidator.validateContent(stateMachine, "-45", nsContext);
        XmlSchemaElementValidator.validateContent(stateMachine, "0", nsContext);
        XmlSchemaElementValidator.validateContent(stateMachine, "1", nsContext);
        XmlSchemaElementValidator.validateContent(stateMachine, "-1", nsContext);
        XmlSchemaElementValidator.validateContent(stateMachine, "97", nsContext);
        XmlSchemaElementValidator.validateContent(stateMachine, "-128", nsContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRangeType() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = new HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>>();

        ArrayList<XmlSchemaRestriction> minValueInclRestr = new ArrayList<XmlSchemaRestriction>(1);

        minValueInclRestr.add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.INCLUSIVE_MIN, new Object(),
                                                       false));

        facets.put(XmlSchemaRestriction.Type.INCLUSIVE_MAX, minValueInclRestr);

        XmlSchemaTypeInfo typeInfo = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL, facets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, typeInfo);

        XmlSchemaElementValidator.validateContent(stateMachine, "128", nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testInvalidRangeValue() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = new HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>>();

        ArrayList<XmlSchemaRestriction> minValueInclRestr = new ArrayList<XmlSchemaRestriction>(1);

        minValueInclRestr.add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.INCLUSIVE_MIN, "fail!",
                                                       false));

        facets.put(XmlSchemaRestriction.Type.INCLUSIVE_MAX, minValueInclRestr);

        XmlSchemaTypeInfo typeInfo = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL, facets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, typeInfo);

        XmlSchemaElementValidator.validateContent(stateMachine, "128", nsContext);
    }

    @Test
    public void testValidStringNoFacets() throws Exception {
        XmlSchemaTypeInfo typeInfo = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, typeInfo);

        XmlSchemaElementValidator.validateContent(stateMachine, "12", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, "aser921f", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, "\u1234\u5432", nsContext);
    }

    @Test
    public void testValidStringLengthRange() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = getLengthRange();

        XmlSchemaTypeInfo typeInfo = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING, facets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, typeInfo);

        XmlSchemaElementValidator.validateContent(stateMachine, "12", nsContext);
        XmlSchemaElementValidator.validateContent(stateMachine, "123", nsContext);
        XmlSchemaElementValidator.validateContent(stateMachine, "1234", nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testStringToShortForRange() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = getLengthRange();

        XmlSchemaTypeInfo typeInfo = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING, facets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, typeInfo);

        XmlSchemaElementValidator.validateContent(stateMachine, "1", nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testStringTooLongForRange() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = getLengthRange();

        XmlSchemaTypeInfo typeInfo = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING, facets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, typeInfo);

        XmlSchemaElementValidator.validateContent(stateMachine, "12345", nsContext);
    }

    @Test
    public void testValidStringLength() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = getLength();

        XmlSchemaTypeInfo typeInfo = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING, facets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, typeInfo);

        XmlSchemaElementValidator.validateContent(stateMachine, "123", nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testStringTooShort() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = getLength();

        XmlSchemaTypeInfo typeInfo = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING, facets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, typeInfo);

        XmlSchemaElementValidator.validateContent(stateMachine, "12", nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testStringTooLong() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = getLength();

        XmlSchemaTypeInfo typeInfo = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING, facets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, typeInfo);

        XmlSchemaElementValidator.validateContent(stateMachine, "1234", nsContext);
    }

    @Test
    public void testValidArray() throws Exception {
        XmlSchemaTypeInfo baseType = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING);

        XmlSchemaTypeInfo listType = new XmlSchemaTypeInfo(baseType);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, listType);

        XmlSchemaElementValidator.validateContent(stateMachine, "1234", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, "1 2 3 4", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, "1 23", nsContext);
    }

    @Test
    public void testValidArrayInRange() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> baseFacets = getExclusiveNumericFacets();

        XmlSchemaTypeInfo baseType = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL, baseFacets);

        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> listFacets = getLengthRange();

        XmlSchemaTypeInfo listType = new XmlSchemaTypeInfo(baseType, listFacets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, listType);

        XmlSchemaElementValidator.validateContent(stateMachine, "97 2", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, "-127 0 127", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, "-127 -64 64 127", nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testArrayTooShort() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> baseFacets = getExclusiveNumericFacets();

        XmlSchemaTypeInfo baseType = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL, baseFacets);

        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> listFacets = getLengthRange();

        XmlSchemaTypeInfo listType = new XmlSchemaTypeInfo(baseType, listFacets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, listType);

        XmlSchemaElementValidator.validateContent(stateMachine, "97", nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testArrayTooLong() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> baseFacets = getExclusiveNumericFacets();

        XmlSchemaTypeInfo baseType = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL, baseFacets);

        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> listFacets = getLengthRange();

        XmlSchemaTypeInfo listType = new XmlSchemaTypeInfo(baseType, listFacets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, listType);

        XmlSchemaElementValidator.validateContent(stateMachine, "1 2 3 4 5", nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testArrayWithInvalidContent() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> baseFacets = getExclusiveNumericFacets();

        XmlSchemaTypeInfo baseType = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL, baseFacets);

        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> listFacets = getLengthRange();

        XmlSchemaTypeInfo listType = new XmlSchemaTypeInfo(baseType, listFacets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, listType);

        XmlSchemaElementValidator.validateContent(stateMachine, "-128 128", nsContext);
    }

    @Test
    public void testArrayLength() throws Exception {
        XmlSchemaTypeInfo baseType = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING);

        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> listFacets = getLength();

        XmlSchemaTypeInfo listType = new XmlSchemaTypeInfo(baseType, listFacets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, listType);

        XmlSchemaElementValidator.validateContent(stateMachine, "1 2 3", nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testArrayLengthTooShort() throws Exception {
        XmlSchemaTypeInfo baseType = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING);

        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> listFacets = getLength();

        XmlSchemaTypeInfo listType = new XmlSchemaTypeInfo(baseType, listFacets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, listType);

        XmlSchemaElementValidator.validateContent(stateMachine, "1 2", nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testArrayLengthTooLong() throws Exception {
        XmlSchemaTypeInfo baseType = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING);

        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> listFacets = getLength();

        XmlSchemaTypeInfo listType = new XmlSchemaTypeInfo(baseType, listFacets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, listType);

        XmlSchemaElementValidator.validateContent(stateMachine, "1 2 3 4", nsContext);
    }

    @Test
    public void testTotalDigitsFacet() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = new HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>>();

        facets.put(XmlSchemaRestriction.Type.DIGITS_TOTAL, getTotalDigitsFacet(5));

        XmlSchemaTypeInfo type = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL, facets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, type);

        XmlSchemaElementValidator.validateContent(stateMachine, "1", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, "12", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, "123", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, "1234", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, "12345", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, ".1", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, ".12", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, ".123", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, ".1234", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, ".12345", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, "1.2", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, "12.3", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, "12.34", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, "12.345", nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testTooManyDigitsFacet() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = new HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>>();

        facets.put(XmlSchemaRestriction.Type.DIGITS_TOTAL, getTotalDigitsFacet(1));

        XmlSchemaTypeInfo type = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL, facets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, type);

        XmlSchemaElementValidator.validateContent(stateMachine, "12", nsContext);
    }

    @Test
    public void testDecimalNoFacets() throws Exception {
        XmlSchemaTypeInfo type = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, type);

        XmlSchemaElementValidator.validateContent(stateMachine, "1234567890.1234567890", nsContext);
    }

    @Test
    public void testFractionDigitsFacet() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = new HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>>();

        facets.put(XmlSchemaRestriction.Type.DIGITS_FRACTION, getFractionDigitsFacet(5));

        XmlSchemaTypeInfo type = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL, facets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, type);

        XmlSchemaElementValidator.validateContent(stateMachine, "12345678", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, "12345678.", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, "12345678.0", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, "12345678.01", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, "12345678.012", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, "12345678.0123", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, "12345678.01234", nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testTooManyFractionDigitsFacet() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = new HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>>();

        facets.put(XmlSchemaRestriction.Type.DIGITS_FRACTION, getFractionDigitsFacet(1));

        XmlSchemaTypeInfo type = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL, facets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, type);

        XmlSchemaElementValidator.validateContent(stateMachine, ".12", nsContext);
    }

    @Test
    public void testValidNoFractionDigitsFacet() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = new HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>>();

        facets.put(XmlSchemaRestriction.Type.DIGITS_FRACTION, getFractionDigitsFacet(0));

        XmlSchemaTypeInfo type = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL, facets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, type);

        XmlSchemaElementValidator.validateContent(stateMachine, "12456789", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, "12456789.", nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testInvalidNoFractionDigitsFacet() throws Exception {
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = new HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>>();

        facets.put(XmlSchemaRestriction.Type.DIGITS_FRACTION, getFractionDigitsFacet(0));

        XmlSchemaTypeInfo type = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL, facets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, type);

        XmlSchemaElementValidator.validateContent(stateMachine, ".1", nsContext);
    }

    @Test
    public void testValidEnumerationFacet() throws Exception {
        ArrayList<XmlSchemaRestriction> enumeration = new ArrayList<XmlSchemaRestriction>(3);

        enumeration.add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.ENUMERATION, "avro", false));

        enumeration.add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.ENUMERATION, "xml", false));

        enumeration.add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.ENUMERATION, "json", false));

        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = new HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>>();

        facets.put(XmlSchemaRestriction.Type.ENUMERATION, enumeration);

        XmlSchemaTypeInfo type = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING, facets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, type);

        XmlSchemaElementValidator.validateContent(stateMachine, "avro", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, "xml", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, "json", nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testInvalidEnumerationFacet() throws Exception {
        ArrayList<XmlSchemaRestriction> enumeration = new ArrayList<XmlSchemaRestriction>(3);

        enumeration.add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.ENUMERATION, "avro", false));

        enumeration.add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.ENUMERATION, "xml", false));

        enumeration.add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.ENUMERATION, "json", false));

        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = new HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>>();

        facets.put(XmlSchemaRestriction.Type.ENUMERATION, enumeration);

        XmlSchemaTypeInfo type = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING, facets);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, type);

        XmlSchemaElementValidator.validateContent(stateMachine, "thrift", nsContext);
    }

    @Test
    public void testValidUnion() throws Exception {
        ArrayList<XmlSchemaTypeInfo> unionTypes = new ArrayList<XmlSchemaTypeInfo>(3);

        unionTypes.add(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.BOOLEAN));
        unionTypes.add(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL));
        unionTypes.add(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DATE));

        XmlSchemaTypeInfo unionType = new XmlSchemaTypeInfo(unionTypes);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, unionType);

        XmlSchemaElementValidator.validateContent(stateMachine, "true", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, "false", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, "128.256", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, "2014-08-14", nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testInvalidUnion() throws Exception {
        ArrayList<XmlSchemaTypeInfo> unionTypes = new ArrayList<XmlSchemaTypeInfo>(3);

        unionTypes.add(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.BOOLEAN));
        unionTypes.add(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL));
        unionTypes.add(new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DATE));

        XmlSchemaTypeInfo unionType = new XmlSchemaTypeInfo(unionTypes);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(xmlElement, null, unionType);

        XmlSchemaElementValidator.validateContent(stateMachine, "fail!", nsContext);
    }

    @Test
    public void testNillableElement() throws Exception {
        XmlSchemaElement element = new XmlSchemaElement(xmlSchema, false);
        element.setNillable(true);
        element.setName("nillable");

        XmlSchemaTypeInfo elemType = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DOUBLE);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(element, null, elemType);

        XmlSchemaElementValidator.validateContent(stateMachine, "", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, null, nsContext);
    }

    @Test
    public void testElementWithDefault() throws Exception {
        XmlSchemaElement element = new XmlSchemaElement(xmlSchema, false);
        element.setNillable(false);
        element.setDefaultValue("123.456");
        element.setName("default");

        XmlSchemaTypeInfo elemType = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DOUBLE);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(element, null, elemType);

        XmlSchemaElementValidator.validateContent(stateMachine, "", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, null, nsContext);
    }

    @Test
    public void testFixedElement() throws Exception {
        XmlSchemaElement element = new XmlSchemaElement(xmlSchema, false);
        element.setNillable(false);
        element.setFixedValue("123.456");
        element.setName("fixed");

        XmlSchemaTypeInfo elemType = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DOUBLE);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(element, null, elemType);

        XmlSchemaElementValidator.validateContent(stateMachine, "", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, null, nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testNonNillableNonDefaultNonFixedElement() throws Exception {
        XmlSchemaElement element = new XmlSchemaElement(xmlSchema, false);
        element.setNillable(false);
        element.setName("invalid");

        XmlSchemaTypeInfo elemType = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DOUBLE);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(element, null, elemType);

        XmlSchemaElementValidator.validateContent(stateMachine, "", nsContext);

        XmlSchemaElementValidator.validateContent(stateMachine, null, nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testElementWithInvalidDefault() throws Exception {
        XmlSchemaElement element = new XmlSchemaElement(xmlSchema, false);
        element.setNillable(false);
        element.setDefaultValue("fail!");
        element.setName("invalid");

        XmlSchemaTypeInfo elemType = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DOUBLE);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(element, null, elemType);

        XmlSchemaElementValidator.validateContent(stateMachine, null, nsContext);
    }

    @Test(expected = ValidationException.class)
    public void testInvalidFixedElement() throws Exception {
        XmlSchemaElement element = new XmlSchemaElement(xmlSchema, false);
        element.setNillable(false);
        element.setFixedValue("fail!");
        element.setName("invalid");

        XmlSchemaTypeInfo elemType = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DOUBLE);

        XmlSchemaStateMachineNode stateMachine = new XmlSchemaStateMachineNode(element, null, elemType);

        XmlSchemaElementValidator.validateContent(stateMachine, null, nsContext);
    }

    private static ArrayList<XmlSchemaAttrInfo> buildAttrs(XmlSchemaTypeInfo attrType) {

        ArrayList<XmlSchemaAttrInfo> attributes = new ArrayList<XmlSchemaAttrInfo>(3);

        attributes.add(new XmlSchemaAttrInfo(prohibitedAttribute, attrType));

        attributes.add(new XmlSchemaAttrInfo(requiredAttribute, attrType));

        attributes.add(new XmlSchemaAttrInfo(optionalAttribute, attrType));

        return attributes;
    }

    private static HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> getExclusiveNumericFacets() {

        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = new HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>>();

        ArrayList<XmlSchemaRestriction> minValueExclRestr = new ArrayList<XmlSchemaRestriction>(1);

        minValueExclRestr.add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.EXCLUSIVE_MIN,
                                                       new Integer(-128), false));

        ArrayList<XmlSchemaRestriction> maxValueExclRestr = new ArrayList<XmlSchemaRestriction>(1);

        maxValueExclRestr.add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.EXCLUSIVE_MAX,
                                                       new Double(128.0), false));

        facets.put(XmlSchemaRestriction.Type.EXCLUSIVE_MIN, minValueExclRestr);
        facets.put(XmlSchemaRestriction.Type.EXCLUSIVE_MAX, maxValueExclRestr);

        return facets;
    }

    private static HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> getInclusiveNumericFacets() {

        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = new HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>>();

        ArrayList<XmlSchemaRestriction> minValueInclRestr = new ArrayList<XmlSchemaRestriction>(1);

        minValueInclRestr.add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.INCLUSIVE_MIN,
                                                       new BigInteger("-128"), false));

        ArrayList<XmlSchemaRestriction> maxValueInclRestr = new ArrayList<XmlSchemaRestriction>(1);

        maxValueInclRestr.add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.INCLUSIVE_MAX,
                                                       new BigDecimal(128), false));

        facets.put(XmlSchemaRestriction.Type.INCLUSIVE_MIN, minValueInclRestr);
        facets.put(XmlSchemaRestriction.Type.INCLUSIVE_MAX, maxValueInclRestr);

        return facets;
    }

    private static HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> getLengthRange() {

        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = new HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>>();

        List<XmlSchemaRestriction> minList = Collections
            .<XmlSchemaRestriction> singletonList(new XmlSchemaRestriction(
                                                                           XmlSchemaRestriction.Type.LENGTH_MIN,
                                                                           "2", false));

        List<XmlSchemaRestriction> maxList = Collections
            .<XmlSchemaRestriction> singletonList(new XmlSchemaRestriction(
                                                                           XmlSchemaRestriction.Type.LENGTH_MIN,
                                                                           "4", false));

        facets.put(XmlSchemaRestriction.Type.LENGTH_MIN, minList);
        facets.put(XmlSchemaRestriction.Type.LENGTH_MAX, maxList);

        return facets;
    }

    private static HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> getLength() {

        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = new HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>>();

        List<XmlSchemaRestriction> facet = Collections
            .<XmlSchemaRestriction> singletonList(new XmlSchemaRestriction(XmlSchemaRestriction.Type.LENGTH,
                                                                           "3", false));

        facets.put(XmlSchemaRestriction.Type.LENGTH, facet);

        return facets;
    }

    private static List<XmlSchemaRestriction> getTotalDigitsFacet(int value) {
        List<XmlSchemaRestriction> facet = Collections
            .<XmlSchemaRestriction> singletonList(new XmlSchemaRestriction(
                                                                           XmlSchemaRestriction.Type.DIGITS_TOTAL,
                                                                           value, false));

        return facet;
    }

    private static List<XmlSchemaRestriction> getFractionDigitsFacet(int value) {
        List<XmlSchemaRestriction> facet = Collections
            .<XmlSchemaRestriction> singletonList(new XmlSchemaRestriction(
                                                                           XmlSchemaRestriction.Type.DIGITS_FRACTION,
                                                                           value, false));

        return facet;
    }
}
