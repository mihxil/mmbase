/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.security;

import org.mmbase.util.functions.Parameters;
import org.mmbase.util.functions.Parameter;
import org.mmbase.util.logging.*;
/**
 * A piece of 'action check' functionality. Provided by actions themselves, but security
 * implementations can perhaps also use this interface to administer their implementation of {@link
 * Authorization#check(UserContext, Action, Parameters)}.
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 * @since MMBase-1.9
 */
public interface ActionChecker extends java.io.Serializable {

    boolean check(UserContext user, Action ac, Parameters parameters);

    Parameter[] getParameterDefinition();


    /**
     * The ActionChecker that always allows every action to to everybody.
     * @since MMBase-1.9.2
     */
    ActionChecker ALLOWS = new ActionChecker() {
        private static final long serialVersionUID = 1L;
        @Override
        public boolean check(final UserContext user, final Action ac, final Parameters parameters) {
            return true;
        }
        @Override
        public Parameter[] getParameterDefinition() {
            return Parameter.EMPTY;
        }
        @Override
        public String toString() {
            return "allows";
        }
    };

    /**
     * This basic implementation of ActionChecker checks the action only based on rank. A minimal
     * rank is to be supplied in the constructor.
     */

    class Rank implements  ActionChecker {
        private static final long serialVersionUID = 7047822780810829661L;
        private static final Logger log = Logging.getLoggerInstance(Rank.class);
        final String rank;
        public Rank(final org.mmbase.security.Rank r) {
            rank = r.toString();
        }
        /**
         * @since MMBase-1.9.6
         */
        public Rank(final String r) {
            rank = r;
        }
        /**
         * @since MMBase-1.9.6
         */
        protected org.mmbase.security.Rank getRank() {
            final org.mmbase.security.Rank r = org.mmbase.security.Rank.getRank(rank);
            if (r == null) {
                log.error("No such rank " + rank + " returning " + org.mmbase.security.Rank.ADMIN);
                return org.mmbase.security.Rank.ADMIN;
            }
            return r;
        }
        @Override
        public boolean check(final UserContext user, final Action ac, final Parameters parameters) {
            return user.getRank().getInt() >= getRank().getInt();
        }
        @Override
        public Parameter[] getParameterDefinition() {
            return Parameter.EMPTY;
        }
        @Override
        public String toString() {
            return "at least " + rank + (org.mmbase.security.Rank.getRank(rank) == null ? "(rank doesn't exist)" : "");
        }
    }
}
