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

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaException;
import org.apache.ws.commons.schema.XmlSchemaForm;

/**
 *
 */
public class XmlSchemaNamedWithFormImpl extends XmlSchemaNamedImpl implements XmlSchemaNamedWithForm {
    private XmlSchemaForm form = XmlSchemaForm.NONE;
    private boolean element;
    private QName wireName;

    /**
     * Delegate object for managing names for attributes and elements.
     * @param parent containing schema.
     * @param topLevel if this object is global.
     * @param element true for an element, false for an attribute.
     */
    public XmlSchemaNamedWithFormImpl(XmlSchema parent, boolean topLevel, boolean element) {
        super(parent, topLevel);
        this.element = element;
    }

    /**
     * Return the <strong>effective</strong> 'form' for this item. If the item
     * has an explicit form declaration, this returns that declared form. If not,
     * it returns the appropriate default form from the containing schema.
     * @return {@link XmlSchemaForm#QUALIFIED} or {@link XmlSchemaForm#UNQUALIFIED}.
     */
    public XmlSchemaForm getForm() {
        if (form != XmlSchemaForm.NONE) {
            return form;
        } else if (element) {
            return parentSchema.getElementFormDefault();
        } else {
            return parentSchema.getAttributeFormDefault();
        }
    }

    /** {@inheritDoc}*/
    public boolean isFormSpecified() {
        return form != XmlSchemaForm.NONE;
    }

    /** {@inheritDoc}*/
    public void setForm(XmlSchemaForm form) {
        if (form == null) {
            throw new XmlSchemaException("form may not be null. "
                                         + "Pass XmlSchemaForm.NONE to use schema default.");
        }
        this.form = form;
        setName(getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setName(String name) {
        super.setName(name);
        if (getForm() == XmlSchemaForm.QUALIFIED) {
            wireName = getQName();
        } else {
            wireName = new QName("", getName());
        }
    }

    /**
     * {@inheritDoc}
     */
    public QName getWireName() {
        // If this is a ref= case, then we take the name from the ref=, not from the QName.
        // what about ref='foo' form='unqualified'? Is that possible?
        if (refTwin != null && refTwin.getTargetQName() != null) {
            return refTwin.getTargetQName();
        } else {
            return wireName;
        }
    }
}
