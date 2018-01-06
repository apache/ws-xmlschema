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

/**
 * <h1>Building an XML Document's Conforming Path</h1>
 *
 * This package is dedicated to walking an XML Document using the SAX API,
 * and building a path through its
 * {@link org.apache.ws.commons.schema.XmlSchemaCollection} to determine
 * how the document conforms to its schema.
 * 
 * <p>
 * This is done in four steps:
 *
 * <ol>
 *   <li>
 *     Build a state machine of the
 *     {@link org.apache.ws.commons.schema.XmlSchemaCollection},
 *     represented as
 *     {@link org.apache.ws.commons.schema.docpath.XmlSchemaStateMachineNode}s.
 *   </li>
 *   <li>
 *     Start a SAX-based walk over the XML Document, either via
 *     {@link javax.xml.parsers.SAXParser}, when working with raw XML, or
 *     {@link org.apache.ws.commons.schema.docpath.SaxWalkerOverDom} when
 *     working with an {@link org.w3c.dom.Document}.
 *   </li>
 *   <li>
 *     Use {@link org.apache.ws.commons.schema.docpath.XmlSchemaPathFinder}
 *     to build a series of
 *     {@link org.apache.ws.commons.schema.docpath.XmlSchemaDocumentNode}s
 *     and {@link org.apache.ws.commons.schema.docpath.XmlSchemaPathNode}s
 *     representing a valid walk through the
 *     {@link org.apache.ws.commons.schema.XmlSchemaCollection}.
 *   </li>
 *   <li>
 *     Add any application-specific information to the paths through the
 *     document using
 *     {@link org.apache.ws.commons.schema.docpath.XmlSchemaDocumentNode#setUserDefinedContent(Object)}
 *     and
 *     {@link org.apache.ws.commons.schema.docpath.XmlSchemaPathNode#setUserDefinedContent(Object)}.
 *   </li>
 * </ol>
 *
 * <h2>XmlSchemaStateMachineNode</h2>
 * 
 * {@link org.apache.ws.commons.schema.docpath.XmlSchemaStateMachineNode}
 * represents a single node in the
 * {@link org.apache.ws.commons.schema.XmlSchema} - either an element or a
 * group - and the possible nodes that may follow (children of the element
 * or group).  Only one state machine node will be created by the
 * {@link org.apache.ws.commons.schema.docpath.XmlSchemaStateMachineGenerator}
 * for each {@link org.apache.ws.commons.schema.XmlSchemaElement}, meaning
 * the {@link org.apache.ws.commons.schema.docpath.XmlSchemaStateMachineNode}s
 * may loop back on themselves if an XML element is a child of itself.
 *
 * <h2>XmlSchemaDocumentNode</h2>
 * 
 * As {@link org.apache.ws.commons.schema.docpath.XmlSchemaStateMachineNode}s
 * represent the XML Schema,
 * {@link org.apache.ws.commons.schema.docpath.XmlSchemaDocumentNode}s
 * represent the XML Schema as applied to an XML Document.  Each node
 * represents all occurrences of the node in the XML Document, and a different
 * set of children can be requested, one for each occurrence.  Document nodes
 * never loop back on themselves; if an element is a child of itself, a new
 * document node instance will be created at each level in the tree.
 *
 * <p>
 * As a result, the
 * {@link org.apache.ws.commons.schema.docpath.XmlSchemaDocumentNode}s form a
 * tree structure, starting with the root node of the document and working
 * downward.  User-defined content can be attached to each node in the tree
 * using the
 * {@link org.apache.ws.commons.schema.docpath.XmlSchemaPathNode#setUserDefinedContent(Object)}.
 * method.
 *
 * <h2>XmlSchemaPathNode</h2>
 *
 * Where {@link org.apache.ws.commons.schema.docpath.XmlSchemaDocumentNode}s
 * represent the XML Schema structure used to describe the XML Document,
 * {@link org.apache.ws.commons.schema.docpath.XmlSchemaPathNode}s define the
 * actual walk through the XML Schema taken to represent that document.
 * <p>
 * Paths may go in four
 * {@link org.apache.ws.commons.schema.docpath.XmlSchemaPathNode.Direction}s:
 *
 * <ul>
 *   <li>
 *     {@link org.apache.ws.commons.schema.docpath.XmlSchemaPathNode.Direction#CHILD}:
 *     The path moves from the current node in the tree (an element or group)
 *     to one of its children.
 *   <li>
 *   <li>
 *     {@link org.apache.ws.commons.schema.docpath.XmlSchemaPathNode.Direction#PARENT}:
 *     The path moves from the current node in the tree to its parent.  If
 *     moving to the parent of an element, this closes the tag.
 *   </li>
 *   <li>
 *     {@link org.apache.ws.commons.schema.docpath.XmlSchemaPathNode.Direction#SIBLING}:
 *     This initiates a new occurrence of the current (wildcard) element or
 *     group.  If creating a sibling (wildcard) element, this closes the tag of
 *     the existing element and opens a new one of the same name.
 *   </li>
 *   <li>
 *     {@link org.apache.ws.commons.schema.docpath.XmlSchemaPathNode.Direction#CONTENT}:
 *     This represents content inside an element (not children tags).  This
 *     will either be the simple content of a simple element, or the text
 *     contained inside a mixed element.  Mixed content may occur as either
 *     a direct child of the owning element, or inside one of the owning
 *     element's child groups.
 *   </li>
 * </ul>
 *
 * <h2>XmlSchemaPathFinder</h2>
 * 
 * {@link org.apache.ws.commons.schema.docpath.XmlSchemaPathFinder} builds
 * {@link org.apache.ws.commons.schema.docpath.XmlSchemaDocumentNode}s
 * and {@link org.apache.ws.commons.schema.docpath.XmlSchemaPathNode}s
 * from {@link org.apache.ws.commons.schema.docpath.XmlSchemaStateMachineNode}
 * during a SAX walk of an XML document.  In addition to confirming the XML
 * Document conforms to its schema's structure, it will also confirm attribute
 * and element content conform to its expected type.
 *
 * <p>
 * <b>Note:</b> This is done entirely over a SAX walk, meaning the source need
 * not be an XML Document at all.  Any data structure that can be traversed
 * via a SAX walk can be confirmed to conform against an expected XML Schema.
 *
 * <h2>SaxWalkerOverDom</h2>
 *
 * This allows SAX-based walks over {@link org.w3c.dom.Document} objects.
 * One can use this in conjunction with
 * {@link org.apache.ws.commons.schema.docpath.XmlSchemaPathFinder} to
 * confirm a document parsed using a {@link javax.xml.parsers.DocumentBuilder}
 * conforms to its XML Schema.
 *
 * <h2>DomBuilderFromSax</h2>
 *
 * In the reverse direction, one can use
 * {@link org.apache.ws.commons.schema.docpath.DomBuilderFromSax} to build an
 * {@link org.w3c.dom.Document} based on an XML Schema and a SAX walk over
 * content that represents a document conforming to that
 * {@link org.apache.ws.commons.schema.XmlSchema} .  For best results, use
 * {@link org.apache.ws.commons.schema.docpath.XmlSchemaStateMachineGenerator}
 * to build a mapping of {@link javax.xml.namespace.QName}s to
 * {@link org.apache.ws.commons.schema.docpath.XmlSchemaStateMachineNode}s.
 * This is used by the <code>DomBuilderFromSax</code> to resolve ambiguities
 * in how to generate the XML Document based on the schema.
 */
package org.apache.ws.commons.schema.docpath;