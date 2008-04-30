<%@page language="java" contentType="text/html;charset=utf-8"%>
<%@include file="../../globals.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="portlets-unsubscribe" scope="request" />
<html:html xhtml="true">
<% 
String userId = request.getParameter("userId");
String newsletterId = request.getParameter("newsletterId");
%>
<form action="?">
		<fmt:message key="unsubscribe.page.question" /><br/>
	   	<html:submit property="remove"><fmt:message key="unsubscribe.yes" /></html:submit>&nbsp;
	   	<html:submit property="cancel"><fmt:message key="unsubscribe.no" /></html:submit>
		<html:hidden property="userId" value="<%=userId%>" />
        <html:hidden property="newsletterId" value="<%=newsletterId%>" />
</form>
</html:html>