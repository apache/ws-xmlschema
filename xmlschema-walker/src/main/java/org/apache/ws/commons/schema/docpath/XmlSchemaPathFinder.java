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
import java.util.Map;

import javax.xml.bind.ValidationException;
import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchemaAny;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.walker.XmlSchemaTypeInfo;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Performs a SAX-based walk through the XML document, determining the
 * interpretation ("path") that best matches the XML Schema.
 * <p>
 * This is a traditional {@link DefaultHandler} that can be attached to either a
 * {@link javax.xml.parsers.SAXParser} during a parse, or to
 * {@link SaxWalkerOverDom} to find paths through an
 * {@link org.w3c.dom.Document}.
 * </p>
 * <p>
 * Because this is a SAX-based walk, the source information need not be an XML
 * document. It can be any data that can be interpreted via a SAX walk. This can
 * be helpful when trying to confirm the source data can be converted back into
 * XML.
 * </p>
 */
public final class XmlSchemaPathFinder<U, V> extends DefaultHandler {

    /*
     * If a group loops back on itself, we don't want to loop until the stack
     * overflows looking for a valid match. We will stop looking once we reach
     * MAX_DEPTH.
     */
    private static final int MAX_DEPTH = 256;

    private final XmlSchemaNamespaceContext nsContext;

    private XmlSchemaPathNode<U, V> rootPathNode;

    private XmlSchemaPathNode<U, V> currentPath;

    private ArrayList<TraversedElement> traversedElements;
    private ArrayList<DecisionPoint<U, V>> decisionPoints;

    private ArrayList<QName> elementStack;
    private ArrayList<QName> anyStack;

    private XmlSchemaPathManager<U, V> pathMgr;

    /*
     * We want to keep track of all of the valid path segments to a particular
     * element, but we do not want to stomp on the very first node until we know
     * which path we want to follow. Likewise, we want to keep the first node in
     * the segment without a "next" node, but every node after that we wish to
     * chain together. To accomplish this, we start with a base node at the end
     * and "prepend" previous nodes until we work our way back to the beginning.
     * When we prepend a node, we link the previous start node to the node
     * directly after it, while leaving the new start node unlinked. Path
     * segments may also be recycled when a decision point is refuted.
     */
    private static final class PathSegment<U, V> implements Comparable<PathSegment<U, V>> {

        private XmlSchemaPathManager<U, V> pathMgr;
        private XmlSchemaPathNode<U, V> start;
        private XmlSchemaPathNode<U, V> end;
        private XmlSchemaPathNode<U, V> afterStart;
        private int length;
        private int afterStartPathIndex;

        PathSegment(XmlSchemaPathManager<U, V> pathMgr, XmlSchemaPathNode<U, V> node) {
            this.pathMgr = pathMgr;
            set(node);
        }

        @Override
        public int compareTo(PathSegment<U, V> o) { //NOPMD
            if (this == o) {
                return 0;
            }

            /*
             * Paths which end in a wildcard element are of lower rank (higher
             * order) than those that end in elements.
             */
            if (!end.getStateMachineNode().getNodeType()
                .equals(o.getEnd().getStateMachineNode().getNodeType())) {

                if (end.getStateMachineNode().getNodeType().equals(XmlSchemaStateMachineNode.Type.ANY)) {
                    return 1;

                } else if (o.getEnd().getStateMachineNode().getNodeType()
                    .equals(XmlSchemaStateMachineNode.Type.ANY)) {
                    return -1;

                } else {
                    throw new IllegalStateException("The end nodes do not have the same machine node type, so one "
                                                        + "should be an ELEMENT and the other should be an ANY.  "
                                                        + "However, this end node is a "
                                                        + end.getStateMachineNode().getNodeType()
                                                        + " while the other has an end node of type "
                                                        + o.getEnd().getStateMachineNode().getNodeType()
                                                        + ".");
                }
            }

            final int thisLength = getLength();
            final int thatLength = o.getLength();

            /*
             * Paths that walk through earlier sequence group children are
             * preferred over paths that walk through later sequence group
             * children. They provide more options later. Shorter paths are also
             * preferred over longer ones.
             */
            if ((thisLength > 0) && (thatLength > 0)) {
                // Both paths have more than just one element.
                XmlSchemaPathNode<U, V> thisIter = afterStart;
                XmlSchemaPathNode<U, V> thatIter = o.getAfterStart();

                while ((thisIter != null) && (thatIter != null)) {
                    if (thisIter.getDirection().getRank() < thatIter.getDirection().getRank()) {
                        return -1;
                    } else if (thisIter.getDirection().getRank() > thatIter.getDirection().getRank()) {
                        return 1;
                    }

                    if (thisIter.getIndexOfNextNodeState() < thatIter.getIndexOfNextNodeState()) {

                        return -1;

                    } else if (thisIter.getIndexOfNextNodeState() > thatIter.getIndexOfNextNodeState()) {

                        return 1;
                    }

                    thisIter = thisIter.getNext();
                    thatIter = thatIter.getNext();
                }

                if ((thisIter == null) && (thatIter != null)) {
                    // This path is shorter.
                    return -1;
                } else if ((thisIter != null) && (thatIter == null)) {
                    // That path is shorter.
                    return 1;
                }

            } else if ((thisLength == 0) && (thatLength > 0)) {
                // This path is shorter.
                return -1;

            } else if ((thisLength > 0) && (thatLength == 0)) {
                // That path is shorter.
                return 1;

            } else {
                // Both paths have exactly one element.
                if (end.getIndexOfNextNodeState() < o.getEnd().getIndexOfNextNodeState()) {

                    return -1;

                } else if (end.getIndexOfNextNodeState() > o.getEnd().getIndexOfNextNodeState()) {

                    return 1;
                }
            }

            /*
             * If all of our different heuristics do not differentiate the
             * paths, we will return equality. This is fine because
             * Collections.sort(List<T>) is stable, and will preserve the
             * ordering.
             */
            return 0;
        }

        int getLength() {
            if ((length == 0) && (start != end)) {
                for (XmlSchemaPathNode<U, V> iter = afterStart; iter != end; iter = iter.getNext()) {
                    ++length;
                }
                ++length; // (afterStart -> end) + start
            }
            return length;
        }

        /*
         * Prepends a new start node to this segment. We want to clone the
         * previous start node as sibling paths may be sharing it. We also need
         * to know the newStart's path index to reach the clonedStartNode, so we
         * know how to properly link them later.
         */
        void prepend(XmlSchemaPathNode<U, V> newStart, int pathIndexToNextNode) {
            // We need to clone start and make it the afterStart.
            final XmlSchemaPathNode<U, V> clonedStartNode = pathMgr.clone(start);

            if (afterStart != null) {
                afterStart.setPreviousNode(clonedStartNode);
                clonedStartNode.setNextNode(afterStartPathIndex, afterStart);
                afterStart = clonedStartNode;

            } else {
                // This path segment only has one node in it; now it has two.
                end = clonedStartNode;
                afterStart = clonedStartNode;
            }

            start = newStart;
            afterStartPathIndex = pathIndexToNextNode;
            length = 0; // Force a recalculation.
        }

