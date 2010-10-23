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
 * Class that defines groups at the schema level that are referenced from the complex types. Groups a set of
 * element declarations so that they can be incorporated as a group into complex type definitions. Represents
 * the World Wide Web Consortium (W3C) group element.
 */

public class XmlSchemaGroup extends XmlSchemaAnnotated implements XmlSchemaNamed,
    XmlSchemaChoiceMember, XmlSchemaSequenceMember {

    private XmlSchemaGroupParticle particle;
    private XmlSchemaNamedImpl namedDelegate;

    public XmlSchemaGroup(XmlSchema parent) {
        namedDelegate = new XmlSchemaNamedImpl(parent, true);
        final XmlSchema fParent = parent;
        CollectionFactory.withSchemaModifiable(new Runnable() {
            public void run() {
                fParent.getItems().add(XmlSchemaGroup.this);
            }
        });
    }


    public XmlSchemaGroupParticle getParticle() {
        return particle;
    }

    public void setParticle(XmlSchemaGroupParticle particle) {
        this.particle = particle;
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
        final String fName = name;
        CollectionFactory.withSchemaModifiable(new Runnable() {
            public void run() {
                if (namedDelegate.getQName() != null) {
                    namedDelegate.getParent().getGroups().remove(namedDelegate.getQName());
                }
                namedDelegate.setName(fName);
                if (fName != null) {
                    namedDelegate.getParent().getGroups().put(namedDelegate.getQName(), XmlSchemaGroup.this);
                }
            }
        });
    }
}
