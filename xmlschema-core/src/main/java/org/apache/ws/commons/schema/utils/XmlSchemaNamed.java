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

/**
 * A named item. Named items have two names; their schema-local name (a string)
 * and a QName qualified by their schema's target namespace. Note that the qualified
 * name is <i>not</i> the on-the-wire name for attributes and elements. Those
 * names depend on the form, and are managed by {@link XmlSchemaNamedWithForm}.
 */
public interface XmlSchemaNamed extends XmlSchemaObjectBase {

    /**
     * Retrieve the name.
     * @return the local name of this object within its schema.
     */
    String getName();

    /**
     * @return true if this object has no name.
     */
    boolean isAnonymous();

    /**
     * Set the name. Set to null to render the object anonymous, or to prepare to
     * change it to refer to some other object.
     * @param name the name.
     */
    void setName(String name);

    /**
     * Retrieve the parent schema.
     * @return the containing schema.
     */
    XmlSchema getParent();

    /**
     * Get the QName for this object. This is always the formal name that identifies this
     * item in the schema. If the item has a form (an element or attribute), and the form
     * is 'unqualified', this is <strong>not</strong> the appropriate QName in an instance
     * document. For those items, the getWiredName method returns the appropriate
     * QName for an instance document.
     * @see XmlSchemaNamedWithForm#getWireName()
     * @return The qualified name of this object.
     */
    QName getQName();

    /**
     * @return true if this item is a top-level item of the schema; false if this item
     * is nested inside of some other schema object.
     */
    boolean isTopLevel();

}
