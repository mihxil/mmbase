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
package nl.leocms.evenementen.forms;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForm;
import org.mmbase.bridge.Cloud;
import org.mmbase.bridge.Node;
import org.mmbase.bridge.NodeList;
import org.mmbase.bridge.Relation;
import org.mmbase.bridge.RelationList;
import org.mmbase.util.logging.*;

import nl.leocms.evenementen.Evenement;
import nl.leocms.util.DoubleDateNode;
import nl.mmatch.NatMMConfig;
import nl.mmatch.HtmlCleaner;

import com.finalist.mmbase.util.CloudFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.*;

/**
 * SubscribeAction
 */
public class SubscribeAction extends Action {
   private static final Logger log = Logging.getLoggerInstance(EvenementAction.class);
   private static final String BACKOFFICE_SUBSCRIPTION = "70594";
   private static final String SITE_SUBSCRIPTION = "86035";
   private static final String GROUP_EXCURSION = "321316";
   public static int NO_COSTS = 0;
   public static int UNKNOWN_COSTS = -1;
   public static int GROUP_EXCURSION_COSTS = -2;
   public static int DEFAULT_COSTS = 9999;

   private void removeObsoleteFormBean(ActionMapping mapping, HttpServletRequest request) {
      // *** Remove the obsolete form bean ***
      if (mapping.getAttribute() != null) {
         if ("request".equals(mapping.getScope())) {
            request.removeAttribute(mapping.getAttribute());
         } else {
            HttpSession session = request.getSession();
            session.removeAttribute(mapping.getAttribute());
         }
      }
   }

   public static String price(int iPrice) {
      String sResult = iPrice/100 + ",";
      if (iPrice%100 < 9) {
         sResult += "0";
      }
      sResult += iPrice%100;
      return "&euro;&nbsp;" + sResult;
   }

   public static String priceFormating(int iPrice) {
      if (iPrice==UNKNOWN_COSTS) { return "onbekend"; }
      if (iPrice==NO_COSTS) { return "gratis"; }
      return price(iPrice);
   }

   public static String priceFormating(String sPrice) {
      return priceFormating(Integer.parseInt(sPrice));
   }

   public static String priceFormating(String sPrice, boolean isGroupExcursion) {
      String sFPrice = priceFormating(sPrice);
      if(isGroupExcursion && sFPrice.equals("gratis")) { 
         sFPrice = "-";
      }
      return sFPrice;
   }   
   
   private static String getDateTime(Node thisEvent) {
      DoubleDateNode ddn = new DoubleDateNode();
      ddn.setBegin(new Date(thisEvent.getLongValue("begindatum")*1000));
      ddn.setEnd(new Date(thisEvent.getLongValue("einddatum")*1000));
      return ddn.getReadableDate() + ", " + ddn.getReadableTime();
   }

   private static String dearSir(Node thisParticipant, String thisParticipantName, String newline) {
      String message = "";
      if (thisParticipant.getStringValue("gender").equals("male")) {
         message += "Geachte heer ";
      } else {
         message += "Geachte mevrouw ";
      }
      message += thisParticipantName + "," + newline + newline;
      return message;
   }

   private static String withThisLetter(Node thisParent, Node thisEvent, String confirmUrl, boolean isGroupExcursion, String newline) {
      String message = "";
      if(confirmUrl.equals("reminder")){
         message += "Met deze brief herinneren wij u aan";
      } else {
         message += "Met deze brief bevestigen wij";
      }
      if(!isGroupExcursion) {
         message += " uw aanmelding voor " + thisEvent.getStringValue("titel") + ", " + getDateTime(thisEvent) + newline;
      } else {
         message += " uw boeking van een groepsexcursie";
         NodeList nl = thisParent.getRelatedNodes("natuurgebieden","related",null);
         if(nl.size()!=0) {
            message += " op het " + nl.getNode(0).getStringValue("naam");
         }
         message += "." + newline;
      }
      return message;
   }
   
