<%--
  This template shows the contents of a mailbox using the <di:table> tag.
  A link is created for every email to the 'email.jsp' page, where the user
  can view the email and do other actions.

  TODO:
  - open the 'inbox' always if there is no mailbox given
  - clickthrough to the 'show the email' page
  - finish other functionality mentioned in FO
--%>
<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>
<mm:content postprocessor="reducespace"  expires="0">
<mm:cloud loginpage="/login.jsp" jspvar="cloud">
	<%@include file="/shared/setImports.jsp" %>
	<mm:import externid="mailbox">-1</mm:import>

	<mm:compare referid="mailbox" value="-1">
		<mm:node number="$user">
			<mm:relatednodes id="inbox" type="mailboxes" max="1" orderby="type">
	             <mm:remove referid="mailbox"/>
                 <mm:import id="mailbox"><mm:field name="number"/></mm:import>
  			</mm:relatednodes>
		</mm:node>
	</mm:compare>

    <mm:node number="$mailbox" notfound="skip">
      <mm:relatednodescontainer type="emails">

        <mm:import id="gfx_attachment"><mm:treefile page="/email/gfx/attachment.gif" objectlist="$includePath" referids="$referids" /></mm:import>

        <di:table maxitems="10">
          <di:row>
            <di:headercell><input type="checkbox" onclick="selectAllClicked(this.form,this.checked)"></input></di:headercell>
            <di:headercell><img src="<mm:write referid="gfx_attachment"/>"/></di:headercell>
            <di:headercell sortfield="subject">Onderwerp</di:headercell>
            <di:headercell sortfield="from">Afzender</di:headercell>
            <di:headercell sortfield="date" default="true">Datum</di:headercell>
          </di:row>
          <mm:relatednodes>
            <mm:import id="link">
              <a href="<mm:treefile page="/email/mailbox/email.jsp" objectlist="$includePath" referids="$referids">
                <mm:param name="mailbox"><mm:write referid="mailbox" /></mm:param>
                <mm:param name="email"><mm:field name="number" /></mm:param>
              </mm:treefile>">
            </mm:import>
            <di:row>
              <di:cell><input type="checkbox" name="ids" value="<mm:field name="number"/>"></input></di:cell>
              <di:cell>
                <mm:relatednodes type="attachments">
                  <mm:first><img src="<mm:write referid="gfx_attachment"/>"/></mm:first>
                </mm:relatednodes>
              </di:cell>
              <di:cell><mm:write escape="none" referid="link"/><mm:field name="subject" /></a></di:cell>
              <di:cell><mm:write escape="none" referid="link"/><mm:field name="from" /></a></di:cell>
              <di:cell><mm:write escape="none" referid="link"/><mm:field name="gui(date)" /></a></di:cell>
            </di:row>
            <mm:remove referid="link" />
          </mm:relatednodes>
        </di:table>
      </mm:relatednodescontainer>
    </mm:node>
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


</mm:cloud>
</mm:content>
