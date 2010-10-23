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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import org.apache.ws.commons.schema.XmlSchemaSerializer.XmlSchemaSerializerException;
import org.apache.ws.commons.schema.utils.CollectionFactory;
import org.apache.ws.commons.schema.utils.NamespaceContextOwner;
import org.apache.ws.commons.schema.utils.NamespacePrefixList;

/**
 * Contains the definition of a schema. All XML Schema definition language (XSD) elements are children of the
 * schema element.
 */
public class XmlSchema extends XmlSchemaAnnotated implements NamespaceContextOwner {
    static final String SCHEMA_NS = XMLConstants.W3C_XML_SCHEMA_NS_URI;

    private static final String UTF_8_ENCODING = "UTF-8";

    // This has be ordered so that things come out in the order we parse them.
    private List<XmlSchemaObject> items;

    private XmlSchemaCollection parent;
    private XmlSchemaDerivationMethod blockDefault;
    private XmlSchemaDerivationMethod finalDefault;
    private XmlSchemaForm elementFormDefault;
    private XmlSchemaForm attributeFormDefault;
    private List<XmlSchemaExternal> externals;
    private Map<QName, XmlSchemaAttributeGroup> attributeGroups;
    private Map<QName, XmlSchemaAttribute> attributes;
    private Map<QName, XmlSchemaElement> elements;
    private Map<QName, XmlSchemaGroup> groups;
    private Map<QName, XmlSchemaNotation> notations;
    private Map<QName, XmlSchemaType> schemaTypes;
    private String syntacticalTargetNamespace;
    private String schemaNamespacePrefix;
    private String logicalTargetNamespace;
    private String version;

    private NamespacePrefixList namespaceContext;
    // keep the encoding of the input
    private String inputEncoding;

    /**
     * Create a schema that is not a member of a collection and has no target namespace or system ID.
     */
    public XmlSchema() {
        this(null, null, null);
    }

    /**
     * Create a new schema with a target namespace and system ID, and record it as a member of a schema
     * collection.
     *
     * @param namespace the target namespace.
     * @param systemId the system ID for the schema. This is used to resolve references to other schemas.
     * @param parent the parent collection.
     */
    public XmlSchema(String namespace, String systemId, XmlSchemaCollection parent) {
        this.parent = parent;
        attributeFormDefault = XmlSchemaForm.UNQUALIFIED;
        elementFormDefault = XmlSchemaForm.UNQUALIFIED;
        blockDefault = XmlSchemaDerivationMethod.NONE;
        finalDefault = XmlSchemaDerivationMethod.NONE;
        items = new ArrayList<XmlSchemaObject>();
        externals = new ArrayList<XmlSchemaExternal>();
        elements = new HashMap<QName, XmlSchemaElement>();
        attributeGroups = new HashMap<QName, XmlSchemaAttributeGroup>();
        attributes = new HashMap<QName, XmlSchemaAttribute>();
        groups = new HashMap<QName, XmlSchemaGroup>();
        notations = new HashMap<QName, XmlSchemaNotation>();
        schemaTypes = new HashMap<QName, XmlSchemaType>();

        logicalTargetNamespace = namespace;
        syntacticalTargetNamespace = namespace;
        if (logicalTargetNamespace == null) {
            logicalTargetNamespace = "";
        }
        if (parent != null) {
            XmlSchemaCollection.SchemaKey schemaKey =
                new XmlSchemaCollection.SchemaKey(
                                                  this.logicalTargetNamespace,
                                                  systemId);
            if (parent.containsSchema(schemaKey)) {
                throw new XmlSchemaException("Schema name conflict in collection");
            } else {
                parent.addSchema(schemaKey, this);
            }
        }
    }

    /**
     * Create a new schema in a collection with a target namespace.
     *
     * @param namespace the target namespace.
     * @param parent the containing collection.
     */
    public XmlSchema(String namespace, XmlSchemaCollection parent) {
        this(namespace, namespace, parent);

    }

    /**
     * Return an array of DOM documents consisting of this schema and any schemas that it references.
     * Referenced schemas are only returned if the {@link XmlSchemaExternal} objects corresponding to them
     * have their 'schema' fields filled in.
     *
     * @return DOM documents.
     */
    public Document[] getAllSchemas() {
        try {

            XmlSchemaSerializer xser = new XmlSchemaSerializer();
            xser.setExtReg(this.parent.getExtReg());
            return xser.serializeSchema(this, true);

        } catch (XmlSchemaSerializer.XmlSchemaSerializerException e) {
            throw new XmlSchemaException("Error serializing schema", e);
        }
    }

