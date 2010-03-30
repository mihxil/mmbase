/*
 * This software is OSI Certified Open Source Software.
 * OSI Certified is a certification mark of the Open Source Initiative. The
 * license (Mozilla version 1.0) can be read at the MMBase site. See
 * http://www.MMBase.org/license
 */
package org.mmbase.core.event;

import java.io.*;
import java.util.*;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 *
 * @author  Michiel Meeuwissen
 * @since   MMBase-1.9.3
 * @version $Id: TransactionEvent.java 41369 2010-03-15 20:54:45Z michiel $
 */
public abstract class SystemEvent extends Event {
    private static final Logger LOG = Logging.getLoggerInstance(SystemEvent.class);

    public SystemEvent() {
    }

    /**
     * Notifies that the local MMBase is now fully up and running
     */
    public static class Up extends SystemEvent {
    }

    static {
        SystemEventListener logger = new SystemEventListener() {
                public void notify(SystemEvent s) {
                    LOG.service(" Received " + s);
                }
            };
        EventManager.getInstance().addEventListener(logger);
    }

}