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
import java.io.Reader;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.extensions.ExtensionRegistry;
import org.apache.ws.commons.schema.resolver.CollectionURIResolver;
import org.apache.ws.commons.schema.resolver.DefaultURIResolver;
import org.apache.ws.commons.schema.resolver.URIResolver;
import org.apache.ws.commons.schema.utils.DOMUtil;
import org.apache.ws.commons.schema.utils.NamespacePrefixList;
import org.apache.ws.commons.schema.utils.TargetNamespaceValidator;

/**
 * Contains a cache of XML Schema definition language (XSD).
 */
public final class XmlSchemaCollection {

    /**
     * base URI is used as the base for loading the imports
     */
    String baseUri;

    /**
     * Key that identifies a schema in a collection, composed of a targetNamespace and a system ID.
     */
    public static class SchemaKey {
        private final String namespace;
        private final String systemId;

        SchemaKey(String pNamespace, String pSystemId) {
            namespace = pNamespace == null ? Constants.NULL_NS_URI : pNamespace;
            systemId = pSystemId == null ? "" : pSystemId;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
            result = prime * result + ((systemId == null) ? 0 : systemId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            SchemaKey other = (SchemaKey)obj;
            if (namespace == null) {
                if (other.namespace != null) {
                    return false;
                }
            } else if (!namespace.equals(other.namespace)) {
                return false;
            }
            if (systemId == null) {
                if (other.systemId != null) {
                    return false;
                }
            } else if (!systemId.equals(other.systemId)) {
                return false;
            }
            return true;
        }



        public String toString() {
            return Constants.NULL_NS_URI.equals(namespace) ? systemId : "{" + namespace + "}" + systemId;
        }

        String getNamespace() {
            return namespace;
        }

        String getSystemId() {
            return systemId;
        }
    }

    /**
     * stack to track imports (to prevent recursion)
     */
    Stack<SchemaKey> stack;
    Map<QName, List<TypeReceiver>> unresolvedTypes;
    XmlSchema xsd;
    // the default extension registry
    private ExtensionRegistry extReg;

    /**
     * This map contains a list of Schema objects keyed in by their namespaces.
     *  When resolving schemas, this map will be checked for the presence of the schema first.
     */
    private Map<String, XmlSchema> knownNamespaceMap;

    /**
     * In-scope namespaces for XML processing
     */
    private NamespacePrefixList namespaceContext;

    /**
     * Resolver to find included schemas.
     */
    private URIResolver schemaResolver;
    /**
     * Map of included schemas.
     */
    private Map<SchemaKey, XmlSchema> schemas;

    /**
     * Creates new XmlSchemaCollection
     */
    public XmlSchemaCollection() {
        init();
    }

    /**
     * Return an indication of whether a particular schema is in the working stack of schemas. This function,
     * while public, is probably not useful outside of the implementation.
     * 
     * @param pKey schema key
     * @return true if the schema is in the stack.
     */
    public boolean check(SchemaKey pKey) {
        return stack.indexOf(pKey) == -1;
    }

    public ExtensionRegistry getExtReg() {
        return extReg;
    }

    /**
     * get the namespace map
     * 
     * @return a map of previously known XMLSchema objects keyed by their namespace (String)
     */
    public Map<String, XmlSchema> getKnownNamespaceMap() {
        return knownNamespaceMap;
    }

    /**
     * Retrieve the namespace context.
     * 
     * @return the namespace context.
     */
    public NamespacePrefixList getNamespaceContext() {
        return namespaceContext;
    }

    /**
     * Retrieve the custom URI resolver, if any.
     * 
     * @return the current resolver.
     */
    public URIResolver getSchemaResolver() {
        return schemaResolver;
    }

    /**
     * Retrieve a global type from the schema collection.
     * 
     * @param schemaTypeName the QName of the type.
     * @return the type object, or null.
     */
    public XmlSchemaType getTypeByQName(QName schemaTypeName) {
        String uri = schemaTypeName.getNamespaceURI();
        for (Iterator iter = schemas.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry)iter.next();
            if (((SchemaKey)entry.getKey()).getNamespace().equals(uri)) {
                XmlSchemaType type = ((XmlSchema)entry.getValue()).getTypeByName(schemaTypeName);
                if (type != null) {
                    return type;
                }
            }
        }
        return null;
    }

