/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.applications.vprowizards.spring.action;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.mmbase.applications.vprowizards.spring.ResultContainer;
import org.mmbase.applications.vprowizards.spring.cache.CacheFlushHint;
import org.mmbase.applications.vprowizards.spring.util.PathBuilder;
import org.mmbase.bridge.*;
import org.mmbase.bridge.util.Queries;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * this action creates a relation. source and destiation nodes can be set either as nodennr or as nodenr reference,
 * which meens they are looked up in the id map of the {@link ResultContainer}.
 *
 * @author Ernst Bunders
 *
 */
public class CreateRelationAction extends AbstractRelationAction {

    public static final String SORT_POSITION_BEGIN = "begin";
    public static final String SORT_POSITION_END = "end";

    private static final Logger log = Logging.getLoggerInstance(CreateRelationAction.class);

    private String sortPosition = CreateRelationAction.SORT_POSITION_END;
    private String sortField;

    @Override
    protected void createCacheFlushHints() {
        CacheFlushHint hint = new CacheFlushHint(CacheFlushHint.TYPE_RELATION);
        hint.setRelationNumber(getNode().getNumber());
        hint.setSourceNodeNumber(sourceNode.getNumber());
        hint.setDestinationNodeNumber(destinationNode.getNumber());
        addCachFlushHint(hint);

    }

    @Override
    protected Node createRelation(Transaction transaction, Map<String, Node> idMap, HttpServletRequest request) {
        //preconditions
        if(!SORT_POSITION_BEGIN.equals(sortPosition) && !SORT_POSITION_END.equals(sortPosition)){
            addGlobalError("error.field.value", new String[]{"sortPosition", sortPosition} );
        }

        if(!StringUtils.isBlank(sortField)){
            if(!relationManager.hasField(sortField)){
                addGlobalError("error.field.unknown", new String[]{"sortField", this.getClass().getName(), relationManager.getName()});
            }
        }
        if (!hasErrors()) {
            Relation rel;
            if ("source".equals(searchDir)) {
                rel = relationManager.createRelation(destinationNode, sourceNode);
            } else {
                rel = relationManager.createRelation(sourceNode, destinationNode);
            }
            for (Map.Entry<String, Object> entry : getRelationValues().entrySet()) {
                rel.setValue(entry.getKey(), entry.getValue());
            }
            log.service("Created relation " + rel);

            return rel;
        } else {
            log.warn("Not creating relation, because already in error");
        }
        return null;
    }

    /**
     * When sortField is set, and it is a valid field, the sort position is calculated and set on the new relation.
     *
     * @see org.mmbase.applications.vprowizards.spring.action.AbstractNodeAction#processNode(org.mmbase.bridge.Transaction)
     */
    @Override
    protected void processNode(Transaction transaction) {
        log.info("Processing relation (sortField " + sortField + ")");
        if (!StringUtils.isBlank(sortField)) {

            // set the position if we need to do that
            Integer sortPositionValue = resolvePosition(transaction);
            log.info("Resolved position value  " + sortPositionValue);
            if (sortPositionValue != null) {
                getNode().setIntValue(sortField, sortPositionValue);
            }
        }
    }

    /**
     * Derive the position field value, if sortField is set. If something goes wrong a global error is set. The sort
     * position value will be, depending on the sortPostion field, either the lowest current sort position value minus
     * 1, or the highest current sort position value plus one.
     *
     * @param transaction
     * @return the value to be inserted as the new sort position.
     */
    protected final Integer resolvePosition(Transaction transaction) {
        if (StringUtils.isBlank(sortField)) {
            return null;
        }
        int position = 1;

        if (sourceNode.getNumber() < 0) {
            // Won't work for new nodes
            return position;
        }

        // find the lowest or highest relation number

        // it is unlikely that the path matches duplicate builder names here, but who knows?
        PathBuilder pathBuilder = new PathBuilder(new String[] { sourceNode.getNodeManager().getName(),
                                                                 role,
                                                                 destinationNode.getNodeManager().getName() });

        Query q = Queries.createQuery(transaction, sourceNode.getNumber() + "", pathBuilder.getPath(),
                                pathBuilder.getStep(1) + "." + sortField,
                                null, pathBuilder.getStep(1) + "." + sortField,
                                (sortPosition.equals("begin") ? "up" : "down"), null, false);
        q.setMaxNumber(1);
        NodeList nl = transaction.getList(q);
        if (nl.size() > 0) {
            position = nl.getNode(0).getIntValue(role + "." + sortField);
            position = (sortPosition.equals("begin") ? position - 1 : position + 1);
        } else {
            log.warn("Query " + q + " didn't result anything");
        }

        return Integer.valueOf(position);
    }

    public String getSortPosition() {
        return sortPosition;
    }

    public void setSortPosition(String sortPosition) {
        this.sortPosition = sortPosition;
    }

    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    @Override
    public void setRole(String role) {
        super.setRole(role);
        if ("posrel".equals(role) && this.sortField == null) {
            this.sortField = "pos";
        }
    }

}
