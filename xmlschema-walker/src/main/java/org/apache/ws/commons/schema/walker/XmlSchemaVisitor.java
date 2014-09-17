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

import org.apache.ws.commons.schema.XmlSchemaAll;
import org.apache.ws.commons.schema.XmlSchemaAny;
import org.apache.ws.commons.schema.XmlSchemaAnyAttribute;
import org.apache.ws.commons.schema.XmlSchemaChoice;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;

/**
 * Defines a visitor interface for notifications when walking an
 * {@link org.apache.ws.commons.schema.XmlSchema} using the
 * {@link XmlSchemaWalker}.
 * <p>
 * Use this interface in conjunction with <code>XmlSchemaWalker</code> to
 * receive events as an {@link org.apache.ws.commons.schema.XmlSchema} is
 * traversed.
 * </p>
 */
public interface XmlSchemaVisitor {

    /**
     * A notification that an {@link XmlSchemaElement} has been entered. The
     * element returned will be a true representation of the element at that
     * point in the schema: if the schema defines a reference, the reference is
     * followed and merged with its global definition.
     * <p>
     * The first time this element is reached, all of its attributes will be
     * visited (if any). Once the attributes have been visited,
     * {@link #onEndAttributes(XmlSchemaElement, XmlSchemaTypeInfo)} will be
     * called. The only exception is when the element has no type information,
     * at which point the next call will be to
     * {@link #onExitElement(XmlSchemaElement, XmlSchemaTypeInfo, boolean)}.
     * </p>
     * <p>
     * On all subsequent visits to this element, <code>previouslyVisited</code>
     * will be set to <code>true</code> and the attributes will not be
     * revisited. The next call will be to
     * {@link #onExitElement(XmlSchemaElement, XmlSchemaTypeInfo, boolean)}, as
     * all of the element's attributes and children have already been provided.
     * </p>
     *
     * @param element The element the walker is currently entering.
     * @param typeInfo The type information of that element.
     * @param previouslyVisited Whether the element was previously visited.
     */
    void onEnterElement(XmlSchemaElement element, XmlSchemaTypeInfo typeInfo, boolean previouslyVisited);

    /**
     * Signifies the end of the element that was previously entered. Provides
     * the same information about the element as was provided in
     * {@link #onEnterElement(XmlSchemaElement, XmlSchemaTypeInfo, boolean)} in
     * the event it is easier to process on exit.
     *
     * @param element The element the walker is currently exiting.
     * @param typeInfo The type information of that element.
     * @param previouslyVisited Whether the element was previously visited.
     */
    void onExitElement(XmlSchemaElement element, XmlSchemaTypeInfo typeInfo, boolean previouslyVisited);

    /**
     * This method is called for each attribute associated with the element,
     * providing the {@link XmlSchemaAttrInfo} representing that attribute.
     *
     * @param element The element owing the attribute.
     * @param attrInfo The attribute information.
     */
    void onVisitAttribute(XmlSchemaElement element, XmlSchemaAttrInfo attrInfo);

    /**
     * This method is called when all of the attributes have been processed
     * (provided the element has a type defined). This is a convenience method
     * to allow the visitor to be notified when no more attributes are coming,
     * and the walker will be traversing the element's children.
     *
     * @param element The element the walker is traversing.
     * @param typeInfo Type information about the element, if it is easier to
     *            process here.
     */
    void onEndAttributes(XmlSchemaElement element, XmlSchemaTypeInfo typeInfo);

    /**
     * This method is called when the walker enters a substitution group. This
     * method is called providing the base type, and then
     * {@link #onEnterElement(XmlSchemaElement, XmlSchemaTypeInfo, boolean)} is
     * called for all types in the substitution group, starting with the base
     * type.
     * <p>
     * The only exception is when the base type of the substitution group is
     * abstract ({@link XmlSchemaElement#isAbstract()} returns <code>true</code>
     * ). When this happens,
     * {@link #onEnterElement(XmlSchemaElement, XmlSchemaTypeInfo, boolean)} is
     * not called with the abstract type, as there is no information to provide.
     * </p>
     * <p>
     * If the root element is the base of a substitution group, this method will
     * be the first one called. Otherwise,
     * {@link #onEnterElement(XmlSchemaElement, XmlSchemaTypeInfo, boolean)}
     * will be called with the root element.
     * </p>
     *
     * @param base The {@link XmlSchemaElement} representing the base of the
     *            substitution group.
     */
    void onEnterSubstitutionGroup(XmlSchemaElement base);

    /**
     * Called when the end of the substitution group is reached. The base
     * element of the substitution group is provided for convenience.
     *
     * @param base The base element of the subtitution group.
     */
    void onExitSubstitutionGroup(XmlSchemaElement base);

    /**
     * Called when an all group is entered.
     *
     * @param all The {@link XmlSchemaAll} representing the all group.
     */
    void onEnterAllGroup(XmlSchemaAll all);

    /**
     * Called when an all group is exited.
     *
     * @param all The {@link XmlSchemaAll} representing the all group.
     */
    void onExitAllGroup(XmlSchemaAll all);

    /**
     * Called when a choice group is entered.
     *
     * @param all The {@link XmlSchemaChoice} representing the choice group.
     */
    void onEnterChoiceGroup(XmlSchemaChoice choice);

    /**
     * Called when a choice group is exited.
     *
     * @param all The {@link XmlSchemaChoice} representing the choice group.
     */
    void onExitChoiceGroup(XmlSchemaChoice choice);

    /**
     * Called when a sequence is entered.
     *
     * @param seq The {@link XmlSchemaSequence} representing the sequence.
     */
    void onEnterSequenceGroup(XmlSchemaSequence seq);

    /**
     * Called when a sequence is exited.
     *
     * @param seq The {@link XmlSchemaSequence} representing the sequence.
     */
    void onExitSequenceGroup(XmlSchemaSequence seq);

    /**
     * Called when a wildcard element is entered.
     *
     * @param any The {@link XmlSchemaAny} representing the wildcard element.
     */
    void onVisitAny(XmlSchemaAny any);

    /**
     * Called when a wildcard attribute is visited. If an element has a wildcard
     * element, this will be called after all other attributes have been
     * visited, and before the call to
     * {@link #onEndAttributes(XmlSchemaElement, XmlSchemaTypeInfo)}.
     *
     * @param element The owning element.
     * @param anyAttr The {@link XmlSchemaAnyAttribute} representing the
     *            wildcard attribute.
     */
    void onVisitAnyAttribute(XmlSchemaElement element, XmlSchemaAnyAttribute anyAttr);
}
