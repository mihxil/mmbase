<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:bundle basename="nl.didactor.component.agenda.AgendaMessageBundle">
<mm:content postprocessor="reducespace" expires="0">
<mm:cloud loginpage="/login.jsp" jspvar="cloud">
<%@include file="/shared/setImports.jsp" %>
<%@ page import="java.util.Calendar"%>
<mm:treeinclude page="/cockpit/cockpit_header.jsp" objectlist="$includePath" referids="$referids">
  <mm:param name="extraheader">
    <title><fmt:message key="CALENDAR" /></title>
    <link rel="stylesheet" type="text/css" href="css/agenda.css" />
  </mm:param>
</mm:treeinclude>
<div class="rows">
<div class="navigationbar">
  <div class="titlebar">
	  <img src="<mm:treefile page="/gfx/icon_agenda.gif" objectlist="$includePath" referids="$referids"/>" alt="<fmt:message key="CALENDAR" />"/>
	  <fmt:message key="CALENDAR" />
  </div>
</div>
<%
  Calendar cal = Calendar.getInstance();
  int day = cal.get( Calendar.DATE );
  int month = cal.get( Calendar.MONTH ) + 1;
  int year = cal.get( Calendar.YEAR );

%>
<mm:import externid="year">-1</mm:import>
<mm:import externid="month">-1</mm:import>
<mm:import externid="day">-1</mm:import>
<mm:compare referid="year" value="-1">
 <mm:remove referid="month"/>
 <mm:remove referid="day"/>
 <mm:remove referid="year"/>
  <mm:import id="year"><%=year%></mm:import>
  <mm:import id="month"><%=month%></mm:import>
  <mm:import id="day"><%=day%></mm:import>
</mm:compare>
<div class="folders">
  <div class="folderHeader">
  <fmt:message key="CALENDAR" />
  </div>
  <div class="folderCalendarBody">
    <mm:treeinclude page="/agenda/calendar.jsp" objectlist="$includePath" referids="$referids">
      <mm:param name="year"><mm:write referid="year"/></mm:param>
      <mm:param name="month"><mm:write referid="month"/></mm:param>
      <mm:param name="day"><mm:write referid="day"/></mm:param>
    </mm:treeinclude>

    <p>
  
    <mm:list nodes="$user" path="people1,invitationrel1,items,invitationrel2,people2" constraints="people1.number != people2.number AND invitationrel1.m_status <= 0" max="1">
	<a href="<mm:treefile page="/agenda/appointments.jsp" objectlist="$includePath" referids="$referids"/>"><di:translate id="newappointments">U heeft nieuwe afspraken</di:translate></a>
    </mm:list>
</div>

</div>


<form action="<mm:treefile page="/agenda/deleteagendaitem.jsp" objectlist="$includePath" referids="$referids,year,month,day"/>" method="post">
<input type="hidden" name="callerpage" value="/agenda/index.jsp">

