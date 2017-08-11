package org.neo4j.extension.adwmainz.tei

import groovy.util.logging.Slf4j
import org.neo4j.extension.adwmainz.Labels
import org.neo4j.extension.adwmainz.RelationshipTypes
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
    def xmlns = new groovy.xml.Namespace("http://www.w3.org/XML/1998/namespace", "xml")

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
        importXml(inputStream)

    }

    Node importXml(InputStream inputStream) {

        withTransaction {
            def tei = new XmlParser().parse(inputStream)

//            def tei = new XmlSlurper().parse(inputStream)

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

            // abstract
            def abstr = tei.teiHeader.profileDesc.abstract
            abstr.each {
                addDocumentContentsRecursively(it, source, RelationshipTypes.HAS_ABSTRACT, null)
            }

            // complete text
            addDocumentContentsRecursively(tei.text.body[0], source, RelationshipTypes.HAS_BODY, null)

            return source
        }
    }

    /**
     *
     * @param xml the current xml tag
     * @param parent
     * @param parentRelType
     * @param currentEndOfChain
     * @return a triplet of [ firstChildNode, lastChildNode, currentEndOfChain ]
     */
    private NodeTriplet addDocumentContentsRecursively(def xml, Node parent, RelationshipType parentRelType, Node currentEndOfChain) {
        switch (xml) {
            case groovy.util.Node:  // we're on a tag (and not on a text node of xml document)

                def referencedNode = findReferencedNodeForTag(xml)
                if (referencedNode) {
                    Node first, last
                    for (child in xml.children()) {
                        def triplet = addDocumentContentsRecursively(child, parent, RelationshipTypes.IS_CHILD_OF, currentEndOfChain)
                        currentEndOfChain = triplet.c
                        if (first == null) {
                            first = triplet.a
                        }
                        last = triplet.b
                    }

                    // create reference rels
                    def hyperEdge = graphDatabaseService.createNode()
                    def relTypeName = "REF_" + xml.name().localPart.toUpperCase()

                    // the hyperedge node is connected via REF_<tagname> relationship to the referenced "thing" (person, place, item,...)
                    hyperEdge.createRelationshipTo(referencedNode, RelationshipType.withName(relTypeName))

                    // for first and last word point to the hyperedge using REF_<tagname>_START and REF_<tagname>_END relationships
                    // in most cases first and last will be the same node, but there might be references spawning over multiple words
                    first.createRelationshipTo(hyperEdge, RelationshipType.withName("${relTypeName}_START"))
                    last.createRelationshipTo(hyperEdge, RelationshipType.withName("${relTypeName}_END"))

                    return new NodeTriplet(first, last, currentEndOfChain)
                } else { // a regular tag (aka not a reference): create a node for this tag and continue recursively for children
                    Node node = graphDatabaseService.createNode(Labels.Tag)
                    String tagName = xml.name().localPart
                    node.setProperty("name", tagName)
                    parent.createRelationshipTo(node, parentRelType)

                    Node lastSibling = null;
                    for (child in xml.children()) {
                        def triplet = addDocumentContentsRecursively(child, node, RelationshipTypes.IS_CHILD_OF, currentEndOfChain ?: node)

                        if (xml instanceof groovy.util.Node) {
                            if (lastSibling!=null) {
                                lastSibling.createRelationshipTo(triplet.a, RelationshipTypes.NEXT_TAG)
                            }
                            lastSibling = triplet.a
                        }
                        currentEndOfChain = triplet.c
                    }
                    return new NodeTriplet(node, node, currentEndOfChain)
                }
                break
            case String: // we've hit text part, so create a node for every word and connect them via :NEXT
                Node first, last
                for (word in xml.tokenize() ) {
                    last = createChildNodeOf(parent, Labels.Word)
                    last.setProperty("text", word)
                    currentEndOfChain.createRelationshipTo(last, RelationshipTypes.NEXT)
                    currentEndOfChain = last
                    if (first ==null) {
                        first = last
                    }
                }
                return new NodeTriplet(first, last, currentEndOfChain)
                break

            default:
                throw new UnsupportedOperationException(xml.getClass().getName())

        }
    }

    private Node findReferencedNodeForTag(def xml) {
        String refAttribute = xml.'@ref'  ?: xml.'@key'  // references in abstract use "ref" attribute, in full text "key" is used
        refAttribute ? nodeReferences[refAttribute[1..-1]] : null  // skip 1st character, e.g. #P007 -> P007
    }

    def setCorrespAction(Node sourceNode, groovy.util.Node xml) {

        String relType = xml.'@type'.toUpperCase()
        assert relType

        Node hyperEdge = graphDatabaseService.createNode()
        def rel = sourceNode.createRelationshipTo(hyperEdge, RelationshipType.withName(relType))

        for (tag in xml.children()) {

            def tagName = tag.name().localPart
            def ref = tag.@ref
            if (ref) {
                ref = ref[1..-1] // strip off leading "#"
                assert nodeReferences[ref] : "no reference node for $ref found"
                hyperEdge.createRelationshipTo(nodeReferences[ref], RelationshipType.withName(tagName.toUpperCase()))
            } else if (tagName == 'date') {
                // set all date properties
                tag.attributes().each {k,v -> rel.setProperty(k,v)}
            }
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

        String id = xml.attributes()[xmlns.id]
        assert id
        Label label = Label.label( toUpperCamelCase(xml.name().localPart))
        def node = graphDatabaseService.findNode(label, "id", id)
        if (node == null) {
            node = graphDatabaseService.createNode(label)
            node.setProperty("id", id)
            nodeReferences[id] = node

            // extract xml tags holding node properties
            def props = xml.children().'**'.grep {
                if (!(it instanceof groovy.util.Node)) return false
                if (it.name().localPart in ["note"]) return false // TODO: for now, we ignore notes on reference points

                if (it.localText().size() > 1) {
                    throw new RuntimeException("dunno how to handle $xml - tag ${it.name()} has multiple values")
                }
                 it.localText().size()==1
            }.collectEntries {
                [it.name().localPart, it.localText()[0] ]
            }
            props.each { k,v -> node.setProperty(k,v)}
            log.debug("added node (:$label $props)")
        }
    }

    private String toUpperCamelCase(String s) {
        def result = s[0].toUpperCase()
        if (s.length()>1) {
            result += s[1..-1]
        }
        result
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

    private static class NodeTriplet {
        Node a
        Node b
        Node c

        NodeTriplet(Node a, Node b, Node c) {
            this.a = a
            this.b = b
            this.c = c
        }
    }
}
