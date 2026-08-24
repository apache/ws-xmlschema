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
import java.util.IdentityHashMap;
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

    /*
     * The walker signals "previously visited" by type identity
     * (XmlSchemaWalker.visitedTypes is reference-keyed), so keep a
     * type-identity index alongside the QName index: two differently-named
     * elements may legally share one named complex type, and two same-named
     * local declarations may have different types. XmlSchemaTypeInfo
     * identity is stable for named types because the walker caches one
     * XmlSchemaScope per named type and XmlSchemaScope.getTypeInfo() returns
     * a stored field.
     */
    private Map<XmlSchemaTypeInfo, ElementInfo> elementInfoByType;

    /*
     * Nodes whose possibleNextStates must be copied from a source node whose
     * element is still on the stack -- i.e. whose children are still being
     * walked because the type is recursive. The copy is deferred until the
     * source node's element finally exits, at which point its content model
     * is complete.
     */
    private Map<XmlSchemaStateMachineNode, List<XmlSchemaStateMachineNode>> pendingNextStateCopies;

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
        elementInfoByType = new IdentityHashMap<XmlSchemaTypeInfo, ElementInfo>();
        pendingNextStateCopies =
            new IdentityHashMap<XmlSchemaStateMachineNode, List<XmlSchemaStateMachineNode>>();
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
             *
             * The QName entry is deliberately an overwrite: the in-flight
             * element must stay reachable by QName from onVisitAttribute()
             * and onEndAttributes(), even if a different declaration with
             * the same QName was walked earlier. The previously-visited
             * branch below therefore never trusts a QName hit alone -- it
             * resolves by type identity first and validates any QName hit
             * against the element's schema type.
             */
            final ElementInfo info = new ElementInfo(element, typeInfo);
            elementInfoByQName.put(element.getQName(), info);
            if (!elementInfoByType.containsKey(typeInfo)) {
                elementInfoByType.put(typeInfo, info);
            }

        } else {
            /*
             * We have previously encountered this element's type (the walker
             * tracks visits by type identity, not element QName), which
             * means we have already collected all of the information we
             * needed to build an XmlSchemaStateMachineNode for that type.
             *
             * Resolve the prior bookkeeping by type identity first. The
             * QName index alone is unreliable here: a differently-named
             * element may share a previously-visited named type (and have no
             * QName entry of its own), and two distinct same-QName
             * declarations overwrite each other's QName entries.
             */
            ElementInfo elemInfo = elementInfoByType.get(typeInfo);

            boolean sharedFromSibling = false;
            if ((elemInfo != null) && (elemInfo.stateMachineNode != null)) {
                sharedFromSibling = !element.getQName().equals(elemInfo.element.getQName());
            } else {
                /*
                 * No type-identity entry: for anonymous types the walker
                 * builds a fresh XmlSchemaTypeInfo on every encounter (only
                 * named types' scopes are cached), so the identity lookup
                 * cannot match. Fall back to the QName index, but validate
                 * the hit against the element's schema type -- reference
                 * equality on the XmlSchemaType is exactly how the walker
                 * decided previouslyVisited -- so two distinct same-QName
                 * declarations can never cross-bind.
                 */
                final ElementInfo byQName = elementInfoByQName.get(element.getQName());
                if ((byQName != null) && (byQName.stateMachineNode != null)
                    && isSameSchemaType(byQName.element, element)) {
                    elemInfo = byQName;
                }
            }

            if ((elemInfo == null) || (elemInfo.stateMachineNode == null)) {
                throw new IllegalStateException("Element " + element.getQName()
                                                + " was already visited, but we do not"
                                                + " have a state machine for it.");

            } else if (stack.isEmpty()) {
                throw new IllegalStateException("Element " + element.getQName()
                                                + " was previously visited, but there is no"
                                                + " parent state machine node to attach it to!");
            }

            XmlSchemaStateMachineNode stateMachineNode;

            if (sharedFromSibling) {
                /*
                 * A differently-named element sharing a previously-visited
                 * named type: build this element its own state machine node
                 * from the type-sharing sibling instead of failing on a
                 * legal schema shape. The walker will not re-walk this
                 * element's children (they were walked on the type's first
                 * visit), so the sibling node's possibleNextStates must be
                 * copied over as well -- otherwise this node would carry an
                 * empty content model and legal documents would fail
                 * downstream in XmlSchemaPathFinder.
                 */
                final ElementInfo newInfo = new ElementInfo(element, typeInfo);
                newInfo.attributes.addAll(elemInfo.attributes);
                newInfo.stateMachineNode =
                    new XmlSchemaStateMachineNode(element, newInfo.attributes, typeInfo);
                copyPossibleNextStates(elemInfo.stateMachineNode, newInfo.stateMachineNode);
                if (!elementInfoByQName.containsKey(element.getQName())) {
                    elementInfoByQName.put(element.getQName(), newInfo);
                }
                stateMachineNode = newInfo.stateMachineNode;

            } else {
                stateMachineNode = elemInfo.stateMachineNode;

                /*
                 * If this element is identical in every way except for the
                 * minimum and maximum number of occurrences, we want to
                 * create a new state machine node to represent this element.
                 * The new node needs the original's possibleNextStates as
                 * well, for the same reason as above: the walker skips the
                 * children of a previously-visited type.
                 */
                if ((stateMachineNode.getMinOccurs() != element.getMinOccurs())
                    || (stateMachineNode.getMaxOccurs() != element.getMaxOccurs())) {
                    stateMachineNode = new XmlSchemaStateMachineNode(element, elemInfo.attributes,
                                                                     elemInfo.typeInfo);
                    copyPossibleNextStates(elemInfo.stateMachineNode, stateMachineNode);
                }
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

        /*
         * If any nodes are waiting on this node's possibleNextStates (a
         * recursive type was re-entered while this node's children were
         * still being walked), and this node no longer appears anywhere on
         * the stack, its content model is complete: perform the deferred
         * copies now.
         */
        final List<XmlSchemaStateMachineNode> waiting = pendingNextStateCopies.get(node);
        if ((waiting != null) && !isOnStack(node)) {
            pendingNextStateCopies.remove(node);
            for (XmlSchemaStateMachineNode waiter : waiting) {
                waiter.addPossibleNextStates(node.getPossibleNextStates());
            }
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

    /*
     * XmlSchemaStateMachineNode.addPossibleNextStates() copies the elements
     * of the collection (Collection.addAll), not the list reference, so a
     * copy taken while the source's children are still being walked (a
     * recursive type) could be an incomplete snapshot. Copy immediately when
     * the source's element is no longer being walked; otherwise defer the
     * copy until the source's element exits (see onExitElement()).
     */
    private void copyPossibleNextStates(XmlSchemaStateMachineNode source,
                                        XmlSchemaStateMachineNode target) {
        if (isOnStack(source)) {
            List<XmlSchemaStateMachineNode> waiting = pendingNextStateCopies.get(source);
            if (waiting == null) {
                waiting = new ArrayList<XmlSchemaStateMachineNode>();
                pendingNextStateCopies.put(source, waiting);
            }
            waiting.add(target);
        } else {
            target.addPossibleNextStates(source.getPossibleNextStates());
        }
    }

    private boolean isOnStack(XmlSchemaStateMachineNode node) {
        for (int index = stack.size() - 1; index >= 0; --index) {
            if (stack.get(index) == node) {
                return true;
            }
        }
        return false;
    }

    /*
     * Reference equality on the schema type mirrors the walker's own
     * previously-visited bookkeeping (XmlSchemaWalker.visitedTypes is
     * keyed on the XmlSchemaType instance). When neither element carries a
     * resolved schema type, fall back to the type's QName, which is unique
     * per namespace for named types.
     */
    private static boolean isSameSchemaType(XmlSchemaElement known, XmlSchemaElement current) {
        if ((known.getSchemaType() != null) || (current.getSchemaType() != null)) {
            return known.getSchemaType() == current.getSchemaType();
        }
        return (known.getSchemaTypeName() != null)
               && known.getSchemaTypeName().equals(current.getSchemaTypeName());
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
