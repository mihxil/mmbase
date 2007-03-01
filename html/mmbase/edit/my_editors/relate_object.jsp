<%@ include file="inc/top.jsp" %>
<mm:content type="text/html" escaper="none" expires="0">
<mm:cloud jspvar="cloud" method="loginpage" loginpage="login.jsp" rank="$rank">
<mm:import externid="ntype" />
<mm:import externid="nr" />
<mm:import externid="rnr" />
<mm:import externid="rkind" /> 
<mm:import externid="dir" /> 
<mm:import externid="pagetitle">Relate with <mm:present referid="ntype"><mm:write referid="ntype" /></mm:present> node</mm:import>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="nl">
<head>
  <%@ include file="inc/head.jsp" %>
</head>
<body>
<div id="frame">
<%@ include file="inc/pageheader.jsp" %>
<div id="sidebar">
  <div class="padsidebar">
    
  <table border="0" cellspacing="0" cellpadding="3" id="back">
  <thead>
    <tr>
      <th class="name"> 
        <a href="<mm:url page="edit_object.jsp" referids="nr" />"><img src="img/mmbase-edit.png" alt="edit" width="21" height="20" /></a>
      </th>
      <th>
        <a href="<mm:url page="edit_object.jsp" referids="nr" />">Back</a> to editing <strong><mm:node referid="nr"><mm:nodeinfo type="type" /></mm:node></strong> object
      </th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>&nbsp;</td>
      <td>
        <mm:node referid="nr">
          <mm:fieldlist type="list">
            <mm:fieldinfo type="guiname" /> 
            <mm:first><strong></mm:first><mm:fieldinfo type="guivalue" /><mm:first></strong></mm:first><br />
          </mm:fieldlist>
        </mm:node>
      </td>
    </tr>
  </tbody>
  </table>
  
  </div><!-- / .padsidebar -->
</div><!-- / #sidebar -->
<div id="content">
  <div class="padcontent">
  
  <%@ include file="inc/relating.jsp" %>
  
  <mm:notpresent referid="rnr">
    <!-- ### results ### --><%@ include file="inc/searchresults.jsp" %><!-- ### /results ###-->
    <!-- ### search ### --><%@ include file="inc/searchbox.jsp" %><!-- ### /search ### -->
  </mm:notpresent>
  
  </div><!-- / .padcontent -->
</div><!-- / #content -->
<%@ include file="inc/footer.jsp" %>
</div><!-- / #frame -->
</body>
</html>
</mm:cloud>
</mm:content>
