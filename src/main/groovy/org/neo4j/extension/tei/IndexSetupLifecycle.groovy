package org.neo4j.extension.tei

import org.apache.commons.configuration.Configuration
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.server.plugins.Injectable
import org.neo4j.server.plugins.PluginLifecycle

/**
 * @author Stefan Armbruster
 */
class IndexSetupLifecycle implements PluginLifecycle {

    @Override
    Collection<Injectable<?>> start(GraphDatabaseService graphDatabaseService, Configuration config) {

        Thread.start {
            sleep 100
            def tx = graphDatabaseService.beginTx()
            try {

                schema = graphDatabaseService.schema()
                schema.indexFor(Labels.Person).on("idno").create()
                schema.indexFor(Labels.Org).on("idno").create()
                schema.indexFor(Labels.Place).on("idno").create()
                tx.success()
            } finally {
                tx.close()
            }
        }
    }

    @Override
    void stop() {

    }
}
