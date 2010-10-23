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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.extensions.ExtensionRegistry;
import org.apache.ws.commons.schema.utils.NamespacePrefixList;

/**
 * Convert from the XML Schema class representation to the standard XML representation.
 */
public class XmlSchemaSerializer {

    /**
     * Exception class used for serialization problems.
     */
    public static class XmlSchemaSerializerException
        extends Exception {

        private static final long serialVersionUID = 1L;

        /**
         * Standard constructor with a message.
         *
         * @param msg the message.
         */
        public XmlSchemaSerializerException(String msg) {
            super(msg);
        }
    }

    public static final String XSD_NAMESPACE = XMLConstants.W3C_XML_SCHEMA_NS_URI;
    private static final String XMLNS_NAMESPACE_URI = "http://www.w3.org/2000/xmlns/";

    String xsdPrefix = "xs";
    List<Document> docs;
    Element schemaElement;
    /**
     * Extension registry for the serializer
     */

    private ExtensionRegistry extReg;
    private Map<String, String> schemaNamespace;

    /**
     * Create a new serializer.
     */
    public XmlSchemaSerializer() {
        docs = new ArrayList<Document>();
        schemaNamespace = Collections.synchronizedMap(new HashMap<String, String>());
    }

    // break string with prefix into two parts part[0]:prefix , part[1]:namespace
    private static String[] getParts(String name) {
        String[] parts = new String[2];

        int index = name.indexOf(":");
        if (index > -1) {
            parts[0] = name.substring(0, index);
            parts[1] = name.substring(index + 1);
        } else {
            parts[0] = "";
            parts[1] = name;
        }
        return parts;
    }

    /**
     * Get the registry of extensions for this serializer.
     *
     * @return the registry.
     */
    public ExtensionRegistry getExtReg() {
        return extReg;
    }

    /**
     * Serialize an entire schema, returning an array of DOM Documents, one per XSL file.
     * If serializeIncluded is false, this will always return a single DOM document. If it is true,
     * and there are external elements in this schema (include, import, or redefine), and
     * they contain references to {@link XmlSchema} objects to represent them, they will
     * be returned as additional documents in the array.
     *
     * @param schemaObj The XML Schema.
     * @param serializeIncluded whether to create DOM trees for any included or imported schemas.
     * @return Documents. If serializeIncluded is false, the array with have one entry. The main document is
     *         always first.
     * @throws XmlSchemaSerializerException
     */
    public Document[] serializeSchema(XmlSchema schemaObj, boolean serializeIncluded)
        throws XmlSchemaSerializerException {
        return serializeSchemaElement(schemaObj, serializeIncluded);
    }

    /**
     * Set the registry of extensions for this serializer.
     *
     * @param extReg the registry.
     */
    public void setExtReg(ExtensionRegistry extReg) {
        this.extReg = extReg;
    }

    /**
     * Serialize an 'all' item.
     * @param doc
     * @param allObj
     * @param schema
     * @return
     * @throws XmlSchemaSerializerException
     */
    Element serializeAll(Document doc, XmlSchemaAll allObj, XmlSchema schema)
        throws XmlSchemaSerializerException {
        Element allEl = createNewElement(doc, "all", schema.getSchemaNamespacePrefix(), XmlSchema.SCHEMA_NS);

        serializeMaxMinOccurs(allObj, allEl);

        if (allObj.getAnnotation() != null) {
            Element annotation = serializeAnnotation(doc, allObj.getAnnotation(), schema);
            allEl.appendChild(annotation);
        }

        List<XmlSchemaElement> itemColl = allObj.getItems();

        if (itemColl != null) {
            int itemLength = itemColl.size();

            for (int i = 0; i < itemLength; i++) {
                XmlSchemaObject obj = itemColl.get(i);
                if (obj instanceof XmlSchemaElement) {
                    Element el = serializeElement(doc, (XmlSchemaElement)obj, schema);
                    allEl.appendChild(el);
                } else {
                    throw new XmlSchemaSerializerException("Only element "
                                                           + "allowed as child of all model type");
                }
            }
        }

        // process extension
        processExtensibilityComponents(allObj, allEl);

        return allEl;
    }

    /**
     * ********************************************************************* Element
     * serializeAnnotation(Document doc, XmlSchemaAnnotation annotationObj, XmlSchema schema)
     * <p/>
     * <p/>
     * Each member of complex type will be appended and pass the element created. Complex type processed
     * according to w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. annotationObj - XmlSchemaAnnotation that will be serialized.
     * schema - Schema Document object of the parent.
     * <p/>
     * Return: annotation element that part of any type. will contain document and appinfo for child.
     * **********************************************************************
     */
    Element serializeAnnotation(Document doc, XmlSchemaAnnotation annotationObj, XmlSchema schema) {

        Element annotation = createNewElement(doc, "annotation", schema.getSchemaNamespacePrefix(),
                                              XmlSchema.SCHEMA_NS);

        List<XmlSchemaAnnotationItem> contents = annotationObj.getItems();
        int contentLength = contents.size();

        for (int i = 0; i < contentLength; i++) {
            XmlSchemaObject obj = contents.get(i);

            if (obj instanceof XmlSchemaAppInfo) {
                XmlSchemaAppInfo appinfo = (XmlSchemaAppInfo)obj;
                Element appInfoEl = serializeAppInfo(doc, appinfo, schema);
                annotation.appendChild(appInfoEl);
            } else if (obj instanceof XmlSchemaDocumentation) {
                XmlSchemaDocumentation documentation = (XmlSchemaDocumentation)obj;

                Element documentationEl = serializeDocumentation(doc, documentation, schema);

                annotation.appendChild(documentationEl);
            }
        }

        // process extension
        processExtensibilityComponents(annotationObj, annotation);

        return annotation;
    }

    /**
     * ********************************************************************* Element serializeAny(Document
     * doc, XmlSchemaAny anyObj, XmlSchema schema)
     * <p/>
     * Each member of complex type will be appended and pass the element created. Complex type processed
     * according to w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. anyObj - XmlSchemaAny that will be serialized. schema -
     * Schema Document object of the parent.
     * <p/>
     * Return: Element of any that is part of its parent.
     * **********************************************************************
     */
    Element serializeAny(Document doc, XmlSchemaAny anyObj, XmlSchema schema) {
        Element anyEl = createNewElement(doc, "any", schema.getSchemaNamespacePrefix(), XmlSchema.SCHEMA_NS);
        if (anyObj.getId() != null && anyObj.getId().length() > 0) {
            anyEl.setAttribute("id", anyObj.getId());
        }

        serializeMaxMinOccurs(anyObj, anyEl);

        if (anyObj.getNamespace() != null) {
            anyEl.setAttribute("namespace", anyObj.getNamespace());
        }

        if (anyObj.getProcessContent() != null
            && anyObj.getProcessContent() != XmlSchemaContentProcessing.NONE) {
            anyEl.setAttribute("processContents", anyObj.getProcessContent().toString());
        }

        if (anyObj.getAnnotation() != null) {
            Element annotation = serializeAnnotation(doc, anyObj.getAnnotation(), schema);
            anyEl.appendChild(annotation);
        }

        // process extension
        processExtensibilityComponents(anyObj, anyEl);

        return anyEl;
    }

    /**
     * ********************************************************************* Element
     * serializeAnyAttribute(Document doc, XmlSchemaAnyAttribute anyAttributeObj, XmlSchema schema)
     * <p/>
     * Each member of complex type will be appended and pass the element created. Complex type processed
     * according to w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. anyAttributeObj - XmlSchemaAnyAttribute that will be
     * serialized. schema - Schema Document object of the parent.
     * <p/>
     * Return: Element of any attribute element.
     * **********************************************************************
     */
    Element serializeAnyAttribute(Document doc, XmlSchemaAnyAttribute anyAttributeObj, XmlSchema schema) {

        Element anyAttribute = createNewElement(doc, "anyAttribute", schema.getSchemaNamespacePrefix(),
                                                XmlSchema.SCHEMA_NS);

        if (anyAttributeObj.namespace != null) {
            anyAttribute.setAttribute("namespace", anyAttributeObj.namespace);
        }

        if (anyAttributeObj.getId() != null) {
            anyAttribute.setAttribute("id", anyAttributeObj.getId());
        }

        if (anyAttributeObj.processContent != null
            && anyAttributeObj.processContent != XmlSchemaContentProcessing.NONE) {
            anyAttribute.setAttribute("processContents", anyAttributeObj.processContent.toString());
        }
        if (anyAttributeObj.getAnnotation() != null) {
            Element annotation = serializeAnnotation(doc, anyAttributeObj.getAnnotation(), schema);
            anyAttribute.appendChild(annotation);
        }

        // process extension
        processExtensibilityComponents(anyAttributeObj, anyAttribute);

        return anyAttribute;
    }

    /**
     * ********************************************************************* Element serializeAppInfo(Document
     * doc,XmlSchemaAppInfo appInfoObj, XmlSchema schema)
     * <p/>
     * <p/>
     * Each member of complex type will be appended and pass the element created. Complex type processed
     * according to w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. appInfoObj - XmlSchemaAppInfo that will be serialized. schema
     * - Schema Document object of the parent.
     * <p/>
     * Return: App info element that is part of the annotation.
     * **********************************************************************
     */
    Element serializeAppInfo(Document doc, XmlSchemaAppInfo appInfoObj, XmlSchema schema) {

        Element appInfoEl = createNewElement(doc, "appinfo", schema.getSchemaNamespacePrefix(),
                                             XmlSchema.SCHEMA_NS);
        if (appInfoObj.source != null) {
            appInfoEl.setAttribute("source", appInfoObj.source);
        }

        if (appInfoObj.markup != null) {
            int markupLength = appInfoObj.markup.getLength();
            for (int j = 0; j < markupLength; j++) {
                Node n = (Node)appInfoObj.markup.item(j);
                appInfoEl.appendChild(doc.importNode(n, true));
            }
        }

        // process extension
        processExtensibilityComponents(appInfoObj, appInfoEl);

        return appInfoEl;
    }

