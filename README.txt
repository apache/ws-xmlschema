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
  xs:import/xs:include/xs:redefine reparses rejects DOCTYPE declarations
  by default and disables external DTD and external entity resolution.
  To accept schema documents that contain a DOCTYPE declaration, set:

    org.apache.ws.commons.schema.allowDTD
      Set to true to allow DOCTYPE declarations. The default is false.
      External DTD and external entity resolution remain disabled when this
      property is true.

For example, set a limit with:

  -Dorg.apache.ws.commons.schema.walker.maxDecisionPoints=20000

    -Dorg.apache.ws.commons.schema.maxImportDepth=128

    -Dorg.apache.ws.commons.schema.maxNestingDepth=256

    -Dorg.apache.ws.commons.schema.allowDTD=true

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

