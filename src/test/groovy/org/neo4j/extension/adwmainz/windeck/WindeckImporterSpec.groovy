package org.neo4j.extension.adwmainz.windeck

import org.junit.Rule
import org.neo4j.extension.spock.Neo4jResource
import spock.lang.Specification


/**
 * @author Stefan Armbruster
 */
class WindeckImporterSpec extends Specification {

    @Delegate(interfaces = false)
    @Rule
    Neo4jResource neo4j = new Neo4jResource()

    def "should import text file"() {
        when:
        def result = "CALL adwmainz.windeck('file:src/test/resources/windeck/windeck_graf.utf8.small', 10)".cypher()

        then:
        "match (n) return count(*) as c".cypher()[0].c == 832

        and:
        "match (n:Word) return min(n.position) as pos".cypher()[0].pos == 10
        //aresult == "abc"

    }

/*
    def "should parse with tagsoup"() {

        when:
        Neo4jUtils.withSuccessTransaction graphDatabaseService, {
            SAXParser saxParser = SAXParserImpl.newInstance()
            saxParser.parse this.getClass().getResourceAsStream("/windeck_graf.utf8.small"), new Handler(graphDatabaseService: graphDatabaseService)
        }

        then:
        true
    }
*/

}