    /**
     * ********************************************************************* Element
     * serializeAttribute(Document doc, XmlSchemaAttribute attributeObj, XmlSchema schema) throws
     * XmlSchemaSerializerException{
     * <p/>
     * Each member of complex type will be appended and pass the element created. `Complex type processed
     * according to w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. attributeObj - XmlSchemaAttribute that will be serialized.
     * schema - Schema Document object of the parent.
     * <p/>
     * Return: Element of attribute. **********************************************************************
     */
    Element serializeAttribute(Document doc, XmlSchemaAttribute attributeObj, XmlSchema schema)
        throws XmlSchemaSerializerException {

        boolean refPresent = attributeObj.getRef().getTargetQName() != null;

        Element attribute = createNewElement(doc, "attribute", schema.getSchemaNamespacePrefix(),
                                             XmlSchema.SCHEMA_NS);
        if (refPresent) {
            String refName = resolveQName(attributeObj.getRef().getTargetQName(), schema);
            attribute.setAttribute("ref", refName);
        } else if (!attributeObj.isAnonymous()) {
            attribute.setAttribute("name", attributeObj.getName());
        }

        /*
         * TODO: should this be caught by refusing to allow both to be true at the same time?
         */
        if (attributeObj.getSchemaTypeName() != null && !refPresent) {
            String typeName = resolveQName(attributeObj.getSchemaTypeName(), schema);
            attribute.setAttribute("type", typeName);
        }

        if (attributeObj.getDefaultValue() != null) {
            attribute.setAttribute("default", attributeObj.getDefaultValue());
        }
        if (attributeObj.getFixedValue() != null) {
            attribute.setAttribute("fixed", attributeObj.getFixedValue());
        }

        /*
         * TODO: should this be caught by refusing to allow both to be true at the same time?
         */
        if (attributeObj.isFormSpecified() && !refPresent) {
            attribute.setAttribute("form", attributeObj.getForm().toString());
        }

        if (attributeObj.getId() != null) {
            attribute.setAttribute("id", attributeObj.getId());
        }

        if (attributeObj.getUse() != null && attributeObj.getUse() != XmlSchemaUse.NONE) {
            attribute.setAttribute("use", attributeObj.getUse().toString());
        }

        if (attributeObj.getAnnotation() != null) {
            Element annotation = serializeAnnotation(doc, attributeObj.getAnnotation(), schema);
            attribute.appendChild(annotation);
        }

        /*
         * TODO: should this be caught by refusing to allow both to be true at the same time?
         */
        if (attributeObj.getSchemaType() != null && !refPresent) {
            try {
                XmlSchemaSimpleType simpleType = attributeObj.getSchemaType();
                Element simpleTypeEl = serializeSimpleType(doc, simpleType, schema);
                attribute.appendChild(simpleTypeEl);
            } catch (ClassCastException e) {
                throw new XmlSchemaSerializerException(
                    "Only an inline simple type is allowed as an attribute's inline type");
            }
        }

        Attr[] unhandled = attributeObj.getUnhandledAttributes();

        Map<String, String> namespaces = Collections.synchronizedMap(new HashMap<String, String>());

        if (unhandled != null) {

            // this is to make the wsdl:arrayType work
            // since unhandles attributes are not handled this is a special case
            // but the basic idea is to see if there is any attibute whose value has ":"
            // if it is present then it is likely that it is a namespace prefix
            // do what is neccesary to get the real namespace for it and make
            // required changes to the prefix

            for (Attr element : unhandled) {
                String name = element.getNodeName();
                String value = element.getNodeValue();
                if ("xmlns".equals(name)) {
                    namespaces.put("", value);
                } else if (name.startsWith("xmlns")) {
                    namespaces.put(name.substring(name.indexOf(":") + 1), value);
                }
            }

            for (Attr element : unhandled) {
                String value = element.getNodeValue();
                String nodeName = element.getNodeName();
                if (value.indexOf(":") > -1 && !nodeName.startsWith("xmlns")) {
                    String prefix = value.substring(0, value.indexOf(":"));
                    String oldNamespace;
                    oldNamespace = namespaces.get(prefix);
                    if (oldNamespace != null) {
                        value = value.substring(value.indexOf(":") + 1);
                        NamespacePrefixList ctx = schema.getNamespaceContext();
                        String[] prefixes = ctx.getDeclaredPrefixes();
                        for (String pref : prefixes) {
                            String uri = ctx.getNamespaceURI(pref);
                            if (uri.equals(oldNamespace)) {
                                value = prefix + ":" + value;
                            }
                        }
                    }

                }
                if (element.getNamespaceURI() != null) {
                    attribute.setAttributeNS(element.getNamespaceURI(), nodeName, value);
                } else {
                    attribute.setAttribute(nodeName, value);
                }
            }
        }

        // process extension
        processExtensibilityComponents(attributeObj, attribute);

        return attribute;
    }

    /**
     * ********************************************************************* Element
     * serializeAttributeGroup(Document doc, XmlSchemaAttributeGroup attributeGroupObj, XmlSchema schema)
     * throws XmlSchemaSerializerException{
     * <p/>
     * Each member of complex type will be appended and pass the element created. Complex type processed
     * according to w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. attributeGroupObj - XmlSchemaAttributeGroup that will be
     * serialized. schema - Schema Document object of the parent.
     * <p/>
     * Return: Element of attribute group.
     * **********************************************************************
     */
    Element serializeAttributeGroup(Document doc, XmlSchemaAttributeGroup attributeGroupObj, XmlSchema schema)
        throws XmlSchemaSerializerException {

        Element attributeGroup = createNewElement(doc, "attributeGroup", schema.getSchemaNamespacePrefix(),
                                                  XmlSchema.SCHEMA_NS);

        if (!attributeGroupObj.isAnonymous()) {
            String attGroupName = attributeGroupObj.getName();
            attributeGroup.setAttribute("name", attGroupName);
        } else {
            throw new XmlSchemaSerializerException("Attribute group must" + "have name");
        }
        if (attributeGroupObj.getId() != null) {
            attributeGroup.setAttribute("id", attributeGroupObj.getId());
        }

        if (attributeGroupObj.getAnnotation() != null) {
            Element annotation = serializeAnnotation(doc, attributeGroupObj.getAnnotation(), schema);
            attributeGroup.appendChild(annotation);
        }
        int attributesLength = attributeGroupObj.getAttributes().size();
        for (int i = 0; i < attributesLength; i++) {
            XmlSchemaAttributeGroupMember obj = attributeGroupObj.getAttributes().get(i);

            if (obj instanceof XmlSchemaAttribute) {
                Element attr = serializeAttribute(doc, (XmlSchemaAttribute)obj, schema);
                attributeGroup.appendChild(attr);
            } else if (obj instanceof XmlSchemaAttributeGroupRef) {
                Element attrGroup = serializeAttributeGroupRef(doc, (XmlSchemaAttributeGroupRef)obj, schema);
                attributeGroup.appendChild(attrGroup);
            }
        }

        if (attributeGroupObj.getAnyAttribute() != null) {
            Element anyAttribute = serializeAnyAttribute(doc, attributeGroupObj.getAnyAttribute(), schema);
            attributeGroup.appendChild(anyAttribute);
        }

        // process extension
        processExtensibilityComponents(attributeGroupObj, attributeGroup);

        return attributeGroup;
    }

    /**
     * ********************************************************************* Element
     * serializeAttributeGroupRef(Document doc, XmlSchemaAttributeGroupRef attributeGroupObj, XmlSchema
     * schema) throws XmlSchemaSerializerException
     * <p/>
     * Each member of complex type will be appended and pass the element created. Complex type processed
     * according to w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. attributeGroupObj - XmlSchemaAttributeGroupRef that will be
     * serialized. schema - Schema Document object of the parent.
     * <p/>
     * Return: Element of attribute group ref that part of type.
     * **********************************************************************
     */
    Element serializeAttributeGroupRef(Document doc, XmlSchemaAttributeGroupRef attributeGroupObj,
                                       XmlSchema schema) throws XmlSchemaSerializerException {

        Element attributeGroupRef = createNewElement(doc, "attributeGroup", schema.getSchemaNamespacePrefix(),
                                                     XmlSchema.SCHEMA_NS);
        if (attributeGroupObj.getRef().getTarget() != null) {
            String refName = resolveQName(attributeGroupObj.getRef().getTargetQName(), schema);
            attributeGroupRef.setAttribute("ref", refName);
        } else {
            throw new XmlSchemaSerializerException("Attribute group must have " + "ref name set");
        }

        if (attributeGroupObj.getId() != null) {
            attributeGroupRef.setAttribute("id", attributeGroupObj.getId());
        }

        if (attributeGroupObj.getAnnotation() != null) {
            Element annotation = serializeAnnotation(doc, attributeGroupObj.getAnnotation(), schema);
            attributeGroupRef.appendChild(annotation);
        }

        // process extension
        processExtensibilityComponents(attributeGroupObj, attributeGroupRef);

        return attributeGroupRef;
    }

    /**
     * ********************************************************************* Element serializeChoice(Document
     * doc, XmlSchemaChoice choiceObj, XmlSchema schema) throws XmlSchemaSerializerException{
     * <p/>
     * Each member of complex type will be appended and pass the element created. Complex type processed
     * according to w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. choiceObj - XmlSchemaChoice that will be serialized. schema -
     * Schema Document object of the parent.
     * <p/>
     * Return: Element of choice schema object.
     * **********************************************************************
     */
    Element serializeChoice(Document doc, XmlSchemaChoice choiceObj, XmlSchema schema)
        throws XmlSchemaSerializerException {
        // todo: handle any non schema attri ?

        Element choice = createNewElement(doc, "choice", schema.getSchemaNamespacePrefix(),
                                          XmlSchema.SCHEMA_NS);
        if (choiceObj.getId() != null && choiceObj.getId().length() > 0) {
            choice.setAttribute("id", choiceObj.getId());
        }

        serializeMaxMinOccurs(choiceObj, choice);

        /*
         * if(choiceObj.maxOccursString != null) choice.setAttribute("maxOccurs", choiceObj.maxOccursString);
         * else if(choiceObj.maxOccurs > 1) choice.setAttribute("maxOccurs", choiceObj.maxOccurs +"");
         */

        if (choiceObj.getAnnotation() != null) {
            Element annotation = serializeAnnotation(doc, choiceObj.getAnnotation(), schema);
            choice.appendChild(annotation);
        }

        List<XmlSchemaObject> itemColl = choiceObj.getItems();

        if (itemColl != null) {
            int itemLength = itemColl.size();

            for (int i = 0; i < itemLength; i++) {
                XmlSchemaObject obj = itemColl.get(i);

                if (obj instanceof XmlSchemaElement) {
                    Element el = serializeElement(doc, (XmlSchemaElement)obj, schema);
                    choice.appendChild(el);
                } else if (obj instanceof XmlSchemaGroupRef) {
                    Element group = serializeGroupRef(doc, (XmlSchemaGroupRef)obj, schema);
                    choice.appendChild(group);
                } else if (obj instanceof XmlSchemaChoice) {
                    Element inlineChoice = serializeChoice(doc, (XmlSchemaChoice)obj, schema);
                    choice.appendChild(inlineChoice);
                } else if (obj instanceof XmlSchemaSequence) {
                    Element inlineSequence = serializeSequence(doc, (XmlSchemaSequence)obj, schema);
                    choice.appendChild(inlineSequence);
                } else if (obj instanceof XmlSchemaAny) {
                    Element any = serializeAny(doc, (XmlSchemaAny)obj, schema);
                    choice.appendChild(any);
                }
            }
        }

        // process extension
        processExtensibilityComponents(choiceObj, choice);

        return choice;
    }

    /**
     * ********************************************************************* Element
     * serializeComplexContent(Document doc, XmlSchemaComplexContent complexContentObj, XmlSchema schema)
     * throws XmlSchemaSerializerException{
     * <p/>
     * Each member of complex type will be appended and pass the element created. Complex type processed
     * according to w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. complexContentObj - XmlSchemaComplexContent that will be
     * serialized. schema - Schema Document object of the parent.
     * <p/>
     * Return: Element of complex type complex content.
     * **********************************************************************
     */
    Element serializeComplexContent(Document doc, XmlSchemaComplexContent complexContentObj, XmlSchema schema)
        throws XmlSchemaSerializerException {

        Element complexContent = createNewElement(doc, "complexContent", schema.getSchemaNamespacePrefix(),
                                                  XmlSchema.SCHEMA_NS);

        if (complexContentObj.getAnnotation() != null) {
            Element annotation = serializeAnnotation(doc, complexContentObj.getAnnotation(), schema);
            complexContent.appendChild(annotation);
        }

        if (complexContentObj.isMixed()) {
            complexContent.setAttribute("mixed", "true");
        }
        if (complexContentObj.getId() != null) {
            complexContent.setAttribute("id", complexContentObj.getId());
        }

        Element content;
        if (complexContentObj.content instanceof XmlSchemaComplexContentRestriction) {
            content = serializeComplexContentRestriction(
                                                         doc,
                                                         (XmlSchemaComplexContentRestriction)
                                                         complexContentObj.content,
                                                         schema);
        } else if (complexContentObj.content instanceof XmlSchemaComplexContentExtension) {
            content = serializeComplexContentExtension(
                                                       doc,
                                                       (XmlSchemaComplexContentExtension)
                                                       complexContentObj.content,
                                                       schema);
        } else {
            throw new XmlSchemaSerializerException("content of complexContent "
                                                   + "must be restriction or extension");
        }

        complexContent.appendChild(content);

        // process extension
        processExtensibilityComponents(complexContentObj, complexContent);

        return complexContent;
    }

