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
import java.util.List;

import javax.xml.namespace.QName;

/**
 * Class for the restriction of simpleType elements. Represents the World Wide Web Consortium (W3C)
 * restriction element for simple types.
 */

public class XmlSchemaSimpleTypeRestriction extends XmlSchemaSimpleTypeContent {
    private XmlSchemaSimpleType baseType;
    private QName baseTypeName;
    private List<XmlSchemaFacet> facets;

    /**
     * Creates new XmlSchemaSimpleTypeRestriction
     */
    public XmlSchemaSimpleTypeRestriction() {
        facets = Collections.synchronizedList(new ArrayList<XmlSchemaFacet>());
    }


    public XmlSchemaSimpleType getBaseType() {
        return this.baseType;
    }

    public void setBaseType(XmlSchemaSimpleType baseType) {
        this.baseType = baseType;
    }

    public QName getBaseTypeName() {
        return this.baseTypeName;
    }

    public void setBaseTypeName(QName baseTypeName) {
        this.baseTypeName = baseTypeName;
    }

    public List<XmlSchemaFacet> getFacets() {
        return this.facets;
    }
}
