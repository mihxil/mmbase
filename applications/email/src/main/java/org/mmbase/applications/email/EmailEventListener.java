/*
 * This software is OSI Certified Open Source Software.
 * OSI Certified is a certification mark of the Open Source Initiative. The
 * license (Mozilla version 1.0) can be read at the MMBase site. See
 * http://www.MMBase.org/license
 */
package org.mmbase.applications.email;
import org.mmbase.core.event.*;

/**
 *
 * @author Michiel Meeuwissen
 * @since MMBase-1.9.3
 * @version $Id: TransactionEventListener.java 41419 2010-03-16 12:54:44Z michiel $
 */
public interface EmailEventListener extends EventListener {
    void notify(EmailEvent event);
}