    /**
     * Retrieve a global attribute by its QName.
     *
     * @param name
     * @return the attribute.
     */
    public XmlSchemaAttribute getAttributeByName(QName name) {
        return this.getAttributeByName(name, true, null);
    }

    /**
     * Look for an attribute by its local name.
     *
     * @param name
     * @return the attribute
     */
    public XmlSchemaAttribute getAttributeByName(String name) {
        QName nameToSearchFor = new QName(this.getTargetNamespace(), name);
        return this.getAttributeByName(nameToSearchFor, false, null);
    }

    /**
     * @return the default attribute form for this schema.
     */
    public XmlSchemaForm getAttributeFormDefault() {
        return attributeFormDefault;
    }

    /**
     * Retrieve an attribute group by QName.
     *
     * @param name
     * @return
     */
    public XmlSchemaAttributeGroup getAttributeGroupByName(QName name) {
        return getAttributeGroupByName(name, true, null);
    }

    /**
     * Return a map containing all the defined attribute groups of this schema. The keys are QNames, where the
     * namespace will always be the target namespace of this schema. This makes it easier to look up items for
     * cross-schema references.
     * <br/>
     * If org.apache.ws.commons.schema.protectReadOnlyCollections
     * is 'true', this will return a map that checks at runtime.
     *
     * @return the map of attribute groups.
     */
    public Map<QName, XmlSchemaAttributeGroup> getAttributeGroups() {
        return CollectionFactory.getProtectedMap(attributeGroups);
    }

    /**
     * Return a map containing all the defined attributes of this schema. The keys are QNames, where the
     * namespace will always be the target namespace of this schema. This makes it easier to look up items for
     * cross-schema references.
     * <br/>
     * If org.apache.ws.commons.schema.protectReadOnlyCollections
     * is 'true', this will return a map that checks at runtime.
     *
     * @return the map of attributes.
     */
    public Map<QName, XmlSchemaAttribute> getAttributes() {
        return CollectionFactory.getProtectedMap(attributes);
    }

    /**
     * Return the default block value for this schema.
     *
     * @return the default block value.
     */
    public XmlSchemaDerivationMethod getBlockDefault() {
        return blockDefault;
    }

    /**
     * Look for a element by its QName.
     *
     * @param name
     * @return the element.
     */
    public XmlSchemaElement getElementByName(QName name) {
        return this.getElementByName(name, true, null);
    }

    /**
     * get an element by its local name.
     *
     * @param name
     * @return the element.
     */
    public XmlSchemaElement getElementByName(String name) {
        QName nameToSearchFor = new QName(this.getTargetNamespace(), name);
        return this.getElementByName(nameToSearchFor, false, null);
    }

    /**
     * @return the default element form for this schema.
     */
    public XmlSchemaForm getElementFormDefault() {
        return elementFormDefault;
    }

    /**
     * Return a map containing all the defined elements of this schema. The keys are QNames, where the
     * namespace will always be the target namespace of this schema. This makes it easier to look up items for
     * cross-schema references.
     * <br/>
     * If org.apache.ws.commons.schema.protectReadOnlyCollections
     * is 'true', this will return a map that checks at runtime
     *
     * @return the map of elements.
     */
    public Map<QName, XmlSchemaElement> getElements() {
        return CollectionFactory.getProtectedMap(elements);
    }

    /**
     * Return all of the includes, imports, and redefines for this schema.
     * <br/>
     * If org.apache.ws.commons.schema.protectReadOnlyCollections
     * is 'true', this will return a list that checks at runtime
     *
     * @return a list of the objects representing includes, imports, and redefines.
     */
    public List<XmlSchemaExternal> getExternals() {
        return CollectionFactory.getProtectedList(externals);
    }

    /**
     * @return the default 'final' value for this schema.
     */
    public XmlSchemaDerivationMethod getFinalDefault() {
        return finalDefault;
    }

    /**
     * Retrieve a group by QName.
     *
     * @param name
     * @return
     */
    public XmlSchemaGroup getGroupByName(QName name) {
        return getGroupByName(name, true, null);
    }

