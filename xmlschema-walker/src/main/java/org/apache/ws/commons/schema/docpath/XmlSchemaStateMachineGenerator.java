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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchemaAll;
import org.apache.ws.commons.schema.XmlSchemaAny;
import org.apache.ws.commons.schema.XmlSchemaAnyAttribute;
import org.apache.ws.commons.schema.XmlSchemaChoice;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.walker.XmlSchemaAttrInfo;
import org.apache.ws.commons.schema.walker.XmlSchemaTypeInfo;
import org.apache.ws.commons.schema.walker.XmlSchemaVisitor;

/**
 * Builds a state machine from an {@link org.apache.ws.commons.schema.XmlSchema}
 * representing how to walk through the schema when parsing an XML document.
 */
public final class XmlSchemaStateMachineGenerator implements XmlSchemaVisitor {

    private List<XmlSchemaStateMachineNode> stack;
    private XmlSchemaStateMachineNode startNode;
    private Map<QName, ElementInfo> elementInfoByQName;

    private static class ElementInfo {
        final List<XmlSchemaAttrInfo> attributes;
        final XmlSchemaTypeInfo typeInfo;
        final XmlSchemaElement element;

        XmlSchemaStateMachineNode stateMachineNode;

        ElementInfo(XmlSchemaElement element, XmlSchemaTypeInfo typeInfo) {

            this.element = element;
            this.typeInfo = typeInfo;
            this.attributes = new ArrayList<XmlSchemaAttrInfo>();
            this.stateMachineNode = null;
        }

        void addAttribute(XmlSchemaAttrInfo attrInfo) {
            attributes.add(attrInfo);
        }
    }

    /**
     * Constructs a new <code>XmlSchemaStateMachineGenerator</code>, ready to
     * start walking {@link org.apache.ws.commons.schema.XmlSchema}s.
     */
    public XmlSchemaStateMachineGenerator() {
        stack = new ArrayList<XmlSchemaStateMachineNode>();
        elementInfoByQName = new HashMap<QName, ElementInfo>();
        startNode = null;
    }

    /**
     * Retrieves the start node of the state machine representing the
     * most-recently walked {@link org.apache.ws.commons.schema.XmlSchema}.
     */
    public XmlSchemaStateMachineNode getStartNode() {
        return startNode;
    }

    /**
     * Retrieves the {@link XmlSchemaStateMachineNode}s representing each
     * {@link XmlSchemaElement} in the walked
     * {@link org.apache.ws.commons.schema.XmlSchema}.
     * <p>
     * Only top-level {@link XmlSchemaElement}s can be retrieved by calling
     * {@link org.apache.ws.commons.schema.XmlSchema#getElementByName(QName)};
     * this allows all elements to be retrieved without walking the schema
     * again.
     * </p>
     */
    public Map<QName, XmlSchemaStateMachineNode> getStateMachineNodesByQName() {
        final HashMap<QName, XmlSchemaStateMachineNode> nodes = new HashMap<QName, XmlSchemaStateMachineNode>();

        for (Map.Entry<QName, ElementInfo> entry : elementInfoByQName.entrySet()) {
            nodes.put(entry.getKey(), entry.getValue().stateMachineNode);
        }

        return nodes;
    }

    /**
     * @see XmlSchemaVisitor#onEnterElement(XmlSchemaElement, XmlSchemaTypeInfo,
     *      boolean)
     */
    @Override
    public void onEnterElement(XmlSchemaElement element, XmlSchemaTypeInfo typeInfo, boolean previouslyVisited) {

        if (!previouslyVisited) {
            /*
             * This is our first encounter of the element. We do not have the
             * attributes yet, so we cannot create a state machine node for it.
             * However, we will have all of the attributes once
             * onEndAttributes() is called, so we can create an ElementInfo
             * entry for it, and wait until later to create the state machine
             * and add it to the stack.
             */
            final ElementInfo info = new ElementInfo(element, typeInfo);
            elementInfoByQName.put(element.getQName(), info);

        } else {
            /*
             * We have previously encountered this element, which means we have
             * already collected all of the information we needed to build an
             * XmlSchemaStateMachineNode. Likewise, we can just reference it.
             */
            final ElementInfo elemInfo = elementInfoByQName.get(element.getQName());
            if ((elemInfo == null) || (elemInfo.stateMachineNode == null)) {
                throw new IllegalStateException("Element " + element.getQName()
                                                + " was already visited, but we do not"
                                                + " have a state machine for it.");

            } else if (stack.isEmpty()) {
                throw new IllegalStateException("Element " + element.getQName()
                                                + " was previously visited, but there is no"
                                                + " parent state machine node to attach it to!");
            }

            XmlSchemaStateMachineNode stateMachineNode = elemInfo.stateMachineNode;

            /*
             * If this element is identical in every way except for the minimum
             * and maximum number of occurrences, we want to create a new state
             * machine node to represent this element.
             */
            if ((stateMachineNode.getMinOccurs() != element.getMinOccurs())
                || (stateMachineNode.getMaxOccurs() != element.getMaxOccurs())) {
                stateMachineNode = new XmlSchemaStateMachineNode(element, elemInfo.attributes,
                                                                 elemInfo.typeInfo);
            }

            stack.get(stack.size() - 1).addPossibleNextState(stateMachineNode);

            stack.add(stateMachineNode);
        }
    }

