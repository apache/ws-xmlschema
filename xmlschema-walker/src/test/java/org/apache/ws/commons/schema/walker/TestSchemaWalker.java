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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.stream.StreamSource;

import org.junit.Assert;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAll;
import org.apache.ws.commons.schema.XmlSchemaAny;
import org.apache.ws.commons.schema.XmlSchemaAnyAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaChoice;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaFractionDigitsFacet;
import org.apache.ws.commons.schema.XmlSchemaMaxInclusiveFacet;
import org.apache.ws.commons.schema.XmlSchemaMinInclusiveFacet;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaPatternFacet;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaUse;
import org.apache.ws.commons.schema.XmlSchemaWhiteSpaceFacet;
import org.apache.ws.commons.schema.testutils.UtilsForTests;
import org.apache.ws.commons.schema.walker.XmlSchemaAttrInfo;
import org.apache.ws.commons.schema.walker.XmlSchemaBaseSimpleType;
import org.apache.ws.commons.schema.walker.XmlSchemaRestriction;
import org.apache.ws.commons.schema.walker.XmlSchemaTypeInfo;
import org.apache.ws.commons.schema.walker.XmlSchemaVisitor;
import org.apache.ws.commons.schema.walker.XmlSchemaWalker;
import org.junit.Test;

public class TestSchemaWalker {

    private static enum Type {
        ELEMENT, SEQUENCE, CHOICE, ALL, SUBSTITUTION_GROUP
    }

    private static class StackEntry {
        StackEntry(Type type) {
            this.type = type;
            this.name = null;
            this.typeName = null;
            this.facets = null;
            this.baseType = null;
            this.minOccurs = 1;
            this.maxOccurs = 1;
        }

        StackEntry(Type type, long minOccurs, long maxOccurs) {
            this(type);
            this.minOccurs = minOccurs;
            this.maxOccurs = maxOccurs;
        }

        StackEntry(Type type, String name) {
            this(type);
            this.name = name;
            this.typeName = null;
        }

        StackEntry(Type type, String name, String typeName) {
            this(type, name);
            this.typeName = typeName;
        }

        StackEntry(Type type, String name, String typeName, XmlSchemaBaseSimpleType baseType) {

            this(type, name, typeName);
            this.baseType = baseType;
        }

        StackEntry(Type type, String name, String typeName, long minOccurs, long maxOccurs) {

            this(type, name, typeName);
            this.minOccurs = minOccurs;
            this.maxOccurs = maxOccurs;
        }

        StackEntry(Type type, String name, String typeName, XmlSchemaBaseSimpleType baseType, long minOccurs,
                   long maxOccurs) {

            this(type, name, typeName, baseType);
            this.minOccurs = minOccurs;
            this.maxOccurs = maxOccurs;
        }

        StackEntry(Type type, String name, String typeName, XmlSchemaBaseSimpleType baseType,
                   Set<XmlSchemaRestriction> facets) {

            this(type, name, typeName, baseType);
            this.facets = facets;
        }

        StackEntry(Type type, String name, String typeName, XmlSchemaBaseSimpleType baseType, long minOccurs,
                   long maxOccurs, Set<XmlSchemaRestriction> facets) {

            this(type, name, typeName, baseType, minOccurs, maxOccurs);
            this.facets = facets;

        }

        Type type;
        String name;
        String typeName;
        Set<XmlSchemaRestriction> facets;
        long minOccurs;
        long maxOccurs;
        XmlSchemaBaseSimpleType baseType;
    }

    private static class Attribute {
        public Attribute(String name, String typeName, XmlSchemaTypeInfo.Type type,
                         XmlSchemaBaseSimpleType baseType) {

            this.name = name;
            this.typeName = typeName;
            this.isOptional = false;
            this.type = type;
            this.baseType = baseType;
        }

        public Attribute(String name, String typeName, XmlSchemaTypeInfo.Type type,
                         XmlSchemaBaseSimpleType baseType, boolean isOptional) {

            this(name, typeName, type, baseType);
            this.isOptional = isOptional;
        }

        public Attribute(String name, String typeName, XmlSchemaTypeInfo.Type type,
                         XmlSchemaBaseSimpleType baseType, Set<XmlSchemaRestriction> facets) {

            this(name, typeName, type, baseType);
            this.facets = facets;
        }

        public Attribute(String name, String typeName, XmlSchemaTypeInfo.Type type,
                         XmlSchemaBaseSimpleType baseType, boolean isOptional,
                         Set<XmlSchemaRestriction> facets) {

            this(name, typeName, type, baseType, isOptional);
            this.facets = facets;
        }

        String name;
        String typeName;
        boolean isOptional;
        Set<XmlSchemaRestriction> facets;
        XmlSchemaBaseSimpleType baseType;
        XmlSchemaTypeInfo.Type type;
    }

    private static class Visitor implements XmlSchemaVisitor {

        Visitor(List<StackEntry> stack, HashMap<String, List<Attribute>> attributes) {

            this.stack = stack;
            this.attributes = attributes;
        }

