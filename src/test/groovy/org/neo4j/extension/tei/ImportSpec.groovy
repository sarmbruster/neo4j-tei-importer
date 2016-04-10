package org.neo4j.extension.tei

import org.junit.Rule
import org.neo4j.extension.spock.Neo4jResource
import org.neo4j.extension.spock.Neo4jUtils
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.tooling.GlobalGraphOperations
import spock.lang.Ignore
import spock.lang.Specification

/**
 * @author Stefan Armbruster
 */
class ImportSpec extends Specification {

    @Delegate
    @Rule
    Neo4jResource neo4j = new Neo4jResource()

    def "should sample xml file be imported successfully"() {

        setup:
        def cut = new TeiImporter(graphDatabaseService: graphDatabaseService)

        when:
        def result = cut.importXml(this.class.getResourceAsStream("/1667-09-23_Langius_a_Lubienietzki-mit-Regest-ohne-Kommentar.xml"))

        then:
        true
        "match (n) return count(*) as c".cypher()[0].c > 0

        and:
        """match (n)-[:NEXT]-()
        with n, count(*) as c
        where c > 2
        return n,c""".cypher().size() == 0

    }

    @Ignore
    def "populate test db"() {

        setup:
        def targetDirectory = "/home/stefan/neo4j-enterprise-2.3.2/data/graph.db"
        new File(targetDirectory).deleteDir()
        GraphDatabaseService graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(targetDirectory)
        def cut = new TeiImporter(graphDatabaseService: graphDatabaseService)

        when:
        cut.importXml(this.class.getResourceAsStream("/1667-09-23_Langius_a_Lubienietzki-mit-Regest-ohne-Kommentar.xml"))

        then:
        true

        cleanup:
        graphDatabaseService.shutdown()
    }

}

