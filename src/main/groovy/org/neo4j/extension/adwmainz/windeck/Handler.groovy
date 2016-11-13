package org.neo4j.extension.adwmainz.windeck

import org.neo4j.extension.adwmainz.Labels
import org.neo4j.extension.adwmainz.RelationshipTypes
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler

/**
 * @author Stefan Armbruster
 */
class Handler extends DefaultHandler {

    String url
    GraphDatabaseService graphDatabaseService

    Node latestWordNode = null

    Deque<Node> stack = new ArrayDeque<Node>();
    Node rootNode
    Node currentTag
    Node previousTag
    int position = 1
    boolean linkToCurrentTag = false

    // status variables for altmann
    boolean inAltmann
    String altmannText = '';
    Node altmannNode = null;

    // used to connect altmann-nodes via :NEXT
    Node prevAltmannNode = null;


    @Override
    void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        switch (localName) {

            case 'a':
                inAltmann = true
                break
            default:
                currentTag = graphDatabaseService.createNode(Labels.Tag)
                currentTag.setProperty("name", localName)

                switch (localName) {
                    case "document":
                        currentTag.setProperty("url", url)
                        break
                    default:
                        // intentionally empty
                        break
                }

                Node parent = stack.peek()
                if (parent) {
                    currentTag.createRelationshipTo(stack.peek(), RelationshipTypes.IS_CHILD_OF)
                }

                if (previousTag) {
                    previousTag.createRelationshipTo(currentTag, RelationshipTypes.NEXT_TAG)
                }
                previousTag = currentTag

                stack.push(currentTag)

                if (rootNode==null) {
                    rootNode = currentTag
                }
                linkToCurrentTag = true
        }
    }

    @Override
    void endElement(String uri, String localName, String qName) throws SAXException {
        switch (localName) {
            case 'a':

                altmannNode = graphDatabaseService.findNode(Labels.Altmann, "name", altmannText)
                if (!altmannNode) {
                    altmannNode = graphDatabaseService.createNode(Labels.Altmann)
                    altmannNode.setProperty("name", altmannText)
                    if (prevAltmannNode) {
                        prevAltmannNode.createRelationshipTo(altmannNode, RelationshipTypes.NEXT_ALTMANN)
                    }
                    prevAltmannNode = altmannNode
                }

                inAltmann = false
                altmannText = ''
                break

            default:
                def me = stack.pop()
                me.createRelationshipTo(latestWordNode, RelationshipTypes.ENDS_AT)
        }
    }

    @Override
    void characters(char[] ch, int start, int length) throws SAXException {
        def text = new String(ch, start, length)

        if (inAltmann) {
            altmannText += text
        } else {
            text.split(/\s+/).findAll {it}.each {
                Node wordNode = graphDatabaseService.createNode(Labels.Word)
                wordNode.setProperty("text", it)
                wordNode.setProperty("position", position++)
                if (latestWordNode) {
                    latestWordNode.createRelationshipTo(wordNode, RelationshipTypes.NEXT)
                }
                //wordNode.createRelationshipTo(currentTag, RelationshipTypes.IS_CHILD_OF)

                if (altmannNode) {
                    wordNode.createRelationshipTo(altmannNode, RelationshipTypes.ALTMANN_REF)
                    altmannNode = null
                }

                if (linkToCurrentTag==true) {
                    linkToCurrentTag = false
                    currentTag.createRelationshipTo(wordNode, RelationshipTypes.STARTS_AT)
                }
                latestWordNode = wordNode
            }
        }

    }
}
