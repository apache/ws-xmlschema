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

For example, set a limit with:

  -Dorg.apache.ws.commons.schema.walker.maxDecisionPoints=20000

    -Dorg.apache.ws.commons.schema.maxImportDepth=128

    -Dorg.apache.ws.commons.schema.maxNestingDepth=256

===================
     Security
===================

  XmlSchemaCollection resolves xs:import, xs:include and xs:redefine
  schema locations through a URIResolver. The bundled DefaultURIResolver
  is a convenience for trusted, operator-controlled schema sets. It
  resolves http, https, file and jar locations and applies no host or
  address filtering, so a schema location naming an internal host, a
  cloud metadata endpoint, or a local file is fetched on request.

  Applications that parse schema or WSDL documents from an untrusted
  source must install a restricting resolver before reading them:

    XmlSchemaCollection collection = new XmlSchemaCollection();
    collection.setSchemaResolver(myRestrictingResolver);
    collection.read(source);

  A resolver that returns null declines the location, and the collection
  falls back to any schema already registered for that namespace; a
  resolver that throws rejects the read outright.

  Note that a host allowlist cannot be enforced from inside a URIResolver:
  it returns a system ID and the JDK opens the connection, following HTTP
  redirects without consulting the resolver again. A resolver that must
  restrict destinations has to fetch the bytes itself and return an
  InputSource wrapping the stream.

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

