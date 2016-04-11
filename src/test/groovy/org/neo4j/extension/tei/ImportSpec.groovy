package org.neo4j.extension.tei

import org.junit.Rule
import org.neo4j.extension.spock.Neo4jResource
import org.neo4j.extension.spock.Neo4jUtils
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Label
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.helpers.collection.Iterables
import org.neo4j.visualization.graphviz.GraphvizWriter
import org.neo4j.walk.Visitor
import org.neo4j.walk.Walker
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
        def result = cut.importXml(this.class.getResourceAsStream("/1667-09-23_Langius_a_Lubienietzki-mit-Regest-ohne-Kommentar.xml"), null)
//        writeGraphvizPngForLabel(Labels.Source)

        then:
        true
        "match (n) return count(*) as c".cypher()[0].c > 0

        and:
        """match (n)-[:NEXT]-()
        with n, count(*) as c
        where c > 2
        return n,c""".cypher().size() == 0

    }

    private void writeGraphvizPngForLabel(Label label) {
        def file = File.createTempFile("neo4j", ".dot")
        Neo4jUtils.withTransaction(graphDatabaseService, {
            new GraphvizWriter().emit(file, new Walker() {
                @Override
                def <R, E extends Throwable> R accept(Visitor<R, E> visitor) throws E {

                    def sourceNodes = graphDatabaseService.findNodes(label)
                    for (path in graphDatabaseService.traversalDescription().traverse(Iterables.asResourceIterable(sourceNodes))) {
                        visitor.visitNode(path.endNode())
                        path.endNode().relationships.each { visitor.visitRelationship(it) }
                    }
                    return visitor.done()
                }
            })
        })
        println "file is $file"
        "dot -Tpng -O $file".execute()
        "eog ${file}.png".execute()
    }

    @Ignore
    def "populate test db"() {

        setup:
        def targetDirectory = "${System.properties["user.home"]}/neo4j-enterprise-2.3.2/data/graph.db"
        new File(targetDirectory).deleteDir()
        GraphDatabaseService graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(targetDirectory)
        def cut = new TeiImporter(graphDatabaseService: graphDatabaseService)

        when:
        cut.importXml(this.class.getResourceAsStream("/1667-09-23_Langius_a_Lubienietzki-mit-Regest-ohne-Kommentar.xml"), null)

        then:
        true

        cleanup:
        graphDatabaseService.shutdown()
    }


}