    /**
     * Return a map containing all the defined groups of this schema. The keys are QNames, where the namespace
     * will always be the target namespace of this schema. This makes it easier to look up items for
     * cross-schema references.<br/>
     * If org.apache.ws.commons.schema.protectReadOnlyCollections
     * is 'true', this will return a map that checks at runtime
     *
     * @return the map of groups.
     */
    public Map<QName, XmlSchemaGroup> getGroups() {
        return CollectionFactory.getProtectedMap(groups);
    }

    /**
     * Return the character encoding for this schema. This will only be present if either the schema was read
     * from an XML document or there was a call to {@link #setInputEncoding(String)}.
     *
     * @return
     */
    public String getInputEncoding() {
        return inputEncoding;
    }

    /**
     * Return all of the global items in this schema.<br/>
     * If org.apache.ws.commons.schema.protectReadOnlyCollections
     * is 'true', this will return a map that checks at runtime.
     * @return <strong>all</strong> of the global items from this schema.
     *
     */
    public List<XmlSchemaObject> getItems() {
        return CollectionFactory.getProtectedList(items);
    }

    /**
     * Return the logical target namespace. If a schema document has no target namespace, but it is referenced
     * via an xs:include or xs:redefine, its logical target namespace is the target namespace of the including
     * schema.
     *
     * @return the logical target namespace.
     */
    public String getLogicalTargetNamespace() {
        return logicalTargetNamespace;
    }

    /**
     * {@inheritDoc}
     */
    public NamespacePrefixList getNamespaceContext() {
        return namespaceContext;
    }

    /**
     * Retrieve a notation by QName.
     *
     * @param name
     * @return the notation
     */
    public XmlSchemaNotation getNotationByName(QName name) {
        return getNotationByName(name, true, null);
    }

    /**
     * Return a map containing all the defined notations of this schema. The keys are QNames, where the
     * namespace will always be the target namespace of this schema. This makes it easier to look up items for
     * cross-schema references.
     * <br/>
     * If org.apache.ws.commons.schema.protectReadOnlyCollections
     * is 'true', this will return a map that checks at runtime.
     *
     * @return the map of notations.
     */
    public Map<QName, XmlSchemaNotation> getNotations() {
        return CollectionFactory.getProtectedMap(notations);
    }

    /**
     * Return the parent XmlSchemaCollection. If this schema was not initialized in a collection the return
     * value will be null.
     *
     * @return the parent collection.
     */
    public XmlSchemaCollection getParent() {
        return parent;
    }

    /**
     * Retrieve a DOM tree for this one schema, independent of any included or related schemas.
     *
     * @return The DOM document.
     * @throws XmlSchemaSerializerException
     */
    public Document getSchemaDocument() throws XmlSchemaSerializerException {
        XmlSchemaSerializer xser = new XmlSchemaSerializer();
        xser.setExtReg(this.parent.getExtReg());
        return xser.serializeSchema(this, false)[0];
    }

    /**
     * @return the namespace prefix for the target namespace.
     */
    public String getSchemaNamespacePrefix() {
        return schemaNamespacePrefix;
    }

    /**
     * Return a map containing all the defined types of this schema. The keys are QNames, where the namespace
     * will always be the target namespace of this schema. This makes it easier to look up items for
     * cross-schema references.
     *
     * @return the map of types.
     */
    public Map<QName, XmlSchemaType> getSchemaTypes() {
        return schemaTypes;
    }

    /**
     * Return the declared target namespace of this schema.
     *
     * @see #getLogicalTargetNamespace()
     * @return the namespace URI.
     */
    public String getTargetNamespace() {
        return syntacticalTargetNamespace;
    }

    /**
     * Search this schema, and its peers in its parent collection, for a schema type specified by QName.
     *
     * @param name the type name.
     * @return the type.
     */
    public XmlSchemaType getTypeByName(QName name) {
        return getTypeByName(name, true, null);
    }

    /**
     * Retrieve a named type from this schema.
     *
     * @param name
     * @return the type.
     */
    public XmlSchemaType getTypeByName(String name) {
        QName nameToSearchFor = new QName(this.getTargetNamespace(), name);
        return getTypeByName(nameToSearchFor, false, null);
    }

