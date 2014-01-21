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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ws.commons.schema.constants.Constants;

/**
 * This class maps from a prefix to an object that is either a String or a URI.
 * In fact, it will work with anything with a toString result that is useful
 * as a namespace URI.
 */
public class NamespaceMap extends HashMap<String, Object> implements NamespacePrefixList {

    private static final long serialVersionUID = 1L;

    public NamespaceMap() {
    }

    public NamespaceMap(Map<String, Object> map) {
        super(map);
    }

    public void add(String prefix, String namespaceURI) {
        put(prefix, namespaceURI);
    }

    public String[] getDeclaredPrefixes() {
        Set<String> keys = keySet();
        return (String[])keys.toArray(new String[keys.size()]);
    }
    
    public String getNamespaceURI(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("The prefix must not be null.");
        }
        if (Constants.XML_NS_PREFIX.equals(prefix)) {
            return Constants.XML_NS_URI;
        }
        if (Constants.XMLNS_ATTRIBUTE.equals(prefix)) {
            return Constants.XMLNS_ATTRIBUTE_NS_URI;
        }
        Object namespaceURI = get(prefix); 
        return namespaceURI == null ? Constants.NULL_NS_URI : namespaceURI .toString();
    }    

    public String getPrefix(String namespaceURI) {
        for (Map.Entry<String, Object> entry : entrySet()) {
            if (entry.getValue().toString().equals(namespaceURI)) {
                return (String)entry.getKey();
            }
        }
        return null;
    }

    public Iterator<String> getPrefixes(String namespaceURI) {
        List<String> list = new ArrayList<String>();
        for (Map.Entry<String, Object> entry : entrySet()) {
            if (entry.getValue().toString().equals(namespaceURI)) {
                list.add(entry.getKey());
            }
        }
        return list.iterator();
    }
}
