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
        long start = System.currentTimeMillis();
        try {
            new XmlSchemaCollection().read(new StringReader(importing()));
            fail("expected the fetch to be refused");
        } catch (XmlSchemaException expected) {
            long elapsed = System.currentTimeMillis() - start;
            assertTrue("refused, but only after " + elapsed + "ms", elapsed < millis);
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
