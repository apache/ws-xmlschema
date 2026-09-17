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
package tests;

import java.io.File;

import org.apache.ws.commons.schema.XmlSchemaException;
import org.apache.ws.commons.schema.resolver.DefaultURIResolver;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.InputSource;

public class DefaultURIResolverTest extends Assert {

    private static File existingDirectory() {
        return new File(System.getProperty("java.io.tmpdir"));
    }

    @Test
    public void testRemoteBaseIsNotRebasedOntoCollectionBase() {
        DefaultURIResolver resolver = new DefaultURIResolver();
        resolver.setCollectionBaseURI(existingDirectory().getAbsolutePath());

        InputSource result = resolver.resolveEntity("urn:x", "sub/x.xsd",
                                                    "http://example.com/dir/remote.xsd");

        assertEquals("http://example.com/dir/sub/x.xsd", result.getSystemId());
    }

    @Test
    public void testAbsoluteFileLocationOnRemoteBaseIsRefused() {
        DefaultURIResolver resolver = new DefaultURIResolver();

        try {
            resolver.resolveEntity("urn:x", "file:///etc/passwd",
                                   "http://example.com/dir/remote.xsd");
            fail("An absolute file URL from a remote base must be refused.");
        } catch (XmlSchemaException expected) {
            // expected
        }
    }

    @Test
    public void testNonLocalFileAuthorityCannotTriggerCollectionRebase() {
        DefaultURIResolver resolver = new DefaultURIResolver();
        resolver.setCollectionBaseURI(existingDirectory().getAbsolutePath());

        try {
            InputSource result = resolver.resolveEntity("urn:x", "../secret.xsd",
                                                        "file://attacker.example/share/base.xsd");
            assertFalse(result.getSystemId().startsWith("file:"));
        } catch (XmlSchemaException expected) {
            // expected
        }
    }

    @Test
    public void testJarFileAuthorityCannotTriggerCollectionRebase() {
        DefaultURIResolver resolver = new DefaultURIResolver();
        resolver.setCollectionBaseURI(existingDirectory().getAbsolutePath());

        try {
            InputSource result = resolver.resolveEntity("urn:x", "../secret.xsd",
                                                        "jar:file://attacker.example/share/a.jar!/dir/base.xsd");
            assertFalse(result.getSystemId().startsWith("file:"));
        } catch (XmlSchemaException expected) {
            // expected
        }
    }

    @Test
    public void testJarFileBaseStillResolvesInsideJar() {
        DefaultURIResolver resolver = new DefaultURIResolver();
        String jarBase = "jar:" + new File(existingDirectory(), "x.zip").toURI()
            + "!/dir/base.xsd";

        InputSource result = resolver.resolveEntity("urn:x", "child.xsd", jarBase);

        assertTrue(result.getSystemId().startsWith("jar:file:"));
        assertTrue(result.getSystemId().endsWith("/dir/child.xsd"));
    }

    @Test
    public void testRootedWindowsLocationWithoutBaseIsRefused() {
        DefaultURIResolver resolver = new DefaultURIResolver();

        assertNull(resolver.resolveEntity("urn:x", "C:\\Windows\\System32\\config\\SAM", null));
        assertNull(resolver.resolveEntity("urn:x", "C:/Windows/System32/config/SAM", null));
    }

    @Test
    public void testPlainRelativeLocationWithoutBaseKeepsLegacyBehavior() {
        DefaultURIResolver resolver = new DefaultURIResolver();

        InputSource result = resolver.resolveEntity("urn:x", "sub/x.xsd", null);

        assertEquals("sub/x.xsd", result.getSystemId());
    }

    @Test
    public void testOpaqueBaseStillFallsBackToCollectionBase() {
        DefaultURIResolver resolver = new DefaultURIResolver();
        resolver.setCollectionBaseURI(existingDirectory().getAbsolutePath());

        InputSource result = resolver.resolveEntity("urn:x", "imported.xsd", "urn:schemas0");

        assertTrue(result.getSystemId().startsWith("file:"));
        assertTrue(result.getSystemId().endsWith("imported.xsd"));
    }

    private static String localBase() {
        return new File(existingDirectory(), "base.xsd").toURI().toString();
    }

    private static void assertSchemeRefused(String schemaLocation, String baseUri) {
        assertSchemeRefused(schemaLocation, baseUri, "not permitted");
    }

    private static void assertSchemeRefused(String schemaLocation, String baseUri,
                                            String expectedMessageFragment) {
        DefaultURIResolver resolver = new DefaultURIResolver();
        try {
            resolver.resolveEntity("urn:x", schemaLocation, baseUri);
            fail("The location \"" + schemaLocation + "\" must be refused.");
        } catch (XmlSchemaException expected) {
            assertTrue(expected.getMessage(),
                       expected.getMessage().contains(expectedMessageFragment));
        }
    }

