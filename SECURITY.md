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
`DefaultURIResolver` restricts the URI schemes it will resolve, but
applies no host or address filtering, and is a convenience default for
trusted, operator-controlled schema sets.

**An application that parses schema or WSDL documents from an untrusted
source must install a restricting resolver via
`XmlSchemaCollection.setSchemaResolver(...)` before calling `read(...)`.**
See [THREAT-MODEL.md](./THREAT-MODEL.md) section 10 for the full set of
downstream responsibilities.

A report that the bundled default resolver dereferenced an attacker-supplied
`schemaLocation` is a documented property of that default, not a
vulnerability in the library (THREAT-MODEL.md section 9 and section 14
Q12). A report that the resolver's scheme or base-scheme checks can be
bypassed *is* in scope, and should be reported through the process above.
