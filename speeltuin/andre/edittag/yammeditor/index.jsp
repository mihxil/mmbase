<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "DTD/xhtml1-strict.dtd"><%@ page language="java" contentType="text/html;charset=utf-8"    import="java.util.*,org.mmbase.bridge.*,org.mmbase.storage.search.*"%><%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %><html><head>	<title>edittag</title><%@ include file="inc/head.jsp" %></head><body><%@ include file="inc/nav.jsp" %><mm:cloud><div id="inhoud"><mm:node number="artikel">  <mm:nodeinfo type="query" write="false" jspvar="query" vartype="Object">  <%  Query q = (Query)query;    java.util.List fl = q.getFields();  Iterator e = fl.iterator();  while (e.hasNext()) {  	StepField sf = (StepField)e.next();  	out.println("<br /><em>sf:</em> " + sf);  	String fn = sf.getFieldName();  	out.println("<br /><em>fn:</em> " + fn);  }  %>    <hr /><em>query:</em> <%= query %>  </mm:nodeinfo>  <hr /><h4>Wat hebben we</h4><ul compact="compact" type="disc">	<li>Step</li>	<li>StepField</li>	<li>RelationStep</li>	<li>SortOrder</li>	<li>Constraint</li>	<li>CompositeConstraint</li></ul>  <ol>  <mm:relatednodes type="urls">    <mm:first><h4>urls gerelateerd aan news</h4></mm:first>  	<li><strong><mm:field name="url" /></strong>  	<mm:nodeinfo type="query" write="false" jspvar="query" vartype="Object">	<%	Query q = (Query)query;	java.util.List fl = q.getFields();	Iterator fe = fl.iterator();	while (fe.hasNext()) {	  StepField sf = (StepField)fe.next();	  out.println("<br /><em>stepfield:</em> " + sf);	}	java.util.List sl = q.getSteps();	Iterator e = sl.iterator();	while (e.hasNext()) {	  Step step = (Step)e.next();	  out.println("<br /><em>step:</em> " + step);	  out.println("<br /><em>step alias:</em> " + step.getAlias());	  out.println("<br /><em>step tabel:</em> " + step.getTableName());	  	  SortedSet sset = step.getNodes();	  if (!sset.isEmpty()) { out.println("<br /><em>node:</em> " + sset.first()); }	}  	%>    <hr /><em>query:</em> <%= query %>  	</mm:nodeinfo>  	</li>  </mm:relatednodes>  </ol></mm:node><hr />news met gerelateerde urls:<mm:related nodes="artikel" path="urls"	fields="urls.url">	<mm:node element="urls">	<p><mm:field name="url" /></p>  	<mm:nodeinfo type="query" write="false" jspvar="query" vartype="Object">	<%	Query q = (Query)query;	%>	<%= query %>  	</mm:nodeinfo>	</mm:node></mm:related><hr />listnodes<br /><mm:listnodes type="images" max="1">  	<mm:nodeinfo type="query" write="false" jspvar="query" vartype="Object">	<%	Query q = (Query)query;	java.util.List fl = q.getFields();	Iterator fe = fl.iterator();	while (fe.hasNext()) {	  StepField sf = (StepField)fe.next();	  out.println("<br /><em>stepfield:</em> " + sf);	}	java.util.List sl = q.getSteps();	Iterator e = sl.iterator();	while (e.hasNext()) {	  Step step = (Step)e.next();	  out.println("<br /><em>step:</em> " + step);	  out.println("<br /><em>step alias:</em> " + step.getAlias());	  out.println("<br /><em>step tabel:</em> " + step.getTableName());	  	  SortedSet sset = step.getNodes();	  if (!sset.isEmpty()) { out.println("<br /><em>node:</em> " + sset.first()); }	}	%>	<br /><%= query %>  	</mm:nodeinfo></mm:listnodes></div></mm:cloud></body></html>