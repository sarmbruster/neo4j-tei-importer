package org.neo4j.extension.tei

import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.NodeChild
import org.neo4j.graphdb.DynamicLabel
import org.neo4j.graphdb.DynamicRelationshipType
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Label
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.RelationshipType

import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Context

/**
 * @author Stefan Armbruster
 */
@Slf4j
@Path("/import")
class TeiImporter {

    @Context
    GraphDatabaseService graphDatabaseService

    Map<String, Node> nodeReferences = [:]

    // during import text nodes are connected by :NEXT relationships. This one keeps track of current end of the chain
    Node currentEndOfChain

    /**
     * @param inputStream if url is not given, we import the uploaded file
     * @param url if given, we fetch from this url
     */
    @PUT
    void importXml(InputStream inputStream, @QueryParam("url") URL url) {

        if (url) {
            log.info "fetch url $url"
            inputStream = url.newInputStream()
        }

        withTransaction {
            def tei = new XmlSlurper().parse(inputStream)

            for (tag in tei.teiHeader.profileDesc.particDesc.listPerson.'*') {
                mergeReferenceNode(tag)
            }
            for (tag in tei.teiHeader.profileDesc.particDesc.listOrg.'*') {
                mergeReferenceNode(tag)
            }
            for (tag in tei.teiHeader.profileDesc.settingDesc.listPlace.'*') {
                mergeReferenceNode(tag)
            }
            for (tag in tei.teiHeader.profileDesc.textClass.keywords.list.'*') {
                mergeReferenceNode(tag)
            }

            Node source = graphDatabaseService.createNode(Labels.Source)
            source.setProperty("title", tei.teiHeader.fileDesc.titleStmt.title.text())
            currentEndOfChain = source

            for (tag in tei.teiHeader.profileDesc.correspDesc.'*') {
                setCorrespAction(source, tag)
            }

            def bodyChildren = tei.text.body.childNodes()

            def firstChild = bodyChildren.next()
            assert firstChild.name() == "pb"

            def secondChild = bodyChildren.next()
            assert secondChild.name() == "opener"

            for (child in bodyChildren) {
                addDocumentContentsRecursively(source, child)
            }
        }
    }

    def setCorrespAction(Node sourceNode, NodeChild xml) {

        String relType = xml.'@type'.text().toUpperCase()
        assert relType

        Node hyperEdge = graphDatabaseService.createNode()
        def rel = sourceNode.createRelationshipTo(hyperEdge, DynamicRelationshipType.withName(relType))

        for (tag in xml.children()) {

            def ref = tag.@ref.text()
            if (ref) {

                ref = ref[1..-1] // strip off leading "#"

                assert nodeReferences[ref] : "no reference node for $ref found"

                hyperEdge.createRelationshipTo(nodeReferences[ref], DynamicRelationshipType.withName(tag.name().toUpperCase()))
            } else if (tag.name() == 'date') {
                // set all date properties
                tag.attributes().each {k,v -> rel.setProperty(k,v)}
            }
        }
    }
/**
     *
     * @param parent
     * @param previous
     * @param reference
     * @param refType
     * @param xml either a String or a groovy.util.sluprersupport.Node
     * @return the node created for this xml element
     */
    private Node addDocumentContentsRecursively(Node parent, def xml, Node reference=null, RelationshipType refType=null ) {
        if (xml instanceof groovy.util.slurpersupport.Node) {
            String ref = xml.attributes().ref
            if (ref) {

                Node target
                if (ref.startsWith("#")) {
                    ref = ref[1..-1]   // remove leading "#"
                    target = nodeReferences[ref]
                } else {
                    target = graphDatabaseService.findNode(Labels.Reference, "ref", ref)
                    if (target==null) {
                        target = graphDatabaseService.createNode(Labels.Reference)
                        target.setProperty("ref", ref)
                    }
                }

                assert target

                // enable this to have intermediate nodes for references allowing for start and end relationsships
//                Node referenceNode = graphDatabaseService.createNode()
//                referenceNode.createRelationshipTo(target, DynamicRelationshipType.withName("REF_${xml.name().toUpperCase()}"))

                Node firstChild, lastChild
                for(child in xml.children() ) {
                    lastChild = addDocumentContentsRecursively(parent,  child )
                    if (firstChild==null) {
                        firstChild = lastChild
                    }
                }
                assert firstChild == lastChild

                firstChild.createRelationshipTo(target, DynamicRelationshipType.withName("REF_${xml.name().toUpperCase()}"))
//                firstChild.createRelationshipTo(referenceNode, DynamicRelationshipType.withName("REF_${xml.name().toUpperCase()}_START"))
//                lastChild.createRelationshipTo(referenceNode, DynamicRelationshipType.withName("REF_${xml.name().toUpperCase()}_END"))
                return parent
            } else {
                Node thisNode = createChildNodeOf(parent, Labels.Tag)
                thisNode.setProperty("tagName", xml.name())
                for(child in xml.children() ) {
                    addDocumentContentsRecursively(thisNode,  child )
                }
                return thisNode
            }
        } else {
            Node thisNode = createChildNodeOf(parent, Labels.Text)
            thisNode.setProperty("text", xml)
            currentEndOfChain.createRelationshipTo(thisNode, RelationshipTypes.NEXT)
            currentEndOfChain = thisNode
            return thisNode
        }
    }

    private Node createChildNodeOf(Node parent, Label... labels) {
        Node thisNode = graphDatabaseService.createNode(labels)
        thisNode.createRelationshipTo(parent, RelationshipTypes.IS_CHILD_OF)
        thisNode
    }

    void mergeReferenceNode(def xml) {
        // pick it attribute "unique identifier"
        // TODO: validate if this assumtion is ok

        String id = xml.'@xml:id'.text()
        assert id
        Label label = DynamicLabel.label( toUpperCamelCase(xml.name()))
        def node = graphDatabaseService.findNode(label, "id", id)
        if (node == null) {
            node = graphDatabaseService.createNode(label)
            node.setProperty("id", id)
            nodeReferences[id] = node

/*
            String typeAttr = xml.'@type'.text()
            if (typeAttr) {
                node.addLabel(DynamicLabel.label(typeAttr))
            }
*/

            // extract xml tags holding node properties
            def props = xml.'**'.grep {
                if (it.name() in ["note"]) return false // TODO: for now, we ignore notes on reference points

                if (it.localText().size() > 1) {
                    throw new RuntimeException("dunno how to handle $xml - tag ${it.name()} has multiple values")
                }
                 it.localText().size()==1
            }.collectEntries {
                [it.name(), it.localText()[0] ]
            }
            props.each { k,v -> node.setProperty(k,v)}
            log.debug("added node (:$label $props)")
        }
    }

    private String toUpperCamelCase(String s) {
        s[0].toUpperCase() + s[1..-1]
    }

    private withTransaction(Closure closure) {
        def tx = graphDatabaseService.beginTx()
        try {
            def retValue = closure.call()
            tx.success()
            return retValue
        } finally {
            tx.close()
        }
    }
}
