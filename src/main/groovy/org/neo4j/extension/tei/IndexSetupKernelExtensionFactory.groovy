package org.neo4j.extension.tei

import org.neo4j.kernel.api.KernelTransaction
import org.neo4j.kernel.api.security.AccessMode
import org.neo4j.kernel.extension.KernelExtensionFactory
import org.neo4j.kernel.impl.factory.GraphDatabaseFacade
import org.neo4j.kernel.impl.spi.KernelContext
import org.neo4j.kernel.lifecycle.Lifecycle
import org.neo4j.kernel.lifecycle.LifecycleAdapter

/**
 * @author Stefan Armbruster
 */
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
                    def facade = dependencies.graphDatabaseFacade()
                    def transaction = facade.beginTransaction(KernelTransaction.Type.explicit, AccessMode.Static.FULL, 10*1000)
                    try {
                        def schema = facade.schema()
                        schema.indexFor(Labels.Person).on("idno").create()
                        schema.indexFor(Labels.Org).on("idno").create()
                        schema.indexFor(Labels.Place).on("idno").create()
                        transaction.success()
                    } finally {
                        transaction.close()
                    }
                }
            }
        }
    }

}