    /**
     * ********************************************************************* Element
     * serializeComplexContentExtension(Document doc, XmlSchemaComplexContentExtension extensionObj, XmlSchema
     * schema) throws XmlSchemaSerializerException{
     * <p/>
     * Each member of complex type will be appended and pass the element created. Complex type processed
     * according to w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. extensionObj - XmlSchemaComplexContentRestriction that will
     * be serialized. schema - Schema Document object of the parent.
     * <p/>
     * Return: Element of complex content extension.
     * **********************************************************************
     */
    Element serializeComplexContentExtension(Document doc, XmlSchemaComplexContentExtension extensionObj,
                                             XmlSchema schema) throws XmlSchemaSerializerException {

        Element extension = createNewElement(doc, "extension", schema.getSchemaNamespacePrefix(),
                                             XmlSchema.SCHEMA_NS);
        if (extensionObj.getBaseTypeName() != null) {
            String baseType = resolveQName(extensionObj.getBaseTypeName(), schema);
            extension.setAttribute("base", baseType);
        }
        if (extensionObj.getAnnotation() != null) {
            Element annotation = serializeAnnotation(doc, extensionObj.getAnnotation(), schema);
            extension.appendChild(annotation);
        }

        if (extensionObj.getParticle() instanceof XmlSchemaSequence) {
            Element sequenceParticle = serializeSequence(doc, (XmlSchemaSequence)extensionObj.getParticle(),
                                                         schema);
            extension.appendChild(sequenceParticle);
        } else if (extensionObj.getParticle() instanceof XmlSchemaChoice) {
            Element choiceParticle = serializeChoice(doc,
                                                     (XmlSchemaChoice)extensionObj.getParticle(), schema);
            extension.appendChild(choiceParticle);
        } else if (extensionObj.getParticle() instanceof XmlSchemaAll) {
            Element allParticle = serializeAll(doc, (XmlSchemaAll)extensionObj.getParticle(), schema);
            extension.appendChild(allParticle);
        } else if (extensionObj.getParticle() instanceof XmlSchemaGroupRef) {
            Element groupRefParticle = serializeGroupRef(doc, (XmlSchemaGroupRef)extensionObj.getParticle(),
                                                         schema);
            extension.appendChild(groupRefParticle);
        }

        int attributesLength = extensionObj.getAttributes().size();
        for (int i = 0; i < attributesLength; i++) {
            XmlSchemaObject obj = extensionObj.getAttributes().get(i);

            if (obj instanceof XmlSchemaAttribute) {
                Element attr = serializeAttribute(doc, (XmlSchemaAttribute)obj, schema);
                extension.appendChild(attr);
            } else if (obj instanceof XmlSchemaAttributeGroupRef) {
                Element attrGroup = serializeAttributeGroupRef(doc, (XmlSchemaAttributeGroupRef)obj, schema);
                extension.appendChild(attrGroup);
            }
        }

        if (extensionObj.getAnyAttribute() != null) {
            Element anyAttribute = serializeAnyAttribute(doc, extensionObj.getAnyAttribute(), schema);
            extension.appendChild(anyAttribute);
        }

        // process extension
        processExtensibilityComponents(extensionObj, extension);

        return extension;
    }

    /**
     * ********************************************************************* Element
     * serializeComplexContentRestriction(Document doc, XmlSchemaComplexContentRestriction restrictionObj,
     * XmlSchema schema) throws XmlSchemaSerializerException{
     * <p/>
     * Each member of complex type will be appended and pass the element created. Complex type processed
     * according to w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. restrictionObj - XmlSchemaSimpleContentRestriction that will
     * be serialized. schema - Schema Document object of the parent.
     * <p/>
     * Return: Element of simple content restriction.
     * **********************************************************************
     */
    Element serializeComplexContentRestriction(Document doc,
                                               XmlSchemaComplexContentRestriction restrictionObj,
                                               XmlSchema schema) throws XmlSchemaSerializerException {

        Element restriction = createNewElement(doc, "restriction", schema.getSchemaNamespacePrefix(),
                                               XmlSchema.SCHEMA_NS);

        if (restrictionObj.getBaseTypeName() != null) {
            String baseTypeName = resolveQName(restrictionObj.getBaseTypeName(), schema);
            restriction.setAttribute("base", baseTypeName);
        }

        if (restrictionObj.getId() != null) {
            restriction.setAttribute("id", restrictionObj.getId());
        }

        if (restrictionObj.getAnnotation() != null) {
            Element annotation = serializeAnnotation(doc, restrictionObj.getAnnotation(), schema);
            restriction.appendChild(annotation);
        }

        if (restrictionObj.getParticle() instanceof XmlSchemaSequence) {
            Element sequenceParticle = serializeSequence(doc, (XmlSchemaSequence)restrictionObj.getParticle(),
                                                         schema);
            restriction.appendChild(sequenceParticle);
        } else if (restrictionObj.getParticle() instanceof XmlSchemaChoice) {
            Element choiceParticle = serializeChoice(doc,
                                                     (XmlSchemaChoice)restrictionObj.getParticle(), schema);
            restriction.appendChild(choiceParticle);
        } else if (restrictionObj.getParticle() instanceof XmlSchemaAll) {
            Element allParticle = serializeAll(doc, (XmlSchemaAll)restrictionObj.getParticle(), schema);
            restriction.appendChild(allParticle);
        } else if (restrictionObj.getParticle() instanceof XmlSchemaGroupRef) {
            Element groupRefParticle = serializeGroupRef(doc, (XmlSchemaGroupRef)restrictionObj.getParticle(),
                                                         schema);
            restriction.appendChild(groupRefParticle);
        }

        int attributesLength = restrictionObj.getAttributes().size();
        for (int i = 0; i < attributesLength; i++) {
            XmlSchemaAttributeOrGroupRef obj = restrictionObj.getAttributes().get(i);

            if (obj instanceof XmlSchemaAttribute) {
                Element attr = serializeAttribute(doc, (XmlSchemaAttribute)obj, schema);
                restriction.appendChild(attr);
            } else if (obj instanceof XmlSchemaAttributeGroupRef) {
                Element attrGroup = serializeAttributeGroupRef(doc, (XmlSchemaAttributeGroupRef)obj, schema);
                restriction.appendChild(attrGroup);
            }
        }

        if (restrictionObj.getAnyAttribute() != null) {
            Element anyAttribute = serializeAnyAttribute(doc, restrictionObj.getAnyAttribute(), schema);
            restriction.appendChild(anyAttribute);
        }

        // process extension
        processExtensibilityComponents(restrictionObj, restriction);

        return restriction;
    }

    /**
     * ********************************************************************* Element
     * serializeComplexType(Document doc, XmlSchemaComplexType complexTypeObj, XmlSchema schema) throws
     * XmlSchemaSerializerException{
     * <p/>
     * Each member of complex type will be appended and pass the element created. Complex type processed
     * according to w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. complexTypeObj - XmlSchemaFacet that will be serialized.
     * schema - Schema Document object of the parent.
     * <p/>
     * Return: Element of complexType. **********************************************************************
     */
    Element serializeComplexType(Document doc, XmlSchemaComplexType complexTypeObj, XmlSchema schema)
        throws XmlSchemaSerializerException {

        Element serializedComplexType = createNewElement(doc, "complexType",
                                                         schema.getSchemaNamespacePrefix(),
                                                         XmlSchema.SCHEMA_NS);

        if (!complexTypeObj.isAnonymous()) {
            serializedComplexType.setAttribute("name", complexTypeObj.getName());
            /*
             * if(complexTypeObj.annotation != null){ Element annotationEl = serializeAnnotation(doc,
             * complexTypeObj.annotation, schema); serializedComplexType.appendChild(annotationEl); }
             */
        }

        if (complexTypeObj.isMixed()) {
            serializedComplexType.setAttribute("mixed", "true");
        }
        if (complexTypeObj.isAbstract()) {
            serializedComplexType.setAttribute("abstract", "true");
        }
        if (complexTypeObj.getId() != null) {
            serializedComplexType.setAttribute("id", complexTypeObj.getId());
        }

        if (complexTypeObj.getAnnotation() != null) {
            Element annotationEl = serializeAnnotation(doc, complexTypeObj.getAnnotation(), schema);
            serializedComplexType.appendChild(annotationEl);
        }

        if (complexTypeObj.getContentModel() instanceof XmlSchemaSimpleContent) {
            Element simpleContent = serializeSimpleContent(
                                                           doc,
                                                           (XmlSchemaSimpleContent)
                                                           complexTypeObj.getContentModel(),
                                                           schema);
            serializedComplexType.appendChild(simpleContent);
        } else if (complexTypeObj.getContentModel() instanceof XmlSchemaComplexContent) {

            Element complexContent = serializeComplexContent(
                                                             doc,
                                                             (XmlSchemaComplexContent)
                                                             complexTypeObj.getContentModel(),
                                                             schema);
            serializedComplexType.appendChild(complexContent);
        }

        if (complexTypeObj.getParticle() instanceof XmlSchemaSequence) {
            Element sequence = serializeSequence(doc,
                                                 (XmlSchemaSequence)complexTypeObj.getParticle(), schema);
            serializedComplexType.appendChild(sequence);
        } else if (complexTypeObj.getParticle() instanceof XmlSchemaChoice) {
            Element choice = serializeChoice(doc, (XmlSchemaChoice)complexTypeObj.getParticle(), schema);
            serializedComplexType.appendChild(choice);
        } else if (complexTypeObj.getParticle() instanceof XmlSchemaAll) {
            Element all = serializeAll(doc, (XmlSchemaAll)complexTypeObj.getParticle(), schema);
            serializedComplexType.appendChild(all);
        } else if (complexTypeObj.getParticle() instanceof XmlSchemaGroupRef) {
            Element group = serializeGroupRef(doc, (XmlSchemaGroupRef)complexTypeObj.getParticle(), schema);
            serializedComplexType.appendChild(group);
        }

        if (complexTypeObj.getBlock() != null
            && complexTypeObj.getBlock() != XmlSchemaDerivationMethod.NONE) {
            serializedComplexType.setAttribute("block", complexTypeObj.toString());
        }

        if (complexTypeObj.getFinalDerivation() != null
            && complexTypeObj.getFinalDerivation() != XmlSchemaDerivationMethod.NONE) {
            serializedComplexType.setAttribute("final", complexTypeObj.getFinalDerivation().toString());
        }

        List<XmlSchemaAttributeOrGroupRef> attrColl = complexTypeObj.getAttributes();
        if (attrColl.size() > 0) {
            setupAttr(doc, attrColl, schema, serializedComplexType);
        }

        XmlSchemaAnyAttribute anyAttribute = complexTypeObj.getAnyAttribute();
        if (anyAttribute != null) {
            serializedComplexType.appendChild(serializeAnyAttribute(doc, anyAttribute, schema));
        }

        // process extension
        processExtensibilityComponents(complexTypeObj, serializedComplexType);

        return serializedComplexType;
    }