    /**
     * Retrieve a set containing the XmlSchema instances with the given system ID. In general, this will
     * return a single instance, or none. However, if the schema has no targetNamespace attribute and was
     * included from schemata with different target namespaces, then it may occur, that multiple schema
     * instances with different logical target namespaces may be returned.
     * 
     * @param systemId the system id for this schema
     * @return array of XmlSchema objects
     */
    public XmlSchema[] getXmlSchema(String systemId) {
        if (systemId == null) {
            systemId = "";
        }
        final List<XmlSchema> result = new ArrayList<XmlSchema>();
        for (Map.Entry<SchemaKey, XmlSchema> entry : schemas.entrySet()) {
            if (((SchemaKey)entry.getKey()).getSystemId().equals(systemId)) {
                result.add(entry.getValue());
            }
        }
        return result.toArray(new XmlSchema[result.size()]);
    }

    /**
     * Returns an array of all the XmlSchemas in this collection.
     * 
     * @return the list of XmlSchema objects
     */
    public XmlSchema[] getXmlSchemas() {
        Collection<XmlSchema> c = schemas.values();
        return c.toArray(new XmlSchema[c.size()]);
    }

    /**
     * This section should comply to the XMLSchema specification; see <a
     * href="http://www.w3.org/TR/2004/PER-xmlschema-2-20040318/datatypes.html#built-in-datatypes">
     * http://www.w3.org/TR/2004/PER-xmlschema-2-20040318/datatypes.html#built-in-datatypes</a>. This needs to
     * be inspected by another pair of eyes
     */
    public void init() {
        
        stack = new Stack<SchemaKey>();
        unresolvedTypes = new HashMap<QName, List<TypeReceiver>>();
        extReg = new ExtensionRegistry();
        knownNamespaceMap = new HashMap<String, XmlSchema>();
        schemaResolver = new DefaultURIResolver();
        schemas = new HashMap<SchemaKey, XmlSchema>();
        // LAST, since the ctor for XmlSchema will reach back into here. 
        xsd = new XmlSchema(XmlSchema.SCHEMA_NS, this);
        /*
         * Defined in section 4.
         */
        addSimpleType(xsd, Constants.XSD_ANYSIMPLETYPE.getLocalPart());
        addSimpleType(xsd, Constants.XSD_ANYTYPE.getLocalPart());

        /*
         * Primitive types 3.2.1 string 3.2.2 boolean 3.2.3 decimal 3.2.4 float 3.2.5 double 3.2.6 duration
         * 3.2.7 dateTime 3.2.8 time 3.2.9 date 3.2.10 gYearMonth 3.2.11 gYear 3.2.12 gMonthDay 3.2.13 gDay
         * 3.2.14 gMonth 3.2.15 hexBinary 3.2.16 base64Binary 3.2.17 anyURI 3.2.18 QName 3.2.19 NOTATION
         */
        addSimpleType(xsd, Constants.XSD_STRING.getLocalPart());
        addSimpleType(xsd, Constants.XSD_BOOLEAN.getLocalPart());
        addSimpleType(xsd, Constants.XSD_FLOAT.getLocalPart());
        addSimpleType(xsd, Constants.XSD_DOUBLE.getLocalPart());
        addSimpleType(xsd, Constants.XSD_QNAME.getLocalPart());
        addSimpleType(xsd, Constants.XSD_DECIMAL.getLocalPart());
        addSimpleType(xsd, Constants.XSD_DURATION.getLocalPart());
        addSimpleType(xsd, Constants.XSD_DATE.getLocalPart());
        addSimpleType(xsd, Constants.XSD_TIME.getLocalPart());
        addSimpleType(xsd, Constants.XSD_DATETIME.getLocalPart());
        addSimpleType(xsd, Constants.XSD_DAY.getLocalPart());
        addSimpleType(xsd, Constants.XSD_MONTH.getLocalPart());
        addSimpleType(xsd, Constants.XSD_MONTHDAY.getLocalPart());
        addSimpleType(xsd, Constants.XSD_YEAR.getLocalPart());
        addSimpleType(xsd, Constants.XSD_YEARMONTH.getLocalPart());
        addSimpleType(xsd, Constants.XSD_NOTATION.getLocalPart());
        addSimpleType(xsd, Constants.XSD_HEXBIN.getLocalPart());
        addSimpleType(xsd, Constants.XSD_BASE64.getLocalPart());
        addSimpleType(xsd, Constants.XSD_ANYURI.getLocalPart());

        /*
         * 3.3.1 normalizedString 3.3.2 token 3.3.3 language 3.3.4 NMTOKEN 3.3.5 NMTOKENS 3.3.6 Name 3.3.7
         * NCName 3.3.8 ID 3.3.9 IDREF 3.3.10 IDREFS 3.3.11 ENTITY 3.3.12 ENTITIES 3.3.13 integer 3.3.14
         * nonPositiveInteger 3.3.15 negativeInteger 3.3.16 long 3.3.17 int 3.3.18 short 3.3.19 byte 3.3.20
         * nonNegativeInteger 3.3.21 unsignedLong 3.3.22 unsignedInt 3.3.23 unsignedShort 3.3.24 unsignedByte
         * 3.3.25 positiveInteger
         */

        // derived types from decimal
        addSimpleType(xsd, Constants.XSD_LONG.getLocalPart());
        addSimpleType(xsd, Constants.XSD_SHORT.getLocalPart());
        addSimpleType(xsd, Constants.XSD_BYTE.getLocalPart());
        addSimpleType(xsd, Constants.XSD_INTEGER.getLocalPart());
        addSimpleType(xsd, Constants.XSD_INT.getLocalPart());
        addSimpleType(xsd, Constants.XSD_POSITIVEINTEGER.getLocalPart());
        addSimpleType(xsd, Constants.XSD_NEGATIVEINTEGER.getLocalPart());
        addSimpleType(xsd, Constants.XSD_NONPOSITIVEINTEGER.getLocalPart());
        addSimpleType(xsd, Constants.XSD_NONNEGATIVEINTEGER.getLocalPart());
        addSimpleType(xsd, Constants.XSD_UNSIGNEDBYTE.getLocalPart());
        addSimpleType(xsd, Constants.XSD_UNSIGNEDINT.getLocalPart());
        addSimpleType(xsd, Constants.XSD_UNSIGNEDLONG.getLocalPart());
        addSimpleType(xsd, Constants.XSD_UNSIGNEDSHORT.getLocalPart());

        // derived types from string
        addSimpleType(xsd, Constants.XSD_NAME.getLocalPart());
        addSimpleType(xsd, Constants.XSD_NORMALIZEDSTRING.getLocalPart());
        addSimpleType(xsd, Constants.XSD_NCNAME.getLocalPart());
        addSimpleType(xsd, Constants.XSD_NMTOKEN.getLocalPart());
        addSimpleType(xsd, Constants.XSD_NMTOKENS.getLocalPart());
        addSimpleType(xsd, Constants.XSD_ENTITY.getLocalPart());
        addSimpleType(xsd, Constants.XSD_ENTITIES.getLocalPart());
        addSimpleType(xsd, Constants.XSD_ID.getLocalPart());
        addSimpleType(xsd, Constants.XSD_IDREF.getLocalPart());
        addSimpleType(xsd, Constants.XSD_IDREFS.getLocalPart());
        addSimpleType(xsd, Constants.XSD_LANGUAGE.getLocalPart());
        addSimpleType(xsd, Constants.XSD_TOKEN.getLocalPart());

        // SchemaKey key = new SchemaKey(XmlSchema.SCHEMA_NS, null);
        // addSchema(key, xsd);

        // look for a system property to see whether we have a registered
        // extension registry class. if so we'll instantiate a new one
        // and set it as the extension registry
        // if there is an error, we'll just print out a message and move on.

        if (System.getProperty(Constants.SystemConstants.EXTENSION_REGISTRY_KEY) != null) {
            try {
                Class clazz = Class.forName(System
                    .getProperty(Constants.SystemConstants.EXTENSION_REGISTRY_KEY));
                this.extReg = (ExtensionRegistry)clazz.newInstance();
            } catch (ClassNotFoundException e) {
                System.err.println("The specified extension registry class cannot be found!");
            } catch (InstantiationException e) {
                System.err.println("The specified extension registry class cannot be instantiated!");
            } catch (IllegalAccessException e) {
                System.err.println("The specified extension registry class cannot be accessed!");
            }
        }
    }

