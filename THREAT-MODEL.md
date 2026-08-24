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

# Apache XMLSchema Security Threat Model

## §1 Header

- **Project**: Apache XMLSchema — *"a lightweight schema object model
  that can be used to manipulate and generate XML schema
  representations"* *(documented: `README.txt`)*.
- **Repository**: `apache/ws-xmlschema`.
- **Version / commit**: this model is based on the default branch
  at clone time. A report against project release *N* should be triaged
  against the model as it stood at *N*, not at HEAD. Latest release
  documented in `RELEASE-NOTE.txt`: 2.3.0.
- **Date**: 2026-05-30.
- **Authors**: ASF Security team, awaiting XMLSchema / Webservices
  PMC review.
- **Status**: under maintainer review.
- **Reporting**: vulnerabilities that fall under §8 (claimed
  properties) should be reported per the Apache Security Team disclosure
  channel (<https://www.apache.org/security/>); reports that fall under
  §3 (out of scope) or §9 (properties not provided) will be closed by
  XMLSchema triagers citing this document. The project does not ship
  an in-repo `SECURITY.md` at assessment time *(inferred — §14 Q1)*.
- **Provenance legend** —
  *(documented)* = drawn from in-repo docs / source comments / project
  website with citation;
  *(maintainer)* = stated by an XMLSchema maintainer in response to this
  threat model;
  *(inferred)* = synthesized by the producer from code structure or
  domain knowledge, awaiting PMC ratification (every *(inferred)* tag has
  a matching §14 question).
- **Model confidence**: 21 documented / 0 maintainer / 24 inferred.

XMLSchema is a Java library that parses, models, walks, and serializes
W3C XML Schema documents (`.xsd` files). It is *not* a document
validator at its core (despite the `xmlschema-walker` providing some
validation helpers): the primary product is the in-memory object model
of a schema, and the schema-imports / schema-includes resolver that lets
a `<xs:include>` or `<xs:import>` pull in additional schemas from a
URI. XMLSchema is consumed primarily by SOAP stacks (Apache CXF, Apache
Axis) and other code-generation / data-binding tooling. It is bundled
into runtimes that ingest WSDLs and policy documents.

## §2 Scope and intended use

### Intended use

- In-process parsing and manipulation of W3C XML Schema documents in
  Java, primarily as part of a tooling pipeline (WSDL import,
  code generation, data-binding) or as part of a SOAP stack at runtime
  *(documented: `README.txt`)*.
- Three modules ship: `xmlschema-core` (the object model + parser +
  serializer + URI resolver), `xmlschema-walker` (visitor-style schema
  walking + a `XmlSchemaElementValidator` used for tooling), and
  `xmlschema-bundle-test` (an OSGi bundle integration test). The
  `w3c-testcases` directory holds the W3C test suite for regression
  testing only.

### Deployment shape

XMLSchema is an in-process Java library. There is no daemon, no
network listener, no CLI. The threat model is therefore that of an
**XML-parsing library that performs caller-directed network or
filesystem IO** when it follows `<xs:include>` / `<xs:import>` /
`schemaLocation` references *(inferred — §14 Q2)*.

### Caller roles

| Role | Trust level | Notes |
| --- | --- | --- |
| **Embedding Java application** | trusted | Calls `XmlSchemaCollection.read(...)`, decides whether the input stream came from disk, classpath, or wire; decides which `URIResolver` to plug in. |
| **`URIResolver` implementation** | trusted | Either the bundled `DefaultURIResolver` (which constructs a `URL` from the parent schema's base URI + the `schemaLocation` and returns an `InputSource` pointing at it) or a caller-supplied resolver *(documented: `xmlschema-core/src/main/java/.../resolver/DefaultURIResolver.java`)*. A hostile resolver is out of model. |
| **`ExtensionRegistry` implementation** | trusted | Pluggable via system property `org.apache.ws.commons.schema.extension_registry` *(documented: `xmlschema-core/src/main/java/org/apache/ws/commons/schema/XmlSchemaCollection.java` line 361)*. |
| **Producer of the schema bytes** (`Reader`, `InputStream`, `InputSource`, `Source`, `Document`, `Element`) | **variable** — see §6 trust table | The *only* attacker-controllable input position; in many embeddings the schema bytes come from a WSDL fetched off the wire. |
| **Producer of imported / included schemas** (resolved by the `URIResolver`) | **variable** — typically as untrusted as the parent schema, but can be a *different* origin if the parent's `<xs:import schemaLocation="http://attacker/evil.xsd">` points elsewhere | Following an `xs:import` is a **second, possibly cross-origin, fetch**. This is the principal SSRF surface. |
| **JDK XML platform** (`DocumentBuilderFactory`, `TransformerFactory`, `SchemaFactory`) | trusted upstream | XMLSchema sets `FEATURE_SECURE_PROCESSING=true` on the factories it constructs but does **not** set `disallow-doctype-decl` or unset external-entity properties explicitly *(documented: `XmlSchemaCollection.java` line 713, `XmlSchema.java` line 886, `DomBuilderFromSax.java` line 81)*. |

### Component-family table

| Family | Representative entry point | Touches outside the process? | In-model? |
| --- | --- | --- | --- |
| `xmlschema-core` object model — `XmlSchema*` types, `XmlSchemaCollection`, `SchemaBuilder` | `XmlSchemaCollection.read(InputSource, ...)` and overloads | **no** for in-memory model; **yes** when the URI resolver follows `xs:import`/`xs:include` | **yes** |
| `xmlschema-core` URI resolver — `DefaultURIResolver`, `CollectionURIResolver`, `URIResolver` | `XmlSchemaCollection.setSchemaResolver()` | **yes** — by design, fetches imported schemas | **yes** |
| `xmlschema-core` serializer — `XmlSchemaSerializer` | `XmlSchema.write(OutputStream)` | **no** | **yes** |
| `xmlschema-core` resource loader (internal) — `DocumentBuilderFactory` and `TransformerFactory` instances configured with `FEATURE_SECURE_PROCESSING=true` | invoked by `XmlSchemaCollection.read(InputSource, ...)` and `XmlSchemaSerializer` | **no** | **yes** |
| `xmlschema-walker` visitor — `XmlSchemaWalker`, `XmlSchemaVisitor`, `XmlSchemaScope` | walked by a caller-supplied `XmlSchemaVisitor` | **no** | **yes** |
| `xmlschema-walker` element validator — `XmlSchemaElementValidator` | walked at SAX-event time; consults the schema model | **no** (operates on caller-supplied SAX events) | **yes** |
| `xmlschema-walker` document path finder — `XmlSchemaPathFinder` | walked at SAX-event time; matches events against the schema model | **no** (operates on caller-supplied SAX events) | **yes** |
| `xmlschema-walker` DOM-from-SAX helper — `DomBuilderFromSax` | builds a DOM out of SAX events for validator inspection | **no** | **yes** |
| `xmlschema-bundle-test` | OSGi packaging regression test | n/a | **out of model** *(§3)* |
| `w3c-testcases/` | W3C schema test data | n/a | **out of model** *(§3)* |

A finding is in-model only if it reaches a row marked **yes**.

## §3 Out of scope (explicit non-goals)

1. **XML *document* validation against a schema.** XMLSchema is a
   *schema* model; the only validation-like artifact is
   `xmlschema-walker`'s `XmlSchemaElementValidator` used in tooling
   contexts. Findings that depend on the JDK `Validator` /
   `SchemaFactory` behavior or on third-party validators built on
   XMLSchema's model are out of model *(inferred — §14 Q3)*. →
   `OUT-OF-MODEL: out-of-layer`.
2. **A SOAP / WSDL parser.** XMLSchema parses the *embedded* `<schema>`
   element of a WSDL, but the WSDL parser itself is upstream (e.g.
   Apache WSDL4J, Apache CXF). → `OUT-OF-MODEL: out-of-layer`.
3. **`SchemaFactory` of the JDK.** XMLSchema does not wrap or replace
   the JDK's `javax.xml.validation.SchemaFactory`. The two are different
   abstractions *(inferred — §14 Q3)*. → `OUT-OF-MODEL: out-of-layer`.
4. **`xmlschema-bundle-test` and `w3c-testcases/`.** Integration test
   harness and W3C regression data. → `OUT-OF-MODEL: unsupported-component`.
5. **The XML parser bytes-to-DOM step when the caller provides a
   pre-parsed `Document` / `Element`.** `XmlSchemaCollection.read(Document)`
   and `.read(Element)` accept a caller-parsed DOM; if the caller's
   `DocumentBuilderFactory` is not hardened, XMLSchema does not
   retroactively secure it *(inferred — §14 Q4)*. →
   `OUT-OF-MODEL: trusted-input`.
6. **The remote host reached when following an `xs:include` /
   `xs:import`.** The URI is in the input schema; XMLSchema follows it.
   What the remote host returns is data XMLSchema then parses, but the
   *integrity* of the remote host is not WSS4J's concern *(inferred —
   §14 Q5)*. → `OUT-OF-MODEL: adversary-not-in-scope` for malicious
   *remote* peer; `VALID` if XMLSchema is tricked into the fetch in the
   first place by a path the §7 model permits.
7. **Replacement `URIResolver` / `ExtensionRegistry` implementations.**
   Caller-pluggable; a hostile resolver is out of model.
8. **Code shipped in `w3c-testcases/`, `*/src/test/`, `etc/`,
   `xmlschema-bundle-test/`.** Test data and supporting fixtures.
   → `OUT-OF-MODEL: unsupported-component`.

## §4 Trust boundaries and data flow

| # | Transition | Authentication | Authorization |
| --- | --- | --- | --- |
| B1 | Caller → `XmlSchemaCollection.read(InputSource | InputStream | Reader | URL | Document | Element)` | none — caller is trusted | none |
| B2 | `XmlSchemaCollection.read(InputSource, ...)` → JDK `DocumentBuilder` (with `FEATURE_SECURE_PROCESSING=true`) | none | none |
| B3 | Schema parser → `URIResolver.resolveEntity(namespace, schemaLocation, baseUri)` | none | bundled `DefaultURIResolver` does **no host filtering**: it constructs `new URL(new URL(baseUri), schemaLocation)` and hands back an `InputSource` pointing at it |
| B4 | Resolved `InputSource` → `XmlSchemaCollection.read(InputSource, ...)` (recursive) | none | none |
| B5 | `XmlSchemaSerializer.serializeSchema(...)` → JDK `TransformerFactory` (with `FEATURE_SECURE_PROCESSING=true`) | none | none |
| B6 | `XmlSchemaCollection` ctor → `System.getProperty("org.apache.ws.commons.schema.extension_registry")` → `Class.forName()` | none | trusts system properties to be operator-controlled |

### Reachability preconditions per family

- **`xmlschema-core` parser** (`XmlSchemaCollection.read(InputSource)`,
  `.read(InputStream)`, `.read(Reader)`): in-model when the bytes are
  attacker-controllable. XMLSchema sets `FEATURE_SECURE_PROCESSING=true`
  on its internal `DocumentBuilderFactory`, but does **not** explicitly
  call `setFeature("http://apache.org/xml/features/disallow-doctype-decl",
  true)` or `setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES,
  false)` *(documented: `XmlSchemaCollection.java` line 713)*.
  `FEATURE_SECURE_PROCESSING` mitigates XML Schema 1.0's most expensive
  expansion cases on Xerces but does not fully disable DTD processing
  on every JDK / parser combination *(inferred — §14 Q6)*.
- **`xmlschema-core` URI resolver** (`DefaultURIResolver`): in-model
  for SSRF / cross-origin fetch when the input schema is attacker-
  controlled and contains an `xs:import schemaLocation="…"`. The
  bundled resolver constructs `new URL(...)` and returns an `InputSource`;
  the JDK then fetches it on `parse()`.
- **`xmlschema-core` parser fed a pre-parsed DOM** (`read(Document)`,
  `read(Element)`): out of model for XXE; the caller's
  `DocumentBuilderFactory` decided that. In-model for whatever the
  schema-model semantics imply about the DOM contents.
- **`xmlschema-walker`**: in-model when the visitor walks an
  attacker-controlled schema. The walker is purely in-memory.
- **`XmlSchemaPathFinder`**: in-model when caller-supplied SAX events
  are matched against an attacker-controlled schema. Its backtracking
  work is bounded per document by configurable decision-point and
  replay-event limits.
- **`XmlSchemaElementValidator` / `DomBuilderFromSax`**: in-model only
  insofar as they parse SAX events the caller hands in. `DomBuilderFromSax`
  sets `FEATURE_SECURE_PROCESSING=true` on its
  `DocumentBuilderFactory` *(documented:
  `xmlschema-walker/src/main/java/.../docpath/DomBuilderFromSax.java`
  line 81)*.

## §5 Assumptions about the environment

- **JDK**: minimum Java 17 in the 2.3.0 release; earlier releases
  supported Java 7 *(documented: `RELEASE-NOTE.txt`)*.
- **JDK XML platform**: assumes a conformant `DocumentBuilderFactory`,
  `TransformerFactory`, and SAX parser. The behavior of
  `FEATURE_SECURE_PROCESSING` depends on the provider in use; XMLSchema
  does not bundle Xerces *(inferred — §14 Q6)*.
- **Network**: the bundled `DefaultURIResolver` will issue HTTP / HTTPS
  / `file://` / `jar:` fetches via the JDK URL handlers when an `xs:include` /
  `xs:import schemaLocation` is followed. Whether the JVM has a proxy
  configured, a `SecurityManager`, or any URL-scheme restriction is
  out of XMLSchema's control *(documented: `DefaultURIResolver.java`)*.
- **Filesystem**: caller-supplied paths; XMLSchema does no path
  sanitization of `schemaLocation` values that begin with `file://`
  *(inferred — §14 Q7)*.
- **Memory**: schemas are held in memory; XMLSchema has no built-in
  ceiling on schema-document size, number of imports, or import-graph
  depth *(inferred — §14 Q8)*.
- **System properties**: `org.apache.ws.commons.schema.extension_registry`
  is consulted at `XmlSchemaCollection` construction time, and the
  named class is loaded via `Class.forName()` *(documented:
  `XmlSchemaCollection.java` line 361)*. This is a startup-time
  privilege escalation surface if the system property can be set by
  an untrusted actor *(inferred — §14 Q9)*.
- **Wrap in `AccessController.doPrivileged`**: XMLSchema brackets the
  schema parse and the system-property read in
  `AccessController.doPrivileged` to be JDK-SecurityManager-compatible
  on pre-17 JDKs *(documented: `XmlSchemaCollection.java` lines 376,
  745)*. On JDK 17+ the SecurityManager is deprecated.

### What XMLSchema does *not* do to its host (negative claims, awaiting maintainer ratification)

- Opens **no** listening sockets *(inferred — §14 Q10)*.
- Spawns **no** child processes *(inferred — §14 Q10)*.
- Installs **no** signal handlers *(inferred — §14 Q10)*.
- Reads only the documented system property
  `org.apache.ws.commons.schema.extension_registry` for
  security-relevant decisions; does **not** consume `LD_*`-style
  envvars *(inferred — §14 Q10)*.
- Writes **nothing** to the filesystem of its own initiative; the
  `XmlSchemaSerializer` writes to the `OutputStream` the caller hands
  in *(inferred — §14 Q10)*.

## §5a Build-time and configuration variants

XMLSchema is a single Maven artifact set (`xmlschema-core`,
`xmlschema-walker`, `xmlschema-bundle-test`). There are **no
compile-time feature toggles** *(inferred — §14 Q11)*. The runtime
security envelope is shaped by a small set of *runtime extension
points*:

| Knob | Default | Maintainer stance | Effect |
| --- | --- | --- | --- |
| `org.apache.ws.commons.schema.extension_registry` system property | unset *(documented: `XmlSchemaCollection.java` line 361)* | dev-time customization; if set by an untrusted actor the named class is loaded into the JVM | extension-registry class is `Class.forName`-loaded at `XmlSchemaCollection` ctor time |
| `XmlSchemaCollection.setSchemaResolver(URIResolver)` | `DefaultURIResolver` *(documented: `DefaultURIResolver.java`)* | **maintainer ruling required** — is the documented expectation that production deployments install a *restricted* resolver that refuses untrusted hosts (proposed: **yes, §10**), or is the default resolver supported as production-safe? *(inferred — §14 Q12)* | controls whether `xs:include`/`xs:import` may reach the network |
| `XmlSchemaCollection.setBaseUri(String)` | unset *(documented)* | caller-supplied | base URI against which relative `schemaLocation` values resolve |
| `org.apache.ws.commons.schema.walker.maxDecisionPoints` system property | `10000` *(documented: `XmlSchemaPathFinder.java`)* | operator-tunable per-process limit | maximum decision points created while matching one document |
| `org.apache.ws.commons.schema.walker.maxReplayedEvents` system property | `1000000` *(documented: `XmlSchemaPathFinder.java`)* | operator-tunable per-process limit | maximum previously traversed events replayed while backtracking through one document |
| `DocumentBuilderFactory` provider | JDK default (typically Xerces fork) *(inferred — §14 Q6)* | depends on the JDK | shape of XML parsing for `read(InputSource)` / `read(InputStream)` paths |

### The insecure-default case

The bundled `DefaultURIResolver` **does follow remote URLs by default**.
The maintainer ruling captured in §14 Q12 will determine whether
"a schema with `<xs:import schemaLocation='http://attacker/'/>` fetched
the URL during parse" is a `VALID` report (production deployments
should be protected by the default) or an `OUT-OF-MODEL:
non-default-build` report (production deployments are *documented* as
required to install a restricting resolver per §10).

