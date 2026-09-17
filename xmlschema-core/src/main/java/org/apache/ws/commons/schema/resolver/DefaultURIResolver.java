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
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
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
 * archive fetched over the network. A deployment with no remote schema sets can turn network
 * resolution off altogether with the {@link #ALLOW_NETWORK_PROPERTY} system property, and
 * filesystem resolution with {@link #ALLOW_FILE_SYSTEM_PROPERTY}, without supplying its own
 * resolver. Within the schemes it does allow it
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

    /**
     * Bounds on a single network fetch. Left to the JDK, a schema location naming a slow or
     * silent host holds the parsing thread for as long as that host keeps the socket open, and
     * one that keeps sending holds as much heap as it cares to send. The import depth and
     * resolution limits bound the shape of the import graph, not the cost of one fetch within
     * it, so they never come into play: a single import is enough.
     */
    public static final String CONNECT_TIMEOUT_PROPERTY =
        "org.apache.ws.commons.schema.remote.connectTimeoutMillis";
    public static final String READ_TIMEOUT_PROPERTY =
        "org.apache.ws.commons.schema.remote.readTimeoutMillis";
    public static final String MAX_FETCH_MILLIS_PROPERTY =
        "org.apache.ws.commons.schema.remote.maxFetchMillis";
    public static final String MAX_BYTES_PROPERTY =
        "org.apache.ws.commons.schema.remote.maxBytes";

    /**
     * Whether a schema location may be fetched over the network at all. Set it to
     * <code>false</code> in a deployment whose schema sets are entirely local: an
     * <code>xs:import</code> naming an <code>http</code> or <code>https</code> location is then
     * refused instead of fetched, without the deployment having to supply its own
     * {@link URIResolver}. It defaults to <code>true</code>, which is the behaviour this resolver
     * has always had. Only "true" and "false" are recognised, so a typo leaves resolution working
     * rather than silently turning it off.
     */
    public static final String ALLOW_NETWORK_PROPERTY =
        "org.apache.ws.commons.schema.remote.allowNetwork";

    /**
     * Whether a schema location may be read from the filesystem at all. Set it to
     * <code>false</code> in a deployment whose schema documents are expected to stand alone: an
     * <code>xs:import</code> naming a <code>file:</code> location, a <code>jar:file:</code> one,
     * or a relative path with no base URI to resolve it against, is then refused instead of read.
     * It defaults to <code>true</code>. Setting it alongside
     * {@link #ALLOW_NETWORK_PROPERTY} leaves this resolver with nothing it will fetch, which is
     * the closest the bundled resolver comes to refusing every external reference; an
     * application that needs to allow some references and refuse others still wants its own
     * {@link URIResolver}. Only "true" and "false" are recognised.
     */
    public static final String ALLOW_FILE_SYSTEM_PROPERTY =
        "org.apache.ws.commons.schema.local.allowFileSystem";

    private static final long DEFAULT_CONNECT_TIMEOUT_MILLIS = 5L * 1000L;
    private static final long DEFAULT_READ_TIMEOUT_MILLIS = 10L * 1000L;
    private static final long DEFAULT_MAX_FETCH_MILLIS = 30L * 1000L;
    private static final long DEFAULT_MAX_BYTES = 64L * 1024L * 1024L;

    private final long connectTimeoutMillis =
        getLongProperty(CONNECT_TIMEOUT_PROPERTY, DEFAULT_CONNECT_TIMEOUT_MILLIS);
    private final long readTimeoutMillis =
        getLongProperty(READ_TIMEOUT_PROPERTY, DEFAULT_READ_TIMEOUT_MILLIS);
    private final long maxFetchMillis =
        getLongProperty(MAX_FETCH_MILLIS_PROPERTY, DEFAULT_MAX_FETCH_MILLIS);
    private final long maxBytes = getLongProperty(MAX_BYTES_PROPERTY, DEFAULT_MAX_BYTES);
    private final boolean allowNetwork = getBooleanProperty(ALLOW_NETWORK_PROPERTY, true);
    private final boolean allowFileSystem = getBooleanProperty(ALLOW_FILE_SYSTEM_PROPERTY, true);

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

                return toInputSource(ref, ref.toString());
            } catch (MalformedURLException e1) {
                throw new XmlSchemaException("Unable to resolve the schema location \"" + schemaLocation
                                             + "\" against the base URI \"" + baseUri + "\"", e1);
            }

        }
        // A location carrying a scheme is checked even when java.net.URI will not parse it.
        // URI is stricter than the java.net.URL the parser goes on to build, so a location URI
        // rejects is not thereby harmless.
        if (isAbsoluteUri(schemaLocation) || extractScheme(schemaLocation) != null) {
            verifyPermittedLocation(schemaLocation, schemaLocation);
            try {
                return toInputSource(new URL(schemaLocation), schemaLocation);
            } catch (MalformedURLException e) {
                return new InputSource(schemaLocation);
            }
        }
        if (isPlainRelativePath(schemaLocation)) {
            if (!allowFileSystem) {
                throw new XmlSchemaException("The schema location \"" + schemaLocation
                                             + "\" is relative and would be resolved against the"
                                             + " working directory, which "
                                             + ALLOW_FILE_SYSTEM_PROPERTY + " has turned off.");
            }
            return new InputSource(schemaLocation);
        }
        return null;

    }

    /**
     * Hands the parser an InputSource for a resolved location. A <code>file:</code> or
     * <code>jar:</code> location keeps the system-id-only form: those reads are local, and the
     * parser opens them as it always has. A network location gets a byte stream that the parser
     * opens the same way it would have, except that it is bounded - left to the JDK the fetch has
     * no timeout and no size limit.
     * <p>
     * The system id is set either way, and the stream is opened lazily on first read, so this
     * method performs no I/O: resolving a location stays a pure URL composition, as callers of
     * {@link URIResolver#resolveEntity} expect. Schema documents also carry relative
     * <code>schemaLocation</code>s resolved against the system id, so a document that arrives as
     * bytes still needs its own URL recorded or its own imports cannot be resolved.
     * </p>
     */
    private InputSource toInputSource(URL url, String systemId) {
        InputSource source = new InputSource(systemId);
        if (isNetworkScheme(url.getProtocol().toLowerCase(Locale.ENGLISH))) {
            source.setByteStream(new BoundedUrlInputStream(url, systemId));
        }
        return source;
    }

    /**
     * A stream over a remote schema document that opens on first read and enforces the per-fetch
     * bounds as it goes. The connect and read timeouts bound each blocking operation separately,
     * so they are not on their own enough: a host trickling bytes below the read-timeout interval
     * resets that timer indefinitely. The total deadline checked on every read is what bounds
     * that, and the running byte count bounds a host that simply keeps sending.
     */
    private final class BoundedUrlInputStream extends InputStream {

        private final URL url;
        private final String systemId;
        private InputStream delegate;
        private long deadlineNanos;
        private long total;
        private boolean closed;

        BoundedUrlInputStream(URL url, String systemId) {
            this.url = url;
            this.systemId = systemId;
        }

        private void ensureOpen() throws IOException {
            if (delegate != null) {
                return;
            }
            if (closed) {
                throw new IOException("The schema location \"" + systemId + "\" is closed.");
            }
            URLConnection connection = url.openConnection();
            connection.setDoInput(true);
            connection.setConnectTimeout(toIntMillis(connectTimeoutMillis));
            connection.setReadTimeout(toIntMillis(readTimeoutMillis));
            deadlineNanos = System.nanoTime() + maxFetchMillis * 1000000L;
            // A declared length is a courtesy: it is absent for a chunked response and is in any
            // case whatever the host chose to claim. The running count below is the real limit.
            if (connection.getContentLengthLong() > maxBytes) {
                throw new IOException("The schema location \"" + systemId
                                      + "\" declared a length above the maximum of "
                                      + maxBytes + " bytes.");
            }
            delegate = connection.getInputStream();
        }

        private void checkDeadline() throws IOException {
            if (System.nanoTime() - deadlineNanos >= 0) {
                throw new IOException("Fetching the schema location \"" + systemId
                                      + "\" took longer than the maximum of "
                                      + maxFetchMillis + " ms.");
            }
        }

        private int count(int read) throws IOException {
            if (read > 0) {
                total += read;
                if (total > maxBytes) {
                    throw new IOException("The schema location \"" + systemId
                                          + "\" returned more than the maximum of "
                                          + maxBytes + " bytes.");
                }
            }
            return read;
        }

        public int read() throws IOException {
            ensureOpen();
            checkDeadline();
            int value = delegate.read();
            count(value == -1 ? -1 : 1);
            return value;
        }

        public int read(byte[] buffer, int offset, int length) throws IOException {
            ensureOpen();
            checkDeadline();
            return count(delegate.read(buffer, offset, length));
        }

        public void close() throws IOException {
            closed = true;
            if (delegate != null) {
                delegate.close();
                delegate = null;
            }
        }
    }

    private static int toIntMillis(long millis) {
        return millis > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)millis;
    }

    /**
     * Reads a boolean system property. Only "true" and "false" count, so an unparseable value
     * leaves the default in place rather than being read as <code>false</code> the way
     * {@link Boolean#parseBoolean} would.
     */
    private static boolean getBooleanProperty(final String name, boolean defaultValue) {
        try {
            String value = AccessController.doPrivileged(new PrivilegedAction<String>() {
                public String run() {
                    return System.getProperty(name);
                }
            });
            if (value != null) {
                String trimmed = value.trim();
                if ("true".equalsIgnoreCase(trimmed)) {
                    return true;
                }
                if ("false".equalsIgnoreCase(trimmed)) {
                    return false;
                }
            }
        } catch (RuntimeException e) {
            // fall through to the default
        }
        return defaultValue;
    }

    private static long getLongProperty(final String name, long defaultValue) {
        try {
            String value = AccessController.doPrivileged(new PrivilegedAction<String>() {
                public String run() {
                    return System.getProperty(name);
                }
            });
            if (value != null && value.trim().length() > 0) {
                long parsed = Long.parseLong(value.trim());
                if (parsed > 0) {
                    return parsed;
                }
            }
        } catch (RuntimeException e) {
            // fall through to the default
        }
        return defaultValue;
    }

    private void verifyComposedUrl(boolean remoteBase, String originalBaseUri, URL base,
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
    private void verifyPermittedLocation(String uri, String schemaLocation) {
        final String scheme = effectiveScheme(uri);
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme)) {
            throw new XmlSchemaException("The schema location \"" + schemaLocation
                                         + "\" resolves to the scheme \"" + scheme
                                         + "\", which is not permitted by DefaultURIResolver.");
        }
        final String trimmed = uri.trim();
        final boolean wrapped = "jar".equals(extractScheme(trimmed));
        if (wrapped && extractScheme(trimmed.substring(4)) == null) {
            throw new XmlSchemaException("The schema location \"" + schemaLocation
                                         + "\" is a jar: URL whose archive names no scheme this"
                                         + " resolver recognises.");
        }
        // A jar: URL delegates to the URL of the archive; the entry after "!/" is inside it.
        final String archive = wrapped ? stripJarEntry(trimmed.substring(4)) : trimmed;
        if (wrapped && isNetworkScheme(scheme)) {
            throw new XmlSchemaException("The schema location \"" + schemaLocation
                                         + "\" reads an archive fetched over the network, \""
                                         + archive + "\", which is not permitted by"
                                         + " DefaultURIResolver.");
        }
        if (!allowNetwork && isNetworkScheme(scheme)) {
            throw new XmlSchemaException("The schema location \"" + schemaLocation
                                         + "\" would be fetched over the network, which "
                                         + ALLOW_NETWORK_PROPERTY + " has turned off.");
        }
        if (!allowFileSystem && "file".equals(scheme)) {
            throw new XmlSchemaException("The schema location \"" + schemaLocation
                                         + "\" would be read from the filesystem, which "
                                         + ALLOW_FILE_SYSTEM_PROPERTY + " has turned off.");
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
            if (!"file".equalsIgnoreCase(parsed.getScheme())) {
                return false;
            }
            final String authority = parsed.getAuthority();
            if (authority != null && authority.length() > 0
                && !"localhost".equalsIgnoreCase(authority)) {
                return false;
            }
            // A host can also arrive in the path rather than the authority: "file:////host/share"
            // parses with no authority at all, and a path beginning "//" is a UNC path on Windows.
            // No schema names a local file that way, so refuse the shape outright.
            final String path = parsed.getPath();
            return path == null || !path.startsWith("//");
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
