====================================
  Apache XMLSchema Release Notes 
====================================

Apache XMLSchema is a lightweight schema object model that can be used to manipulate and
generate XML schema representations. It has very few external dependencies and can
be easily integrated into an existing project.

You are welcome to kick the tires and get XMLSchema on the move. If you like to 
help us shape XMLSchema any contribution in the form of coding, testing, 
submitting improvements to the documentation, and reporting bugs are always 
welcome.

Thanks for your interest in XMLSchema!

-The XMLSchema Development Team
https://ws.apache.org/xmlschema/

===================
   Documentation
===================
 
Documentation can be found in the 'documents' distribution of this release and in
the main site.

===================
    Configuration
===================

The XML Schema path finder limits the work it performs while backtracking
through ambiguous schema content models. The following JVM system properties
adjust the per-document limits:

  org.apache.ws.commons.schema.walker.maxDecisionPoints
      Maximum decision points created while matching a document. The default is
      10000.

  org.apache.ws.commons.schema.walker.maxReplayedEvents
      Maximum previously traversed events replayed while backtracking. The
      default is 1000000.

The XML Schema walker limits how deeply it recurses while walking a schema,
so that a deeply nested schema is rejected instead of exhausting the thread
stack. The following JVM system property adjusts the limit:

  org.apache.ws.commons.schema.walker.maxDepth
      Maximum depth of nested elements, model groups and substitution group
      members while walking a schema; the same limit applies separately to
      levels of type derivation and of attribute group references. The
      default is 256, which fits a thread stack of 512 KB. Lower it if the
      walker runs on threads with smaller stacks.

  The schema collection limits the work performed while resolving imported and
  included schemas. The following JVM system properties adjust the limits:

    org.apache.ws.commons.schema.maxImportDepth
      Maximum import/include resolution depth for a schema read. The default
      is 64.

    org.apache.ws.commons.schema.maxSchemaResolutions
      Maximum number of schema documents resolved during a single top-level
      read. The default is 1000.

    org.apache.ws.commons.schema.maxNestingDepth
      Maximum structural nesting depth while building the schema model,
      including nested include/import/redefine document resolutions. The
      default is 512.

  The internal parser used by XmlSchemaCollection.read(InputSource),
  read(Reader), stream-backed read(Source), and recursive
  xs:import/xs:include/xs:redefine reparses never resolves external DTD
  subsets, external general entities or external parameter entities, so a
  schema document cannot read local files or reach the network through its
  DOCTYPE. There is no property to relax this.

  The DOCTYPE declaration itself is accepted: an internal DTD subset is a
  legitimate part of many real schema documents - the W3C's own XML
  Signature, XML Encryption and XKMS schemas declare their target namespace
  as an entity in one - and FEATURE_SECURE_PROCESSING bounds entity
  expansion by both count and accumulated size.

  When the DefaultURIResolver fetches a schema over http or https, the fetch
  is bounded: left to the JDK it has no timeout and no size limit, so one
  schemaLocation naming a slow or endless host can hold a parsing thread or
  its heap indefinitely. The import and resolution limits above bound the
  shape of the import graph, not the cost of a single fetch within it. The
  following JVM system properties adjust the bounds:

    org.apache.ws.commons.schema.remote.connectTimeoutMillis
      Connect timeout for a remote schema fetch. The default is 5000.

    org.apache.ws.commons.schema.remote.readTimeoutMillis
      Per-read timeout for a remote schema fetch. The default is 10000.

    org.apache.ws.commons.schema.remote.maxFetchMillis
      Maximum total wall-clock time for one remote schema fetch. The
      per-read timeout above bounds each blocking read separately, so this
      is what stops a host that trickles bytes below that interval. The
      default is 30000.

    org.apache.ws.commons.schema.remote.maxBytes
      Maximum bytes accepted from one remote schema fetch. The default is
      67108864 (64 MB).

    org.apache.ws.commons.schema.remote.allowNetwork
      Whether a schema location may be fetched over the network at all. The
      default is true. Set it to false in a deployment whose schema sets are
      entirely local: an xs:import naming an http or https location is then
      refused rather than fetched, and no resolver has to be supplied to get
      that. Only true and false are recognised, so a typo leaves resolution
      working rather than quietly turning it off. Local file: and jar: reads
      are unaffected either way, so this is not on its own a defence against
      an untrusted schema document - see the Security section below.

    org.apache.ws.commons.schema.remote.maxRedirects
      How many HTTP redirects one remote schema fetch may follow. The default
      is 5. Redirects are followed by the resolver rather than by the JDK, so
      the chain is bounded, every hop goes through the same checks as the
      location the schema named, and the whole chain counts against the one
      fetch deadline above. A redirect that changes scheme is refused either
      way, so an http location cannot become a file read. Set it to 0 to
      refuse a redirected schema location outright.

    org.apache.ws.commons.schema.local.allowFileSystem
      Whether a schema location may be read from the filesystem at all. The
      default is true. Set it to false where schema documents are expected
      to stand alone: a file: location, a jar:file: one, and a relative path
      with no base URI to resolve it against are then refused rather than
      read. Only true and false are recognised.

      Set alongside remote.allowNetwork=false, this leaves the bundled
      resolver with nothing it will fetch, which is the closest it comes to
      refusing every external reference. An application that must allow some
      references and refuse others still needs its own URIResolver.

  file: and jar: locations are read as before, without buffering.

  The collections returned by the "read-only" accessors on the schema model
  are, by default, the live internal collections rather than unmodifiable
  views. To wrap them so that modification throws instead, set:

    org.apache.ws.commons.schema.protectReadOnlyCollections
      Set to true to return unmodifiable views from the read-only
      accessors. The default is false.