   private static String addText(String text, String newline) {
      String message = "";
      if(text!=null&&!HtmlCleaner.cleanText(text,"<",">","").trim().equals("")){
         message = text + newline;
      }
      return message;
   }
   private static String necessaryInfo(Node thisParent, String newline) {
      String message = "";
      RelationList relations = thisParent.getRelations("posrel","vertrekpunten");
      for(int r=0; r<relations.size(); r++) {
         if(r==0) {
            if(relations.size()==1) {
               message += "Vertrekpunt:" + newline + newline;
            } else {
               message += "Vertrekpunten:" + newline + newline;
            }
         }
         Relation relation = relations.getRelation(r);
         Node departurePoint = relation.getDestination();
         message  += departurePoint.getStringValue("titel") + newline
            + departurePoint.getStringValue("tekst") + newline + newline;
      }

      relations = thisParent.getRelations("posrel","bevestigings_teksten");
      for(int r=0; r<relations.size(); r++) {
         if(r==0) {
            message += "Let op:" + newline + newline;
         }
         Relation relation = relations.getRelation(r);
         Node confirmationText = relation.getDestination();
         message  += confirmationText.getStringValue("titel") + newline;
         message += addText(confirmationText.getStringValue("intro"),newline);
         message += addText(confirmationText.getStringValue("body"),newline);
      }

      relations = thisParent.getRelations("readmore","extra_info");
      for(int r=0; r<relations.size(); r++) {
         if(r==0) {
            message += "Let op:" + newline + newline;
         }
         Relation relation = relations.getRelation(r);
         Node extraInfoText = relation.getDestination();
         message  += "-" + extraInfoText.getStringValue("omschrijving") + newline;
         if(r==relations.size()-1) {
            message +=  newline;
         }
      }
      return message;
   }

   private static String subscriptionStatus(Node thisEvent, String confirmUrl, String phoneNumbers, String newline) {
      String message = "";
      int iCurPart = thisEvent.getIntValue("cur_aantal_deelnemers");
      int iMinPart = thisEvent.getIntValue("min_aantal_deelnemers");
      if(iCurPart<iMinPart) {
          message += "Er zijn nog niet genoeg aanmeldingen voor deze activiteit om uw aanmelding definitief te bevestigen. Hierdoor is de kans aanwezig dat de activiteit niet door kan gaan. Als de activiteit niet door kan gaan, nemen wij contact met u op.";
      } else {
         if(!confirmUrl.equals("reminder")){
            message += "Uw aanmelding is nu definitief. ";
         }
         message += "Als u bent verhinderd meldt u dan af bij " + phoneNumbers + ".";
      }
      return message;
   }

   private static String youSubscribedWith(Node thisSubscription, String newline) {

      String message = "U heeft zich ingeschreven met:" + newline + "<table>";
      String paymentCondition = "";
      RelationList relations = thisSubscription.getRelations("posrel","deelnemers");
      int totalCosts = 0;
      for(int r=0; r<relations.size(); r++) {

         Relation relation = relations.getRelation(r);

         Node nextParticipant = relation.getDestination();
         message += "<tr><td>"+ nextParticipant.getStringValue("bron");
         RelationList dnc_relations = nextParticipant.getRelations("related","deelnemers_categorie");
         if(!dnc_relations.isEmpty()) {
               Node dnCategory = dnc_relations.getRelation(0).getSource();
               message += " " + dnCategory.getStringValue("naam");
         }
         message += "</td>";

         String sPaymentType = thisSubscription.getStringValue("betaalwijze");
         if (sPaymentType.equals(SubscribeForm.cashPaymentType)){
            
            int costs = relation.getIntValue("pos");
            if(totalCosts!=-1) { totalCosts += costs; }

            String sCosts = priceFormating(costs);
            if(sCosts.equals("onbekend")) { 
               totalCosts = -1; 
            }

            message += "<td>&nbsp;" + sCosts + "</td></tr>";
         } else {
            message += "<td>&nbsp;" + sPaymentType + "(*)</td></tr>";
            paymentCondition = "(*) Dit aanbod is geldig voor twee personen (voor extra deelnemers betaalt u de ledenprijs)" + newline;
            totalCosts = -1;
         }
      }
      message += "<tr><td>TOTAAL</td><td>&nbsp;" 
         + (totalCosts==-1 ?  "de prijs voor uw inschrijving kon helaas niet worden berekend" : priceFormating(totalCosts) )
         + "</td></tr>"
         + "</table>" + newline + paymentCondition + newline ;
      return message;
   }

