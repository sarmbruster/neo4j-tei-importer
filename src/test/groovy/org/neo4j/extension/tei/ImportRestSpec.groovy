package org.neo4j.extension.tei

import com.sun.jersey.api.client.UniformInterfaceException
import org.junit.ClassRule
import org.neo4j.extension.spock.Neo4jServerResource
import org.neo4j.test.server.HTTP
import spock.lang.Shared
import spock.lang.Specification

/**
 * @author Stefan Armbruster
 */
class ImportRestSpec extends Specification {

    @Shared
    @ClassRule
    Neo4jServerResource neo4j = new Neo4jServerResource(
            thirdPartyJaxRsPackages: [
                    "org.neo4j.extension.tei": "/tei",
            ]
    )

    def "upload a file"() {
        when:
        def response = neo4j.http.PUT("tei/import", HTTP.RawPayload.rawPayload(this.class.getResource("/1667-09-23.xml").text))

        then:
        def e = thrown(UniformInterfaceException)  // HTTP 204 (empty response) throws and exception
        e.response.status == 204
    }

}
