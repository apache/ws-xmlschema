<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Security Policy

## Reporting a Vulnerability

`apache/ws-xmlschema` follows the [Apache Software Foundation security process](https://www.apache.org/security/). Please report suspected
vulnerabilities privately to `security@apache.org`; do not open public
GitHub issues or pull requests for security reports.

## Threat Model

What the project treats as in scope and out of scope, the security
properties it provides and disclaims, the adversary model, and how
findings are triaged are documented in [THREAT-MODEL.md](./THREAT-MODEL.md).

## Parsing untrusted schema documents

`XmlSchemaCollection` follows `xs:import` / `xs:include` /
`xs:redefine` schema locations through a `URIResolver`. The bundled
`DefaultURIResolver` restricts the URI schemes it will resolve; refuses a
local (`file:` or `jar:`) location from a remote base, a `file:` location
naming a remote host, a `jar:` archive fetched over the network, and a
local location that is not a regular file; refuses remote addresses in
classes that never serve a schema (link-local, which includes cloud
metadata endpoints, multicast, wildcard and IPv6 unique-local; see
`org.apache.ws.commons.schema.remote.checkAddresses`, which is skipped when
a proxy carries the fetch); and bounds each remote fetch in time, bytes and
redirects. A location may move a remote base between `http` and `https`,
in either direction, so an `https` schema can import one over plain
`http`; only a redirect that changes scheme is refused. It has no host
allowlist:
any routable host, loopback or private address, or readable local file
that a schema location names is fetched. An operator with no remote, or no
local, schema sets can turn that transport off (see README.txt). The
bundled resolver is a convenience default for trusted, operator-controlled
schema sets.

**An application that parses schema or WSDL documents from an untrusted
source must install a restricting resolver via
`XmlSchemaCollection.setSchemaResolver(...)` before calling `read(...)`.**
See [THREAT-MODEL.md](./THREAT-MODEL.md) section 10 for the full set of
downstream responsibilities.

The parser that `XmlSchemaCollection` reads schema documents with never
resolves external DTDs or external entities. It allows at most 1000 entity
expansions per document, and bounds the element depth of what it parses,
including markup that an entity expands to. That depth
limit is set by `org.apache.ws.commons.schema.maxNestingDepth`: twice its
value plus 64, which is 1088 by default. A lower `jdk.xml.maxElementDepth`
is kept (on JDK 8, 11 and 17, only when set as a system property rather
than in `jaxp.properties`), but raising `jdk.xml.maxElementDepth` alone has
no effect. The
library's resource limits and how to set them are listed in README.txt.

A report that the bundled default resolver dereferenced an attacker-supplied
`schemaLocation` is a documented property of that default, not a
vulnerability in the library (THREAT-MODEL.md section 9 and section 14
Q12). A report that one of the checks or bounds the resolver does apply,
listed above, can be bypassed *is* in scope, and should be reported through
the process above.
