<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<mm:cloud>
<mm:import externid="action" />
<mm:import externid="modelfilename" id="prefix" />
<mm:import externid="sub" >none</mm:import>
<mm:import externid="id" >none</mm:import>
<mm:import externid="help" >on</mm:import>
<mm:import externid="name" />
<mm:import externid="package" />

<mm:nodefunction set="mmpb" name="getProjectInfo" referids="name">
        <mm:import id="dir"><mm:field name="dir" /></mm:import>
</mm:nodefunction>

<mm:import id="modelfilename"><mm:write referid="dir" /><mm:write referid="prefix" /></mm:import>


<mm:compare value="addneededbuilder" referid="action">
	<mm:import externid="newbuilder" />
	<mm:import externid="newmaintainer" />
	<mm:import externid="newversion" />
	<mm:booleanfunction set="mmpb" name="addNeededBuilder" referids="modelfilename,newbuilder,newmaintainer,newversion" />
</mm:compare>

<mm:compare value="addneededreldef" referid="action">
	<mm:import externid="newsource" />
	<mm:import externid="newtarget" />
	<mm:import externid="newdirection" />
	<mm:import externid="newguisourcename" />
	<mm:import externid="newguitargetname" />
	<mm:import externid="newbuilder" />
	<mm:booleanfunction set="mmpb" name="addNeededRelDef" referids="modelfilename,newbuilder,newsource,newtarget,newdirection,newguisourcename,newguitargetname" />
</mm:compare>

<mm:compare value="addallowedrelation" referid="action">
	<mm:import externid="newfrom" />
	<mm:import externid="newto" />
	<mm:import externid="newtype" />
	<mm:booleanfunction set="mmpb" name="addAllowedRelation" referids="modelfilename,newfrom,newto,newtype" />
</mm:compare>

</mm:cloud>