    /**
     * Return the declared XML Schema version of this schema. XmlSchema supports only version 1.0.
     *
     * @return
     */
    public String getVersion() {
        return version;
    }

    /**
     * Set the default attribute form for this schema.
     *
     * @param value the form. This may not be null.
     */
    public void setAttributeFormDefault(XmlSchemaForm value) {
        attributeFormDefault = value;
    }

    /**
     * Set the default block value for this schema.
     *
     * @param blockDefault the new block value.
     */
    public void setBlockDefault(XmlSchemaDerivationMethod blockDefault) {
        this.blockDefault = blockDefault;
    }

    /**
     * Set the default element form for this schema.
     *
     * @param elementFormDefault the element form. This may not be null.
     */
    public void setElementFormDefault(XmlSchemaForm elementFormDefault) {
        this.elementFormDefault = elementFormDefault;
    }

    /**
     * Set the default 'final' value for this schema. The value may not be null.
     *
     * @param finalDefault the new final value.
     */
    public void setFinalDefault(XmlSchemaDerivationMethod finalDefault) {
        this.finalDefault = finalDefault;
    }

    /**
     * Set the character encoding name for the schema. This is typically set when reading a schema from an XML
     * file, so that it can be written back out in the same encoding.
     *
     * @param encoding Character encoding name.
     */
    public void setInputEncoding(String encoding) {
        this.inputEncoding = encoding;
    }

    /**
     * Sets the schema elements namespace context. This may be used for schema serialization, until a better
     * mechanism was found.
     */
    public void setNamespaceContext(NamespacePrefixList namespaceContext) {
        this.namespaceContext = namespaceContext;
    }

    /**
     * Set the namespace prefix corresponding to the target namespace.
     *
     * @param schemaNamespacePrefix
     */
    public void setSchemaNamespacePrefix(String schemaNamespacePrefix) {
        this.schemaNamespacePrefix = schemaNamespacePrefix;
    }

    /**
     * Set the target namespace for this schema.
     *
     * @param targetNamespace the new target namespace URI. A value of "" is ignored.
     */
    public void setTargetNamespace(String targetNamespace) {
        if (!"".equals(targetNamespace)) {
            logicalTargetNamespace = targetNamespace;
            syntacticalTargetNamespace = targetNamespace;
        }
    }

    @Override
    public String toString() {
        return super.toString() + "[" + logicalTargetNamespace + "]";
    }

    /**
     * Serialize the schema as XML to the specified stream using the encoding established with
     * {@link #setInputEncoding(String)}.
     *
     * @param out - the output stream to write to
     * @throws UnsupportedEncodingException for an invalid encoding.
     */
    public void write(OutputStream out) throws UnsupportedEncodingException {
        if (this.inputEncoding != null && !"".equals(this.inputEncoding)) {
            write(new OutputStreamWriter(out, this.inputEncoding));
        } else {
            // As per the XML spec the default is taken to be UTF 8
            write(new OutputStreamWriter(out, UTF_8_ENCODING));
        }

    }

    /**
     * Serialize the schema as XML to the specified stream using the encoding established with
     * {@link #setInputEncoding(String)}.
     *
     * @param out - the output stream to write to
     * @param options - a map of options
     * @throws UnsupportedEncodingException
     */
    public void write(OutputStream out, Map<String, String> options) throws UnsupportedEncodingException {
        if (this.inputEncoding != null && !"".equals(this.inputEncoding)) {
            write(new OutputStreamWriter(out, this.inputEncoding), options);
        } else {
            write(new OutputStreamWriter(out, UTF_8_ENCODING), options);
        }
    }

    /**
     * Serialize the schema to a {@link java.io.Writer}.
     *
     * @param writer - the writer to write this
     */
    public void write(Writer writer) {
        serializeInternal(this, writer, null);
    }

    /**
     * Serialize the schema to a {@link java.io.Writer}.
     *
     * @param writer - the writer to write this
     */
    public void write(Writer writer, Map<String, String> options) {
        serializeInternal(this, writer, options);
    }

