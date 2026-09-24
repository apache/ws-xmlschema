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

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaException;
import org.apache.ws.commons.schema.resolver.DefaultURIResolver;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.InputSource;

/**
 * A schema location naming a hostile host must not be able to hold the parsing thread or its
 * heap. The import depth and resolution limits bound the shape of the import graph, not the
 * cost of one fetch within it, so a single import reaches none of them.
 */
public class RemoteFetchBoundsTest extends Assert {

    private ServerSocket server;
    private volatile boolean running;

    /** Accepts, then sends nothing and never closes. */
    private static final int MODE_SILENT = 0;
    /** Sends a byte at a time forever, resetting the per-read timeout on each one. */
    private static final int MODE_TRICKLE = 1;
    /** Sends a well-formed but endless body. */
    private static final int MODE_FLOOD = 2;
    /** Redirects to a sibling path on the same server, once per connection. */
    private static final int MODE_REDIRECT = 3;
    /** Serves a small valid schema for namespace urn:b. */
    private static final int MODE_SCHEMA = 4;
    /** Redirects for ever, so the hop cap is what stops it. */
    private static final int MODE_REDIRECT_LOOP = 5;
    /** Redirects to a file: URL, changing scheme. */
    private static final int MODE_REDIRECT_TO_FILE = 6;

    private static final String SCHEMA_BODY =
        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:b\"/>";

