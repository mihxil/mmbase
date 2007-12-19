<%@include file="globals.jsp" %>

<cmscedit:head title="reactions.title">
	<fmt:message key="globalstats.title" />
</cmscedit:head>

<jsp:useBean id="globalOverviewBean" scope="request" type="com.finalist.newsletter.module.bean.GlobalOverviewBean" />

<table width="50%">
	<tr>
		<td><fmt:message key="globalstats.total.newsletters" /></td>
		<td><jsp:getProperty name="globalOverviewBean" property="numberOfNewsletters"  /></td>
	</tr>
	<tr>
		<td><fmt:message key="globalstats.total.themes" /></td>
		<td><jsp:getProperty name="globalOverviewBean" property="numberOfThemes"  /></td>
	</tr>
	<tr>
		<td><fmt:message key="globalstats.total.publications" /></td>
		<td><jsp:getProperty name="globalOverviewBean" property="numberOfPublications"  /></td>
	</tr>
	<tr>
		<td><fmt:message key="globalstats.total.subscriptions" /></td>
		<td><jsp:getProperty name="globalOverviewBean" property="numberOfSubscribtions"  /></td>
	</tr>
</table>