    /**
     * ********************************************************************* Element
     * serializeDocumentation(Document doc,XmlSchemaDocumentation documentationObj, XmlSchema schema){
     * <p/>
     * <p/>
     * Each member of complex type will be appended and pass the element created. Complex type processed
     * according to w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. documentationObj - XmlSchemaAppInfo that will be serialized.
     * schema - Schema Document object of the parent.
     * <p/>
     * Return: Element representation of documentation that is part of annotation.
     * **********************************************************************
     */
    Element serializeDocumentation(Document doc, XmlSchemaDocumentation documentationObj, XmlSchema schema) {

        Element documentationEl = createNewElement(doc, "documentation", schema.getSchemaNamespacePrefix(),
                                                   XmlSchema.SCHEMA_NS);
        if (documentationObj.source != null) {
            documentationEl.setAttribute("source", documentationObj.source);
        }
        if (documentationObj.language != null) {
            documentationEl.setAttributeNS("http://www.w3.org/XML/1998/namespace", "xml:lang",
                                           documentationObj.language);
        }

        if (documentationObj.markup != null) {
            int markupLength = documentationObj.markup.getLength();
            for (int j = 0; j < markupLength; j++) {
                Node n = (Node)documentationObj.markup.item(j);

                switch (n.getNodeType()) {
                case Node.ELEMENT_NODE:
                    appendElement(doc, documentationEl, n, schema);
                    break;
                case Node.TEXT_NODE:
                    Text t = doc.createTextNode(n.getNodeValue());
                    documentationEl.appendChild(t);
                    break;
                case Node.CDATA_SECTION_NODE:
                    CDATASection s = doc.createCDATASection(n.getNodeValue());
                    documentationEl.appendChild(s);
                    break;
                case Node.COMMENT_NODE:
                    Comment c = doc.createComment(n.getNodeValue());
                    documentationEl.appendChild(c);
                    break;
                default:
                    break;
                }
            }
        }
        // process extension
        processExtensibilityComponents(documentationObj, documentationEl);

        return documentationEl;
    }

    /**
     * ********************************************************************* Element serializeElement(Document
     * doc, XmlSchemaElement elementObj, XmlSchema schema) throws XmlSchemaSerializerException
     * <p/>
     * Each member of Element will be appended and pass the element created. Element processed according to
     * w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. elementObj - XmlSchemaInclude that will be serialized. schema
     * - Schema Document object of the parent.
     * <p/>
     * Return: Element object of element.
     * **********************************************************************
     */
    Element serializeElement(Document doc, XmlSchemaElement elementObj, XmlSchema schema)
        throws XmlSchemaSerializerException {
        Element serializedEl = createNewElement(doc, "element", schema.getSchemaNamespacePrefix(),
                                                XmlSchema.SCHEMA_NS);

        if (elementObj.getRef().getTargetQName() != null) {

            String resolvedName = resolveQName(elementObj.getRef().getTargetQName(), schema);
            serializedEl.setAttribute("ref", resolvedName);
        } else if (!elementObj.isAnonymous()) {
            serializedEl.setAttribute("name", elementObj.getName());
        }

        if (elementObj.isAbstractElement()) {
            serializedEl.setAttribute("abstract", "true");
        }

        if (elementObj.getBlock() != null && elementObj.getBlock() != XmlSchemaDerivationMethod.NONE) {
            serializedEl.setAttribute("block", elementObj.getBlock().toString());
        }
        if (elementObj.getDefaultValue() != null) {
            serializedEl.setAttribute("default", elementObj.getDefaultValue());
        }

        if (elementObj.getFinalDerivation() != null
            && elementObj.getFinalDerivation() != XmlSchemaDerivationMethod.NONE) {
            serializedEl.setAttribute("final", elementObj.getFinalDerivation().toString());
        }
        if (elementObj.getFixedValue() != null) {
            serializedEl.setAttribute("fixed", elementObj.getFixedValue());
        }

        if (elementObj.isFormSpecified()) {
            serializedEl.setAttribute("form", elementObj.getForm().toString());
        }

        if (elementObj.getId() != null) {
            serializedEl.setAttribute("id", elementObj.getId());
        }

        serializeMaxMinOccurs(elementObj, serializedEl);

        if (elementObj.getSubstitutionGroup() != null) {
            String resolvedQName = resolveQName(elementObj.getSubstitutionGroup(), schema);
            serializedEl.setAttribute("substitutionGroup", resolvedQName);
        }
        if (elementObj.getSchemaTypeName() != null) {
            String resolvedName = resolveQName(elementObj.getSchemaTypeName(), schema);
            serializedEl.setAttribute("type", resolvedName);
        }
        if (elementObj.getAnnotation() != null) {
            Element annotationEl = serializeAnnotation(doc, elementObj.getAnnotation(), schema);
            serializedEl.appendChild(annotationEl);
        }
        if (elementObj.getSchemaType() != null && elementObj.getSchemaTypeName() == null) {
            if (elementObj.getSchemaType() instanceof XmlSchemaComplexType) {

                Element complexType =
                    serializeComplexType(doc,
                                         (XmlSchemaComplexType)elementObj.getSchemaType(),
                                         schema);
                serializedEl.appendChild(complexType);
            } else if (elementObj.getSchemaType() instanceof XmlSchemaSimpleType) {
                Element simpleType = serializeSimpleType(doc, (XmlSchemaSimpleType)elementObj.getSchemaType(),
                                                         schema);
                serializedEl.appendChild(simpleType);
            }
        }
        if (elementObj.getConstraints().size() > 0) {
            for (int i = 0; i < elementObj.getConstraints().size(); i++) {
                Element constraint = serializeIdentityConstraint(
                                                                 doc,
                                                                 (XmlSchemaIdentityConstraint)
                                                                 elementObj.getConstraints()
                                                                     .get(i), schema);
                serializedEl.appendChild(constraint);
            }
        }
        if (elementObj.isNillable()) {
            serializedEl.setAttribute("nillable", "true");
        }

        // process extension
        processExtensibilityComponents(elementObj, serializedEl);

        return serializedEl;
    }

    /**
     * ********************************************************************* Element serializeFacet(Document
     * doc, XmlSchemaFacet facetObj, XmlSchema schema) throws XmlSchemaSerializerException{
     * <p/>
     * detect what type of facet and cass appropriatelly, construct the element and pass it.
     * <p/>
     * Parameter: doc - Document the parent use. facetObj - XmlSchemaFacet that will be serialized. schema -
     * Schema Document object of the parent.
     * <p/>
     * Return: Element of simple type with facet.
     * **********************************************************************
     */
    Element serializeFacet(Document doc, XmlSchemaFacet facetObj, XmlSchema schema)
        throws XmlSchemaSerializerException {

        Element serializedFacet;

        if (facetObj instanceof XmlSchemaMinExclusiveFacet) {
            serializedFacet = constructFacet(facetObj, doc, schema, "minExclusive");
        } else if (facetObj instanceof XmlSchemaMinInclusiveFacet) {
            serializedFacet = constructFacet(facetObj, doc, schema, "minInclusive");
        } else if (facetObj instanceof XmlSchemaMaxExclusiveFacet) {
            serializedFacet = constructFacet(facetObj, doc, schema, "maxExclusive");
        } else if (facetObj instanceof XmlSchemaMaxInclusiveFacet) {
            serializedFacet = constructFacet(facetObj, doc, schema, "maxInclusive");
        } else if (facetObj instanceof XmlSchemaTotalDigitsFacet) {
            serializedFacet = constructFacet(facetObj, doc, schema, "totalDigits");
        } else if (facetObj instanceof XmlSchemaFractionDigitsFacet) {
            serializedFacet = constructFacet(facetObj, doc, schema, "fractionDigits");
        } else if (facetObj instanceof XmlSchemaLengthFacet) {
            serializedFacet = constructFacet(facetObj, doc, schema, "length");
        } else if (facetObj instanceof XmlSchemaMinLengthFacet) {
            serializedFacet = constructFacet(facetObj, doc, schema, "minLength");
        } else if (facetObj instanceof XmlSchemaMaxLengthFacet) {
            serializedFacet = constructFacet(facetObj, doc, schema, "maxLength");
        } else if (facetObj instanceof XmlSchemaEnumerationFacet) {
            serializedFacet = constructFacet(facetObj, doc, schema, "enumeration");
        } else if (facetObj instanceof XmlSchemaWhiteSpaceFacet) {
            serializedFacet = constructFacet(facetObj, doc, schema, "whiteSpace");
        } else if (facetObj instanceof XmlSchemaPatternFacet) {
            serializedFacet = constructFacet(facetObj, doc, schema, "pattern");
        } else {
            throw new XmlSchemaSerializerException("facet not exist " + facetObj.getClass().getName());
        }

        if (facetObj.getId() != null) {
            serializedFacet.setAttribute("id", facetObj.getId());
            // if (facetObj.annotation != null) {
            // Element annotation = serializeAnnotation(doc, facetObj.annotation,
            // schema);
            // serializedFacet.appendChild(annotation);
            // }
        }

        // process extension
        processExtensibilityComponents(facetObj, serializedFacet);

        return serializedFacet;
    }

    /**
     * ********************************************************************* Element serializeField(Document
     * doc, XmlSchemaXPath fieldObj, XmlSchema schema) throws XmlSchemaSerializerException
     * <p/>
     * Each member of complex type will be appended and pass the element created. Complex type processed
     * according to w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. fieldObj - XmlSchemaXPath that will be serialized. schema -
     * Schema Document object of the parent.
     * <p/>
     * Return: field element that part of constraint.
     * **********************************************************************
     */
    Element serializeField(Document doc, XmlSchemaXPath fieldObj, XmlSchema schema)
        throws XmlSchemaSerializerException {

        Element field = createNewElement(doc, "field",
                                         schema.getSchemaNamespacePrefix(),
                                         XmlSchema.SCHEMA_NS);

        if (fieldObj.xpath != null) {
            field.setAttribute("xpath", fieldObj.xpath);
        } else {
            throw new XmlSchemaSerializerException("xpath can't be null");
        }

        if (fieldObj.getAnnotation() != null) {
            Element annotation = serializeAnnotation(doc, fieldObj.getAnnotation(), schema);
            field.appendChild(annotation);
        }

        // process extension
        processExtensibilityComponents(fieldObj, field);

        return field;
    }

    /**
     * ********************************************************************* Element serializeGroup(Document
     * doc, XmlSchemaGroup groupObj, XmlSchema schema) throws XmlSchemaSerializerException{
     * <p/>
     * Each member of complex type will be appended and pass the element created. Complex type processed
     * according to w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. groupObj - XmlSchemaGroup that will be serialized. schema -
     * Schema Document object of the parent.
     * <p/>
     * Return: Element of group elements.
     * **********************************************************************
     */
    Element serializeGroup(Document doc, XmlSchemaGroup groupObj, XmlSchema schema)
        throws XmlSchemaSerializerException {

        Element group = createNewElement(doc, "group", schema.getSchemaNamespacePrefix(),
                                         XmlSchema.SCHEMA_NS);

        if (!groupObj.isAnonymous()) {
            String grpName = groupObj.getName();
            if (grpName.length() > 0) {
                group.setAttribute("name", grpName);
            }
        } else {
            throw new XmlSchemaSerializerException("Group must have " + "name or ref");
        }

        /* annotations are supposed to be written first!!!!! */
        if (groupObj.getAnnotation() != null) {
            Element annotation = serializeAnnotation(doc, groupObj.getAnnotation(), schema);
            group.appendChild(annotation);
        }

        if (groupObj.getParticle() instanceof XmlSchemaSequence) {
            Element sequence = serializeSequence(doc, (XmlSchemaSequence)groupObj.getParticle(), schema);
            group.appendChild(sequence);
        } else if (groupObj.getParticle() instanceof XmlSchemaChoice) {
            Element choice = serializeChoice(doc, (XmlSchemaChoice)groupObj.getParticle(), schema);
            group.appendChild(choice);
        } else if (groupObj.getParticle() instanceof XmlSchemaAll) {
            Element all = serializeAll(doc, (XmlSchemaAll)groupObj.getParticle(), schema);
            group.appendChild(all);
        }

        // process extension
        processExtensibilityComponents(groupObj, group);

        return group;
    }

