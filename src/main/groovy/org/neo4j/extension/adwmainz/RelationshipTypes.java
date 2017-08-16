package org.neo4j.extension.adwmainz;

import org.neo4j.graphdb.RelationshipType;

/**
 * @author Stefan Armbruster
 */
public enum RelationshipTypes implements RelationshipType {
    IS_CHILD_OF,
    NEXT,
    HAS_ABSTRACT,
    HAS_BODY,
    NEXT_TAG,
    STARTS_AT,
    ENDS_AT,
    ALTMANN_REF,
    NEXT_ALTMANN;
}