    protected XmlSchemaAttribute getAttributeByName(QName name, boolean deep, Stack<XmlSchema> schemaStack) {
        if (schemaStack != null && schemaStack.contains(this)) {
            // recursive schema - just return null
            return null;
        }
        XmlSchemaAttribute attribute = (XmlSchemaAttribute)attributes.get(name);
        if (deep) {
            if (attribute == null) {
                // search the imports
                for (XmlSchemaExternal item : externals) {

                    XmlSchema schema = getSchema(item);

                    if (schema != null) {
                        // create an empty stack - push the current parent in
                        // and
                        // use the protected method to process the schema
                        if (schemaStack == null) {
                            schemaStack = new Stack<XmlSchema>();
                        }
                        schemaStack.push(this);
                        attribute = schema.getAttributeByName(name, deep, schemaStack);
                        if (attribute != null) {
                            return attribute;
                        }
                    }
                }
            } else {
                return attribute;
            }
        }

        return attribute;

    }

    protected XmlSchemaAttributeGroup getAttributeGroupByName(QName name, boolean deep,
                                                              Stack<XmlSchema> schemaStack) {
        if (schemaStack != null && schemaStack.contains(this)) {
            // recursive schema - just return null
            return null;
        }

        XmlSchemaAttributeGroup group = attributeGroups.get(name);
        if (deep) {
            if (group == null) {
                // search the imports
                for (XmlSchemaExternal item : externals) {

                    XmlSchema schema = getSchema(item);

                    if (schema != null) {
                        // create an empty stack - push the current parent in
                        // and
                        // use the protected method to process the schema
                        if (schemaStack == null) {
                            schemaStack = new Stack<XmlSchema>();
                        }
                        schemaStack.push(this);
                        group = schema.getAttributeGroupByName(name, deep, schemaStack);
                        if (group != null) {
                            return group;
                        }
                    }
                }
            } else {
                return group;
            }
        }
        return group;
    }

    protected XmlSchemaElement getElementByName(QName name, boolean deep, Stack<XmlSchema> schemaStack) {
        if (schemaStack != null && schemaStack.contains(this)) {
            // recursive schema - just return null
            return null;
        }

        XmlSchemaElement element = elements.get(name);
        if (deep) {
            if (element == null) {
                // search the imports
                for (XmlSchemaExternal item : externals) {

                    XmlSchema schema = getSchema(item);

                    if (schema != null) {
                        // create an empty stack - push the current parent in
                        // and
                        // use the protected method to process the schema
                        if (schemaStack == null) {
                            schemaStack = new Stack<XmlSchema>();
                        }
                        schemaStack.push(this);
                        element = schema.getElementByName(name, deep, schemaStack);
                        if (element != null) {
                            return element;
                        }
                    }
                }
            } else {
                return element;
            }
        }
        return element;
    }

    protected XmlSchemaGroup getGroupByName(QName name, boolean deep, Stack<XmlSchema> schemaStack) {
        if (schemaStack != null && schemaStack.contains(this)) {
            // recursive schema - just return null
            return null;
        }
        XmlSchemaGroup group = groups.get(name);
        if (deep) {
            if (group == null) {
                // search the imports
                for (XmlSchemaExternal item : externals) {

                    XmlSchema schema = getSchema(item);

                    if (schema != null) {
                        // create an empty stack - push the current parent in
                        // and
                        // use the protected method to process the schema
                        if (schemaStack == null) {
                            schemaStack = new Stack<XmlSchema>();
                        }
                        schemaStack.push(this);
                        group = schema.getGroupByName(name, deep, schemaStack);
                        if (group != null) {
                            return group;
                        }
                    }
                }
            } else {
                return group;
            }
        }

        return group;

    }

    protected XmlSchemaNotation getNotationByName(QName name, boolean deep, Stack<XmlSchema> schemaStack) {
        if (schemaStack != null && schemaStack.contains(this)) {
            // recursive schema - just return null
            return null;
        }
        XmlSchemaNotation notation = notations.get(name);
        if (deep) {
            if (notation == null) {
                // search the imports
                for (XmlSchemaExternal item : externals) {

                    XmlSchema schema = getSchema(item);

                    if (schema != null) {
                        // create an empty stack - push the current parent in
                        // and
                        // use the protected method to process the schema
                        if (schemaStack == null) {
                            schemaStack = new Stack<XmlSchema>();
                        }
                        schemaStack.push(this);
                        notation = schema.getNotationByName(name, deep, schemaStack);
                        if (notation != null) {
                            return notation;
                        }
                    }
                }
            } else {
                return notation;
            }
        }

        return notation;

    }

