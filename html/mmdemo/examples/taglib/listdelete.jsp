<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<mm:cloud name="mmbase" method="http">
	<mm:import externid ="deletenumber" />
	<mm:import externid ="nodemanager" />
	

<html>

<head>
  <title>Taglib examples</title>
  <link rel="stylesheet" type="text/css" href="style.css" />
</head>

<body>

<%@ include file="menu.jsp"%>

<h1>List &amp; delete</h1>

<p>
  We demonstrate a simple editor here.
</p>

<%@include file="codesamples/listdelete.jsp.1" %>


<hr />
<p>
It was implemented like this:
</p>
<pre><mm:formatter format="escapexml"><mm:include page="codesamples/listdelete.jsp.1" /></mm:formatter></pre>

</body>

</html>

</mm:cloud>