        @Override
        public void onEnterElement(XmlSchemaElement element, XmlSchemaTypeInfo typeInfo,
                                   boolean previouslyVisited) {

            StackEntry next = pop();
            if (next.type != Type.ELEMENT) {
                throw new IllegalStateException("Expected a " + next.type + " named \"" + next.name
                                                + "\" but received an element named \"" + element.getName()
                                                + "\".");

            } else if (!next.name.equals(element.getName())) {
                throw new IllegalStateException("Expected an element named \"" + next.name
                                                + "\" but received an element named " + element.getName()
                                                + "\"");

            } else if ((next.typeName == null) && !element.getSchemaType().isAnonymous()) {

                throw new IllegalStateException("Expected the element named \"" + next.name
                                                + "\" to carry an anonymous type, but the type was "
                                                + element.getSchemaType().getQName());

            } else if ((next.typeName != null) && element.getSchemaType().isAnonymous()) {
                throw new IllegalStateException("Expected the element named \"" + next.name
                                                + "\" to carry a type named \"" + next.typeName
                                                + "\"; but the type was anonymous instead.");
            }

            checkMinAndMaxOccurs(next, element);

            if (typeInfo != null) {
                checkFacets(next.name, typeInfo, next.facets);

                if ((next.baseType == null) && (typeInfo.getBaseType() != null)
                    && !typeInfo.getBaseType().equals(XmlSchemaBaseSimpleType.ANYTYPE)) {

                    throw new IllegalStateException("Element \"" + next.name
                                                    + "\" was not expected to have an Avro schema,"
                                                    + " but has a schema of " + typeInfo.getBaseType());

                } else if ((next.baseType != null) && (typeInfo.getBaseType() == null)) {

                    throw new IllegalStateException("Element \"" + next.name
                                                    + "\" was expected to have a schema of " + next.baseType
                                                    + " but instead has no schema.");

                } else if ((next.baseType != null) && !next.baseType.equals(typeInfo.getBaseType())) {
                    throw new IllegalStateException("Element \"" + next.name
                                                    + "\" was expected to have a schema of " + next.baseType
                                                    + " but instead has a schema of "
                                                    + typeInfo.getBaseType());
                }

            } else if (next.baseType != null) {
                throw new IllegalStateException("Expected a schema of " + next.baseType
                                                + " but received none.");
            }

            if ((next.facets != null) && !next.facets.isEmpty()) {
                StringBuilder errMsg = new StringBuilder("Element \"");
                errMsg.append(next.name);
                errMsg.append("\" was expected to have the following facets, ");
                errMsg.append("but did not:");

                for (XmlSchemaRestriction facet : next.facets) {
                    errMsg.append(" \"").append(facet).append('\"');
                }

                throw new IllegalStateException(errMsg.toString());
            }
        }

        @Override
        public void onExitElement(XmlSchemaElement element, XmlSchemaTypeInfo typeInfo,
                                  boolean previouslyVisited) {

            if (!previouslyVisited && attributes.containsKey(element.getName())) {
                List<Attribute> remainingAttrs = attributes.get(element.getName());
                if (!remainingAttrs.isEmpty()) {
                    StringBuilder errMsg = new StringBuilder("Element \"");
                    errMsg.append(element.getName());
                    errMsg.append("\" did not have the expected attributes ");
                    errMsg.append("of the following names:");
                    for (Attribute attr : remainingAttrs) {
                        errMsg.append(" \"").append(attr.name).append('\"');
                    }
                    throw new IllegalStateException(errMsg.toString());
                }
            }
        }

