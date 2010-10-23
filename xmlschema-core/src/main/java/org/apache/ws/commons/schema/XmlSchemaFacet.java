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

import org.w3c.dom.Element;

/**
 * Abstract class for all facets that are used when simple types are derived by restriction.
 */

public abstract class XmlSchemaFacet extends XmlSchemaAnnotated {

    boolean fixed;

    Object value;

    /**
     * Creates new XmlSchemaFacet
     */

    protected XmlSchemaFacet() {
    }

    protected XmlSchemaFacet(Object value, boolean fixed) {
        this.value = value;
        this.fixed = fixed;
    }

    public static XmlSchemaFacet construct(Element el) {
        String name = el.getLocalName();
        boolean fixed = false;
        if (el.getAttribute("fixed").equals("true")) {
            fixed = true;
        }
        XmlSchemaFacet facet;
        if ("enumeration".equals(name)) {
            facet = new XmlSchemaEnumerationFacet();
        } else if ("fractionDigits".equals(name)) {
            facet = new XmlSchemaFractionDigitsFacet();
        } else if ("length".equals(name)) {
            facet = new XmlSchemaLengthFacet();
        } else if ("maxExclusive".equals(name)) {
            facet = new XmlSchemaMaxExclusiveFacet();
        } else if ("maxInclusive".equals(name)) {
            facet = new XmlSchemaMaxInclusiveFacet();
        } else if ("maxLength".equals(name)) {
            facet = new XmlSchemaMaxLengthFacet();
        } else if ("minLength".equals(name)) {
            facet = new XmlSchemaMinLengthFacet();
        } else if ("minExclusive".equals(name)) {
            facet = new XmlSchemaMinExclusiveFacet();
        } else if ("minInclusive".equals(name)) {
            facet = new XmlSchemaMinInclusiveFacet();
        } else if ("pattern".equals(name)) {
            facet = new XmlSchemaPatternFacet();
        } else if ("totalDigits".equals(name)) {
            facet = new XmlSchemaTotalDigitsFacet();
        } else if ("whiteSpace".equals(name)) {
            facet = new XmlSchemaWhiteSpaceFacet();
        } else {
            throw new XmlSchemaException("Incorrect facet with name \"" + name + "\" found.");
        }
        if (el.hasAttribute("id")) {
            facet.setId(el.getAttribute("id"));
        }
        facet.setFixed(fixed);
        facet.setValue(el.getAttribute("value"));
        return facet;
    }

    public Object getValue() {
        return value;
    }

    public boolean isFixed() {
        return fixed;
    }

    public void setFixed(boolean fixed) {
        this.fixed = fixed;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
