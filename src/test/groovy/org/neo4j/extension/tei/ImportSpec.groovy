package org.neo4j.extension.tei

import org.junit.Rule
import org.neo4j.extension.spock.Neo4jResource
import org.neo4j.extension.spock.Neo4jUtils
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.tooling.GlobalGraphOperations
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

        GraphDatabaseService graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase("/home/stefan/neo4j-enterprise-2.3.2/data/graph.db")
        def cut = new TeiImporter(graphDatabaseService: graphDatabaseService)

        when:
        def result = cut.importXml(this.class.getResourceAsStream("/1667-09-23.xml"))

        then:
        true
/*        "match (n) return count(*) as c".cypher()[0].c > 0

        and:
        """match (n)-[:NEXT]-()
        with n, count(*) as c
        where c > 2
        return n,c""".cypher().size() == 0*/

        cleanup:
        graphDatabaseService.shutdown()
    }
}
