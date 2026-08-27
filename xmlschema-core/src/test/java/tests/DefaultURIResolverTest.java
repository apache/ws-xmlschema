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
}
