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

package org.apache.ws.commons.schema.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.ws.commons.schema.constants.Constants;


/**
 * Implementation of {@link NamespaceContext}, which is based on a DOM node.
 */
public final class NodeNamespaceContext implements NamespacePrefixList, Serializable {
    private static final boolean DOM_LEVEL_3;
    
    private static final Collection<String> XML_NS_PREFIX_COLLECTION = Collections
        .singletonList(Constants.XML_NS_PREFIX);
    private static final Collection<String> XMLNS_ATTRIBUTE_COLLECTION = Collections
        .singletonList(Constants.XMLNS_ATTRIBUTE);


    private final Map<String, String> declarations;
    private String[] prefixes;

    static {
        boolean level3 = false;
        try {
            Class cls = Class.forName("org.w3c.dom.UserDataHandler", false, Node.class.getClassLoader());
            Node.class.getMethod("getUserData", new Class[] {
                String.class
            });
            Node.class.getMethod("setUserData", new Class[] {
                String.class, Object.class, cls
            });
            level3 = true;
        } catch (Throwable e) {
            level3 = false;
        }
        DOM_LEVEL_3 = level3;
    }

    /**
     * Creates a new instance with the given nodes context.
     */
    private NodeNamespaceContext(Map<String, String> decls) {
        declarations = decls;
    }


    public static String getNamespacePrefix(Element el, String ns) {
        if (DOM_LEVEL_3) {
            return getNamespacePrefixDomLevel3(el, ns);
        }

        return getNamespaceContext(el).getPrefix(ns);
    }
    private static String getNamespacePrefixDomLevel3(Element el, String ns) {
        return el.lookupPrefix(ns);
    }
    
    
    public static String getNamespaceURI(Element el, String pfx) {
        if ("xml".equals(pfx)) {
            return "http://www.w3.org/XML/1998/namespace";
        } else  if ("xmlns".equals(pfx)) {
            return "http://www.w3.org/2000/xmlns/";
        }
        if (DOM_LEVEL_3) {
            return getNamespaceURIDomLevel3(el, pfx);
        }
        return getNamespaceContext(el).getNamespaceURI(pfx);
    }
    private static String getNamespaceURIDomLevel3(Element el, String pfx) {
        if ("".equals(pfx)) {
            pfx = null;
        }
        return el.lookupNamespaceURI(pfx);
    }
    
    public static NodeNamespaceContext getNamespaceContext(Node pNode) {
        final Map<String, String> declarations = new HashMap<String, String>();
        new PrefixCollector() {
            protected void declare(String pPrefix, String pNamespaceURI) {
                declarations.put(pPrefix, pNamespaceURI);
            }
        } .searchAllPrefixDeclarations(pNode);
        return new NodeNamespaceContext(declarations);
    }

    public String[] getDeclaredPrefixes() {
        if (prefixes == null) {
            Collection<String> keys = declarations.keySet();
            prefixes = keys.toArray(new String[keys.size()]);
        }
        return prefixes;
    }

    public String getNamespaceURI(String pPrefix) {
        if (pPrefix == null) {
            throw new IllegalArgumentException("The prefix must not be null.");
        }
        if (Constants.XML_NS_PREFIX.equals(pPrefix)) {
            return Constants.XML_NS_URI;
        }
        if (Constants.XMLNS_ATTRIBUTE.equals(pPrefix)) {
            return Constants.XMLNS_ATTRIBUTE_NS_URI;
        }
        final String uri = declarations.get(pPrefix);
        return uri == null ? Constants.NULL_NS_URI : uri;
    }

    public String getPrefix(String pNamespaceURI) {
        if (pNamespaceURI == null) {
            throw new IllegalArgumentException("The namespace URI must not be null.");
        }
        if (Constants.XML_NS_URI.equals(pNamespaceURI)) {
            return Constants.XML_NS_PREFIX;
        }
        if (Constants.XMLNS_ATTRIBUTE_NS_URI.equals(pNamespaceURI)) {
            return Constants.XMLNS_ATTRIBUTE;
        }
        for (Iterator iter = declarations.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry)iter.next();
            if (pNamespaceURI.equals(entry.getValue())) {
                return (String)entry.getKey();
            }
        }
        return null;
    }

    public Iterator<String> getPrefixes(String pNamespaceURI) {
        if (pNamespaceURI == null) {
            throw new IllegalArgumentException("The namespace URI must not be null.");
        }
        if (Constants.XML_NS_URI.equals(pNamespaceURI)) {
            return XML_NS_PREFIX_COLLECTION.iterator();
        }
        if (Constants.XMLNS_ATTRIBUTE_NS_URI.equals(pNamespaceURI)) {
            return XMLNS_ATTRIBUTE_COLLECTION.iterator();
        }
        final List<String> list = new ArrayList<String>();
        for (Map.Entry<String, String> entry : declarations.entrySet()) {
            if (pNamespaceURI.equals(entry.getValue())) {
                list.add(entry.getKey());
            }
        }
        return list.iterator();
    }
}
