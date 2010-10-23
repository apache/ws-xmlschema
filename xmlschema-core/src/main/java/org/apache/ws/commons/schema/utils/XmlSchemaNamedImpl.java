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

/**
 * Common class of all of the named objects in the XML Schema model.
 * Because 'being named' is not part of the XML Schema logical
 * object hierarchy, this is used as a delegate, not as a base class.
 *
 * By definition, all of these objects live in some particular (parent)
 * schema.
 *
 * The parent is intentionally immutable; there's no good reason to move
 * an object from one schema to another, and this simplifies some of the 
 * book-keeping.
 * 
 */
public class XmlSchemaNamedImpl implements XmlSchemaNamed {
    
    protected XmlSchema parentSchema;
    /*
     * Some objects implement both name= and ref=. This reference allows us some error
     * checking.
     */
    protected XmlSchemaRefBase refTwin;
    // Store the name as a QName for the convenience of QName fans.
    private QName qname;
    private boolean topLevel;

    /**
     * Create a new named object.
     * @param parent the parent schema.
     */
    public XmlSchemaNamedImpl(XmlSchema parent, boolean topLevel) {
        this.parentSchema = parent;
        this.topLevel = topLevel;
    }
    
    /**
     * If the named object also implements ref=, it should pass the reference object
     * here for some error checking.
     * @param refBase
     */
    public void setRefObject(XmlSchemaRefBase refBase) {
        refTwin = refBase;
    }

    /** {@inheritDoc}*/
    public String getName() {
        if (qname == null) {
            return null;
        } else {
            return qname.getLocalPart();
        }
    }
    
    /** {@inheritDoc}*/
    public boolean isAnonymous() {
        return qname == null;
    }

    /** {@inheritDoc}*/
    public void setName(String name) {
        if (name == null) {
            this.qname = null;
        } else if ("".equals(name)) {
            throw new XmlSchemaException("Attempt to set empty name.");
        } else {
            if (refTwin != null && refTwin.getTargetQName() != null) {
                throw new XmlSchemaException("Attempt to set name on object with ref='xxx'");
            }
            qname = new QName(parentSchema.getLogicalTargetNamespace(), name);
        }
    }
    
    /** {@inheritDoc}*/
    public XmlSchema getParent() {
        return parentSchema;
    }
    
    /** {@inheritDoc}*/
    public QName getQName() {
        return qname; 
    }

    /** {@inheritDoc}*/
    public boolean isTopLevel() {
        return topLevel;
    }
}
