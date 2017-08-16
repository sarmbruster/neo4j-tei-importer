package org.neo4j.extension.adwmainz

import groovy.transform.CompileStatic
import org.neo4j.graphdb.Label
import org.neo4j.graphdb.schema.IndexDefinition
import org.neo4j.graphdb.schema.Schema
import org.neo4j.helpers.collection.Iterables
import org.neo4j.kernel.api.KernelTransaction
import org.neo4j.kernel.api.security.AccessMode
import org.neo4j.kernel.api.security.SecurityContext
import org.neo4j.kernel.extension.KernelExtensionFactory
import org.neo4j.kernel.impl.factory.GraphDatabaseFacade
import org.neo4j.kernel.impl.spi.KernelContext
import org.neo4j.kernel.lifecycle.Lifecycle
import org.neo4j.kernel.lifecycle.LifecycleAdapter

import java.util.concurrent.TimeUnit

/**
 * @author Stefan Armbruster
 */
@CompileStatic
class IndexSetupKernelExtensionFactory extends KernelExtensionFactory<IndexSetupKernelExtensionFactory.Dependencies> {

    interface Dependencies {
        GraphDatabaseFacade graphDatabaseFacade()
    }

    public IndexSetupKernelExtensionFactory() {
        super("indexSetup")
    }

    @Override
    Lifecycle newInstance(KernelContext context, Dependencies dependencies) throws Throwable {
        new LifecycleAdapter() {
            @Override
            void start() throws Throwable {
                Thread.start {
                    GraphDatabaseFacade facade = dependencies.graphDatabaseFacade()
                    def transaction = facade.beginTransaction(KernelTransaction.Type.explicit, SecurityContext.AUTH_DISABLED, 10, TimeUnit.SECONDS)
                    try {
                        Schema schema = facade.schema()
                        createIndexIfNotExists(schema, Labels.Altmann, "name")
//                        schema.indexFor(Labels.Person).on("idno").create()
//                        schema.indexFor(Labels.Org).on("idno").create()
//                        schema.indexFor(Labels.Place).on("idno").create()
                        transaction.success()
                    } finally {
                        transaction.close()
                    }
                }
            }

            void createIndexIfNotExists(Schema schema, Label label, String propertyKey) {
                if (!indexExists(schema, label, propertyKey)) {
                    schema.indexFor(label).on(propertyKey).create()
                }
            }

            boolean indexExists(Schema schema, Label label, String propertyKey) {
                for (IndexDefinition id: schema.getIndexes(label)) {
                    String indexProperty = Iterables.single(id.propertyKeys);
                    if (indexProperty == propertyKey) {
                        return true
                    }
                }
                return false
            }
        }
    }

}