    /**
     * Pop the stack of schemas. This function, while public, is probably not useful outside of the
     * implementation.
     */
    public void pop() {
        stack.pop();
    }

    /**
     * Push a schema onto the stack of schemas. This function, while public, is probably not useful outside of
     * the implementation.
     * 
     * @param pKey the schema key.
     */
    public void push(SchemaKey pKey) {
        stack.push(pKey);
    }

    /**
     * Read an XML Schema from a complete XSD XML DOM Document into this collection. Schemas in a collection
     * must be unique in the concatenation of SystemId and targetNamespace.
     * 
     * @param doc The schema document.
     * @param systemId System ID for this schema.
     * @return the schema object.
     */
    public XmlSchema read(Document doc, String systemId) {
        return read(doc, systemId, null);
    }

    /**
     * Read an XML Schema from a complete XSD XML DOM Document into this collection. Schemas in a collection
     * must be unique in the concatenation of SystemId and targetNamespace.
     * 
     * @param doc Source document.
     * @param systemId System id.
     * @param validator object that is called back to check validity of the target namespace.
     * @return the schema object.
     */
    public XmlSchema read(Document doc, String systemId, TargetNamespaceValidator validator) {
        SchemaBuilder builder = new SchemaBuilder(this, validator);
        XmlSchema schema = builder.build(doc, systemId);
        schema.setInputEncoding(DOMUtil.getInputEncoding(doc));
        return schema;
    }