    /**
     * @see XmlSchemaVisitor#onExitElement(XmlSchemaElement, XmlSchemaTypeInfo,
     *      boolean)
     */
    @Override
    public void onExitElement(XmlSchemaElement element, XmlSchemaTypeInfo typeInfo, boolean previouslyVisited) {

        if (stack.isEmpty()) {
            throw new IllegalStateException("Exiting " + element.getQName() + ", but the stack is empty.");
        }

        final XmlSchemaStateMachineNode node = stack.remove(stack.size() - 1);
        if (!node.getNodeType().equals(XmlSchemaStateMachineNode.Type.ELEMENT)) {
            throw new IllegalStateException("Exiting element " + element.getQName() + ", but  " + node
                                            + " is on the stack.");

        } else if (!node.getElement().getQName().equals(element.getQName())) {
            throw new IllegalStateException("Element " + element.getQName()
                                            + " is not the same in-memory copy we received on creation.  Our"
                                            + " copy is of a " + node.getElement().getQName());
        }
    }

    /**
     * @see XmlSchemaVisitor#onVisitAttribute(XmlSchemaElement,
     *      XmlSchemaAttrInfo)
     */
    @Override
    public void onVisitAttribute(XmlSchemaElement element, XmlSchemaAttrInfo attrInfo) {

        final ElementInfo elemInfo = elementInfoByQName.get(element.getQName());
        if (elemInfo == null) {
            throw new IllegalStateException("No record exists for element " + element.getQName());
        }

        elemInfo.addAttribute(attrInfo);
    }

    /**
     * @see XmlSchemaVisitor#onEndAttributes(XmlSchemaElement,
     *      XmlSchemaTypeInfo)
     */
    @Override
    public void onEndAttributes(XmlSchemaElement element, XmlSchemaTypeInfo elemTypeInfo) {

        /*
         * The parent of this group is an element that needs to be added to the
         * stack.
         */
        final ElementInfo elemInfo = elementInfoByQName.get(element.getQName());

        if (elemInfo.stateMachineNode != null) {
            throw new IllegalStateException("Parent element " + element.getQName()
                                            + " is supposedly undefined, but that entry already has a state"
                                            + " machine of " + elemInfo.stateMachineNode);
        }

        elemInfo.stateMachineNode = new XmlSchemaStateMachineNode(elemInfo.element, elemInfo.attributes,
                                                                  elemInfo.typeInfo);

        if (!stack.isEmpty()) {
            stack.get(stack.size() - 1).addPossibleNextState(elemInfo.stateMachineNode);
        } else {
            // This is the root node.
            startNode = elemInfo.stateMachineNode;
        }

        stack.add(elemInfo.stateMachineNode);

    }

    /**
     * @see XmlSchemaVisitor#onEnterSubstitutionGroup(XmlSchemaElement)
     */
    @Override
    public void onEnterSubstitutionGroup(XmlSchemaElement base) {
        if (stack.isEmpty()) {
            // The root element is the base of a substitution group.
            startNode = new XmlSchemaStateMachineNode(XmlSchemaStateMachineNode.Type.SUBSTITUTION_GROUP,
                                                      base.getMinOccurs(), base.getMaxOccurs());
            stack.add(startNode);
        } else {
            pushGroup(XmlSchemaStateMachineNode.Type.SUBSTITUTION_GROUP, base.getMinOccurs(),
                      base.getMaxOccurs());
        }
    }

