<%@page import="nl.leocms.servlets.UrlConverter" %>
<%@include file="/taglibs.jsp" %>
<html>
<head>
   <link href="<mm:url page="<%= editwizard_location %>"/>/style/color/wizard.css" type="text/css" rel="stylesheet"/>
   <link href="<mm:url page="<%= editwizard_location %>"/>/style/layout/wizard.css" type="text/css" rel="stylesheet"/>
</head>
<body style="overflow:auto;">
   <cache:flush scope="application"/>
   <% // UrlConverter.getCache().flushAll(); %>
   <h3>Publiceren van alle pagina's</h3>
   <b>Alle pagina zijn gepubliceerd.</b><br>
</body>
</html>