    /**
     * Protected method that allows safe (non-recursive schema loading). It looks for a type with constraints.
     *
     * @param name
     * @param deep
     * @param schemaStack
     * @return the type.
     */
    protected XmlSchemaType getTypeByName(QName name, boolean deep, Stack<XmlSchema> schemaStack) {
        if (schemaStack != null && schemaStack.contains(this)) {
            // recursive schema - just return null
            return null;
        }
        XmlSchemaType type = schemaTypes.get(name);

        if (deep) {
            if (type == null) {
                // search the imports
                for (XmlSchemaExternal item : externals) {

                    XmlSchema schema = getSchema(item);

                    if (schema != null) {
                        // create an empty stack - push the current parent
                        // use the protected method to process the schema
                        if (schemaStack == null) {
                            schemaStack = new Stack<XmlSchema>();
                        }
                        schemaStack.push(this);
                        type = schema.getTypeByName(name, deep, schemaStack);
                        if (type != null) {
                            return type;
                        }
                    }
                }
            } else {
                return type;
            }
        }

        return type;
    }

    String getSyntacticalTargetNamespace() {
        return syntacticalTargetNamespace;
    }

    void setLogicalTargetNamespace(String logicalTargetNamespace) {
        this.logicalTargetNamespace = logicalTargetNamespace;
    }

    void setParent(XmlSchemaCollection parent) {
        this.parent = parent;
    }

    void setSyntacticalTargetNamespace(String syntacticalTargetNamespace) {
        this.syntacticalTargetNamespace = syntacticalTargetNamespace;
    }

    void setVersion(String version) {
        this.version = version;
    }

    /**
     * Get a schema from an import
     *
     * @param includeOrImport
     * @return return the schema object.
     */
    private XmlSchema getSchema(Object includeOrImport) {
        XmlSchema schema;
        if (includeOrImport instanceof XmlSchemaImport) {
            schema = ((XmlSchemaImport)includeOrImport).getSchema();
        } else if (includeOrImport instanceof XmlSchemaInclude) {
            schema = ((XmlSchemaInclude)includeOrImport).getSchema();
        } else {
            // skip ?
            schema = null;
        }

        return schema;
    }

    /**
     * Load the default options
     *
     * @param options - the map of
     */
    private void loadDefaultOptions(Map<String, String> options) {
        options.put(OutputKeys.OMIT_XML_DECLARATION, "yes");
        options.put(OutputKeys.INDENT, "yes");
    }

    /**
     * serialize the schema - this is the method tht does to work
     *
     * @param schema
     * @param out
     * @param options
     */
    private void serializeInternal(XmlSchema schema, Writer out, Map<String, String> options) {

        try {
            XmlSchemaSerializer xser = new XmlSchemaSerializer();
            xser.setExtReg(this.parent.getExtReg());
            Document[] serializedSchemas = xser.serializeSchema(schema, false);
            TransformerFactory trFac = TransformerFactory.newInstance();

            try {
                trFac.setAttribute("indent-number", "4");
            } catch (IllegalArgumentException e) {
                // do nothing - we'll just silently let this pass if it
                // was not compatible
            }

            Source source = new DOMSource(serializedSchemas[0]);
            Result result = new StreamResult(out);
            javax.xml.transform.Transformer tr = trFac.newTransformer();

            // use the input encoding if there is one
            if (schema.inputEncoding != null && !"".equals(schema.inputEncoding)) {
                tr.setOutputProperty(OutputKeys.ENCODING, schema.inputEncoding);
            }

            // let these be configured from outside if any is present
            // Note that one can enforce the encoding by passing the necessary
            // property in options

            if (options == null) {
                options = new HashMap<String, String>();
                loadDefaultOptions(options);
            }
            Iterator<String> keys = options.keySet().iterator();
            while (keys.hasNext()) {
                Object key = keys.next();
                tr.setOutputProperty((String)key, options.get(key));
            }

            tr.transform(source, result);
            out.flush();
        } catch (TransformerConfigurationException e) {
            throw new XmlSchemaException(e.getMessage());
        } catch (TransformerException e) {
            throw new XmlSchemaException(e.getMessage());
        } catch (XmlSchemaSerializer.XmlSchemaSerializerException e) {
            throw new XmlSchemaException(e.getMessage());
        } catch (IOException e) {
            throw new XmlSchemaException(e.getMessage());
        }
    }
}