    /**
     * ********************************************************************* Element
     * serializeGroupRef(Document doc, XmlSchemaGroupRef groupRefObj, XmlSchema schema) throws
     * XmlSchemaSerializerException{
     * <p/>
     * Each member of complex type will be appended and pass the element created. Complex type processed
     * according to w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. groupRefObj - XmlSchemaGroupRef that will be serialized.
     * schema - Schema Document object of the parent.
     * <p/>
     * Return: Element of group elements ref inside its parent.
     * **********************************************************************
     */
    Element serializeGroupRef(Document doc, XmlSchemaGroupRef groupRefObj, XmlSchema schema)
        throws XmlSchemaSerializerException {

        Element groupRef = createNewElement(doc, "group", schema.getSchemaNamespacePrefix(),
                                            XmlSchema.SCHEMA_NS);

        if (groupRefObj.getRefName() != null) {
            String groupRefName = resolveQName(groupRefObj.getRefName(), schema);
            groupRef.setAttribute("ref", groupRefName);
        } else {
            throw new XmlSchemaSerializerException("Group must have name or ref");
        }

        serializeMaxMinOccurs(groupRefObj, groupRef);

        if (groupRefObj.getParticle() != null) {
            if (groupRefObj.getParticle() instanceof XmlSchemaChoice) {
                serializeChoice(doc, (XmlSchemaChoice)groupRefObj.getParticle(), schema);
            } else if (groupRefObj.getParticle() instanceof XmlSchemaSequence) {
                serializeSequence(doc, (XmlSchemaSequence)groupRefObj.getParticle(), schema);
            } else if (groupRefObj.getParticle() instanceof XmlSchemaAll) {
                serializeAll(doc, (XmlSchemaAll)groupRefObj.getParticle(), schema);
            } else {
                throw new XmlSchemaSerializerException("The content of group " + "ref particle should be"
                                                       + " sequence, choice or all reference:  "
                                                       + "www.w3.org/TR/xmlschema-1#element-group-3.7.2");
            }
        }
        if (groupRefObj.getAnnotation() != null) {
            Element annotation = serializeAnnotation(doc, groupRefObj.getAnnotation(), schema);
            groupRef.appendChild(annotation);
        }

        // process extension
        processExtensibilityComponents(groupRefObj, groupRef);

        return groupRef;
    }

    /**
     * ********************************************************************* Element
     * serializeIdentityConstraint(Document doc, XmlSchemaIdentityConstraint constraintObj, XmlSchema schema)
     * throws XmlSchemaSerializerException{
     * <p/>
     * Each member of complex type will be appended and pass the element created. Complex type processed
     * according to w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. constraintObj - XmlSchemaIdentityConstraint that will be
     * serialized. schema - Schema Document object of the parent.
     * <p/>
     * Return: Element of key, keyref or unique that part of its parent.
     * **********************************************************************
     */
    Element serializeIdentityConstraint(Document doc, XmlSchemaIdentityConstraint constraintObj,
                                        XmlSchema schema) throws XmlSchemaSerializerException {

        Element constraint;

        if (constraintObj instanceof XmlSchemaUnique) {
            constraint = createNewElement(doc, "unique", schema.getSchemaNamespacePrefix(),
                                          XmlSchema.SCHEMA_NS);
        } else if (constraintObj instanceof XmlSchemaKey) {
            constraint = createNewElement(doc, "key", schema.getSchemaNamespacePrefix(),
                                          XmlSchema.SCHEMA_NS);
        } else if (constraintObj instanceof XmlSchemaKeyref) {
            constraint = createNewElement(doc, "keyref", schema.getSchemaNamespacePrefix(),
                                          XmlSchema.SCHEMA_NS);
            XmlSchemaKeyref keyref = (XmlSchemaKeyref)constraintObj;
            if (keyref.refer != null) {
                String keyrefStr = resolveQName(keyref.refer, schema);
                constraint.setAttribute("refer", keyrefStr);
            }
        } else {
            throw new XmlSchemaSerializerException("not valid identity " + "constraint");
        }

        if (constraintObj.getName() != null) {
            constraint.setAttribute("name", constraintObj.getName());
        }
        if (constraintObj.getAnnotation() != null) {
            Element annotation = serializeAnnotation(doc, constraintObj.getAnnotation(), schema);
            constraint.appendChild(annotation);
        }

        if (constraintObj.getSelector() != null) {
            Element selector = serializeSelector(doc, constraintObj.getSelector(), schema);
            constraint.appendChild(selector);
        }
        List<XmlSchemaXPath> fieldColl = constraintObj.getFields();
        if (fieldColl != null) {
            int fieldLength = fieldColl.size();
            for (int i = 0; i < fieldLength; i++) {
                Element field = serializeField(doc, fieldColl.get(i), schema);
                constraint.appendChild(field);
            }
        }

        // process extension
        processExtensibilityComponents(constraintObj, constraint);

        return constraint;
    }

    /**
     * ********************************************************************* Element serializeImport(Document
     * doc, XmlSchemaImport importObj, XmlSchema schema)throws XmlSchemaSerializerException
     * <p/>
     * Add each of the attribute of XmlSchemaImport obj into import Element Then serialize schema that is
     * included by this import. Include the serialized schema into document pool.
     * <p/>
     * Parameter: doc - Document the parent use. includeObj - XmlSchemaInclude that will be serialized. schema
     * - Schema Document object of the parent.
     * <p/>
     * Return: Element object representation of XmlSchemaImport
     * **********************************************************************
     */
    Element serializeImport(Document doc, XmlSchemaImport importObj, XmlSchema schema,
                            boolean serializeIncluded) throws XmlSchemaSerializerException {

        Element importEl = createNewElement(doc, "import",
                                            schema.getSchemaNamespacePrefix(), XmlSchema.SCHEMA_NS);

        if (importObj.namespace != null) {
            importEl.setAttribute("namespace", importObj.namespace);
        }

        if (importObj.schemaLocation != null && !importObj.schemaLocation.trim().equals("")) {
            importEl.setAttribute("schemaLocation", importObj.schemaLocation);
        }

        if (importObj.getId() != null) {
            importEl.setAttribute("id", importObj.getId());
        }

        if (importObj.getAnnotation() != null) {
            Element annotation = serializeAnnotation(doc, importObj.getAnnotation(), schema);

            importEl.appendChild(annotation);
        }

        if (importObj.schema != null && serializeIncluded) {

            XmlSchemaSerializer importSeri = new XmlSchemaSerializer();
            importSeri.serializeSchemaElement(importObj.schema, serializeIncluded);
            docs.addAll(importSeri.docs);
        }

        // process extension
        processExtensibilityComponents(importObj, importEl);

        return importEl;
    }

    /**
     * ********************************************************************* Element serializeInclude(Document
     * doc, XmlSchemaInclude includeObj, XmlSchema schema)throws XmlSchemaSerializerException
     * <p/>
     * set appropriate attribute as per this object attribute availability. Call included schema to append to
     * this schema document collection. Then add the document created into document pool.
     * <p/>
     * Parameter: doc - Document the parent use. includeObj - XmlSchemaInclude that will be serialized. schema
     * - Schema Document object of the parent.
     * <p/>
     * Return: Element object representation of XmlSchemaInclude
     * **********************************************************************
     */
    Element serializeInclude(Document doc, XmlSchemaInclude includeObj, XmlSchema schema,
                             boolean serializeIncluded) throws XmlSchemaSerializerException {

        Element includeEl = createNewElement(doc, "include", schema.getSchemaNamespacePrefix(),
                                             XmlSchema.SCHEMA_NS);

        if (includeObj.schemaLocation != null) {
            includeEl.setAttribute("schemaLocation", includeObj.schemaLocation);
        }

        if (includeObj.getId() != null) {
            includeEl.setAttribute("id", includeObj.getId());
        }

        if (includeObj.getAnnotation() != null) {
            Element annotation = serializeAnnotation(doc, includeObj.getAnnotation(), schema);
            includeEl.appendChild(annotation);
        }

        // Get the XmlSchema obj and append that to the content
        XmlSchema includedSchemaObj = includeObj.getSchema();
        if (includedSchemaObj != null && serializeIncluded) {
            XmlSchemaSerializer includeSeri = new XmlSchemaSerializer();
            includeSeri.serializeSchemaElement(includedSchemaObj, true);
            // XmlSchemaObjectCollection ii = includedSchemaObj.getItems();
            docs.addAll(includeSeri.docs);
        }

        // process includes
        processExtensibilityComponents(includeObj, includeEl);

        return includeEl;
    }

    /**
     * ********************************************************************* Element
     * serializeRedefine(Document doc, XmlSchemaRedefine redefineObj, XmlSchema schema)throws
     * XmlSchemaSerializerException
     * <p/>
     * Add each of the attribute of XmlSchemaImport obj into import Element Then serialize schema that is
     * included by this import. Include the serialized schema into document pool.
     * <p/>
     * Parameter: doc - Document the parent use. redefineObj - XmlSchemaInclude that will be serialized.
     * schema - Schema Document object of the parent.
     * <p/>
     * Return: Element object representation of XmlSchemaRedefine
     * **********************************************************************
     */
    Element serializeRedefine(Document doc, XmlSchemaRedefine redefineObj, XmlSchema schema)
        throws XmlSchemaSerializerException {

        Element redefine = createNewElement(doc, "redefine", schema.getSchemaNamespacePrefix(),
                                            XmlSchema.SCHEMA_NS);

        if (redefineObj.schemaLocation != null) {
            redefine.setAttribute("schemaLocation", redefineObj.schemaLocation);
        } else {
            throw new XmlSchemaSerializerException("redefine must have " + "schemaLocation fields fill");
        }

        if (redefineObj.getId() != null) {
            redefine.setAttribute("id", redefineObj.getId());
        }

        if (redefineObj.getAnnotation() != null) {
            Element annotation = serializeAnnotation(doc, redefineObj.getAnnotation(), schema);
            redefine.appendChild(annotation);
        }
        int itemsLength = redefineObj.getItems().size();
        for (int i = 0; i < itemsLength; i++) {
            XmlSchemaObject obj = redefineObj.getItems().get(i);
            if (obj instanceof XmlSchemaSimpleType) {
                Element simpleType = serializeSimpleType(doc, (XmlSchemaSimpleType)obj, schema);
                redefine.appendChild(simpleType);
            } else if (obj instanceof XmlSchemaComplexType) {
                Element complexType = serializeComplexType(doc, (XmlSchemaComplexType)obj, schema);
                redefine.appendChild(complexType);
            } else if (obj instanceof XmlSchemaGroupRef) {
                Element groupRef = serializeGroupRef(doc, (XmlSchemaGroupRef)obj, schema);
                redefine.appendChild(groupRef);
            } else if (obj instanceof XmlSchemaGroup) {
                Element group = serializeGroup(doc, (XmlSchemaGroup)obj, schema);
                redefine.appendChild(group);
            } else if (obj instanceof XmlSchemaAttributeGroup) {
                Element attributeGroup = serializeAttributeGroup(doc, (XmlSchemaAttributeGroup)obj, schema);
                redefine.appendChild(attributeGroup);
            } else if (obj instanceof XmlSchemaAttributeGroupRef) {
                Element attributeGroupRef = serializeAttributeGroupRef(doc, (XmlSchemaAttributeGroupRef)obj,
                                                                       schema);
                redefine.appendChild(attributeGroupRef);
            }
        }

        // process extension
        processExtensibilityComponents(redefineObj, redefine);

        return redefine;
    }