    /**
     * Read an XML schema into the collection from a DOM document. Schemas in a collection must be unique in
     * the concatenation of system ID and targetNamespace. In this API, the systemID is taken from the
     * document.
     * 
     * @param doc the XSD document.
     * @param veh handler that is called back for validation.
     * @return the XML schema object.
     */
    public XmlSchema read(Document doc) {
        SchemaBuilder builder = new SchemaBuilder(this, null);
        return builder.build(doc, null);
    }

    /**
     * Read an XML Schema into the collection from a DOM element. Schemas in a collection must be unique in
     * the concatentation of System ID and targetNamespace. The system ID will be empty for this API.
     * 
     * @param elem the DOM element for the schema.
     * @return the XmlSchema
     */
    public XmlSchema read(Element elem) {
        SchemaBuilder builder = new SchemaBuilder(this, null);
        XmlSchema xmlSchema = builder.handleXmlSchemaElement(elem, null);
        xmlSchema.setInputEncoding(DOMUtil.getXmlEncoding(elem.getOwnerDocument()));
        return xmlSchema;
    }

    /**
     * Read a schema from a DOM tree into the collection. The schemas in a collection must be unique in the
     * concatenation of the target namespace and the system ID.
     * 
     * @param elem xs:schema DOM element.
     * @param systemId System id.
     * @return the schema object.
     */
    public XmlSchema read(Element elem, String systemId) {
        SchemaBuilder builder = new SchemaBuilder(this, null);
        XmlSchema xmlSchema = builder.handleXmlSchemaElement(elem, systemId);
        xmlSchema.setInputEncoding(DOMUtil.getInputEncoding(elem.getOwnerDocument()));
        return xmlSchema;
    }

    /**
     * Read an XML schema into the collection from a SAX InputSource. Schemas in a collection must be unique
     * in the concatenation of system ID and targetNamespace. In this API, the systemID is taken from the
     * source.
     * 
     * @param inputSource the XSD document.
     * @return the XML schema object.
     */
    public XmlSchema read(InputSource inputSource) {
        return read(inputSource, null);
    }

    public XmlSchema read(Reader r) {
        return read(new InputSource(r));
    }

