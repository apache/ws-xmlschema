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

import org.apache.ws.commons.schema.constants.Constants;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Walks over a {@link Document} in a SAX style, notifying listeners with SAX
 * events.
 * <p>
 * Because the document has already been processed, only the following methods
 * in the {@link ContentHandler} will be called:
 * <ul>
 * <li>{@link ContentHandler#startDocument()}</li>
 * <li>{@link ContentHandler#startPrefixMapping(String, String)}</li>
 * <li>{@link ContentHandler#startElement(String, String, String, Attributes)}</li>
 * <li>{@link ContentHandler#characters(char[], int, int)}</li>
 * <li>{@link ContentHandler#endElement(String, String, String)}</li>
 * <li>{@link ContentHandler#endPrefixMapping(String)}</li>
 * <li>{@link ContentHandler#endDocument()}</li>
 * </ul>
 */
public final class SaxWalkerOverDom {

    private List<ContentHandler> listeners;

    private static class Attr {

        final QName qName;
        final String qualifiedName;
        final String value;

        Attr(String namespace, String localName, String qualName, String val) {
            qName = new QName(namespace, localName);
            qualifiedName = qualName;
            value = val;
        }

        Attr(Node node) {
            this(node.getNamespaceURI(), node.getLocalName(), node.getNodeName(), node.getNodeValue());
        }
    }

    private static class DomAttrsAsSax implements org.xml.sax.Attributes {

        private final List<Attr> attributes;
        private final Map<String, Attr> attrsByQualifiedName;
        private final Map<QName, Attr> attrsByQName;
        private final Map<String, Integer> indexByQualifiedName;
        private final Map<QName, Integer> indexByQName;

        DomAttrsAsSax(NamedNodeMap domAttrs) throws SAXException {
            attributes = new ArrayList<Attr>();
            attrsByQualifiedName = new HashMap<String, Attr>();
            attrsByQName = new HashMap<QName, Attr>();

            indexByQualifiedName = new HashMap<String, Integer>();
            indexByQName = new HashMap<QName, Integer>();

            if (domAttrs != null) {
                for (int attrIdx = 0; attrIdx < domAttrs.getLength(); ++attrIdx) {
                    final Node domAttr = domAttrs.item(attrIdx);

                    if (Constants.XMLNS_ATTRIBUTE_NS_URI.equals(domAttr.getNamespaceURI())) {

                        // Namespace declarations will be handled separately.
                        continue;
                    }

                    final Attr attribute = new Attr(domAttr);
                    attributes.add(attribute);

                    attrsByQualifiedName.put(attribute.qualifiedName, attribute);
                    attrsByQName.put(attribute.qName, attribute);

                    indexByQualifiedName.put(attribute.qualifiedName, attrIdx);
                    indexByQName.put(attribute.qName, attrIdx);
                }
            }
        }

        @Override
        public int getLength() {
            return attributes.size();
        }

        @Override
        public String getURI(int index) {
            if (attributes.size() <= index) {
                return null;
            } else {
                return attributes.get(index).qName.getNamespaceURI();
            }
        }

        @Override
        public String getLocalName(int index) {
            if (attributes.size() <= index) {
                return null;
            } else {
                return attributes.get(index).qName.getLocalPart();
            }
        }

        @Override
        public String getQName(int index) {
            if (attributes.size() <= index) {
                return null;
            } else {
                return attributes.get(index).qualifiedName;
            }
        }

        @Override
        public String getType(int index) {
            if (attributes.size() <= index) {
                return null;
            } else {
                return "CDATA"; // We do not know the type information.
            }
        }

        @Override
        public String getValue(int index) {
            if (attributes.size() <= index) {
                return null;
            } else {
                return attributes.get(index).value;
            }
        }

        @Override
        public int getIndex(String uri, String localName) {
            if ((uri == null) || (localName == null)) {
                return -1;
            }

            final QName qName = new QName(uri, localName);
            final Integer index = indexByQName.get(qName);

            if (index == null) {
                return -1;
            } else {
                return index;
            }
        }

        @Override
        public int getIndex(String qName) {
            if (qName == null) {
                return -1;
            }

            final Integer index = indexByQualifiedName.get(qName);
            if (index == null) {
                return -1;
            } else {
                return index;
            }
        }

        @Override
        public String getType(String uri, String localName) {
            if ((uri == null) || (localName == null)) {
                return null;
            } else {
                final Attr attr = attrsByQName.get(new QName(uri, localName));
                return (attr == null) ? null : "CDATA";
            }
        }

        @Override
        public String getType(String qName) {
            if (qName == null) {
                return null;
            } else {
                final Attr attr = attrsByQualifiedName.get(qName);
                return (attr == null) ? null : "CDATA";
            }
        }

        @Override
        public String getValue(String uri, String localName) {
            if ((uri == null) || (localName == null)) {
                return null;
            } else {
                final Attr attr = attrsByQName.get(new QName(uri, localName));
                return (attr == null) ? null : attr.value;
            }
        }

        @Override
        public String getValue(String qName) {
            if (qName == null) {
                return null;
            } else {
                final Attr attr = attrsByQualifiedName.get(qName);
                return (attr == null) ? null : attr.value;
            }
        }
    }

    /**
     * Constructs a new <code>SaxWalkerOverDom</code>.
     */
    public SaxWalkerOverDom() {
        listeners = null;
    }

    /**
     * Constructs a new <code>SaxWalkerOverDom</code> with the provided
     * {@link ContentHandler} to send SAX events.
     *
     * @param contentHandler The content handler to send events to.
     */
    public SaxWalkerOverDom(ContentHandler contentHandler) {
        this();
        listeners = new ArrayList<ContentHandler>(1);
        listeners.add(contentHandler);
    }

    /**
     * Constructs a new <code>SaxWalkerOverDom</code>, taking ownership of the
     * list of {@link ContentHandler}s to send events to.
     *
     * @param contentHandlers The list of content handlers to send events to.
     */
    public SaxWalkerOverDom(List<ContentHandler> contentHandlers) {
        this();
        listeners = contentHandlers;
    }

    /**
     * Adds the provided {@link ContentHandler} to the list of content handlers
     * to send events to. If this content handler was already added, it will be
     * sent events twice (or more often).
     *
     * @param contentHandler The content handler to send events to.
     */
    public void addContentHandler(ContentHandler contentHandler) {
        if (listeners == null) {
            listeners = new ArrayList<ContentHandler>(1);
        }
        listeners.add(contentHandler);
    }

    /**
     * Removes the first instance of the provided {@link ContentHandler} from
     * the set of handlers to send events to. If the content handler was added
     * more than once, it will continue to receive events.
     *
     * @param contentHandler The content handler to stop sending events to.
     * @return <code>true</code> if it was found, <code>false</code> if not.
     */
    public boolean removeContentHandler(ContentHandler contentHandler) {
        return listeners != null && listeners.remove(contentHandler);
    }

    /**
     * Walks the provided {@link Document}, sending events to all of the
     * {@link ContentHandler}s as it traverses. If there are no content
     * handlers, this method is a no-op.
     *
     * @param document The {@link Document} to traverse.
     * @throws SAXException if an exception occurs when notifying the handlers.
     */
    public void walk(Document document) throws SAXException {
        if (document == null) {
            throw new IllegalArgumentException("Document cannot be null.");
        }

        if ((listeners == null) || listeners.isEmpty()) {
            return;
        }

        for (ContentHandler listener : listeners) {
            listener.startDocument();
        }

        final List<String> prefixes = startPrefixMappings(document);

        walk(document.getDocumentElement());

        for (ContentHandler listener : listeners) {
            for (String prefix : prefixes) {
                listener.endPrefixMapping(prefix);
            }
            listener.endDocument();
        }
    }

    private void walk(Element element) throws SAXException {
        DomAttrsAsSax attrs = new DomAttrsAsSax(element.getAttributes());

        final List<String> prefixes = startPrefixMappings(element);

        for (ContentHandler listener : listeners) {
            listener.startElement(convertNullToEmptyString(element.getNamespaceURI()),
                                  convertNullToEmptyString(element.getLocalName()),
                                  convertNullToEmptyString(element.getNodeName()), attrs);
        }

        NodeList children = element.getChildNodes();

        for (int childIndex = 0; childIndex < children.getLength(); ++childIndex) {
            Node node = children.item(childIndex);
            if (node instanceof Element) {
                walk((Element)node);
            } else if (node instanceof Text) {
                walk((Text)node);
            } else if (node instanceof org.w3c.dom.Comment) {
                // Ignored.
            } else {
                throw new SAXException("Unrecognized child of " + element.getTagName() + " of type "
                                       + node.getClass().getName());
            }
        }

        for (ContentHandler listener : listeners) {
            listener.endElement(convertNullToEmptyString(element.getNamespaceURI()),
                                convertNullToEmptyString(element.getLocalName()),
                                convertNullToEmptyString(element.getNodeName()));

            for (String prefix : prefixes) {
                listener.endPrefixMapping(prefix);
            }
        }
    }

    private void walk(Text text) throws SAXException {
        /*
         * TODO: getData() may throw a org.w3c.dom.DOMException if the actual
         * text data is too large to fit into a single DOMString (the DOM impl's
         * internal storage of text data). If that's the case, substringData()
         * must be called to retrieve the data in pieces. The documentation does
         * not supply information on the maximum DOMString size; it appears to
         * require trial & error.
         */
        if (text.getLength() > 0) {
            char[] data = text.getData().toCharArray();
            for (ContentHandler listener : listeners) {
                listener.characters(data, 0, data.length);
            }
        }
    }

    private static String convertNullToEmptyString(String input) {
        if (input == null) {
            return "";
        }
        return input;
    }

    private List<String> startPrefixMappings(Node node) throws DOMException, SAXException {

        switch (node.getNodeType()) {
        case Node.DOCUMENT_NODE:
        case Node.ELEMENT_NODE:
            break;
        default:
            throw new IllegalArgumentException("Cannot start prefix mappings for a node of type "
                                               + node.getNodeType());
        }

        final ArrayList<String> prefixes = new ArrayList<String>();

        final NamedNodeMap attrs = node.getAttributes();
        if (attrs == null) {
            return prefixes;
        }

        for (int attrIndex = 0; attrIndex < attrs.getLength(); ++attrIndex) {
            final Node attr = attrs.item(attrIndex);
            final String attrUri = attr.getNamespaceURI();

            if (Constants.XMLNS_ATTRIBUTE_NS_URI.equals(attrUri)) {
                final String localName = attr.getLocalName();
                String prefix = null;

                if (Constants.XMLNS_ATTRIBUTE.equals(localName)) {
                    prefix = Constants.DEFAULT_NS_PREFIX;
                } else {
                    prefix = localName;
                }

                prefixes.add(prefix);

                for (ContentHandler listener : listeners) {
                    listener.startPrefixMapping(prefix, attr.getNodeValue());
                }
            }
        }

        return prefixes;
    }
}
