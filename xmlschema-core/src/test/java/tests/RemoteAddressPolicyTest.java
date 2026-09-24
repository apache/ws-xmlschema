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

import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaException;
import org.apache.ws.commons.schema.resolver.DefaultURIResolver;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * A schema location must not be able to reach an address class that only ever appears in an SSRF
 * attempt: a cloud metadata service, a multicast group, the wildcard address. Loopback and
 * RFC 1918 stay reachable, because that is where a local test server or an internal schema mirror
 * lives.
 */
public class RemoteAddressPolicyTest extends Assert {

    private ServerSocket server;
    private volatile boolean running;

    @After
    public void stopServer() throws IOException {
        running = false;
        if (server != null) {
            server.close();
        }
    }

    private String importing(String location) {
        return "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:a\">"
            + "<xs:import namespace=\"urn:b\" schemaLocation=\"" + location + "\"/>"
            + "</xs:schema>";
    }

    private String refusalFor(String location) {
        try {
            new XmlSchemaCollection().read(new StringReader(importing(location)));
            return null;
        } catch (XmlSchemaException expected) {
            return expected.getMessage() == null ? "" : expected.getMessage();
        }
    }

    private void assertAddressRefused(String location) {
        String message = refusalFor(location);
        assertNotNull("expected " + location + " to be refused", message);
        assertTrue("refused, but not for its address: " + message,
                   message.contains("an address class this resolver will not fetch"));
    }

    @Test(timeout = 60000)
    public void testLinkLocalIsRefused() {
        // The address the report's proof of concept used to reach IAM credentials.
        assertAddressRefused("http://169.254.169.254/latest/meta-data/");
    }

    @Test(timeout = 60000)
    public void testMulticastAndWildcardAreRefused() {
        assertAddressRefused("http://224.0.0.1/x.xsd");
        assertAddressRefused("http://0.0.0.0/x.xsd");
    }

    @Test(timeout = 60000)
    public void testIpv6UniqueLocalIsRefused() {
        // No JDK predicate matches fd00::/7, and cloud metadata lives there too.
        assertAddressRefused("http://[fd00:ec2::254]/latest/meta-data/");
    }

    @Test(timeout = 60000)
    public void testIpv6FormsEmbeddingAForbiddenIpv4AreRefused() {
        assertAddressRefused("http://[::ffff:169.254.169.254]/latest/meta-data/");
        assertAddressRefused("http://[64:ff9b::a9fe:a9fe]/latest/meta-data/");
    }

    /** Loopback must stay reachable: it is where a local schema server lives. */
    @Test(timeout = 60000)
    public void testLoopbackIsNotRefusedForItsAddress() throws IOException {
        startServer(true);
        XmlSchemaCollection collection = new XmlSchemaCollection();
        collection.read(new StringReader(importing(
            "http://127.0.0.1:" + server.getLocalPort() + "/b.xsd")));
        assertNotNull(collection.schemaForNamespace("urn:b"));
    }

    /** A redirect may not reach an address the schema could not have named directly. */
    @Test(timeout = 60000)
    public void testRedirectToAForbiddenAddressIsRefused() throws IOException {
        startServer(false);
        String message = refusalFor("http://127.0.0.1:" + server.getLocalPort() + "/b.xsd");
        assertNotNull("expected the redirected fetch to be refused", message);
        assertTrue("refused, but not for the hop's address: " + message,
                   message.contains("an address class this resolver will not fetch"));
    }

    @Test(timeout = 60000)
    public void testCheckCanBeTurnedOff() {
        System.setProperty(DefaultURIResolver.CHECK_ADDRESSES_PROPERTY, "false");
        try {
            String message = refusalFor("http://169.254.169.254/latest/meta-data/");
            // It will still fail - nothing answers - but not for the address.
            if (message != null) {
                assertFalse("the check should have been skipped: " + message,
                            message.contains("an address class this resolver will not fetch"));
            }
        } finally {
            System.clearProperty(DefaultURIResolver.CHECK_ADDRESSES_PROPERTY);
        }
    }

    /**
     * @param serveSchema true to answer with a schema, false to redirect to a link-local address.
     */
    private void startServer(final boolean serveSchema) throws IOException {
        server = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
        running = true;
        Thread thread = new Thread(new Runnable() {
            public void run() {
                while (running) {
                    try {
                        Socket socket = server.accept();
                        serve(socket, serveSchema);
                    } catch (IOException e) {
                        return;
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void serve(Socket socket, boolean serveSchema) throws IOException {
        String body = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\""
            + " targetNamespace=\"urn:b\"/>";
        String response = serveSchema
            ? "HTTP/1.1 200 OK\r\nContent-Type: text/xml\r\nContent-Length: "
                + body.getBytes(StandardCharsets.UTF_8).length
                + "\r\nConnection: close\r\n\r\n" + body
            : "HTTP/1.1 302 Found\r\nLocation: http://169.254.169.254/latest/meta-data/"
                + "\r\nContent-Length: 0\r\nConnection: close\r\n\r\n";
        OutputStream out = socket.getOutputStream();
        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.flush();
        socket.close();
    }
}