XMLSchema sets `FEATURE_SECURE_PROCESSING=true` on its
`DocumentBuilderFactory` and `TransformerFactory` instances
*(documented: `XmlSchemaCollection.java` line 713,
`XmlSchema.java` line 886)* but does **not** call
`setFeature("http://apache.org/xml/features/disallow-doctype-decl",
true)`. The maintainer ruling on whether XXE / billion-laughs reports
against a callable `read(InputSource)` are `VALID` (the secure-processing
feature is the intended defense and is sufficient) or `MODEL-GAP`
(stricter feature toggles should be set) is captured in §14 Q6.

## §6 Assumptions about inputs

### Per-entry-point trust table

| Entry point | Parameter | Attacker-controllable? | Caller must enforce |
| --- | --- | --- | --- |
| `XmlSchemaCollection.read(InputSource is)` | `is` bytes | **yes** | nothing — XMLSchema sets `FEATURE_SECURE_PROCESSING=true` on its `DocumentBuilderFactory`; caller may need to install a restricting `URIResolver` if the source contains untrusted `xs:include`/`xs:import` |
| `XmlSchemaCollection.read(InputStream in)` | `in` bytes | **yes** | same as above |
| `XmlSchemaCollection.read(Reader r)` | `r` characters | **yes** | same as above |
| `XmlSchemaCollection.read(URL url)` | `url` | caller-supplied | caller controls; XMLSchema fetches via JDK URL handlers |
| `XmlSchemaCollection.read(Document doc)` | `doc` | **yes if doc was parsed from untrusted bytes** | caller's `DocumentBuilderFactory` is responsible for XXE / DTD posture; XMLSchema does not re-parse |
| `XmlSchemaCollection.read(Element el)` | `el` | same as `read(Document)` | same as above |
| `XmlSchemaCollection.setSchemaResolver(URIResolver)` | resolver | caller-supplied | replacing the default is the documented path for production hardening *(inferred — §14 Q12)* |
| `XmlSchemaCollection.setBaseUri(String)` | `baseUri` | **caller-supplied trusted string** | not validated; if attacker can set this they can pivot the import-resolver origin |
| `XmlSchemaCollection.setExtReg(ExtensionRegistry)` | registry | caller-supplied | caller's choice |
| `XmlSchema.write(OutputStream)` / `XmlSchema.write(Writer)` | output sink | caller-supplied | caller's choice; `FEATURE_SECURE_PROCESSING=true` is set on the internal `TransformerFactory` *(documented: `XmlSchema.java` line 886)* |
| `XmlSchemaWalker.walk(XmlSchemaElement)` | walked schema | as untrusted as the schema | none — pure in-memory walking |
| `XmlSchemaPathFinder` | SAX events + schema model | as untrusted as both | configure backtracking limits for the deployment; defaults bound decision points and replayed events per document |
| `XmlSchemaElementValidator` (walker module) | SAX events + schema model | as untrusted as both | caller validates / sanitizes outside |
| `DomBuilderFromSax` | SAX events | as untrusted as the source of the events | `FEATURE_SECURE_PROCESSING=true` is set *(documented: `DomBuilderFromSax.java` line 81)* |
| System property `org.apache.ws.commons.schema.extension_registry` | class name | **trusted by §3 item 7** | operator must lock down property setting in shared-JVM deployments |

