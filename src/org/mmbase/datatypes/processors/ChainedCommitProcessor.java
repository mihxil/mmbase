/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.datatypes.processors;

import org.mmbase.bridge.*;
import java.util.*;

/**
 * Chains a bunch of other processors into one new processor.
 *
 * @author Michiel Meeuwissen
 * @version $Id: ChainedCommitProcessor.java,v 1.5 2007-03-29 12:36:54 pierre Exp $
 * @since MMBase-1.7
 */

public class ChainedCommitProcessor implements  CommitProcessor, org.mmbase.util.PublicCloneable {

    private static final long serialVersionUID = 1L;

    private ArrayList<CommitProcessor> processors = new ArrayList<CommitProcessor>();

    public ChainedCommitProcessor add(CommitProcessor proc) {
        processors.add(proc);
        return this;
    }

    public void commit(Node node, Field field) {
        Iterator<CommitProcessor> i = processors.iterator();
        while (i.hasNext()) {
            CommitProcessor proc = i.next();
            proc.commit(node, field);
        }
        return;
    }

    public String toString() {
        return "chained" + processors;
    }

    public Object clone() {
        try {
            Object clone = super.clone();
            ((ChainedCommitProcessor) clone).processors = (ArrayList<CommitProcessor>) processors.clone();
            return clone;
        } catch (CloneNotSupportedException cnse) {
            //
            return null;
        }
    }

}
