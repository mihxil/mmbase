<%@taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm"
%><%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" 
    %><mm:content type="text/javascript">
    <mm:cloud>
<mm:import id="gfx_item_none"><mm:treefile page="/gfx/spacer.gif" objectlist="$includePath" /></mm:import>
<mm:import id="gfx_item_opened"><mm:treefile page="/gfx/icon_arrow_tab_open.gif" objectlist="$includePath" /></mm:import>
<mm:import id="gfx_item_closed"><mm:treefile page="/gfx/icon_arrow_tab_closed.gif" objectlist="$includePath" /></mm:import>

    var ITEM_NONE = "${gfx_item_none}";
var ITEM_OPENED = "${gfx_item_opened}";
var ITEM_CLOSED = "${gfx_item_closed}";
var currentnumber = -1;
var contenttype = new Array();
var contentnumber = new Array();

  function addContent( type, number ) {
    contenttype[contenttype.length] = type;
    contentnumber[contentnumber.length] = number;
    if ( contentnumber.length == 1 ) {
      currentnumber = contentnumber[0];
    }
  }

  function nextContent() {
    for(var count = 0; count <= contentnumber.length; count++) {
     if ( contentnumber[count] == currentnumber ) {
       if ( count < contentnumber.length ) {
         if ("tests" == contenttype[count]) {
           alert("<di:translate key="education.testalert" />");
           return;
         }
         var opentype = contenttype[count+1];
         var opennumber = contentnumber[count+1];
       }
     }
   }
   openContent( opentype, opennumber );
   openOnly('div'+opennumber,'img'+opennumber);
  }

  function previousContent() {
    for(var count = 0; count <= contentnumber.length; count++) {
      if ( contentnumber[count] == currentnumber ) {
        if ( count > 0 ) {
          if ("tests" == contenttype[count]) {
            alert("<di:translate key="education.testalert" />");
            return;
          }
          var opentype = contenttype[count-1];
          var opennumber = contentnumber[count-1];
        }
      }
    }
    openContent( opentype, opennumber );
    openOnly('div'+opennumber,'img'+opennumber);
  }


  function openContent( type, number ) {

    if (document.getElementById('content-'+currentnumber)) {
      document.getElementById('content-'+currentnumber).className = "";
    }
    if ( number > 0 ) {
      currentnumber = number;
    }


    switch ( type ) {
      case "educations":

   //    note that document.content is not supported by mozilla!
   //    so use frames['content'] instead

        frames['content'].location.href = addParameter('<mm:treefile page="/education/educations.jsp" objectlist="$includePath" referids="$referids" escapeamps="false"/>', 'edu='+number);
        break;
      case "learnblocks":
      case "htmlpages":
        frames['content'].location.href= addParameter('<mm:treefile page="/education/learnblocks/index.jsp" objectlist="$includePath" referids="$referids,fb_madetest?" escapeamps="false"/>', 'learnobject='+number);
        break;
      case "tests":
        frames['content'].location.href= addParameter('<mm:treefile page="/education/tests/index.jsp" objectlist="$includePath" referids="$referids,fb_madetest?,justposted?" escapeamps="false"/>', 'learnobject='+number);
        break;
      case "pages":
        frames['content'].location.href= addParameter('<mm:treefile page="/education/pages/index.jsp" objectlist="$includePath" referids="$referids,fb_madetest?" escapeamps="false"/>', 'learnobject='+number);
        break;
      case "flashpages":
        frames['content'].location.href= addParameter('<mm:treefile page="/education/flashpages/index.jsp" objectlist="$includePath" referids="$referids,fb_madetest?" escapeamps="false"/>', 'learnobject='+number);
        break;
    }
    frames['content'].scrollTop = '0px';
    document.body.scrollTop = '0px';
    if (document.getElementById('content-'+currentnumber)) {
    document.getElementById('content-'+currentnumber).className = "selectedContent";
    }

  }

  function openClose(div, img) {
    var realdiv = document.getElementById(div);
    var realimg = document.getElementById(img);
    if (realdiv != null) {
      if (realdiv.getAttribute("opened") == "1") {
        realdiv.setAttribute("opened", "0");
        realdiv.style.display = "none";
        realimg.src = ITEM_CLOSED;
      } else {
        realdiv.setAttribute("opened", "1");
        realdiv.style.display = "block";
        realimg.src = ITEM_OPENED;
      }
    }
  }

  function openOnly(div, img) {
    var realdiv = document.getElementById(div);
    var realimg = document.getElementById(img);
    // alert("openOnly("+div+","+img+"); - "+realdiv);
    if (realdiv != null) {
        realdiv.setAttribute("opened", "1");
        realdiv.style.display = "block";
        realimg.src = ITEM_OPENED;

        var className = realdiv.className;
        if (className) {
            // ignore "lbLevel" in classname to get the level depth
            var level = className.substring(7,className.length);
            // alert("level = "+level);
            var findparent = realdiv;
            var findparentClass = className;


            //There is a JS error here
            if (level > 1) {
                // also open parents
                do {
                    findparent = findparent.parentNode;
                    findparentClass = findparent.className || "";
                } while (findparent && findparentClass.indexOf("lbLevel") != 0);

                if (findparent) {
                    var divid = findparent.id;
                    var imgid = "img"+divid.substring(3,divid.length);
                    openOnly(divid,imgid);
                }
             }
                

        }
    } else { // find enclosing div
        var finddiv = realimg;
        while (finddiv != null && (! finddiv.className || finddiv.className.substring(0,7) != "lbLevel")) {
            finddiv = finddiv.parentNode;
            // if (finddiv.className) alert(finddiv.className.substring(0,7));
        }
        if (finddiv != null) {
            var divid = finddiv.id;
            var imgid = "img"+divid.substring(3,divid.length);
            openOnly(divid,imgid);
        }
    }
  }

  function closeAll() {
    var divs = document.getElementsByTagName("div");
    for (i=0; i<divs.length; i++) {
      var div = divs[i];
      var cl = "" + div.className;
      if (cl.match("lbLevel")) {
        divs[i].style.display = "none";
      }
    }
  }

  function removeButtons() {
    // Remove all the buttons in front of divs that have no children
    var imgs = document.getElementsByTagName("img");
    for (i=0; i<imgs.length; i++) {
      var img = imgs[i];
      var cl = "" + img.className;
      if (cl.match("imgClose")) {
        if (img.getAttribute("haschildren") != "1") {
          img.src = ITEM_NONE;
        }
      }
    }
  }

</mm:cloud>
</mm:content>
