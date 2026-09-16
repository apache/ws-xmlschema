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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.apache.ws.commons.schema.XmlSchemaException;
import org.xml.sax.InputSource;

/**
 * This resolver provides the means of resolving the imports and includes of a given schema document. The
 * system will call this default resolver if there is no other resolver present in the system.
 * <p>
 * This resolver is a convenience for trusted, operator-controlled schema sets. It restricts the URI
 * schemes it will resolve to <code>http</code>, <code>https</code>, <code>file</code> and
 * <code>jar</code>, and refuses a schema location that changes the scheme of a remote base URI,
 * names a non-local authority with the <code>file:</code> scheme, or reads a <code>jar:</code>
 * archive fetched over the network. Within the schemes it does allow it
 * applies no host or address filtering, so any reachable host or readable file a schema location
 * names is fetched. An application that parses untrusted schema documents must install a restricting
 * resolver instead; see
 * {@link org.apache.ws.commons.schema.XmlSchemaCollection#setSchemaResolver(URIResolver)}.
 * </p>
 */
public class DefaultURIResolver implements CollectionURIResolver {

    /**
     * The URI schemes this resolver is willing to hand back to the parser. Schemes outside this
     * set are never used by schema documents, so refusing them costs nothing and keeps the JDK
     * URL handlers for them out of reach of an untrusted <code>schemaLocation</code>.
     */
    private static final Set<String> ALLOWED_SCHEMES = Collections.unmodifiableSet(
        new HashSet<String>(Arrays.asList("http", "https", "file", "jar")));

    private String collectionBaseURI;

    /**
     * Try to resolve a schema location to some data.
     *
     * @param namespace target namespace.
     * @param schemaLocation system ID.
     * @param baseUri base URI for the schema.
     * @return an input source for the resolved location, or <code>null</code> if the location
     *         cannot be resolved against the given base.
     * @throws XmlSchemaException if the location resolves to a URI scheme this resolver does not
     *         permit, or escapes the scheme or authority of its base URI.
     */
    public InputSource resolveEntity(String namespace, String schemaLocation, String baseUri) {

        if (baseUri != null) {
            final String originalBaseUri = baseUri;
            final boolean remoteBase = isRemoteBase(baseUri);
            try {
                if (!remoteBase) {
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
                }

                URL base = new URL(baseUri);
                URL ref = new URL(base, schemaLocation);
                verifyComposedUrl(remoteBase, originalBaseUri, base, ref, schemaLocation);

                return new InputSource(ref.toString());
            } catch (MalformedURLException e1) {
                throw new XmlSchemaException("Unable to resolve the schema location \"" + schemaLocation
                                             + "\" against the base URI \"" + baseUri + "\"", e1);
            }

        }
        if (isAbsoluteUri(schemaLocation)) {
            verifyPermittedLocation(schemaLocation, schemaLocation);
            return new InputSource(schemaLocation);
        }
        if (isPlainRelativePath(schemaLocation)) {
            return new InputSource(schemaLocation);
        }
        return null;

    }

    private static void verifyComposedUrl(boolean remoteBase, String originalBaseUri, URL base,
                                          URL composed, String schemaLocation) {
        verifyPermittedLocation(composed.toString(), schemaLocation);
        final String composedScheme = composed.getProtocol().toLowerCase(Locale.ENGLISH);
        if (isAbsoluteUri(schemaLocation)) {
            if (remoteBase && !isNetworkScheme(composedScheme)) {
                throw new XmlSchemaException("The schema location \"" + schemaLocation
                                             + "\" of the remote base URI \"" + originalBaseUri
                                             + "\" uses the non-network scheme \"" + composedScheme
                                             + "\".");
            }
            return;
        }
        if (remoteBase) {
            final String originalScheme = extractScheme(originalBaseUri);
            if (originalScheme != null && !originalScheme.equals(composedScheme)) {
                throw new XmlSchemaException("The schema location \"" + schemaLocation
                                             + "\" changes the scheme of its base URI from \""
                                             + originalScheme + "\" to \"" + composedScheme + "\".");
            }
        }
        if (!composed.getProtocol().equalsIgnoreCase(base.getProtocol())) {
            throw new XmlSchemaException("The schema location \"" + schemaLocation
                                         + "\" changes the scheme of its base URI from \""
                                         + base.getProtocol() + "\" to \"" + composed.getProtocol() + "\".");
        }
    }

