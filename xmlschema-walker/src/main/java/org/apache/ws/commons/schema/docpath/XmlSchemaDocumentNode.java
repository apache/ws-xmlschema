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
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The <code>XmlSchemaDocumentNode</code> represents a node in the XML Schema as
 * it is used by an XML document. As {@link XmlSchemaPathFinder} walks through
 * an XML document, it builds {@link XmlSchemaPathNode}s representing the path
 * walked, and <code>XmlSchemaDocumentNode</code>s representing where the XML
 * document's elements fall in the XML Schema's sequences, choices, and all
 * groups.
 * <p>
 * While {@link XmlSchemaStateMachineNode}s may loop back on themselves, the
 * <code>XmlSchemaDocumentNode</code>s will not. Likewise, the document nodes
 * form a tree that represent the XML Schema as it is applied to the document.
 * </p>
 * <p>
 * If a single node has multiple occurrences, the children of each occurrence
 * can be retrieved by calling {@link #getChildren(int)}. {@link #getChildren()}
 * returns the child nodes for the final occurrence.
 * </p>
 */
public final class XmlSchemaDocumentNode<U> {

    private XmlSchemaStateMachineNode stateMachineNode;
    private XmlSchemaDocumentNode<U> parent;
    private List<SortedMap<Integer, XmlSchemaDocumentNode<U>>> children;
    private List<XmlSchemaPathNode<U, ?>> visitors;
    private boolean receivedContent;
    private U userDefinedContent;

    XmlSchemaDocumentNode(XmlSchemaDocumentNode<U> parent, XmlSchemaStateMachineNode stateMachineNode) {

        userDefinedContent = null;
        set(parent, stateMachineNode);
    }

    /**
     * Returns the {@link XmlSchemaStateMachineNode} representing the place in
     * the XML Schema that this <code>XmlSchemaDocumentNode</code> represents.
     */
    public XmlSchemaStateMachineNode getStateMachineNode() {
        return stateMachineNode;
    }

    /**
     * The <code>XmlSchemaDocumentNode</code> representing this one's immediate
     * parent.
     */
    public XmlSchemaDocumentNode<U> getParent() {
        return parent;
    }

    /**
     * Retrieves the children in the last occurrence of this node, mapped to
     * their relative position.
     */
    public SortedMap<Integer, XmlSchemaDocumentNode<U>> getChildren() {
        if (children == null) {
            return null;
        } else {
            return getChildren(children.size());
        }
    }

    /**
     * Retrieves the children in the provided occurrence of this node, mapped to
     * their relative position.
     *
     * @param iteration The 1-based occurrence to retrieve children for.
     */
    public SortedMap<Integer, XmlSchemaDocumentNode<U>> getChildren(int iteration) {
        if ((children == null) || (children.size() < iteration) || (iteration < 1)) {
            return null;
        } else {
            return children.get(iteration - 1);
        }
    }

    /**
     * Indicates whether an element has text in it.
     */
    boolean getReceivedContent() {
        return receivedContent;
    }

    /**
     * Sets whether the element has text in it.
     * 
     * @param receivedContent
     */
    void setReceivedContent(boolean receivedContent) {
        this.receivedContent = receivedContent;
    }

    /**
     * A visitor is a CHILD or SIBLING {@link XmlSchemaPathNode} entering this
     * <code>XmlSchemaDocumentNode</code>. This is used to keep track of how
     * many occurrences are active via the current path winding through the
     * schema.
     */
    void addVisitor(XmlSchemaPathNode<U, ?> path) {
        if (path.getDocumentNode() != this) {
            throw new IllegalArgumentException("Path node must have this XmlSchemaDocumentNode "
                                               + "as its document node.");
        }

        switch (path.getDirection()) {
        case CHILD:
        case SIBLING:
            break;
        default:
            throw new IllegalArgumentException("Only CHILD and SIBLING paths may be visitors of an "
                                               + "XmlSchemaDocumentNode, not a " + path.getDirection()
                                               + " path.");
        }

        if (visitors == null) {
            visitors = new ArrayList<XmlSchemaPathNode<U, ?>>(4);
        }

        if (children != null) {
            if (children.size() == visitors.size()) {
                children.add(new TreeMap<Integer, XmlSchemaDocumentNode<U>>());
            } else {
                throw new IllegalStateException(
                                                "Attempted to add a new visitor when the number of occurrences ("
                                                    + children.size()
                                                    + ") did not match the number of existing visitors ("
                                                    + visitors.size() + ").");
            }
        }

        visitors.add(path);
    }

    boolean removeVisitor(XmlSchemaPathNode<U, ?> path) {
        if ((visitors == null) || visitors.isEmpty()) {
            return false;
        }

        if ((children != null) && (visitors.size() != children.size())) {
            throw new IllegalStateException("The number of visitors (" + visitors.size()
                                            + ") does not match the number of occurrences ("
                                            + children.size() + ").");
        }

        int visitorIndex = 0;
        for (; visitorIndex < visitors.size(); ++visitorIndex) {
            if (visitors.get(visitorIndex) == path) {
                break;
            }
        }

        if (visitors.size() == visitorIndex) {
            return false;
        }

        visitors.remove(visitorIndex);

        if (children != null) {
            children.remove(visitorIndex);
        }

        return true;
    }

    /**
     * The total number of occurrences of this
     * <code>XmlSchemaDocumentNode</code> in the underlying document.
     */
    public int getIteration() {
        if ((children != null) && (children.size() != visitors.size())) {
            throw new IllegalStateException("The number of occurrences (" + children.size()
                                            + ") is not equal to the number of visitors (" + visitors.size()
                                            + ").");
        }
        return visitors.size();
    }

    /**
     * Shortcut for calling <code>getStateMachineNode().getMinOccurs()</code>.
     *
     * @see XmlSchemaStateMachineNode#getMinOccurs()
     */
    public long getMinOccurs() {
        return stateMachineNode.getMinOccurs();
    }

    /**
     * Shortcut for calling <code>getStateMachineNode().getMaxOccurs()</code>.
     *
     * @see XmlSchemaStateMachineNode#getMaxOccurs()
     */
    public long getMaxOccurs() {
        return stateMachineNode.getMaxOccurs();
    }

    int getSequencePosition() {
        if ((children == null)
            || (!stateMachineNode.getNodeType().equals(XmlSchemaStateMachineNode.Type.SEQUENCE))) {
            return -1;
        } else if (children.isEmpty()) {
            return 0;
        } else if (children.get(children.size() - 1).isEmpty()) {
            return 0;
        } else {
            return children.get(children.size() - 1).lastKey();
        }
    }

    void set(XmlSchemaDocumentNode<U> parent, XmlSchemaStateMachineNode stateMachineNode) {

        this.parent = parent;
        this.stateMachineNode = stateMachineNode;
        this.receivedContent = false;
        this.visitors = null;

        if ((this.stateMachineNode.getPossibleNextStates() == null)
            || this.stateMachineNode.getPossibleNextStates().isEmpty()) {
            this.children = null;

        } else {
            this.children = new ArrayList<SortedMap<Integer, XmlSchemaDocumentNode<U>>>(1);
        }
    }

    /**
     * Retrieves any user-defined content attached to this
     * <code>XmlSchemaDocumentNode</code>, or <code>null</code> if none.
     */
    public U getUserDefinedContent() {
        return userDefinedContent;
    }

    /**
     * Attaches user-defined content to this <code>XmlSchemaDocumentNode</code>.
     */
    public void setUserDefinedContent(U userDefinedContent) {
        this.userDefinedContent = userDefinedContent;
    }
}
