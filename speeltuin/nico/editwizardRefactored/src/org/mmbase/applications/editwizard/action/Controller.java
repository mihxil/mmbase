/*
 * 
 * This software is OSI Certified Open Source Software. OSI Certified is a certification mark of the
 * Open Source Initiative.
 * 
 * The license (Mozilla version 1.0) can be read at the MMBase site. See
 * http://www.MMBase.org/license
 * 
 */
package org.mmbase.applications.editwizard.action;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.*;
import javax.servlet.jsp.PageContext;

import org.mmbase.applications.editwizard.WizardException;
import org.mmbase.applications.editwizard.data.BaseData;
import org.mmbase.applications.editwizard.schema.ActionElm;
import org.mmbase.applications.editwizard.schema.WizardSchema;
import org.mmbase.applications.editwizard.session.*;
import org.mmbase.applications.editwizard.util.HttpUtil;
import org.mmbase.applications.editwizard.util.UploadUtil;
import org.mmbase.bridge.Cloud;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.xml.URIResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * this object initialed and used by settings.jsp
 * it will act as a controller
 */
public class Controller {
    
    public static final String ATTRKEY_XSLT_DOCUMENT = "xslt.document";
    public static final String ATTRKEY_XSLT_PARAMS = "xslt.params";
    public static final String ATTRKEY_XSLT_FILEPATH = "xslt.filepath";
    public static final String ATTRKEY_XSLT_URIRESOLVER = "xslt.uriresolver";
    
    public static final String ATTRKEY_REDIRECT_URI = "redirect.uri";
    public static final String ATTRKEY_EXCEPTION = "error.exception";

    public static final String ATTRKEY_SESSIONKEY = "sessionkey";

    
    static final Logger log = Logging.getLoggerInstance(Controller.class);

    public static Controller getInstance(PageContext pageContext) {
        Controller controller = new Controller();
        controller.request = (HttpServletRequest)pageContext.getRequest();
        controller.response = (HttpServletResponse)pageContext.getResponse();
        return controller;
    }

    private HttpServletRequest request = null;
    private HttpServletResponse response = null;

    // Stores the current configuration for the wizard as whole, so all open lists
    // and wizards are stored in this struct.
    private SessionData sessionData = null;
    
    //default means: 'this is not a popup'
    private String popupId = "";
    private String sessionkey = "";
    
    /**
     * construct is not allowed out of this class.
     */
    private Controller() {
        // construct is not allowed out of this class.
    }
    
    /**
     * init controller's parameter
     * @return
     * @throws WizardException
     * @throws IOException
     */
    public boolean init() throws WizardException, IOException {
        // default means: 'this is not a popup'
        popupId = HttpUtil.getParam(request, "popupid", "");
        
        // It is possible to specify an alternatvie 'sessionkey'
        sessionkey = HttpUtil.getParam(request, "sessionkey","editwizard");
        sessionData = SessionDataManager.getSessionData(request, sessionkey);
        return false;
    }

    /**
     * @return
     */
    private boolean isPopup() {
        return !"".equals(popupId);
    }

