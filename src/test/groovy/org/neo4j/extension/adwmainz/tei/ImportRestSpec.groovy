package org.neo4j.extension.adwmainz.tei

import com.sun.jersey.api.client.UniformInterfaceException
import org.junit.Rule
import org.neo4j.extension.spock.Neo4jServerResource
import org.neo4j.test.server.HTTP
import spock.lang.Specification

/**
 * @author Stefan Armbruster
 */
class ImportRestSpec extends Specification {

    @Delegate(interfaces = false)
    @Rule
    Neo4jServerResource neo4j = new Neo4jServerResource(
            thirdPartyJaxRsPackages: [
                    "org.neo4j.extension.adwmainz.tei": "/tei",
            ]
    )
    def testResource = this.class.getResource("/tei/1667-09-23_Langius_a_Lubienietzki-mit-Regest-ohne-Kommentar.xml")

    def "upload a file"() {
        when:
        def response = neo4j.http.request("PUT", "tei/import", HTTP.RawPayload.rawPayload(testResource.text))

        then:
        def e = thrown(UniformInterfaceException)  // HTTP 204 (empty response) throws and exception
        e.response.status == 204
    }

    def "import a file by url reference"() {
        when:
        def urlEncodedUrl = URLEncoder.encode(testResource.toString(), "UTF-8")
        def response = neo4j.http.request("PUT", "tei/import?url=${urlEncodedUrl}")

        then:
        def e = thrown(UniformInterfaceException)  // HTTP 204 (empty response) throws and exception
        e.response.status == 204
    }



}
