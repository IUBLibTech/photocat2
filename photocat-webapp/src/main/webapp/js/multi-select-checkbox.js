function getSelectCheckBoxes() {
    var checkboxes = new Array();
    var j = 0;
    
    var inputs = document.getElementsByTagName("input");
    for (i = 0; i < inputs.length; i++) {
        var input = inputs[i];
        if (input.className == "imageSelectCheck") {
            checkboxes[j] = input;
            j++;
        }
    }
    
    return checkboxes;
    // return document.getElementsByClassName("imageSelectCheck");
}

function isAnyChecked() {
    var imageCheckBoxes = getSelectCheckBoxes();
    for (i = 0; i < imageCheckBoxes.length; i++) {
        var cb = imageCheckBoxes[i];
        if (cb.checked == true) {
            return true;
        }
    }
    
    return false;
}

function isAllChecked() {
    var imageCheckBoxes = getSelectCheckBoxes();
    for (i = 0; i < imageCheckBoxes.length; i++) {
        var cb = imageCheckBoxes[i];
        if (cb.checked == false) {
            return false;
        }
    }
    
    return true;
}

var lastCheckboxClicked=false;

function determineCheckBoxSelection(checkbox) {
    var checking=checkbox.checked;
    var started=false;
    if (shiftKeyIsPressed && lastCheckboxClicked) {
        var imageCheckBoxes = getSelectCheckBoxes();
        for (i = 0; i < imageCheckBoxes.length; i++) {
            var cb = imageCheckBoxes[i];
            if (cb == lastCheckboxClicked || cb == checkbox) {
                if (started) {
                     started = false;
                     break;
                } else {
                    started = true;
                }
            } 
            if (started) {
                cb.checked = checking;
            }
        }
    } else {
        lastCheckboxClicked = checkbox;
    }

}

  var shiftKeyIsPressed = false;
  
function handleKeyPress(evt) {
     var nbr;
     if (window.Event) {
         nbr = evt.which;
     } else {
         nbr = event.keyCode;
     }
     if (nbr == 16) {
         shiftKeyIsPressed = true;
     }
     return true;
}
  
   function handleKeyUp(evt) {
       shiftKeyIsPressed = false;
   }
  
   document.onkeydown = handleKeyPress;
   document.onkeyup = handleKeyUp;
 
