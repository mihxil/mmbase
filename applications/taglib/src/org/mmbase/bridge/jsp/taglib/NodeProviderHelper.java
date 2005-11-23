/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.jsp.taglib;


import org.mmbase.bridge.jsp.taglib.util.Attribute;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.util.Stack;

import org.mmbase.bridge.*;
import org.mmbase.bridge.util.Queries;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;


/**
 *
 * @author Michiel Meeuwissen
 * @version $Id: NodeProviderHelper.java,v 1.12 2005-11-23 15:09:00 michiel Exp $ 
 * @since MMBase-1.7
 */

public class NodeProviderHelper implements NodeProvider {

    private static final Logger log = Logging.getLoggerInstance(NodeProviderHelper.class);

    public static final String STACK_ATTRIBUTE = "org.mmbase.bridge.jsp.taglib._nodeStack";
    public static final String _NODE = "_node";
        
    private   Node   node;        
    private   Query  query = null;
    private   String jspvar = null;
    private   ContextReferrerTag thisTag;
    private Attribute commit = Attribute.NULL;

    /**
     * 'underscore' stack, containing the values for '_node'.
     * @since MMBase_1.8
     */
    private   Stack _Stack;

    public NodeProviderHelper(ContextReferrerTag thisTag) {
        this.thisTag     = thisTag;
    }

    

    // general attributes for NodeProviders
    // id from TagSupport

    public void setJspvar(String jv) {
        jspvar = jv;
        if ("".equals(jspvar)) jspvar = null;
    }
    public void setCommitonclose(String c) throws JspTagException {
        commit = thisTag.getAttribute(c);
    }

        
    /**
    * For use by children, they can find the current 'node' belonging
    * to this tag.
    */
    
    public Node getNodeVar() {
        return node;
    }
    
    /**
     * Children can also directly access the node member, but the
     * prefered method is to treat this variable as much as possible
     * as private, and use this.
     */

    public void setNodeVar(Node node) {        
        this.node = node;
    }
    

    public void setGeneratingQuery(Query q) {
        query = q;
    }

    public Query getGeneratingQuery() throws JspTagException {
        if (query == null) {
            query = Queries.createNodeQuery(getNodeVar());
        } 
        return query;
    }

    public String getId() {
        try {
            return (String) thisTag.id.getValue(thisTag);
        } catch (JspTagException j) {
            throw new RuntimeException(j);
        }
    }


    boolean checked = false; // need to check jspvar/pagecontext-var conflict only first time.
    /**
     * Fill the jsp and context vars
     *
     */

    public void fillVars() throws JspTagException {    
        org.mmbase.bridge.jsp.taglib.util.ContextContainer cc = thisTag.getContextProvider().getContextContainer();
        if (thisTag.id != Attribute.NULL) {
            cc.registerNode(getId(), node);
        }
        if (jspvar != null && node != null) {
            cc.setJspVar(thisTag.getPageContext(), jspvar, WriterHelper.TYPE_NODE, node);
        }
        PageContext pageContext = thisTag.getPageContext();

        _Stack = (Stack) pageContext.getAttribute(STACK_ATTRIBUTE);
        if (_Stack == null) {
            _Stack = new Stack();
            pageContext.setAttribute(STACK_ATTRIBUTE, _Stack);
        }
        _Stack.push(node);
        pageContext.setAttribute(_NODE, org.mmbase.util.Casting.wrap(node, (org.mmbase.util.transformers.CharTransformer) pageContext.getAttribute(ContentTag.ESCAPER_KEY)));
    }
               
    private String getSimpleReturnValueName(String fieldName){        
        return getSimpleReturnValueName(jspvar, fieldName);
    }
    /**
     * Generates the variable-name for a field.
     *
     * @param prefix A prefix to use. Can be null.
     * @param fieldName The name of the field.
     */
    static String getSimpleReturnValueName(String prefix, String fieldName){
        String field = fieldName.replace('.','_');
        if (prefix != null && ! "".equals(prefix)) {
            field = prefix + "_" + field;
        }
        return field;
    }

    /**
    * Does everything needed on the afterbody tag of every
    * NodeProvider.  Normally this function would be overrided with
    * one that has to call super.doAfterBody().  But not all servlet
    * engines to call this function if there is no body. So, in that
    * case it should be called from doEndTag, if the tag can do
    * something without a body.
    **/
    public int doAfterBody() throws JspTagException {
        if ((node.isChanged() || node.isNew()) && commit.getBoolean(thisTag, true)) {
            node.commit();
            log.service("Committed node " + node.getNumber() + " for user " + node.getCloud().getUser().getIdentifier());
        }
        if (_Stack != null) {
            Object pop = _Stack.pop();
            if (_Stack.empty()) {
                thisTag.getPageContext().removeAttribute(_NODE);
            } else {
                thisTag.getPageContext().setAttribute(_NODE, _Stack.peek());
            }
            _Stack = null;
        }
        return BodyTagSupport.SKIP_BODY;
    }
    
    public int doEndTag() throws JspTagException {
        // to enable gc:
        node     = null;
        checked  = false;
        return BodyTagSupport.EVAL_PAGE;
    }
}
