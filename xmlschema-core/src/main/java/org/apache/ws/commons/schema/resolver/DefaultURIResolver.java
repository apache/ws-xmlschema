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
import java.net.HttpURLConnection;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;
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
 * <code>jar</code>, and refuses a schema location that takes a remote base URI to a local
 * (<code>file:</code> or <code>jar:</code>) scheme, names a non-local authority with the
 * <code>file:</code> scheme, or reads a <code>jar:</code> archive fetched over the network. A
 * location may move a remote base between <code>http</code> and <code>https</code> in either
 * direction; only a redirect that changes scheme is refused. A deployment with no remote schema sets can turn network
 * resolution off altogether with the {@link #ALLOW_NETWORK_PROPERTY} system property, and
 * filesystem resolution with {@link #ALLOW_FILE_SYSTEM_PROPERTY}, without supplying its own
 * resolver, and the address classes that only ever appear in an SSRF attempt are refused before
 * a remote fetch (see {@link #CHECK_ADDRESSES_PROPERTY}). Within the schemes it does allow it
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
     * How many HTTP redirects one fetch may follow. Redirects are followed by this resolver
     * rather than by the JDK, so that the chain is bounded, every hop is checked the way the
     * location the schema named was checked, and the whole chain counts against one fetch
     * deadline. Set it to <code>0</code> to refuse a redirected schema location outright.
     */
    public static final String MAX_REDIRECTS_PROPERTY =
        "org.apache.ws.commons.schema.remote.maxRedirects";

    /**
     * Whether the address a remote schema location resolves to is checked before it is fetched.
     * Defaults to <code>true</code>, which refuses the address classes that can never legitimately
     * serve a schema document: link-local (cloud metadata services live at
     * <code>169.254.169.254</code>), multicast, the wildcard address, IPv6 unique-local (which
     * includes IPv6 metadata endpoints such as <code>fd00:ec2::254</code>), and the IPv6 forms
     * that embed one of those IPv4 addresses.
     * <p>
     * Loopback and private (RFC 1918) addresses are <em>permitted</em>: a schema served from
     * localhost or an internal mirror is ordinary. This is a denylist of never-legitimate address
     * classes, not a host allowlist, and it is no defence against a hostile host at a routable
     * address — that still needs a resolver of the application's own.
     * </p>
     * <p>
     * The check is skipped when the fetch would go through an HTTP proxy, because the proxy
     * resolves the host itself and the addresses this JVM sees say nothing about where the fetch
     * lands; in such a deployment the proxy is the egress control. Set this to
     * <code>false</code> to skip it everywhere.
     * </p>
     */
    public static final String CHECK_ADDRESSES_PROPERTY =
        "org.apache.ws.commons.schema.remote.checkAddresses";

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
    private static final long DEFAULT_MAX_REDIRECTS = 5L;

    private final long connectTimeoutMillis =
        getLongProperty(CONNECT_TIMEOUT_PROPERTY, DEFAULT_CONNECT_TIMEOUT_MILLIS);
    private final long readTimeoutMillis =
        getLongProperty(READ_TIMEOUT_PROPERTY, DEFAULT_READ_TIMEOUT_MILLIS);
    private final long maxFetchMillis =
        getLongProperty(MAX_FETCH_MILLIS_PROPERTY, DEFAULT_MAX_FETCH_MILLIS);
    private final long maxBytes = getLongProperty(MAX_BYTES_PROPERTY, DEFAULT_MAX_BYTES);
    // Zero is meaningful here -- it refuses a redirect outright -- so the minimum is 0, not 1.
    private final long maxRedirects =
        getLongProperty(MAX_REDIRECTS_PROPERTY, DEFAULT_MAX_REDIRECTS, 0L);
    private final boolean allowNetwork = getBooleanProperty(ALLOW_NETWORK_PROPERTY, true);
    private final boolean allowFileSystem = getBooleanProperty(ALLOW_FILE_SYSTEM_PROPERTY, true);
    private final boolean checkAddresses = getBooleanProperty(CHECK_ADDRESSES_PROPERTY, true);

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
            // The parser opens this against the working directory, so it is as much a local read
            // as a file: URL and needs the same check, made on the file the parser will open.
            verifyRegularFile(relativeLocationFile(schemaLocation), schemaLocation);
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
            // One deadline for the whole fetch, so a redirect chain cannot buy more time.
            deadlineNanos = System.nanoTime() + maxFetchMillis * 1000000L;
            URL target = url;
            long hops = 0;
            URLConnection connection = null;
            while (connection == null) {
                checkDeadline();
                verifyAddressPermitted(target, systemId);
                URLConnection candidate = target.openConnection();
                candidate.setDoInput(true);
                candidate.setConnectTimeout(toIntMillis(connectTimeoutMillis));
                candidate.setReadTimeout(toIntMillis(readTimeoutMillis));
                String redirectedTo = null;
                if (candidate instanceof HttpURLConnection) {
                    HttpURLConnection http = (HttpURLConnection)candidate;
                    // Followed here rather than by the JDK: that bounds the chain, puts every hop
                    // through the same checks as the location the schema named, and keeps the
                    // whole chain inside one deadline.
                    http.setInstanceFollowRedirects(false);
                    if (isRedirect(http.getResponseCode())) {
                        redirectedTo = http.getHeaderField("Location");
                        http.disconnect();
                        if (hops >= maxRedirects) {
                            throw new IOException("The schema location \"" + systemId
                                                  + "\" redirected more than " + maxRedirects
                                                  + " times, the maximum set by "
                                                  + MAX_REDIRECTS_PROPERTY + ".");
                        }
                        hops++;
                    }
                }
                if (redirectedTo == null) {
                    connection = candidate;
                } else {
                    target = nextHop(target, redirectedTo);
                }
            }
            try {
                // A declared length is a courtesy: it is absent for a chunked response and is in
                // any case whatever the host chose to claim. The running count below is the real
                // limit.
                if (connection.getContentLengthLong() > maxBytes) {
                    throw new IOException("The schema location \"" + systemId
                                          + "\" declared a length above the maximum of "
                                          + maxBytes + " bytes.");
                }
                delegate = connection.getInputStream();
            } catch (IOException | RuntimeException e) {
                // Nothing else holds the connection, so close it here: left alone, its socket
                // stays open until the connection is garbage collected.
                if (connection instanceof HttpURLConnection) {
                    ((HttpURLConnection)connection).disconnect();
                }
                throw e;
            }
        }

        private boolean isRedirect(int code) {
            return code == HttpURLConnection.HTTP_MOVED_PERM
                || code == HttpURLConnection.HTTP_MOVED_TEMP
                || code == HttpURLConnection.HTTP_SEE_OTHER
                || code == 307
                || code == 308;
        }

        /**
         * The next URL in a redirect chain, or an exception if it is one this resolver will not
         * fetch. A redirect that changes scheme is refused, which is what the JDK does when it
         * follows redirects itself, so an http location cannot become a file read or an https one
         * be downgraded.
         */
        private URL nextHop(URL from, String location) throws IOException {
            if (location == null || location.trim().length() == 0) {
                throw new IOException("The schema location \"" + systemId
                                      + "\" redirected without saying where to.");
            }
            URL next;
            try {
                next = new URL(from, location.trim());
            } catch (MalformedURLException e) {
                throw new IOException("The schema location \"" + systemId
                                      + "\" redirected to \"" + location.trim()
                                      + "\", which is not a usable URL.", e);
            }
            if (!next.getProtocol().equalsIgnoreCase(from.getProtocol())) {
                throw new IOException("The schema location \"" + systemId
                                      + "\" redirected from the scheme \"" + from.getProtocol()
                                      + "\" to \"" + next.getProtocol() + "\".");
            }
            try {
                verifyPermittedLocation(next.toString(), systemId);
            } catch (XmlSchemaException e) {
                // Surface it as an IOException: this runs inside a read(), where the parser
                // expects I/O failures.
                throw new IOException(e.getMessage(), e);
            }
            return next;
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
        return getLongProperty(name, defaultValue, 1L);
    }

    private static long getLongProperty(final String name, long defaultValue, long minimum) {
        try {
            String value = AccessController.doPrivileged(new PrivilegedAction<String>() {
                public String run() {
                    return System.getProperty(name);
                }
            });
            if (value != null && value.trim().length() > 0) {
                long parsed = Long.parseLong(value.trim());
                if (parsed >= minimum) {
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
        if ("file".equals(scheme)) {
            if (!isLocalFileUri(archive)) {
                throw new XmlSchemaException("The schema location \"" + schemaLocation
                                             + "\" resolves to a file URL with a non-local"
                                             + " authority.");
            }
            verifyRegularFile(archive, schemaLocation);
        }
    }

    /**
     * Refuse a local location that exists but is not a regular file. A remote fetch is bounded in
     * time and bytes; a local read is handed to the parser as a system id and is bounded by
     * nothing, so a named pipe holds the parsing thread for as long as nothing writes to it. A
     * schema document is a regular file, so requiring one costs nothing and also puts directories,
     * character devices and sockets out of reach.
     * <p>
     * A location that does not exist is left alone: that is an ordinary missing-schema error and
     * the parser reports it as it always has.
     * </p>
     *
     * @param uri the resolved location, or for a <code>jar:</code> URL the archive it names.
     * @param schemaLocation the original schema location, for the error message.
     */
    private static void verifyRegularFile(String uri, String schemaLocation) {
        verifyRegularFile(toLocalFile(uri), schemaLocation);
    }

    private static void verifyRegularFile(File file, String schemaLocation) {
        if (file != null && file.exists() && !file.isFile()) {
            throw new XmlSchemaException("The schema location \"" + schemaLocation
                                         + "\" is not a regular file. A schema document cannot be"
                                         + " a directory, a device or a pipe, and reading one can"
                                         + " block the parse for as long as nothing writes to it.");
        }
    }

    /**
     * The file the parser opens for a relative location with no base URI. It resolves the location
     * as a URI against the working directory and opens the path of the result, so escapes are
     * decoded and a query or fragment is dropped: <code>pip%65.xsd</code>,
     * <code>pipe.xsd#x</code> and <code>pipe.xsd?x</code> all name the file <code>pipe.xsd</code>.
     * Taking the location as a file name instead would check a file the parser never opens.
     */
    private static File relativeLocationFile(String location) {
        try {
            final URL workingDirectory = new File("").getAbsoluteFile().toURI().toURL();
            // The parser takes a backslash in a system id as a path separator.
            final URL resolved = new URL(workingDirectory, location.replace('\\', '/'));
            // URLDecoder would read '+' as a space, which a URL path does not.
            return new File(URLDecoder.decode(resolved.getPath().replace("+", "%2B"), "UTF-8"));
        } catch (IOException | IllegalArgumentException e) {
            // A malformed escape: the parser cannot open the location either.
            return new File(location);
        }
    }

    /**
     * The local file a <code>file:</code> URL names, or <code>null</code> if it cannot be mapped to
     * one. Null means the check above does not apply rather than that the location is safe; the
     * scheme and authority rules have already run.
     */
    private static File toLocalFile(String uri) {
        final URI parsed;
        try {
            parsed = new URI(uri.trim());
        } catch (URISyntaxException e) {
            return null;
        }
        if (parsed.isOpaque()) {
            // "file:x", with no slash after the scheme, has no path as a URI, but the JDK opens it
            // as x in the working directory, as it does a relative location with no base.
            return relativeLocationFile(parsed.getRawSchemeSpecificPart());
        }
        try {
            return new File(parsed);
        } catch (IllegalArgumentException e) {
            // File(URI) refuses an authority, even "localhost", which isLocalFileUri allows. The
            // path is the part that names the file.
            final String path = parsed.getPath();
            return path == null || path.length() == 0 ? null : new File(path);
        }
    }

    /**
     * Refuse a target whose address belongs to a class that can never legitimately serve a schema
     * document. Called for the location the schema named and again for every redirect hop, so the
     * document that is fetched is one this check has passed.
     *
     * @param target the URL about to be opened.
     * @param systemId the location the schema named, for the error message.
     * @throws IOException if the address is refused, or the host cannot be resolved.
     */
    private void verifyAddressPermitted(URL target, String systemId) throws IOException {
        if (!checkAddresses || usesProxy(target)) {
            return;
        }
        final String host = target.getHost();
        if (host == null || host.length() == 0) {
            return;
        }
        final InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new IOException("The schema location \"" + systemId + "\" names the host \""
                                  + host + "\", which could not be resolved.", e);
        }
        // Every address the name answers with, so a multi-record answer cannot slip one past.
        for (InetAddress address : addresses) {
            if (isForbiddenAddress(address)) {
                throw new IOException("The schema location \"" + systemId + "\" resolves to "
                                      + address.getHostAddress()
                                      + ", an address class this resolver will not fetch"
                                      + " (link-local, multicast, wildcard, IPv6 unique-local, or"
                                      + " an IPv6 form embedding one). Set "
                                      + CHECK_ADDRESSES_PROPERTY + "=false to skip this check.");
            }
        }
    }

    /**
     * Whether a fetch of this URL would go through a proxy, in which case the proxy resolves the
     * host and the addresses seen here do not describe where the fetch lands.
     */
    private static boolean usesProxy(URL target) {
        final ProxySelector selector = ProxySelector.getDefault();
        if (selector == null) {
            return false;
        }
        try {
            final List<Proxy> proxies = selector.select(target.toURI());
            if (proxies != null) {
                for (Proxy proxy : proxies) {
                    if (proxy.type() != Proxy.Type.DIRECT) {
                        return true;
                    }
                }
            }
        } catch (URISyntaxException e) {
            return false;
        } catch (RuntimeException e) {
            return false;
        }
        return false;
    }

    /**
     * Whether an address belongs to a class the resolver must never connect to. Package-private so
     * a test can reach it without a network. Loopback and RFC 1918 are deliberately absent: those
     * are where an internal schema mirror or a local test server lives.
     */
    static boolean isForbiddenAddress(InetAddress address) {
        if (address.isLinkLocalAddress() || address.isMulticastAddress()
            || address.isAnyLocalAddress()) {
            return true;
        }
        if (address instanceof Inet6Address) {
            final byte[] bytes = address.getAddress();
            // IPv6 unique-local, fd00::/7 (RFC 4193). No JDK predicate matches it, yet it holds
            // metadata endpoints such as the AWS IMDS IPv6 address fd00:ec2::254 - the same class
            // the link-local rejection exists for.
            if ((bytes[0] & 0xfe) == 0xfc) {
                return true;
            }
            // An IPv6 address that embeds an IPv4 one - the NAT64 well-known prefix 64:ff9b::/96
            // (RFC 6052) or an IPv4-mapped ::ffff:0:0/96 - is classified by the IPv4 address the
            // gateway would deliver to.
            final InetAddress embedded = embeddedIpv4(bytes);
            if (embedded != null && isForbiddenAddress(embedded)) {
                return true;
            }
        }
        return false;
    }

    private static InetAddress embeddedIpv4(byte[] bytes) {
        if (bytes.length != 16) {
            return null;
        }
        boolean nat64 = bytes[0] == 0x00 && bytes[1] == 0x64
            && bytes[2] == (byte)0xff && bytes[3] == (byte)0x9b;
        for (int i = 4; nat64 && i < 12; i++) {
            nat64 = bytes[i] == 0x00;
        }
        boolean mapped = true;
        for (int i = 0; mapped && i < 10; i++) {
            mapped = bytes[i] == 0x00;
        }
        mapped = mapped && bytes[10] == (byte)0xff && bytes[11] == (byte)0xff;
        if (!nat64 && !mapped) {
            return null;
        }
        try {
            return InetAddress.getByAddress(
                new byte[] {bytes[12], bytes[13], bytes[14], bytes[15]});
        } catch (UnknownHostException e) {
            // Cannot happen for four bytes; if it ever does, treat the address as unclassifiable.
            return null;
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