    /**
     * process request
     */
    public String processRequest(Cloud cloud) {
        String servletPath = request.getServletPath();
        try {
            if (request.getParameter("logout") != null) {
                // what to do if 'logout' is requested?
                // return to the deeped backpage and clear the session.
                return doLogout();
            }

            // removing top page from the session
            if (request.getParameter("remove") != null) {
                String uri = doRemove();
                if (uri!=null) {
                    return uri;
                }
            }

            if (servletPath.endsWith("wizard.jsp")) {
                return doWizard(cloud);
            }
            else if (servletPath.endsWith("list.jsp")) {
                boolean replace = false;
                if (isPopup()) {
                    log.debug("this is a popup");
                    replace = "true".equals(HttpUtil.getParam(request, "replace"));
                } else {
                    log.debug("this is not a popup");
                }
                return doList(cloud, replace);
            }
            else if (servletPath.endsWith("deletelistitem.jsp")) {
                return doDeleteNode(cloud);
            }
            else if (servletPath.endsWith("upload.jsp")) {
                return gotoUpload();
            }
            else if (servletPath.endsWith("processuploads.jsp")) {
                return doUpload();
            }
        }
        catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace(System.err);
            return processException(e);
        }
        return servletPath;
    }
    
    private String gotoUpload() throws WizardException {
        WizardConfig wizardConfig = SessionDataManager.findWizardConfig(sessionData, popupId);
        if (wizardConfig==null) {
            // TODO: handle exception
            throw new WizardException("what will cause this error, upload without parent wizard?");
        }
        String did = request.getParameter("did");
        if (did==null) {
            throw new WizardException("No valid parameters for the upload routines. Make sure to supply did field.");
        }
        
        request.setAttribute("did",did);
        request.setAttribute("wizardname", wizardConfig.getWizardName());
        request.setAttribute("sessionkey", sessionkey);
        request.setAttribute("popupid", popupId);
        request.setAttribute("maxsize", sessionData.getMaxUploadSize()+"");
        return "uploaddialog.jsp";
    }

    private String doUpload() throws WizardException {
        String did = request.getParameter("did");
        
        String errorMessage = null;
        String returnMessage= null;
        WizardConfig wizardConfig = SessionDataManager.findWizardConfig(sessionData, popupId);
        if (wizardConfig==null) {
            // TODO: handle exception
            throw new WizardException("what will cause this error, upload without parent wizard?");
        }
        request.setAttribute("wizardname", wizardConfig.getWizardName());
        request.setAttribute("sessionkey", sessionkey);
        request.setAttribute("popupid", popupId);
        request.setAttribute("maxsize", sessionData.getMaxUploadSize()+"");
        
        int maxsize = sessionData.getMaxUploadSize();
        try {
            String maxSizeParameter = request.getParameter("maxsize");
            if (maxSizeParameter != null && "".equals(maxSizeParameter)) {
                maxsize = Integer.parseInt(maxSizeParameter);
            }
        } catch (NumberFormatException e) {
            log.debug("Maxsize reqiest parameter " + e.getMessage());
        }
        try {
            returnMessage = UploadUtil.uploadFiles(request, wizardConfig, returnMessage, maxsize);
        }catch (WizardException we) {
            errorMessage = we.getMessage();
        }
        request.setAttribute("did",did);
        request.setAttribute("returnmessage",returnMessage);
        request.setAttribute("errormessage",errorMessage);
        
        return "uploadmessage.jsp";
    }

    private String processException(Exception exception){
//        try{
            request.setAttribute(ATTRKEY_EXCEPTION,exception);
            return "error.jsp";
//        } catch (Exception e) {
//            log.debug("The following error occurred: " + exception + org.mmbase.util.logging.Logging.stackTrace(exception));
//            return "error.jsp";
//        }
    }
    
    /**
     * process logout request. backpage and sessionkey of the session data is required.
     * @return true, processed request and jumped to logout page; false, should contiue;
     */
    private String doLogout() {
        // TODO: this should be invoke by jsp
        String refer = sessionData.getBackPage();
        // what to do if 'logout' is requested?
        // return to the deeped backpage and clear the session.
        log.debug("logout parameter given, clearing session");
        SessionDataManager.clearSession(request, sessionkey);
        log.debug("Redirecting to " + refer);
        if (!refer.startsWith("http:")) {
            refer = response.encodeRedirectURL(request.getContextPath() + refer);
        }
        request.setAttribute("refer",refer);
        return "logout.jsp";
    }
    
    /**
     * process remove request
     */
    private String doRemove() {
        String refer = sessionData.getBackPage();
        if (!SessionDataManager.isFinished(sessionData)) {
            if (!isPopup()) { // remove inline
                log.debug("popping one of subObjects " + sessionData);
                SessionDataManager.closeConfig(sessionData);
            }
            else { // popup
                log.debug("a separate running popup, so remove sessiondata for " + popupId);
                Object closedObject = SessionDataManager.getPopupConfig(sessionData, popupId);
                boolean popupFinished = SessionDataManager.closePopupConfig(sessionData, popupId);
                if (popupFinished) { 
                    log.debug("going to close this window");
                    if (closedObject instanceof WizardConfig) {
                        //    && ((WizardConfig) closedObject).isCommited()) { 
                        // data already commit to mmbase
                        // XXXX I find all this stuff in wizard.jsp too. Why??

                        log.debug("A popup was closed (commited)");
                        String sendCmd = "";
                        String objnr = "";
                        WizardConfig popupWiz = (WizardConfig) closedObject;
                        // we move from a popup sub-wizard to a parent wizard...
                        // with an inline popupwizard we should like to pass the newly created or
                        // updated
                        // item to the 'lower' wizard.
                        objnr = popupWiz.objectNumber;
                        if ("new".equals(objnr)) {
                            // obtain new object number
                            //objnr = popupWiz.wiz.getObjectNumber();
                            if (log.isDebugEnabled())
                                log.debug("Objectnumber was 'new', now " + objnr);
                            String parentFid = popupWiz.parentFid;
                            if ((parentFid != null) && (!parentFid.equals(""))) {
                                log.debug("Settings. Sending an add-item command ");
                                String parentDid = popupWiz.parentDid;
                                sendCmd = "cmd/add-item/" + parentFid + "/" + parentDid + "//";
                            }
                        }
                        else {
                            if (log.isDebugEnabled())
                                log.debug("Aha, this was existing, send an 'update-item' cmd for object "
                                                + objnr);
                            sendCmd = "cmd/update-item////";
                        }
                        if (log.isDebugEnabled())
                            log.debug("Sending command " + sendCmd + " , " + objnr);
                    }
                    
                    return redirectTo("remove.jsp");
                }
            } // popup
        } // not subObject empty

        if (SessionDataManager.isFinished(sessionData)) { // it _is_ empty? Then we are ready.
            log.debug("last object cleared, redirecting");
            log.debug("Redirecting to " + refer);
            return redirectTo(refer);
        }
        else if (SessionDataManager.isListOnTop(sessionData)) {
            log.debug("Redirecting to list");
            return redirectTo(response.encodeRedirectURL("list.jsp?proceed=true&sessionkey="
                    + sessionkey));
        }
        return null;
    }

      private String doList(Cloud cloud, boolean replace) throws WizardException {
        // stores the configuration specific for this list.
        ListConfig listConfig = null; 
        
        ListConfig config = SessionDataManager.findListConfig(sessionData, popupId);
        if (config==null) {
            listConfig = SessionDataManager.createListConfig(sessionData, popupId, replace);
            listConfig.setPage(response.encodeURL(request.getServletPath() 
                    + "?proceed=true&sessionkey=" + sessionkey));
        } else {
            listConfig = config;
            
        }
        //TODO: here should consider the case for search list 
        // configure the thing, that means, look at the parameters.
        listConfig.configure(request, cloud, sessionData.getUriResolver());

        ListWorkspace workspace = ListWorkspace.getInstance(listConfig, cloud);
        
        if (workspace.doSearch()) {
            Document listDoc = workspace.getDocument();
            Map params = workspace.getParams();
            fillAttributes(params);
            return doXslt(listDoc, params,listConfig.getTemplate());
        }
        return "error.jsp";
    }
    
    private String doDeleteNode(Cloud cloud) throws WizardException {

        ListConfig listConfig = SessionDataManager.findListConfig(sessionData);
        WizardSchema schema = listConfig.getWizardSchema();
        ActionElm actionElm = schema.getActionByType("delete");
        if (actionElm != null) {
            // Ok. let's delete this object.
            org.mmbase.bridge.Node obj = cloud.getNode(request.getParameter("objectnumber"));
            obj.delete(true);
            return redirectTo("list.jsp?proceed=true&sessionkey=" + sessionkey);
        } else {
            // No delete action defined in the wizard schema. We cannot delete.
            throw new WizardException("No delete action is defined in the wizard schema: '"+ listConfig.getWizardName() 
                    + "'. You should place &lt;action type=\"delete\" /> in your schema so that delete actions will be allowed.");
        }
    }

    /**
     * This method usually processes one command. Possible wizard commands are:
     * <ul>
     * <li>delete-item</li>
     * <li>update-item</li>
     * <li>add-item</li>
     * <li>move-up</li>
     * <li>move-down</li>
     * <li>start-wizard</li>
     * <li>goto-form</li>
     * <li>cancel</li>
     * <li>commit</li>
     * </ul>
     * 
     * @param cmd The command to be processed
     */
    private String doWizard(Cloud cloud) throws WizardException {
        
        WizardCommand command = this.getRequestCommand();
        
        WizardConfig wizardConfig = SessionDataManager.findWizardConfig(sessionData, popupId);
        
        if (wizardConfig!=null && (command!=null && command.getType()==WizardCommand.CANCEL)==false) {
            // 1) save data into current wizard config
            // 2) in another case : when upload dialog close to refresh the opener
            //    we also need to save current data.\
            // 3) if cancel, do not need to save
            WizardWorkspace workspace = WizardWorkspace.getInstance(sessionData,wizardConfig, cloud);
            workspace.saveWizardData(request);
        }
        if (command == null) {
            // check whether we need create new wizard config
            // in two case we need to create new config:
            // 1) specified object number is new
            // 2) specified object number is differ with current object number
            WizardConfig config = this.checkWizardConfig(wizardConfig, cloud);
            WizardWorkspace workspace = null;
            wizardConfig = config;
            workspace = WizardWorkspace.getInstance(sessionData,wizardConfig, cloud);
            //start new wizard
            if (wizardConfig.wizardData==null) {
                workspace.doLoad();
            }
            Document doc = workspace.getDocument();
            Map params =  workspace.getParams();
            fillAttributes(params);
            return doXslt(doc, params,"xsl/wizard.xsl");
        } else {
            if (wizardConfig==null) {
                //TODO: handle exception here
                throw new WizardException("this scenario is not defined in editwizard");
            }
            // doing command in current wizard config
            WizardWorkspace workspace = WizardWorkspace.getInstance(sessionData,wizardConfig, cloud);
            AbstractConfig returnConfig = wizardConfig;
            // here we need this status to judge what to do next
            // 1, continue another command/save, for example:
            //    after a commit maybe we need a add item
            // 2, get another config, for example:
            //    after commit, we could use original wizardconfig to add item 
            // 3, judge redirect to other page to refresh, for example:
            //    goto list.jsp or wizard.jsp  
            log.debug("Processing command " + command);
            // processes the given command
            switch (command.getType()) { //switch
                case WizardCommand.ADD_ITEM: {
                    // add item! sample: cmd/add-item/f_9/d_2//=|277|266
                    // The command parameters are the fid of the list in which the item need be added,
                    // the did of the object under which it should be added (the parent node),
                    // and a second id, indicating the object id to add.
                    // The second id can be passed either as a paremeter (the 'otherdid' parameter), in
                    // which case it involves a newly created item, OR as a value, in which case it is an
                    // enumerated list of did's, the result of a search.
                    StringTokenizer st = new StringTokenizer(command.getValue(),"|");
                    List list = new ArrayList();
                    while (st.hasMoreTokens()) {
                        String number = st.nextToken();
                        if ("".equals(number)==false) {
                            list.add(number);
                        }
                    }
                    workspace.doAddItem(command.getFid(),command.getDid(), list);
                    break;
                }
                case WizardCommand.DELETE_ITEM: {
                    // delete item! sample: cmd/delete-item/f_8/d_7//=
                    // The command parameters is the did of the node to delete.
                    // note that a fid parameter is expected in the command syntax but ignored
                    workspace.doDeleteItem(command.getFid(),command .getDid());
                    break;
                }
                case WizardCommand.UPDATE_ITEM: {
                    // update an item - replaces all fields of the item with updated values
                    // retrieved from MMbase
                    // The command parameters is a value indicating the number of the node(s) to update.
                    workspace.doUpdateItem(command.getFid(),command.getDid(),command.getParameter(2), command.getParameter(3), command.getValue());
                    break;
                }
                case WizardCommand.MOVE_UP:
                case WizardCommand.MOVE_DOWN: {
                    // This is in fact a SWAP action (swapping the order-by fieldname), not really move up or down.
                    // The command parameters are the fid of the list in which the item falls (determines order),
                    // and the did's of the nodes that are to be swapped.
                    workspace.doMoveItem(command.getFid(),command.getDid(),command.getParameter(2));
                    break;
                }
                case WizardCommand.START_WIZARD: {
                    // this involves a redirect and is handled by the jsp pages
                    // example: cmd/start-wizard/f_7/d_2/new/280/=tasks/news
                    //          cmd/start-wizard/$fid/$did/$objectnumber/$origin=$wizardname
                    // redirect with following parameter:
                    //    proceed=true
                    //    objectnumber=new
                    //    origin=280
                    //    popupid=
                    //    sessionkey=editwizard
                    //    did=d_2
                    //    wizard=tasks/news
                    //    fid=f_7
                    return this.redirectTo("wizard.jsp?proceed=true&sessionkey=" + sessionkey
                            + "&objectnumber=" + command.getParameter(2) 
                            + "&fid=" + command.getFid() 
                            + "&did=" + command.getDid() 
                            + "&origin="+command.getParameter(3) 
                            + "&wizard="+command.getValue()
                            + "&popupid="+this.popupId);
                    
                }
                case WizardCommand.GOTO_FORM: {
                    // The command parameters is the did of the form to jump to.
                    // note that a fid parameter is expected in the command syntax but ignored
                    workspace.doGotoForm(command.getDid());
                    break;
                }
                case WizardCommand.CANCEL: {
                    // This command takes no parameters.
                    //TODO: get previous config
                    returnConfig = SessionDataManager.removeCurrentConfig(sessionData, popupId);
                    if (wizardConfig.getPopupId() != null && !"".equals(wizardConfig.getPopupId())) {
                        // wizardConfig is popup
                        if (returnConfig instanceof WizardConfig) {
                            WizardConfig wizconfig = (WizardConfig)returnConfig;
                            if (wizardConfig.getPopupId().equals(wizconfig.getPopupId())==false) {
                                return closePopup(null,null,false);
                            }
                        }
                    }
                    break;
                }
                case WizardCommand.SAVE: {
                    log.debug("Wizard " + wizardConfig.objectNumber + " will be saved (but not closed)");
                    workspace.doSave();
                    workspace.doLoad();
                    break;
                }
                case WizardCommand.COMMIT: {
                    log.debug("Committing wizard " + wizardConfig.objectNumber);
                    boolean isNew = wizardConfig.wizardData.getStatus()==BaseData.STATUS_NEW;
                    workspace.doSave();
                    returnConfig = SessionDataManager.removeCurrentConfig(sessionData, popupId);
                    if (wizardConfig.getPopupId()!=null && "".equals(wizardConfig.getPopupId())==false) {
                        if (returnConfig instanceof WizardConfig) {
                            WizardConfig wizconfig = (WizardConfig)returnConfig;
                            if (wizardConfig.getPopupId().equals(wizconfig.getPopupId())==false) {
                                //if popup window, shoud send invoke opener.sendcommand
                                //if prevconfig is a wizard config, should invoke add-item/update-item command
                                String sendcmd = null;
                                String objnumber = null;
                                if (isNew) {
                                    sendcmd = "cmd/add-item/"+wizardConfig.parentFid+"/"+wizardConfig.parentDid+"//";
                                    objnumber = wizardConfig.wizardData.getNumber();
                                } else {
                                    sendcmd = "cmd/update-item/"+wizardConfig.parentFid+"/"+wizardConfig.parentDid + 
                                        "/" + wizardConfig.objectNumber+ "/" + wizardConfig.origin+ "/";
                                    objnumber = wizardConfig.wizardData.getNumber();
                                }
                                return closePopup(sendcmd,objnumber,true);
                            }
                        }
                    }
                    if (wizardConfig.parentDid!=null && "".equals(wizardConfig.parentDid)==false) {
                        WizardConfig prevWizardCfg = (WizardConfig)returnConfig;
                        workspace.init(sessionData,prevWizardCfg,cloud);
                        if (isNew) {
                            List numberList = new ArrayList();
                            numberList.add(wizardConfig.wizardData.getNumber());
                            workspace.doAddItem(wizardConfig.parentFid,wizardConfig.parentDid,numberList);
                        } else {
                            workspace.doUpdateItem(wizardConfig.parentFid,
                                    wizardConfig.parentDid,
                                    wizardConfig.objectNumber,
                                    wizardConfig.origin,
                                    wizardConfig.wizardData.getNumber());
                        }
                    }
                    //TODO: if prevconfig is list config, it will return to list
                    break;
                }
                default: {
                    log.warn("Received an unknown wizard command '" + command.getValue() + "'");
                }
            } // switch
            if (returnConfig==null) {
                // use current config
                String refer = sessionData.getBackPage();
                return this.redirectTo(refer);
//                return this.redirectTo("wizard.jsp?proceed=true&sessionkey="+sessionKey+
//                        "&objectnumber="+wizardConfig.objectNumber);
            } else {
                return redirectTo(returnConfig.getPage());
            }
        }
        
    }
    
    private String redirectTo(String uri) {
        assert(uri!=null);
        if (uri.startsWith("http:")==false 
                && uri.startsWith("/")==true) {
            uri = request.getContextPath()+uri;
        }
        log.debug("Redirecting to " + uri);
        uri = response.encodeRedirectURL(uri);
        log.debug("Redirecting to " + uri);
        request.setAttribute(ATTRKEY_REDIRECT_URI,uri);
//      response.sendRedirect(uri);
        return "redirect.jsp";
    }
    
    /**
     * jumped to xslt.jsp to transform the dom object to html page
     * 
     * @param node
     * @param params
     * @param xslFilePath
     * @return
     * @throws WizardException
     */
    private String doXslt(Node node, Map params, String xslFilePath) throws WizardException{
        
        URIResolver uriResolver = sessionData.getUriResolver();
        URL stylesheetFile = null;     
        try {
            stylesheetFile = uriResolver.resolveToURL(xslFilePath, null);
        }
        catch (Exception e) {
            throw new WizardException(e);
        }

        if (stylesheetFile == null) {
            throw new WizardException("Could not resolve XSL " + stylesheetFile + "  with "
                    + uriResolver);
        }
        
        return doXslt(node,params,stylesheetFile);
    }
    
    /**
     * jumped to xslt.jsp to transform the dom object to html page
     * 
     * @param node
     * @param params
     * @param xslFile
     * @return
     * @throws WizardException
     */
    private String doXslt(Node node, Map params, URL xslFile) {
        
        request.setAttribute(ATTRKEY_XSLT_DOCUMENT,node);
        request.setAttribute(ATTRKEY_XSLT_PARAMS,params);
        request.setAttribute(ATTRKEY_XSLT_FILEPATH,xslFile);
        request.setAttribute(ATTRKEY_XSLT_URIRESOLVER,sessionData.getUriResolver());
        
        return "xslt.jsp";
    }
    
    /**
     * 
     * @param commited
     * @return
     */
    private String closePopup(String sendCmd, String objNumbers, boolean isCommited) {
        request.setAttribute("committed",""+isCommited);
        if (isCommited==true) {
            request.setAttribute("sendcmd",sendCmd);
            request.setAttribute("objnumber",objNumbers);
        }
        return "closepopup.jsp";
    }

    /**
     * Fills the given map with all attributes from the URI, but first all attributes from the
     * first call are added.  No arrays supported, only single values.
     * @since MMBase-1.7
     */
    protected void fillAttributes(Map map) {
        if (sessionData!=null && sessionData.getAttributes()!=map) {
            map.putAll(sessionData.getAttributes());  // start with setting in global config
        }
        
        Enumeration enumeration = request.getParameterNames();
        while (enumeration.hasMoreElements()) {
            String param = (String) enumeration.nextElement();
            String value = request.getParameter(param);
            if ("referrer".equals(param)) {
                value = value.replace('\\','/');
            }
            map.put(param, value);
        }
        map.put("ew_context", request.getContextPath());
        // necessary if client doesn't accept cookies to store sessionid (this is appended to urls)
        String sessionId = response.encodeURL("test.jsp").substring(8);
        map.put("sessionid", sessionId);
        map.put("sessionkey", sessionkey);
    }
    
    /**
     * @return Returns the sessionData.
     */
    protected SessionData getSessionData() {
        return sessionData;
    }
    
    /**
     * 
     * @return
     */
    private WizardCommand getRequestCommand(){
        Enumeration enu = request.getParameterNames();
        while(enu.hasMoreElements()) {
            String paramName = (String)enu.nextElement();
            String paramValue = request.getParameter(paramName);
            if (paramName.startsWith("cmd/")) {
                return new WizardCommand(paramName,paramValue);
            }
        }
        return null;
    }
    
    /**
     * In the following two case, create new wizard config object.
     * 1) if wizardConfig's objectnumber is differ with current objectnumber in request
     * 2) create new wizardConfig
     * @return
     * @throws WizardException
     */
    WizardConfig checkWizardConfig(WizardConfig wizardConfig, Cloud cloud) throws WizardException{
        String objectNumber = HttpUtil.getParam(request, "objectnumber");
        if (wizardConfig!=null) {
            if (objectNumber != null && (objectNumber.equals("new") ||
                    objectNumber.equals(wizardConfig.objectNumber)==false)) {
                log.debug("found wizard is for other other object (" + objectNumber + "!= " + wizardConfig.objectNumber + ")");
                //prepare for new wizard
                wizardConfig = null;
            }
        }
        if (wizardConfig==null){
            log.trace("creating new wizard");
            wizardConfig = SessionDataManager.createWizardConfig(sessionData, popupId);
            String page = request.getServletPath()+ "?proceed=true"+"&sessionkey=" + sessionkey;
            if ("".equals(popupId)==false) {
                page += "&popupid="+popupId;
            }
            wizardConfig.setPage(page);
            //determine the objectnumber and assign the wizard name.
            wizardConfig.configure(request, cloud, sessionData.getUriResolver());
            // wizard should now have a name!
            if (wizardConfig.getWizardName() == null) {
                throw new WizardException("Wizardname may not be null, configurated by class with name: " + this.getClass().getName());
            }
        }
        return wizardConfig;
    }
    
}
