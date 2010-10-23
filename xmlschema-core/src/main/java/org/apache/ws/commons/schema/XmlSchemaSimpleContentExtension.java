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
 * Class for simple types that are derived by extension. Extends the simple type content of the element by
 * adding attributes. Represents the World Wide Web Consortium (W3C) extension element for simple content.
 */

public class XmlSchemaSimpleContentExtension extends XmlSchemaContent {

    /* Allows an XmlSchemaAnyAttribute to be used for the attribute value. */
    private XmlSchemaAnyAttribute anyAttribute;

    /*
     * Contains XmlSchemaAttribute and XmlSchemaAttributeGroupRef. Collection of attributes for the simple
     * type.
     */
    private List<XmlSchemaAttributeOrGroupRef> attributes;

    /* Name of the built-in data type, simple type, or complex type. */
    private QName baseTypeName;

    /**
     * Creates new XmlSchemaSimpleContentExtension
     */
    public XmlSchemaSimpleContentExtension() {
        attributes = Collections.synchronizedList(new ArrayList<XmlSchemaAttributeOrGroupRef>());

    }

    public XmlSchemaAnyAttribute getAnyAttribute() {
        return this.anyAttribute;
    }

    public List<XmlSchemaAttributeOrGroupRef> getAttributes() {
        return this.attributes;
    }

    public QName getBaseTypeName() {
        return this.baseTypeName;
    }

    public void setAnyAttribute(XmlSchemaAnyAttribute anyAttribute) {
        this.anyAttribute = anyAttribute;
    }

    public void setBaseTypeName(QName baseTypeName) {
        this.baseTypeName = baseTypeName;
    }

    public void setAttributes(List<XmlSchemaAttributeOrGroupRef> attributes) {
        this.attributes = attributes;
    }

}