   private static String youSubscribedAs(Node thisParticipant, Node thisSubscription, String thisParticipantName, boolean isGroupExcursion, String newline) {

      String message = "U bent als volgt ingeschreven:" + newline + "<table>";
      if(isGroupExcursion&&!thisParticipant.getStringValue("prefix").equals("")) {
         message += "<tr><td>Naam van de groep:</td><td>" + thisParticipant.getStringValue("prefix") + "</td></tr>";
      }
      message += "<tr><td>Naam:</td><td>" + thisParticipantName + "</td></tr>";
      if(!thisParticipant.getStringValue("email").equals("")) {
         message += "<tr><td>Email:</td><td>" + thisParticipant.getStringValue("email") + "</td></tr>";
      }
      if(!thisParticipant.getStringValue("privatephone").equals("")) {
         message += "<tr><td>Telefoon:</td><td>" + thisParticipant.getStringValue("privatephone") + "</td></tr>";
      }
      if(!thisParticipant.getStringValue("straatnaam").equals("")) {
         message += "<tr><td>Adres:</td><td>"
            + thisParticipant.getStringValue("straatnaam")
            + " " + thisParticipant.getStringValue("huisnummer")
            + ", " + thisParticipant.getStringValue("postcode")
            + " " + thisParticipant.getStringValue("plaatsnaam")
            + " " + thisParticipant.getStringValue("land")
            + "</td></tr>";
      }
      if(!thisParticipant.getStringValue("lidnummer").equals("")) {
          message += "<tr><td>Lidmaatschapsnummer:</td><td>"
            + thisParticipant.getStringValue("lidnummer")
            + "</td></tr>";
      }
      if(!isGroupExcursion&&!thisSubscription.getStringValue("description").equals("")) {
          message += "<tr><td>Bijzonderheden:</td><td>"
            + thisSubscription.getStringValue("description")
            + "</td></tr>";
      }
      message += "</table>" + newline + newline;
      return message;
   }

   private static String yourGroupExcursion(Node thisParent, Node thisEvent, Node thisSubscription, String newline) {

      DoubleDateNode ddn = new DoubleDateNode();
      ddn.setBegin(new Date(thisEvent.getLongValue("begindatum")*1000));
      ddn.setEnd(new Date(thisEvent.getLongValue("einddatum")*1000));

      String message = "U heeft voor uw groep " + thisEvent.getStringValue("titel") + " gereserveerd op " +  ddn.getReadableDate() + "."
            + " De excursie duurt van " + ddn.getReadableStartTime() + " tot " + ddn.getReadableEndTime() + ".";

      Node confirmationText = null;
      NodeList nl = thisSubscription.getRelatedNodes("bevestigings_teksten","daterel",null);
      if(nl.size()!=0) {
         confirmationText = nl.getNode(0);
      } else {
         nl = thisParent.getRelatedNodes("bevestigings_teksten","posrel",null);
         if(nl.size()!=0) {
            confirmationText = nl.getNode(0);
         }
      }
      if(confirmationText!=null) {
         message += addText(confirmationText.getStringValue("intro"),newline);
         message += addText(confirmationText.getStringValue("body"),newline);
      }

      int costs = Evenement.getCategoryCosts(thisSubscription,GROUP_EXCURSION);
      message += "Bij deze brief is een overzicht ingesloten met praktische informatie, zoals een routebeschrijving naar de plaats van afvaart, en tips over wat mee te nemen." + newline + newline;
      message += "De prijs van de vaarexcursie bedraagt " + price(costs) + newline + newline;
      message += "U wordt vriendelijk verzocht dit bedrag over te maken naar rekeningnummer 32391, ten name van Natuurmonumenten 's-Graveland, onder vermelding van \"";
      nl = thisParent.getRelatedNodes("natuurgebieden","related",null);
      if(nl.size()!=0) {
         message += nl.getNode(0).getStringValue("titel_de");
      }
      message += "\", en de datum van uw excursie." + newline;
      message += "Wij zien uw betaling graag uiterlijk veertien dagen voor de afvaart tegemoet. (Als u kort van te voren heeft geboekt, dan uiteraard graag zo spoedig mogelijk)." + newline + newline;
      message += "We wensen u een mooie excursie!";
      return message;
   }      

