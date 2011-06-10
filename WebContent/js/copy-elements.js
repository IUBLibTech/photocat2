  
  var CopyElement = {
  
  copyElement: function(elementId, repeatCountElId, buttonColumnElId) {
    var repeatCountEl = document.getElementById(repeatCountElId);
    var nextId = repeatCountEl.value;
    var source = document.getElementById(elementId);
    var copy = source.cloneNode(true);
    var buttonColumn = document.getElementById(buttonColumnElId);
    this.updateIds(copy, nextId);
    source.parentNode.appendChild(copy);
    repeatCountEl.value = parseInt(repeatCountEl.value) + 1;
    buttonColumn.rowSpan = parseInt(repeatCountEl.value) + 1;
    this.setFocus(copy);
  },
  
  updateIds: function(element, nextId) {
      var id = element.id;
      this.updateIdAndName(element, nextId);
      this.clearValue(element);
      for (var i = 0; i < element.childNodes.length;  i++)    {
          this.updateIds(element.childNodes[i], nextId);
      }
  },
  
  updateIdAndName: function(element, nextId) {
    if (element.id != undefined) {
        var underscoreIndex = element.id.lastIndexOf('_');
        if (underscoreIndex != -1) {
          element.id = element.id.substring(0, underscoreIndex + 1) + nextId;
        } else {
          element.id = "";
        }
    }
    if (element.name != undefined) {
        underscoreIndex = element.name.lastIndexOf('_');
        if (underscoreIndex != -1) {
          element.name = element.name.substring(0, underscoreIndex + 1) + nextId;
        } else {
          element.name = "";
        }
    }
  },
  
  clearValue: function(element) {
    if (element.type == "text") {
       element.value = "";
    }
    if (element.type == "textarea") {
       element.value = "";
    }
  },
  
  setFocus: function(element) {
    for (var i = 0; i < element.childNodes.length;  i++)    {
      var child = element.childNodes[i];
      if ((child.nodeName=="INPUT" && child.type=="text") || child.nodeName=="TEXTAREA") {
        child.focus();
        return true;
      }
      if (this.setFocus(child)) {
        return true;
      }
    }
    return false;
  },
};
  
        
 