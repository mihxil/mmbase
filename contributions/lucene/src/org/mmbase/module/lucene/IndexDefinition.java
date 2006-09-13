/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.lucene;

import java.util.*;
import org.mmbase.util.CloseableIterator;
import org.mmbase.bridge.Node;
import org.mmbase.bridge.Cloud;
import org.apache.lucene.analysis.Analyzer;

/**
 * Defines a query and possible options for the fields to index.
 *
 * @author Pierre van Rooden
 * @author Michiel Meeuwissen
 * @version $Id: IndexDefinition.java,v 1.13 2006-09-13 09:51:14 michiel Exp $
 **/
interface IndexDefinition {


    /**
     * Returns an Iterator over all {@link IndexEntry}'s defined by this index. Only makes sense if this is not a 'sub definition'.
     */
    CloseableIterator<? extends IndexEntry> getCursor();

    /**
     * Returns an Iterator over all {@link IndexEntry}'s defined by this index, restricted by a
     * certain identifier.  For a 'top' level index definition, this should normally return only one
     * Entry, and the key must be the identifier of this entry. For 'nested' IndexDefinitions, the
     * key most be the key of the parent.
     */
    CloseableIterator<? extends IndexEntry> getSubCursor(String key);

    /**
     * Per index a an Analyzer can be defined.
     */
    Analyzer getAnalyzer();

    /**
     * Defines how a Node for this index must be produced. For MMBase indices this is of course
     * quite straight-forward, but other indices may create virtual nodes here.
     */
    Node getNode(Cloud cloud, String identifier);


}

