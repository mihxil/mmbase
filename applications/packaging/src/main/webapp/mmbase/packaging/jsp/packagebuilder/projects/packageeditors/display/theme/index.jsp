<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<mm:cloud>
<%@ include file="../../../../thememanager/loadvars.jsp" %>
<mm:import externid="main" />
<mm:import externid="sub" />
<mm:import externid="name" />
<mm:import externid="mode" />
<mm:import externid="name" id="project" />
<mm:import externid="package" />
<mm:import externid="package" id="target" />
<table cellpadding="0" cellspacing="0" class="list" style="margin-top : 10px;" width="65%">
<tr>
	<th colspan="3">
	Display/Theme Package Settings
	</th>
</tr>
<tr>
	<form action="<mm:url page="index.jsp" referids="main,sub,name,package,mode" />" method="post">
	<input type="hidden" name="action" value="setpackagevalue" />
	<th width="100">BaseName</ht>
	 <mm:import id="nid" reset="true">themename</mm:import>
	<td><input name="newvalue" style="width: 98%" value="<mm:function set="mmpb" name="getPackageValue" referids="project,target,nid@name" />"></td>
	<input type="hidden" name="newname" value="<mm:write referid="nid" />" />
	<td width="50"><input type="submit" value="save"></td>
	</form>
</tr>


</mm:cloud>
