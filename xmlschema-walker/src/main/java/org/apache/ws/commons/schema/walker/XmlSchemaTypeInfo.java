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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.namespace.QName;

/**
 * Represents an element's or attribute's type, meaning either a
 * {@link XmlSchemaBaseSimpleType} with facets, a union or list of them, or a
 * complex type.
 * <p>
 * Also maintains a {@link QName} representing a type the user recognizes. Users
 * attempting to convert from one schema to another may use this to track which
 * types in XML Schema map to their own schema types.
 * </p>
 */
public final class XmlSchemaTypeInfo {

    private Type type;
    private HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets;
    private boolean isMixed;
    private XmlSchemaBaseSimpleType baseSimpleType;
    private QName userRecognizedType;
    private List<XmlSchemaTypeInfo> childTypes;

    /**
     * What the data in this <code>XmlSchemaTypeInfo</code> represents. It may
     * be a simple type ({@link XmlSchemaBaseSimpleType}), a list or union of
     * simple types, or a complex type.
     * <p>
     * Complex types are reserved for when an element only contains attributes,
     * or the element's children are mixed with text.
     * </p>
     */
    public enum Type {
        LIST, UNION, ATOMIC, COMPLEX;
    }

    /**
     * Constructs a new <code>XmlSchemaTypeInfo</code> representing a list of
     * other <code>XmlSchemaTypeInfo</code>s. Lists are homogeneous, so only one
     * type is necessary.
     * <p>
     * Lists may be either of atomic types or unions of atomic types. Lists of
     * lists are not allowed.
     * </p>
     *
     * @param listType The list's type.
     */
    public XmlSchemaTypeInfo(XmlSchemaTypeInfo listType) {
        type = Type.LIST;
        childTypes = new ArrayList<XmlSchemaTypeInfo>(1);
        childTypes.add(listType);

        isMixed = false;
        facets = null;
        userRecognizedType = null;
    }

    /**
     * Constructs a list with facets. Lists may be constrained by their length;
     * meaning they may have a {@link XmlSchemaRestriction.Type#LENGTH} facet, a
     * {@link XmlSchemaRestriction.Type#LENGTH_MIN} facet, or a
     * {@link XmlSchemaRestriction.Type#LENGTH_MAX} facet (or both
     * <code>LENGTH_MIN</code> and <code>LENGTH_MAX</code>).
     *
     * @param listType The list type.
     * @param facets Constraining facets on the list itself.
     */
    public XmlSchemaTypeInfo(XmlSchemaTypeInfo listType,
                             HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets) {
        this(listType);
        this.facets = facets;
    }

    /**
     * Constructs a union with the set of valid types a value adhering to the
     * union must conform to.
     * <p>
     * A union may either be of a set of atomic types or a set of list types,
     * but not mixed between the two. A union of list types cannot be a type of
     * a list.
     * </p>
     *
     * @param unionTypes The set of types that a value may adhere to in order to
     *            conform to the union.
     */
    public XmlSchemaTypeInfo(List<XmlSchemaTypeInfo> unionTypes) {
        type = Type.UNION;
        childTypes = unionTypes;

        isMixed = false;
        facets = null;
        userRecognizedType = null;
    }

    /**
     * Constructs a union with the set of valid types the a value adhering to
     * the union must conform to, along with any constraining facets on the
     * union itself.
     *
     * @param unionTypes The set of types that a value must adhere to.
     * @param facets Constraining facets on the union.
     */
    public XmlSchemaTypeInfo(List<XmlSchemaTypeInfo> unionTypes,
                             HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets) {
        this(unionTypes);
        this.facets = facets;
    }

    /**
     * Constructs an atomic type with the {@link XmlSchemaBaseSimpleType}
     * conforming values must adhere to.
     *
     * @param baseSimpleType The value type.
     */
    public XmlSchemaTypeInfo(XmlSchemaBaseSimpleType baseSimpleType) {
        if (baseSimpleType.equals(XmlSchemaBaseSimpleType.ANYTYPE)) {
            type = Type.COMPLEX;
        } else {
            type = Type.ATOMIC;
        }

        this.baseSimpleType = baseSimpleType;

        isMixed = false;
        facets = null;
        childTypes = null;
        userRecognizedType = null;
    }

    /**
     * Constructs an atomic type with the {@link XmlSchemaBaseSimpleType}
     * conforming values must adhere to, along with any additional constraining
     * facets.
     *
     * @param baseSimpleType The value type.
     * @param facets The constraining facets on the value.
     */
    public XmlSchemaTypeInfo(XmlSchemaBaseSimpleType baseSimpleType,
                             HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> facets) {

        this(baseSimpleType);
        this.facets = facets;
    }

    /**
     * Constructs a complex type whose value may or may not be mixed.
     *
     * @param isMixed Whether the element is a mixed type.
     */
    public XmlSchemaTypeInfo(boolean isMixed) {
        type = Type.COMPLEX;
        baseSimpleType = XmlSchemaBaseSimpleType.ANYTYPE;
        this.isMixed = isMixed;

        facets = null;
        childTypes = null;
        userRecognizedType = null;
    }

    /**
     * The set of constraining facets on the value, or <code>null</code> if
     * none.
     */
    public HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> getFacets() {
        return facets;
    }

    /**
     * If this represents an atomic type, returns the type. If this is a complex
     * type, returns {@link XmlSchemaBaseSimpleType#ANYTYPE}.
     */
    public XmlSchemaBaseSimpleType getBaseType() {
        return baseSimpleType;
    }

    /**
     * The type represented by this <code>XmlSchemaTypeInfo</code>.
     */
    public Type getType() {
        return type;
    }

    /**
     * If this represents a list or a union, returns the set of children types.
     * (Lists will only have one child type.)
     * <p>
     * Otherwise, returns <code>null</code>.
     * </p>
     */
    public List<XmlSchemaTypeInfo> getChildTypes() {
        return childTypes;
    }

    /**
     * The corresponding user-defined type, or <code>null</code> if none.
     */
    public QName getUserRecognizedType() {
        return userRecognizedType;
    }

    /**
     * If this is a complex type, returns whether its value is mixed. Otherwise,
     * returns <code>false</code>.
     */
    public boolean isMixed() {
        return isMixed;
    }

    /**
     * Sets the user-recognized type.
     *
     * @param userRecType The user-recognized type.
     */
    public void setUserRecognizedType(QName userRecType) {
        userRecognizedType = userRecType;
    }

    /**
     * A {@link String} representation of this <code>XmlSchemaTypeInfo</code>.
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("XmlSchemaTypeInfo [");
        str.append(type).append("] Base Type: ").append(baseSimpleType);
        str.append(" User Recognized Type: ").append(userRecognizedType);
        str.append(" Is Mixed: ").append(isMixed);
        str.append(" Num Children: ");
        str.append((childTypes == null) ? 0 : childTypes.size());
        return str.toString();
    }
}