    @Test
    public void testDisallowedSchemesAreRefusedFromALocalBase() {
        assertSchemeRefused("mailto:someone@example.com", localBase());
        assertSchemeRefused("jrt:/java.base/java/lang/Object.class", localBase());
        assertSchemeRefused("ftp://attacker.example/x.xsd", localBase());
    }

    @Test
    public void testDisallowedSchemesAreRefusedWithoutABase() {
        assertSchemeRefused("mailto:someone@example.com", null);
        assertSchemeRefused("jrt:/java.base/java/lang/Object.class", null);
        assertSchemeRefused("ftp://attacker.example/x.xsd", null);
    }

    @Test
    public void testJarWrappingANetworkUrlIsRefusedAsThatNetworkScheme() {
        // "jar:ftp://..." reads as protocol "jar" but performs an FTP fetch, so the scheme
        // check has to look through the jar: wrapper.
        assertSchemeRefused("jar:ftp://attacker.example/a.jar!/x.xsd", localBase());
        assertSchemeRefused("jar:ftp://attacker.example/a.jar!/x.xsd", null);
    }

    @Test
    public void testAllowedSchemesStillResolve() {
        DefaultURIResolver resolver = new DefaultURIResolver();

        assertEquals("http://example.com/x.xsd",
                     resolver.resolveEntity("urn:x", "http://example.com/x.xsd", localBase())
                         .getSystemId());
        assertEquals("https://example.com/x.xsd",
                     resolver.resolveEntity("urn:x", "https://example.com/x.xsd", null)
                         .getSystemId());
        assertTrue(resolver.resolveEntity("urn:x", "sibling.xsd", localBase())
                       .getSystemId().startsWith("file:"));
        assertTrue(resolver.resolveEntity("urn:x", "jar:file:///tmp/a.jar!/x.xsd", localBase())
                       .getSystemId().startsWith("jar:file:"));
    }

    @Test
    public void testPlainRelativeLocationWithoutBaseIsNotSchemeChecked() {
        DefaultURIResolver resolver = new DefaultURIResolver();

        assertEquals("sub/x.xsd", resolver.resolveEntity("urn:x", "sub/x.xsd", null).getSystemId());
    }

    @Test
    public void testFileUrlWithNonLocalAuthorityIsRefused() {
        // On Windows this is a UNC path, so the JVM would make an SMB connection to a host the
        // schema author chose. No base URI makes it reachable: an absolute location used to skip
        // every check but the scheme allowlist.
        assertSchemeRefused("file://attacker.example/share/x.xsd", localBase(), "non-local authority");
        assertSchemeRefused("file://attacker.example/share/x.xsd", null, "non-local authority");
        assertSchemeRefused("jar:file://attacker.example/share/a.jar!/x.xsd", localBase(),
                            "non-local authority");
        assertSchemeRefused("jar:file://attacker.example/share/a.jar!/x.xsd", null,
                            "non-local authority");
    }

    @Test
    public void testLocalFileAuthoritiesAreStillAccepted() {
        DefaultURIResolver resolver = new DefaultURIResolver();

        assertEquals("file:///local/x.xsd",
                     resolver.resolveEntity("urn:x", "file:///local/x.xsd", null).getSystemId());
        assertEquals("file://localhost/local/x.xsd",
                     resolver.resolveEntity("urn:x", "file://localhost/local/x.xsd", null)
                         .getSystemId());
    }

    @Test
    public void testJarOverTheNetworkIsRefused() {
        // JarURLConnection would fetch and cache the whole remote archive, and the URL reads as
        // protocol "jar" rather than "http".
        for (String location : new String[] {"jar:http://attacker.example/a.jar!/x.xsd",
                                             "jar:https://attacker.example/a.jar!/x.xsd"}) {
            assertSchemeRefused(location, localBase(), "over the network");
            assertSchemeRefused(location, null, "over the network");
        }
    }

    @Test
    public void testJarEntryNeedNotParseAsAUri() {
        // The archive is local; the entry after "!/" is not part of the URI that names it.
        DefaultURIResolver resolver = new DefaultURIResolver();

        InputSource result = resolver.resolveEntity("urn:x", "jar:file:///tmp/a.jar!/has space.xsd",
                                                    null);

        assertEquals("jar:file:///tmp/a.jar!/has space.xsd", result.getSystemId());
    }