### Size / shape / rate

- No documented limit on schema-document size *(inferred — §14 Q8)*.
- No documented limit on the import-graph depth or breadth
  *(inferred — §14 Q8)*.
- No rate limit on URL fetches when following `xs:import`
  *(inferred — §14 Q12)*.
- `XmlSchemaPathFinder` bounds decision points and replayed events per
  document by default; these limits are configurable through the
  `org.apache.ws.commons.schema.walker.maxDecisionPoints` and
  `org.apache.ws.commons.schema.walker.maxReplayedEvents` system
  properties *(documented: `XmlSchemaPathFinder.java`)*.

## §7 Adversary model

### Actors

| Actor | In scope? | Capabilities |
| --- | --- | --- |
| **Producer of the input schema bytes** | **yes** | full byte control of the schema; can include `<xs:import schemaLocation="…"/>` pointing anywhere |
| **Producer of an imported schema** at a URL the parent schema references | **yes** | returns a malicious downstream `.xsd` after the parent triggers the fetch |
| **Network attacker on the path between XMLSchema and an HTTP `schemaLocation`** | **partial** *(inferred — §14 Q13)* — TLS is the JDK URL handler's concern; XMLSchema does no certificate pinning; if the operator allowed plain `http://` fetches a network attacker can substitute |
| **In-process callers (the embedding application)** | **out of scope** | trivially full control |
| **Owner of the host filesystem** | **out of scope** | by construction |
| **Author of a hostile `URIResolver` / `ExtensionRegistry`** | **out of scope** *(§3 item 7)* |
| **Author of system-property setting on JVM startup** | **out of scope** if operator-controlled; in scope only when an untrusted actor can set `org.apache.ws.commons.schema.extension_registry` *(inferred — §14 Q9)* |
| **Co-tenant on the same JVM** (multi-app servlet container) | **out of scope** *(inferred — §14 Q14)* |
| **Side-channel observer** | **out of scope** *(inferred — §14 Q14)* |
| **Quantum adversary** | **out of scope** |

