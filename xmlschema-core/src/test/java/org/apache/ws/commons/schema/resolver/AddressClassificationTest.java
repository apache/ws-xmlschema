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

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Assert;
import org.junit.Test;

/**
 * Which address classes {@link DefaultURIResolver} will not fetch from. In this package so the
 * classification stays package-private rather than becoming public API.
 */
public class AddressClassificationTest extends Assert {

    private void assertForbidden(String literal) throws UnknownHostException {
        assertTrue(literal + " must be refused",
                   DefaultURIResolver.isForbiddenAddress(InetAddress.getByName(literal)));
    }

    private void assertPermitted(String literal) throws UnknownHostException {
        assertFalse(literal + " must be permitted",
                    DefaultURIResolver.isForbiddenAddress(InetAddress.getByName(literal)));
    }

    @Test
    public void testNeverLegitimateClassesAreRefused() throws UnknownHostException {
        assertForbidden("169.254.169.254");        // cloud metadata
        assertForbidden("fe80::1");               // IPv6 link-local
        assertForbidden("224.0.0.1");             // multicast
        assertForbidden("ff02::1");               // IPv6 multicast
        assertForbidden("0.0.0.0");               // wildcard
        assertForbidden("::");                    // IPv6 wildcard
        assertForbidden("fd00:ec2::254");         // IPv6 unique-local, no JDK predicate matches it
        assertForbidden("fc00::1");               // the other half of fd00::/7
    }

    /** An IPv6 address embedding a forbidden IPv4 one is judged by the embedded address. */
    @Test
    public void testIpv6FormsEmbeddingAForbiddenIpv4AreRefused() throws UnknownHostException {
        assertForbidden("::ffff:169.254.169.254");  // IPv4-mapped
        assertForbidden("64:ff9b::a9fe:a9fe");      // NAT64 well-known prefix
    }

    /**
     * Loopback and RFC 1918 are permitted on purpose: a local test server and an internal schema
     * mirror both live there, and refusing them would break ordinary deployments for no gain
     * against a schema author who can name a routable address anyway.
     */
    @Test
    public void testLocalAndPrivateAddressesArePermitted() throws UnknownHostException {
        assertPermitted("127.0.0.1");
        assertPermitted("::1");
        assertPermitted("10.1.2.3");
        assertPermitted("172.16.0.1");
        assertPermitted("192.168.1.1");
        assertPermitted("93.184.216.34");
        assertPermitted("::ffff:127.0.0.1");
        assertPermitted("::ffff:10.1.2.3");
    }
}
