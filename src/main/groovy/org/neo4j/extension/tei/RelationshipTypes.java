package org.neo4j.extension.tei;

import org.neo4j.graphdb.RelationshipType;

/**
 * @author Stefan Armbruster
 */
public enum RelationshipTypes implements RelationshipType {
    IS_CHILD_OF,
    NEXT,
    HAS_ABSTRACT,
    HAS_BODY,
    NEXT_TAG;
}
