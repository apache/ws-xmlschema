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

import org.apache.ws.commons.schema.XmlSchemaForm;

/**
 * Attributes and elements have names that are influenced by their form.
 * Essentially, the 'form' has three possible values: qualified,
 * unqualified, and 'inherit from parent' (= unspecified).
 */
public interface XmlSchemaNamedWithForm extends XmlSchemaNamed {
    /**
     * Get the current form, which may be inherited from the parent schema.
     * This will never return XmlSchemaForm.NONE.
     * @return
     */
    XmlSchemaForm getForm();

    /**
     * Set the schema form.
     * @param form Schema form. Pass in XmlSchemaForm.NONE to inherit
     * from the parent schema.
     */
    void setForm(XmlSchemaForm form);

    /**
     * True if this item has a specified form, false if it inherits from
     * the parent schema.
     * @return
     */
    boolean isFormSpecified();

    /**
     * The name of this item as it is sent 'over the wire' or stored
     * in an XML file. If the form is unqualified, this has "" for a namespaceURI.
     * Otherwise, it is the same as getQName().
     * @return
     */
    QName getWireName();
}