        XmlSchemaPathNode<U, V> getStart() {
            return start;
        }

        XmlSchemaPathNode<U, V> getEnd() {
            return end;
        }

        XmlSchemaPathNode<U, V> getAfterStart() {
            return afterStart;
        }

        int getAfterStartPathIndex() {
            return afterStartPathIndex;
        }

        void set(XmlSchemaPathNode<U, V> node) {
            if (node == null) {
                throw new IllegalArgumentException("DocumentPathNode cannot be null.");
            }

            this.start = node;
            this.end = node;
            this.afterStart = null;
            this.afterStartPathIndex = -1;
            this.length = 0;
        }

        @Override
        public String toString() {
            final StringBuilder str = new StringBuilder("Path Segment: [ ");

            str.append(start.getDirection()).append(" | ");
            str.append(start.getStateMachineNode()).append(" ]");

            if (afterStart != null) {
                XmlSchemaPathNode<U, V> path = afterStart;

                do {
                    str.append(" [").append(path.getDirection()).append(" | ");
                    str.append(path.getStateMachineNode()).append(" ]");

                    path = path.getNext();
                } while (path != null);

            } else {
                str.append(" [").append(end.getDirection()).append(" | ");
                str.append(end.getStateMachineNode()).append(" ]");
            }

            return str.toString();
        }
    }

    /*
     * A <code>DescisionPoint</code> is a location in a document path where an
     * element in the document can be reached by following two or more different
     * traversals through the XML Schema. When we reach such a decision point,
     * we will keep track of the different paths through the XML Schema that
     * reach the element. We will then follow each path, one-by-one from the
     * shortest through the longest, until we find a path that successfully
     * navigates both the document and the schema.
     */
    private static class DecisionPoint<U, V> {

        private final XmlSchemaPathNode<U, V> decisionPoint;
        private final List<PathSegment<U, V>> choices;
        private final int traversedElementIndex;
        private final ArrayList<QName> elementStack;
        private final ArrayList<QName> anyStack;

        DecisionPoint(XmlSchemaPathNode<U, V> decisionPoint, List<PathSegment<U, V>> choices,
                      int traversedElementIndex, ArrayList<QName> elementStack, ArrayList<QName> anyStack) {

            if (decisionPoint == null) {
                throw new IllegalArgumentException("The decision point path node cannot be null.");
            } else if (choices == null) {
                throw new IllegalArgumentException("The set of choice paths to follow cannot be null.");
            } else if (choices.size() < 2) {
                throw new IllegalArgumentException(
                                                   "There must be at least two choices to constitute a decision point"
                                                       + ", not " + choices.size());
            }

            this.decisionPoint = decisionPoint;
            this.choices = choices;
            this.traversedElementIndex = traversedElementIndex;
            this.elementStack = new ArrayList<QName>(elementStack);

            if (anyStack == null) {
                this.anyStack = null;
            } else {
                this.anyStack = new ArrayList<QName>(anyStack);
            }

            java.util.Collections.sort(choices);
        }

        /**
         * Returns the next <code>PathSegment</code> to try, or
         * <code>null</code> if all <code>PathSegment</code>s have been
         * followed.
         */
        PathSegment<U, V> tryNextPath() {
            if (choices.isEmpty()) {
                return null;
            } else {
                return choices.remove(0);
            }
        }

        XmlSchemaPathNode<U, V> getDecisionPoint() {
            return decisionPoint;
        }

        ArrayList<QName> getElementStack() {
            return new ArrayList<QName>(elementStack);
        }

        ArrayList<QName> getAnyStack() {
            return (anyStack == null) ? null : new ArrayList<QName>(anyStack);
        }

        @Override
        public String toString() {
            final String nl = System.getProperty("line.separator");

            final StringBuilder str = new StringBuilder("Decision Point: ");

            str.append(decisionPoint.getDirection()).append(" | ");
            str.append(decisionPoint.getStateMachineNode());
            str.append(" ]").append(nl);

            for (PathSegment<U, V> choice : choices) {
                str.append('\t').append(choice).append(nl);
            }

            return str.toString();
        }
    }

    /*
     * Represents an element-start, element-end, or content we have seen before.
     * When walking our way through the XML Schema, we need this information in
     * order to properly backtrack if we took a wrong path.
     */
    private static class TraversedElement {

        QName elemName;
        Traversal traversal;

        enum Traversal {
            START, CONTENT, END
        }

        TraversedElement(QName elemName, Traversal traversal) {
            this.elemName = elemName;
            this.traversal = traversal;
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder(elemName.toString());
            str.append(" : ").append(traversal);
            return str.toString();
        }
    }

    /*
     * Represents a group's fulfillment state. It is either not fulfilled,
     * meaning more children are required, partially fulfilled, meaning at least
     * the minimum number of children have been added, or completely fulfilled,
     * meaning no more children can be introduced.
     */
    private enum Fulfillment {
        NOT, PARTIAL, COMPLETE
    }

    /**
     * Creates a new <code>XmlToAvroPathCreator</code> with the root
     * {@link XmlSchemaStateMachineNode} to start from when evaluating
     * documents.
     */
    public XmlSchemaPathFinder(XmlSchemaStateMachineNode root) {
        pathMgr = new XmlSchemaPathManager<U, V>();
        nsContext = new XmlSchemaNamespaceContext();

        rootPathNode = pathMgr.createStartPathNode(XmlSchemaPathNode.Direction.CHILD, root);
        rootPathNode.setIteration(1);

        traversedElements = new ArrayList<TraversedElement>();
        elementStack = new ArrayList<QName>();
        currentPath = null;
        decisionPoints = null; // Hopefully there won't be any!
    }

    /**
     * Kick-starts a new SAX walk, building new <code>XmlSchemaPathNode</code>
     * and <code>XmlSchemaDocumentNode</code> traversals in the process.
     *
     * @see DefaultHandler#startDocument()
     */
    @Override
    public void startDocument() throws SAXException {
        currentPath = null;

        traversedElements.clear();
        elementStack.clear();

        if (decisionPoints != null) {
            decisionPoints.clear();
        }
    }

    /**
     * Handles a new prefix mapping in the SAX walk.
     *
     * @see DefaultHandler#startPrefixMapping(String, String)
     */
    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {

        nsContext.addNamespace(prefix, uri);
    }

