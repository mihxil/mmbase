<td class="listItem">
  <mm:field name="problems.name" jspvar="dummy" vartype="String" write="false">
    <%= ( "".equals(dummy) ? "&nbsp;" : dummy )%>
  </mm:field>
</td>
<mm:node number="assessment.education" notfound="skip">
  <mm:relatednodes type="learnblocks" path="posrel,learnblocks" orderby="posrel.pos">
<%
    int rating = -1; // not rated
    String problemConstraint = "problems.number=" + problem_number;
%>
    <mm:related path="posrel,problems" constraints="<%= problemConstraint %>">
      <mm:field name="posrel.pos" jspvar="problem_weight" vartype="Integer" write="false">
<%
        try {
          rating = problem_weight.intValue();
        }
        catch (Exception e) {
        }
%>
      </mm:field>
    </mm:related>
    <td class="listItem">
<%
      if (rating < 0) {
        %>&nbsp;<%
      } else {
        %><img src="<mm:treefile page="<%= "/assessment/gfx/icon_rating_" + rating + ".gif"%>" objectlist="$includePath" 
               referids="$referids"/>" border="0" title="<%=problemWeights[rating] %>" alt="<%=problemWeights[rating] %>" /><%
      }
%>
    </td>
  </mm:relatednodes>
</mm:node>