<%@ include file="inc/top.jsp" %><mm:cloud><html><head>	<title>edittag - example 3</title><%@ include file="inc/head.jsp" %></head><body><%@ include file="inc/nav.jsp" %><h4>3de voorbeeld met edittag</h4><p>Een artikel met gerelateerde plaatjes en links. En hier is aan een plaatje ook weer een persoon gerelateerd.Plus het aloude bekende magazine met gerelateerde newsitems</p><mm:edit editor="yammeditor.jsp" icon="/mmbase/edit/my_editors/img/mmbase-edit.gif">	<mm:node number="artikel">	  <h2>[<mm:field name="number" id="nr" />] <mm:field name="title" /></h2>	  <div class="intro"><mm:field name="intro" /></div>	  <mm:list nodes="$nr" path="news,posrel,images" 	  	fields="posrel.pos" orderby="posrel.pos">		<mm:node element="images">		  <em><mm:field name="gui()" /><br />		  [<mm:field name="number" />] <mm:field name="title" /></em>		  <mm:related path="people" fields="people.firstname">(van: <mm:field name="people.firstname" />)</mm:related><br />		</mm:node>	  </mm:list>	  <div><mm:field name="body" />	  <mm:related path="posrel,urls"	  	  fields="urls.url,posrel.pos"	  	  orderby="posrel.pos">	  	  <mm:first><br /><strong>Links:</strong></mm:first>	  	  <a href="<mm:field name="urls.url" />"><mm:field name="urls.name" /></a>	  	  <mm:last inverse="true">,</mm:last>	  </mm:related>	  </div>	</mm:node>	<h3>Magazine</h3>	<mm:related nodes="default.mags" path="posrel,news,people"		fields="news.number,news.title,news.subtitle,people.email,posrel.pos" orderby="posrel.pos">		[<mm:field name="news.number" />] <strong><mm:field name="news.title" /></strong><br />		by <mm:field name="people.email" /><br />	</mm:related></mm:edit></body></html></mm:cloud>