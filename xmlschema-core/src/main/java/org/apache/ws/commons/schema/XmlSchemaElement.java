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

import org.apache.ws.commons.schema.utils.CollectionFactory;
import org.apache.ws.commons.schema.utils.XmlSchemaNamedWithForm;
import org.apache.ws.commons.schema.utils.XmlSchemaNamedWithFormImpl;
import org.apache.ws.commons.schema.utils.XmlSchemaRef;
import org.apache.ws.commons.schema.utils.XmlSchemaRefBase;

/**
 * Class for elements, representing xs:element.
 *
 * This class represents both global elements and elements nested inside of
 * types.
 */

public class XmlSchemaElement extends XmlSchemaParticle implements TypeReceiver, XmlSchemaNamedWithForm,
    XmlSchemaChoiceMember, XmlSchemaSequenceMember,
    XmlSchemaItemWithRef<XmlSchemaElement> {

    /**
     * Attribute used to block a type derivation.
     */
    private XmlSchemaDerivationMethod block;

    private List<XmlSchemaIdentityConstraint> constraints;

    /**
     * Provides the default value of the element if its content is a simple type or the element's content is
     * textOnly.
     */
    private String defaultValue;
    private String fixedValue;

    private XmlSchemaDerivationMethod finalDerivation;

    private boolean abstractElement;
    private boolean nillable;
    private XmlSchemaRef<XmlSchemaElement> ref;

    /**
     * Returns the type of the element. This can either be a complex type or a simple type.
     */
    private XmlSchemaType schemaType;

    /**
     * QName of a built-in data type defined in this schema or another schema indicated by the specified
     * namespace.
     */
    private QName schemaTypeName;

    /**
     * QName of an element that can be a substitute for this element.
     */
    private QName substitutionGroup;

    private XmlSchemaNamedWithFormImpl namedDelegate;

    /**
     * Creates new XmlSchemaElement
     */
    public XmlSchemaElement(XmlSchema parentSchema, boolean topLevel) {
        namedDelegate = new XmlSchemaNamedWithFormImpl(parentSchema, topLevel, true);
        ref = new XmlSchemaRef<XmlSchemaElement>(parentSchema, XmlSchemaElement.class);
        namedDelegate.setRefObject(ref);
        ref.setNamedObject(namedDelegate);

        constraints = Collections.synchronizedList(new ArrayList<XmlSchemaIdentityConstraint>());
        abstractElement = false;
        nillable = false;
        finalDerivation = XmlSchemaDerivationMethod.NONE;
        block = XmlSchemaDerivationMethod.NONE;
        final XmlSchema fParentSchema = parentSchema;
        if (topLevel) {
            CollectionFactory.withSchemaModifiable(new Runnable() {
                public void run() {
                    fParentSchema.getItems().add(XmlSchemaElement.this);
                }
            });
        }
    }

    /**
     * Returns a collection of constraints on the element.
     */
    public List<XmlSchemaIdentityConstraint> getConstraints() {
        return constraints;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public XmlSchemaDerivationMethod getBlock() {
        return block;
    }

    public void setBlock(XmlSchemaDerivationMethod block) {
        this.block = block;
    }

    public XmlSchemaDerivationMethod getFinal() {
        return finalDerivation;
    }

    public void setFinal(XmlSchemaDerivationMethod finalDerivationValue) {
        this.finalDerivation = finalDerivationValue;
    }

    public String getFixedValue() {
        return fixedValue;
    }

    public void setFixedValue(String fixedValue) {
        this.fixedValue = fixedValue;
    }

    public boolean isAbstract() {
        return abstractElement;
    }

    public void setAbstract(boolean isAbstract) {
        this.abstractElement = isAbstract;
    }

    public boolean isNillable() {
        return nillable;
    }

    public void setNillable(boolean isNillable) {
        this.nillable = isNillable;
    }


    public XmlSchemaRef<XmlSchemaElement> getRef() {
        return ref;
    }

    public XmlSchemaType getSchemaType() {
        return schemaType;
    }

    public void setSchemaType(XmlSchemaType schemaType) {
        this.schemaType = schemaType;
    }

    public QName getSchemaTypeName() {
        return schemaTypeName;
    }

    public void setSchemaTypeName(QName schemaTypeName) {
        this.schemaTypeName = schemaTypeName;
    }

    public QName getSubstitutionGroup() {
        return substitutionGroup;
    }

    public void setSubstitutionGroup(QName substitutionGroup) {
        this.substitutionGroup = substitutionGroup;
    }

    public void setType(XmlSchemaType type) {
        this.schemaType = type;
    }


    public String getName() {
        return namedDelegate.getName();
    }


    public XmlSchema getParent() {
        return namedDelegate.getParent();
    }


    /**
     * {@inheritDoc}
     */
    public QName getQName() {
        return namedDelegate.getQName();
    }


    public boolean isAnonymous() {
        return namedDelegate.isAnonymous();
    }


    public boolean isTopLevel() {
        return namedDelegate.isTopLevel();
    }

    public void setName(String name) {
        final String fName = name;
        CollectionFactory.withSchemaModifiable(new Runnable() {
            public void run() {
                if (namedDelegate.isTopLevel() && namedDelegate.getName() != null) {
                    namedDelegate.getParent().getElements().remove(getQName());
                }
                namedDelegate.setName(fName);
                if (namedDelegate.isTopLevel()) {
                    namedDelegate.getParent().getElements().put(getQName(), XmlSchemaElement.this);
                }
            }
        });
    }

    public XmlSchemaForm getForm() {
        return namedDelegate.getForm();
    }

    public boolean isFormSpecified() {
        return namedDelegate.isFormSpecified();
    }

    public void setForm(XmlSchemaForm form) {
        namedDelegate.setForm(form);
    }

    public QName getWireName() {
        return namedDelegate.getWireName();
    }

    /**
     * @param finalDerivation The finalDerivation to set.
     */
    public void setFinalDerivation(XmlSchemaDerivationMethod finalDerivation) {
        this.finalDerivation = finalDerivation;
    }

    /** * @return Returns the finalDerivation.
     */
    public XmlSchemaDerivationMethod getFinalDerivation() {
        return finalDerivation;
    }

    /**
     * Set the 'abstract' attribute of this element.
     * @param abstractElement
     */
    public void setAbstractElement(boolean abstractElement) {
        this.abstractElement = abstractElement;
    }


    /**
     *  @return true if this element is abstract.
     */
    public boolean isAbstractElement() {
        return abstractElement;
    }

    public boolean isRef() {
        return ref.getTargetQName() != null;
    }

    public QName getTargetQName() {
        return ref.getTargetQName();
    }

    public XmlSchemaRefBase getRefBase() {
        return ref;
    }
}
