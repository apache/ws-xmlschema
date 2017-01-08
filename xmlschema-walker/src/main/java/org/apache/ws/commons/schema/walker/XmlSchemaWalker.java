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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAll;
import org.apache.ws.commons.schema.XmlSchemaAllMember;
import org.apache.ws.commons.schema.XmlSchemaAny;
import org.apache.ws.commons.schema.XmlSchemaChoice;
import org.apache.ws.commons.schema.XmlSchemaChoiceMember;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaGroup;
import org.apache.ws.commons.schema.XmlSchemaGroupParticle;
import org.apache.ws.commons.schema.XmlSchemaGroupRef;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;
import org.apache.ws.commons.schema.XmlSchemaType;

/**
 * Walks an {@link XmlSchema} from a starting {@link XmlSchemaElement},
 * notifying attached visitors as it descends.
 */
public final class XmlSchemaWalker {

    private Set<QName> userRecognizedTypes;

    private final XmlSchemaCollection schemas;
    private final ArrayList<XmlSchemaVisitor> visitors;
    private final Map<QName, List<XmlSchemaElement>> elemsBySubstGroup;
    private final Map<String, XmlSchema> schemasByNamespace;
    private final Map<QName, XmlSchemaScope> scopeCache;
    private final Set<QName> visitedElements;

    /**
     * Initializes the {@link XmlSchemaWalker} with the
     * {@link XmlScheamCollection} to reference when following an
     * {@link XmlSchemaElement}.
     */
    public XmlSchemaWalker(XmlSchemaCollection xmlSchemas) {
        if (xmlSchemas == null) {
            throw new IllegalArgumentException("Input XmlSchemaCollection cannot be null.");
        }

        schemas = xmlSchemas;
        visitors = new ArrayList<XmlSchemaVisitor>(1);

        schemasByNamespace = new HashMap<String, XmlSchema>();
        elemsBySubstGroup = new HashMap<QName, List<XmlSchemaElement>>();

        for (XmlSchema schema : schemas.getXmlSchemas()) {
            schemasByNamespace.put(schema.getTargetNamespace(), schema);

            for (XmlSchemaElement elem : schema.getElements().values()) {
                if (elem.getSubstitutionGroup() != null) {
                    List<XmlSchemaElement> elems = elemsBySubstGroup.get(elem.getSubstitutionGroup());
                    if (elems == null) {
                        elems = new ArrayList<XmlSchemaElement>();
                        elemsBySubstGroup.put(elem.getSubstitutionGroup(), elems);
                    }
                    elems.add(elem);
                }
            }
        }

        scopeCache = new HashMap<QName, XmlSchemaScope>();
        visitedElements = new java.util.HashSet<QName>();
        userRecognizedTypes = null;
    }

    /**
     * Initializes the <code>XmlSchemaWalker</code> with an
     * {@link XmlSchemaVisitor} to notify as the schema is walked.
     * <p>
     * (Other visitors may continue to be added after this one.)
     * </p>
     *
     * @param xmlSchemas The set of schemas to walk.
     * @param visitor The visitor to visit during the walk.
     */
    public XmlSchemaWalker(XmlSchemaCollection xmlSchemas, XmlSchemaVisitor visitor) {

        this(xmlSchemas);
        if (visitor != null) {
            visitors.add(visitor);
        }
    }

    /**
     * Adds a new visitor to be notified as the XML Schemas are walked.
     *
     * @param visitor The visitor to be notified.
     * @return This <code>XmlSchemaWalker</code> instance for method chaining.
     */
    public XmlSchemaWalker addVisitor(XmlSchemaVisitor visitor) {
        visitors.add(visitor);
        return this;
    }

    /**
     * Removes the visitor to be notified as the XML Schemas are walked.
     *
     * @param visitor The visitor to remove.
     * @return This <code>XmlSchemaWalker</code> instance for method chaining.
     */
    public XmlSchemaWalker removeVisitor(XmlSchemaVisitor visitor) {
        if (visitor != null) {
            visitors.remove(visitor);
        }
        return this;
    }