    Document[] serializeSchemaElement(XmlSchema schemaObj,
                                      boolean serializeIncluded) throws XmlSchemaSerializerException {

        List<XmlSchemaObject> items = schemaObj.getItems();
        Document serializedSchemaDocs;
        try {
            DocumentBuilderFactory docFac = DocumentBuilderFactory.newInstance();
            docFac.setNamespaceAware(true);
            DocumentBuilder builder = docFac.newDocumentBuilder();
            serializedSchemaDocs = builder.newDocument();
        } catch (ParserConfigurationException e) {
            throw new XmlSchemaException(e.getMessage());
        }

        Element serializedSchema;

        serializedSchema = setupNamespaces(serializedSchemaDocs, schemaObj);
        schemaElement = serializedSchema;

        if (schemaObj.getSyntacticalTargetNamespace() != null) {
            serializedSchema.setAttribute("targetNamespace", schemaObj.getSyntacticalTargetNamespace());

            String targetNS =
                    (String)schemaNamespace.get(schemaObj.getSyntacticalTargetNamespace());

            //if the namespace is not entered then add
            //the targetNamespace
            if (targetNS == null) {
                String prefix = null;
                if (schemaObj.getNamespaceContext() != null) {
                    prefix = schemaObj.
                        getNamespaceContext().getPrefix(schemaObj.getSyntacticalTargetNamespace());
                }
                if (prefix == null
                    && schemaObj.getParent() != null
                    && schemaObj.getParent().getNamespaceContext() != null) {
                    prefix = schemaObj.getParent().
                        getNamespaceContext().getPrefix(schemaObj.getSyntacticalTargetNamespace());
                }
                //check if the chosen prefix is ok
                if (prefix == null) {
                    if (serializedSchema.getAttributeNode("xmlns") == null) {
                        prefix = "";
                    }
                } else {
                    String ns = serializedSchema.getAttribute("xmlns:" + prefix);
                    if (ns != null && !"".equals(ns)) {
                        prefix = null;
                    }
                }
                if (prefix == null) {
                    //find a usable prefix
                    int count = 0;
                    prefix = "tns";
                    String ns = serializedSchema.getAttribute("xmlns:" + prefix);
                    while (ns != null && !"".equals(ns)) {
                        ++count;
                        prefix = "tns" + count;
                        ns = serializedSchema.getAttribute("xmlns:" + prefix);
                    }
                }
                if ("".equals(prefix)) {
                    serializedSchema.setAttributeNS(XMLNS_NAMESPACE_URI,
                                                    "xmlns", schemaObj.getSyntacticalTargetNamespace());
                } else {
                    serializedSchema.setAttributeNS(XMLNS_NAMESPACE_URI,
                                                    "xmlns:"
                                                    + prefix, schemaObj.getSyntacticalTargetNamespace());
                }
                schemaNamespace.put(schemaObj.getSyntacticalTargetNamespace(), prefix);
            }
        }


        //todo: implement xml:lang,
        if (schemaObj.getAttributeFormDefault() != null) {
            String formQualified = schemaObj.getAttributeFormDefault().toString();

            if (!formQualified.equals(XmlSchemaForm.NONE)) {
                serializedSchema.setAttribute("attributeFormDefault", formQualified);
            }
        }

        if (schemaObj.getElementFormDefault() != null) {
            String formQualified = schemaObj.getElementFormDefault().toString();

            if (!formQualified.equals(XmlSchemaForm.NONE)) {
                serializedSchema.setAttribute("elementFormDefault", formQualified);
            }
        }


        if (schemaObj.getAnnotation() != null) {
            Element annotation = serializeAnnotation(serializedSchemaDocs,
                    schemaObj.getAnnotation(), schemaObj);
            serializedSchema.appendChild(annotation);
        }
        if (schemaObj.getId() != null) {
            serializedSchema.setAttribute("id",
                    schemaObj.getId());
        }
        if (schemaObj.getBlockDefault() != XmlSchemaDerivationMethod.NONE) {
            String blockDefault = schemaObj.getBlockDefault().toString();
            serializedSchema.setAttribute("blockDefault", blockDefault);
        }

        if (schemaObj.getFinalDefault() != XmlSchemaDerivationMethod.NONE) {
            String finalDefault = schemaObj.getFinalDefault().toString();
            serializedSchema.setAttribute("finalDefault", finalDefault);
        }

        if (schemaObj.getVersion() != null) {
            serializedSchema.setAttribute("version", schemaObj.getVersion());
        }

        //after serialize the schema add into documentation
        //and add to document collection array  which at the end
        //returned
        serializeSchemaChild(items, serializedSchema, serializedSchemaDocs,
                schemaObj, serializeIncluded);

        //process extension elements/attributes
        processExtensibilityComponents(schemaObj, serializedSchema);


        serializedSchemaDocs.appendChild(serializedSchema);
        docs.add(serializedSchemaDocs);


        Document[] serializedDocs = new Document[docs.size()];
        docs.toArray(serializedDocs);

        return serializedDocs;
    }

    /**
     * ********************************************************************* Element
     * serializeSelector(Document doc, XmlSchemaXPath selectorObj, XmlSchema schema) throws
     * XmlSchemaSerializerException{
     * <p/>
     * Each member of complex type will be appended and pass the element created. Complex type processed
     * according to w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. selectorObj - XmlSchemaXPath that will be serialized. schema
     * - Schema Document object of the parent.
     * <p/>
     * Return: Element of selector with collection of xpath as its attrib. The selector itself is the part of
     * identity type. eg <key><selector xpath="..."
     * **********************************************************************
     */
    Element serializeSelector(Document doc, XmlSchemaXPath selectorObj, XmlSchema schema)
        throws XmlSchemaSerializerException {

        Element selector = createNewElement(doc, "selector", schema.getSchemaNamespacePrefix(),
                                            XmlSchema.SCHEMA_NS);

        if (selectorObj.xpath != null) {
            selector.setAttribute("xpath", selectorObj.xpath);
        } else {
            throw new XmlSchemaSerializerException("xpath can't be null");
        }

        if (selectorObj.getAnnotation() != null) {
            Element annotation = serializeAnnotation(doc, selectorObj.getAnnotation(), schema);
            selector.appendChild(annotation);
        }
        // process extension
        processExtensibilityComponents(selectorObj, selector);
        return selector;
    }

    /**
     * ********************************************************************* Element
     * serializeSequence(Document doc, XmlSchemaSequence sequenceObj, XmlSchema schema)throws
     * XmlSchemaSerializerException{
     * <p/>
     * Each member of complex type will be appended and pass the element created. `Complex type processed
     * according to w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. sequenceObj - XmlSchemaFacet that will be serialized. schema
     * - Schema Document object of the parent.
     * <p/>
     * Return: Element of sequence particle.
     * **********************************************************************
     */
    Element serializeSequence(Document doc, XmlSchemaSequence sequenceObj, XmlSchema schema)
        throws XmlSchemaSerializerException {

        Element sequence = createNewElement(doc, "sequence", schema.getSchemaNamespacePrefix(),
                                            XmlSchema.SCHEMA_NS);

        if (sequenceObj.getId() != null) {
            sequence.setAttribute("id", sequenceObj.getId());
        }

        serializeMaxMinOccurs(sequenceObj, sequence);

        List<XmlSchemaSequenceMember> seqColl = sequenceObj.getItems();
        int containLength = seqColl.size();
        for (int i = 0; i < containLength; i++) {
            XmlSchemaSequenceMember obj = seqColl.get(i);
            if (obj instanceof XmlSchemaElement) {
                Element el = serializeElement(doc, (XmlSchemaElement)obj, schema);
                sequence.appendChild(el);
            } else if (obj instanceof XmlSchemaGroupRef) {
                Element group = serializeGroupRef(doc, (XmlSchemaGroupRef)obj, schema);
                sequence.appendChild(group);
            } else if (obj instanceof XmlSchemaChoice) {
                Element choice = serializeChoice(doc, (XmlSchemaChoice)obj, schema);
                sequence.appendChild(choice);
            } else if (obj instanceof XmlSchemaSequence) {
                Element sequenceChild = serializeSequence(doc, (XmlSchemaSequence)obj, schema);
                sequence.appendChild(sequenceChild);
            } else if (obj instanceof XmlSchemaAny) {
                Element any = serializeAny(doc, (XmlSchemaAny)obj, schema);
                sequence.appendChild(any);
            }
        }

        // process extension
        processExtensibilityComponents(sequenceObj, sequence);

        return sequence;
    }

    /**
     * ********************************************************************* Element
     * serializeSimpleContent(Document doc, XmlSchemaSimpleContent simpleContentObj, XmlSchema schema) throws
     * XmlSchemaSerializerException{
     * <p/>
     * Each member of complex type will be appended and pass the element created. Complex type processed
     * according to w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. simpleContentObj - XmlSchemaSimpleContent that will be
     * serialized. schema - Schema Document object of the parent.
     * <p/>
     * Return: Element of complex type simple content.
     * **********************************************************************
     */
    Element serializeSimpleContent(Document doc, XmlSchemaSimpleContent simpleContentObj, XmlSchema schema)
        throws XmlSchemaSerializerException {
        Element simpleContent = createNewElement(doc, "simpleContent", schema.getSchemaNamespacePrefix(),
                                                 XmlSchema.SCHEMA_NS);

        Element content;
        if (simpleContentObj.getAnnotation() != null) {
            Element annotation = serializeAnnotation(doc, simpleContentObj.getAnnotation(), schema);
            simpleContent.appendChild(annotation);
        }
        if (simpleContentObj.content instanceof XmlSchemaSimpleContentRestriction) {
            content = serializeSimpleContentRestriction(
                                                        doc,
                                                        (XmlSchemaSimpleContentRestriction)
                                                        simpleContentObj.content,
                                                        schema);
        } else if (simpleContentObj.content instanceof XmlSchemaSimpleContentExtension) {
            content = serializeSimpleContentExtension(
                                                      doc,
                                                      (XmlSchemaSimpleContentExtension)
                                                      simpleContentObj.content,
                                                      schema);
        } else {
            throw new XmlSchemaSerializerException("content of simple content "
                                                   + "must be restriction or extension");
        }

        simpleContent.appendChild(content);

        // process extension
        processExtensibilityComponents(simpleContentObj, simpleContent);

        return simpleContent;
    }

    /**
     * ********************************************************************* Element
     * serializeSimpleContentExtension(Document doc, XmlSchemaSimpleContentExtension extensionObj, XmlSchema
     * schema) throws XmlSchemaSerializerException{
     * <p/>
     * <p/>
     * Each member of complex type will be appended and pass the element created. Complex type processed
     * according to w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. extensionObj - XmlSchemaSimpleContentExtension that will be
     * serialized. schema - Schema Document object of the parent.
     * <p/>
     * Return: Element of simple content extension.
     * **********************************************************************
     */
    Element serializeSimpleContentExtension(Document doc, XmlSchemaSimpleContentExtension extensionObj,
                                            XmlSchema schema) throws XmlSchemaSerializerException {

        Element extension = createNewElement(doc, "extension", schema.getSchemaNamespacePrefix(),
                                             XmlSchema.SCHEMA_NS);

        if (extensionObj.getBaseTypeName() != null) {
            String baseTypeName = resolveQName(extensionObj.getBaseTypeName(), schema);

            extension.setAttribute("base", baseTypeName);
        }

        if (extensionObj.getId() != null) {
            extension.setAttribute("id", extensionObj.getId());
        }

        if (extensionObj.getAnnotation() != null) {
            Element annotation = serializeAnnotation(doc, extensionObj.getAnnotation(), schema);
            extension.appendChild(annotation);
        }

        List<XmlSchemaAttributeOrGroupRef> attributes = extensionObj.getAttributes();
        int attributeLength = attributes.size();
        for (int i = 0; i < attributeLength; i++) {
            XmlSchemaObject obj = attributes.get(i);

            if (obj instanceof XmlSchemaAttribute) {
                Element attribute = serializeAttribute(doc, (XmlSchemaAttribute)obj, schema);
                extension.appendChild(attribute);
            } else if (obj instanceof XmlSchemaAttributeGroupRef) {
                Element attributeGroupRef = serializeAttributeGroupRef(doc, (XmlSchemaAttributeGroupRef)obj,
                                                                       schema);
                extension.appendChild(attributeGroupRef);
            }
        }

        /*
         * anyAttribute must comeafter any other attributes
         */
        if (extensionObj.getAnyAttribute() != null) {
            Element anyAttribute = serializeAnyAttribute(doc, extensionObj.getAnyAttribute(), schema);
            extension.appendChild(anyAttribute);
        }

        // process extension
        processExtensibilityComponents(extensionObj, extension);

        return extension;
    }