## §8 Security properties the project provides

### P1 — Memory-safety on the schema parse, given a JDK conformant to §5

- **Condition**: bytes arrive through one of the `read(...)` paths; the
  JDK platform supplies a working `DocumentBuilder`; XMLSchema sets
  `FEATURE_SECURE_PROCESSING=true` on its factories.
- **Violation symptom**: a crafted `.xsd` causes XMLSchema's *own*
  code (not the JDK XML parser) to throw an unhandled exception that
  is **not** a `XmlSchemaException` / `ParserConfigurationException` /
  `IOException` / `SAXException` — i.e. crashes outside the documented
  failure mode. Memory corruption is JVM-level and not a Java-side
  defect.
- **Severity**: typically **correctness-only** (Java does not have
  memory corruption); `VALID-HARDENING` if the symptom is an
  unhandled `RuntimeException` that the caller cannot catch through
  the documented surface *(inferred — §14 Q15)*.
- *(inferred — §14 Q15)*

### P2 — Some mitigation of "expensive" schema expansion via `FEATURE_SECURE_PROCESSING=true` on the internal `DocumentBuilderFactory` and `TransformerFactory`

- **Condition**: the JDK XML provider honors `FEATURE_SECURE_PROCESSING`;
  XMLSchema's `read(InputSource | InputStream | Reader)` paths take the
  internal `DocumentBuilderFactory`.
