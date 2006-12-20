<%@include file="/taglibs.jsp" %>
<%@include file="../../request_parameters.jsp" %>
<mm:cloud jspvar="cloud">
<mm:node number="<%= shop_itemID %>" 
><mm:field name="number" jspvar="products_number" vartype="String" write="false"><% 
	shop_itemHref = ph.createPaginaUrl(paginaID,request.getContextPath()) + "?u=" + products_number;
	%><mm:list nodes="<%= shop_itemID %>" nodes="product,posrel,pagina">
		  <mm:field name="pagina.number" jspvar="pagina_number" vartype="String" write="false">
		  	  <% shop_itemHref += "&p=" + pagina_number; %>
		  </mm:field>
	  </mm:list>	
	  <tr><td rowspan="3" class="bottom" background="<mm:related path="posrel,images" constraints="posrel.pos=1"
						><mm:node element="images"
							><mm:image template="s(100x50)" 
						/></mm:node
					></mm:related>" style="background-position:center bottom;background-repeat:no-repeat;">
		<mm:field name="type"
			><mm:isnotempty
				><mm:compare value="standaard" inverse="true"
				><img style="float:right;" src="media/<mm:write />.gif"></mm:compare
			></mm:isnotempty
		></mm:field
		><a style="display:block;width:100px;height:40px;" href="<mm:url page="<%= shop_itemHref %>" />" ><img src="media/trans.gif" border="0" alt=""></a></td>
		<td colspan="3" style="width:80%;padding:3px;">
			<a href="<mm:url page="<%= shop_itemHref %>" />" class="bold"><mm:field name="title" /></a><br>
			<mm:field name="intro"><mm:isnotempty><mm:write /></mm:isnotempty></mm:field>
		</td></tr>
	<tr><td style="width:35%;padding-left:3px;padding-right:3px;padding-top:2px;"><%@include file="../includes/relatedprice.jsp" %></td>
		<td style="width:5%;"><img src="media/trans.gif" border="0" alt="" width="1" height="1"></td><% 
		shop_itemHref += "&p=bestel";
		%><%@include file="../includes/relatedshoppingcart.jsp"%></tr>
	<tr><td colspan="3"><img src="media/trans.gif" border="0" alt="" width="1" height="6"></td></tr>
	<tr><td class="maincolor" colspan="4"><img src="media/trans.gif" border="0" alt="" width="1" height="1"></td></tr>
</mm:field
></mm:node
></mm:cloud>