    @Test
    public void testFileUrlCarryingItsHostInThePathIsRefused() {
        // "file:////host/share/x.xsd" parses with a null authority and the host in the path, so an
        // authority-only check lets it through. On Windows that path is a UNC path.
        assertSchemeRefused("file:////attacker.example/share/x.xsd", null, "non-local authority");
        assertSchemeRefused("file:////attacker.example/share/x.xsd", localBase(), "non-local authority");
        assertSchemeRefused("file://///attacker.example/share/x.xsd", null, "non-local authority");
        assertSchemeRefused("jar:file:////attacker.example/share/a.jar!/x.xsd", null,
                            "non-local authority");
        // The same shape reached by composing a relative location against a local base.
        assertSchemeRefused("////attacker.example/share/x.xsd", localBase(), "non-local authority");
    }

    @Test
    public void testSchemeCheckSurvivesALocationUriCannotParse() {
        // java.net.URI rejects a space, so isAbsoluteUri() said "not absolute" and the location
        // fell through to the relative-path branch, which returned it unchecked. java.net.URL is
        // laxer, so the parser still fetched it: a space was enough to defeat the scheme allowlist.
        assertSchemeRefused("ftp://attacker.example/a b.xsd", null);
        assertSchemeRefused("mailto:someone@example.com?x= y", null);
        assertSchemeRefused("jrt:/java.base/a b", null);
    }

    @Test
    public void testUnparseableFileLocationIsRefused() {
        DefaultURIResolver resolver = new DefaultURIResolver();
        try {
            resolver.resolveEntity("urn:x", "file:///\\\\attacker.example\\share\\x.xsd", null);
            fail("A file: location that will not parse as a URI must be refused.");
        } catch (XmlSchemaException expected) {
            // expected
        }
    }

    @Test
    public void testJarWrappingAnUnrecognisedSchemeIsRefused() {
        assertSchemeRefused("jar:fi%6Ce://attacker.example/a.jar!/x.xsd", null, "names no scheme");
        // Against a base the URL parser rejects the unknown nested protocol before this check,
        // so only require that it is refused.
        DefaultURIResolver resolver = new DefaultURIResolver();
        try {
            resolver.resolveEntity("urn:x", "jar:fi%6Ce://attacker.example/a.jar!/x.xsd",
                                   localBase());
            fail("A jar: URL wrapping an unrecognised scheme must be refused.");
        } catch (XmlSchemaException expected) {
            // expected
        }
    }

    @Test
    public void testRelativeLocationsAreStillNotSchemeChecked() {
        DefaultURIResolver resolver = new DefaultURIResolver();

        // No scheme: extractScheme() returns null for a colon that follows a path separator, so
        // these stay plain relative paths.
        assertEquals("sub/x.xsd", resolver.resolveEntity("urn:x", "sub/x.xsd", null).getSystemId());
        assertEquals("dir/a:b.xsd",
                     resolver.resolveEntity("urn:x", "dir/a:b.xsd", null).getSystemId());
    }

    /**
     * A deployment whose schema sets are all local can turn remote fetching off without writing
     * its own resolver. The property is read when the resolver is constructed, as the per-fetch
     * bounds are, so each case builds its resolver after setting it.
     */
    @Test
    public void testNetworkResolutionCanBeTurnedOff() {
        System.setProperty(DefaultURIResolver.ALLOW_NETWORK_PROPERTY, "false");
        try {
            DefaultURIResolver resolver = new DefaultURIResolver();
            for (String location : new String[] {"http://example.com/x.xsd",
                                                 "https://example.com/x.xsd"}) {
                for (String base : new String[] {null, localBase()}) {
                    try {
                        resolver.resolveEntity("urn:x", location, base);
                        fail("network resolution is off, so \"" + location
                             + "\" must be refused.");
                    } catch (XmlSchemaException expected) {
                        assertTrue(expected.getMessage(),
                                   expected.getMessage().contains("turned off"));
                    }
                }
            }
            // Local resolution is unaffected: that is the point of the switch.
            assertEquals("file:///legit/local.xsd",
                         resolver.resolveEntity("urn:x", "file:///legit/local.xsd", null)
                             .getSystemId());
            assertEquals("sub/x.xsd",
                         resolver.resolveEntity("urn:x", "sub/x.xsd", null).getSystemId());
            assertTrue(resolver.resolveEntity("urn:x", "jar:file:///a.jar!/x.xsd", null)
                           .getSystemId().startsWith("jar:file:"));
        } finally {
            System.clearProperty(DefaultURIResolver.ALLOW_NETWORK_PROPERTY);
        }
    }

    @Test
    public void testNetworkResolutionIsAllowedByDefault() {
        assertNull(System.getProperty(DefaultURIResolver.ALLOW_NETWORK_PROPERTY));

        assertEquals("http://example.com/x.xsd",
                     new DefaultURIResolver()
                         .resolveEntity("urn:x", "http://example.com/x.xsd", null).getSystemId());
    }