- **Violation symptom**: a billion-laughs / quadratic-expansion attack
  parses to completion (no `SAXException`, no `XmlSchemaException`),
  the host JVM runs out of memory or pegs the CPU.
- **Severity**: **maintainer ruling required** — see §14 Q6.
- *(documented: `XmlSchemaCollection.java` line 713,
  `XmlSchema.java` line 886, `DomBuilderFromSax.java` line 81)*

### P3 — Round-trip parse → model → serialize consistency

- **Condition**: a well-formed schema is parsed and serialized; the
  serializer's `TransformerFactory` is configured with
  `FEATURE_SECURE_PROCESSING=true`.
- **Violation symptom**: a serialized schema is not a valid XSD, or
  differs semantically from the parsed one *(inferred — §14 Q16)*.
- **Severity**: **correctness-only**; not security-relevant unless the
  divergence creates a security-meaningful misinterpretation downstream
  *(inferred — §14 Q16)*.
- *(inferred — §14 Q16)*

### P4 — `xmlschema-walker` deterministic visit order

- **Condition**: visitor pattern is invoked on the in-memory model.
- **Violation symptom**: visit order varies across invocations on the
  same model, breaking caller assumptions.
- **Severity**: **correctness-only**.
- *(inferred — §14 Q17)*

## §9 Security properties the project does *not* provide

State each plainly so a triager can route an inbound report to the
matching disclaimer.

- **No SSRF defense on `xs:import`/`xs:include schemaLocation` URLs.**
  The bundled `DefaultURIResolver` constructs a `URL` from the parent
  schema's base URI plus the schema-location value and returns an
  `InputSource` pointing at it. The JDK then fetches it on parse.
  XMLSchema applies *no* allowlist, *no* protocol restriction, and *no*
  host filtering of any kind. The caller is responsible for installing
  a restricting `URIResolver` if the input schema is attacker-controlled
  *(documented: `DefaultURIResolver.java`)*.
- **No XXE / DTD defense beyond `FEATURE_SECURE_PROCESSING=true`.**
  XMLSchema does **not** call
  `setFeature("http://apache.org/xml/features/disallow-doctype-decl",
  true)`, does **not** set
  `setFeature("http://xml.org/sax/features/external-general-entities",
  false)`, and does **not** unset the analogous `XMLInputFactory`
  properties. Whether this is sufficient depends on the JDK XML
  provider in use *(inferred — §14 Q6)*.
- **No defense when the caller passes in a pre-parsed `Document` or
  `Element`.** The hardening on the internal `DocumentBuilderFactory`
  is moot — the caller's parser produced the DOM *(documented:
  `XmlSchemaCollection.read(Document)` / `.read(Element)`)*.
- **No bound on schema-document size, import-graph depth, or import
  fan-out.** A schema that includes thousands of imports, or imports
  recursively, will be processed to completion or until JVM resources
  are exhausted *(inferred — §14 Q8)*.
- **No protection of imported schemas at rest.** Schemas pulled from
  HTTP are fetched in cleartext if the URL is `http://`. The caller
  must use TLS-protected URLs or install a restricting resolver
  *(documented: `DefaultURIResolver.java`)*.
- **No defense against the system-property-controlled
  `ExtensionRegistry` being a malicious class.** The named class is
  loaded into the running JVM and instantiated.
- **No data-at-rest protection.** Schemas serialized by
  `XmlSchemaSerializer` are plain XML on whatever sink the caller
  provided.
- **No constant-time guarantees.** XMLSchema does not deal with
  secrets.
- **No defense against side-channel observation.**
- **No quantum resistance.**

### False-friend properties (call out separately)

- **`FEATURE_SECURE_PROCESSING=true` looks like full XXE defense,
  but it is not.** On many JDK XML providers it imposes safe limits on
  entity expansion (XML-1.0 quadratic-blow-up bounds) but does **not**
  disable external entities or DTD processing entirely. Strict XXE
  defense in Java requires the explicit
  `setFeature("http://apache.org/xml/features/disallow-doctype-decl",
  true)` plus `setFeature("http://xml.org/sax/features/external-general-entities",
  false)` and `setFeature("http://xml.org/sax/features/external-parameter-entities",
  false)`. XMLSchema does the *FEATURE_SECURE_PROCESSING* hardening
  but not the explicit XXE flags *(inferred — §14 Q6)*.
- **`DefaultURIResolver` looks like a sandbox, but it isn't.** It is
  the *bundled* resolver; its job is to resolve `xs:include`/`xs:import`,
  not to filter destinations.
- **`XmlSchemaCollection.setBaseUri(String)` looks like a chroot for
  imports, but it isn't.** If the importing schema sets an absolute
  URL in `schemaLocation`, the base URI is irrelevant — the absolute
  URL wins *(inferred — §14 Q18)*.
- **`xmlschema-walker.XmlSchemaElementValidator` looks like a
  document validator, but it is a tooling helper.** It is not a
  drop-in replacement for `javax.xml.validation.Validator` and does
  not promise XSD-1.0 / 1.1 conformance *(inferred — §14 Q3)*.

### Well-known attack classes XMLSchema does not single-handedly defend against

- **XXE / external-entity disclosure** via DTD-enabled JDK XML
  providers — depends on the JDK XML factory and on caller-side
  hardening when `read(Document)` / `read(Element)` is used.
- **SSRF via `xs:import schemaLocation`** — see §9 first bullet.
- **Billion-laughs / quadratic blowup** — partially mitigated by
  `FEATURE_SECURE_PROCESSING=true`, but not universally.
- **Schema-amplification DoS** — a deeply nested or heavily-recursive
  schema can exhaust memory.
- **Confused-deputy fetch via untrusted `baseUri` + relative
  `schemaLocation`** — the operator-supplied base URI is trusted.

## §10 Downstream responsibilities

The embedding Java application **must**:

