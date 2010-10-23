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

import org.apache.ws.commons.schema.utils.CollectionFactory;

/**
 * Common class for include, import, and redefine. All have in common two items:
 * the location of the referenced schema (required) and an optional
 * reference to that schema as represented in XmlSchema.
 */
public abstract class XmlSchemaExternal extends XmlSchemaAnnotated {

    XmlSchema schema;
    String schemaLocation;

    /**
     * Creates new XmlSchemaExternal
     */
    protected XmlSchemaExternal(XmlSchema parent) {
        final XmlSchema fParent = parent;
        CollectionFactory.withSchemaModifiable(new Runnable() {

            public void run() {
                fParent.getExternals().add(XmlSchemaExternal.this);
                fParent.getItems().add(XmlSchemaExternal.this);
            }
        });
    }

    public XmlSchema getSchema() {
        return schema;
    }

    /**
     * Store a reference to an XmlSchema corresponding to this item. This only
     * case in which this will be read is if you ask the XmlSchemaSerializer
     * to serialize external schemas.
     * @param sc schema reference
     */
    public void setSchema(XmlSchema sc) {
        schema = sc;
    }

    public String getSchemaLocation() {
        return schemaLocation;
    }

    public void setSchemaLocation(String schemaLocation) {
        this.schemaLocation = schemaLocation;
    }
}
