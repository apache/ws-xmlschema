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
import java.util.Map;

/**
 * Factory for creating {@link XmlSchemaPathNode}s. This allows for recycling
 * and abstracts away the complexity of walking through an XML Schema.
 */
final class XmlSchemaPathManager<U, V> {

    private ArrayList<XmlSchemaPathNode<U, V>> unusedPathNodes;
    private ArrayList<XmlSchemaDocumentNode<U>> unusedDocNodes;

    /**
     * Constructs the document path node factory.
     */
    XmlSchemaPathManager() {
        unusedPathNodes = new ArrayList<XmlSchemaPathNode<U, V>>();
        unusedDocNodes = new ArrayList<XmlSchemaDocumentNode<U>>();
    }

    XmlSchemaPathNode<U, V> createStartPathNode(XmlSchemaPathNode.Direction direction,
                                                XmlSchemaStateMachineNode state) {

        return createPathNode(direction, null, state);
    }

    XmlSchemaPathNode<U, V> addParentSiblingOrContentNodeToPath(XmlSchemaPathNode<U, V> startNode,
                                                                XmlSchemaPathNode.Direction direction) {

        XmlSchemaDocumentNode<U> position = startNode.getDocumentNode();

        switch (direction) {
        case PARENT:
            if (position != null) {
                position = position.getParent();
            }
        case SIBLING: //NOPMD
        case CONTENT:
            if (position == null) {
                throw new IllegalStateException(
                                                "When calling addParentSiblingOrContentNodeToPath(), the "
                                                    + "startNode's document node (and its parent) cannot be null.");
            }
            break;
        default:
            throw new IllegalStateException("This method cannot be called if following a child.  Use "
                                            + "addChildNodeToPath(startNode, direction, stateIndex).");
        }

        XmlSchemaPathNode<U, V> node = null;
        if (!unusedPathNodes.isEmpty()) {
            node = unusedPathNodes.remove(unusedPathNodes.size() - 1);
            node.update(direction, startNode, position);
        } else {
            node = new XmlSchemaPathNode<U, V>(direction, startNode, position);
        }

        if (direction.equals(XmlSchemaPathNode.Direction.SIBLING)) {
            node.setIteration(position.getIteration() + 1);
        } else {
            node.setIteration(position.getIteration());
        }

        return node;
    }

    XmlSchemaPathNode<U, V> addChildNodeToPath(XmlSchemaPathNode<U, V> startNode, int branchIndex) {

        final XmlSchemaStateMachineNode stateMachine = startNode.getStateMachineNode();

        if (stateMachine.getPossibleNextStates() == null) {
            throw new IllegalStateException("Cannot follow the branch index; no possible next states.");

        } else if (stateMachine.getPossibleNextStates().size() <= branchIndex) {
            throw new IllegalArgumentException("Cannot follow the branch index; branch " + branchIndex
                                               + " was requested when there are only "
                                               + stateMachine.getPossibleNextStates().size()
                                               + " branches to follow.");
        }

        final XmlSchemaPathNode<U, V> next = createPathNode(XmlSchemaPathNode.Direction.CHILD,
                                                            startNode,
                                                            stateMachine.getPossibleNextStates()
                                                                .get(branchIndex));

        final XmlSchemaDocumentNode<U> docNode = startNode.getDocumentNode();

        if ((startNode.getDocumentNode() != null) && (docNode.getChildren(startNode.getIteration()) != null)
            && docNode.getChildren().containsKey(branchIndex)) {

            next.setDocumentNode(docNode.getChildren(startNode.getIteration()).get(branchIndex));
            next.setIteration(next.getDocIteration() + 1);

        } else {
            next.setIteration(1);
        }

        return next;
    }

    /**
     * Recyles the provided {@link XmlSchemaPathNode} and all of the nodes that
     * follow it. Unlinks from its previous node.
     */
    void recyclePathNode(XmlSchemaPathNode<U, V> toRecycle) {
        if (toRecycle.getPrevious() != null) {
            toRecycle.getPrevious().setNextNode(-1, null);
            toRecycle.setPreviousNode(null);
        }

        if (toRecycle.getNext() != null) {
            recyclePathNode(toRecycle.getNext());
        }

        unusedPathNodes.add(toRecycle);
    }

    XmlSchemaPathNode<U, V> clone(XmlSchemaPathNode<U, V> original) {
        final XmlSchemaPathNode<U, V> clone = createPathNode(original.getDirection(), original.getPrevious(),
                                                             original.getStateMachineNode());

        clone.setIteration(original.getIteration());

        if (original.getDocumentNode() != null) {
            clone.setDocumentNode(original.getDocumentNode());
        }

        return clone;
    }

