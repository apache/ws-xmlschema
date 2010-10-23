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
 * Class represents a notation. An XML Schema definition language (XSD) notation declaration is a
 * reconstruction of XML 1.0 NOTATION declarations. The purpose of notations is to describe the format of
 * non-XML data within an XML document. Represents the World Wide Web Consortium (W3C) notation element.
 */

public class XmlSchemaNotation extends XmlSchemaAnnotated implements XmlSchemaNamed {

    private String system;
    private String publicNotation;
    private XmlSchemaNamedImpl namedDelegate;

    /**
     * Creates new XmlSchemaNotation
     */
    public XmlSchemaNotation(XmlSchema parent) {
        namedDelegate = new XmlSchemaNamedImpl(parent, true);
        final XmlSchema fParent = parent;
        CollectionFactory.withSchemaModifiable(new Runnable() {
            public void run() {
                fParent.getItems().add(XmlSchemaNotation.this);
            }
        });
    }

    public String getPublic() {
        return publicNotation;
    }

    public void setPublic(String isPublic) {
        this.publicNotation = isPublic;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
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

    public String toString() {
        return namedDelegate.toString();
    }

    void setPublicNotation(String publicNotation) {
        this.publicNotation = publicNotation;
    }

    String getPublicNotation() {
        return publicNotation;
    }

    public String getName() {
        return namedDelegate.getName();
    }

    public void setName(String name) {
        final String fName = name;
        CollectionFactory.withSchemaModifiable(new Runnable() {
            public void run() {
                if (namedDelegate.getName() != null) {
                    namedDelegate.getParent().getNotations().remove(getQName());
                }
                namedDelegate.setName(fName);
                namedDelegate.getParent().getNotations().put(getQName(), XmlSchemaNotation.this);
            }
        });
    }
}
