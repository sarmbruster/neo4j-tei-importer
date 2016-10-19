package org.neo4j.extension.adwmainz

import org.junit.Rule
import org.neo4j.extension.spock.Neo4jResource
import org.neo4j.extension.spock.WithNeo4jTransaction
import org.neo4j.graphdb.schema.IndexDefinition
import org.neo4j.graphdb.schema.Schema
import spock.lang.Specification

/**
 * @author Stefan Armbruster
 */
class IndexSetupKernelExtensionFactorySpec extends Specification {

    @Rule
    @Delegate(interfaces = false)
    Neo4jResource neo4j = new Neo4jResource()

    @WithNeo4jTransaction
    def "should indexes be created"() {
        when:
        Schema schema = graphDatabaseService.schema()
        def indexes = schema.indexes.collectEntries {
            IndexDefinition i -> [ i.label.name(), i.propertyKeys.first() ]
        }

        then:
        indexes.size() == 3
        ["Person", "Org", "Place"].every { indexes[it] == "idno"}

    }

}
