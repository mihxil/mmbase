 <mm:hasrank minvalue="administrator">
   <p><a href="#" onclick="create()" style="background:url(<cmsc:staticurl page='/editors/gfx/icons/new.png'/>) left no-repeat;padding-left:20px;"><fmt:message key="community.preference.new" /></a></p>
 </mm:hasrank>
 <table border="0" >  
   <tr>
      <td style="width:103px"><fmt:message key="community.preference.user" /></td>
      <td><html:text style="width: 200px" property="userId"/></td>
   </tr>
   <tr>
      <td style="width:103px"><fmt:message key="community.preference.module" /></td>
      <td><html:text style="width: 200px" property="module"/></td>
   </tr>
   <tr>
      <td style="width:103px"><fmt:message key="community.preference.key" /></td>
      <td><html:text style="width: 200px" property="key"/></td>
   </tr>
   <tr>
      <td style="width:103px"><fmt:message key="community.preference.value" /></td>
      <td><html:text style="width: 200px" property="value"/></td>
   </tr>
   <tr>
   <td></td>
   <td><input type="button" name="submitButton"  
            value="<fmt:message key="community.preference.submit" />" onclick="search()" class="button"/>   
  </td>
</tr>
</table>