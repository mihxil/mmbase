<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm"%>
<mm:cloud method="logout" />
<mm:redirect page="/mmeditors/" />
<%--<%@ include file="page_base.jsp" 
%><mm:content type="text/html" language="$config.lang" expires="0">
<mm:cloud jspvar="cloud" sessionname="$config.session" method="asis">
  <mm:import id="userlogon"><%= "" + cloud.getUser().getIdentifier() %> </mm:import>
  <mm:log><%= "" + cloud.getUser().getIdentifier() %> </mm:log>
</mm:cloud>
<mm:notpresent referid="userlogon">
  <mm:import id="userlogon">--</mm:import>
</mm:notpresent>
<mm:compare referid="userlogon" value="anonymous">
  <mm:remove referid="userlogon" />
  <mm:import id="userlogon" />
</mm:compare>

<mm:cloud method="logout" sessionname="$config.session" />
<mm:write referid="style" escape="none" />
<title>Logging out</title>
</head>
<mm:log />
<body class="basic"><% 
request.getSession().invalidate(); // start all over again %>
<h2>You were logged out. </h2>
<hr />
<form action="<mm:url page="search_node.jsp" />">
  <input type="submit" name="back" value="back to editors" />
  As: <input type="text" name="userlogon" value="<mm:write referid="userlogon" />" />
</form>
<%@ include file="footfoot.jsp"  %>
</mm:content>--%>