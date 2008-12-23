/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.security.implementation.cloudcontext;

import org.mmbase.security.implementation.cloudcontext.builders.Ranks;
import org.mmbase.security.implementation.cloudcontext.builders.Users;
import java.util.*;
import org.mmbase.util.transformers.*;
import org.mmbase.module.core.*;
import org.mmbase.core.CoreField;
import org.mmbase.security.SecurityException;
import org.mmbase.security.Rank;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.*;
import org.mmbase.cache.Cache;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * This is a basic implemention of {@link Provider} that implements all the methods in a default way.
 *
 * @author Michiel Meeuwissen
 * @version $Id: BasicUserProvider.java,v 1.1 2008-12-23 17:30:42 michiel Exp $
 * @since  MMBase-1.9.1
 */
public abstract class BasicUserProvider implements UserProvider {

    private static final Logger log = Logging.getLoggerInstance(BasicUserProvider.class);

    private static final CharTransformer md5 = new MD5();

    protected String statusField           = Users.FIELD_STATUS;
    protected String userNameField         = Users.FIELD_USERNAME;
    protected String passwordField         = Users.FIELD_PASSWORD;
    protected String defaultcontextField   = Users.FIELD_DEFAULTCONTEXT;
    protected String validFromField        = Users.FIELD_VALID_FROM;
    protected String validToField          = Users.FIELD_VALID_TO;
    protected String lastLogonField        = Users.FIELD_LAST_LOGON;

    private final MMObjectBuilder userBuilder;

    public BasicUserProvider(MMObjectBuilder ub) {
        userBuilder = ub;
    }


    public MMObjectNode getAnonymousUser() throws SecurityException {
        return getUser("anonymous", "", true);
    }