<div class="mainContent">
  <div class="contentHeader">
  <mm:write referid="day"/>/
  <mm:write referid="month"/>/
  <mm:write referid="year"/>
 </div>
	<div class="contentSubHeader">
	<% int count = 0; %>
    <di:hasrole role="student">
      <%count++;%>
    </di:hasrole>
    <di:hasrole role="teacher">
      <%count++;%>
    </di:hasrole>
    <% if ( count > 0 ) { %>
      <%-- personal agenda items --%>
      <a href="<mm:treefile page="/agenda/addagendaitem.jsp" objectlist="$includePath" referids="$referids,year,month,day">
		            <mm:param name="callerpage">/agenda/index.jsp</mm:param>
                    <mm:param name="typeof">1</mm:param>
		    <mm:param name="year"><mm:write referid="year"/></mm:param>
		    <mm:param name="month"><mm:write referid="month"/></mm:param>
		    <mm:param name="day"><mm:write referid="day"/></mm:param>
  		       </mm:treefile>">
        <img src="<mm:treefile page="/agenda/gfx/icon_agenda_item_person.gif" objectlist="$includePath" referids="$referids"/>" border="0" alt="<fmt:message key="ADDPERSONALAGENDAITEM" />"/></a>
    <% }%>

    <%-- workgroup related agenda items --%>
    <mm:node number="$user">
      <%-- there must be at least one workgroup --%>
      <mm:relatednodes type="workgroups" max="1">
        <a href="<mm:treefile page="/agenda/addagendaitem.jsp" objectlist="$includePath" referids="$referids">
  	               <mm:param name="callerpage">/agenda/index.jsp</mm:param>
	               <mm:param name="typeof">3</mm:param>
		    <mm:param name="year"><mm:write referid="year"/></mm:param>
		    <mm:param name="month"><mm:write referid="month"/></mm:param>
		    <mm:param name="day"><mm:write referid="day"/></mm:param>
	             </mm:treefile>">
          <img src="<mm:treefile page="/agenda/gfx/icon_agenda_item_workgroup.gif" objectlist="$includePath" referids="$referids"/>" border="0" alt="<fmt:message key="ADDWORKGROUPAGENDAITEM" />"/></a>
      </mm:relatednodes>
    </mm:node>

    <%-- class related agenda items --%>
    <di:hasrole role="teacher">
      <mm:node number="$user">
        <%-- there must be at least one class --%>
        <mm:relatednodes type="classes" max="1">
          <a href="<mm:treefile page="/agenda/addagendaitem.jsp" objectlist="$includePath" referids="$referids">
			                <mm:param name="callerpage">/agenda/index.jsp</mm:param>
	                        <mm:param name="typeof">2</mm:param>
				<mm:param name="year"><mm:write referid="year"/></mm:param>
				<mm:param name="month"><mm:write referid="month"/></mm:param>
				<mm:param name="day"><mm:write referid="day"/></mm:param>
			       </mm:treefile>">

            <img src="<mm:treefile page="/agenda/gfx/icon_agenda_item_class.gif" objectlist="$includePath" referids="$referids"/>" border="0" alt="<fmt:message key="ADDCLASSAGENDAITEM" />"/></a>
        </mm:relatednodes>
      </mm:node>
    </di:hasrole>

     <%-- classmates related agenda items --%>
      <mm:node number="$user">
        <%-- there must be at least one class --%>
        <mm:relatednodes type="classes" max="1">
          <a href="<mm:treefile page="/agenda/addagendaitem.jsp" objectlist="$includePath" referids="$referids">
			                <mm:param name="callerpage">/agenda/index.jsp</mm:param>
	                        <mm:param name="typeof">4</mm:param>
				<mm:param name="year"><mm:write referid="year"/></mm:param>
				<mm:param name="month"><mm:write referid="month"/></mm:param>
				<mm:param name="day"><mm:write referid="day"/></mm:param>
			       </mm:treefile>">

            <img src="<mm:treefile page="/agenda/gfx/icon_agenda_invitation.gif" objectlist="$includePath" referids="$referids"/>" border="0" alt="<fmt:message key="CREATEINVITATION" />"/></a>
        </mm:relatednodes>
      </mm:node>
	    <a href="appointments.jsp"><di:translate id="listappointments">Bekijk afspraken</di:translate></a>
    
	    <input type="image" src="<mm:treefile page="/agenda/gfx/afspraak verwijderen.gif" objectlist="$includePath" referids="$referids"/>">
    </div>
  <div class="contentBody">
    <mm:treeinclude page="/agenda/agenda.jsp" objectlist="$includePath" referids="$referids">
    <mm:param name="typeof">2</mm:param>
    <mm:param name="year"><mm:write referid="year"/></mm:param>
    <mm:param name="month"><mm:write referid="month"/></mm:param>
    <mm:param name="day"><mm:write referid="day"/></mm:param>
    </mm:treeinclude>
  </div>
</div>
</div>
</form>

<script>

      function selectAllClicked(frm, newState) {
	  if (frm.elements['ids'].length) {
	    for(var count =0; count < frm.elements['ids'].length; count++ ) {
		var box = frm.elements['ids'][count];
		box.checked=newState;
	    }
	  }
	  else {
	      frm.elements['ids'].checked=newState;
	  }
      }

</script>


<mm:treeinclude page="/cockpit/cockpit_footer.jsp" objectlist="$includePath" referids="$referids" />
</mm:cloud>
</mm:content>
</fmt:bundle>

