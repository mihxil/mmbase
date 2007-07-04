package nl.didactor.security;

import org.mmbase.security.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.bridge.*;
import org.mmbase.storage.search.RelationStep;

import org.mmbase.module.core.MMObjectNode;
import java.util.*;

/**
 * This security-related class wraps around a user node. This object
 * contains all information about a user, it can also report back
 * the roles based on a given context.
 * @author Johannes Verelst &lt;johannes.verelst@eo.nl&gt;
 * @author Michiel Meeuwissen
 * @version $Id: UserContext.java,v 1.9 2007-07-04 15:52:29 michiel Exp $
 */
public class UserContext extends org.mmbase.security.BasicUser {
    private static final Logger log = Logging.getLoggerInstance(UserContext.class);

    private final int wrappedNode;
    private final String identifier ;
    private final String owner;
    private final Rank rank;
    private final Set<String> roles;

    /**
     * Copy constructor which only resets the application. This is needed because 'asis' is
     * considered an application here.
     */
    public UserContext(UserContext uc, String application) {
        super(application);
        wrappedNode = uc.wrappedNode;
        identifier = uc.identifier;
        owner = uc.owner;
        rank = uc.rank;
        roles = uc.roles;
    }

    /**
     * From the org.mmbase.security.UserContext interface
     */
    public UserContext(String identifier, String owner, Rank rank, String app) {
        super(app);
        this.identifier = identifier;
        this.owner = owner;
        this.rank = rank;
        wrappedNode = 0;
        roles = new HashSet<String>();
    }

    /**
     * Initialize the usercontext for a given user node. This checks
     * the rank of the user by looking at the node type
     */
    public UserContext(MMObjectNode node, String app) {
        super(app);
        //wrappedNode = node.getCloud().getNode(node.getNumber());
        owner = node.getStringValue("username");
        identifier = owner;
        this.wrappedNode = node == null ? 0 : node.getNumber();
        roles = getRoles(node);
        Rank proposedRank = Rank.ANONYMOUS;
        for (String roleName : getRoles()) {
            if (roleName.equals("courseeditor")) {
                Rank editor = Rank.getRank("editor");
                if (editor.getInt() > proposedRank.getInt()) {
                    proposedRank = editor;
                }
                continue;
            }
            if (roleName.equals("systemadministrator")) {
                proposedRank = Rank.ADMIN; 
                break;
            }
            Rank user = Rank.getRank("didactor user");
            if (user.getInt() > proposedRank.getInt()) {
                proposedRank = user;
            }
        }
        if (proposedRank == Rank.ANONYMOUS) {
            throw new org.mmbase.security.SecurityException("No role");
        }
        rank = proposedRank;

    }

    public static Set<String> getRoles(MMObjectNode node) {
        Set<String> result = new HashSet<String>();
        if (node != null) {
            List<MMObjectNode> roles = node.getRelatedNodes("roles", RelationStep.DIRECTIONS_DESTINATION);
            for (MMObjectNode role : roles) {
                result.add(role.getStringValue("name"));
            }
        }
        return result;
    }

    public Set<String> getRoles() {
        return roles;
    }
    
    /**
     * From the org.mmbase.security.UserContext interface
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * From the org.mmbase.security.UserContext interface
     */
    public String getOwnerField() {
        return owner;
    }

    /**
     * From the org.mmbase.security.UserContext interface
     */
    public Rank getRank() throws org.mmbase.security.SecurityException {
        return rank;
    }

    public Integer getUserNumber() {
        return Integer.valueOf(wrappedNode);
    }
}
