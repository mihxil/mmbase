<%@page errorPage="error.jsp" language="java" contentType="text/html; charset=utf-8"  import="java.util.Stack"
%><?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "DTD/xhtml1-transitional.dtd">
<%!
void push(Stack stack, String id,  String url) {
   stack.push(new String[] {id, url});
}
void push(Stack stack, String id, HttpServletRequest request) {
   push(stack, id, request.getServletPath() + "?" + request.getQueryString());
}
String peek(Stack stack) {
  return ((String []) stack.peek())[1];
}

String toHtml(Stack stack, HttpServletRequest request) {
  StringBuffer buf = new StringBuffer();
  if (stack.size() < 1) return "";
  String[] info = (String []) stack.get(0);
  buf.append("<a href='" + request.getContextPath() +  info[1] + "'>" + info[0] + "</a>");
  for (int i = 1; i < stack.size(); i++) {
     info = (String []) stack.get(i);
     buf.append("-&gt; <a href='" + request.getContextPath() +  info[1] + "&amp;pop=" + (stack.size() - i) + "'>" + info[0] + "</a>");
  }
  return buf.toString();
}
%>
<% 

response.setContentType("text/html; charset=utf-8");
// as many browsers as possible should not cache:
response.setHeader("Cache-Control", "no-cache");
response.setHeader("Pragma","no-cache");
long now = System.currentTimeMillis();
response.setDateHeader("Expires",  now);
response.setDateHeader("Last-Modified",  now);
response.setDateHeader("Date",  now);


Stack urlStack = (Stack) session.getAttribute("editor_stack");

if (urlStack == null) {
   urlStack = new Stack();
   session.setAttribute("editor_stack", urlStack);
}


%><html>
<head>
<link rel="icon" href="images/favicon.ico" type="image/x-icon" />
<link rel="shortcut icon" href="images/favicon.ico" type="image/x-icon" />
<%@ page import="org.mmbase.bridge.*"
%><%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0"  prefix="mm"
%>
<mm:import externid="pop" />
<mm:import externid="push" />
<mm:import externid="nopush" />
<mm:import externid="clearstack" />
<mm:present referid="clearstack">
  <% urlStack.clear(); %>
</mm:present>

<mm:notpresent referid="nopush">
  <mm:present referid="push">
    <mm:write referid="push" jspvar="v" vartype="string">
      <% push(urlStack, v, request); %>
    </mm:write>
  </mm:present>
</mm:notpresent>
<mm:present referid="pop">
  <mm:write referid="pop" jspvar="pop" vartype="integer">
    <% for (int i = 0; i < pop.intValue(); i++) urlStack.pop(); %>
  </mm:write>
</mm:present>

<mm:import id="config" externid="mmeditors_config" from="session" />

<mm:context id="config" referid="config">
  <mm:import externid="page_size" from="parameters,this">20</mm:import>
  <mm:import id="style_sheet" externid="mmjspeditors_style"     from="parameters,cookie,this">mmbase.css</mm:import>
  <mm:import id="liststyle"   externid="mmjspeditors_liststyle" from="parameters,cookie,this">short</mm:import>  
  <mm:import id="lang"        externid="mmjspeditors_language"  from="parameters,cookie,this" ><%=LocalContext.getCloudContext().getDefaultLocale().getLanguage()%></mm:import>
  <mm:import id="method"      externid="mmjspeditors_method"    from="parameters,cookie,this" >loginpage</mm:import>
  <mm:import id="session"     externid="mmjspeditors_session"   from="parameters,cookie,this">mmbase_editors_cloud</mm:import>
</mm:context>

<mm:write referid="config" session="mmeditors_config" />

<% java.util.ResourceBundle m = null; // short var-name because we'll need it all over the place
   java.util.Locale locale = null; %>
<mm:write referid="config.lang" jspvar="lang" vartype="string">
<%
  locale  =  new java.util.Locale(lang, "");
  m = java.util.ResourceBundle.getBundle("org.mmbase.applications.jsp.editors.editors", locale);
%>
</mm:write>
<mm:import id="style"><style type="text/css">@import url(css/<mm:write referid="config.style_sheet" />);</style></mm:import>