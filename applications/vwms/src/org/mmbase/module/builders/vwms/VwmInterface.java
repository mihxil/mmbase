/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders.vwms;

import org.mmbase.module.core.*;
import org.mmbase.module.builders.*;

/**
 * Virtual Web Master interface.
 * A VWM should implement this interface.
 * The routines defined here are entry routines for initializing a VWM, associating listeners (clients) with the VWM,
 * and managing changes in local and remote nodes.
 *
 * Note: This interface defines a few methods also defined by the MMBaseObserver interface. Maybe it should extend it?
 *
 * @author Daniel Ockeloen
 * @author Pierre van Rooden (javadocs)
 * @version 5-Apr-2001
 */

public interface VwmInterface {
    /**
    * Initialize the Vwm.
    * @param vwmnode
    * @param Vwms The VWMs builder. It is not really necessary as this is the same as the <code>parent</code> attribute of <code>vwmnode</code>.
    *
    */
    public void init(MMObjectNode node, Vwms Vwms);

    /**
    * Add a client to the listen queue of the wvm.
    * @param client The client-object to add
    * @return <code>true</code> if the client was added, <code>false</code> if it already existed in the queue.
    */
    public boolean addClient(VwmCallBackInterface client);

    /**
    * Release a client from the listen queue of the wvm.
    * @param client The client-object to release
    * @return <code>true</code> if the client was released, <code>false</code> if it did not exist in the queue.
    */
    public boolean releaseClient(VwmCallBackInterface client);

    /**
     * Called when a local node is changed.
     * @param number Number of the changed node as a <code>String</code>
     * @param builder type of the changed node
     * @param ctype command type, 'c'=changed, 'd'=deleted', 'r'=relations changed, 'n'=new
     * @return <code>true</code> if maintenance was performed, <code>false</code> (the default) otherwise
     */
    public boolean nodeRemoteChanged(String number,String builder,String ctype);

    /**
     * Called when a remote node is changed.
     * @param number Number of the changed node as a <code>String</code>
     * @param builder type of the changed node
     * @param ctype command type, 'c'=changed, 'd'=deleted', 'r'=relations changed, 'n'=new
     * @return <code>true</code> if maintenance was performed, <code>false</code> (the default) otherwise
     */
    public boolean nodeLocalChanged(String number,String builder,String ctype);
}