For example, set a limit with:

  -Dorg.apache.ws.commons.schema.walker.maxDecisionPoints=20000

  -Dorg.apache.ws.commons.schema.walker.maxDepth=128

    -Dorg.apache.ws.commons.schema.maxImportDepth=128

    -Dorg.apache.ws.commons.schema.maxNestingDepth=256

    -Dorg.apache.ws.commons.schema.protectReadOnlyCollections=true

    -Dorg.apache.ws.commons.schema.remote.maxFetchMillis=10000

===================
     Security
===================

  XmlSchemaCollection resolves xs:import, xs:include and xs:redefine
  schema locations through a URIResolver. The bundled DefaultURIResolver
  is a convenience for trusted, operator-controlled schema sets. It
  resolves http, https, file and jar locations and applies no host or
  address filtering to the http and https targets it allows, so a schema
  location naming an internal host, a cloud metadata endpoint, or a local
  file is fetched on request. It does refuse a file: location that names a
  non-local authority, and a jar: archive fetched over the network.

  Where a deployment needs no remote schemas at all, setting
  org.apache.ws.commons.schema.remote.allowNetwork to false refuses http and
  https locations outright, which closes the remote-fetch half of this
  without any code. Where schema documents are expected to stand alone,
  org.apache.ws.commons.schema.local.allowFileSystem=false closes the local
  half as well, and the two together leave the bundled resolver with nothing
  it will fetch.

  Neither switch replaces a restricting resolver for an application that has
  to allow some references and refuse others: they are all-or-nothing per
  transport, and they are read when the resolver is constructed.

  Applications that parse schema or WSDL documents from an untrusted
  source must install a restricting resolver before reading them:

    XmlSchemaCollection collection = new XmlSchemaCollection();
    collection.setSchemaResolver(myRestrictingResolver);
    collection.read(source);

  A resolver that returns null declines the location, and the collection
  falls back to any schema already registered for that namespace; a
  resolver that throws rejects the read outright.

  Note that a host allowlist cannot be enforced simply by inspecting the
  location: whoever opens the connection follows HTTP redirects, and a
  redirect can move the fetch to another host. A resolver that must
  restrict destinations has to open the connection itself, with redirect
  following disabled, and re-check each hop. The bundled resolver does
  open network connections itself, to bound them, but applies no host
  policy and leaves redirects to the JDK.

  See THREAT-MODEL.md section 10 for the full list of downstream
  responsibilities.

===================
      Support
===================
 
Any problem with this release can be reported to ws-dev mailing list. If you 
are sending an email to the mailing list make sure to add the [XMLSchema] prefix 
to the subject.

To the subscribe to the mailing list send an empty email to:

  dev-subscribe@ws.apache.org


You can also log issues into the XMLSchema issue tracker at:
https://issues.apache.org/jira/browse/XMLSCHEMA