   private static String withKindRegards(Node thisParticipant, String emailAddresses, String newline) {
      String message = "Met vriendelijke groeten," + newline + newline + newline + emailAddresses + newline + newline;

      if(thisParticipant.getStringValue("lidnummer").equals("")) {
         message += "Nog geen lid? Meld u nu aan als lid op http://www.natuurmonumenten.nl/lidworden";
      }
      return message;
   }

   public static String [] getPhoneAndEmail(Node thisParent, String newline) {
      
      String bookPhone = "(035) 655 99 55";
      String bookEmail = "denatuurin@natuurmonumenten.nl";
      String phoneNumbers = "";
      String emailAddresses = "";

      RelationList relations = thisParent.getRelations("readmore","afdelingen");
      for(int r=0; r<relations.size(); r++) {
         Relation relation = relations.getRelation(r);
         Node bookingDepartment = relation.getDestination();
         if(relation.getStringValue("readmore").equals("2")) {

            if(bookingDepartment.getStringValue("naam").equals("Ledenservice")) { // *** tailor-made ledenservice info ***
               if(!bookingDepartment.getStringValue("email").equals("") ) {
                  bookEmail = bookingDepartment.getStringValue("email");
               }

               if(!bookingDepartment.getStringValue("telefoonnummer").equals("")) {
                  bookPhone = bookingDepartment.getStringValue("telefoonnummer");
               }

            } else {
               phoneNumbers += (!phoneNumbers.equals("") ? " of " : "") + bookingDepartment.getStringValue("naam");
               emailAddresses += (!emailAddresses.equals("") ? newline : "") + bookingDepartment.getStringValue("naam") + newline + newline;

               if(!bookingDepartment.getStringValue("email").equals("") ) {
                  emailAddresses += "email: " +  bookingDepartment.getStringValue("email") + newline;
               }

               if(!bookingDepartment.getStringValue("telefoonnummer").equals("")) {
                  phoneNumbers += " " + bookingDepartment.getStringValue("telefoonnummer");
                  emailAddresses += "telefoon: " +  bookingDepartment.getStringValue("telefoonnummer") + newline;
               }
            }
         }
      }

      phoneNumbers = "onze ledenservice " + bookPhone  + (!phoneNumbers.equals("") ? " of " : "") + phoneNumbers ;
      emailAddresses =
         "Natuurmonumenten Ledenservice" + newline + newline
         + "email: " + bookEmail + newline
         + "telefoon: " + bookPhone + newline + newline
         + (!emailAddresses.equals("") ? newline : "") + emailAddresses;

      return new String [] { phoneNumbers, emailAddresses };
   }

