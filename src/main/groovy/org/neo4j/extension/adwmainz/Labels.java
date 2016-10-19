package org.neo4j.extension.adwmainz;

import org.neo4j.graphdb.Label;

/**
 * @author Stefan Armbruster
 */
public enum Labels implements Label {
    Person,
    Org,
    Place,
    Source, // "Quelle"
    Reference, // allg. Referenzknoten
    Word,
    Tag,
    Abstract,
    Root;

}