    /**
     * @see XmlSchemaVisitor#onExitSubstitutionGroup(XmlSchemaElement)
     */
    @Override
    public void onExitSubstitutionGroup(XmlSchemaElement base) {
        popGroup(XmlSchemaStateMachineNode.Type.SUBSTITUTION_GROUP);
    }

    /**
     * @see XmlSchemaVisitor#onEnterAllGroup(XmlSchemaAll)
     */
    @Override
    public void onEnterAllGroup(XmlSchemaAll all) {
        pushGroup(XmlSchemaStateMachineNode.Type.ALL, all.getMinOccurs(), all.getMaxOccurs());
    }

    /**
     * @see XmlSchemaVisitor#onExitAllGroup(XmlSchemaAll)
     */
    @Override
    public void onExitAllGroup(XmlSchemaAll all) {
        popGroup(XmlSchemaStateMachineNode.Type.ALL);
    }

    /**
     * @see XmlSchemaVisitor#onEnterChoiceGroup(XmlSchemaChoice)
     */
    @Override
    public void onEnterChoiceGroup(XmlSchemaChoice choice) {
        pushGroup(XmlSchemaStateMachineNode.Type.CHOICE, choice.getMinOccurs(), choice.getMaxOccurs());
    }

    /**
     * @see XmlSchemaVisitor#onExitChoiceGroup(XmlSchemaChoice)
     */
    @Override
    public void onExitChoiceGroup(XmlSchemaChoice choice) {
        popGroup(XmlSchemaStateMachineNode.Type.CHOICE);
    }

    /**
     * @see XmlSchemaVisitor#onEnterSequenceGroup(XmlSchemaSequence)
     */
    @Override
    public void onEnterSequenceGroup(XmlSchemaSequence seq) {
        pushGroup(XmlSchemaStateMachineNode.Type.SEQUENCE, seq.getMinOccurs(), seq.getMaxOccurs());
    }

    /**
     * @see XmlSchemaVisitor#onExitSequenceGroup(XmlSchemaSequence)
     */
    @Override
    public void onExitSequenceGroup(XmlSchemaSequence seq) {
        popGroup(XmlSchemaStateMachineNode.Type.SEQUENCE);
    }

    /**
     * @see XmlSchemaVisitor#onVisitAny(XmlSchemaAny)
     */
    @Override
    public void onVisitAny(XmlSchemaAny any) {
        final XmlSchemaStateMachineNode node = new XmlSchemaStateMachineNode(any);

        if (stack.isEmpty()) {
            throw new IllegalStateException("Reached an wildcard with no parent!  The stack is empty.");
        }

        stack.get(stack.size() - 1).addPossibleNextState(node);
    }

    /**
     * @see XmlSchemaVisitor#onVisitAnyAttribute(XmlSchemaElement,
     *      XmlSchemaAnyAttribute)
     */
    @Override
    public void onVisitAnyAttribute(XmlSchemaElement element, XmlSchemaAnyAttribute anyAttr) {

        // Ignored.
    }

    private void pushGroup(XmlSchemaStateMachineNode.Type groupType, long minOccurs, long maxOccurs) {

        if (stack.isEmpty()) {
            throw new IllegalStateException("Attempted to create a(n) " + groupType
                                            + " group with no parent - the stack is empty!");
        }

        final XmlSchemaStateMachineNode node = new XmlSchemaStateMachineNode(groupType, minOccurs, maxOccurs);

        stack.get(stack.size() - 1).addPossibleNextState(node);
        stack.add(node);
    }

    private void popGroup(XmlSchemaStateMachineNode.Type groupType) {
        if (stack.isEmpty()) {
            throw new IllegalStateException("Exiting an " + groupType + " group, but the stack is empty!");
        }

        final XmlSchemaStateMachineNode node = stack.remove(stack.size() - 1);

        if (!node.getNodeType().equals(groupType)) {
            throw new IllegalStateException("Attempted to pop a " + groupType
                                            + " off of the stack, but found a " + node.getNodeType()
                                            + " instead!");
        }

        if (!groupType.equals(XmlSchemaStateMachineNode.Type.SUBSTITUTION_GROUP) && stack.isEmpty()) {
            throw new IllegalStateException("Popped a group of type " + groupType
                                            + " only to find it did not have a parent.");
        }
    }
}
