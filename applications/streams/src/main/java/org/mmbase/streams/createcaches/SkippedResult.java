/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.streams.createcaches;

import org.mmbase.bridge.Node;
import org.mmbase.applications.media.MimeType;

import java.net.*;

class SkippedResult extends Result {
    SkippedResult(JobDefinition def, URI in) {
        super(def, in);
    }
    public Node getDestination() {
        return null;
    }
    public URI getOut() {
        return null;
    }
    public MimeType getMimeType() {
        return null;
    }
    public boolean isReady() {
        return true;
    }
}