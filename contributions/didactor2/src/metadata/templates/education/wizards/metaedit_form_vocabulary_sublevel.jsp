<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm"%>

<%@page import = "java.util.ArrayList" %>
<%@page import = "java.util.Iterator" %>
<%@page import = "java.util.HashSet" %>
<%@page import = "org.mmbase.bridge.*" %>
<%@page import = "org.mmbase.bridge.util.*" %>

<%@page import = "nl.didactor.metadata.util.MetaDataHelper" %>


<mm:cloud method="delegate" jspvar="cloud">
<%@include file="/shared/setImports.jsp" %>
<%
   String sVocabularyID = (String) request.getParameter("vocabulary");
   String sMetaDefinitionID = (String) request.getParameter("metadefinition");
   NodeList nlRelatedNodes = (NodeList) session.getAttribute("metaeditor_multilevel_metavocabulary_all_metadata");

   Node nodeTemporalRootMetaVocabulary = cloud.getNode(sVocabularyID);

   int iBlockedLevel = 255;
   if("true".equals(request.getParameter("blocked"))){
       iBlockedLevel = 0;
   }
%>



<%
//   Queries.createQuery(cloud, sVocabularyID, "metavocabulary", "", "", "metavocabulary.value", "", "destination", false).

   NodeQuery nodeQuery = Queries.createNodeQuery(nodeTemporalRootMetaVocabulary);

   GrowingTreeList tree = new GrowingTreeList(nodeQuery, 30, nodeTemporalRootMetaVocabulary.getNodeManager(), "related", "destination");
   Query query = tree.getTemplate();
//   query.addSortOrder(new BasicStepField(Step step, new FieldDefs()) , Queries.getSortOrder("UP"));

//   System.out.println(Queries.getSortOrder("UP"));

   TreeIterator it = tree.treeIterator();


   boolean bIsMoreThanOneElement = false;
   if(it.hasNext()){
       it.next();
       if(it.hasNext()){
           bIsMoreThanOneElement = true;
       }
   }


   //TreeIterator returns to already passed nodes when it finishes subbranches
   //So we have to put them in skiplist
   //Otherwise users will see them many times
   HashSet hsetNodesToSkip = new HashSet();

   //nodeTemporalRootMetaVocabulary has been already shown
   hsetNodesToSkip.add(nodeTemporalRootMetaVocabulary);


   ArrayList arliPathToCheckBox = new ArrayList();
   arliPathToCheckBox.add(0, cloud.getNode(sVocabularyID));

   if(bIsMoreThanOneElement){
      it = tree.treeIterator();

      int depth;
      while(it.hasNext()){
         Node nodeMetaVocabulary = it.nextNode();

//         System.out.println(nodeMetaVocabulary.getNumber());

         if(hsetNodesToSkip.contains(nodeMetaVocabulary)){
             //We have returned to the top element from one of the subbranches
             //Do skip it.
             continue;
         }

         depth = it.currentDepth();

         //Adding a new node number to the chain of MetaVocabularies
         //"null" means the end of the tree
         arliPathToCheckBox.add(depth - 2, nodeMetaVocabulary);
         arliPathToCheckBox.add(depth - 1, null);


         //Generating unique checkbox id
         String sCheckBoxUniqueID = new String(sVocabularyID);
         for(Iterator itVocabularyNode = arliPathToCheckBox.iterator(); itVocabularyNode.hasNext();){
            Node nodeMetaVocabularyCheckBox = (Node) itVocabularyNode.next();


            if(nodeMetaVocabularyCheckBox == null){
            //We current branch is over, but the list can have got rubbish from other branches
            //So the "null" is a end-marker.
               break;
            }

            sCheckBoxUniqueID += "_" + nodeMetaVocabularyCheckBox.getNumber();
         }

         //"switchMetaVocabularyTree(this)" is declared in metaedit_form.jsp
         %>
            <mm:node number="<%= "" + nodeMetaVocabulary.getNumber() %>">
               <mm:field name="number" jspvar="sID" vartype="String" write="false">
                  <span style="width:<%= (depth - 1) * 30 %>px;">&nbsp;</span>
                  <input type="checkbox" name="m<%= sMetaDefinitionID %>" value="<%= sID %>" checkbox_id="<%= sCheckBoxUniqueID %>" onClick="switchMetaVocabularyTree(this)"
                     <%
                        if(iBlockedLevel < depth){
                           %> disabled="disabled" <%
                        }

                        if(iBlockedLevel > depth){
                           iBlockedLevel = depth;
                        }
                     %>

                     <mm:relatednodes type="metadata" jspvar="mNode">
                        <%

                           if(nlRelatedNodes.contains(mNode))
                           {
                              %> checked="checked" <%
                               if(iBlockedLevel >= depth){
                                  iBlockedLevel = depth + 1;
                               }
                           }
                        %>
                     </mm:relatednodes>

                  />
                  <mm:field name="number" jspvar="sMetavocabularyID" vartype="String">
                     <mm:node number="$user" jspvar="nodeUser">
                        <%= MetaDataHelper.getAliasForObject(cloud, sMetavocabularyID, nodeUser.getNumber()) %>
                     </mm:node>
                  </mm:field>
               </mm:field>
               <br/>
            </mm:node>
         <%
      }
   }
%>
</mm:cloud>
