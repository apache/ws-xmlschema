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
 * Class for complex types. Defines a complex type that determines the set of attributes and content of an
 * element. Represents the World Wide Web Consortium (W3C) complexType element.
 */

public class XmlSchemaComplexType extends XmlSchemaType {
    private XmlSchemaAnyAttribute anyAttribute;
    private XmlSchemaAnyAttribute attributeWildcard;
    private List<XmlSchemaAttributeOrGroupRef> attributes;
    private XmlSchemaDerivationMethod block;
    private XmlSchemaDerivationMethod blockResolved;
    private XmlSchemaContentModel contentModel;
    private XmlSchemaContentType contentType;
    private XmlSchemaParticle particleType;
    private XmlSchemaParticle particle;
    private boolean isAbstract;
    private boolean isMixed;

    /**
     * Creates new XmlSchemaComplexType
     */
    public XmlSchemaComplexType(XmlSchema schema, boolean topLevel) {
        super(schema, topLevel);
        attributes = Collections.synchronizedList(new ArrayList<XmlSchemaAttributeOrGroupRef>());
        block = XmlSchemaDerivationMethod.NONE;
        isAbstract = false;
        isMixed = false;
    }

    public XmlSchemaAnyAttribute getAnyAttribute() {
        return anyAttribute;
    }

    public void setAnyAttribute(XmlSchemaAnyAttribute anyAttribute) {
        this.anyAttribute = anyAttribute;
    }

    public List<XmlSchemaAttributeOrGroupRef> getAttributes() {
        return attributes;
    }

    public XmlSchemaAnyAttribute getAttributeWildcard() {
        return attributeWildcard;
    }

    public XmlSchemaDerivationMethod getBlock() {
        return block;
    }

    public void setBlock(XmlSchemaDerivationMethod block) {
        this.block = block;
    }

    public XmlSchemaDerivationMethod getBlockResolved() {
        return blockResolved;
    }

    public XmlSchemaContentModel getContentModel() {
        return contentModel;
    }

    public void setContentModel(XmlSchemaContentModel contentModel) {
        this.contentModel = contentModel;
    }

    public XmlSchemaContentType getContentType() {
        return contentType;
    }

    public void setContentType(XmlSchemaContentType contentType) {
        this.contentType = contentType;
    }

    public XmlSchemaParticle getContentTypeParticle() {
        return particleType;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setAbstract(boolean b) {
        isAbstract = b;
    }

    public boolean isMixed() {
        return isMixed;
    }

    public void setMixed(boolean b) {
        isMixed = b;
    }

    public XmlSchemaParticle getParticle() {
        return particle;
    }

    public void setParticle(XmlSchemaParticle particle) {
        this.particle = particle;
    }
    /**
     * Return the QName of the base schema type, if any, as defined in the content model.
     */
    public QName getBaseSchemaTypeName() {
        XmlSchemaContentModel model = getContentModel();
        if (model == null) {
            return null;
        }
        XmlSchemaContent content = model.getContent();
        if (content == null) {
            return null;
        }

        if (!(content instanceof XmlSchemaComplexContentExtension)) {
            return null;
        }

        XmlSchemaComplexContentExtension ext = (XmlSchemaComplexContentExtension)content;
        return ext.getBaseTypeName();
    }

    void setAttributeWildcard(XmlSchemaAnyAttribute attributeWildcard) {
        this.attributeWildcard = attributeWildcard;
    }

    void setAttributes(List<XmlSchemaAttributeOrGroupRef> attributes) {
        this.attributes = attributes;
    }

    void setBlockResolved(XmlSchemaDerivationMethod blockResolved) {
        this.blockResolved = blockResolved;
    }

    void setParticleType(XmlSchemaParticle particleType) {
        this.particleType = particleType;
    }

    XmlSchemaParticle getParticleType() {
        return particleType;
    }
}
