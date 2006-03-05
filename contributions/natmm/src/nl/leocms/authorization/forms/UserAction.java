/*
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is LeoCMS.
 *
 * The Initial Developer of the Original Code is
 * 'De Gemeente Leeuwarden' (The dutch municipality Leeuwarden).
 *
 * See license.txt in the root of the LeoCMS directory for the full license.
 */
package nl.leocms.authorization.forms;

import java.util.Map;

import nl.leocms.authorization.AuthorizationHelper;

import org.apache.log4j.Category;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForm;
import org.mmbase.bridge.Cloud;
import org.mmbase.bridge.Node;

import com.finalist.mmbase.util.CloudFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * LoginInitAction
 *
 * @author Edwin van der Elst
 * @version $Revision: 1.1 $, $Date: 2006-03-05 21:43:58 $
 *
 * @struts:action name="UserForm"
 *                path="/editors/usermanagement/UserAction"
 *                scope="request"
 *                validate="true"
 *                input="/editors/usermanagement/user.jsp"
 *
 * @struts:action-forward name="success" path="/editors/usermanagement/userlist.jsp"
 */
public class UserAction extends Action {
   transient Category log = Category.getInstance(this.getClass());

   /**
    * The actual perform function: MUST be implemented by subclasses.
    *
    * @param mapping
    * @param form
    * @param request
    * @param response
    * @return
    * @throws Exception
    */
   public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
      log.debug("UserAction - doPerform()");
      if (!isCancelled(request)) {
         UserForm userForm = (UserForm) form;
         Cloud cloud = CloudFactory.getCloud();
         Node userNode;
         int id = userForm.getNodeNumber();
         if (id == -1) {
            userNode = cloud.getNodeManager("users").createNode();
            userNode.setStringValue("account", userForm.getUsername());
           } else {
            userNode = cloud.getNode(id);
         }
         userNode.setStringValue("voornaam", userForm.getVoornaam());
         userNode.setStringValue("tussenvoegsel", userForm.getTussen());
         userNode.setStringValue("achternaam", userForm.getAchternaam());
         userNode.setStringValue("afdeling", userForm.getAfdeling());
         userNode.setBooleanValue("emailsignalering", userForm.isEmailSignalering());
         userNode.setStringValue("emailadres", userForm.getEmail() );
         if (!"".equals(userForm.getPassword())) {
            userNode.setStringValue("password", userForm.getPassword());
            if ("admin".equals(userNode.getStringValue("account"))) {
               Util.updateAdminPassword(userForm.getPassword());
            }
         }
         userNode.setStringValue("notitie", userForm.getNotitie());
         if (!"admin".equals(userNode.getStringValue("account"))) {
            userNode.setStringValue("rank",userForm.getRank());
         }

         userNode.commit();

         Map rollen = Util.buildRolesFromRequest(request);
         new AuthorizationHelper(cloud).setUserRights(userNode, rollen);
         new CheckBoxTree().setRelations(cloud,request);
      }
      return mapping.findForward("success");
   }
}

/**
 * $Log: not supported by cvs2svn $
 * Revision 1.4  2003/11/28 10:20:19  edwin
 * wijzigen van admin password nu correct (rank blijft bewaard)
 *
 * Revision 1.3  2003/10/16 11:34:23  edwin
 * *** empty log message ***
 *
 * Revision 1.2  2003/10/15 15:30:24  edwin
 * *** empty log message ***
 *
 * Revision 1.1  2003/10/14 15:45:47  edwin
 * *** empty log message ***
 *
 * Revision 1.1  2003/10/13 15:25:39  edwin
 * *** empty log message ***
 *
 *
 */