    /**
     * Clears the internal state in preparation for another walk through the
     * schema.
     */
    public void clear() {
        scopeCache.clear();
        visitedElements.clear();
    }

    /**
     * Defines the set of types the calling code recognizes. If one of the types
     * are found during the walk through the XML Schema, it is attached to the
     * relevant {@link XmlSchemaTypeInfo} that is passed to the
     * {@link XmlSchemaVisitor}s, with lower types in the hierarchy taking
     * precedence over higher types.
     * <p>
     * This information is useful when translating from XML Schema to another
     * schema, as this automatically associates the destination type with the
     * source XML Schema type.
     * </p>
     *
     * @param userRecognizedTypes The set of types the user recognizes and would
     *            like recognized when traversed.
     */
    public void setUserRecognizedTypes(Set<QName> userRecognizedTypes) {
        this.userRecognizedTypes = userRecognizedTypes;
    }

    /**
     * The user-defined types set with the call to
     * {@link #setUserRecognizedTypes(Set)}, or <code>null</code> if none.
     */
    public Set<QName> getUserRecognizedTypes() {
        return userRecognizedTypes;
    }

    /**
     * Initiates a walk through the {@link XmlSchemaCollection} starting with
     * the provided root {@link XmlSchemaElement}. Any visitors will be notified
     * as the walk progresses.
     * <p>
     * Once this method completes, call {@link #clear()} before starting another
     * walk through the XML Schemas.
     * </p>
     *
     * @param element The root element to start the walk from.
     */
    public void walk(XmlSchemaElement element) {
        element = getElement(element, false);

        final XmlSchemaElement substGroupElem = element;

        /*
         * If this element is the root of a substitution group, notify the
         * visitors.
         */
        List<XmlSchemaElement> substitutes = null;
        if (elemsBySubstGroup.containsKey(getElementQName(element))) {
            substitutes = elemsBySubstGroup.get(element.getQName());

            for (XmlSchemaVisitor visitor : visitors) {
                visitor.onEnterSubstitutionGroup(substGroupElem);
            }

            // Force a copy to change the min & max occurs.
            element = getElement(element, true);
            element.setMinOccurs(XmlSchemaParticle.DEFAULT_MIN_OCCURS);
            element.setMaxOccurs(XmlSchemaParticle.DEFAULT_MAX_OCCURS);
        }

        XmlSchemaType schemaType = element.getSchemaType();
        if (schemaType == null) {
            final QName typeQName = element.getSchemaTypeName();
            if (typeQName != null) {
                XmlSchema schema = schemasByNamespace.get(typeQName.getNamespaceURI());
                schemaType = schema.getTypeByName(typeQName);
            }
        }

        if (schemaType != null) {
            XmlSchemaScope scope = null;
            if ((schemaType.getQName() != null) && scopeCache.containsKey(schemaType.getQName())) {
                scope = scopeCache.get(schemaType.getQName());
            } else {
                scope = new XmlSchemaScope(schemaType, schemasByNamespace, scopeCache, userRecognizedTypes);
                if (schemaType.getQName() != null) {
                    scopeCache.put(schemaType.getQName(), scope);
                }
            }

            // 1. Fetch all attributes as a List<XmlSchemaAttribute>.
            final Collection<XmlSchemaAttrInfo> attrs = scope.getAttributesInScope();
            final XmlSchemaTypeInfo typeInfo = scope.getTypeInfo();

            // 2. for each visitor, call visitor.startElement(element, type);
            final boolean previouslyVisited = (!element.isAnonymous() && visitedElements.contains(element
                .getQName()));

            for (XmlSchemaVisitor visitor : visitors) {
                visitor.onEnterElement(element, typeInfo, previouslyVisited);
            }

            if (!element.isAnonymous() && !previouslyVisited) {
                visitedElements.add(element.getQName());
            }

            // If we already visited this element, skip the attributes and
            // child.
            if (!previouslyVisited) {

                // 3. Walk the attributes in the element, retrieving type
                // information.
                if (attrs != null) {
                    for (XmlSchemaAttrInfo attr : attrs) {
                        XmlSchemaType attrType = attr.getAttribute().getSchemaType();
                        if (attrType != null) {
                            XmlSchemaScope attrScope;
                            if ((attrType.getQName() != null) && scopeCache.containsKey(attrType.getQName())) {
                                attrScope = scopeCache.get(attrType.getQName());
                            } else {
                                attrScope = new XmlSchemaScope(attrType,
                                                               schemasByNamespace, scopeCache,
                                                               userRecognizedTypes);

                                if (attrType.getName() != null) {
                                    scopeCache.put(attrType.getQName(), attrScope);
                                }
                            }

                            final XmlSchemaTypeInfo attrTypeInfo = attrScope.getTypeInfo();
                            attr.setType(attrTypeInfo);
                        }

                        for (XmlSchemaVisitor visitor : visitors) {
                            visitor.onVisitAttribute(element, attr);
                        }
                    }
                }

                // 4. Visit the anyAttribute, if any.
                if (scope.getAnyAttribute() != null) {
                    for (XmlSchemaVisitor visitor : visitors) {
                        visitor.onVisitAnyAttribute(element, scope.getAnyAttribute());
                    }
                }

                /*
                 * 5. Notify that we visited all of the attributes (even if
                 * there weren't any).
                 */
                for (XmlSchemaVisitor visitor : visitors) {
                    visitor.onEndAttributes(element, typeInfo);
                }

                // 6. Walk the child groups and elements (if any), depth-first.
                final XmlSchemaParticle child = scope.getParticle();
                if (child != null) {
                    walk(child);
                }
            }

            /*
             * 7. On the way back up, call visitor.endElement(element, type,
             * attributes);
             */
            for (XmlSchemaVisitor visitor : visitors) {
                visitor.onExitElement(element, typeInfo, previouslyVisited);
            }

        } else if (!element.isAbstract()) {
            throw new IllegalStateException("Element " + element.getQName()
                                            + " is not abstract and has no type.");
        }

        // 8. Now handle substitute elements, if any.
        if (substitutes != null) {
            for (XmlSchemaElement substitute : substitutes) {
                walk(substitute);
            }

            for (XmlSchemaVisitor visitor : visitors) {
                visitor.onExitSubstitutionGroup(substGroupElem);
            }
        }
    }

