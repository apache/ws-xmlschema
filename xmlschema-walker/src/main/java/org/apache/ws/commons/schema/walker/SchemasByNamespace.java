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
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroup;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaGroup;
import org.apache.ws.commons.schema.XmlSchemaType;

/**
 * A class that allows XmlSchemas to be stored, indexed by a String which is the schema's target namespace.
 * Multiple schemas are allowed for each namespace.
 * It provides methods that allow all schemas for a given namespace to be searched, for example when the caller
 * wishes to find an xsd type defined for a particular namespace.
 **/
class SchemasByNamespace {

    /**
     * the map of namespaces to a list of XmlSchemas
     */
    private final Map<String, List<XmlSchema>> mData = new HashMap<String, List<XmlSchema>>();

    /**
     * Associates an XmlSchema with a namespace
     * @param aNamespace the namespace in question
     * @param aSchema a schema with aNamespace as its target
     */
    public void addSchema(String aNamespace, XmlSchema aSchema) {
        if (aNamespace == null) {
            aNamespace = "";
        }
        List<XmlSchema> value = mData.get(aNamespace);
        if (value == null) {
            value = new ArrayList<XmlSchema>();
            mData.put(aNamespace, value);
        }
        value.add(aSchema);
    }

    /**
     * Returns a List of XmlSchemas that have been associated with the given namespace using addSchema(...).
     * @param aNamespace the namespace in question
     * @return a List of XmlSchemas which target aNamespace
     */
    public XmlSchema[] getSchemas(String aNamespace) {
        if (aNamespace == null) {
            aNamespace = "";
        }
        List<XmlSchema> l = mData.get(aNamespace);
        return l == null ? new XmlSchema[0] : l.toArray(new XmlSchema[l.size()]);
    }

    /**
     * Returns an XmlSchemaType with the given QName.
     * The namespace defined by the name is used to find the schema in which the type is defined, and thus the type itself.
     * @param aTypeName the name of the type, which must return the correct value for aTypeName.getNamespaceURI().
     * @return the XmlSchemaType with name aTypeName or null if no such type is found
     */
    public XmlSchemaType getTypeByName(QName aTypeName) {
        XmlSchema[] schemas = getSchemas(aTypeName.getNamespaceURI());
        for (XmlSchema s : schemas) {
            XmlSchemaType t = s.getTypeByName(aTypeName);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    /**
     * Returns an XmlSchemaGroup with the given QName.
     * The namespace defined by the name is used to find the schema in which the group is defined, and thus the group itself.
     * @param aGroupName the name of the group, which must return the correct value for aGroupName.getNamespaceURI().
     * @return the XmlSchemaGroup with name aGroupName or null if no such group is found
     */
    public XmlSchemaGroup getGroupByName(QName aGroupName) {
        XmlSchema[] schemas = getSchemas(aGroupName.getNamespaceURI());
        for (XmlSchema s : schemas) {
            XmlSchemaGroup g = s.getGroupByName(aGroupName);
            if (g != null) {
                return g;
            }
        }
        return null;
    }

    /**
     * Returns an XmlSchemaAttribute with the given QName.
     * The namespace defined by the name is used to find the schema in which the attribute is defined, and thus the attribute itself.
     * @param aAttName the name of the attribute, which must return the correct value for aAttName.getNamespaceURI().
     * @return the XmlSchemaAttribute with name aAttName or null if no such attribute is found
     */
    public XmlSchemaAttribute getAttributeByName(QName aAttName) {
        XmlSchema[] schemas = getSchemas(aAttName.getNamespaceURI());
        for (XmlSchema s : schemas) {
            XmlSchemaAttribute a = s.getAttributeByName(aAttName);
            if (a != null) {
                return a;
            }
        }
        return null;
    }

    /**
     * Returns an XmlSchemaAttributeGroup with the given QName.
     * The namespace defined by the name is used to find the schema in which the attribute group is defined, and thus the attribute group itself.
     * @param aAGName the name of the attribute group, which must return the correct value for aAGName.getNamespaceURI().
     * @return the XmlSchemaAttributeGroup with name aAGName or null if no such attribute group is found
     */
    public XmlSchemaAttributeGroup getAttributeGroupByName(QName aAGName) {
        XmlSchema[] schemas = getSchemas(aAGName.getNamespaceURI());
        for (XmlSchema s : schemas) {
            XmlSchemaAttributeGroup ag = s.getAttributeGroupByName(aAGName);
            if (ag != null) {
                return ag;
            }
        }
        return null;
    }

    /**
     * Returns an XmlSchemaElement with the given QName.
     * The namespace defined by the name is used to find the schema in which the element is defined, and thus the element itself.
     * @param aElementName the name of the element, which must return the correct value for aElementName.getNamespaceURI().
     * @return the XmlSchemaElement with name aElementName or null if no such element is found
     */
    public XmlSchemaElement getElementByName(QName aElementName) {
        XmlSchema[] schemas = getSchemas(aElementName.getNamespaceURI());
        for (XmlSchema s : schemas) {
            XmlSchemaElement e = s.getElementByName(aElementName);
            if (e != null) {
                return e;
            }
        }
        return null;
    }

    /**
     * Returns an XmlSchema which includes an element with the given QName.
     * The namespace defined by the name is used to find the schema in which the element is defined.
     * @param aElementName the name of the element, which must return the correct value for aElementName.getNamespaceURI().
     * @return the XmlSchema which includes the element with name aElementName or null if no such schema is found
     */
    public XmlSchema getSchemaDefiningElement(QName aElementName) {
        XmlSchema[] schemas = getSchemas(aElementName.getNamespaceURI());
        for (XmlSchema s : schemas) {
            XmlSchemaElement e = s.getElementByName(aElementName);
            if (e != null) {
                return s;
            }
        }
        return null;
    }

    /**
     * Returns an XmlSchema which includes an attribute with the given QName.
     * The namespace defined by the name is used to find the schema in which the attribute is defined.
     * @param aAttName the name of the attribute, which must return the correct value for aAttName.getNamespaceURI().
     * @return the XmlSchema which includes the attribute with name aAttName or null if no such schema is found
     */
    public XmlSchema getSchemaDefiningAttribute(QName aAttName) {
        XmlSchema[] schemas = getSchemas(aAttName.getNamespaceURI());
        for (XmlSchema s : schemas) {
            XmlSchemaAttribute a = s.getAttributeByName(aAttName);
            if (a != null) {
                return s;
            }
        }
        return null;
    }

    /**
     * Returns the first XmlSchema added with the given namespace, using addSchema(...)
     * @param aNamespace the namespace in question
     * @return the XmlSchema first added with aNamespace
     */
    public XmlSchema getFirstSchema(String aNamespace) {
        XmlSchema[] schemas = getSchemas(aNamespace);
        if (schemas.length > 0) {
            return schemas[0];
        } else {
            return null;
        }
    }
}
