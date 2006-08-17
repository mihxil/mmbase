<mm:node number="component.assessment" notfound="skip">
   <mm:node number="$learnobject" jspvar="nodeLearnObject">
      <mm:related path="descriptionrel,learnblocks" searchdir="destination">
         <a href="#" style="text-decoration:none">
            <% //so that Mozilla thinks the table below is an URL and shows a correct cursor :) %>
            <table border="1" cellpadding="0" cellspacing="0" width="70%" style="color:#FFFFFF;background:#949494;cursor:hand"
               onClick="top.location.href='<mm:url referids="provider,education,class" page="../index.jsp">
                                              <mm:node element="learnblocks">
                                                 <mm:param name="learnobject"><mm:field name="number"/></mm:param>
                                                 <mm:param name="the_only_node_to_show"><mm:field name="number"/></mm:param>
                                                 <mm:param name="return_to"><%= nodeLearnObject.getNumber() %></mm:param>
                                                 <mm:param name="return_to_type"><%= nodeLearnObject.getNodeManager().getName() %></mm:param>
                                              </mm:node>
                                           </mm:url>'">

               <tr>
                  <td width="0px">
                     <mm:node element="learnblocks">
                        <mm:relatednodes type="images" max="1">
                           <img src="<mm:image/>" />
                        </mm:relatednodes>
                     </mm:node>
                  </td>
                  <td width="100%">
                     <div style="padding-left:20px;">
                        <mm:node element="learnblocks">
                           <mm:field name="name"/>
                        </mm:node>
                        <br />
                        <mm:node element="descriptionrel">
                           <mm:field name="description"/>
                        </mm:node>
                     </div>
                  </td>
               </tr>
            </table>
         </a>
      </mm:related>
   </mm:node>
</mm:node>