1. Decide whether the schema bytes being parsed are
   attacker-controllable. If yes, install a restricting
   `URIResolver` via `XmlSchemaCollection.setSchemaResolver(...)` that
   refuses arbitrary `http://` / `https://` / `file://` / `jar:` /
   `ftp:` URLs. The bundled `DefaultURIResolver` does not filter
   *(documented: `DefaultURIResolver.java`)*.
2. When passing a pre-parsed `Document` / `Element` into
   `XmlSchemaCollection.read(...)`, use a `DocumentBuilderFactory`
   hardened against XXE — specifically with `disallow-doctype-decl=true`
   and external-entity processing disabled.
3. When using the `read(InputSource | InputStream | Reader)` path
   against attacker-controlled bytes, verify the JDK XML provider in
   use treats `FEATURE_SECURE_PROCESSING=true` as sufficient defense
   *(inferred — §14 Q6)*.
4. Bound the maximum allowable schema size and the maximum import-graph
   depth at the *caller* level. XMLSchema imposes no such limit
   *(inferred — §14 Q8)*.
5. Set `org.apache.ws.commons.schema.extension_registry` only at JVM
   startup from a trusted source; do not allow untrusted actors to set
   it.
6. Set `XmlSchemaCollection.setBaseUri(...)` from an operator-trusted
   string, not from anywhere an attacker can influence.
7. Run on a release-supported branch (currently 2.3.0 line)
   *(documented: `RELEASE-NOTE.txt`)*.

The embedding Java application **should** additionally implement these
defense-in-depth controls:

1. Enforce URL scheme and destination restrictions in the resolver:
  allow only `https://` to approved hosts; deny `file://`, `jar:`,
  loopback, link-local, and RFC1918/private address ranges.
2. Set explicit XML parser anti-XXE features in addition to
  `FEATURE_SECURE_PROCESSING=true`: `disallow-doctype-decl=true`,
  `external-general-entities=false`, and
  `external-parameter-entities=false`.
3. Apply import-fetch budgets at the caller boundary: maximum import
  depth, total imported bytes, and total import count per top-level
  parse.
4. Use connect/read timeouts for import fetches and fail closed on
  timeout or policy-check errors.
5. Log import-resolution decisions (requested URI, normalized target,
  allow/deny result, reason) for incident response and triage.
6. Prefer integrity-controlled schema sources (pinned internal mirror
  or checksum-verified artifacts) instead of live internet fetches.

## §11 Known misuse patterns

- **Passing an attacker-controlled `.xsd` to
  `XmlSchemaCollection.read(InputSource)` with the
  `DefaultURIResolver` in place.** The schema's `xs:include` /
  `xs:import` are followed unrestricted, leading to SSRF / cross-origin
  fetch / `file://` disclosure *(documented:
  `DefaultURIResolver.java`)*. The fix is per §10 item 1.
- **Trusting `read(Document)` / `read(Element)` to inherit XXE
  defenses from XMLSchema.** XMLSchema does not re-parse; the caller's
  factory chose the posture.
- **Setting `org.apache.ws.commons.schema.extension_registry` from a
  servlet-context init parameter or a config file an end user can
  influence.** The named class is loaded and instantiated at
  `XmlSchemaCollection` construction.
- **Treating `XmlSchemaElementValidator` (walker module) as a
  conformant XSD validator.** It is a tooling helper; conformant
  document validation against an XSD requires the JDK
  `javax.xml.validation.Validator` *(inferred — §14 Q3)*.
- **Using the same `XmlSchemaCollection` instance across thread
  boundaries without external synchronization.** XMLSchema does not
  document its threading model *(inferred — §14 Q19)*.
- **Fetching imported schemas over plain `http://` from a remote
  registry.** The operator should restrict to `https://` or pin to a
  trusted mirror.
- **Letting the parsed schema's base URI be derived from the
  attacker-controlled bytes** (e.g. an absolute `xmlns:tns` URL the
  attacker provided) and then walking the URI graph from that base.

## §11a Known non-findings (recurring false positives)

This section is the highest-leverage input for automated agentic
security scans. Each entry: tool symptom, why it is safe under the
model, the section that licenses the call.

- **"`new URL(baseUri, schemaLocation).openConnection()` — SSRF risk in
  `DefaultURIResolver`."** Bundled behavior, explicitly documented as
  defaulted unrestricted; operator must install a restricting resolver
  per §10 item 1. → `OUT-OF-MODEL: trusted-input` *(if the maintainer
  rules at Q12 that the default is dev/test)*, or `VALID-HARDENING`
  *(if the maintainer rules the default is supported)*.
- **"`DocumentBuilderFactory.newInstance()` not setting
  `disallow-doctype-decl`."** Investigated under §14 Q6 — XMLSchema
  *does* set `FEATURE_SECURE_PROCESSING=true` which is the
  XSLT-conventional hardening lever. → `KNOWN-NON-FINDING` if the Q6
  ruling lands "secure-processing is sufficient", else `VALID-HARDENING`.
- **"`Class.forName(System.getProperty(...))` is dynamic-class-loading."**
  Documented extension point; the system property is the trust gate
  *(documented: `XmlSchemaCollection.java` line 361)*. → `OUT-OF-MODEL:
  trusted-input`.
- **"`AccessController.doPrivileged` deprecated in JDK 17."** JDK
  compatibility wrapper; deprecation is informational on JDK 17 and
  required on pre-17 JDKs. → `KNOWN-NON-FINDING`.
- **"Hardcoded password in test resources."** No such resource exists
  in this repo; if a tool flags `w3c-testcases/` data, it is W3C
  conformance suite data *(documented: top-level `w3c-testcases/`)*. →
  `OUT-OF-MODEL: unsupported-component`.
- **"`XmlSchemaSerializer` uses identity Transformer — could be
  exploited via XSLT injection."** Identity transform; no stylesheet
  reachable from input *(documented: `XmlSchema.java` line 886)*. →
  `KNOWN-NON-FINDING`.
- **"`URLConnection.getInputStream()` without timeout."** True;
  XMLSchema does no read-timeout on fetched imports
  *(inferred — §14 Q12)*. → `VALID-HARDENING` if Q12 rules the
  default-resolver is production-safe; otherwise documented as a §10
  responsibility.
