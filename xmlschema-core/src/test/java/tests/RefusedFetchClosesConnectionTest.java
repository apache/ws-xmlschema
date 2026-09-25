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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * A remote fetch refused before its body is read must close its connection, not leave the socket
 * open until the connection is garbage collected.
 */
public class RefusedFetchClosesConnectionTest extends Assert {

    private ServerSocket server;

    @After
    public void stopServer() throws IOException {
        if (server != null) {
            server.close();
        }
    }

    @Test(timeout = 60000)
    public void testConnectionIsClosedWhenTheDeclaredLengthIsRefused() throws Exception {
        assertConnectionClosedAfter("HTTP/1.1 200 OK\r\nContent-Length: 999999999999\r\n\r\n",
                                    "declared a length");
    }

    @Test(timeout = 60000)
    public void testConnectionIsClosedOnAnErrorResponse() throws Exception {
        assertConnectionClosedAfter("HTTP/1.1 500 Internal Server Error\r\n"
                                    + "Content-Length: 5\r\n\r\nerror", null);
    }

    private void assertConnectionClosedAfter(final String response, String messageFragment)
        throws Exception {
        server = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
        final boolean[] closed = new boolean[1];
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try (Socket socket = server.accept()) {
                    readRequestHeaders(socket.getInputStream());
                    OutputStream out = socket.getOutputStream();
                    out.write(response.getBytes(StandardCharsets.ISO_8859_1));
                    out.flush();
                    // The client has nothing more to send: end of stream means it closed.
                    socket.setSoTimeout(10000);
                    closed[0] = socket.getInputStream().read() == -1;
                } catch (SocketTimeoutException e) {
                    closed[0] = false;
                } catch (IOException e) {
                    // A reset also means the client closed its end.
                    closed[0] = true;
                }
            }
        });
        thread.start();

        String location = "http://127.0.0.1:" + server.getLocalPort() + "/x.xsd";
        try {
            new XmlSchemaCollection().read(new StringReader(
                "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
                + "<xs:import namespace=\"urn:x\" schemaLocation=\"" + location + "\"/>"
                + "</xs:schema>"));
            fail("The fetch of " + location + " should be refused.");
        } catch (XmlSchemaException expected) {
            if (messageFragment != null) {
                assertTrue(expected.getMessage(), expected.getMessage().contains(messageFragment));
            }
        }
        thread.join();
        assertTrue("The connection to " + location + " was left open.", closed[0]);
    }

    private static void readRequestHeaders(InputStream in) throws IOException {
        int matched = 0;
        final byte[] end = {'\r', '\n', '\r', '\n'};
        while (matched < end.length) {
            int b = in.read();
            if (b < 0) {
                return;
            }
            matched = b == end[matched] ? matched + 1 : (b == '\r' ? 1 : 0);
        }
    }
}
