package org.neo4j.extension.adwmainz.windeck

import org.ccil.cowan.tagsoup.jaxp.SAXParserImpl
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.neo4j.extension.spock.Neo4jResource
import org.neo4j.extension.spock.Neo4jUtils
import org.neo4j.extension.adwmainz.windeck.Handler
import spock.lang.Specification

import javax.xml.parsers.SAXParser

/**
 * @author Stefan Armbruster
 */
class WindeckImporterSpec extends Specification {

    @Delegate(interfaces = false)
    @Rule
    Neo4jResource neo4j = new Neo4jResource()

    def "should import text file"() {
        when:
        def result = "CALL adwmainz.windeck('file:src/test/resources/windeck/windeck_graf.utf8.small')".cypher()

        then:
        "match (n) return count(*) as c".cypher()[0].c == 829
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