    /**
     * ********************************************************************* Element
     * serializeSimpleContentRestriction(Document doc, XmlSchemaSimpleContentRestriction restrictionObj,
     * XmlSchema schema) throws XmlSchemaSerializerException{
     * <p/>
     * <p/>
     * Each member of complex type will be appended and pass the element created. Complex type processed
     * according to w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. restrictionObj - XmlSchemaSimpleContentRestriction that will
     * be serialized. schema - Schema Document object of the parent.
     * <p/>
     * Return: Element of simple content restriction.
     * **********************************************************************
     */
    Element serializeSimpleContentRestriction(Document doc, XmlSchemaSimpleContentRestriction restrictionObj,
                                              XmlSchema schema) throws XmlSchemaSerializerException {

        Element restriction = createNewElement(doc, "restriction", schema.getSchemaNamespacePrefix(),
                                               XmlSchema.SCHEMA_NS);

        if (restrictionObj.getBaseTypeName() != null) {
            String baseTypeName = resolveQName(restrictionObj.getBaseTypeName(), schema);

            restriction.setAttribute("base", baseTypeName);

        }
        if (restrictionObj.getId() != null) {
            restriction.setAttribute("id", restrictionObj.getId());
        }

        if (restrictionObj.getAnnotation() != null) {
            Element annotation = serializeAnnotation(doc, restrictionObj.getAnnotation(), schema);
            restriction.appendChild(annotation);
        }
        int attrCollLength = restrictionObj.getAttributes().size();
        for (int i = 0; i < attrCollLength; i++) {
            XmlSchemaAnnotated obj = restrictionObj.getAttributes().get(i);

            if (obj instanceof XmlSchemaAttribute) {
                Element attribute = serializeAttribute(doc, (XmlSchemaAttribute)obj, schema);
                restriction.appendChild(attribute);
            } else if (obj instanceof XmlSchemaAttributeGroupRef) {
                Element attributeGroup = serializeAttributeGroupRef(doc, (XmlSchemaAttributeGroupRef)obj,
                                                                    schema);
                restriction.appendChild(attributeGroup);
            }
        }
        if (restrictionObj.getBaseType() != null) {
            Element inlineSimpleType = serializeSimpleType(doc, restrictionObj.getBaseType(), schema);
            restriction.appendChild(inlineSimpleType);
        }
        if (restrictionObj.anyAttribute != null) {
            Element anyAttribute = serializeAnyAttribute(doc, restrictionObj.anyAttribute, schema);
            restriction.appendChild(anyAttribute);
        }
        List<XmlSchemaFacet> facets = restrictionObj.getFacets();
        int facetLength = facets.size();
        for (int i = 0; i < facetLength; i++) {
            Element facet = serializeFacet(doc, facets.get(i), schema);
            restriction.appendChild(facet);
        }

        // process extension
        processExtensibilityComponents(restrictionObj, restriction);

        return restriction;
    }

    /**
     * ********************************************************************* Element
     * serializeSimpleType(Document doc, XmlSchemaSimpleType simpleTypeObj, XmlSchema schema) throws
     * XmlSchemaSerializerException{
     * <p/>
     * Each member of simple type will be appended and pass the element created. Simple type processed
     * according to w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. simpleTypeObj - XmlSchemaSimpleType that will be serialized.
     * schema - Schema Document object of the parent.
     * <p/>
     * Return: Element object of SimpleType
     * **********************************************************************
     */
    Element serializeSimpleType(Document doc, XmlSchemaSimpleType simpleTypeObj, XmlSchema schema)
        throws XmlSchemaSerializerException {

        Element serializedSimpleType = createNewElement(doc, "simpleType", schema.getSchemaNamespacePrefix(),
                                                        XmlSchema.SCHEMA_NS);

        if (simpleTypeObj.getFinalDerivation() != null
            && simpleTypeObj.getFinalDerivation() != XmlSchemaDerivationMethod.NONE) {
            serializedSimpleType.setAttribute("final", simpleTypeObj.getFinalDerivation().toString());
        }
        if (simpleTypeObj.getId() != null) {
            serializedSimpleType.setAttribute("id", simpleTypeObj.getId());
        }
        if (!simpleTypeObj.isAnonymous()) {
            serializedSimpleType.setAttribute("name", simpleTypeObj.getName());
        }
        if (simpleTypeObj.getAnnotation() != null) {
            Element annotationEl = serializeAnnotation(doc, simpleTypeObj.getAnnotation(), schema);
            serializedSimpleType.appendChild(annotationEl);
        }
        if (simpleTypeObj.content != null) {
            if (simpleTypeObj.content instanceof XmlSchemaSimpleTypeRestriction) {
                Element restEl = serializeSimpleTypeRestriction(
                                                                doc,
                                                                (XmlSchemaSimpleTypeRestriction)
                                                                simpleTypeObj.content,
                                                                schema);
                serializedSimpleType.appendChild(restEl);
            } else if (simpleTypeObj.content instanceof XmlSchemaSimpleTypeList) {
                Element listEl = serializeSimpleTypeList(doc, (XmlSchemaSimpleTypeList)simpleTypeObj.content,
                                                         schema);
                serializedSimpleType.appendChild(listEl);
            } else if (simpleTypeObj.content instanceof XmlSchemaSimpleTypeUnion) {
                Element unionEl = serializeSimpleTypeUnion(doc,
                                                           (XmlSchemaSimpleTypeUnion)simpleTypeObj.content,
                                                           schema);
                serializedSimpleType.appendChild(unionEl);
            }
            /*
             * else throw new XmlSchemaSerializerException("Invalid type inserted " +
             * "in simpleType content, the content is: " + simpleTypeObj.content.getClass().getName() +
             * " valid content should be XmlSchemaSimpleTypeunion, " +
             * "XmlSchemaSimpleTyperestriction or list");
             */
        }
        /*
         * else throw new XmlSchemaSerializerException("simple type must be set " +
         * "with content, either union, restriction or list");
         */

        // process extension
        processExtensibilityComponents(simpleTypeObj, serializedSimpleType);

        return serializedSimpleType;
    }

    /**
     * ********************************************************************* Element
     * serializeSimpleTypeList(Document doc, XmlSchemaSimpleTypeList listObj, XmlSchema schema) throws
     * XmlSchemaSerializerException{
     * <p/>
     * Each member of complex type will be appended and pass the element created. Complex type processed
     * according to w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. listObj - XmlSchemaSimpleTypeList that will be serialized.
     * schema - Schema Document object of the parent.
     * <p/>
     * Return: Element of simple type with list method.
     * **********************************************************************
     */
    Element serializeSimpleTypeList(Document doc, XmlSchemaSimpleTypeList listObj, XmlSchema schema)
        throws XmlSchemaSerializerException {

        Element list = createNewElement(doc, "list", schema.getSchemaNamespacePrefix(), XmlSchema.SCHEMA_NS);

        if (listObj.itemTypeName != null) {
            String listItemType = resolveQName(listObj.itemTypeName, schema);
            list.setAttribute("itemType", listItemType);
        }
        if (listObj.getId() != null) {
            list.setAttribute("id", listObj.getId());
        } else if (listObj.itemType != null) {
            Element inlineSimpleEl = serializeSimpleType(doc, listObj.itemType, schema);
            list.appendChild(inlineSimpleEl);
        }
        if (listObj.getAnnotation() != null) {
            Element annotation = serializeAnnotation(doc, listObj.getAnnotation(), schema);
            list.appendChild(annotation);
        }

        // process extension
        processExtensibilityComponents(listObj, list);

        return list;
    }

    /**
     * ********************************************************************* Element
     * serializeSimpleTypeRestriction(Document doc, XmlSchemaSimpleTypeRestriction restrictionObj, XmlSchema
     * schema) throws XmlSchemaSerializerException{
     * <p/>
     * Each member of simple type will be appended and pass the element created. Simple type's <restriction>
     * processed according to w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. restrictionObj - XmlSchemaRestriction that will be
     * serialized. schema - Schema Document object of the parent.
     * <p/>
     * Return: Element of simple type restriction and its child.
     * **********************************************************************
     */
    Element serializeSimpleTypeRestriction(Document doc, XmlSchemaSimpleTypeRestriction restrictionObj,
                                           XmlSchema schema) throws XmlSchemaSerializerException {
        // todo: need to implement any attribute that related to non schema namespace
        Element serializedRestriction = createNewElement(doc, "restriction",
                                                         schema.getSchemaNamespacePrefix(),
                                                         XmlSchema.SCHEMA_NS);

        if (schema.getSchemaNamespacePrefix().length() > 0) {
            serializedRestriction.setPrefix(schema.getSchemaNamespacePrefix());
        }
        if (restrictionObj.getBaseTypeName() != null) {
            String baseType = resolveQName(restrictionObj.getBaseTypeName(), schema);
            serializedRestriction.setAttribute("base", baseType);
        } else if (restrictionObj.getBaseType() instanceof XmlSchemaSimpleType) {
            Element inlineSimpleType = serializeSimpleType(doc, restrictionObj.getBaseType(), schema);
            serializedRestriction.appendChild(inlineSimpleType);
        } else {
            throw new XmlSchemaSerializerException("restriction must be define "
                                                   + "with specifying base or inline simpleType");
        }

        if (restrictionObj.getId() != null) {
            serializedRestriction.setAttribute("id", restrictionObj.getId());
        }

        if (restrictionObj.getAnnotation() != null) {
            Element annotation = serializeAnnotation(doc, restrictionObj.getAnnotation(), schema);
            serializedRestriction.appendChild(annotation);
        }
        if (restrictionObj.getFacets().size() > 0) {
            int facetsNum = restrictionObj.getFacets().size();
            for (int i = 0; i < facetsNum; i++) {
                Element facetEl = serializeFacet(doc, (XmlSchemaFacet)restrictionObj.getFacets().get(i),
                                                 schema);
                serializedRestriction.appendChild(facetEl);
            }
        }

        // process extension
        processExtensibilityComponents(restrictionObj, serializedRestriction);

        return serializedRestriction;
    }

    /**
     * ********************************************************************* Element
     * serializeSimpleTypeUnion(Document doc, XmlSchemaSimpleTypeUnion unionObj, XmlSchema schema) throws
     * XmlSchemaSerializerException{
     * <p/>
     * Each member of complex type will be appended and pass the element created. Complex type processed
     * according to w3c Recommendation May 2 2001.
     * <p/>
     * Parameter: doc - Document the parent use. unionObj - XmlSchemaSimpleTypeUnion that will be serialized.
     * schema - Schema Document object of the parent.
     * <p/>
     * Return: Element of simple type with union method.
     * **********************************************************************
     */
    Element serializeSimpleTypeUnion(Document doc, XmlSchemaSimpleTypeUnion unionObj, XmlSchema schema)
        throws XmlSchemaSerializerException {

        Element union = createNewElement(doc, "union", schema.getSchemaNamespacePrefix(),
                                         XmlSchema.SCHEMA_NS);
        if (unionObj.getId() != null) {
            union.setAttribute("id", unionObj.getId());
        }

        if (unionObj.getMemberTypesSource() != null) {
            union.setAttribute("memberTypes", unionObj.getMemberTypesSource());
        }
        if (unionObj.getBaseTypes().size() > 0) {
            int baseTypesLength = unionObj.getBaseTypes().size();
            Element baseType;
            for (int i = 0; i < baseTypesLength; i++) {
                try {
                    baseType = serializeSimpleType(doc, (XmlSchemaSimpleType)unionObj.getBaseTypes().get(i),
                                                   schema);
                    union.appendChild(baseType);
                } catch (ClassCastException e) {
                    throw new XmlSchemaSerializerException("only inline simple type allow as attribute's "
                                                           + "inline type");
                }
            }
        }
        if (unionObj.getAnnotation() != null) {
            Element annotation = serializeAnnotation(doc, unionObj.getAnnotation(), schema);
            union.appendChild(annotation);
        }

        // process extension
        processExtensibilityComponents(unionObj, union);

        return union;
    }

