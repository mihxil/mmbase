/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.security;

import java.util.Set;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
/**
 * This class is used when no authorization is configured.
 * @javadoc
 * @author Eduard Witteveen
 * @version $Id: NoAuthorization.java,v 1.6 2002-06-07 12:56:55 pierre Exp $
 */
public class NoAuthorization extends Authorization {
    private static Logger log=Logging.getLoggerInstance(NoAuthorization.class.getName());

    /**
     *	This method does nothing
     */
    protected void load() {
    }

    /**
     *	This method does nothing
     */
    public void create(UserContext user, int nodeid) {
    }

    /**
     *	This method does nothing
     */
    public void update(UserContext user, int nodeid) {
    }

    /**
     * This method does nothing
     */
    public void remove(UserContext user, int nodeid) {
    }

    /**
     * This method does nothing
     */
    public boolean check(UserContext user, int nodeid, Operation operation) {
        return true;
    }

    /**
     * This method does nothing
     */
    public void assert(UserContext user, int nodeid, Operation operation) throws org.mmbase.security.SecurityException {
    }

    /**
     * This method does nothing
     */
    public boolean check(UserContext user, int nodeid, int srcNodeid, int dstNodeid, Operation operation) {
        return true;
    }

    /**
     * This method does nothing
     */
    public void assert(UserContext user, int nodeid, int srcNodeid, int dstNodeid, Operation operation) throws org.mmbase.security.SecurityException {
    }

    // used to get some very basic functionality inside the security..
    private static String EVERYBODY = "everybody";

    /**
     * This method does nothing, except from giving a specified string back
     */
    public String getContext(UserContext user, int nodeid) throws org.mmbase.security.SecurityException {
        return EVERYBODY;
    }

    /**
     * This method does nothing, except from checking if the setContext was valid..
     */
    public void setContext(UserContext user, int nodeid, String context) throws org.mmbase.security.SecurityException {
        if(!EVERYBODY.equals(context)) throw new org.mmbase.security.SecurityException("unknown context");
    }

    /**
     * This method does nothing, except from returning a dummy value
     */
    public Set getPossibleContexts(UserContext user, int nodeid) throws org.mmbase.security.SecurityException {
        java.util.HashSet contexts = new java.util.HashSet();
    contexts.add(EVERYBODY);
    return contexts;
    }
}