    /**
     * Read an XML schema into the collection from a TRaX source. Schemas in a collection must be unique in
     * the concatenation of system ID and targetNamespace. In this API, the systemID is taken from the Source.
     * 
     * @param source the XSD document.
     * @param veh handler that is called back for validation.
     * @return the XML schema object.
     */
    public XmlSchema read(Source source) {
        if (source instanceof SAXSource) {
            return read(((SAXSource)source).getInputSource());
        } else if (source instanceof DOMSource) {
            Node node = ((DOMSource)source).getNode();
            if (node instanceof Document) {
                node = ((Document)node).getDocumentElement();
            }
            return read((Document)node);
        } else if (source instanceof StreamSource) {
            StreamSource ss = (StreamSource)source;
            InputSource isource = new InputSource(ss.getSystemId());
            isource.setByteStream(ss.getInputStream());
            isource.setCharacterStream(ss.getReader());
            isource.setPublicId(ss.getPublicId());
            return read(isource);
        } else {
            InputSource isource = new InputSource(source.getSystemId());
            return read(isource);
        }
    }

    /**
     * Return the schema from this collection for a particular targetNamespace.
     * 
     * @param uri target namespace URI.
     * @return the schema.
     */
    public XmlSchema schemaForNamespace(String uri) {
        for (Iterator iter = schemas.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry)iter.next();
            if (((SchemaKey)entry.getKey()).getNamespace().equals(uri)) {
                return (XmlSchema)entry.getValue();
            }
        }
        return null;
    }

    /**
     * Set the base URI. This is used when schemas need to be loaded from relative locations
     * 
     * @param baseUri baseUri for this collection.
     */
    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
        if (schemaResolver instanceof CollectionURIResolver) {
            CollectionURIResolver resolverWithBase = (CollectionURIResolver)schemaResolver;
            resolverWithBase.setCollectionBaseURI(baseUri);
        }
    }

    public void setExtReg(ExtensionRegistry extReg) {
        this.extReg = extReg;
    }

    /**
     * sets the known namespace map
     * 
     * @param knownNamespaceMap a map of previously known XMLSchema objects keyed by their namespace (String)
     */
    public void setKnownNamespaceMap(Map<String, XmlSchema> knownNamespaceMap) {
        this.knownNamespaceMap = knownNamespaceMap;
    }

    /**
     * Set the namespace context for this collection, which controls the assignment of namespace prefixes to
     * namespaces.
     * 
     * @param namespaceContext the context.
     */
    public void setNamespaceContext(NamespacePrefixList namespaceContext) {
        this.namespaceContext = namespaceContext;
    }

    /**
     * Register a custom URI resolver
     * 
     * @param schemaResolver resolver
     */
    public void setSchemaResolver(URIResolver schemaResolver) {
        this.schemaResolver = schemaResolver;
    }

    public String toString() {
        return super.toString() + "[" + schemas.toString() + "]";
    }

    void addSchema(SchemaKey pKey, XmlSchema pSchema) {
        if (schemas.containsKey(pKey)) {
            throw 
                new IllegalStateException("A schema with target namespace " 
                                          + pKey.getNamespace()
                                          + " and system ID " 
                                          + pKey.getSystemId() + " is already present.");
        }
        schemas.put(pKey, pSchema);
    }

    void addUnresolvedType(QName type, TypeReceiver receiver) {
        List<TypeReceiver> receivers = unresolvedTypes.get(type);
        if (receivers == null) {
            receivers = new ArrayList<TypeReceiver>();
            unresolvedTypes.put(type, receivers);
        }
        receivers.add(receiver);
    }

    boolean containsSchema(SchemaKey pKey) {
        return schemas.containsKey(pKey);
    }

    /**
     * gets a schema from the external namespace map
     * 
     * @param namespace
     * @return
     */
    XmlSchema getKnownSchema(String namespace) {
        return (XmlSchema)knownNamespaceMap.get(namespace);
    }

    /**
     * Get a schema given a SchemaKey
     * 
     * @param pKey
     * @return
     */
    XmlSchema getSchema(SchemaKey pKey) {
        return schemas.get(pKey);
    }

    XmlSchema read(InputSource inputSource, TargetNamespaceValidator namespaceValidator) {
        try {
            DocumentBuilderFactory docFac = DocumentBuilderFactory.newInstance();
            docFac.setNamespaceAware(true);
            final DocumentBuilder builder = docFac.newDocumentBuilder();
            Document doc = null;
            doc = parseDoPriv(inputSource, builder, doc);
            return read(doc, inputSource.getSystemId(), namespaceValidator);
        } catch (ParserConfigurationException e) {
            throw new XmlSchemaException(e.getMessage());
        } catch (IOException e) {
            throw new XmlSchemaException(e.getMessage());
        } catch (SAXException e) {
            throw new XmlSchemaException(e.getMessage());
        }
    }

    void resolveType(QName typeName, XmlSchemaType type) {
        List receivers = unresolvedTypes.get(typeName);
        if (receivers == null) {
            return;
        }
        for (Iterator i = receivers.iterator(); i.hasNext();) {
            TypeReceiver receiver = (TypeReceiver)i.next();
            receiver.setType(type);
        }
        unresolvedTypes.remove(typeName);
    }

    private void addSimpleType(XmlSchema schema, String typeName) {
        XmlSchemaSimpleType type;
        type = new XmlSchemaSimpleType(schema, true);
        type.setName(typeName);
    }

    private Document parseDoPriv(final InputSource inputSource, final DocumentBuilder builder, Document doc)
        throws IOException, SAXException {
        try {
            doc = java.security.AccessController.doPrivileged(new PrivilegedExceptionAction<Document>() {
                public Document run() throws IOException, SAXException {
                    return builder.parse(inputSource);
                }
            });
        } catch (PrivilegedActionException e) {
            Exception exception = e.getException();
            if (exception instanceof IOException) {
                throw (IOException)exception;
            }
            if (exception instanceof SAXException) {
                throw (SAXException)exception;
            }
        }
        return doc;
    }
    
    /**
     * Find a global attribute by QName in this collection of schemas.
     * 
     * @param schemaAttributeName the name of the attribute.
     * @return the attribute or null.
     */
    public XmlSchemaAttribute getAttributeByQName(QName schemaAttributeName) {
        String uri = schemaAttributeName.getNamespaceURI();
        for (Iterator iter = schemas.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry)iter.next();
            if (((SchemaKey)entry.getKey()).getNamespace().equals(uri)) {
                XmlSchemaAttribute attribute = ((XmlSchema)entry.getValue())
                    .getAttributeByName(schemaAttributeName);
                if (attribute != null) {
                    return attribute;
                }
            }
        }
        return null;
    }

    /**
     * Retrieve a global element from the schema collection.
     * 
     * @param qname the element QName.
     * @return the element object, or null.
     */
    public XmlSchemaElement getElementByQName(QName qname) {
        String uri = qname.getNamespaceURI();
        for (Iterator iter = schemas.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry)iter.next();
            if (((SchemaKey)entry.getKey()).getNamespace().equals(uri)) {
                XmlSchemaElement element = ((XmlSchema)entry.getValue()).getElementByName(qname);
                if (element != null) {
                    return element;
                }
            }
        }
        return null;
    }

    
    public XmlSchemaAttributeGroup getAttributeGroupByQName(QName name) {
        String uri = name.getNamespaceURI();
        for (Iterator iter = schemas.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry)iter.next();
            if (((SchemaKey)entry.getKey()).getNamespace().equals(uri)) {
                XmlSchemaAttributeGroup group = ((XmlSchema)entry.getValue())
                    .getAttributeGroupByName(name);
                if (group != null) {
                    return group;
                }
            }
        }
        return null;
    }
    
    public XmlSchemaGroup getGroupByQName(QName name) {
        String uri = name.getNamespaceURI();
        for (Iterator iter = schemas.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry)iter.next();
            if (((SchemaKey)entry.getKey()).getNamespace().equals(uri)) {
                XmlSchemaGroup group = ((XmlSchema)entry.getValue())
                    .getGroupByName(name);
                if (group != null) {
                    return group;
                }
            }
        }
        return null;
    }
    
    public XmlSchemaNotation getNotationByQName(QName name) {
        String uri = name.getNamespaceURI();
        for (Iterator iter = schemas.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry)iter.next();
            if (((SchemaKey)entry.getKey()).getNamespace().equals(uri)) {
                XmlSchemaNotation notation = ((XmlSchema)entry.getValue())
                    .getNotationByName(name);
                if (notation != null) {
                    return notation;
                }
            }
        }
        return null;
    }

}
