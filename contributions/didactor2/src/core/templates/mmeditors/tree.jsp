<mm:relatednodescontainer type="object" searchdirs="$searchdir">
  <mm:tree type="object" searchdir="$searchdir" maxdepth="8">
    <mm:grow>
      <ul><mm:onshrink></ul></mm:onshrink>
    </mm:grow>
    <li>
      <mm:nodeinfo type="guitype" />: <mm:field id="node_number" name="number" /> <mm:function name="gui" escape="none" />
      <span class="navigate" style="height:2ex;">
        <a href="<mm:url referids="node_number,node_number@push,nopush?" page="change_node.jsp"/>">
           <span class="change"></span><span class="alt">[change]</span>  
         </a>
         <a href="<mm:url referids="node_number"  />">
            <span class="tree"></span><span class="alt">[tree]</span>  
          </a>
      </span>
    <mm:onshrink></li></mm:onshrink>

    <mm:shrink />

  </mm:tree>
</mm:relatednodescontainer>