    /**
     * Follows the path starting at <code>startNode</code>, creating
     * {@link XmlSchemaDocumentNode}s and linking them along the way.
     *
     * @param startNode The node to start building the tree from.
     */
    void followPath(XmlSchemaPathNode<U, V> startNode) {
        if (startNode.getDocumentNode() == null) {
            if (!startNode.getDirection().equals(XmlSchemaPathNode.Direction.CHILD)) {

                throw new IllegalStateException(
                                                "The startNode may only have a null XmlSchemaDocumentNode if it "
                                                    + "represents the root node, and likewise its only valid "
                                                    + "direction is CHILD, not " + startNode.getDirection());
            }

            // startNode is the root node.
            XmlSchemaDocumentNode<U> rootDoc = createDocumentNode(null, startNode.getStateMachineNode());
            startNode.setDocumentNode(rootDoc);
            rootDoc.addVisitor(startNode);
        }

        XmlSchemaPathNode<U, V> prev = startNode;
        XmlSchemaPathNode<U, V> iter = prev.getNext();
        while (iter != null) {
            if (iter.getDocumentNode() == null) {
                if (!iter.getDirection().equals(XmlSchemaPathNode.Direction.CHILD)) {
                    throw new IllegalStateException(
                                                    "XmlSchemaPathNode has a direction of "
                                                        + iter.getDirection()
                                                        + " but it does not have an XmlSchemaDocumentNode to represent"
                                                        + " its state machine (" + iter.getStateMachineNode()
                                                        + ").");
                }

                final XmlSchemaDocumentNode<U> newDocNode = createDocumentNode(prev.getDocumentNode(),
                                                                               iter.getStateMachineNode());

                iter.setDocumentNode(newDocNode);

                final Map<Integer, XmlSchemaDocumentNode<U>> siblings = prev.getDocumentNode().getChildren();

                if (prev.getIndexOfNextNodeState() < 0) {
                    throw new IllegalStateException(
                                                    "Creating a new document node for a node represented by "
                                                        + iter.getStateMachineNode()
                                                        + " but its previous state does not know how to reach me.");
                }

                siblings.put(prev.getIndexOfNextNodeState(), iter.getDocumentNode());
            }

            switch (iter.getDirection()) {
            case CHILD:
            case SIBLING:
                iter.getDocumentNode().addVisitor(iter);
                break;
            default:
            }

            if (iter.getIteration() != iter.getDocIteration()) {
                throw new IllegalStateException("The current path node (representing "
                                                + iter.getStateMachineNode() + ") has an iteration of "
                                                + iter.getIteration()
                                                + ", which does not match the document node iteration of "
                                                + iter.getDocIteration() + '.');
            }

            prev = iter;
            iter = iter.getNext();
        }
    }

    void unfollowPath(XmlSchemaPathNode<U, V> startNode) {
        // Walk to the end and work backwards, recycling as we go.
        XmlSchemaPathNode<U, V> iter = startNode;
        XmlSchemaPathNode<U, V> prev = null;

        while (iter != null) {
            prev = iter;
            iter = iter.getNext();
        }

        while (prev != startNode) {
            iter = prev;
            prev = iter.getPrevious();

            if (iter.getDocumentNode() != null) {
                iter.getDocumentNode().removeVisitor(iter);
                if (iter.getDocIteration() == 0) {
                    recycleDocumentNode(iter.getDocumentNode());
                }
            }
            recyclePathNode(iter);
        }
    }

    void clear() {
        unusedPathNodes.clear();
        unusedDocNodes.clear();
    }

    private XmlSchemaPathNode<U, V> createPathNode(XmlSchemaPathNode.Direction direction,
                                                   XmlSchemaPathNode<U, V> previous,
                                                   XmlSchemaStateMachineNode state) {

        if (!unusedPathNodes.isEmpty()) {
            XmlSchemaPathNode<U, V> node = unusedPathNodes.remove(unusedPathNodes.size() - 1);
            node.update(direction, previous, state);
            return node;
        } else {
            return new XmlSchemaPathNode<U, V>(direction, previous, state);
        }
    }

    private XmlSchemaDocumentNode<U> createDocumentNode(XmlSchemaDocumentNode<U> parent,
                                                        XmlSchemaStateMachineNode state) {

        if (!unusedDocNodes.isEmpty()) {
            XmlSchemaDocumentNode<U> node = unusedDocNodes.remove(unusedDocNodes.size() - 1);
            node.set(parent, state);
            return node;
        } else {
            return new XmlSchemaDocumentNode<U>(parent, state);
        }
    }

    void recycleDocumentNode(XmlSchemaDocumentNode<U> node) {
        if (node.getParent() != null) {
            final Map<Integer, XmlSchemaDocumentNode<U>> siblings = node.getParent().getChildren();

            for (Map.Entry<Integer, XmlSchemaDocumentNode<U>> sibling : siblings.entrySet()) {

                if (sibling.getValue() == node) {
                    siblings.remove(sibling.getKey());
                    break;
                }
            }

            if (node.getChildren() != null) {
                for (Map.Entry<Integer, XmlSchemaDocumentNode<U>> child : node.getChildren().entrySet()) {
                    recycleDocumentNode(child.getValue());
                }
            }
        }

        unusedDocNodes.add(node);
    }
}
