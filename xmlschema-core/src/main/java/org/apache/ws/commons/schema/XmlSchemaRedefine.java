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

package org.apache.ws.commons.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

/**
 * Allows simple and complex types, groups, and attribute groups from external schema files to be redefined in
 * the current schema. This class provides versioning for the schema elements. Represents the World Wide Web
 * Consortium (W3C) redefine element.
 */

public class XmlSchemaRedefine extends XmlSchemaExternal {

    private Map<QName, XmlSchemaAttributeGroup> attributeGroups;
    private Map<QName, XmlSchemaGroup> groups;
    private Map<QName, XmlSchemaType> schemaTypes;

    private List<XmlSchemaObject> items;

    /**
     * Creates new XmlSchemaRedefine
     */
    public XmlSchemaRedefine(XmlSchema parent) {
        super(parent);
        items = Collections.synchronizedList(new ArrayList<XmlSchemaObject>());
        schemaTypes = Collections.synchronizedMap(new HashMap<QName, XmlSchemaType>());
        groups = Collections.synchronizedMap(new HashMap<QName, XmlSchemaGroup>());
        attributeGroups = Collections.synchronizedMap(new HashMap<QName, XmlSchemaAttributeGroup>());
    }

    public Map<QName, XmlSchemaAttributeGroup> getAttributeGroups() {
        return attributeGroups;
    }

    public Map<QName, XmlSchemaGroup> getGroups() {
        return groups;
    }

    public List<XmlSchemaObject> getItems() {
        return items;
    }

    public Map<QName, XmlSchemaType> getSchemaTypes() {
        return schemaTypes;
    }

}
