<%@include file="/taglibs.jsp" %>
<%@include file="../../includes/image_vars.jsp" %>
<%@include file="../../includes/time.jsp" %>
<mm:cloud jspvar="cloud">
<%
   String rubriekID = request.getParameter("r");
   String styleSheet = request.getParameter("rs");
   String paginaID = request.getParameter("s");
   PaginaHelper ph = new PaginaHelper(cloud);

   String articleConstraint = "(artikel.embargo < '" + (nowSec+quarterOfAnHour) + "') AND "
                              + "(artikel.use_verloopdatum='0' OR artikel.verloopdatum > '" + nowSec + "' )";
										
	String sImageTemplate = "s(195)+part(0,0,195,113)";
%>
<script language="JavaScript">
<!--
function changeImages() {
  for (var i=0; i<changeImages.arguments.length; i+=2) {
    document[changeImages.arguments[i]].src = changeImages.arguments[i+1];
  }
}
// -->
</script>
<mm:node number="channels" notfound="skipbody">
  <div class="headerBar" style="width:100%;">
  		<img src="includes/portal/logo.gif" border="0" style="vertical-align:middle;"/>
		VANDAAG 
  </div>
  <mm:related path="rubriek,parent,rubriek2,posrel,pagina,contentrel,artikel" orderby="artikel.embargo"
             constraints="<%= articleConstraint %>" max="3">
    <mm:field name="rubriek2.naam" jspvar="rubriek2_naam" vartype="String" write="false">
    <mm:field name="artikel.number" jspvar="artikel_number" vartype="String" write="false">
    <mm:field name="pagina.number" jspvar="pagina_number" vartype="String" write="false">
    <mm:first>
      <table>
      <tr>
        <td style="vertical-align:top;">
          <mm:node element="artikel">
            <mm:relatednodes type="images" max="1">
              <mm:import id="first_image"><mm:image template="<%= sImageTemplate %>" /></mm:import>
              <img src="<mm:image template="<%= sImageTemplate %>" />" alt="" name="rollimage" border="0" />
            </mm:relatednodes>
          </mm:node>
        </td>
        <td>
    </mm:first>
          <a href="<%= ph.createItemUrl(artikel_number,pagina_number,null,request.getContextPath()) %>" class="maincolor_link_shorty">
            <span class="colortitle">
              <%= rubriek2_naam.toUpperCase() %>
            </span>
          </a><br/>
          <mm:node element="artikel">
            <a href="<%= ph.createItemUrl(artikel_number,pagina_number,null,request.getContextPath()) %>"
              <mm:relatednodes type="images" max="1">
                onmouseover="changeImages('rollimage', '<mm:image  template="<%= sImageTemplate %>" />'); return true;"
                onmouseout="changeImages('rollimage', '<mm:write referid="first_image"/>'); return true;"
              </mm:relatednodes>
              class="hover"><mm:field name="titel"/></a>
          </mm:node>
          <br/>
          <div class="rule" style="margin-bottom:6px;margin-top:6px;"></div>
    <mm:last>
        </td>
      </tr>
      </table>
    </mm:last>
    </mm:field>
    </mm:field>
    </mm:field>
  </mm:related>

  <div class="headerBar" style="width:100%;">LAATSTE NIEUWS</div>
  <table style="width:100%;">
  <tr>
    <td style="vertical-align:top;">
      <mm:related path="rubriek,parent,rubriek2,posrel,pagina,contentrel,artikel" orderby="artikel.embargo"
                 constraints="<%= articleConstraint %>" offset="3" max="5">
        <mm:field name="rubriek2.naam" jspvar="rubriek2_naam" vartype="String" write="false">
        <mm:field name="artikel.number" jspvar="artikel_number" vartype="String" write="false">
        <mm:field name="pagina.number" jspvar="pagina_number" vartype="String" write="false">
          <a href="<%= ph.createItemUrl(artikel_number,pagina_number,null,request.getContextPath()) %>" class="maincolor_link_shorty">
            <span class="colortitle">
              <%= rubriek2_naam.toUpperCase() %>
            </span>
          </a>
          | <a href="<%= ph.createItemUrl(artikel_number,pagina_number,null,request.getContextPath()) %>" 
               class="hover"><mm:field name="artikel.titel"/></a><br/>
        </mm:field>
        </mm:field>
        </mm:field>
      </mm:related>
      <div class="rule" style="margin-bottom:6px;margin-top:6px;"></div>
      Overzicht laatste 24 uur | Videonieuws | Radionieuws <br/>
    </td>
    <td style="vertical-align:top;">
      <img src="includes/portal/beurs.gif" border="0" />
    </td>
  </tr>
  </table>
</mm:node>
</mm:cloud>