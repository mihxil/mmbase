<%@include file="/taglibs.jsp" %>
<mm:cloud logon="admin" pwd="<%= (String) com.finalist.mmbase.util.CloudFactory.getAdminUserCredentials().get("password") %>" method="pagelogon" jspvar="cloud">
<%@include file="includes/templateheader.jsp" %>
<%@include file="includes/cacheparams.jsp" %>
<cache:cache groups="<%= paginaID %>" key="<%= cacheKey %>" time="<%= expireTime %>" scope="application">
<%@include file="includes/calendar.jsp" %>
<%

// this is a special version of the article template which includes news
// - the title of the page is Gesignaleerd
// - articles are saved to the archive page connected to this page by a dreadmore relation
// - the archive page should have an alias with %archief%
String readmoreUrl = "article_info.jsp";
if(!articleId.equals("-1")) { 
    String articleTemplate = "article.jsp" + templateQueryString;
	 response.sendRedirect(articleTemplate);
    %><%--jsp:include page="<%= articleTemplate %>" /--%><%

} else {  

   %><%@include file="includes/header.jsp" 
   %><td><%@include file="includes/pagetitle.jsp" %></td>
     <td><% String rightBarTitle = "Gesignaleerd";
            %><%@include file="includes/rightbartitle.jsp" 
      %></td>
   </tr>
   <tr>
   <td class="transperant">
   <div class="<%= infopageClass %>" id="infopage">
   <table border="0" cellpadding="0" cellspacing="0">
       <tr>
		 	<td style="padding:10px;padding-top:18px;">
         <%@include file="includes/back_print.jsp" %>
			<%@include file="includes/relatedteaser.jsp" %>
			</td>
		</tr>
   </table>
   </div>
   </td><%
   
   // *********************************** right bar *******************************
   %><td><%@include file="includes/whiteline.jsp" 
   %><div class="rightcolumn" id="rightcolumn">
   <table cellpadding="0" cellspacing="0" align="left">
   <tr><td style="padding-bottom:10px;padding-left:19px;padding-right:9px;">
   <%@include file="includes/info/movetoarchive.jsp" 
   %><mm:list nodes="<%= paginaID %>" path="pagina,contentrel,artikel"  searchdir="destination" 
        orderby="artikel.embargo" directions="DOWN"
        constraints="<%= "(artikel.embargo < '" + (nowSec+quarterOfAnHour) + "')" %>"
         ><mm:remove referid="this_article"
         /><mm:node element="artikel" id="this_article"
         /><%@include file="includes/relatedsummaries.jsp" 
   %></mm:list>
   </td></tr>
   </table>
   </div>
   </td><%
} 
%>
<%@include file="includes/footer.jsp" %>
</cache:cache>
</mm:cloud>
