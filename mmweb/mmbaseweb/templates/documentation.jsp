<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<%@page language="java" contentType="text/html; charset=iso8859-1" session="true"%>
<mm:cloud>
<%@include file="/includes/getids.jsp" %>
<%@include file="/includes/header.jsp" %>
<td class="white" colspan="2" valign="top">
  <h2>Documentation</h2>
  <mm:list nodes="$page" path="pages1,posrel,pages2,urls" orderby="posrel.pos" directions="UP" searchdir="destination">
  <a href="<mm:field name="urls.url"/>" target="docs"><mm:field name="urls.name"/></a><br />
  </mm:list>
  <h2>ApiDocs</h2>
  <mm:list nodes="$page" path="pages1,pages2,posrel,pages3,urls" orderby="posrel.pos" directions="UP" searchdir="destination">
  <a href="<mm:field name="urls.url"/>" target="docs"><mm:field name="urls.name"/></a><br />
  </mm:list>

</td>

<%@include file="/includes/footer.jsp" %>
</mm:cloud>
