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

package org.apache.ws.commons.schema.walker;

import org.apache.ws.commons.schema.XmlSchemaEnumerationFacet;
import org.apache.ws.commons.schema.XmlSchemaFacet;
import org.apache.ws.commons.schema.XmlSchemaFractionDigitsFacet;
import org.apache.ws.commons.schema.XmlSchemaLengthFacet;
import org.apache.ws.commons.schema.XmlSchemaMaxExclusiveFacet;
import org.apache.ws.commons.schema.XmlSchemaMaxInclusiveFacet;
import org.apache.ws.commons.schema.XmlSchemaMaxLengthFacet;
import org.apache.ws.commons.schema.XmlSchemaMinExclusiveFacet;
import org.apache.ws.commons.schema.XmlSchemaMinInclusiveFacet;
import org.apache.ws.commons.schema.XmlSchemaMinLengthFacet;
import org.apache.ws.commons.schema.XmlSchemaPatternFacet;
import org.apache.ws.commons.schema.XmlSchemaTotalDigitsFacet;
import org.apache.ws.commons.schema.XmlSchemaWhiteSpaceFacet;

/**
 * This represents an {@link XmlSchemaFacet}. It uses an enum to more easily
 * work with different facets, and its {@link #equals(Object)} and
 * {@link #hashCode()} reflect that only enumerations and patterns can have
 * multiple constraints.
 */
public class XmlSchemaRestriction {

    private Type type;
    private Object value;
    private boolean isFixed;

    /**
     * The facet type: one of the known <a
     * href="http://www.w3.org/TR/xmlschema-2/#rf-facets">constraining
     * facets</a> defined by XML Schema.
     */
    public enum Type {
        ENUMERATION, EXCLUSIVE_MIN, EXCLUSIVE_MAX, INCLUSIVE_MIN, INCLUSIVE_MAX, PATTERN, WHITESPACE, LENGTH, LENGTH_MAX, LENGTH_MIN, DIGITS_FRACTION, DIGITS_TOTAL;
    }

    XmlSchemaRestriction(XmlSchemaFacet facet) {
        if (facet instanceof XmlSchemaEnumerationFacet) {
            type = Type.ENUMERATION;
        } else if (facet instanceof XmlSchemaMaxExclusiveFacet) {
            type = Type.EXCLUSIVE_MAX;
        } else if (facet instanceof XmlSchemaMaxInclusiveFacet) {
            type = Type.INCLUSIVE_MAX;
        } else if (facet instanceof XmlSchemaMinExclusiveFacet) {
            type = Type.EXCLUSIVE_MIN;
        } else if (facet instanceof XmlSchemaMinInclusiveFacet) {
            type = Type.INCLUSIVE_MIN;
        } else if (facet instanceof XmlSchemaFractionDigitsFacet) {
            type = Type.DIGITS_FRACTION;
        } else if (facet instanceof XmlSchemaTotalDigitsFacet) {
            type = Type.DIGITS_TOTAL;
        } else if (facet instanceof XmlSchemaPatternFacet) {
            type = Type.PATTERN;
        } else if (facet instanceof XmlSchemaWhiteSpaceFacet) {
            type = Type.WHITESPACE;
        } else if (facet instanceof XmlSchemaLengthFacet) {
            type = Type.LENGTH;
        } else if (facet instanceof XmlSchemaMinLengthFacet) {
            type = Type.LENGTH_MIN;
        } else if (facet instanceof XmlSchemaMaxLengthFacet) {
            type = Type.LENGTH_MAX;
        } else {
            throw new IllegalArgumentException("Unrecognized facet " + facet.getClass().getName());
        }

        value = facet.getValue();
        isFixed = facet.isFixed();
    }

    /**
     * Constructs a new {@link XmlSchemaRestriction} from only the {@link Type}.
     *
     * @param type The facet's type.
     */
    public XmlSchemaRestriction(Type type) {
        this.type = type;
        this.value = null;
        this.isFixed = false;
    }

    /**
     * Constructs a new {@link XmlSchemaRestriction} from the {@link Type},
     * constraining value, and whether the facet may be overridden by child type
     * definitions.
     *
     * @param type The constraining facet type.
     * @param value The constraining value.
     * @param isFixed Whether the value may be overridden in child definitions.
     */
    public XmlSchemaRestriction(Type type, Object value, boolean isFixed) {
        this.type = type;
        this.value = value;
        this.isFixed = isFixed;
    }

    /**
     * The constraining facet's {@link Type}.
     */
    public Type getType() {
        return type;
    }

    /**
     * The facet's constraint value.
     */
    public Object getValue() {
        return value;
    }

    /**
     * Whether the constraint value may be overridden in child definitions (
     * <code>true</code> means it cannot).
     */
    public boolean isFixed() {
        return isFixed;
    }

    /**
     * Sets the constraint value.
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * Sets whether the constraint value may be overridden in child definitions
     * (<code>true</code> means that it cannot).
     */
    public void setFixed(boolean isFixed) {
        this.isFixed = isFixed;
    }

    /**
     * Generates a hash code based on the contents. If the type is an
     * enumeration, then the isFixed and value elements are used in calculating
     * the hash code. All of the other Restrictions are considered to be the
     * same.
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());

        if ((type != null) && ((type == Type.ENUMERATION) || (type == Type.PATTERN))) {

            result = prime * result + (isFixed ? 1231 : 1237);
            result = prime * result + ((value == null) ? 0 : value.hashCode());
        }
        return result;
    }

    /**
     * Determines equality. If the type is an enumeration, then the isFixed and
     * value elements are used determining equality. All of the other
     * Restrictions are considered to be equal to each other.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        XmlSchemaRestriction other = (XmlSchemaRestriction)obj;
        if (type != other.type)
            return false;

        if ((type != null) && ((type == Type.ENUMERATION) || (type == Type.PATTERN))) {

            if (isFixed != other.isFixed)
                return false;
            if (value == null) {
                if (other.value != null)
                    return false;
            } else if (!value.equals(other.value))
                return false;
        }
        return true;
    }

    /**
     * Returns a {@link String} representation of this facet.
     */
    @Override
    public String toString() {
        return type.name() + ": " + value + " (Fixed: " + isFixed + ")";
    }
}