    private void walk(XmlSchemaParticle particle) {
        if (particle instanceof XmlSchemaGroupRef) {
            XmlSchemaGroupRef groupRef = (XmlSchemaGroupRef)particle;
            XmlSchemaGroupParticle group = groupRef.getParticle();
            if (group == null) {
                XmlSchema schema = schemasByNamespace.get(groupRef.getRefName().getNamespaceURI());

                group = schema.getGroupByName(groupRef.getRefName()).getParticle();
            }
            walk(group, groupRef.getMinOccurs(), groupRef.getMaxOccurs());

        } else if (particle instanceof XmlSchemaGroupParticle) {
            walk((XmlSchemaGroupParticle)particle, particle.getMinOccurs(), particle.getMaxOccurs());

        } else if (particle instanceof XmlSchemaElement) {
            walk((XmlSchemaElement)particle);

        } else if (particle instanceof XmlSchemaAny) {
            for (XmlSchemaVisitor visitor : visitors) {
                visitor.onVisitAny((XmlSchemaAny)particle);
            }

        } else {
            throw new IllegalArgumentException("Unknown particle type " + particle.getClass().getName());
        }

    }

    private void walk(XmlSchemaGroupParticle group, long minOccurs, long maxOccurs) {

        // Only make a copy of the particle if the minOccurs or maxOccurs was
        // set.
        final boolean forceCopy = ((minOccurs != group.getMinOccurs()) || (maxOccurs != group.getMaxOccurs()));

        // 1. Determine the group particle type.
        XmlSchemaAll all = null;
        XmlSchemaChoice choice = null;
        XmlSchemaSequence seq = null;

        ArrayList<XmlSchemaParticle> children = null;

        if (group instanceof XmlSchemaAll) {
            all = (XmlSchemaAll)group;

        } else if (group instanceof XmlSchemaChoice) {
            choice = (XmlSchemaChoice)group;

        } else if (group instanceof XmlSchemaSequence) {
            seq = (XmlSchemaSequence)group;

        } else {
            throw new IllegalArgumentException("Unrecognized XmlSchemaGroupParticle of type "
                                               + group.getClass().getName());
        }

        // 2. Make a copy if necessary.
        if (forceCopy) {
            if (all != null) {
                XmlSchemaAll copy = new XmlSchemaAll();
                copy.setAnnotation(all.getAnnotation());
                copy.setId(all.getId());
                copy.setLineNumber(all.getLineNumber());
                copy.setLinePosition(all.getLinePosition());
                copy.setMetaInfoMap(all.getMetaInfoMap());
                copy.setMinOccurs(minOccurs);
                copy.setMaxOccurs(maxOccurs);
                copy.setSourceURI(all.getSourceURI());
                copy.setUnhandledAttributes(all.getUnhandledAttributes());
                copy.getItems().addAll(all.getItems());

                all = copy;

            } else if (choice != null) {
                XmlSchemaChoice copy = new XmlSchemaChoice();
                copy.setAnnotation(choice.getAnnotation());
                copy.setId(choice.getId());
                copy.setLineNumber(choice.getLineNumber());
                copy.setLinePosition(choice.getLinePosition());
                copy.setMinOccurs(minOccurs);
                copy.setMaxOccurs(maxOccurs);
                copy.setMetaInfoMap(choice.getMetaInfoMap());
                copy.setSourceURI(choice.getSourceURI());
                copy.setUnhandledAttributes(choice.getUnhandledAttributes());
                copy.getItems().addAll(choice.getItems());

                choice = copy;

            } else if (seq != null) {
                XmlSchemaSequence copy = new XmlSchemaSequence();
                copy.setAnnotation(seq.getAnnotation());
                copy.setId(seq.getId());
                copy.setLineNumber(seq.getLineNumber());
                copy.setLinePosition(seq.getLinePosition());
                copy.setMinOccurs(minOccurs);
                copy.setMaxOccurs(maxOccurs);
                copy.setMetaInfoMap(seq.getMetaInfoMap());
                copy.setSourceURI(seq.getSourceURI());
                copy.setUnhandledAttributes(seq.getUnhandledAttributes());

                seq = copy;
            }
        }

        // 3. Notify the visitors.
        for (XmlSchemaVisitor visitor : visitors) {
            if (all != null) {
                visitor.onEnterAllGroup(all);
            } else if (choice != null) {
                visitor.onEnterChoiceGroup(choice);
            } else if (seq != null) {
                visitor.onEnterSequenceGroup(seq);
            }
        }

        // 4. Walk the children.
        if (all != null) {
            children = new ArrayList<XmlSchemaParticle>(all.getItems().size());
            for (XmlSchemaAllMember item : all.getItems()) {
                if (item instanceof XmlSchemaGroup) {
                    children.add(((XmlSchemaGroup)item).getParticle());
                } else if (item instanceof XmlSchemaParticle) {
                    children.add((XmlSchemaParticle)item);
                } else {
                    throw new IllegalArgumentException(
                                                       "All child is not an XmlSchemaGroup or XmlSchemaParticle; "
                                                           + "it is a " + item.getClass().getName());
                }
            }
        } else if (choice != null) {
            children = new ArrayList<XmlSchemaParticle>(choice.getItems().size());
            for (XmlSchemaChoiceMember item : choice.getItems()) {
                if (item instanceof XmlSchemaGroup) {
                    children.add(((XmlSchemaGroup)item).getParticle());
                } else if (item instanceof XmlSchemaParticle) {
                    children.add((XmlSchemaParticle)item);
                } else {
                    throw new IllegalArgumentException(
                                                       "Choice child is not an XmlSchemaGroup or XmlSchemaParticle; "
                                                           + "it is a " + item.getClass().getName());
                }
            }

        } else if (seq != null) {
            children = new ArrayList<XmlSchemaParticle>(seq.getItems().size());
            for (XmlSchemaSequenceMember item : seq.getItems()) {
                if (item instanceof XmlSchemaGroup) {
                    children.add(((XmlSchemaGroup)item).getParticle());
                } else if (item instanceof XmlSchemaParticle) {
                    children.add((XmlSchemaParticle)item);
                } else {
                    throw new IllegalArgumentException(
                                                       "Sequence child is not an XmlSchemaGroup or XmlSchemaParticle; "
                                                           + "it is a " + item.getClass().getName());
                }
            }
        }

        if (children == null) {
            throw new IllegalStateException("Could not process group of type " + group.getClass().getName());
        }

        for (XmlSchemaParticle child : children) {
            walk(child);
        }

        // 5. Notify the visitors we are exiting the group.
        for (XmlSchemaVisitor visitor : visitors) {
            if (all != null) {
                visitor.onExitAllGroup(all);
            } else if (choice != null) {
                visitor.onExitChoiceGroup(choice);
            } else if (seq != null) {
                visitor.onExitSequenceGroup(seq);
            }
        }
    }

