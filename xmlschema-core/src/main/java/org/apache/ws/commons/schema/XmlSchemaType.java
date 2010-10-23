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

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.utils.CollectionFactory;
import org.apache.ws.commons.schema.utils.XmlSchemaNamed;
import org.apache.ws.commons.schema.utils.XmlSchemaNamedImpl;


/**
 * The base class for all simple types and complex types.
 */

public abstract class XmlSchemaType extends XmlSchemaAnnotated implements XmlSchemaNamed {

    private XmlSchemaDerivationMethod deriveBy;
    private XmlSchemaDerivationMethod finalDerivation;
    private XmlSchemaDerivationMethod finalResolved;
    private boolean isMixed;

    private XmlSchemaNamedImpl namedDelegate;

    /**
     * Creates new XmlSchemaType
     */
    protected XmlSchemaType(XmlSchema schema, boolean topLevel) {
        final XmlSchema fSchema = schema;
        namedDelegate = new XmlSchemaNamedImpl(schema, topLevel);
        finalDerivation = XmlSchemaDerivationMethod.NONE;
        if (topLevel) {
            CollectionFactory.withSchemaModifiable(new Runnable() {

                public void run() {
                    fSchema.getItems().add(XmlSchemaType.this);
                }
            });
        }
    }

    public XmlSchemaDerivationMethod getDeriveBy() {
        return deriveBy;
    }

    public XmlSchemaDerivationMethod getFinal() {
        return finalDerivation;
    }

    public void setFinal(XmlSchemaDerivationMethod finalDerivationValue) {
        this.finalDerivation = finalDerivationValue;
    }

    public XmlSchemaDerivationMethod getFinalResolved() {
        return finalResolved;
    }

    public boolean isMixed() {
        return isMixed;
    }

    public void setMixed(boolean isMixedValue) {
        this.isMixed = isMixedValue;
    }

    public String toString() {
        if (getName() == null) {
            return super.toString() + "[anonymous]";
        } else if (namedDelegate.getParent().getLogicalTargetNamespace() == null) {
            return super.toString() + "[{}" + getName() + "]";

        } else {
            return super.toString()
                + "[{" + namedDelegate.getParent().getLogicalTargetNamespace() + "}"
                + getName() + "]";
        }
    }

    public String getName() {
        return namedDelegate.getName();
    }

    public XmlSchema getParent() {
        return namedDelegate.getParent();
    }

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
       /*
        * Inside a redefine, a 'non-top-level' type can have a name.
        * This requires us to tolerate this case (non-top-level, named) even it
        * in any other case it's completely invalid.
        */
        if (isTopLevel() && name == null) {
            throw new XmlSchemaException("A non-top-level type may not be anonyous.");
        }
        if (namedDelegate.isTopLevel() && namedDelegate.getName() != null) {
            namedDelegate.getParent().getSchemaTypes().remove(getQName());
        }
        namedDelegate.setName(name);
        if (namedDelegate.isTopLevel()) {
            namedDelegate.getParent().getSchemaTypes().put(getQName(), this);
        }
    }

    void setFinalResolved(XmlSchemaDerivationMethod finalResolved) {
        this.finalResolved = finalResolved;
    }

    public void setFinalDerivation(XmlSchemaDerivationMethod finalDerivation) {
        this.finalDerivation = finalDerivation;
    }

    public XmlSchemaDerivationMethod getFinalDerivation() {
        return finalDerivation;
    }

    public void setDeriveBy(XmlSchemaDerivationMethod deriveBy) {
        this.deriveBy = deriveBy;
    }
}