   public static String getMessage(Node thisEvent, Node thisParent, Node thisSubscription, Node thisParticipant, String confirmUrl, String type) {

      boolean isGroupExcursion = Evenement.isOfCategory(thisParent,GROUP_EXCURSION);
      
      String newline = "<br/>";
      if(type.equals("plain")) { newline = "\n"; }
      String [] phoneAndEmail = getPhoneAndEmail(thisParent, newline);
   
      String thisParticipantName =  thisParticipant.getStringValue("firstname")
         + (thisParticipant.getStringValue("initials").equals("") ? "" : " " +  thisParticipant.getStringValue("initials"))
         + (thisParticipant.getStringValue("suffix").equals("") ? "" : " " +  thisParticipant.getStringValue("suffix"))
         + (thisParticipant.getStringValue("lastname").equals("") ? "" : " " +  thisParticipant.getStringValue("lastname"));

      String message = dearSir(thisParticipant, thisParticipantName, newline);

      if(isGroupExcursion) {
         message += withThisLetter(thisParent, thisEvent, confirmUrl, isGroupExcursion, newline);
         message += youSubscribedAs(thisParticipant, thisSubscription, thisParticipantName, isGroupExcursion, newline);
         message += yourGroupExcursion(thisParent, thisEvent, thisSubscription, newline) + newline + newline;
         message += withKindRegards(thisParticipant, phoneAndEmail[1], newline);

      } else {
      
         if(confirmUrl.equals("")||confirmUrl.equals("reminder")) { // *** after the visitor clicks the confirmation url    
            message += withThisLetter(thisParent, thisEvent, confirmUrl, isGroupExcursion, newline);
            message += subscriptionStatus(thisEvent, confirmUrl, phoneAndEmail[0], newline);
            message += newline + newline;
            message += necessaryInfo(thisParent, newline);
      
         } else { // *** the visitor booked on the website
            message += "Gebruik de onderstaande link om uw aanmelding voor "  + thisEvent.getStringValue("titel") + ", " + getDateTime(thisEvent) + " te bevestigen: " + newline + newline;
            if(type.equals("plain")) {
               message += confirmUrl + newline + newline;
            } else {
               message += "<b><a href=\"" + confirmUrl + "\">bevestig uw aanmelding </a></b>" + newline + newline;
            }
         }
   
         if(!type.equals("plain")) {
            message += youSubscribedWith(thisSubscription, newline);
      
            if(confirmUrl.equals("")) {
               message += youSubscribedAs(thisParticipant, thisSubscription, thisParticipantName, isGroupExcursion, newline);
            }
            message += withKindRegards(thisParticipant, phoneAndEmail[1], newline);
         }
      }
      return message;
   }

   private static void sendConfirmEmail(Cloud cloud, Node thisEvent, Node thisParent, Node thisSubscription, Node thisParticipant, String confirmUrl) {

      String fromEmailAddress = "denatuurin@natuurmonumenten.nl";
      String emailSubject = "";

      if(thisEvent!=null && thisSubscription !=null && thisParticipant!=null) {

         String toEmailAddress = thisParticipant.getStringValue("email");

         if(!toEmailAddress.equals("")) { // ** email field might be empty

            if(confirmUrl.equals("")) {
               emailSubject += "Bevestiging aanmelding";
            } else if(confirmUrl.equals("reminder")) {
               emailSubject += "Herinnering aanmelding";
            } else {
               emailSubject += "Aanmelding";
            }

            emailSubject += " " + thisEvent.getStringValue("titel") + ", " + getDateTime(thisEvent);
            log.info(confirmUrl);
            Node emailNode = cloud.getNodeManager("email").createNode();
            emailNode.setValue("to", toEmailAddress);
            emailNode.setValue("from", fromEmailAddress);
            emailNode.setValue("subject", emailSubject);
            emailNode.setValue("replyto", fromEmailAddress);
            emailNode.setValue("body",
                            "<multipart id=\"plaintext\" type=\"text/plain\" encoding=\"UTF-8\">"
                               + getMessage(thisEvent,thisParent,thisSubscription,thisParticipant,confirmUrl,"plain")
                            + "</multipart>"
                            + "<multipart id=\"htmltext\" alt=\"plaintext\" type=\"text/html\" encoding=\"UTF-8\">"
                            + "<html>"
                              + getMessage(thisEvent,thisParent,thisSubscription,thisParticipant,confirmUrl,"html") + "</html>"
                            + "</multipart>");
            emailNode.commit();
            emailNode.getValue("mail(oneshotkeep)");

            thisSubscription.createRelation(emailNode,cloud.getRelationManager("related")).commit();

            // *** update inschrijvingen,related,inschrijvings_status if present status is aangemeld 
            // this means the inschrijving is made by the backoffice
            // for website bookings the status is set to confirmed in includes/events_doconfirm.jsp
            RelationList relations = thisSubscription.getRelations("related","inschrijvings_status");
            if(!relations.isEmpty()) {
               Relation  thisRelation = relations.getRelation(0);
               if(thisRelation.getDestination().getStringValue("naam").equals("aangemeld")) {
                  thisRelation.delete();
                  Node thisStatus = cloud.getNode("confirmed");
                  if(thisStatus!=null) {
                     thisSubscription.createRelation(thisStatus,cloud.getRelationManager("related")).commit();
                  }
               }
            }
         }
      }
   }