    private void startServer(final int mode) throws IOException {
        server = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
        running = true;
        Thread thread = new Thread(new Runnable() {
            public void run() {
                while (running) {
                    try {
                        Socket socket = server.accept();
                        serve(socket, mode);
                    } catch (IOException e) {
                        return;
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void serve(Socket socket, int mode) throws IOException {
        if (mode == MODE_SILENT) {
            return;
        }
        OutputStream out = socket.getOutputStream();
        if (mode == MODE_REDIRECT || mode == MODE_SCHEMA || mode == MODE_REDIRECT_LOOP
            || mode == MODE_REDIRECT_TO_FILE) {
            out.write(oneShotResponse(mode).getBytes(StandardCharsets.UTF_8));
            out.flush();
            socket.close();
            return;
        }
        out.write("HTTP/1.1 200 OK\r\nContent-Type: text/xml\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        out.flush();
        byte[] chunk = mode == MODE_TRICKLE
            ? new byte[] {' '}
            : new byte[64 * 1024];
        while (running) {
            try {
                out.write(chunk);
                out.flush();
                if (mode == MODE_TRICKLE) {
                    Thread.sleep(50);
                }
            } catch (Exception e) {
                return;
            }
        }
    }

    private String oneShotResponse(int mode) {
        if (mode == MODE_SCHEMA) {
            return "HTTP/1.1 200 OK\r\nContent-Type: text/xml\r\nContent-Length: "
                + SCHEMA_BODY.getBytes(StandardCharsets.UTF_8).length
                + "\r\nConnection: close\r\n\r\n" + SCHEMA_BODY;
        }
        String location = mode == MODE_REDIRECT_TO_FILE
            ? "file:///etc/passwd"
            : "http://127.0.0.1:" + server.getLocalPort() + "/next.xsd";
        return "HTTP/1.1 302 Found\r\nLocation: " + location
            + "\r\nContent-Length: 0\r\nConnection: close\r\n\r\n";
    }

    /** First connection answers with {@code first}, every later one with {@code rest}. */
    private void startServer(final int first, final int rest) throws IOException {
        server = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
        running = true;
        Thread thread = new Thread(new Runnable() {
            public void run() {
                boolean isFirst = true;
                while (running) {
                    try {
                        Socket socket = server.accept();
                        serve(socket, isFirst ? first : rest);
                        isFirst = false;
                    } catch (IOException e) {
                        return;
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private String importing() {
        return "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:a\">"
            + "<xs:import namespace=\"urn:b\" schemaLocation=\"http://127.0.0.1:"
            + server.getLocalPort() + "/b.xsd\"/>"
            + "</xs:schema>";
    }

    @After
    public void stopServer() throws IOException {
        running = false;
        if (server != null) {
            server.close();
        }
    }

    @Before
    public void shortenBounds() {
        System.setProperty(DefaultURIResolver.CONNECT_TIMEOUT_PROPERTY, "2000");
        System.setProperty(DefaultURIResolver.READ_TIMEOUT_PROPERTY, "1000");
        System.setProperty(DefaultURIResolver.MAX_FETCH_MILLIS_PROPERTY, "2000");
        System.setProperty(DefaultURIResolver.MAX_BYTES_PROPERTY, "1048576");
    }

    @After
    public void restoreBounds() {
        System.clearProperty(DefaultURIResolver.CONNECT_TIMEOUT_PROPERTY);
        System.clearProperty(DefaultURIResolver.READ_TIMEOUT_PROPERTY);
        System.clearProperty(DefaultURIResolver.MAX_FETCH_MILLIS_PROPERTY);
        System.clearProperty(DefaultURIResolver.MAX_BYTES_PROPERTY);
    }

    private void assertRefusedWithin(long millis) throws IOException {
        assertRefusedWithin(millis, null);
    }

    /**
     * @param messageFragment when given, the reason the fetch was refused must mention it, so a
     *     test for one bound cannot pass because a different bound happened to fire first.
     */
    private void assertRefusedWithin(long millis, String messageFragment) throws IOException {
        long start = System.currentTimeMillis();
        try {
            new XmlSchemaCollection().read(new StringReader(importing()));
            fail("expected the fetch to be refused");
        } catch (XmlSchemaException expected) {
            long elapsed = System.currentTimeMillis() - start;
            assertTrue("refused, but only after " + elapsed + "ms", elapsed < millis);
            if (messageFragment != null) {
                assertTrue("refused for the wrong reason: " + expected.getMessage(),
                           expected.getMessage().contains(messageFragment));
            }
        }
    }

    @Test(timeout = 60000)
    public void testSilentHostDoesNotHoldTheThread() throws IOException {
        startServer(MODE_SILENT);
        assertRefusedWithin(30000);
    }

    /** The case a per-read timeout alone does not catch. */
    @Test(timeout = 60000)
    public void testTricklingHostIsCutOffAtTheTotalDeadline() throws IOException {
        startServer(MODE_TRICKLE);
        assertRefusedWithin(30000);
    }

    @Test(timeout = 60000)
    public void testOversizedResponseIsRefused() throws IOException {
        startServer(MODE_FLOOD);
        assertRefusedWithin(30000);
    }

    /**
     * A schema that has simply moved must still resolve. Bounding a fetch is not a reason to stop
     * following a redirect, and schemas do get reorganised behind one.
     */
    @Test(timeout = 60000)
    public void testMovedSchemaIsStillFollowed() throws Exception {
        startServer(MODE_REDIRECT, MODE_SCHEMA);
        XmlSchemaCollection collection = new XmlSchemaCollection();
        collection.read(new StringReader(importing()));
        assertNotNull("a schema behind a redirect must still resolve",
                      collection.schemaForNamespace("urn:b"));
    }

    @Test(timeout = 60000)
    public void testEndlessRedirectChainIsCutOffAtTheHopCap() throws Exception {
        System.setProperty(DefaultURIResolver.MAX_REDIRECTS_PROPERTY, "3");
        try {
            startServer(MODE_REDIRECT_LOOP, MODE_REDIRECT_LOOP);
            assertRefusedWithin(30000, "redirected more than 3 times");
        } finally {
            System.clearProperty(DefaultURIResolver.MAX_REDIRECTS_PROPERTY);
        }
    }

    /** Zero hops is meaningful: it refuses a redirected location outright. */
    @Test(timeout = 60000)
    public void testZeroHopsRefusesARedirect() throws Exception {
        System.setProperty(DefaultURIResolver.MAX_REDIRECTS_PROPERTY, "0");
        try {
            startServer(MODE_REDIRECT, MODE_SCHEMA);
            assertRefusedWithin(30000, "redirected more than 0 times");
        } finally {
            System.clearProperty(DefaultURIResolver.MAX_REDIRECTS_PROPERTY);
        }
    }

    /** A redirect may not turn a network fetch into a local read. */
    @Test(timeout = 60000)
    public void testRedirectChangingSchemeIsRefused() throws Exception {
        startServer(MODE_REDIRECT_TO_FILE, MODE_SCHEMA);
        assertRefusedWithin(30000, "redirected from the scheme");
    }

    /** Local schemas keep the system-id-only path: no buffering, no behaviour change. */
    @Test
    public void testLocalImportStillResolves() throws Exception {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        collection.setBaseUri(Resources.TEST_RESOURCES);
        XmlSchema schema = collection.read(new InputSource(Resources.asURI("importBase.xsd")));
        assertNotNull(schema);
        assertNotNull(collection.schemaForNamespace("http://soapinterop.org/xsd2"));
    }
}
