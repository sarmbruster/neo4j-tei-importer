package org.neo4j.extension.adwmainz.windeck

import groovy.transform.CompileStatic
import org.ccil.cowan.tagsoup.jaxp.SAXParserImpl
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.logging.Log
import org.neo4j.procedure.Context
import org.neo4j.procedure.Name
import org.neo4j.procedure.PerformsWrites
import org.neo4j.procedure.Procedure
import org.neo4j.graphdb.Node

import javax.xml.parsers.SAXParser
import java.nio.charset.StandardCharsets
import java.util.stream.Stream

/**
 * @author Stefan Armbruster
 */
@CompileStatic
class WindeckImporter {

    @Context
    public GraphDatabaseService graphDatabaseService

    @Context
    public Log log

    @Procedure(value = "adwmainz.windeck")
    @PerformsWrites
    public Stream<NodeResult> importWindeck(@Name("url") String url) {

        log.info("importing document from uri %s", url)

        // wrap the supplied URL into a document tags - otherwise tagsoup gets confused
        def streams = [new ByteArrayInputStream("<document>".getBytes(StandardCharsets.UTF_8)),
                        new URL(url).newInputStream(),
                       new ByteArrayInputStream("</document>".getBytes(StandardCharsets.UTF_8))
        ]
        InputStream document = new SequenceInputStream(Collections.enumeration(streams))

        SAXParser saxParser = SAXParserImpl.newInstance()
        def handler = new Handler(url: url, graphDatabaseService: graphDatabaseService)
        saxParser.parse(document, handler)
        Stream.of(new NodeResult(node:handler.rootNode))
    }

    public static class NodeResult {
        public Node node
    }
}