   public static void sendConfirmEmail(Cloud cloud, String subscriptionNumber, String confirmUrl) {
      Node thisSubscription = cloud.getNode(subscriptionNumber);
      Node thisEvent = null;
      Node thisParent = null;
      Node thisParticipant = null;
      NodeList thisNodeList = thisSubscription.getRelatedNodes("evenement");
      if(thisNodeList.size()>0) {
         thisEvent = thisNodeList.getNode(0);
      }
      thisParent = cloud.getNode(Evenement.findParentNumber(thisEvent.getStringValue("number")));
      if(thisNodeList.size()>0) {
         thisParticipant = thisNodeList.getNode(0);
      }
      thisNodeList = thisSubscription.getRelatedNodes("deelnemers");
      if(thisNodeList.size()>0) {
         thisParticipant = thisNodeList.getNode(0);
      }
      if(thisEvent!=null&&thisParent!=null&&thisParticipant!=null) {
         sendConfirmEmail(cloud, thisEvent, thisParent, thisSubscription, thisParticipant, confirmUrl);
      } else {
         log.info("Could not send confirmation email for subscription " + subscriptionNumber);
      }
   }

   public static void sendConfirmEmail(Cloud cloud, String subscriptionNumber) {
      sendConfirmEmail(cloud, subscriptionNumber, "");
   }

   public static void sendReminderEmail(Cloud cloud, String subscriptionNumber) {
      sendConfirmEmail(cloud, subscriptionNumber, "reminder");
   }

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

        SubscribeForm subscribeForm = (SubscribeForm) form;
        ActionForward forwardAction = null;
        String action = subscribeForm.getAction();

        log.info("SubscribeAction.execute(" + action
            + "," + subscribeForm.getSubscriptionNumber()
            + "," + subscribeForm.getTicketOffice()
            + ")");

        Cloud cloud = CloudFactory.getCloud();

