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
package org.apache.ws.commons.schema.resolver;

import org.xml.sax.InputSource;

/**
 * Resolves the <code>schemaLocation</code> of an <code>xs:import</code>,
 * <code>xs:include</code> or <code>xs:redefine</code> to the document it names.
 * <p>
 * An implementation of this interface decides what a schema document is allowed to pull in, so it
 * is the control point for applications that parse untrusted schema documents: install one that
 * refuses locations outside an approved set, via
 * {@link org.apache.ws.commons.schema.XmlSchemaCollection#setSchemaResolver(URIResolver)}.
 * </p>
 */
public interface URIResolver {
    /**
     * Resolve a schema location to the document it names.
     * 
     * @param targetNamespace the target namespace of the referenced schema, as declared by the
     *                        referring document.
     * @param schemaLocation the schema location to resolve.
     * @param baseUri the base URI of the referring document, or <code>null</code> if it has none.
     * @return an input source for the referenced document, or <code>null</code> to decline the
     *         location.
     */
    InputSource resolveEntity(String targetNamespace, String schemaLocation, String baseUri);

}