    @Test
    public void testUnparseableAllowNetworkValueLeavesResolutionOn() {
        // Boolean.parseBoolean would read this as false and quietly break the deployment.
        System.setProperty(DefaultURIResolver.ALLOW_NETWORK_PROPERTY, "no");
        try {
            assertEquals("http://example.com/x.xsd",
                         new DefaultURIResolver()
                             .resolveEntity("urn:x", "http://example.com/x.xsd", null)
                             .getSystemId());
        } finally {
            System.clearProperty(DefaultURIResolver.ALLOW_NETWORK_PROPERTY);
        }
    }

    @Test
    public void testFileSystemResolutionCanBeTurnedOff() {
        System.setProperty(DefaultURIResolver.ALLOW_FILE_SYSTEM_PROPERTY, "false");
        try {
            DefaultURIResolver resolver = new DefaultURIResolver();
            assertRefusedWith(resolver, "file:///legit/local.xsd", null, "turned off");
            assertRefusedWith(resolver, "file:///legit/local.xsd", localBase(), "turned off");
            // jar:file: is a filesystem read too; effectiveScheme sees through the wrapper.
            assertRefusedWith(resolver, "jar:file:///a.jar!/x.xsd", null, "turned off");
            // With no base a relative path is resolved against the working directory.
            assertRefusedWith(resolver, "sub/x.xsd", null, "turned off");
            // The network switch is independent: this one does not touch http.
            assertEquals("http://example.com/x.xsd",
                         resolver.resolveEntity("urn:x", "http://example.com/x.xsd", null)
                             .getSystemId());
        } finally {
            System.clearProperty(DefaultURIResolver.ALLOW_FILE_SYSTEM_PROPERTY);
        }
    }

    /** Both switches off is the closest the bundled resolver comes to refusing everything. */
    @Test
    public void testBothSwitchesOffLeaveNothingToFetch() {
        System.setProperty(DefaultURIResolver.ALLOW_FILE_SYSTEM_PROPERTY, "false");
        System.setProperty(DefaultURIResolver.ALLOW_NETWORK_PROPERTY, "false");
        try {
            DefaultURIResolver resolver = new DefaultURIResolver();
            for (String location : new String[] {"http://example.com/x.xsd",
                                                 "https://example.com/x.xsd",
                                                 "file:///legit/local.xsd",
                                                 "jar:file:///a.jar!/x.xsd",
                                                 "sub/x.xsd"}) {
                for (String base : new String[] {null, localBase()}) {
                    try {
                        resolver.resolveEntity("urn:x", location, base);
                        fail("both switches are off, so \"" + location + "\" must be refused.");
                    } catch (XmlSchemaException expected) {
                        assertTrue(expected.getMessage(),
                                   expected.getMessage().contains("turned off"));
                    }
                }
            }
        } finally {
            System.clearProperty(DefaultURIResolver.ALLOW_FILE_SYSTEM_PROPERTY);
            System.clearProperty(DefaultURIResolver.ALLOW_NETWORK_PROPERTY);
        }
    }

    @Test
    public void testFileSystemResolutionIsAllowedByDefault() {
        assertNull(System.getProperty(DefaultURIResolver.ALLOW_FILE_SYSTEM_PROPERTY));
        DefaultURIResolver resolver = new DefaultURIResolver();

        assertEquals("file:///legit/local.xsd",
                     resolver.resolveEntity("urn:x", "file:///legit/local.xsd", null).getSystemId());
        assertEquals("sub/x.xsd",
                     resolver.resolveEntity("urn:x", "sub/x.xsd", null).getSystemId());
    }

    @Test
    public void testUnparseableAllowFileSystemValueLeavesResolutionOn() {
        System.setProperty(DefaultURIResolver.ALLOW_FILE_SYSTEM_PROPERTY, "off");
        try {
            assertEquals("file:///legit/local.xsd",
                         new DefaultURIResolver()
                             .resolveEntity("urn:x", "file:///legit/local.xsd", null)
                             .getSystemId());
        } finally {
            System.clearProperty(DefaultURIResolver.ALLOW_FILE_SYSTEM_PROPERTY);
        }
    }

    private static void assertRefusedWith(DefaultURIResolver resolver, String schemaLocation,
                                          String baseUri, String expectedMessageFragment) {
        try {
            resolver.resolveEntity("urn:x", schemaLocation, baseUri);
            fail("The location \"" + schemaLocation + "\" must be refused.");
        } catch (XmlSchemaException expected) {
            assertTrue(expected.getMessage(),
                       expected.getMessage().contains(expectedMessageFragment));
        }
    }
}