        if (subscribeForm.getButtons().getGoBack().pressed()
            ||(action.indexOf("Annuleer")>-1)
            ||(action.indexOf("Naar agenda")>-1)
            ||(action.indexOf("andere data")>-1)
            ||(action.indexOf("fix_date")>-1)) {                                           // **************** GoBack / Annuleer / .... ********

           // removeObsoleteFormBean(mapping, request);
           subscribeForm.resetNumbers();
           if(action.indexOf("andere data")>-1) {
               subscribeForm.setAction("select_date");
           } else if(!(action.indexOf("fix_date")>-1)) { // *** leave fix_date unchanged ***
               subscribeForm.setAction("canceled");
           }
           forwardAction = mapping.findForward("success");

        } else if(subscribeForm.getButtons().getDeleteParticipant().pressed()) {             // ****************** DeleteParticipant *********************

           if(!subscribeForm.getSelectedParticipant().equals("-1")) {

              Node thisParticipant = cloud.getNode(subscribeForm.getSelectedParticipant());
              thisParticipant.delete(true);

              Node thisEvent = cloud.getNode(subscribeForm.getNode());
              thisEvent.commit(); // *** save to update cur_aantal_deelnemers
           }

           forwardAction = mapping.findForward("continue");

        } else if(subscribeForm.getButtons().getDeleteSubscription().pressed()) {            // ****************** DeleteSubscription *********************

           if(!subscribeForm.getSubscriptionNumber().equals("-1")) {

               Node thisSubscription = cloud.getNode(subscribeForm.getSubscriptionNumber());
               NodeList thisnodesList = thisSubscription.getRelatedNodes("deelnemers");
               for(int i=0; i<thisnodesList.size(); i++) {
                  Node thisnode = thisnodesList.getNode(i);
                  thisnode.delete(true);
               }
               thisSubscription.delete(true);

               Node thisEvent = cloud.getNode(subscribeForm.getNode());
               thisEvent.commit(); // *** save to update cur_aantal_deelnemers
           }

           forwardAction = mapping.findForward("continue");

        } else if(action.indexOf("Nieuwe aanmelding")>-1) {                                  // ****************** Nieuwe aanmelding *********************

           subscribeForm.resetBean();
           forwardAction = mapping.findForward("continue");

        } else if(subscribeForm.getButtons().getShowPastDates().pressed()) {                 // ****************** ShowPastDates *************

           if(subscribeForm.getShowPastDates().equals("true")) {
               subscribeForm.setShowPastDates("false");
           } else {
               subscribeForm.setShowPastDates("true");
           }

           forwardAction = mapping.findForward("continue");

       } else if(action.indexOf("Adres")>-1) {                                               // ****************** Address *********************

           if(subscribeForm.getShowAddress().equals("true")) {
               subscribeForm.setShowAddress("false");
           } else {
               subscribeForm.setShowAddress("true");
           }

           forwardAction = mapping.findForward("continue");

        } else if(action.indexOf("Meld aan")>-1
                  ||action.indexOf("Wijzig")>-1
                  ||subscribeForm.getButtons().getAddParticipant().pressed()) {              // ****************** Meld aan / Wijzig / AddParticipant *********************

            Node thisEvent = cloud.getNode(subscribeForm.getNode());

            Node thisSubscription = null;
            if(action.indexOf("Meld aan")>-1) { // *** create new inschrijving
               thisSubscription = cloud.getNodeManager("inschrijvingen").createNode();
               thisSubscription.setLongValue("datum_inschrijving",(new Date()).getTime()/1000);
            } else {
               thisSubscription = cloud.getNode(subscribeForm.getSubscriptionNumber());
            }
            thisSubscription.setStringValue("source",subscribeForm.getSource());
            thisSubscription.setStringValue("description",subscribeForm.getDescription());
            thisSubscription.setStringValue("ticket_office",subscribeForm.getTicketOffice());
            thisSubscription.setStringValue("betaalwijze", subscribeForm.getPaymentType());
            thisSubscription.commit();

            if(action.indexOf("Meld aan")>-1) { // *** create inschrijvingen,posrel,evenementen
               thisEvent.createRelation(thisSubscription,cloud.getRelationManager("posrel")).commit();
                                                // *** create inschrijvingen,schrijver,users
               NodeList userList = cloud.getNodeManager("users").getList("account='"+subscribeForm.getUserId()+"'",null,null);
               if (userList.size()!=0) {
                  userList.getNode(0).createRelation(thisSubscription,cloud.getRelationManager("schrijver")).commit();
               }
            }

            // *** update inschrijvingen,related,inschrijvings_status
            RelationList relations = thisSubscription.getRelations("related","inschrijvings_status");
            for(int r=0; r<relations.size(); r++) { relations.getRelation(r).delete(); }

            // *** before using status, set status when necessary
            // *** possible situations:
            // (a) website booker, initial status is BACKOFFICE_SUBSCRIPTION: change status
            // (b) website booker, who subsequently books in backoffice: change status
            String thisStatus = subscribeForm.getStatus();
            if(thisStatus.equals("")
               ||thisStatus.equals(BACKOFFICE_SUBSCRIPTION)
               ||thisStatus.equals(SITE_SUBSCRIPTION)) {

               if(subscribeForm.getTicketOffice().equals("website")) {
                  thisStatus = SITE_SUBSCRIPTION;
               } else {
                  thisStatus = BACKOFFICE_SUBSCRIPTION;
               }
               subscribeForm.setStatus(thisStatus);
            }
            thisSubscription.createRelation(cloud.getNode(thisStatus),cloud.getRelationManager("related")).commit();

            // *** add participant to subscription
            if(subscribeForm.getTicketOffice().equals("backoffice")) {
               Node thisParticipant = null;
               boolean onlyGroupExcursion = false;
               if(Evenement.isGroupExcursion(cloud,subscribeForm.getParent())) {
                  // for a group excursion the first participant should be of the category "group excursion"
                  NodeList dcl = cloud.getList( thisSubscription.getStringValue("number")
                              ,"inschrijvingen,posrel,deelnemers,related,deelnemers_categorie"
                              ,"deelnemers_categorie.number","deelnemers_categorie.number='" + Evenement.groupExcursion(cloud) + "'"
                              ,null,null,null,true);
                  if(dcl.size()==0) {
                     thisParticipant =
                        subscribeForm.createParticipant(cloud,"Meld aan",thisEvent,thisSubscription,Evenement.groupExcursion(cloud),"1");
                     if(subscribeForm.getParticipantsCategory().equals("-1")) {
                        onlyGroupExcursion = true;
                     }
                  }
               }
               if(!onlyGroupExcursion) {
                  thisParticipant =
                     subscribeForm.createParticipant(cloud,action,thisEvent,thisSubscription,subscribeForm.getParticipantsCategory(),subscribeForm.getNumberInCategory());
               }

               if(subscribeForm.getButtons().getAddParticipant().pressed()) {
                  // *** in case of Wijzig, setting number in category to 1 would lead to resetting number of participants by clicking the "Wijzig" button twice
                  subscribeForm.setNumberInCategory("1");
               }

               subscribeForm.setSelectedParticipant(thisParticipant.getStringValue("number"));
               subscribeForm.setSubscriptionNumber(thisSubscription.getStringValue("number"));
               subscribeForm.setAction(null);

            } else {
               NodeList dcl = cloud.getList( subscribeForm.getParent(), "evenement,posrel,deelnemers_categorie","deelnemers_categorie.number",null,"deelnemers_categorie.naam","UP","DESTINATION",true);
               Node thisParticipant = null;
               for(int i = 0; i < dcl.size(); i++) {
                  String participantsPerCat = subscribeForm.getParticipantsPerCat(i);
                  if(!participantsPerCat.equals("0")) {
                     thisParticipant = subscribeForm.createParticipant(cloud,action,thisEvent,thisSubscription,dcl.getNode(i).getStringValue("deelnemers_categorie.number"),participantsPerCat);
                  }
               }
               if(thisParticipant==null) {
                     thisParticipant = subscribeForm.createParticipant(cloud,action,thisEvent,thisSubscription,"-1",subscribeForm.getParticipantsPerCat(0));
               }

               String confirmUrl = NatMMConfig.liveUrl + "/events.jsp";
               confirmUrl += "?action=confirm&s=" + thisSubscription.getStringValue("datum_inschrijving") + "_" + thisSubscription.getStringValue("number");
               Node thisParent = cloud.getNode(subscribeForm.getParent());
               sendConfirmEmail(cloud, thisEvent, thisParent, thisSubscription, thisParticipant, confirmUrl);
               subscribeForm.resetNumbers();
               subscribeForm.setAction("promptforconfirmation");
            }
            forwardAction = mapping.findForward("continue");

       } else if(subscribeForm.getButtons().getConfirmSubscription().pressed()) {            // ******************* Confirm *************************

         Node thisEvent = cloud.getNode(subscribeForm.getNode());
         Node thisParent = cloud.getNode(subscribeForm.getParent());
         Node thisSubscription = cloud.getNode(subscribeForm.getSubscriptionNumber());
         Node thisParticipant = cloud.getNode(subscribeForm.getSelectedParticipant());
         sendConfirmEmail(cloud, thisEvent, thisParent, thisSubscription, thisParticipant,"");

         forwardAction = mapping.findForward("continue");

       }
       return forwardAction;
   }

}
