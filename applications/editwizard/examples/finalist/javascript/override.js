var preloadimages = new Array();
function preLoadButtons() {
   a = 0;
   for (i = 0; i < document.images.length; i++) { 
      if (document.images[i].id.substr(0,"bottombutton-".length)=="bottombutton-") {
         preloadimages[a] = new Image();
         preloadimages[a].src = document.images[i].getAttribute('disabledsrc');
         a++;
      }
   }
}

function setButtonsInactive() {
   for (i = 0; i < document.images.length; i++) { 
      if (document.images[i].id.substr(0,"bottombutton-".length)=="bottombutton-") {
         var image = document.images[i];
         image.src = image.getAttribute('disabledsrc');
         image.className = "bottombutton-disabled";
         image.disabled = true;
      }
   }
}

function updateButtons(allvalid) {
   var savebut = document.getElementById("bottombutton-save");
    
   if(savebut!= null) {
      if (allvalid) {
         setSaveInactive("false");

         savebut.src = savebut.getAttribute("enabledsrc");
         savebut.className = "bottombutton";
         savebut.disabled = false;
         var usetext = getToolTipValue(savebut,"titlesave", "Stores all changes.");
         savebut.title = usetext;
      } else {
         setSaveInactive("true");

         savebut.src = savebut.getAttribute("disabledsrc");
         savebut.className = "bottombutton-disabled";
         savebut.disabled = true;
         var usetext = getToolTipValue(savebut,"titlenosave", "You cannot save because one or more forms are invalid.");
         savebut.title = usetext;
      }
   }
}

function setFacusOnFirstInput() {
    var form = document.forms["form"];
    for (var i=0; i < form.elements.length; i++) {
        var elem = form.elements[i];
        // find first editable field
        var hidden = elem.getAttribute("type"); //.toLowerCase();
        if (hidden != "hidden") {
            // It is very annoying when you want to scroll with a wheel mouse
            // when you open a wizard and the selectbox is the first field.
            if (elem.getAttribute('ftype') != 'enum') {
                elem.focus();
                break;
            }
        }
    }
}