        @Override
        public void onVisitAttribute(XmlSchemaElement element, XmlSchemaAttrInfo attrInfo) {

            final XmlSchemaAttribute attribute = attrInfo.getAttribute();
            final XmlSchemaTypeInfo attributeType = attrInfo.getType();

            if (!attributes.containsKey(element.getName())) {
                throw new IllegalStateException("No attributes were expected for \"" + element.getName()
                                                + "\", but \"" + attribute.getQName() + "\" was found.");
            }

            List<Attribute> attrs = attributes.get(element.getName());
            boolean found = false;
            int index = 0;

            for (; index < attrs.size(); ++index) {
                Attribute attr = attrs.get(index);
                if (attr.name.equals(attribute.getName())) {
                    if ((attr.typeName == null)
                            && attribute.getSchemaType() != null
                            && !attribute.getSchemaType().isAnonymous()) {

                        throw new IllegalStateException("Element \"" + element.getName()
                                                        + "\" has an attribute named \"" + attr.name
                                                        + "\" whose type was expected to be anonymous, "
                                                        + "but actually is named \""
                                                        + attribute.getSchemaType().getName() + "\"");

                    } else if ((attr.typeName != null)
                            && attribute.getSchemaType() != null
                            && attribute.getSchemaType().isAnonymous()) {

                        throw new IllegalStateException("Element \"" + element.getName()
                                                        + "\" has an attribute named \"" + attr.name
                                                        + "\" whose type was expected to be \""
                                                        + attr.typeName + "\"; but is anonymous instead.");

                    } else if ((attr.typeName != null)
                            && attribute.getSchemaType() != null
                            && !attr.typeName.equals(attribute.getSchemaType().getName())) {

                        throw new IllegalStateException("Element \"" + element.getName()
                                                        + "\" has an attribute named \"" + attr.name
                                                        + "\"; its type was expected to be \""
                                                        + attr.typeName + "\" but instead was \""
                                                        + attribute.getSchemaType().getName() + "\"");

                    } else if (attr.isOptional && !attribute.getUse().equals(XmlSchemaUse.OPTIONAL)) {

                        throw new IllegalStateException(
                                                        "Element \""
                                                            + element.getName()
                                                            + "\" has an attribute named \""
                                                            + attr.name
                                                            + "\" whose usage was expected to be optional, but instead is "
                                                            + attribute.getUse());

                    } else if (!attr.isOptional && attribute.getUse().equals(XmlSchemaUse.OPTIONAL)) {

                        throw new IllegalStateException("Element \"" + element.getName()
                                                        + "\" has an attribute named \"" + attr.name
                                                        + "\" whose usage was expected to be required,"
                                                        + " but is actually optional.");

                    } else if (attr.type != null && !attr.type.equals(attributeType.getType())) {
                        throw new IllegalStateException("Element \"" + element.getName()
                                                        + "\" has an attribute named \"" + attr.name
                                                        + "\" whose type was expected to be " + attr.type
                                                        + " but actually was " + attributeType.getType());

                    } else if (attr.type != null && attr.type.equals(XmlSchemaTypeInfo.Type.ATOMIC)
                               && !attr.baseType.equals(attributeType.getBaseType())) {

                        throw new IllegalStateException("Element \"" + element.getName()
                                                        + "\" has an attribute named \"" + attr.name
                                                        + "\" whose type was expected to be "
                                                        + attr.baseType.name() + " but actually was "
                                                        + attributeType.getBaseType());

                    } else if (attr.type != null && attr.type.equals(XmlSchemaTypeInfo.Type.LIST)
                               && !attr.baseType.equals(attributeType.getChildTypes().get(0).getBaseType())) {

                        throw new IllegalStateException("Element \"" + element.getName()
                                                        + "\" has an attribute named \"" + attr.name
                                                        + "\" with a type of " + attr.type
                                                        + " whose base type is expected to be "
                                                        + attr.baseType + " but actually is "
                                                        + attributeType.getChildTypes().get(0).getBaseType());

                    } else {

                        checkFacets(attr.name, attributeType, attr.facets);

                        found = true;
                        break;
                    }
                }
            }

            if (found) {
                attrs.remove(index);
            } else {
                throw new IllegalStateException("Element \"" + element.getName()
                                                + "\" has unexpected attribute \"" + attribute.getName()
                                                + "\"");
            }
        }

        @Override
        public void onEndAttributes(XmlSchemaElement element, XmlSchemaTypeInfo elemTypeInfo) {

        }

        @Override
        public void onEnterSubstitutionGroup(XmlSchemaElement base) {
            StackEntry next = pop();

            if (next.type != Type.SUBSTITUTION_GROUP) {
                throw new IllegalStateException("Expected a " + next.type
                                                + " but instead found a substition group of \""
                                                + base.getName() + "\"");

            } else if (!next.name.equals(base.getName())) {
                throw new IllegalStateException("Expected a substitution group for element \"" + next.name
                                                + "\", but instead received one for \"" + base.getName()
                                                + "\"");

            } else if (next.minOccurs != base.getMinOccurs()) {
                throw new IllegalStateException("Expected a substitution group for element \"" + next.name
                                                + "\" and min occurs of " + next.minOccurs
                                                + ", but received a min occurs of " + base.getMinOccurs());

            } else if (next.maxOccurs != base.getMaxOccurs()) {
                throw new IllegalStateException("Expected a substitution group for element \"" + next.name
                                                + "\" and max occurs of " + next.maxOccurs
                                                + ", but received a max occurs of " + base.getMaxOccurs());
            }
        }

        @Override
        public void onExitSubstitutionGroup(XmlSchemaElement base) {
        }

        @Override
        public void onEnterAllGroup(XmlSchemaAll all) {
            StackEntry next = pop();
            if (next.type != Type.ALL) {
                throw new IllegalStateException("Expected a " + next.type + " but received an All group.");
            }
            checkMinAndMaxOccurs(next, all);
        }

        @Override
        public void onExitAllGroup(XmlSchemaAll all) {
        }

        @Override
        public void onEnterChoiceGroup(XmlSchemaChoice choice) {
            StackEntry next = pop();
            if (next.type != Type.CHOICE) {
                throw new IllegalStateException("Expected a " + next.type + " but received a Choice group.");
            }
            checkMinAndMaxOccurs(next, choice);
        }

        @Override
        public void onExitChoiceGroup(XmlSchemaChoice choice) {
        }

        @Override
        public void onEnterSequenceGroup(XmlSchemaSequence seq) {
            StackEntry next = pop();
            if (next.type != Type.SEQUENCE) {
                throw new IllegalStateException("Expected a " + next.type + " but received a Sequence group.");
            }
            checkMinAndMaxOccurs(next, seq);
        }

        @Override
        public void onExitSequenceGroup(XmlSchemaSequence seq) {
        }

        @Override
        public void onVisitAny(XmlSchemaAny any) {
            throw new IllegalStateException("No Any types were expected in the schema.");
        }

        @Override
        public void onVisitAnyAttribute(XmlSchemaElement element, XmlSchemaAnyAttribute anyAttr) {

            throw new IllegalStateException("No anyAttribute types were expected in the schema.");
        }