    // for each collection if it is an attribute serialize attribute and
    // append that child to container element.
    void setupAttr(Document doc, List<XmlSchemaAttributeOrGroupRef> attrColl,
                   XmlSchema schema, Element container)
        throws XmlSchemaSerializerException {
        int collectionLength = attrColl.size();
        for (int i = 0; i < collectionLength; i++) {
            XmlSchemaAttributeOrGroupRef obj = attrColl.get(i);
            if (obj instanceof XmlSchemaAttribute) {
                XmlSchemaAttribute attr = (XmlSchemaAttribute)obj;
                Element attrEl = serializeAttribute(doc, attr, schema);
                container.appendChild(attrEl);
            } else if (obj instanceof XmlSchemaAttributeGroupRef) {
                XmlSchemaAttributeGroupRef attr = (XmlSchemaAttributeGroupRef)obj;
                Element attrEl = serializeAttributeGroupRef(doc, attr, schema);
                container.appendChild(attrEl);
            }
        }
    }

    // recursively add any attribute, text and children append all
    // found children base on parent as its root.
    private void appendElement(Document doc, Element parent, Node children, XmlSchema schema) {
        Element elTmp = (Element)children;
        Element el = createNewElement(doc, elTmp.getLocalName(), schema.getSchemaNamespacePrefix(),
                                      XmlSchema.SCHEMA_NS);
        NamedNodeMap attributes = el.getAttributes();
        // check if child node has attribute
        // create new element and append it if found
        int attributeLength = attributes.getLength();
        for (int i = 0; i < attributeLength; i++) {
            Node n = attributes.item(i);
            // assuming attributes got to throw exception if not later
            el.setAttribute(n.getNodeName(), n.getNodeValue());
        }

        // check any descendant of this node
        // if there then append its child
        NodeList decendants = el.getChildNodes();
        int decendantLength = decendants.getLength();
        for (int i = 0; i < decendantLength; i++) {
            Node n = decendants.item(i);
            short nodeType = n.getNodeType();
            if (nodeType == Node.TEXT_NODE) {
                String nValue = n.getNodeValue();
                Text t = doc.createTextNode(nValue);
                el.appendChild(t);
            } else if (nodeType == Node.CDATA_SECTION_NODE) {
                String nValue = n.getNodeValue();
                CDATASection s = doc.createCDATASection(nValue);
                el.appendChild(s);
            } else if (nodeType == Node.ELEMENT_NODE) {
                appendElement(doc, el, n, schema);
            }
        }
    }

    private Element constructFacet(XmlSchemaFacet facetObj, Document doc, XmlSchema schema, String tagName) {

        Element facetEl = createNewElement(doc, tagName, schema.getSchemaNamespacePrefix(),
                                           XmlSchema.SCHEMA_NS);

        facetEl.setAttribute("value", facetObj.value.toString());
        if (facetObj.fixed) {
            facetEl.setAttribute("fixed", "true");
        }

        if (facetObj.getAnnotation() != null) {
            Element annotation = serializeAnnotation(doc, facetObj.getAnnotation(), schema);
            facetEl.appendChild(annotation);
        }
        return facetEl;
    }

    // Create new element with given local name and namespaces check whether
    // the prefix is there or not.
    private Element createNewElement(Document document, String localName, String prefix, String namespace) {
        if (prefix.length() > 0) {
            prefix += ":";
        }
        String elementName = prefix + localName;
        return document.createElementNS(namespace, elementName);
    }

    /**
     * A generic method to process the extra attributes and the extra elements present within the schema. What
     * are considered extensions are child elements with non schema namespace and child attributes with any
     * namespace.
     *
     * @param schemaObject
     * @param parentElement
     */
    private void processExtensibilityComponents(XmlSchemaObject schemaObject, Element parentElement) {

        if (extReg != null) {
            Map metaInfoMap = schemaObject.getMetaInfoMap();
            if (metaInfoMap != null && !metaInfoMap.isEmpty()) {
                // get the extra objects and call the respective deserializers
                Iterator keysIt = metaInfoMap.keySet().iterator();
                while (keysIt.hasNext()) {
                    Object key = keysIt.next();
                    extReg.serializeExtension(schemaObject, metaInfoMap.get(key).getClass(), parentElement);

                }

            }

        }

    }

    /**
     * will search whether the prefix is available in global hash table, if it is there than append the prefix
     * to the element name. If not then it will create new prefix corresponding to that namespace and store
     * that in hash table. Finally add the new prefix and namespace to <schema> element
     *
     * @param names
     * @param schemaObj
     * @return resolved QName of the string
     */

    private String resolveQName(QName names, XmlSchema schemaObj) {

        String namespace = names.getNamespaceURI();
        String type[] = getParts(names.getLocalPart());
        String typeName = type.length > 1 ? type[1] : type[0];
        String prefixStr;

        // If the namespace is "" then the prefix is also ""
        String prefix = "".equals(namespace) ? "" : schemaNamespace.get(namespace);

        if (prefix == null) {
            if (Constants.XMLNS_URI.equals(namespace)) {
                prefix = Constants.XMLNS_PREFIX;
            } else {
                int magicNumber = 0;
                Collection<String> prefixes = schemaNamespace.values();
                while (prefixes.contains("ns" + magicNumber)) {
                    magicNumber++;
                }
                prefix = "ns" + magicNumber;
                schemaNamespace.put(namespace, prefix);

                // setting xmlns in schema
                schemaElement.setAttributeNS(XMLNS_NAMESPACE_URI, "xmlns:" + prefix.toString(), namespace);
            }
        }

        prefixStr = prefix.toString();
        prefixStr = prefixStr.trim().length() > 0 ? prefixStr + ":" : "";

        return prefixStr + typeName;
    }

    /**
     * A common method to serialize the max/min occurs
     *
     * @param particle
     * @param element
     */
    private void serializeMaxMinOccurs(XmlSchemaParticle particle, Element element) {
        if (particle.getMaxOccurs() < Long.MAX_VALUE
            && (particle.getMaxOccurs() > 1 || particle.getMaxOccurs() == 0)) {
            element.setAttribute("maxOccurs", particle.getMaxOccurs() + "");
        } else if (particle.getMaxOccurs() == Long.MAX_VALUE) {
            element.setAttribute("maxOccurs", "unbounded");
            // else not serialized
        }

        // 1 is the default and hence not serialized
        // there is no valid case where min occurs can be unbounded!
        if (particle.getMinOccurs() > 1 || particle.getMinOccurs() == 0) {
            element.setAttribute("minOccurs", particle.getMinOccurs() + "");
        }
    }

    private void serializeSchemaChild(List<XmlSchemaObject> items, Element serializedSchema,
                                      Document serializedSchemaDocs, XmlSchema schemaObj,
                                      boolean serializeIncluded) throws XmlSchemaSerializerException {

        int itemsLength = items.size();
        /**
         * For each of the items that belong to this schema, serialize each member found. Valid members
         * are: element, simpleType, complexType, group, attrributeGroup, Attribute, include, import and
         * redefine. if any of the member found then serialize the component.
         */

        // Since imports and includes need to be the first items of the
        // serialized schema. So this loop does the serialization of the
        // imports and includes
        for (int i = 0; i < itemsLength; i++) {
            XmlSchemaObject obj = items.get(i);
            if (obj instanceof XmlSchemaInclude) {
                Element e = serializeInclude(serializedSchemaDocs, (XmlSchemaInclude)obj, schemaObj,
                                             serializeIncluded);
                serializedSchema.appendChild(e);
            } else if (obj instanceof XmlSchemaImport) {
                Element e = serializeImport(serializedSchemaDocs, (XmlSchemaImport)obj, schemaObj,
                                            serializeIncluded);
                serializedSchema.appendChild(e);
            }
        }

        // reloop to serialize the others
        for (int i = 0; i < itemsLength; i++) {
            XmlSchemaObject obj = items.get(i);

            if (obj instanceof XmlSchemaElement) {
                Element e = serializeElement(serializedSchemaDocs, (XmlSchemaElement)obj, schemaObj);
                serializedSchema.appendChild(e);

            } else if (obj instanceof XmlSchemaSimpleType) {
                Element e = serializeSimpleType(serializedSchemaDocs, (XmlSchemaSimpleType)obj, schemaObj);
                serializedSchema.appendChild(e);
            } else if (obj instanceof XmlSchemaComplexType) {
                Element e = serializeComplexType(serializedSchemaDocs, (XmlSchemaComplexType)obj, schemaObj);
                serializedSchema.appendChild(e);
            } else if (obj instanceof XmlSchemaGroup) {
                Element e = serializeGroup(serializedSchemaDocs, (XmlSchemaGroup)obj, schemaObj);
                serializedSchema.appendChild(e);
            } else if (obj instanceof XmlSchemaAttributeGroup) {
                Element e = serializeAttributeGroup(serializedSchemaDocs, (XmlSchemaAttributeGroup)obj,
                                                    schemaObj);
                serializedSchema.appendChild(e);
            } else if (obj instanceof XmlSchemaAttribute) {
                Element e = serializeAttribute(serializedSchemaDocs, (XmlSchemaAttribute)obj, schemaObj);
                serializedSchema.appendChild(e);
            } else if (obj instanceof XmlSchemaRedefine) {
                Element e = serializeRedefine(serializedSchemaDocs, (XmlSchemaRedefine)obj, schemaObj);
                serializedSchema.appendChild(e);
            }
        }
    }
    /**
     * Set up <schema> namespaces appropriately and append that attr
     * into specified element
     */
    private Element setupNamespaces(Document schemaDocs, XmlSchema schemaObj) {
        NamespacePrefixList ctx = schemaObj.getNamespaceContext();
        if (ctx != null) {
            xsdPrefix = ctx.getPrefix(XSD_NAMESPACE);
        } else {
            xsdPrefix = null;
        }

        if (xsdPrefix == null) {
            //find a prefix to use
            xsdPrefix = "";
            if (ctx != null && ctx.getNamespaceURI(xsdPrefix) != null) {
                xsdPrefix = "xsd";
            }
            int count = 0;
            while (ctx != null && ctx.getNamespaceURI(xsdPrefix) != null) {
                xsdPrefix = "xsd" + ++count;
            }
        }
        schemaObj.setSchemaNamespacePrefix(xsdPrefix);


        Element schemaEl = createNewElement(schemaDocs, "schema",
                                            schemaObj.getSchemaNamespacePrefix(), XmlSchema.SCHEMA_NS);

        if (ctx != null) {
            String[] prefixes = ctx.getDeclaredPrefixes();
            for (int i = 0;  i < prefixes.length;  i++) {
                String prefix = prefixes[i];
                String uri = ctx.getNamespaceURI(prefix);
                if (uri != null && prefix != null) {
                    if ("".equals(prefix) || !schemaNamespace.containsKey(uri)) {
                        schemaNamespace.put(uri, prefix);
                    }
                    prefix = (prefix.length() > 0) ? "xmlns:" + prefix : "xmlns";
                    schemaEl.setAttributeNS(XMLNS_NAMESPACE_URI,
                                            prefix, uri);
                }
            }
        }
        //for schema that not set the xmlns attrib member
        if (schemaNamespace.get(XSD_NAMESPACE) == null) {
            schemaNamespace.put(XSD_NAMESPACE, xsdPrefix);
            if ("".equals(xsdPrefix)) {
                schemaEl.setAttributeNS(XMLNS_NAMESPACE_URI,
                                        "xmlns", XSD_NAMESPACE);
            } else {
                schemaEl.setAttributeNS(XMLNS_NAMESPACE_URI,
                                        "xmlns:" + xsdPrefix, XSD_NAMESPACE);
            }
            schemaObj.setSchemaNamespacePrefix(xsdPrefix);
        }
        return schemaEl;
    }

}
