<%@ page errorPage="exception.jsp"
%><%@ include file="settings.jsp"%><mm:locale language="<%=ewconfig.language%>"><mm:cloud method="$loginmethod"  loginpage="login.jsp" jspvar="cloud"><mm:log jspvar="log"><%@page import="org.mmbase.bridge.*,javax.servlet.jsp.JspException"
%><%@ page import="org.w3c.dom.Document"
%><%
    /**
     * list.jsp
     *
     * @since    MMBase-1.6
     * @version  $Id: list.jsp,v 1.39 2003-06-10 15:28:20 michiel Exp $
     * @author   Kars Veling
     * @author   Michiel Meeuwissen
     * @author   Pierre van Rooden
     */

log.trace("list.jsp");


Config.ListConfig listConfig = null; // stores the configuration specific for this list.
Config.SubConfig    top = null;

if (! ewconfig.subObjects.empty()) {
    top  = (Config.SubConfig) ewconfig.subObjects.peek();
    if (! popup) { 
        log.debug("This is not a popup");
        if (top instanceof Config.ListConfig) {
            listConfig = (Config.ListConfig) top;
        } else {
            log.debug("not a list on the stack?");
        }
    
    } else {
        log.debug("this is a popup");
        Stack stack = (Stack) top.popups.get(popupId);
        if (stack == null) {
            log.debug("No configuration found for popup list");
            stack = new Stack();
            top.popups.put(popupId, stack);
            listConfig = null;
        } else {
            if (stack.empty()) {
                log.error("Empty stack?");
            } else {                
                listConfig = (Config.ListConfig) stack.peek();
            }
        }
    }
} else {
    log.debug("nothing found on stack");
    if (popup) {
        throw new WizardException("Popup without parent");
    }
}

if (listConfig == null) {
    listConfig = configurator.createList(cloud);
    if (! popup) {       
        if (log.isDebugEnabled()) log.trace("putting new list on the stack for list " + listConfig.title);
        ewconfig.subObjects.push(listConfig);
    } else {
        if (log.isDebugEnabled()) log.trace("putting new list in popup map  for list " + listConfig.title);
        Stack stack = (Stack) top.popups.get(popupId);
        stack.push(listConfig);
    }
}

configurator.config(listConfig); // configure the thing, that means, look at the parameters.

if (listConfig.age > -1) {
    // maxlistConfig.age is set. pre-query to find objectnumber
    long daymarker = (new java.util.Date().getTime() / (60*60*24*1000)) - listConfig.age;

    NodeManager mgr = cloud.getNodeManager("daymarks");

    NodeList tmplist = mgr.getList("daycount>="+daymarker, null,null);
    String ageconstraint = "";
    if (tmplist.size()<1) {
        // not found. No objects can be found.
        ageconstraint = "number>99999";
    } else {
        Node n = tmplist.getNode(0);
        ageconstraint = "number>"+n.getStringValue("mark");
    }

    if (listConfig.multilevel) ageconstraint=listConfig.mainObjectName+"."+ageconstraint;

    if (listConfig.constraints == null || listConfig.constraints.equals("")) {
        listConfig.constraints = ageconstraint;
    } else {
        listConfig.constraints = "(" + listConfig.constraints+") AND " + ageconstraint;
    }
}


boolean deletable = false;
boolean creatable = false;
String deletedescription = "";
String deleteprompt = "";
org.w3c.dom.NodeList titles = null;
if (listConfig.wizard != null) {

    Wizard wiz = null;
    wiz = new Wizard(request.getContextPath(), ewconfig.uriResolver, listConfig.wizard, null, cloud);
    deletable = (Utils.selectSingleNode(wiz.getSchema(), "/*/action[@type='delete']")!=null);
    creatable = (Utils.selectSingleNode(wiz.getSchema(), "/*/action[@type='create']")!=null);
    
    deletedescription = Utils.selectSingleNodeText(wiz.getSchema(), "/*/action[@type='delete']/description", null, cloud);
    deleteprompt = Utils.selectSingleNodeText(wiz.getSchema(), "/*/action[@type='delete']/prompt", null, cloud);
    titles            = Utils.selectNodeList(wiz.getSchema(), "/wizard-schema/title");

}

// fire query
NodeList results;

// do not list anything if search is forced and no searchvalue given
if (listConfig.forceSearch && listConfig.searchFields!=null && "".equals(listConfig.searchValue)) {
    results = cloud.getCloudContext().createNodeList();    
} else if (listConfig.multilevel) {
    log.trace("this is a multilevel");
    results = cloud.getList(listConfig.startNodes, listConfig.nodePath, listConfig.fields, listConfig.constraints,
                            listConfig.orderBy,
                            listConfig.directions, "both",
                            listConfig.distinct);
} else {
    log.trace("This is not a multilevel. Getting nodes from type " + listConfig.nodePath);
    NodeManager mgr = cloud.getNodeManager(listConfig.nodePath);
    log.trace("directions: " + listConfig.directions);
    results = mgr.getList(listConfig.constraints, listConfig.orderBy, listConfig.directions);
}

log.trace("Got " + results.size() + " results");

int start = listConfig.start;
int len        = listConfig.pagelength;
int maxpages   = listConfig.maxpagecount;

if (start>results.size()-1) start = results.size()-1;
if (start<0) start=0;
int end = len+start;
if (end > results.size()) end = results.size();