        private static void checkMinAndMaxOccurs(StackEntry next, XmlSchemaParticle particle) {

            if (next.minOccurs != particle.getMinOccurs()) {
                throw new IllegalStateException("Expected a minOccurs of " + next.minOccurs + " for "
                                                + next.type + " \"" + next.name
                                                + "\", but found a minOccurs of " + particle.getMinOccurs());

            } else if (next.maxOccurs != particle.getMaxOccurs()) {
                throw new IllegalStateException("Expected a maxOccurs of " + next.maxOccurs + " for "
                                                + next.type + " \"" + next.name
                                                + "\", but found a maxOccurs of " + particle.getMaxOccurs());
            }
        }

        private StackEntry pop() {
            if (stack.isEmpty()) {
                throw new IllegalStateException("Ran out of stack!");
            }

            StackEntry entry = stack.get(0);
            stack.remove(entry);
            return entry;
        }

        private List<StackEntry> stack;
        private HashMap<String, List<Attribute>> attributes;
    }

    /**
     * Test for src/main/resources/test_schema.xsd
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test() throws Exception {
        // Build the expectations.
        ArrayList<Attribute> attrGroupAttrs = new ArrayList<Attribute>(43);

        attrGroupAttrs.add(new Attribute("anySimpleType", "anySimpleType", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.ANYSIMPLETYPE, true));

        HashSet<XmlSchemaRestriction> whiteSpaceCollapseFixedRestrictions = new HashSet<XmlSchemaRestriction>();

        whiteSpaceCollapseFixedRestrictions
            .add(new XmlSchemaRestriction(new XmlSchemaWhiteSpaceFacet("collapse", true)));

        attrGroupAttrs.add(new Attribute("duration", "duration", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.DURATION, true,
                                         (Set<XmlSchemaRestriction>)whiteSpaceCollapseFixedRestrictions
                                             .clone()));
        attrGroupAttrs.add(new Attribute("dateTime", "dateTime", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.DATETIME, true,
                                         (Set<XmlSchemaRestriction>)whiteSpaceCollapseFixedRestrictions
                                             .clone()));
        attrGroupAttrs.add(new Attribute("date", "date", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.DATE, true,
                                         (Set<XmlSchemaRestriction>)whiteSpaceCollapseFixedRestrictions
                                             .clone()));
        attrGroupAttrs.add(new Attribute("time", "time", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.TIME, true,
                                         (Set<XmlSchemaRestriction>)whiteSpaceCollapseFixedRestrictions
                                             .clone()));
        attrGroupAttrs.add(new Attribute("gYearMonth", "gYearMonth", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.YEARMONTH, true,
                                         (Set<XmlSchemaRestriction>)whiteSpaceCollapseFixedRestrictions
                                             .clone()));
        attrGroupAttrs.add(new Attribute("gYear", "gYear", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.YEAR, true,
                                         (Set<XmlSchemaRestriction>)whiteSpaceCollapseFixedRestrictions
                                             .clone()));
        attrGroupAttrs.add(new Attribute("gDay", "gDay", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.DAY, true,
                                         (Set<XmlSchemaRestriction>)whiteSpaceCollapseFixedRestrictions
                                             .clone()));
        attrGroupAttrs.add(new Attribute("gMonth", "gMonth", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.MONTH, true,
                                         (Set<XmlSchemaRestriction>)whiteSpaceCollapseFixedRestrictions
                                             .clone()));
        attrGroupAttrs.add(new Attribute("gMonthDay", "gMonthDay", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.MONTHDAY, true,
                                         (Set<XmlSchemaRestriction>)whiteSpaceCollapseFixedRestrictions
                                             .clone()));
        attrGroupAttrs.add(new Attribute("boolean", "boolean", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.BOOLEAN, true,
                                         (Set<XmlSchemaRestriction>)whiteSpaceCollapseFixedRestrictions
                                             .clone()));
        attrGroupAttrs.add(new Attribute("base64Binary", "base64Binary", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.BIN_BASE64, true,
                                         (Set<XmlSchemaRestriction>)whiteSpaceCollapseFixedRestrictions
                                             .clone()));
        attrGroupAttrs.add(new Attribute("hexBinary", "hexBinary", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.BIN_HEX, true,
                                         (Set<XmlSchemaRestriction>)whiteSpaceCollapseFixedRestrictions
                                             .clone()));
        attrGroupAttrs.add(new Attribute("float", "float", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.FLOAT, true,
                                         (Set<XmlSchemaRestriction>)whiteSpaceCollapseFixedRestrictions
                                             .clone()));
        attrGroupAttrs.add(new Attribute("double", "double", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.DOUBLE, true,
                                         (Set<XmlSchemaRestriction>)whiteSpaceCollapseFixedRestrictions
                                             .clone()));
        attrGroupAttrs.add(new Attribute("anyURI", "anyURI", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.ANYURI, true,
                                         (Set<XmlSchemaRestriction>)whiteSpaceCollapseFixedRestrictions
                                             .clone()));
        attrGroupAttrs.add(new Attribute("qname", "QName", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.QNAME, true,
                                         (Set<XmlSchemaRestriction>)whiteSpaceCollapseFixedRestrictions
                                             .clone()));

        HashSet<XmlSchemaRestriction> decimalFacets = (HashSet<XmlSchemaRestriction>)whiteSpaceCollapseFixedRestrictions
            .clone();
        decimalFacets.add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.DIGITS_FRACTION, new Integer(0),
                                                   false));

        attrGroupAttrs.add(new Attribute("decimal", null, XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.DECIMAL, true, decimalFacets));

        HashSet<XmlSchemaRestriction> integerFacets = (HashSet<XmlSchemaRestriction>)whiteSpaceCollapseFixedRestrictions
            .clone();
        integerFacets.add(new XmlSchemaRestriction(new XmlSchemaFractionDigitsFacet(new Integer(0), true)));
        integerFacets.add(new XmlSchemaRestriction(new XmlSchemaPatternFacet("[\\-+]?[0-9]+", false)));
        attrGroupAttrs.add(new Attribute("integer", "integer", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.DECIMAL, true, integerFacets));

        HashSet<XmlSchemaRestriction> nonPositiveIntegerFacets = (HashSet<XmlSchemaRestriction>)integerFacets
            .clone();
        nonPositiveIntegerFacets.add(new XmlSchemaRestriction(new XmlSchemaMaxInclusiveFacet(new Integer(0),
                                                                                             false)));
        attrGroupAttrs.add(new Attribute("nonPositiveInteger", "nonPositiveInteger",
                                         XmlSchemaTypeInfo.Type.ATOMIC, XmlSchemaBaseSimpleType.DECIMAL,
                                         true, nonPositiveIntegerFacets));

        HashSet<XmlSchemaRestriction> negativeIntegerFacets = (HashSet<XmlSchemaRestriction>)integerFacets
            .clone();
        negativeIntegerFacets.add(new XmlSchemaRestriction(new XmlSchemaMaxInclusiveFacet(new Integer(-1),
                                                                                          false)));
        attrGroupAttrs.add(new Attribute("negativeInteger", "negativeInteger", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.DECIMAL, true, negativeIntegerFacets));

        HashSet<XmlSchemaRestriction> longFacets = (HashSet<XmlSchemaRestriction>)integerFacets.clone();
        longFacets
            .add(new XmlSchemaRestriction(new XmlSchemaMinInclusiveFacet(new Long(-9223372036854775808L),
                                                                         false)));
        longFacets
            .add(new XmlSchemaRestriction(new XmlSchemaMaxInclusiveFacet(new Long(9223372036854775807L),
                                                                         false)));
        attrGroupAttrs.add(new Attribute("long", "long", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.DECIMAL, true, longFacets));

        HashSet<XmlSchemaRestriction> intFacets = (HashSet<XmlSchemaRestriction>)integerFacets.clone();
        intFacets
            .add(new XmlSchemaRestriction(new XmlSchemaMinInclusiveFacet(new Integer(-2147483648), false)));
        intFacets.add(new XmlSchemaRestriction(new XmlSchemaMaxInclusiveFacet(2147483647, false)));
        attrGroupAttrs.add(new Attribute("int", "int", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.DECIMAL, true, intFacets));

        HashSet<XmlSchemaRestriction> shortFacets = (HashSet<XmlSchemaRestriction>)integerFacets.clone();
        shortFacets.add(new XmlSchemaRestriction(new XmlSchemaMinInclusiveFacet(new Short((short)-32768),
                                                                                false)));
        shortFacets.add(new XmlSchemaRestriction(new XmlSchemaMaxInclusiveFacet(new Short((short)32767),
                                                                                false)));
        attrGroupAttrs.add(new Attribute("short", "short", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.DECIMAL, true, shortFacets));

        HashSet<XmlSchemaRestriction> byteFacets = (HashSet<XmlSchemaRestriction>)integerFacets.clone();
        byteFacets.add(new XmlSchemaRestriction(new XmlSchemaMinInclusiveFacet(new Byte((byte)-128), false)));
        byteFacets.add(new XmlSchemaRestriction(new XmlSchemaMaxInclusiveFacet(new Byte((byte)127), false)));
        attrGroupAttrs.add(new Attribute("byte", "byte", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.DECIMAL, true, byteFacets));

        HashSet<XmlSchemaRestriction> nonNegativeIntegerFacets = (HashSet<XmlSchemaRestriction>)integerFacets
            .clone();
        nonNegativeIntegerFacets.add(new XmlSchemaRestriction(new XmlSchemaMinInclusiveFacet(new Integer(0),
                                                                                             false)));
        attrGroupAttrs.add(new Attribute("nonNegativeInteger", "nonNegativeInteger",
                                         XmlSchemaTypeInfo.Type.ATOMIC, XmlSchemaBaseSimpleType.DECIMAL,
                                         true, nonNegativeIntegerFacets));

        HashSet<XmlSchemaRestriction> positiveIntegerFacets = (HashSet<XmlSchemaRestriction>)integerFacets
            .clone();
        positiveIntegerFacets.add(new XmlSchemaRestriction(new XmlSchemaMinInclusiveFacet(new Integer(1),
                                                                                          false)));
        attrGroupAttrs.add(new Attribute("positiveInteger", "positiveInteger", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.DECIMAL, true, positiveIntegerFacets));

        HashSet<XmlSchemaRestriction> unsignedLongFacets = (HashSet<XmlSchemaRestriction>)nonNegativeIntegerFacets
            .clone();
        unsignedLongFacets
            .add(new XmlSchemaRestriction(
                                          new XmlSchemaMaxInclusiveFacet(
                                                                         new BigInteger(
                                                                                        "18446744073709551615"),
                                                                         false)));
        attrGroupAttrs.add(new Attribute("unsignedLong", "unsignedLong", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.DECIMAL, true, unsignedLongFacets));

        HashSet<XmlSchemaRestriction> unsignedIntFacets = (HashSet<XmlSchemaRestriction>)nonNegativeIntegerFacets
            .clone();
        unsignedIntFacets.add(new XmlSchemaRestriction(new XmlSchemaMaxInclusiveFacet(new Long(4294967295L),
                                                                                      false)));
        attrGroupAttrs.add(new Attribute("unsignedInt", "unsignedInt", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.DECIMAL, true, unsignedIntFacets));

        HashSet<XmlSchemaRestriction> unsignedShortFacets = (HashSet<XmlSchemaRestriction>)nonNegativeIntegerFacets
            .clone();
        unsignedShortFacets.add(new XmlSchemaRestriction(new XmlSchemaMaxInclusiveFacet(new Integer(65535),
                                                                                        false)));
        attrGroupAttrs.add(new Attribute("unsignedShort", "unsignedShort", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.DECIMAL, true, unsignedShortFacets));

        HashSet<XmlSchemaRestriction> unsignedByteFacets = (HashSet<XmlSchemaRestriction>)nonNegativeIntegerFacets
            .clone();
        unsignedByteFacets.add(new XmlSchemaRestriction(new XmlSchemaMaxInclusiveFacet(new Short((short)255),
                                                                                       false)));
        attrGroupAttrs.add(new Attribute("unsignedByte", "unsignedByte", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.DECIMAL, true, unsignedByteFacets));

        attrGroupAttrs.add(new Attribute("unknown", null, null, null, true));

        HashSet<XmlSchemaRestriction> stringFacets = new HashSet<XmlSchemaRestriction>();
        stringFacets.add(new XmlSchemaRestriction(new XmlSchemaWhiteSpaceFacet("preserve", false)));
        attrGroupAttrs.add(new Attribute("string", "string", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.STRING, true, stringFacets));

        HashSet<XmlSchemaRestriction> normalizedStringFacets = new HashSet<XmlSchemaRestriction>();
        normalizedStringFacets.add(new XmlSchemaRestriction(new XmlSchemaWhiteSpaceFacet("replace", false)));
        attrGroupAttrs.add(new Attribute("normalizedString", "normalizedString",
                                         XmlSchemaTypeInfo.Type.ATOMIC, XmlSchemaBaseSimpleType.STRING, true,
                                         normalizedStringFacets));

        HashSet<XmlSchemaRestriction> tokenFacets = new HashSet<XmlSchemaRestriction>();
        tokenFacets.add(new XmlSchemaRestriction(new XmlSchemaWhiteSpaceFacet("collapse", false)));
        attrGroupAttrs.add(new Attribute("token", "token", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.STRING, true, tokenFacets));

        HashSet<XmlSchemaRestriction> languageFacets = (HashSet<XmlSchemaRestriction>)tokenFacets.clone();
        languageFacets
            .add(new XmlSchemaRestriction(new XmlSchemaPatternFacet("[a-zA-Z]{1,8}(-[a-zA-Z0-9]{1,8})*",
                                                                    false)));
        attrGroupAttrs.add(new Attribute("language", "language", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.STRING, true, languageFacets));

        HashSet<XmlSchemaRestriction> nmTokenFacets = (HashSet<XmlSchemaRestriction>)tokenFacets.clone();
        nmTokenFacets.add(new XmlSchemaRestriction(new XmlSchemaPatternFacet("\\c+", false)));
        attrGroupAttrs.add(new Attribute("nmtoken", "NMTOKEN", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.STRING, true, nmTokenFacets));

        HashSet<XmlSchemaRestriction> nameFacets = (HashSet<XmlSchemaRestriction>)tokenFacets.clone();
        nameFacets.add(new XmlSchemaRestriction(new XmlSchemaPatternFacet("\\i\\c*", false)));
        attrGroupAttrs.add(new Attribute("name", "Name", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.STRING, true, nameFacets));

        HashSet<XmlSchemaRestriction> ncNameFacets = (HashSet<XmlSchemaRestriction>)tokenFacets.clone();
        ncNameFacets.add(new XmlSchemaRestriction(new XmlSchemaPatternFacet("[\\i-[:]][\\c-[:]]*", false)));
        attrGroupAttrs.add(new Attribute("ncName", "NCName", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.STRING, true, ncNameFacets));

        attrGroupAttrs.add(new Attribute("id", "ID", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.STRING, true,
                                         (Set<XmlSchemaRestriction>)ncNameFacets.clone()));
        attrGroupAttrs.add(new Attribute("idref", "IDREF", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.STRING, true,
                                         (Set<XmlSchemaRestriction>)ncNameFacets.clone()));
        attrGroupAttrs.add(new Attribute("idrefs", "IDREFS", XmlSchemaTypeInfo.Type.LIST,
                                         XmlSchemaBaseSimpleType.STRING, true, null));
        attrGroupAttrs.add(new Attribute("entity", "ENTITY", XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.STRING, true,
                                         (Set<XmlSchemaRestriction>)ncNameFacets.clone()));
        attrGroupAttrs.add(new Attribute("entities", "ENTITIES", XmlSchemaTypeInfo.Type.LIST,
                                         XmlSchemaBaseSimpleType.STRING, true, null));
        attrGroupAttrs.add(new Attribute("nmtokens", "NMTOKENS", XmlSchemaTypeInfo.Type.LIST,
                                         XmlSchemaBaseSimpleType.STRING, true, null));

        HashSet<XmlSchemaRestriction> nonNullPrimitiveTypeFacets = new HashSet<XmlSchemaRestriction>(14);
        nonNullPrimitiveTypeFacets.add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.ENUMERATION,
                                                                "boolean", false));
        nonNullPrimitiveTypeFacets.add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.ENUMERATION, "int",
                                                                false));
        nonNullPrimitiveTypeFacets.add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.ENUMERATION,
                                                                "long", false));
        nonNullPrimitiveTypeFacets.add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.ENUMERATION,
                                                                "float", false));
        nonNullPrimitiveTypeFacets.add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.ENUMERATION,
                                                                "double", false));
        nonNullPrimitiveTypeFacets.add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.ENUMERATION,
                                                                "decimal", false));
        nonNullPrimitiveTypeFacets.add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.ENUMERATION,
                                                                "bytes", false));
        nonNullPrimitiveTypeFacets.add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.ENUMERATION,
                                                                "string", false));
        nonNullPrimitiveTypeFacets.add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.PATTERN, "\\c+",
                                                                false));
        nonNullPrimitiveTypeFacets.add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.WHITESPACE,
                                                                "collapse", false));

        XmlSchemaBaseSimpleType nonNullPrimitiveType = XmlSchemaBaseSimpleType.STRING;
        XmlSchemaBaseSimpleType primitiveType = XmlSchemaBaseSimpleType.STRING;

        HashSet<XmlSchemaRestriction> primitiveTypeFacets = new HashSet<XmlSchemaRestriction>(15);
        primitiveTypeFacets
            .add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.ENUMERATION, "null", false));
        primitiveTypeFacets.addAll(nonNullPrimitiveTypeFacets);

        LinkedList<StackEntry> stack = new LinkedList<StackEntry>();

        // Indentation follows tree.
        stack.add(new StackEntry(Type.ELEMENT, "root"));
        stack.add(new StackEntry(Type.SEQUENCE));
        stack.add(new StackEntry(Type.CHOICE, 0, Long.MAX_VALUE));
        stack.add(new StackEntry(Type.ELEMENT, "primitive", "primitiveType", primitiveType,
                                 (Set<XmlSchemaRestriction>)primitiveTypeFacets.clone()));
        stack.add(new StackEntry(Type.ELEMENT, "nonNullPrimitive", "nonNullPrimitiveType",
                                 nonNullPrimitiveType, (Set<XmlSchemaRestriction>)nonNullPrimitiveTypeFacets
                                     .clone()));
        stack.add(new StackEntry(Type.SUBSTITUTION_GROUP, "record"));
        stack.add(new StackEntry(Type.ELEMENT, "record", "recordType"));
        stack.add(new StackEntry(Type.SEQUENCE));
        stack.add(new StackEntry(Type.CHOICE, 0, Long.MAX_VALUE));
        /* 10 */stack.add(new StackEntry(Type.ELEMENT, "primitive", "primitiveType", primitiveType,
                                         (Set<XmlSchemaRestriction>)primitiveTypeFacets.clone()));
        stack.add(new StackEntry(Type.ELEMENT, "nonNullPrimitive", "nonNullPrimitiveType",
                                 nonNullPrimitiveType, (Set<XmlSchemaRestriction>)nonNullPrimitiveTypeFacets
                                     .clone()));
        stack.add(new StackEntry(Type.SUBSTITUTION_GROUP, "record"));
        stack.add(new StackEntry(Type.ELEMENT, "record", "recordType"));
        stack.add(new StackEntry(Type.ELEMENT, "map"));
        stack.add(new StackEntry(Type.SEQUENCE));
        stack.add(new StackEntry(Type.CHOICE, 0, Long.MAX_VALUE));
        stack.add(new StackEntry(Type.ELEMENT, "primitive", "primitiveType", primitiveType,
                                 (Set<XmlSchemaRestriction>)primitiveTypeFacets.clone()));
        stack.add(new StackEntry(Type.ELEMENT, "nonNullPrimitive", "nonNullPrimitiveType",
                                 nonNullPrimitiveType, (Set<XmlSchemaRestriction>)nonNullPrimitiveTypeFacets
                                     .clone()));
        stack.add(new StackEntry(Type.SUBSTITUTION_GROUP, "record"));
        /* 20 */stack.add(new StackEntry(Type.ELEMENT, "record", "recordType"));
        stack.add(new StackEntry(Type.ELEMENT, "map"));
        stack.add(new StackEntry(Type.ELEMENT, "list"));
        stack.add(new StackEntry(Type.CHOICE));
        stack.add(new StackEntry(Type.ELEMENT, "primitive", "primitiveType", primitiveType, 1, 100,
                                 (Set<XmlSchemaRestriction>)primitiveTypeFacets.clone()));
        stack.add(new StackEntry(Type.SUBSTITUTION_GROUP, "record", "recordType", 1, 100));
        stack.add(new StackEntry(Type.ELEMENT, "record", "recordType", 1, 1));
        stack.add(new StackEntry(Type.ELEMENT, "map"));
        stack.add(new StackEntry(Type.ELEMENT, "tuple"));
        stack.add(new StackEntry(Type.ALL));
        /* 30 */stack.add(new StackEntry(Type.ELEMENT, "primitive", "primitiveType", primitiveType,
                                         (Set<XmlSchemaRestriction>)primitiveTypeFacets.clone()));
        stack.add(new StackEntry(Type.ELEMENT, "nonNullPrimitive", "nonNullPrimitiveType",
                                 nonNullPrimitiveType, (Set<XmlSchemaRestriction>)nonNullPrimitiveTypeFacets
                                     .clone()));
        stack.add(new StackEntry(Type.SUBSTITUTION_GROUP, "record"));
        stack.add(new StackEntry(Type.ELEMENT, "record", "recordType"));
        stack.add(new StackEntry(Type.ELEMENT, "map"));
        stack.add(new StackEntry(Type.ELEMENT, "list"));
        stack.add(new StackEntry(Type.ELEMENT, "list"));
        stack.add(new StackEntry(Type.ELEMENT, "tuple"));
        stack.add(new StackEntry(Type.ELEMENT, "map"));
        stack.add(new StackEntry(Type.ELEMENT, "list"));
        stack.add(new StackEntry(Type.ELEMENT, "tuple"));

