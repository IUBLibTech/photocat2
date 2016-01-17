function toggle(elementId, className, altClass) {
    var element = document.getElementById(elementId);
    if (element) {
      if (element.className == className) {
        element.className = altClass;
      } else {
        element.className = className;
      }
    }
}