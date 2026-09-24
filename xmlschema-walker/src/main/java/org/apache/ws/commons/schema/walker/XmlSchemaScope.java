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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAll;
import org.apache.ws.commons.schema.XmlSchemaAnyAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroup;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroupMember;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroupRef;
import org.apache.ws.commons.schema.XmlSchemaAttributeOrGroupRef;
import org.apache.ws.commons.schema.XmlSchemaChoice;
import org.apache.ws.commons.schema.XmlSchemaComplexContent;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexContentRestriction;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaContent;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaException;
import org.apache.ws.commons.schema.XmlSchemaFacet;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentExtension;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentRestriction;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeContent;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeList;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeUnion;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.XmlSchemaUse;
import org.apache.ws.commons.schema.utils.XmlSchemaNamed;

/**
 * The scope represents the set of types, attributes, and child groups &
 * elements that the current type represents.
 */
final class XmlSchemaScope {

    private SchemasByNamespace schemasByNamespace;
    private Map<QName, XmlSchemaScope> scopeCache;

    private XmlSchemaTypeInfo typeInfo;
    private HashMap<QName, XmlSchemaAttrInfo> attributes;
    private XmlSchemaParticle child;
    private XmlSchemaAnyAttribute anyAttr;
    private Set<QName> userRecognizedTypes;
    private Set<XmlSchemaType> typesInProgress;
    private int maxDepth;
    private final Set<QName> attributeGroupsInProgress = new HashSet<QName>();

    /**
     * Initialization of members to be filled in during the walk.
     */
    private XmlSchemaScope() {
        typeInfo = null;
        attributes = null;
        child = null;
        anyAttr = null;
    }

    private XmlSchemaScope(XmlSchemaScope child, XmlSchemaType type) {
        this();
        this.schemasByNamespace = child.schemasByNamespace;
        this.scopeCache = child.scopeCache;
        this.userRecognizedTypes = child.userRecognizedTypes;
        this.typesInProgress = child.typesInProgress;
        this.maxDepth = child.maxDepth;

        walkWithCycleCheck(type);
    }

    /**
     * Initializes a new {@link XmlSchemaScope} with a base
     * {@link XmlSchemaElement}. The element type and attributes will be
     * traversed, and attribute lists and element children will be retrieved.
     * Chains of type derivation and of attribute group references deeper
     * than <code>maxDepth</code> are rejected.
     */
    XmlSchemaScope(XmlSchemaType type, SchemasByNamespace xmlSchemasByNamespace,
                   Map<QName, XmlSchemaScope> scopeCache, Set<QName> userRecognizedTypes,
                   int maxDepth) {

        this();

        schemasByNamespace = xmlSchemasByNamespace;
        this.scopeCache = scopeCache;
        this.userRecognizedTypes = userRecognizedTypes;
        this.typesInProgress =
            Collections.newSetFromMap(new IdentityHashMap<XmlSchemaType, Boolean>());
        this.maxDepth = maxDepth;

        walkWithCycleCheck(type);
    }

    private void walkWithCycleCheck(XmlSchemaType type) {
        // Each level of derivation recurses; an acyclic chain can still
        // exhaust the thread stack.
        if (typesInProgress.size() >= maxDepth) {
            throw new XmlSchemaException("The type " + getName(type, "{Anonymous}")
                                         + " is derived through more than " + maxDepth
                                         + " levels of base types; refusing to walk it. The limit"
                                         + " may be changed with the "
                                         + XmlSchemaWalker.MAX_DEPTH_PROPERTY + " system property.");
        }
        if (!typesInProgress.add(type)) {
            throw new XmlSchemaException("Cyclic type derivation detected involving type "
                                         + getName(type, "{Anonymous}") + '.');
        }
        try {
            walk(type);
        } finally {
            typesInProgress.remove(type);
        }
    }

    /**
     * The type information of the value in scope.
     */
    XmlSchemaTypeInfo getTypeInfo() {
        return typeInfo;
    }

    /**
     * The attributes visible in the current scope.
     */
    Collection<XmlSchemaAttrInfo> getAttributesInScope() {
        if (attributes == null) {
            return null;
        }
        return attributes.values();
    }

    /**
     * If the value is represented by a particle, returns that particle.
     * Otherwise returns <code>null</code>.
     */
    XmlSchemaParticle getParticle() {
        return child;
    }