    /**
     * Refuse a resolved location this resolver will not dereference. Three rules apply, to every
     * location and whatever the base URI was: the effective scheme must be one a schema document
     * may use, a <code>jar:</code> URL may not pull its archive over the network, and a
     * <code>file:</code> URL may not name a remote authority.
     *
     * @param uri the resolved location that would be handed to the parser.
     * @param schemaLocation the original schema location, for the error message.
     */
    private static void verifyPermittedLocation(String uri, String schemaLocation) {
        final String scheme = effectiveScheme(uri);
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme)) {
            throw new XmlSchemaException("The schema location \"" + schemaLocation
                                         + "\" resolves to the scheme \"" + scheme
                                         + "\", which is not permitted by DefaultURIResolver.");
        }
        final String trimmed = uri.trim();
        final boolean wrapped = "jar".equals(extractScheme(trimmed));
        // A jar: URL delegates to the URL of the archive; the entry after "!/" is inside it.
        final String archive = wrapped ? stripJarEntry(trimmed.substring(4)) : trimmed;
        if (wrapped && isNetworkScheme(scheme)) {
            throw new XmlSchemaException("The schema location \"" + schemaLocation
                                         + "\" reads an archive fetched over the network, \""
                                         + archive + "\", which is not permitted by"
                                         + " DefaultURIResolver.");
        }
        if ("file".equals(scheme) && !isLocalFileUri(archive)) {
            throw new XmlSchemaException("The schema location \"" + schemaLocation
                                         + "\" resolves to a file URL with a non-local authority.");
        }
    }

    /**
     * Drop the entry part of a jar: URL, leaving the URL of the archive itself. The entry is an
     * arbitrary path inside the archive and need not parse as part of a URI.
     */
    private static String stripJarEntry(String uri) {
        final int separator = uri.indexOf("!/");
        return separator < 0 ? uri : uri.substring(0, separator);
    }

    /**
     * The scheme that is actually dereferenced when the location is fetched. A "jar:" URL
     * delegates to the URL it wraps, so "jar:http://host/a.jar!/x.xsd" performs an HTTP fetch
     * even though its protocol reads as "jar".
     */
    private static String effectiveScheme(String uri) {
        final String trimmed = uri.trim();
        final String scheme = extractScheme(trimmed);
        if ("jar".equals(scheme)) {
            final String nested = extractScheme(trimmed.substring(4));
            if (nested != null) {
                return nested;
            }
        }
        return scheme;
    }

    private static boolean isAbsoluteUri(String uri) {
        if (isWindowsDriveRootedPath(uri)) {
            return false;
        }
        try {
            return new URI(uri).isAbsolute();
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static boolean isRemoteBase(String uri) {
        final String trimmed = uri.trim();
        if (trimmed.startsWith("//")) {
            return true;
        }
        final String scheme = extractScheme(trimmed);
        if (scheme == null) {
            return false;
        }
        if ("file".equals(scheme)) {
            return !isLocalFileUri(trimmed);
        }
        if ("urn".equals(scheme)) {
            return false;
        }
        if ("jar".equals(scheme)) {
            final String nested = extractScheme(trimmed.substring(4));
            return !"file".equals(nested) || !isLocalFileUri(trimmed.substring(4));
        }
        return true;
    }

    private static boolean isLocalFileUri(String uri) {
        try {
            URI parsed = new URI(uri);
            final String authority = parsed.getAuthority();
            return "file".equalsIgnoreCase(parsed.getScheme())
                && (authority == null || authority.length() == 0 || "localhost".equalsIgnoreCase(authority));
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static boolean isNetworkScheme(String scheme) {
        return "http".equals(scheme) || "https".equals(scheme) || "ftp".equals(scheme);
    }

    private static String extractScheme(String uri) {
        final String trimmed = uri.trim();
        final int colon = trimmed.indexOf(':');
        if (colon <= 1 || !isAsciiLetter(trimmed.charAt(0))) {
            return null;
        }
        for (int i = 1; i < colon; i++) {
            final char c = trimmed.charAt(i);
            if (!isAsciiLetter(c) && !(c >= '0' && c <= '9') && c != '+' && c != '-' && c != '.') {
                return null;
            }
        }
        return trimmed.substring(0, colon).toLowerCase(Locale.ENGLISH);
    }

    private static boolean isAsciiLetter(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }

    private static boolean isPlainRelativePath(String location) {
        if (location.startsWith("/") || location.startsWith("\\")
            || isWindowsDriveRootedPath(location)) {
            return false;
        }
        for (String segment : location.replace('\\', '/').split("/")) {
            if ("..".equals(segment)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWindowsDriveRootedPath(String location) {
        return location.length() >= 3 && isAsciiLetter(location.charAt(0))
            && location.charAt(1) == ':' && (location.charAt(2) == '/' || location.charAt(2) == '\\');
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
