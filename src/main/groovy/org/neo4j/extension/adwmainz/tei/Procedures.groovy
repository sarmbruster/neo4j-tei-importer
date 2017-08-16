package org.neo4j.extension.adwmainz.tei

import groovy.transform.CompileStatic
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.procedure.Context
import org.neo4j.procedure.Mode
import org.neo4j.procedure.Name
import org.neo4j.procedure.Procedure

import java.util.stream.Stream

@CompileStatic
class Procedures {

    @Context
    public GraphDatabaseService graphDatabaseService;

    @Procedure(name="tei.import", mode= Mode.WRITE)
    Stream<NodeResult> importXml(@Name("url") String urlString) {

        def importer = new TeiImporter(graphDatabaseService: graphDatabaseService)
        def sourceNode = importer.importXml(new URL(urlString).newInputStream())

        return Stream.of(new NodeResult(node: sourceNode))
    }

}