    /**
     * The wildcard attribute, if any.
     */
    XmlSchemaAnyAttribute getAnyAttribute() {
        return anyAttr;
    }

    /**
     * Resolves a named type reference that the schema requires to be a simple type. A reference
     * that does not resolve, or resolves to a complex type, is a defect in the schema: the core
     * parser does not check either, so both reach here.
     */
    private XmlSchemaSimpleType simpleTypeByName(QName typeName, String role, String owner) {
        final XmlSchemaType type =
            (typeName == null) ? null : schemasByNamespace.getTypeByName(typeName);
        if (type == null) {
            throw new XmlSchemaException("The " + role + " " + owner + " (" + typeName
                                         + ") does not resolve to a type in this collection.");
        }
        if (!(type instanceof XmlSchemaSimpleType)) {
            throw new XmlSchemaException("The " + role + " " + owner + " (" + typeName
                                         + ") resolves to a complex type; a simple type is required.");
        }
        return (XmlSchemaSimpleType)type;
    }

    private void walk(XmlSchemaType type) {
        if (type instanceof XmlSchemaSimpleType) {
            walk((XmlSchemaSimpleType)type);
        } else if (type instanceof XmlSchemaComplexType) {
            walk((XmlSchemaComplexType)type);
        } else {
            throw new IllegalArgumentException("Unrecognized XmlSchemaType of type "
                                               + type.getClass().getName());
        }
    }

    private void walk(XmlSchemaSimpleType simpleType) {
        XmlSchemaSimpleTypeContent content = simpleType.getContent();

        if (content == null) {
            /*
             * Only anyType contains no content. We reached the root of the type
             * hierarchy.
             */
            typeInfo = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.ANYTYPE);

        } else if (content instanceof XmlSchemaSimpleTypeList) {
            XmlSchemaSimpleTypeList list = (XmlSchemaSimpleTypeList)content;
            XmlSchemaSimpleType listType = list.getItemType();
            if (listType == null) {
                listType = simpleTypeByName(list.getItemTypeName(), "item type of list",
                                            getName(simpleType, "{Anonymous List Type}"));
            }

            XmlSchemaScope parentScope = getScope(listType);

            switch (parentScope.getTypeInfo().getType()) {
            case UNION:
            case ATOMIC:
                break;
            default:
                throw new XmlSchemaException("The list "
                                             + getName(simpleType, "{Anonymous List Type}")
                                             + " has an item type of "
                                             + parentScope.getTypeInfo().getType()
                                             + "; a list item must be atomic or a union.");
            }

            typeInfo = new XmlSchemaTypeInfo(parentScope.getTypeInfo());

        } else if (content instanceof XmlSchemaSimpleTypeUnion) {
            XmlSchemaSimpleTypeUnion union = (XmlSchemaSimpleTypeUnion)content;
            QName[] namedBaseTypes = union.getMemberTypesQNames();
            List<XmlSchemaSimpleType> baseTypes = union.getBaseTypes();

            if (namedBaseTypes != null) {
                if (baseTypes == null) {
                    baseTypes = new ArrayList<XmlSchemaSimpleType>(namedBaseTypes.length);
                }

                for (QName namedBaseType : namedBaseTypes) {
                    baseTypes.add(simpleTypeByName(namedBaseType, "member type of union",
                                                   getName(simpleType, "{Anonymous Union Type}")));
                }
            }

            /*
             * baseTypes cannot be null at this point; there must be a union of
             * types.
             */
            if ((baseTypes == null) || baseTypes.isEmpty()) {
                throw new XmlSchemaException("The union "
                                             + getName(simpleType, "{Anonymous Union Type}")
                                             + " has no member types.");
            }

            List<XmlSchemaTypeInfo> childTypes = new ArrayList<XmlSchemaTypeInfo>(baseTypes.size());

            for (XmlSchemaSimpleType baseType : baseTypes) {
                XmlSchemaScope parentScope = getScope(baseType);
                if (parentScope.getTypeInfo().getType().equals(XmlSchemaTypeInfo.Type.UNION)) {
                    childTypes.addAll(parentScope.getTypeInfo().getChildTypes());
                } else {
                    childTypes.add(parentScope.getTypeInfo());
                }
            }

            typeInfo = new XmlSchemaTypeInfo(childTypes);

        } else if (content instanceof XmlSchemaSimpleTypeRestriction) {
            final XmlSchemaSimpleTypeRestriction restr = (XmlSchemaSimpleTypeRestriction)content;

            final List<XmlSchemaFacet> facets = restr.getFacets();

            XmlSchemaTypeInfo parentTypeInfo = null;

            if (XmlSchemaBaseSimpleType.isBaseSimpleType(simpleType.getQName())) {
                // If this is a base simple type, use it!
                typeInfo = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.getBaseSimpleTypeFor(simpleType
                    .getQName()), mergeFacets(null, facets));

            } else {
                XmlSchemaSimpleType baseType = restr.getBaseType();
                if (baseType == null) {
                    baseType = simpleTypeByName(restr.getBaseTypeName(), "base type of restriction",
                                                getName(simpleType, "{Anonymous Simple Type}"));
                }

                if (baseType != null) {
                    final XmlSchemaScope parentScope = getScope(baseType);

                    /*
                     * We need to track the original type as well as the set of
                     * facets imposed on that type. Once the recursion ends, and
                     * we make it all the way back to the first scope, the user
                     * of this type info will know the derived type and all of
                     * its imposed facets. Unions can restrict unions, lists can
                     * restrict lists, and atomic types restrict other atomic
                     * types. We need to follow all of these too.
                     */
                    parentTypeInfo = parentScope.getTypeInfo();

                    HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> mergedFacets = mergeFacets(parentTypeInfo
                                                                                                                  .getFacets(),
                                                                                                              facets);

                    typeInfo = restrictTypeInfo(parentTypeInfo, mergedFacets);

                } else {
                    throw new IllegalArgumentException("Unrecognized base type for "
                                                       + getName(simpleType, "{Anonymous Simple Type}"));
                }
            }