- **"Path traversal via `XmlSchemaCollection.setBaseUri()`."** Caller-
  supplied trusted string per §6. → `OUT-OF-MODEL: trusted-input`.
- **"Schemas in `w3c-testcases/` contain wide-open DTDs."** W3C
  conformance suite, used as input to unit tests *(documented:
  top-level `w3c-testcases/`)*. → `OUT-OF-MODEL: unsupported-component`.
- **"`ExtensionRegistry` is `Class.forName`-loaded — RCE."** Trusted
  system property per §3 item 7. → `OUT-OF-MODEL: trusted-input`.
- **"`XmlSchemaCollection.read(URL)` follows `file://` to read
  arbitrary local files."** Caller passed the URL; trusted entry point.
  → `OUT-OF-MODEL: trusted-input`.

## §12 Conditions that would change this model

Revise this document when any of the following lands:

- A change in the default `URIResolver` behavior — e.g. adding host
  filtering, refusing non-HTTPS, or adding read/connect timeouts.
- A change in the default `FEATURE_SECURE_PROCESSING=true` hardening —
  e.g. adding explicit `disallow-doctype-decl=true`.
- A new public entry point on `XmlSchemaCollection` that accepts new
  input shapes.
- A new built-in resource limit, or a change to an existing resource
  limit.
- A new feature flag in `xmlschema-walker`'s validator that turns it
  into a claimed-conformant document validator.
- An upgrade of the supported JDK minimum that changes the JDK XML
  provider behavior.
- A change in the system-property contract for `ExtensionRegistry`.
- A vulnerability report that cannot be cleanly routed to one of the
  §13 dispositions — evidence the model has a gap.

## §13 Triage dispositions

A report against XMLSchema receives exactly one of the following:

| Disposition | Meaning | Licensed by |
| --- | --- | --- |
| `VALID` | Violates a §8 property via an in-scope §7 adversary using an in-scope §6 input. | §8, §6, §7 |
| `VALID-HARDENING` | No §8 property violated, but a §11 misuse pattern can be made harder to fall into by code change. Typically no CVE. | §11 |
| `OUT-OF-MODEL: trusted-input` | Requires attacker control of a §6 parameter the model marks trusted (caller-supplied `Document` / `Element`, caller-supplied `baseUri`, caller-supplied `URIResolver`, JVM system property, etc.). | §6 |
| `OUT-OF-MODEL: adversary-not-in-scope` | Requires a §7 actor the model excludes (in-process caller, hostile `URIResolver`, operator). | §7 |
| `OUT-OF-MODEL: unsupported-component` | Lands in `w3c-testcases/`, `*/src/test/`, `etc/`, `xmlschema-bundle-test/`. | §3 items 4, 8 |
| `OUT-OF-MODEL: non-default-build` | Only manifests under a §5a configuration the maintainer rules dev/test (e.g. an unsafe custom `URIResolver`). | §5a |
| `OUT-OF-MODEL: out-of-layer` | Concerns a *document* validation step delegated to `javax.xml.validation.Validator`, or a WSDL parser upstream. | §3 items 1–3 |
| `BY-DESIGN: property-disclaimed` | Concerns a §9 property the project explicitly does not provide (no SSRF defense, no XXE defense beyond secure-processing, no schema-size or import-graph resource ceiling). | §9 |
| `KNOWN-NON-FINDING` | Matches a §11a recurring false positive. | §11a |
| `MODEL-GAP` | Cannot be cleanly routed to any of the above — triggers §12 model revision. | §12 |

## §14 Open questions for the maintainers

Every *(inferred)* tag in the body maps to one of these. Proposed
answers are inline; please confirm, correct, or strike.

### Wave 1 — security policy + meta

**Q1.** XMLSchema does not currently ship an in-repo `SECURITY.md`.
The de facto policy is "report via `https://www.apache.org/security/`".
Should the project (a) adopt a `SECURITY.md` that names a supported-
branch matrix (proposed), (b) leave reporting to the foundation page
only, or (c) defer to the Webservices PMC's umbrella policy? *(meta)*

**Q2.** Confirm that XMLSchema's threat model treats the project as
**a schema-modeling library that performs network IO when resolving
imports**, not a generic XML parser (proposed: **yes**). *(maps to §2)*

**Q3.** `xmlschema-walker.XmlSchemaElementValidator` — proposed
position is "tooling helper, not a claimed-conformant document
validator". Confirm? *(maps to §3 item 1, §9 false-friend, §11)*

### Wave 2 — XXE / DTD posture (highest leverage)

**Q4.** When `read(Document)` / `read(Element)` is the entry point,
proposed position is "the caller's DOM was already parsed; XMLSchema
makes no XXE claim about it" (`OUT-OF-MODEL: trusted-input`). Confirm?
*(maps to §3 item 5, §13)*

**Q5.** When the URI resolver follows an `xs:import schemaLocation`,
proposed position is "what the remote host returns is parsed by the
*caller-installed* parser path; the *fetch* itself is the
attacker-influenced action and §9 disclaims SSRF defense" (`§9`).
Confirm? *(maps to §3 item 6, §9)*

**Q6.** **The big XXE question.** XMLSchema sets
`FEATURE_SECURE_PROCESSING=true` on its internal
`DocumentBuilderFactory` and `TransformerFactory`. It does **not** set
`disallow-doctype-decl=true`, `external-general-entities=false`, or
`external-parameter-entities=false`. The maintainer ruling is required
on:

- (a) Is `FEATURE_SECURE_PROCESSING=true` considered the sufficient
  defense (so XXE reports against
  `XmlSchemaCollection.read(InputSource)` are `KNOWN-NON-FINDING`)?
- (b) Or are explicit `disallow-doctype-decl=true` and external-entity
  toggles the supported posture, with the current state being a
  `VALID-HARDENING` to be addressed?

Proposed answer: **(b), with a future-PR to add the explicit toggles
and document `FEATURE_SECURE_PROCESSING=true` as the present
inheritance from JDK Xerces.** *(maps to §5a, §8 P2, §9, §11a)*