    /**
     * Handles the end of a prefix mapping in the SAX walk.
     *
     * @see DefaultHandler#endPrefixMapping(String)
     */
    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        nsContext.removeNamespace(prefix);
    }

    /**
     * Find the path through the XML Schema that best matches this element,
     * traversing any relevant groups, and backtracking if necessary.
     *
     * @see DefaultHandler#startElement(String, String, String, Attributes)
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {

        final QName elemQName = new QName(uri, localName);

        try {
            if (currentPath == null) {
                /*
                 * We just started a new document. Likewise we need to move into
                 * a position to process the root element.
                 */
                currentPath = rootPathNode;

            } else if (currentPath.getStateMachineNode().getNodeType()
                .equals(XmlSchemaStateMachineNode.Type.ANY)
                       && (anyStack != null) && !anyStack.isEmpty()) {
                /*
                 * If we are currently following a wildcard element node, we
                 * don't know anything about the element or its children. So it
                 * does not make sense to follow the children or grandchildren
                 * of this element.
                 */
                elementStack.add(elemQName);
                anyStack.add(elemQName);
                return;
            }

            // 1. Find possible paths.
            List<PathSegment<U, V>> possiblePaths = find(currentPath, elemQName);

            PathSegment<U, V> nextPath = null;

            if ((possiblePaths != null) && !possiblePaths.isEmpty()) {
                /*
                 * 2. If multiple paths were returned, add a DecisionPoint. Sort
                 * the paths where paths ending in elements are favored over
                 * element wild cards, and shorter paths are favored over longer
                 * paths.
                 */
                if (possiblePaths.size() > 1) {
                    final DecisionPoint<U, V> decisionPoint =
                        new DecisionPoint<U, V>(currentPath, possiblePaths, traversedElements.size(),
                                                elementStack, anyStack);

                    if (decisionPoints == null) {
                        decisionPoints = new ArrayList<DecisionPoint<U, V>>(4);
                    }
                    decisionPoints.add(decisionPoint);

                    nextPath = decisionPoint.tryNextPath();
                } else {
                    nextPath = possiblePaths.get(0);
                }

                if (nextPath == null) {
                    throw new IllegalStateException("When searching for " + elemQName
                                                    + ", received a set of path choices of size "
                                                    + possiblePaths.size() + ", but the next path is null.");
                }

                followPath(nextPath);

            } else {
                // OR: If no paths are returned:
                while ((decisionPoints != null) && !decisionPoints.isEmpty()) {
                    /*
                     * 2a. Backtrack to the most recent decision point. Remove
                     * the top path (the one we just tried), and select the next
                     * one.
                     */
                    final DecisionPoint<U, V> priorPoint = decisionPoints.get(decisionPoints.size() - 1);

                    nextPath = priorPoint.tryNextPath();

                    if (nextPath == null) {
                        /*
                         * We have tried all paths at this decision point.
                         * Remove it and try the next prior decision point.
                         */
                        decisionPoints.remove(decisionPoints.size() - 1);
                        continue;
                    }

                    pathMgr.unfollowPath(priorPoint.getDecisionPoint());

                    elementStack = priorPoint.getElementStack();
                    anyStack = priorPoint.getAnyStack();

                    /*
                     * Walk through the traversedElements list again from that
                     * index and see if we traverse through all of the elements
                     * in the list, including this one. If not, repeat step 2a,
                     * removing decision points from the stack as we refute
                     * them.
                     */
                    followPath(nextPath);

                    final QName traversedQName = traversedElements.get(priorPoint.traversedElementIndex).elemName;

                    elementStack.add(traversedQName);

                    if (currentPath.getStateMachineNode().getNodeType()
                        .equals(XmlSchemaStateMachineNode.Type.ANY)) {
                        if (anyStack == null) {
                            anyStack = new ArrayList<QName>();
                        }
                        anyStack.add(traversedQName);
                    }

                    int index = priorPoint.traversedElementIndex + 1;
                    for (; index < traversedElements.size(); ++index) {
                        nextPath = null;

                        final TraversedElement te = traversedElements.get(index);

                        if (te.traversal.equals(TraversedElement.Traversal.START)) {
                            possiblePaths = find(currentPath, te.elemName);

                            if ((possiblePaths == null) || possiblePaths.isEmpty()) {
                                break;

                            } else if (possiblePaths.size() > 1) {
                                final DecisionPoint<U, V> decisionPoint =
                                    new DecisionPoint<U, V>(currentPath, possiblePaths, index, elementStack, anyStack);

                                decisionPoints.add(decisionPoint);
                                nextPath = decisionPoint.tryNextPath();

                            } else {
                                nextPath = possiblePaths.get(0);
                            }

                            if (nextPath == null) {
                                throw new IllegalStateException("Somehow after finding a new path to follow,"
                                                                + " that path is null.");
                            }

                            // If we find (a) path(s) that match(es), success!
                            // Follow it.
                            followPath(nextPath);

                            if (currentPath.getStateMachineNode().getNodeType()
                                .equals(XmlSchemaStateMachineNode.Type.ANY)) {
                                if (anyStack == null) {
                                    anyStack = new ArrayList<QName>();
                                }
                                anyStack.add(te.elemName);
                            }

                            elementStack.add(te.elemName);

                        } else if (te.traversal.equals(TraversedElement.Traversal.END)) {
                            final boolean isAny = (currentPath.getStateMachineNode().getNodeType()
                                .equals(XmlSchemaStateMachineNode.Type.ANY)
                                                   && (anyStack != null) && !anyStack.isEmpty());

                            if (!isAny) {
                                walkUpToElement(te.elemName);
                            }

                            walkUpTree(te.elemName);

                            final QName endingElemName = elementStack.remove(elementStack.size() - 1);

                            if (!te.elemName.equals(endingElemName)) {
                                throw new IllegalStateException("Attempted to end element " + te.elemName
                                                                + " but found " + endingElemName
                                                                + " on the stack instead!");
                            }

                            if (isAny) {
                                anyStack.remove(anyStack.size() - 1);
                            }

                        } else if (te.traversal.equals(TraversedElement.Traversal.CONTENT)) {

                            final XmlSchemaPathNode<U, V> contentPath =
                                pathMgr
                                  .addParentSiblingOrContentNodeToPath(currentPath,
                                                                       XmlSchemaPathNode.Direction.CONTENT);

                            currentPath.setNextNode(-1, contentPath);
                            currentPath = contentPath;

                        } else {
                            throw new IllegalStateException("Unrecognized element traversal direction for "
                                                            + te.elemName + " of " + te.traversal + '.');
                        }
                    }

                    if (index < traversedElements.size()) {
                        /*
                         * This attempt is also incorrect. However, we may have
                         * introduced new decision points along the way, and we
                         * want to follow them first. So let's go back around
                         * for another try.
                         */
                        continue;
                    }

                    /*
                     * We made it to the end of the element list! Now try the
                     * current one again.
                     */
                    possiblePaths = find(currentPath, elemQName);

                    if (possiblePaths == null) {
                        // Still incorrect!
                        continue;

                    } else if (possiblePaths.size() > 1) {
                        final DecisionPoint<U, V> decisionPoint =
                            new DecisionPoint<U, V>(currentPath, possiblePaths, traversedElements.size(),
                                                    elementStack, anyStack);

                        decisionPoints.add(decisionPoint);
                        nextPath = decisionPoint.tryNextPath();
                    } else {
                        nextPath = possiblePaths.get(0);
                    }

                    if (nextPath != null) {
                        followPath(nextPath);
                        break;
                    }
                }
            }

            if (nextPath == null) {
                /*
                 * If we go through all prior decision points and are unable to
                 * find one or more paths through the XML Schema that match the
                 * document, throw an error. There is nothing more we can do
                 * here.
                 */
                throw new IllegalStateException(
                                                "Walked through XML Schema and could not find a traversal that "
                                                    + "represented this XML Document.");
            }

            /*
             * Current path now points to the element we just started. Validate
             * its attributes.
             */
            validateAttributes(atts);

            traversedElements.add(new TraversedElement(elemQName, TraversedElement.Traversal.START));
            elementStack.add(elemQName);

            /*
             * If this is element is of type xsd:any, we do not track it or its
             * children. So, we keep a stack of the element and its children,
             * allowing us to know when we leave and can start tracking elements
             * again.
             */
            if (currentPath.getStateMachineNode().getNodeType().equals(XmlSchemaStateMachineNode.Type.ANY)) {
                if (anyStack == null) {
                    anyStack = new ArrayList<QName>();
                }
                anyStack.add(elemQName);
            }

        } catch (Exception e) {
            /*
             * A SAX Exception cannot be thrown because it is caught, and its
             * internal exception is thrown instead. Likewise, any useful info
             * about the error reported in the wrapper SAXException is lost.
             */
            throw new RuntimeException("Error occurred while starting element " + elemQName
                                       + "; traversed path is " + getElementsTraversedAsString(), e);
        }
    }

    /**
     * Adds a new {@link XmlSchemaPathNode.Direction#CONTENT}
     * {@link XmlSchemaPathNode} to the path. Throws an
     * {@link IllegalStateException} if the owning element should not receive
     * content, or the content is empty when it should not be.
     *
     * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
     */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {

        /*
         * If the most recent path node is an element with simple content,
         * confirm these characters match the data type expected. If we are not
         * expecting an element with simple content, and the characters don't
         * represent all whitespace, throw an exception.
         */

        try {
            if (currentPath.getStateMachineNode().getNodeType().equals(XmlSchemaStateMachineNode.Type.ANY)
                && (anyStack != null) && !anyStack.isEmpty()) {
                /*
                 * If this represents a wildcard element, we don't care - we
                 * won't be processing the content.
                 */
                return;
            }

            final XmlSchemaStateMachineNode state = getStateMachineOfOwningElement();

            final XmlSchemaElement element = state.getElement();
            final XmlSchemaTypeInfo elemTypeInfo = state.getElementType();

            final String text = new String(ch, start, length).trim();

            final boolean elemExpectsContent = ((elemTypeInfo != null) && (!elemTypeInfo.getType()
                .equals(XmlSchemaTypeInfo.Type.COMPLEX) || elemTypeInfo.isMixed()));

            if (!elemExpectsContent && (text.length() == 0)) {
                // Nothing to see here.
                return;

            } else if (!elemExpectsContent && (text.length() > 0)) {
                throw new IllegalStateException("Element " + state.getElement().getQName()
                                                + " has no content, but we received \"" + text + "\" for it.");

            } else if (elemExpectsContent && (text.length() == 0) && !state.getElement().isNillable()
                       && !elemTypeInfo.isMixed() && (element.getDefaultValue() == null)
                       && (element.getFixedValue() == null)) {
                throw new IllegalStateException("Received empty text for element "
                                                + state.getElement().getQName()
                                                + " when content was expected.");
            }

            XmlSchemaElementValidator.validateContent(state, text, nsContext);

            currentPath.getDocumentNode().setReceivedContent(true);

            final XmlSchemaPathNode<U, V> contentPath = pathMgr
                .addParentSiblingOrContentNodeToPath(currentPath, XmlSchemaPathNode.Direction.CONTENT);

            currentPath.setNextNode(-1, contentPath);
            currentPath = contentPath;

            traversedElements
                .add(new TraversedElement(element.getQName(), TraversedElement.Traversal.CONTENT));

        } catch (Exception e) {
            throw new RuntimeException("Error occurred while processing characters; traversed path was "
                                       + getElementsTraversedAsString(), e);
        }
    }

    /**
     * Ends the current element. If the current element is not of the provided
     * <code>uri</code> and <code>localName</code>, throws an
     * {@link IllegalStateException}.
     *
     * @see DefaultHandler#endElement(String, String, String)
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        final QName elemQName = new QName(uri, localName);

        try {
            final boolean isAny = (currentPath.getStateMachineNode().getNodeType()
                .equals(XmlSchemaStateMachineNode.Type.ANY)
                                   && (anyStack != null) && !anyStack.isEmpty());

            if (!isAny && !elementStack.get(elementStack.size() - 1).equals(elemQName)) {
                throw new IllegalStateException("Attempting to end element " + elemQName
                                                + " but the stack is expecting "
                                                + elementStack.get(elementStack.size() - 1));
            }

            if (!isAny) {
                walkUpToElement(elemQName);
            }

            final XmlSchemaStateMachineNode state = currentPath.getStateMachineNode();

            if (state.getNodeType().equals(XmlSchemaStateMachineNode.Type.ELEMENT)) {

                // 1. Is this the element we are looking for?
                if (!state.getElement().getQName().equals(elemQName)) {
                    throw new IllegalStateException("We are ending element " + elemQName
                                                    + " but our current position is for element "
                                                    + state.getElement().getQName() + '.');
                }

                // 2. Check the element received the expected content, if any.
                final XmlSchemaTypeInfo elemTypeInfo = state.getElementType();

                final boolean elemExpectsContent = (elemTypeInfo != null)
                                                   && (!elemTypeInfo.getType()
                                                       .equals(XmlSchemaTypeInfo.Type.COMPLEX));

                if (elemExpectsContent && !state.getElement().isNillable()
                    && (state.getElement().getDefaultValue() == null)
                    && (state.getElement().getFixedValue() == null)
                    && !currentPath.getDocumentNode().getReceivedContent()) {
                    throw new IllegalStateException("We are ending element " + elemQName
                                                    + "; it expected to receive content but did not.");
                }
            }

            traversedElements.add(new TraversedElement(elemQName, TraversedElement.Traversal.END));

            elementStack.remove(elementStack.size() - 1);
            if (isAny) {
                anyStack.remove(anyStack.size() - 1);
            }

            if ((anyStack == null) || anyStack.isEmpty()) {
                walkUpTree(elemQName);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error occurred while ending element " + elemQName
                                       + "; traversed path was " + getElementsTraversedAsString(), e);
        }
    }

    /**
     * Called when the XML Document traversal is complete.
     * <p>
     * Confirms all open elements have been closed. If not, throws an
     * {@link IllegalStateException}.
     * </p>
     */
    @Override
    public void endDocument() throws SAXException {
        if (!elementStack.isEmpty()) {
            throw new IllegalStateException("Ended the document but " + elementStack.size()
                                            + " elements have not been closed.");
        }

        pathMgr.clear();

        if (decisionPoints != null) {
            decisionPoints.clear();
        }
    }

    /**
     * Once a traversal completes successfully, this method may be called to
     * retrieve the relevant interpretation of the path through the
     * {@link XmlSchemaStateMachineNode}s.
     * <p>
     * {@link XmlSchemaPathNode#getDocumentNode()} can be called to retrieve the
     * interpretation of the XML Schema as applied to the document; meanwhile
     * the walk through {@link XmlSchemaPathNode}s will show how that schema was
     * traversed.
     * </p>
     */
    public XmlSchemaPathNode<U, V> getXmlSchemaTraversal() {
        return rootPathNode;
    }

    private static <U, V> Fulfillment isPositionFulfilled(XmlSchemaPathNode<U, V> currentPath,
                                                          List<Integer> possiblePaths) {
        boolean completelyFulfilled = true;
        boolean partiallyFulfilled = true;

        final XmlSchemaStateMachineNode state = currentPath.getStateMachineNode();

        if (currentPath.getDocumentNode() == null) {
            // This is the root node. It is not fulfilled.
            partiallyFulfilled = false;
        } else if (currentPath.getDocIteration() >= state.getMinOccurs()) {
            partiallyFulfilled = true;
        } else {
            partiallyFulfilled = false;
        }

        if (currentPath.getDocumentNode() == null) {
            completelyFulfilled = false;
        } else if (currentPath.getDocIteration() == state.getMaxOccurs()) {
            completelyFulfilled = true;
        } else if (currentPath.getDocIteration() > state.getMaxOccurs()) {
            throw new IllegalStateException("Current path's document iteration of "
                                            + currentPath.getDocIteration()
                                            + " is greater than the maximum number of occurrences ("
                                            + state.getMaxOccurs() + ").");

        } else {
            completelyFulfilled = false;
        }

        final List<XmlSchemaStateMachineNode> nextStates = state.getPossibleNextStates();

        Map<Integer, XmlSchemaDocumentNode<U>> children = null;
        if (currentPath.getDocumentNode() != null) {
            children = currentPath.getDocumentNode().getChildren();
        }

        switch (state.getNodeType()) {
        case ELEMENT:
        case ANY:
            // We only needed to perform the occurrence check.
            break;
        case CHOICE:
        case SUBSTITUTION_GROUP: {
            /*
             * If any child meets the minimum number, we are partially
             * fulfilled. If all elements meet the maximum, we are completely
             * fulfilled.
             */
            boolean groupPartiallyFulfilled = false;
            boolean groupCompletelyFulfilled = false;
            for (int stateIndex = 0; stateIndex < nextStates.size(); ++stateIndex) {

                XmlSchemaStateMachineNode nextState = nextStates.get(stateIndex);

                if ((children != null) && children.containsKey(stateIndex)) {
                    final XmlSchemaDocumentNode<U> child = children.get(stateIndex);
                    final int iteration = child.getIteration();
                    if (iteration >= nextState.getMinOccurs()) {
                        groupPartiallyFulfilled = true;
                        if (possiblePaths != null) {
                            possiblePaths.clear();
                            if (iteration < nextState.getMaxOccurs()) {
                                possiblePaths.add(stateIndex);
                            } else {
                                groupCompletelyFulfilled = true;
                            }
                        }
                        break;
                    } else if (possiblePaths != null) {
                        possiblePaths.add(stateIndex);
                    }
                } else {
                    if (nextState.getMinOccurs() == 0) {
                        groupPartiallyFulfilled = true;
                    }
                    if (nextState.getMaxOccurs() == 0) {
                        groupCompletelyFulfilled = true;
                    } else if (possiblePaths != null) {
                        possiblePaths.add(stateIndex);
                    }
                }
            }
            partiallyFulfilled &= groupPartiallyFulfilled;
            completelyFulfilled &= groupCompletelyFulfilled;
            break;
        }
        case ALL: {
            // If all children meet the minimum number, we succeeded.
            for (int stateIndex = 0; stateIndex < nextStates.size(); ++stateIndex) {

                final XmlSchemaStateMachineNode nextState = nextStates.get(stateIndex);

                if ((children != null) && children.containsKey(stateIndex)) {
                    final XmlSchemaDocumentNode<U> child = children.get(stateIndex);
                    final int iteration = child.getIteration();
                    if (iteration < nextState.getMinOccurs()) {
                        partiallyFulfilled = false;
                    }
                    if (iteration < nextState.getMaxOccurs()) {
                        completelyFulfilled = false;
                        if (possiblePaths != null) {
                            possiblePaths.add(stateIndex);
                        }
                    }
                } else {
                    if (nextState.getMinOccurs() > 0) {
                        partiallyFulfilled = false;
                    }
                    if (nextState.getMaxOccurs() > 0) {
                        completelyFulfilled = false;
                        if (possiblePaths != null) {
                            possiblePaths.add(stateIndex);
                        }
                    }
                }
            }

            break;
        }
        case SEQUENCE: {
            // If the sequence is complete, we succeeded.
            int stateIndex = currentPath.getDocSequencePosition();
            if (stateIndex < 0) {
                stateIndex = 0;
            }
            for (; stateIndex < nextStates.size(); ++stateIndex) {
                final XmlSchemaStateMachineNode nextState = nextStates.get(stateIndex);

                if ((children != null) && children.containsKey(stateIndex)) {
                    final XmlSchemaDocumentNode<U> child = children.get(stateIndex);
                    if (child.getIteration() < nextState.getMinOccurs()) {
                        partiallyFulfilled = false;
                    }
                    if (child.getIteration() < nextState.getMaxOccurs()) {
                        completelyFulfilled = false;
                        if (possiblePaths != null) {
                            possiblePaths.add(stateIndex);
                        }
                    }
                } else {
                    if (nextState.getMinOccurs() > 0) {
                        partiallyFulfilled = false;
                    }
                    if (nextState.getMaxOccurs() > 0) {
                        completelyFulfilled = false;
                        if (possiblePaths != null) {
                            possiblePaths.add(stateIndex);
                        }
                    }
                }
            }
            break;
        }
        default:
            throw new IllegalStateException("Current position has a node of unrecognized type \""
                                            + currentPath.getStateMachineNode().getNodeType() + '\"');
        }

        Fulfillment fulfillment = Fulfillment.NOT;
        if (completelyFulfilled) {
            fulfillment = Fulfillment.COMPLETE;
        } else if (partiallyFulfilled) {
            fulfillment = Fulfillment.PARTIAL;
        }
        return fulfillment;
    }

    private List<PathSegment<U, V>> find(XmlSchemaPathNode<U, V> startNode, QName elemQName) {

        final XmlSchemaPathNode<U, V> startOfPath = startNode;

        if (startNode.getStateMachineNode().getNodeType().equals(XmlSchemaStateMachineNode.Type.ELEMENT)
            && !elementStack.isEmpty()
            && startNode.getStateMachineNode().getElement().getQName()
                .equals(elementStack.get(elementStack.size() - 1))) {

            /*
             * We are at an element in an existing document. This is the start
             * node of a child. Likewise we need to move down one level to the
             * children.
             */
            verifyCurrentPositionIsAtElement("Started element " + elemQName);

            if (startNode.getStateMachineNode().getPossibleNextStates() == null) {

                final String elemName = getLeafNodeName(startNode.getStateMachineNode());

                throw new IllegalStateException("Element " + elemName
                                                + " has null children!  Exactly one is expected.");

            } else if (startNode.getStateMachineNode().getPossibleNextStates().isEmpty()) {

                final String elemName = getLeafNodeName(startNode.getStateMachineNode());

                throw new IllegalStateException("Element " + elemName
                                                + " has zero children!  Exactly one is expected.");

            } else if (currentPath.getStateMachineNode().getPossibleNextStates().size() > 1) {

                final String elemName = getLeafNodeName(currentPath.getStateMachineNode());

                throw new IllegalStateException("Element "
                                                + elemName
                                                + " has "
                                                + currentPath.getStateMachineNode().getPossibleNextStates()
                                                    .size() + " children!  Only one was expected.");
            }

            if ((startNode.getDocumentNode() != null) && (startNode.getDocumentNode().getChildren() != null)
                && !startNode.getDocumentNode().getChildren().isEmpty()
                && (startNode.getDocumentNode().getChildren().size() > 1)) {
                throw new IllegalStateException(
                                                "There are multiple children in the document node for element "
                                                    + currentPath.getStateMachineNode().getElement()
                                                        .getQName());
            }

            final XmlSchemaPathNode<U, V> childPath = pathMgr.addChildNodeToPath(startNode, 0);

            startNode.setNextNode(0, childPath);
            startNode = childPath;
        }

        final List<PathSegment<U, V>> choices = find(startNode, elemQName, null);

        if ((choices != null) && (startOfPath != startNode)) {
            /*
             * If we moved down to the children, we need to prepend the path
             * with the original start node.
             */
            for (PathSegment<U, V> choice : choices) {
                choice.prepend(startOfPath, 0);
            }
        }

        return choices;
    }

    private List<PathSegment<U, V>> find(XmlSchemaPathNode<U, V> startNode, QName elemQName,
                                   XmlSchemaStateMachineNode doNotFollow) {

        final ArrayList<Integer> childrenNodes = new ArrayList<Integer>();
        final boolean isFulfilled = !isPositionFulfilled(startNode, childrenNodes).equals(Fulfillment.NOT);

        // First, try searching down the tree.
        List<PathSegment<U, V>> choices = null;
        List<PathSegment<U, V>> currChoices = null;

        if (startNode.getIteration() > startNode.getDocIteration()) {
            choices = find(startNode, elemQName, 0);
        } else {
            for (Integer childPath : childrenNodes) {
                if (doNotFollow == startNode.getStateMachineNode().getPossibleNextStates().get(childPath)) {
                    /*
                     * We are coming up from a child node; do not traverse back
                     * down to that child.
                     */
                    continue;
                }
                final XmlSchemaPathNode<U, V> currPath = pathMgr.addChildNodeToPath(startNode, childPath);

                currChoices = find(currPath, elemQName, 0);
                if (currChoices != null) {
                    for (PathSegment<U, V> choice : currChoices) {
                        choice.prepend(startNode, childPath);
                    }

                    if (choices == null) {
                        choices = currChoices;
                    } else {
                        choices.addAll(currChoices);
                    }
                }
            }
        }

        // Second, if the node is currently fulfilled, try siblings and parents.
        if (isFulfilled) {

            // Try siblings.
            if (startNode.getIteration() < startNode.getMaxOccurs()) {
                final XmlSchemaPathNode<U, V> siblingPath = pathMgr
                    .addParentSiblingOrContentNodeToPath(startNode, XmlSchemaPathNode.Direction.SIBLING);
                siblingPath.setIteration(startNode.getIteration() + 1);

                currChoices = find(siblingPath, elemQName, 0);
                if (currChoices != null) {
                    for (PathSegment<U, V> choice : currChoices) {
                        choice.prepend(startNode, -1);
                    }

                    if (choices == null) {
                        choices = currChoices;
                    } else {
                        choices.addAll(currChoices);
                    }
                }
            }

            // Try parent.
            if (startNode.getDocumentNode().getParent() == null) {
                // This is the root element; there is no parent.
                return choices;
            }

            final XmlSchemaPathNode<U, V> path = pathMgr
                .addParentSiblingOrContentNodeToPath(startNode, XmlSchemaPathNode.Direction.PARENT);

            if (path.getStateMachineNode().getNodeType().equals(XmlSchemaStateMachineNode.Type.ELEMENT)
                && path.getStateMachineNode().getElement().getQName()
                    .equals(elementStack.get(elementStack.size() - 1))) {
                return choices;
            }

            final List<PathSegment<U, V>> pathsOfParent = find(path, elemQName, startNode.getStateMachineNode());

            if (pathsOfParent != null) {
                for (PathSegment<U, V> choice : pathsOfParent) {
                    choice.prepend(startNode, -1);
                }

                if (choices == null) {
                    choices = pathsOfParent;
                } else {
                    choices.addAll(pathsOfParent);
                }
            } else {
                // path would not have been recycled at a lower level.
                pathMgr.recyclePathNode(path);
            }
        }

        return choices;
    }

    private List<PathSegment<U, V>> find(XmlSchemaPathNode<U, V> startNode, QName elemQName, int currDepth) {

        final XmlSchemaStateMachineNode state = startNode.getStateMachineNode();

        if (currDepth > MAX_DEPTH) {
            /*
             * We are likely in an infinite recursive loop looking for an
             * element in a group whose definition includes itself. Likewise,
             * we'll stop here and say we were unable to find the element we
             * were looking for.
             */
            return null;

        } else if (startNode.getStateMachineNode() != state) {

            throw new IllegalStateException("While searching for " + elemQName
                                            + ", the DocumentPathNode state machine ("
                                            + startNode.getStateMachineNode().getNodeType()
                                            + ") does not match the tree node (" + state.getNodeType() + ").");

        } else if (startNode.getIteration() <= startNode.getDocIteration()) {
            throw new IllegalStateException("While searching for " + elemQName
                                            + ", the DocumentPathNode iteration (" + startNode.getIteration()
                                            + ") should be greater than the tree node's iteration ("
                                            + startNode.getDocIteration()
                                            + ").  Current state machine position is " + state.getNodeType());

        } else if (state.getMaxOccurs() < startNode.getIteration()) {
            return null;
        }

        // If this is a group, confirm it has children.
        if (!state.getNodeType().equals(XmlSchemaStateMachineNode.Type.ELEMENT)
            && !state.getNodeType().equals(XmlSchemaStateMachineNode.Type.ANY)
            && ((state.getPossibleNextStates() == null) || state.getPossibleNextStates().isEmpty())) {

            throw new IllegalStateException("Group " + state.getNodeType()
                                            + " has no children.  Found when processing " + elemQName);
        }

        List<PathSegment<U, V>> choices = null;

        switch (state.getNodeType()) {
        case ELEMENT: {
            if (state.getElement().getQName().equals(elemQName)
                && startNode.getIteration() <= state.getMaxOccurs()) {

                choices = new ArrayList<PathSegment<U, V>>(1);
                choices.add(new PathSegment<U, V>(pathMgr, startNode));
            }
        }
            break;

        case SEQUENCE: {
            // Find the next one in the sequence that matches.
            int position = startNode.getDocSequencePosition();

            if (startNode.getDocIteration() > startNode.getMaxOccurs()) {
                throw new IllegalStateException("Somehow the document iteration for "
                                                + startNode.getStateMachineNode() + " of "
                                                + startNode.getDocIteration()
                                                + " exceeds the maximum number of occurrences of "
                                                + startNode.getMaxOccurs());

            } else if (startNode.getDocIteration() == startNode.getMaxOccurs()) {
                ++position;
            }

            for (int stateIndex = position; stateIndex < startNode.getStateMachineNode()
                .getPossibleNextStates().size(); ++stateIndex) {

                // Process child.
                final XmlSchemaPathNode<U, V> nextPath = pathMgr.addChildNodeToPath(startNode, stateIndex);

                /*
                 * Both the tree node's and the document path node's state
                 * machine nodes should point to the same state machine node in
                 * memory.
                 */
                if (nextPath.getIteration() > nextPath.getMaxOccurs()) {
                    throw new IllegalStateException("Reached a sequence group when searching for "
                                                    + elemQName
                                                    + " whose iteration at the current position ("
                                                    + nextPath.getIteration() + ") was already maxed out ("
                                                    + nextPath.getMaxOccurs() + ").  Was at position "
                                                    + stateIndex + "; tree node's starting position was "
                                                    + startNode.getDocSequencePosition());
                }

                final boolean reachedMinOccurs = (nextPath.getDocIteration() >= nextPath.getMinOccurs());

                final List<PathSegment<U, V>> seqPaths = find(nextPath, elemQName, currDepth + 1);

                if (seqPaths != null) {
                    for (PathSegment<U, V> seqPath : seqPaths) {
                        seqPath.prepend(startNode, stateIndex);
                    }

                    // nextPath was cloned by all path segments, so it can be
                    // recycled.
                    pathMgr.recyclePathNode(nextPath);

                    if (choices == null) {
                        choices = seqPaths;
                    } else {
                        choices.addAll(seqPaths);
                    }
                }

                if (!reachedMinOccurs) {

                    /*
                     * If we have not traversed this node in the sequence the
                     * minimum number of times, we cannot advance to the next
                     * node in the sequence.
                     */
                    break;
                }
            }

            break;
        }

        case ALL:
        case SUBSTITUTION_GROUP:
        case CHOICE: {
            /*
             * All groups only contain elements. Find one that matches. The
             * max-occurrence check will confirm it wasn't already selected.
             * Choice groups may have multiple paths through its children which
             * are valid. In addition, a wild card ("any" element) may be a
             * child of any group, thus creating another decision point.
             */
            for (int stateIndex = 0; stateIndex < state.getPossibleNextStates().size(); ++stateIndex) {

                final XmlSchemaStateMachineNode nextState = state.getPossibleNextStates().get(stateIndex);

                if (state.getNodeType().equals(XmlSchemaStateMachineNode.Type.ALL)
                    && !nextState.getNodeType().equals(XmlSchemaStateMachineNode.Type.ELEMENT)
                    && !nextState.getNodeType().equals(XmlSchemaStateMachineNode.Type.SUBSTITUTION_GROUP)
                    && !nextState.getNodeType().equals(XmlSchemaStateMachineNode.Type.ANY)) {

                    throw new IllegalStateException(
                                                    "While searching for "
                                                        + elemQName
                                                        + ", encountered an All group which contained a child of type "
                                                        + nextState.getNodeType() + '.');
                }

                final XmlSchemaPathNode<U, V> nextPath = pathMgr.addChildNodeToPath(startNode, stateIndex);

                final List<PathSegment<U, V>> choicePaths = find(nextPath, elemQName, currDepth + 1);

                if (choicePaths != null) {
                    for (PathSegment<U, V> choicePath : choicePaths) {
                        choicePath.prepend(startNode, stateIndex);
                    }

                    // nextPath was cloned by all path segments, so it can be
                    // recycled.
                    pathMgr.recyclePathNode(nextPath);

                    if (choices == null) {
                        choices = choicePaths;
                    } else {
                        choices.addAll(choicePaths);
                    }
                }
            }

            break;
        }
        case ANY: {
            /*
             * If the XmlSchemaAny namespace and processing rules apply, this
             * element matches. False otherwise.
             */
            if (traversedElements.size() < 2) {
                throw new IllegalStateException("Reached a wildcard element while searching for " + elemQName
                                                + ", but we've only seen " + traversedElements.size()
                                                + " element(s)!");
            }

            final XmlSchemaAny any = state.getAny();

            if (any.getNamespace() == null) {
                choices = new ArrayList<PathSegment<U, V>>(1);
                choices.add(new PathSegment<U, V>(pathMgr, startNode));
                break;
            }

            boolean needTargetNamespace = false;
            boolean matches = false;
            boolean matchOnNotTargetNamespace = false;

            List<String> validNamespaces = null;

            if (any.getNamespace().equals("##any")) {
                // Any namespace is valid. This matches.
                matches = true;

            } else if (any.getNamespace().equals("##other")) {
                needTargetNamespace = true;
                matchOnNotTargetNamespace = true;
                validNamespaces = new ArrayList<String>(1);

            } else {
                final String[] namespaces = any.getNamespace().trim().split(" ");
                validNamespaces = new ArrayList<String>(namespaces.length);
                for (String namespace : namespaces) {
                    if ("##targetNamespace".equals(namespace)) {
                        needTargetNamespace = true;

                    } else if ("##local".equals(namespace) && (elemQName.getNamespaceURI() == null)) {

                        matches = true;

                    } else {
                        validNamespaces.add(namespace);
                    }
                }
            }

            if (!matches) {
                if (needTargetNamespace) {
                    validNamespaces.add(any.getTargetNamespace());
                }

                matches = validNamespaces.contains(elemQName.getNamespaceURI());

                if (matchOnNotTargetNamespace) {
                    matches = !matches;
                }
            }

            if (matches) {
                choices = new ArrayList<PathSegment<U, V>>(1);
                choices.add(new PathSegment<U, V>(pathMgr, startNode));
            }
        }
            break;
        default:
            throw new IllegalStateException("Unrecognized node type " + state.getNodeType()
                                            + " when processing element " + elemQName);
        }

        if ((choices == null) && (currDepth > 0)) {
            pathMgr.recyclePathNode(startNode);
        }
        return choices;
    }

    /*
     * Walks up the tree from the current element to the prior one. Confirms the
     * provided QName matches the current one before traversing. If currElem is
     * null, the current position must be a wildcard element.
     */
    private void walkUpTree(QName currElem) {
        final XmlSchemaStateMachineNode state = currentPath.getStateMachineNode();

        switch (state.getNodeType()) {
        case ANY:
            break;
        case ELEMENT:
            if (!state.getElement().getQName().equals(currElem)) {
                throw new IllegalStateException("We expected to walk upwards from element " + currElem
                                                + ", but our current element is "
                                                + state.getElement().getQName());
            }
            break;
        default:
            throw new IllegalStateException("We expected to walk upwards from element " + currElem
                                            + ", but our current position is in a node of type "
                                            + state.getNodeType());
        }

        XmlSchemaDocumentNode<U> iter = currentPath.getDocumentNode();
        XmlSchemaPathNode<U, V> path = currentPath;

        do {
            if (iter.getIteration() < iter.getStateMachineNode().getMaxOccurs()) {
                break;
            }

            if (!isPositionFulfilled(path, null).equals(Fulfillment.COMPLETE)) {
                break;
            }

            iter = iter.getParent();

            if (iter == null) {
                // We are exiting the root node. Nothing to see here!
                break;
            }

            final XmlSchemaPathNode<U, V> nextPath = pathMgr
                .addParentSiblingOrContentNodeToPath(path, XmlSchemaPathNode.Direction.PARENT);

            path.setNextNode(-1, nextPath);
            path = nextPath;

        } while (!iter.getStateMachineNode().getNodeType().equals(XmlSchemaStateMachineNode.Type.ELEMENT));

        currentPath = path;
    }

    private void walkUpToElement(QName element) {
        XmlSchemaDocumentNode<U> iter = currentPath.getDocumentNode();

        if (iter.getStateMachineNode().getNodeType().equals(XmlSchemaStateMachineNode.Type.ELEMENT)
            && iter.getStateMachineNode().getElement().getQName().equals(element)) {
            // We are already at the element!
            return;
        }

        do {

            iter = iter.getParent();
            if (iter != null) {
                final XmlSchemaPathNode<U, V> nextPath = pathMgr
                    .addParentSiblingOrContentNodeToPath(currentPath, XmlSchemaPathNode.Direction.PARENT);

                currentPath.setNextNode(-1, nextPath);
                currentPath = nextPath;
            }
        } while ((iter != null)
                 && !iter.getStateMachineNode().getNodeType().equals(XmlSchemaStateMachineNode.Type.ELEMENT));

        if (!currentPath.getStateMachineNode().getNodeType().equals(XmlSchemaStateMachineNode.Type.ELEMENT)
            || !currentPath.getStateMachineNode().getElement().getQName().equals(element)) {
            throw new IllegalStateException("Walked up tree and stopped at node "
                                            + currentPath.getStateMachineNode()
                                            + ", which does not represent element " + element);
        }
    }

    private void followPath(PathSegment<U, V> path) {
        switch (path.getEnd().getStateMachineNode().getNodeType()) {
        case ELEMENT:
        case ANY:
            break;
        default:
            throw new IllegalStateException("Path does not end in an element or a wildcard element.");
        }

        // Join the start element with the new path.
        XmlSchemaPathNode<U, V> startNode = path.getStart();

        if (path.getAfterStart() != null) {
            startNode.setNextNode(path.getAfterStartPathIndex(), path.getAfterStart());
        }

        pathMgr.followPath(startNode);

        currentPath = path.getEnd();
    }

    /*
     * Perhaps this would be better implemented as a bunch of starting and
     * ending tags on separate lines, properly indented, to generate an XML
     * document similar to the one being parsed? An idea to consider later.
     */
    private String getElementsTraversedAsString() {
        final StringBuilder traversed = new StringBuilder("[");
        if ((traversedElements != null) && !traversedElements.isEmpty()) {
            for (int i = 0; i < traversedElements.size() - 1; ++i) {
                traversed.append(traversedElements.get(i)).append(" | ");
            }
            traversed.append(traversedElements.get(traversedElements.size() - 1));
        }
        traversed.append(" ]");

        return traversed.toString();
    }

    private void verifyCurrentPositionIsAtElement(String errMsgPrefix) {
        if (!currentPath.getStateMachineNode().getNodeType().equals(XmlSchemaStateMachineNode.Type.ELEMENT)

        && !currentPath.getStateMachineNode().getNodeType().equals(XmlSchemaStateMachineNode.Type.ANY)) {

            throw new IllegalStateException(errMsgPrefix + " when our current position in the tree is a "
                                            + currentPath.getStateMachineNode().getNodeType() + '.');
        }
    }

    private String getLeafNodeName(XmlSchemaStateMachineNode node) {
        if (!node.getNodeType().equals(XmlSchemaStateMachineNode.Type.ELEMENT)
            && !node.getNodeType().equals(XmlSchemaStateMachineNode.Type.ANY)) {

            throw new IllegalStateException(
                                            "State machine node needs to be an element or a wildcard element, "
                                                + "not a " + currentPath.getStateMachineNode().getNodeType()
                                                + '.');
        }

        String elemName = "a wildcard element";
        if (node.getNodeType().equals(XmlSchemaStateMachineNode.Type.ELEMENT)) {
            elemName = node.getElement().getQName().toString();
        }
        return elemName;
    }

    private XmlSchemaStateMachineNode getStateMachineOfOwningElement() {
        QName element = elementStack.get(elementStack.size() - 1);
        XmlSchemaDocumentNode<U> iter = currentPath.getDocumentNode();

        if (iter.getStateMachineNode().getNodeType().equals(XmlSchemaStateMachineNode.Type.ELEMENT)
            && iter.getStateMachineNode().getElement().getQName().equals(element)) {
            // We are already at the element!
            return currentPath.getStateMachineNode();
        }

        do {
            iter = iter.getParent();
        } while ((iter != null)
                 && !iter.getStateMachineNode().getNodeType().equals(XmlSchemaStateMachineNode.Type.ELEMENT));

        if (iter == null || !iter.getStateMachineNode().getNodeType().equals(XmlSchemaStateMachineNode.Type.ELEMENT)
            || !iter.getStateMachineNode().getElement().getQName().equals(element)) {
            throw new IllegalStateException("Walked up tree and stopped at node "
                                            + currentPath.getStateMachineNode()
                                            + ", which does not represent element " + element);
        }

        return iter.getStateMachineNode();
    }

    private void validateAttributes(Attributes attrs) {
        if (currentPath.getStateMachineNode().getNodeType().equals(XmlSchemaStateMachineNode.Type.ANY)) {
            // No validation is performed on ANY elements.
            return;
        }

        try {
            XmlSchemaElementValidator.validateAttributes(currentPath.getStateMachineNode(), attrs, nsContext);
        } catch (ValidationException ve) {
            throw new IllegalStateException(
                                            "Cannot validate attributes of "
                                                + currentPath.getStateMachineNode().getElement().getQName()
                                                + '.', ve);
        }
    }
}