            typeInfo.setUserRecognizedType(getUserRecognizedType(simpleType.getQName(), parentTypeInfo));

        } else {
            throw new IllegalArgumentException("XmlSchemaSimpleType "
                                               + getName(simpleType, "{Anonymous Simple Type}")
                                               + "contains unrecognized XmlSchemaSimpleTypeContent "
                                               + content.getClass().getName());
        }
    }

    private void walk(XmlSchemaComplexType complexType) {
        XmlSchemaContent complexContent = (complexType.getContentModel() != null) ? complexType
            .getContentModel().getContent() : null;

        /*
         * Process the complex type extensions and restrictions. If there aren't
         * any, the content is be defined by the particle.
         */
        if (complexContent != null) {
            boolean isMixed = false;
            if (complexType.isMixed()) {
                isMixed = complexType.isMixed();
            } else if (complexType.getContentModel() instanceof XmlSchemaComplexContent) {
                isMixed = ((XmlSchemaComplexContent)complexType.getContentModel()).isMixed();
            }

            walk(isMixed, complexContent);

            final QName userRecognizedType =
                getUserRecognizedType(complexType.getQName(), null);
            if (userRecognizedType != null) {
              typeInfo.setUserRecognizedType(userRecognizedType);
            }

        } else {
            child = complexType.getParticle();
            attributes = createAttributeMap(complexType.getAttributes());
            anyAttr = complexType.getAnyAttribute();
            typeInfo = new XmlSchemaTypeInfo(complexType.isMixed());
        }
    }

    private void walk(boolean isMixed, XmlSchemaContent content) {
        if (content instanceof XmlSchemaComplexContentExtension) {
            XmlSchemaComplexContentExtension ext = (XmlSchemaComplexContentExtension)content;

            XmlSchemaType baseType = schemasByNamespace.getTypeByName(ext.getBaseTypeName());

            XmlSchemaParticle baseParticle = null;
            XmlSchemaAnyAttribute baseAnyAttr = null;
            XmlSchemaScope parentScope = null;

            if (baseType != null) {
                /*
                 * Complex content extensions add attributes and elements in
                 * addition to what was retrieved from the parent. Since there
                 * will be no collisions, it is safe to perform a straight add.
                 */
                parentScope = getScope(baseType);
                attributes = createAttributeMap(ext.getAttributes());

                if (attributes == null) {
                    attributes = parentScope.attributes;
                } else if (parentScope.attributes != null) {
                    attributes.putAll(parentScope.attributes);
                }

                baseParticle = parentScope.getParticle();
                baseAnyAttr = parentScope.anyAttr;
            }

            /*
             * An extension of a complex type is equivalent to creating a
             * sequence of two particles: the parent particle followed by the
             * child particle.
             */
            if (ext.getParticle() == null) {
                child = baseParticle;
            } else if (baseParticle == null) {
                child = ext.getParticle();
            } else if (!(baseParticle instanceof XmlSchemaSequenceMember)
                       || !(ext.getParticle() instanceof XmlSchemaSequenceMember)) {
                // Only an xs:all is not a sequence member, and XML Schema 1.0
                // does not allow one to be combined with any other particle.
                // An empty particle adds no content, though, so an xs:all
                // alongside one is the whole content model.
                if (isEmptyParticle(ext.getParticle())) {
                    child = baseParticle;
                } else if (isEmptyParticle(baseParticle)) {
                    child = ext.getParticle();
                } else {
                    throw new XmlSchemaException("An extension of " + ext.getBaseTypeName()
                                                 + " adds content to it, but one of the two content"
                                                 + " models is an xs:all group, which cannot be"
                                                 + " combined with other particles.");
                }
            } else {
                XmlSchemaSequence seq = new XmlSchemaSequence();
                seq.getItems().add((XmlSchemaSequenceMember)baseParticle);
                seq.getItems().add((XmlSchemaSequenceMember)ext.getParticle());
                child = seq;
            }

            /*
             * An extension of an anyAttribute means the child defines the
             * processContents field, while a union of all namespaces between
             * the parent and child is taken.
             */
            if (baseAnyAttr == null) {
                anyAttr = ext.getAnyAttribute();
            } else if (ext.getAnyAttribute() == null) {
                anyAttr = baseAnyAttr;
            } else {
                // An absent namespace attribute means ##any, and a union with
                // ##any is ##any.
                final String baseNamespace = baseAnyAttr.getNamespace();
                final String childNamespace = ext.getAnyAttribute().getNamespace();
                final boolean unionIsAny = isAnyNamespace(baseNamespace) || isAnyNamespace(childNamespace);
                String[] baseNamespaces = unionIsAny ? new String[0] : baseNamespace.split(" ");
                String[] childNamespaces = unionIsAny ? new String[0] : childNamespace.split(" ");

                HashSet<String> namespaces = new HashSet<String>();
                for (String baseNs : baseNamespaces) {
                    if (baseNs.length() > 0) {
                        namespaces.add(baseNs);
                    }
                }
                for (String childNs : childNamespaces) {
                    if (childNs.length() > 0) {
                        namespaces.add(childNs);
                    }
                }

                StringBuilder nsAsString = new StringBuilder();
                for (String namespace : namespaces) {
                    nsAsString.append(namespace).append(' ');
                }

                anyAttr = new XmlSchemaAnyAttribute();
                anyAttr.setNamespace(unionIsAny ? "##any" : nsAsString.toString());
                anyAttr.setProcessContent(ext.getAnyAttribute().getProcessContent());
                anyAttr.setAnnotation(ext.getAnyAttribute().getAnnotation());
                anyAttr.setId(ext.getAnyAttribute().getId());
                anyAttr.setLineNumber(ext.getAnyAttribute().getLineNumber());
                anyAttr.setLinePosition(ext.getAnyAttribute().getLinePosition());
                anyAttr.setMetaInfoMap(ext.getAnyAttribute().getMetaInfoMap());
                anyAttr.setSourceURI(ext.getAnyAttribute().getSourceURI());
                anyAttr.setUnhandledAttributes(ext.getUnhandledAttributes());
            }

            final XmlSchemaTypeInfo parentTypeInfo = (parentScope == null) ? null : parentScope.getTypeInfo();

            if ((parentTypeInfo != null) && !parentTypeInfo.getType().equals(XmlSchemaTypeInfo.Type.COMPLEX)) {
                typeInfo = parentScope.getTypeInfo();
            } else {
                typeInfo = new XmlSchemaTypeInfo(isMixed);
            }

        } else if (content instanceof XmlSchemaComplexContentRestriction) {
            final XmlSchemaComplexContentRestriction rstr = (XmlSchemaComplexContentRestriction)content;

            final XmlSchemaType baseType = schemasByNamespace.getTypeByName(rstr.getBaseTypeName());

            XmlSchemaScope parentScope = null;
            if (baseType != null) {
                parentScope = getScope(baseType);

                attributes = mergeAttributes(parentScope.attributes, createAttributeMap(rstr.getAttributes()));

                child = parentScope.getParticle();
            }

            /*
             * There is no inheritance when restricting particles. If the schema
             * writer wishes to include elements in the parent type, (s)he must
             * redefine them in the child.
             */
            if (rstr.getParticle() != null) {
                child = rstr.getParticle();
            }

            /*
             * There is no inheritance when restricting attribute wildcards. The
             * only requirement is that the namespaces of the restricted type is
             * a subset of the namespaces of the base type. This will not be
             * checked here (all schemas are assumed correct).
             */
            anyAttr = rstr.getAnyAttribute();

            final XmlSchemaTypeInfo parentTypeInfo = (parentScope == null) ? null : parentScope.getTypeInfo();

            if ((parentTypeInfo != null) && !parentTypeInfo.getType().equals(XmlSchemaTypeInfo.Type.COMPLEX)) {
                typeInfo = parentTypeInfo;
            } else {
                typeInfo = new XmlSchemaTypeInfo(isMixed);
            }

        } else if (content instanceof XmlSchemaSimpleContentExtension) {
            XmlSchemaSimpleContentExtension ext = (XmlSchemaSimpleContentExtension)content;
            attributes = createAttributeMap(ext.getAttributes());

            XmlSchemaType baseType = schemasByNamespace.getTypeByName(ext.getBaseTypeName());

            if (baseType != null) {
                final XmlSchemaScope parentScope = getScope(baseType);
                typeInfo = parentScope.getTypeInfo();

                if (attributes == null) {
                    attributes = parentScope.attributes;
                } else if (parentScope.attributes != null) {
                    attributes.putAll(parentScope.attributes);
                }
            }

            anyAttr = ext.getAnyAttribute();

        } else if (content instanceof XmlSchemaSimpleContentRestriction) {
            XmlSchemaSimpleContentRestriction rstr = (XmlSchemaSimpleContentRestriction)content;
            attributes = createAttributeMap(rstr.getAttributes());

            XmlSchemaType baseType = null;
            if (rstr.getBaseType() != null) {
                baseType = rstr.getBaseType();
            } else {
                baseType = schemasByNamespace.getTypeByName(rstr.getBaseTypeName());
            }

            if (baseType != null) {
                XmlSchemaScope parentScope = getScope(baseType);
                typeInfo = restrictTypeInfo(parentScope.getTypeInfo(),
                                            mergeFacets(parentScope.getTypeInfo().getFacets(),
                                                        rstr.getFacets()));

                attributes = mergeAttributes(parentScope.attributes, attributes);
            }

            anyAttr = rstr.getAnyAttribute();
        }
    }

    private ArrayList<XmlSchemaAttrInfo> getAttributesOf(XmlSchemaAttributeGroupRef groupRef) {

        XmlSchemaAttributeGroup attrGroup = groupRef.getRef().getTarget();
        if (attrGroup == null) {
            attrGroup = schemasByNamespace.getAttributeGroupByName(groupRef.getTargetQName());
        }

        final QName groupName = groupRef.getTargetQName();
        if (attrGroup == null) {
            throw new XmlSchemaException("The attribute group reference " + groupName
                                         + " does not resolve to an attribute group in this collection.");
        }
        if (attributeGroupsInProgress.size() >= maxDepth) {
            throw new XmlSchemaException("The attribute group reference " + groupName
                                         + " is nested more than " + maxDepth
                                         + " levels deep; refusing to walk it. The limit may be"
                                         + " changed with the " + XmlSchemaWalker.MAX_DEPTH_PROPERTY
                                         + " system property.");
        }
        if ((groupName != null) && !attributeGroupsInProgress.add(groupName)) {
            throw new XmlSchemaException("Cyclic attribute group reference detected involving "
                                         + groupName + '.');
        }
        try {
            return getAttributesOf(attrGroup);
        } finally {
            if (groupName != null) {
                attributeGroupsInProgress.remove(groupName);
            }
        }
    }

    private ArrayList<XmlSchemaAttrInfo> getAttributesOf(XmlSchemaAttributeGroup attrGroup) {

        ArrayList<XmlSchemaAttrInfo> attrs = new ArrayList<XmlSchemaAttrInfo>(attrGroup.getAttributes()
            .size());

        for (XmlSchemaAttributeGroupMember member : attrGroup.getAttributes()) {
            if (member instanceof XmlSchemaAttribute) {
                attrs.add(getAttribute((XmlSchemaAttribute)member, false));

            } else if (member instanceof XmlSchemaAttributeGroup) {
                attrs.addAll(getAttributesOf((XmlSchemaAttributeGroup)member));

            } else if (member instanceof XmlSchemaAttributeGroupRef) {
                attrs.addAll(getAttributesOf((XmlSchemaAttributeGroupRef)member));

            } else {
                throw new IllegalArgumentException("Attribute Group "
                                                   + getName(attrGroup, "{Anonymous Attribute Group}")
                                                   + " contains unrecognized attribute group memeber type "
                                                   + member.getClass().getName());
            }
        }

        return attrs;
    }

    private XmlSchemaAttrInfo getAttribute(XmlSchemaAttribute attribute, boolean forceCopy) {

        if (!attribute.isRef() && (attribute.getSchemaType() != null) && !forceCopy) {

            if (attribute.getUse().equals(XmlSchemaUse.NONE)) {
                attribute.setUse(XmlSchemaUse.OPTIONAL);
            }

            return new XmlSchemaAttrInfo(attribute);
        }

        XmlSchemaAttribute globalAttr = null;
        QName attrQName = null;

        if (attribute.isRef()) {
            attrQName = attribute.getRefBase().getTargetQName();
        } else {
            attrQName = attribute.getQName();
        }

        if (!attribute.isRef() && (forceCopy || (attribute.getSchemaType() == null))) {
            // If we are forcing a copy, there is no reference to follow.
            globalAttr = attribute;
        } else {
            if (attribute.getRef().getTarget() != null) {
                globalAttr = attribute.getRef().getTarget();
            } else {
                globalAttr = schemasByNamespace.getAttributeByName(attrQName);
            }
        }

        if (globalAttr == null) {
            throw new XmlSchemaException("The attribute reference " + attrQName
                                         + " does not resolve to an attribute in this collection.");
        }

        XmlSchemaSimpleType schemaType = globalAttr.getSchemaType();
        if (schemaType == null) {
            final QName typeQName = globalAttr.getSchemaTypeName();
            if (typeQName != null) {
                schemaType = simpleTypeByName(typeQName, "type of attribute",
                                              String.valueOf(globalAttr.getQName()));
            }
        }

        /*
         * The attribute reference defines the attribute use and overrides the
         * ID, default, and fixed fields. Everything else is defined by the
         * global attribute.
         */
        String fixedValue = attribute.getFixedValue();
        if ((fixedValue != null) && (attribute != globalAttr)) {
            fixedValue = globalAttr.getFixedValue();
        }

        String defaultValue = attribute.getDefaultValue();
        if ((defaultValue == null) && (fixedValue == null) && (attribute != globalAttr)) {
            defaultValue = globalAttr.getDefaultValue();
        }

        String id = attribute.getId();
        if ((id == null) && (attribute != globalAttr)) {
            id = globalAttr.getId();
        }

        XmlSchemaUse attrUsage = attribute.getUse();
        if (attrUsage.equals(XmlSchemaUse.NONE)) {
            attrUsage = XmlSchemaUse.OPTIONAL;
        }

        XmlSchema schema = schemasByNamespace.getSchemaDefiningAttribute(attrQName);
        if (schema == null) {
            // TODO: is this correct? The previous code used whichever schema was stored in the schemasByNamespace HashMap
            // for the attribute's namespace.
            schema = attribute.getParent();
        }
        final XmlSchemaAttribute copy = new XmlSchemaAttribute(schema, false);
        copy.setName(globalAttr.getName());

        copy.setAnnotation(globalAttr.getAnnotation());
        copy.setDefaultValue(defaultValue);
        copy.setFixedValue(fixedValue);
        copy.setForm(globalAttr.getForm());
        copy.setId(id);
        copy.setLineNumber(attribute.getLineNumber());
        copy.setLinePosition(attribute.getLinePosition());
        copy.setMetaInfoMap(globalAttr.getMetaInfoMap());
        copy.setSchemaType(schemaType);
        copy.setSchemaTypeName(globalAttr.getSchemaTypeName());
        copy.setSourceURI(globalAttr.getSourceURI());
        copy.setUnhandledAttributes(globalAttr.getUnhandledAttributes());
        copy.setUse(attrUsage);

        return new XmlSchemaAttrInfo(copy, globalAttr.isTopLevel());
    }

    private HashMap<QName, XmlSchemaAttrInfo> createAttributeMap(Collection<? extends XmlSchemaAttributeOrGroupRef> attrs) {

        if ((attrs == null) || attrs.isEmpty()) {
            return null;
        }

        HashMap<QName, XmlSchemaAttrInfo> attributes = new HashMap<QName, XmlSchemaAttrInfo>();

        for (XmlSchemaAttributeOrGroupRef attr : attrs) {

            if (attr instanceof XmlSchemaAttribute) {
                XmlSchemaAttrInfo attribute = getAttribute((XmlSchemaAttribute)attr, false);

                attributes.put(attribute.getAttribute().getQName(), attribute);

            } else if (attr instanceof XmlSchemaAttributeGroupRef) {
                final List<XmlSchemaAttrInfo> attrList = getAttributesOf((XmlSchemaAttributeGroupRef)attr);

                for (XmlSchemaAttrInfo attribute : attrList) {
                    attributes.put(attribute.getAttribute().getQName(), attribute);
                }
            }
        }

        return attributes;
    }

    private HashMap<QName, XmlSchemaAttrInfo> mergeAttributes(HashMap<QName, XmlSchemaAttrInfo> parentAttrs,
                                                              HashMap<QName, XmlSchemaAttrInfo> childAttrs) {

        if ((parentAttrs == null) || parentAttrs.isEmpty()) {
            return childAttrs;
        } else if ((childAttrs == null) || childAttrs.isEmpty()) {
            return parentAttrs;
        }

        HashMap<QName, XmlSchemaAttrInfo> newAttrs = new HashMap<QName, XmlSchemaAttrInfo>(parentAttrs);

        /*
         * Child attributes inherit all parent attributes, but may change the
         * type, usage, default value, or fixed value.
         */
        for (Map.Entry<QName, XmlSchemaAttrInfo> parentAttrEntry : parentAttrs.entrySet()) {

            XmlSchemaAttrInfo parentAttr = parentAttrEntry.getValue();
            XmlSchemaAttrInfo childAttr = childAttrs.get(parentAttrEntry.getKey());
            if (childAttr != null) {
                XmlSchemaAttrInfo newAttr = getAttribute(parentAttr.getAttribute(), true);

                if (childAttr.getAttribute().getSchemaType() != null) {
                    newAttr.getAttribute().setSchemaType(childAttr.getAttribute().getSchemaType());
                }

                if (childAttr.getAttribute().getUse() != XmlSchemaUse.NONE) {
                    newAttr.getAttribute().setUse(childAttr.getAttribute().getUse());
                }

                // Attribute values may be defaulted or fixed, but not both.
                if (childAttr.getAttribute().getDefaultValue() != null) {
                    newAttr.getAttribute().setDefaultValue(childAttr.getAttribute().getDefaultValue());
                    newAttr.getAttribute().setFixedValue(null);

                } else if (childAttr.getAttribute().getFixedValue() != null) {
                    newAttr.getAttribute().setFixedValue(childAttr.getAttribute().getFixedValue());
                    newAttr.getAttribute().setDefaultValue(null);
                }

                newAttrs.put(newAttr.getAttribute().getQName(), newAttr);
            }
        }

        return newAttrs;
    }

    private XmlSchemaScope getScope(XmlSchemaType type) {
        if ((type.getQName() != null) && scopeCache.containsKey(type.getQName())) {
            return scopeCache.get(type.getQName());
        } else {
            XmlSchemaScope scope = new XmlSchemaScope(this, type);
            if (type.getQName() != null) {
                scopeCache.put(type.getQName(), scope);
            }
            return scope;
        }
    }

    private QName getUserRecognizedType(QName simpleType, XmlSchemaTypeInfo parent) {

        if (userRecognizedTypes == null) {
            return null;
        } else if (simpleType == null) {
            return (parent == null) ? null : parent.getUserRecognizedType();

        } else if (userRecognizedTypes.contains(simpleType)) {
            return simpleType;
        }

        if (XmlSchemaBaseSimpleType.isBaseSimpleType(simpleType)) {

            boolean checkAnyType = true;
            boolean checkAnySimpleType = true;
            switch (XmlSchemaBaseSimpleType.getBaseSimpleTypeFor(simpleType)) {
            case ANYTYPE:
                checkAnyType = false;
                break;
            case ANYSIMPLETYPE:
                checkAnySimpleType = false;
                break;
            default:
            }

            if (checkAnySimpleType) {
                final QName anySimpleType = XmlSchemaBaseSimpleType.ANYSIMPLETYPE.getQName();
                if (userRecognizedTypes.contains(anySimpleType)) {
                    return anySimpleType;
                }
            }

            if (checkAnyType) {
                final QName anyType = XmlSchemaBaseSimpleType.ANYTYPE.getQName();
                if (userRecognizedTypes.contains(anyType)) {
                    return anyType;
                }
            }
        }

        return (parent == null) ? null : parent.getUserRecognizedType();
    }

    /**
     * Whether a particle can match nothing at all: one that may not occur, an
     * xs:all or xs:sequence with no particles, or an optional xs:choice with
     * none. These are the particles XML Schema treats as empty content when it
     * builds the content model of an extension.
     */
    private static boolean isEmptyParticle(XmlSchemaParticle particle) {
        if (particle.getMaxOccurs() == 0) {
            return true;
        }
        if (particle instanceof XmlSchemaAll) {
            return ((XmlSchemaAll)particle).getItems().isEmpty();
        }
        if (particle instanceof XmlSchemaSequence) {
            return ((XmlSchemaSequence)particle).getItems().isEmpty();
        }
        if (particle instanceof XmlSchemaChoice) {
            return ((XmlSchemaChoice)particle).getItems().isEmpty() && particle.getMinOccurs() == 0;
        }
        return false;
    }

    private static boolean isAnyNamespace(String namespace) {
        return (namespace == null) || "##any".equals(namespace.trim());
    }

    private static String getName(XmlSchemaNamed name, String defaultName) {
        if (name.isAnonymous()) {
            return defaultName;
        } else {
            return name.getName();
        }
    }

    private static HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> mergeFacets(HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> parentFacets,
                                                                                              List<XmlSchemaFacet> child) {

        if ((child == null) || child.isEmpty()) {
            return parentFacets;
        }

        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> childFacets = new HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>>(
                                                                                                                                                        child
                                                                                                                                                            .size());

        for (XmlSchemaFacet facet : child) {
            XmlSchemaRestriction rstr = new XmlSchemaRestriction(facet);
            List<XmlSchemaRestriction> rstrList = childFacets.get(rstr.getType());
            if (rstrList == null) {
                // Only enumerations may have more than one value.
                if (rstr.getType() == XmlSchemaRestriction.Type.ENUMERATION) {
                    rstrList = new ArrayList<XmlSchemaRestriction>(5);
                } else {
                    rstrList = new ArrayList<XmlSchemaRestriction>(1);
                }
                childFacets.put(rstr.getType(), rstrList);
            }
            rstrList.add(rstr);
        }

        if (parentFacets == null) {
            return childFacets;
        }

        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> mergedFacets
            = new HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>>(parentFacets);

        // Child facets override parent facets
        for (Map.Entry<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> rstrEntry : childFacets
            .entrySet()) {

            mergedFacets.put(rstrEntry.getKey(), rstrEntry.getValue());
        }

        return mergedFacets;
    }

    private static XmlSchemaTypeInfo restrictTypeInfo(XmlSchemaTypeInfo parentTypeInfo,
                                                      HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets) {

        XmlSchemaTypeInfo typeInfo = null;

        switch (parentTypeInfo.getType()) {
        case LIST:
            typeInfo = new XmlSchemaTypeInfo(parentTypeInfo.getChildTypes().get(0), facets);
            break;
        case UNION:
            typeInfo = new XmlSchemaTypeInfo(parentTypeInfo.getChildTypes(), facets);
            break;
        case ATOMIC:
            typeInfo = new XmlSchemaTypeInfo(parentTypeInfo.getBaseType(), facets);
            break;
        default:
            throw new XmlSchemaException("Cannot restrict on a " + parentTypeInfo.getType() + " type.");
        }

        if (parentTypeInfo.getUserRecognizedType() != null) {
            typeInfo.setUserRecognizedType(parentTypeInfo.getUserRecognizedType());
        }

        return typeInfo;
    }
}