    /**
     * If the provided {@link XmlSchemaElement} is a reference, track down the
     * original and add the minimum and maximum occurrence fields. Otherwise,
     * just return the provided <code>element</code>.
     *
     * @param element The element to get the definition of.
     * @return The real {@link XmlSchemaElement}.
     */
    private XmlSchemaElement getElement(XmlSchemaElement element, boolean isSubstitutionGroup) {

        if (!element.isRef() && !isSubstitutionGroup) {
            return element;
        }

        final QName elemQName = getElementQName(element);
        final XmlSchema schema = schemasByNamespace.get(elemQName.getNamespaceURI());

        XmlSchemaElement globalElem = null;
        if (!element.isRef()) {
            globalElem = element;
        } else if (element.getRef().getTarget() != null) {
            globalElem = element.getRef().getTarget();
        } else {
            globalElem = schema.getElementByName(elemQName);
        }

        /*
         * An XML Schema element reference defines the id, minOccurs, and
         * maxOccurs attributes, while the global element definition defines id
         * and all other attributes. This combines the two together.
         */
        String id = element.getId();
        if (id == null) {
            id = globalElem.getId();
        }

        final XmlSchemaElement copy = new XmlSchemaElement(schema, false);
        copy.setName(globalElem.getName());
        copy.setAbstract(globalElem.isAbstract());
        copy.setAnnotation(globalElem.getAnnotation());
        copy.setBlock(globalElem.getBlock());
        copy.setDefaultValue(globalElem.getDefaultValue());
        copy.setFinal(globalElem.getFinal());
        copy.setFixedValue(globalElem.getFixedValue());
        copy.setForm(globalElem.getForm());
        copy.setId(id);
        copy.setLineNumber(element.getLineNumber());
        copy.setLinePosition(element.getLinePosition());
        copy.setMaxOccurs(element.getMaxOccurs());
        copy.setMinOccurs(element.getMinOccurs());
        copy.setMetaInfoMap(globalElem.getMetaInfoMap());
        copy.setNillable(globalElem.isNillable());
        copy.setType(globalElem.getSchemaType());
        copy.setSchemaTypeName(globalElem.getSchemaTypeName());
        copy.setSourceURI(globalElem.getSourceURI());
        copy.setSubstitutionGroup(globalElem.getSubstitutionGroup());
        copy.setUnhandledAttributes(globalElem.getUnhandledAttributes());

        return copy;
    }

    private static QName getElementQName(XmlSchemaElement element) {
        if (element.isRef()) {
            return element.getRefBase().getTargetQName();
        } else {
            return element.getQName();
        }
    }
}