    public MMObjectNode getUser(String userName, String password, boolean encode) {

        if (log.isDebugEnabled()) {
            log.debug("username: '" + userName + "' password: '" + password + "'");
        }
        MMObjectNode user = getUser(userName);

        if (userName.equals("anonymous")) {
            log.debug("an anonymous username");
            if (user == null) {
                throw new SecurityException("no node for anonymous user"); // odd.
            }
            return user;
        }

        if (user == null) {
            log.debug("username: '" + userName + "' --> USERNAME NOT CORRECT");
            return null;
        }
        String encodedPassword = encode ? encode(password) : password;
        String dbPassword = user.getStringValue(getPasswordField());
        if (encodedPassword.equals(dbPassword)) {
            if (log.isDebugEnabled()) {
                log.debug("username: '" + userName + "' password: '" + password + "' found in node #" + user.getNumber());
            }
            Rank userRank = getRank(user);
            if (userRank == null) {
                userRank = Rank.ANONYMOUS;
                log.warn("rank for '" + userName + "' is unknown or not registered, using anonymous.");
            }
            if (userRank.getInt() < Rank.ADMIN.getInt() && getField(getStatusField()) != null) {
                int status = user.getIntValue(getStatusField());
                if (status == -1) {
                    throw new SecurityException("account for '" + userName + "' is blocked");
                }
            }
            if (userRank.getInt() < Rank.ADMIN_INT && getValidFromField() != null) {
                long validFrom = user.getLongValue(getValidFromField());
                if (validFrom != -1 && validFrom * 1000 > System.currentTimeMillis() ) {
                    throw new SecurityException("account for '" + userName + "' not yet active");
                }
            }
            if (userRank.getInt() < Rank.ADMIN_INT && getValidToField() != null) {
                long validTo = user.getLongValue(getValidToField());
                if (validTo != -1 && validTo * 1000 < System.currentTimeMillis() ) {
                    throw new SecurityException("account for '" + userName + "' is expired");
                }
            }
            if (getLastLogonField() != null) {
                user.setValue(getLastLogonField(), System.currentTimeMillis() / 1000);
                user.commit();
            }
            return user;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("username: '" + userName + "' found in node #" + user.getNumber() + " --> PASSWORDS NOT EQUAL (" + encodedPassword + " != " + dbPassword + ")");
            }
            return null;
        }
    }

    /**
     * Gets the usernode by userName (the 'identifier'). Or a securityException if not found.
     */
    public MMObjectNode getUser(String userName)  {
        if (userName == null ) return null;
        boolean userNameCaseSensitive = getUserNameCaseSensitive();
        if (!userNameCaseSensitive) {
            userName = userName.toLowerCase();
        }
        Cache<String,MMObjectNode> userCache = Caches.getUserCache();
        MMObjectNode user = userCache.get(userName);
        if (user == null) {
            NodeSearchQuery nsq = new NodeSearchQuery(getUserBuilder());
            StepField sf        = nsq.getField(getField(getUserNameField()));
            BasicFieldValueConstraint cons = new BasicFieldValueConstraint(sf, userName);
            cons.setCaseSensitive(userNameCaseSensitive);
            nsq.setConstraint(cons);
            nsq.addSortOrder(nsq.getField(getField("number")));
            SearchQueryException e = null;
            try {
                Iterator<MMObjectNode> i = getUserBuilder().getNodes(nsq).iterator();
                if(i.hasNext()) {
                    user = i.next();
                }

                if(i.hasNext()) {
                    log.warn("Found more users with username '" + userName + "'");
                }
            } catch (SearchQueryException sqe) {
                e = sqe; // even if database down 'extra admins' can log on.
            }
            if (user == null) {
                User admin =  Authenticate.getLoggedInExtraAdmin(userName);
                if (admin != null) {
                    user = admin.getNode();
                }
            }
            if (user == null && e != null) {
                throw new SecurityException(e);
            }
            userCache.put(userName, user);
        }
        return user;
    }


    /**
     * @param rank Rank to be searched. Never <code>null</code>.
     * @param userName Username to match or <code>null</code>
     * @since MMBase-1.8
     */
    public MMObjectNode getUserByRank(String rank, String userName) {
        MMBase mmb = MMBase.getMMBase();
        BasicSearchQuery query = new BasicSearchQuery();
        MMObjectBuilder ranks = mmb.getBuilder("mmbaseranks");
        BasicStep step = query.addStep(ranks);
        StepField sf = query.addField(step, ranks.getField("name"));
        Constraint cons = new BasicFieldValueConstraint(sf, rank);
        query.addField(step, ranks.getField("number"));
        BasicRelationStep relStep = query.addRelationStep(mmb.getInsRel(), getUserBuilder());
        query.addField(relStep.getNext(), getField("number"));
        relStep.setDirectionality(RelationStep.DIRECTIONS_SOURCE);
        relStep.setRole(Integer.valueOf(mmb.getRelDef().getNumberByName("rank")));
        if (userName != null) {
            StepField sf2 = query.addField(relStep.getNext(), getField(getUserNameField()));
            BasicFieldValueConstraint cons2 = new BasicFieldValueConstraint(sf2, userName);
            cons2.setCaseSensitive(getUserNameCaseSensitive());
            BasicCompositeConstraint composite = new BasicCompositeConstraint(CompositeConstraint.LOGICAL_AND);
            composite.addChild(cons);
            composite.addChild(cons2);
            cons = composite;
        }

        query.setConstraint(cons);
        // sometimes, I quite hate the 'core version' query-framework.

        try {
            List<MMObjectNode> result = mmb.getClusterBuilder().getClusterNodesFromQueryHandler(query);
            if (log.isDebugEnabled()) {
                log.debug("Executing " + query + " --> " + result);
            }
            if (result.size() > 0) {
                return result.get(0).getNodeValue("mmbaseusers");
            } else {
                return null;
            }
        } catch (SearchQueryException sqe) {
            log.error(sqe);
            return null;
        }
    }

    public Rank getRank(MMObjectNode userNode) {
        Cache<Integer,Rank> rankCache = Caches.getRankCache();
        Integer userNumber = Integer.valueOf(userNode.getNumber());
        Rank rank;
        if (userNode != null) {
            rank = rankCache.get(userNumber);
        } else {
            log.warn("No node given, returning Anonymous.");
            return Rank.ANONYMOUS;
        }

        if (rank == null) {
            if (userNode instanceof Authenticate.AdminVirtualNode) {
                rank = Rank.ADMIN;
            } else {
                List<MMObjectNode> ranks =  userNode.getRelatedNodes("mmbaseranks", RelationStep.DIRECTIONS_DESTINATION);
                if (ranks.size() > 1) {
                    log.warn("More then one rank related to mmbase-user " + userNode.getNumber() + " (but " + ranks.size() + ")");
                }
                rank = Rank.ANONYMOUS;
                if (ranks.size() == 0) {
                    log.debug("No ranks related to this user");
                } else {
                    Iterator<MMObjectNode> i = ranks.iterator();
                    while (i.hasNext()) {
                        Ranks rankBuilder = Ranks.getBuilder();
                        Rank r = rankBuilder.getRank(i.next());
                        if (r.compareTo(rank) > 0) rank = r; // choose the highest  one
                    }
                }
            }
            rankCache.put(userNumber, rank);
        }
        return rank;
    }


    /**
     * @javadoc
     */
    public boolean isValid(MMObjectNode node)  {
        if (! (getUserBuilder().getClass().isInstance(node.getBuilder()))) {
            log.info("Node is no Users object but " + node.getBuilder().getTableName() + ", corresponding user is invalid");
            return false;
        }
        boolean valid = true;
        long time = System.currentTimeMillis() / 1000;
        if (getValidFromField() != null) {
            long from = node.getLongValue(getValidFromField());
            if (from > time) {
                valid = false;
            }
        }
        if (getValidToField() != null) {
            long to = node.getLongValue(getValidToField());
            if (to > 0 && to < time) {
                valid = false;
            }
        }
        if (node.getIntValue(getStatusField()) < 0) {
            valid = false;
        }
        if (! valid) {
            Caches.invalidateCaches(node.getNumber());
        }
        return valid;
    }


    public String encode(String e) {
        return md5.transform(e);
    }

    public MMObjectBuilder getUserBuilder() {
        return userBuilder;
    }


    /**
     * @see org.mmbase.security.implementation.cloudcontext.User#getOwnerField
     */
    public String getDefaultContext(MMObjectNode node)  {
        if (node == null) return "system";
        MMObjectNode contextNode = node.getNodeValue(getDefaultContextField());
        return contextNode == null ? null : contextNode.getStringValue("name");
    }

    public boolean isOwnNode(User user, MMObjectNode node) {
        MMObjectNode userNode = user.getNode();
        return  (userNode != null && getUserBuilder().getClass().isInstance(userNode.getBuilder()) && userNode.equals(node));
    }


    protected String getPasswordField() {
        return passwordField;
    }
    protected String getStatusField() {
        return statusField;
    }
    protected String getUserNameField() {
        return userNameField;
    }
    protected String getValidFromField() {
        return validFromField;
    }
    protected String getValidToField() {
        return validToField;
    }
    protected String getLastLogonField() {
        return lastLogonField;
    }
    protected String getDefaultContextField() {
        return defaultcontextField;
    }

    protected CoreField getField(String f) {
        return getUserBuilder().getField(f);
    }


    protected boolean getUserNameCaseSensitive() {
        return true;
    }


}
