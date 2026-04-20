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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.xml.sax.InputSource;

/**
 * This resolver provides the means of resolving the imports and includes of a given schema document. The
 * system will call this default resolver if there is no other resolver present in the system.
 */
public class DefaultURIResolver implements CollectionURIResolver {

    private String collectionBaseURI;

    /**
     * Try to resolve a schema location to some data.
     *
     * @param namespace target namespace.
     * @param schemaLocation system ID.
     * @param baseUri base URI for the schema.
     */
    public InputSource resolveEntity(String namespace, String schemaLocation, String baseUri) {

        if (baseUri != null) {
            try {
                File baseFile = null;
                try {
                    URI uri = new URI(baseUri);
                    baseFile = new File(uri);
                    if (!baseFile.exists()) {
                        baseFile = new File(baseUri);
                    }
                } catch (Throwable ex) {
                    baseFile = new File(baseUri);
                }
                if (baseFile.exists()) {
                    baseUri = baseFile.toURI().toString();
                } else if (collectionBaseURI != null) {
                    baseFile = new File(collectionBaseURI);
                    if (baseFile.exists()) {
                        baseUri = baseFile.toURI().toString();
                    }
                }

                String ref = new URL(new URL(baseUri), schemaLocation).toString();

                return new InputSource(ref);
            } catch (MalformedURLException e1) {
                throw new RuntimeException(e1);
            }

        }
        return new InputSource(schemaLocation);

    }

    /**
     * Get the base URI derived from a schema collection. It serves as a fallback from the specified base.
     *
     * @return URI
     */
    public String getCollectionBaseURI() {
        return collectionBaseURI;
    }

    /**
     * set the collection base URI, which serves as a fallback from the base of the immediate schema.
     *
     * @param collectionBaseURI the URI.
     */
    public void setCollectionBaseURI(String collectionBaseURI) {
        this.collectionBaseURI = collectionBaseURI;
    }
}
