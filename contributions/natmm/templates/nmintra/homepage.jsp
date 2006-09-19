<%@include file="/taglibs.jsp" %>
<mm:cloud logon="admin" pwd="<%= (String) com.finalist.mmbase.util.CloudFactory.getAdminUserCredentials().get("password") %>" method="pagelogon" jspvar="cloud">
<%@include file="includes/templateheader.jsp" %>
<%@include file="includes/calendar.jsp" %>
<%@include file="includes/cacheparams.jsp" %>
<% expireTime = newsExpireTime; %>
<cache:cache groups="<%= paginaID %>" key="<%= cacheKey %>" time="<%= expireTime %>" scope="application">
<% String rightBarTitle = "Smoelenboek"; %>
<%@include file="includes/header.jsp" %>
  <td><%@include file="includes/pagetitle.jsp" %></td>
  <td><%@include file="includes/rightbartitle.jsp"%></td>
</tr>
<tr>
<td class="transperant">
<div class="infopage" id="infopage">
  <table border="0" cellpadding="0" cellspacing="0">
    <tr><td colspan="3"><img src="media/spacer.gif" width="1" height="8"></td></tr>
    <tr><td><img src="media/spacer.gif" width="10" height="1"></td>
      <td>
        <table border="0" cellpadding="0" cellspacing="0">
          <%
          String articleConstraint = (new SearchUtil()).articleConstraint(nowSec,quarterOfAnHour);
          int numberOfMessages = 3;
          %>
          <mm:list nodes="<%= paginaID %>" path="pagina1,readmore,pagina" orderby="readmore.pos">
            <mm:first inverse="true">
              <tr><td><img src="media/spacer.gif" width="1" height="10"></td></tr>
              <tr><td class="black"><img src="media/spacer.gif" width="491" height="1"></td></tr>
              <tr><td><img src="media/spacer.gif" width="1" height="10"></td></tr>
            </mm:first>
            <tr>
              <td>
                <%@include file="includes/info/summary.jsp" %>
              </td>
            </tr>
          </mm:list>
        </table>
      </td>
      <td><img src="media/spacer.gif" width="10" height="1"></td>
    </tr>
  </table>
</div>
</td><% 

// *************************************** right bar *******************************
%>
<td>
  <%@include file="includes/birthday.jsp" %>
  <%@include file="includes/tickertape.jsp" %>
  <br/><br/>
  <%@include file="includes/itemurls.jsp" %>
</td>
<%@include file="includes/footer.jsp" %>
</cache:cache>
</mm:cloud>