// place all objects
String s = "<list count=\"" + results.size() + "\" />";
Document doc = Utils.parseXML(s);

log.trace("Create document");
org.w3c.dom.Node docel = doc.getDocumentElement();

if (titles != null) {
   Document owner = docel.getOwnerDocument();
   for (int i = 0; i < titles.getLength(); i++) {
       docel.appendChild(owner.importNode(titles.item(i),  true));
   }
}

log.trace("hoi");

String mainManager=listConfig.mainObjectName;
if (mainManager.charAt(mainManager.length()-1)<='9') mainManager=mainManager.substring(0,mainManager.length()-1);

NodeManager manager=cloud.getNodeManager(mainManager);
if (!manager.mayCreateNode()) creatable=false;

for (int i=start; i< end; i++) {
    Node item = results.getNode(i);
    org.w3c.dom.Node obj;
    if (listConfig.multilevel) {
        obj = addObject(docel, item.getIntValue(listConfig.mainObjectName+".number"), i+1,
                                         mainManager, manager.getGUIName(2));
    } else {
        obj = addObject(docel, item.getNumber(), i+1, mainManager, manager.getGUIName(2));
    }
    for (int j=0; j < listConfig.fieldList.size(); j++) {
        String fieldname = (String)listConfig.fieldList.get(j);
        String fieldguiname = fieldname;
        if (listConfig.multilevel) {
            int period=fieldname.indexOf('.');
            String nmname=fieldname.substring(0,period);
            if (nmname.charAt(period-1)<='9') nmname=nmname.substring(0, period-1);
            fieldguiname=cloud.getNodeManager(nmname).getField(fieldname.substring(period+1)).getGUIName();
        } else {
            fieldguiname=item.getNodeManager().getField(fieldname).getGUIName();
        }
        addField(obj, fieldguiname, item.getStringValue("gui(" + fieldname + ")"));
    }
    if (listConfig.multilevel) {
        item=item.getNodeValue(listConfig.mainObjectName);
    }
    Utils.setAttribute(obj, "mayedit", ""+item.mayWrite());
    Utils.setAttribute(obj, "maydelete", ""+item.mayDelete());
}


// place page information
int pagecount = new Double(Math.floor(results.size() / len)).intValue();
if (results.size() % len>0) pagecount++;
int currentpage = new Double(Math.floor((start / len))).intValue();

org.w3c.dom.Node pages = doc.createElement("pages");
Utils.setAttribute(pages, "count", pagecount+"");
Utils.setAttribute(pages, "currentpage", (currentpage+1)+ "");
docel.appendChild(pages);

if (pagecount>maxpages) {
    Utils.setAttribute(pages, "showing", maxpages+"");
}

for (int i = 0; i<pagecount && i<maxpages; i++) {
    org.w3c.dom.Node pagenode = doc.createElement("page");
    Utils.setAttribute(pagenode, "start", (i*len)+"");
    Utils.setAttribute(pagenode, "current", (i==currentpage)+"");
    Utils.setAttribute(pagenode, "previous", (i==currentpage-1)+"");
    Utils.setAttribute(pagenode, "next",  (i==currentpage+1)+"");
    pages.appendChild(pagenode);
}

log.trace("Setting xsl parameters");
java.util.Map params = listConfig.getAttributes();

params.put("start",      String.valueOf(start));
params.put("referrer",   ewconfig.backPage);
if (ewconfig.templates != null) params.put("templatedir",  ewconfig.templates);
params.put("len",        String.valueOf(len));
params.put("sessionkey", ewconfig.sessionKey);
params.put("sessionid",  ewconfig.sessionId);
params.put("deletable",  deletable+"");
params.put("creatable",  creatable+"");
params.put("cloud",  cloud);
params.put("popupid",  popupId);

if (deletedescription!=null) params.put("deletedescription", deletedescription);
if (deleteprompt!=null) params.put("deleteprompt", deleteprompt);
if (listConfig.title == null) {
    params.put("title", manager.getGUIName(2));
}
                  
params.put("username", cloud.getUser().getIdentifier());
params.put("language", cloud.getLocale().getLanguage());
params.put("ew_context", request.getContextPath());
params.put("ew_path", new File(request.getServletPath()).getParentFile().getParent() + "/");

log.trace("Doing the transformation for " + listConfig.template);
Utils.transformNode(doc, listConfig.template, ewconfig.uriResolver, out, params);

if (log.isDebugEnabled()) log.trace("ready: " + ewconfig.subObjects);

%><%!

private org.w3c.dom.Node addObject(org.w3c.dom.Node el, int number, int index, String type, String guitype) {
    org.w3c.dom.Node n = el.getOwnerDocument().createElement("object");
    Utils.setAttribute(n, "number", ""+number);
    Utils.setAttribute(n, "index", ""+index);
    Utils.setAttribute(n, "type", type);
    Utils.setAttribute(n, "guitype", guitype);
    el.appendChild(n);
    return n;

}

private org.w3c.dom.Node addField(org.w3c.dom.Node el, String name, String value) {
    org.w3c.dom.Node n = el.getOwnerDocument().createElement("field");
    Utils.setAttribute(n, "name", name);
    Utils.storeText(n, value);
    el.appendChild(n);
    return n;
}
%></mm:log></mm:cloud></mm:locale>