### Wave 3 — URI resolver / SSRF

**Q7.** Confirm that XMLSchema does *not* sanitize `schemaLocation`
when it begins with `file://`, `jar:`, etc. (proposed: no sanitization;
operator's `URIResolver` is the gate). *(maps to §5, §11a)*

**Q8.** No documented bound on schema-document size or
import-graph depth (proposed: confirm "no bound, operator's
responsibility to cap"). Are there *de facto* bounds inside XMLSchema?
*(maps to §5, §9, §10 item 4)*

**Q9.** `org.apache.ws.commons.schema.extension_registry` system
property: confirm that production deployments are expected to set
it (if at all) at JVM startup from a trusted source — i.e. an
untrusted-actor-set value is `OUT-OF-MODEL: trusted-input` (proposed).
*(maps to §5a, §11)*

**Q10.** Negative-side inventory in §5: XMLSchema opens **no**
sockets *other than what the JDK URL handler does when following an
import*; spawns **no** processes; installs **no** signal handlers;
reads **only** the documented system property; writes **nothing** of
its own initiative. Confirm? *(maps to §5)*

**Q11.** Build-time variants: confirm there are no compile-time feature
toggles; the security envelope is shaped only by runtime extension
points (proposed). *(maps to §5a)*

**Q12.** **The big URI-resolver question.** The bundled
`DefaultURIResolver` follows `http://` / `https://` / `file://` /
`jar:` URLs without filtering. Is this:

- (a) "Supported production posture" — a report that an attacker
  schema's `<xs:import schemaLocation='http://attacker/'/>` triggered
  a fetch is `VALID`?
- (b) "Dev/test default; operators are documented as required to
  install a restricting resolver per §10" — same report is
  `OUT-OF-MODEL: non-default-build`?

Proposed: **(b)** with a clarification in `README.txt` and/or
`SECURITY.md` that production deployments handling untrusted schema
bytes must install a restricting `URIResolver`. *(maps to §5a, §9,
§10 item 1, §11a, §13)*

### Wave 4 — adversary model, edge cases

**Q13.** Network attacker on `http://` import fetches: proposed
position is "operator is documented as required to use `https://`
or a restricting resolver; TLS validity is the JDK URL handler's
concern". Confirm? *(maps to §7)*

**Q14.** Co-tenant in shared JVM and side-channel observers: out of
scope (proposed)? *(maps to §7)*

**Q15.** §8 P1 (memory safety on parse): proposed wording "any
`RuntimeException` outside the documented `XmlSchemaException`/
`ParserConfigurationException`/`IOException`/`SAXException` family is
`VALID-HARDENING`". Confirm? *(maps to §8 P1)*

**Q16.** §8 P3 (round-trip parse → model → serialize consistency):
do you claim this as a security property, or only as a correctness
one? Proposed: correctness-only unless the divergence creates a
security-meaningful downstream misinterpretation, in which case it is
a `VALID-HARDENING`. *(maps to §8 P3)*

**Q17.** §8 P4 (walker deterministic visit order): is the visit order
documented? Proposed: correctness-only. *(maps to §8 P4)*

**Q18.** `setBaseUri(...)` semantics: if an `<xs:import
schemaLocation='http://absolute/'/>` is encountered, the base URI is
ignored. Confirm? *(maps to §9 false-friend)*

**Q19.** Threading model: is `XmlSchemaCollection` documented as
thread-safe? Proposed: not thread-safe; callers synchronize. *(maps to
§11)*

### Wave 5 — coexistence & publication

**Q20.** This document should be hosted in-repo at
`docs/security/threat-model.md` (proposed) or on
`ws.apache.org/xmlschema/`? *(meta)*

**Q21.** §11a known-non-findings is thin (~11 patterns). Could the
XMLSchema PMC populate from the JIRA "not a bug" / "wontfix"
closures (`XMLSCHEMA-*` tickets)? Concrete asks: 3–5 patterns the PMC
sees recur in inbound reports. *(meta — §11a)*

**Q22.** What kind of change to XMLSchema should trigger a revision
(proposed list in §12 — confirm or correct)? *(meta — §12)*

---

## Appendix: SECURITY.md / website → §x back-map

XMLSchema does not ship a `SECURITY.md`. The threat-model sources are
the in-repo `README.txt`, the `RELEASE-NOTE.txt`, and the JavaDoc /
source comments. The project website is
<https://ws.apache.org/xmlschema/>.

| Source | Claim | Lands in |
| --- | --- | --- |
| `README.txt` | "lightweight schema object model that can be used to manipulate and generate XML schema representations" | §1, §2 intended use |
| `RELEASE-NOTE.txt` (2.3.0) | Java 17 minimum, Java 7 dropped | §5 environment |
| `xmlschema-core/src/main/java/org/apache/ws/commons/schema/XmlSchemaCollection.java` line 361 | `org.apache.ws.commons.schema.extension_registry` system property loaded via `Class.forName` | §5a, §6, §11 |
| `XmlSchemaCollection.java` line 713 | `docFac.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, TRUE)` | §5a, §8 P2 |
| `XmlSchemaCollection.java` line 745 | `AccessController.doPrivileged` wrapper for the SAX parse | §5 |
| `XmlSchema.java` line 886 | `trFac.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, TRUE)` for serializer | §5a, §8 P2 |
| `xmlschema-core/src/main/java/.../resolver/DefaultURIResolver.java` | URL composed from `baseUri` + `schemaLocation`; no filtering | §3 item 7, §9 SSRF disclaim, §10 item 1, §11 first bullet |
| `xmlschema-core/src/main/java/.../resolver/URIResolver.java` | Resolver interface — caller-pluggable | §2 caller-roles, §10 item 1 |
| `xmlschema-walker/src/main/java/.../docpath/DomBuilderFromSax.java` line 81 | `factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, TRUE)` | §5a, §8 P2 |
| `xmlschema-walker/src/main/java/.../docpath/XmlSchemaPathFinder.java` | Configurable per-document limits on decision points and replayed events | §4, §5a, §6, §12 |