        HashMap<String, List<Attribute>> attributes = new HashMap<String, List<Attribute>>();
        attributes.put("root", attrGroupAttrs);

        HashSet<XmlSchemaRestriction> listAttrFacets = (HashSet<XmlSchemaRestriction>)nonNegativeIntegerFacets
            .clone();
        listAttrFacets.add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.EXCLUSIVE_MAX, 100, false));
        ArrayList<Attribute> listAttributes = new ArrayList<Attribute>(1);
        listAttributes.add(new Attribute("size", null, XmlSchemaTypeInfo.Type.ATOMIC,
                                         XmlSchemaBaseSimpleType.DECIMAL, true, listAttrFacets));
        attributes.put("list", listAttributes);

        HashSet<XmlSchemaRestriction> mapAttrFacets = (HashSet<XmlSchemaRestriction>)ncNameFacets.clone();
        mapAttrFacets.add(new XmlSchemaRestriction(XmlSchemaRestriction.Type.LENGTH_MIN, 1, false));
        ArrayList<Attribute> mapAttributes = new ArrayList<Attribute>(1);
        mapAttributes.add(new Attribute("id", null, XmlSchemaTypeInfo.Type.ATOMIC,
                                        XmlSchemaBaseSimpleType.STRING, mapAttrFacets));
        attributes.put("map", mapAttributes);

        // Compare against the actual.
        final Visitor visitor = new Visitor(stack, attributes);
        final int numEntries = stack.size();

        XmlSchemaCollection collection = null;
        FileReader fileReader = null;
        try {
            File file = UtilsForTests.buildFile("src", "test", "resources", "test_schema.xsd");
            fileReader = new FileReader(file);

            collection = new XmlSchemaCollection();
            collection.read(new StreamSource(fileReader, file.getAbsolutePath()));

        } finally {
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }

        XmlSchemaElement elem = getElementOf(collection, "root");
        XmlSchemaWalker walker = new XmlSchemaWalker(collection, visitor);
        try {
            walker.walk(elem);
        } catch (Exception e) {
            throw new IllegalStateException("Failed on stack entry " + (numEntries - stack.size()), e);
        }

        Assert.assertTrue(stack.isEmpty());
    }

    private static void checkFacets(String nextName, XmlSchemaTypeInfo typeInfo,
                                    Set<XmlSchemaRestriction> nextFacets) {
        if (typeInfo == null) {
            return;
        }

        final HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets = typeInfo.getFacets();

        if ((facets == null) && (nextFacets != null)) {
            throw new IllegalStateException("Expected " + nextFacets.size() + " facets for element \""
                                            + nextName + "\" but received null facets.");

        } else if ((facets != null) && facets.isEmpty() && (nextFacets != null) && !nextFacets.isEmpty()) {
            throw new IllegalStateException("Expected " + nextFacets.size() + " facets for element \""
                                            + nextName + "\" but found none.");

        } else if ((facets != null) && !facets.isEmpty() && (nextFacets != null) && nextFacets.isEmpty()) {

            throw new IllegalStateException("Element " + nextName + " has facets, but none were expected.");
        }

        if (facets != null) {
            for (Map.Entry<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facetsForType : facets
                .entrySet()) {

                for (XmlSchemaRestriction facet : facetsForType.getValue()) {
                    if (!nextFacets.remove(facet)) {
                        throw new IllegalStateException("Element \"" + nextName
                                                        + "\" has unexpected facet \"" + facet + "\".");
                    }
                }
            }
        }

    }

    private static XmlSchemaElement getElementOf(XmlSchemaCollection collection, String name) {

        XmlSchemaElement elem = null;
        for (XmlSchema schema : collection.getXmlSchemas()) {
            elem = schema.getElementByName(name);
            if (elem != null) {
                break;
            }
        }
        return elem;
    }
}
