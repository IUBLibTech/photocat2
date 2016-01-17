
/**
 * A function to take the values from fields whose id
 * is the given prefix followed by an integer to append
 * to the value of the element with the name indicated
 * by the "box" parameter.
 */
function importValues(prefix, boxId) {
    var targetBox = document.getElementById(boxId);
    var values = "";
    var i = 0;
    var sourceBox = document.getElementById(prefix + '_' + i);
    while (sourceBox) {
      if (sourceBox.value.length > 0) {
          if (values.length > 0) {
              values = values + ", ";
          }
          values = values + flipNames(sourceBox.value);
      }
      sourceBox = document.getElementById(prefix + '_' + (++i));        
    } 
    if (targetBox.value.length > 0) {
        targetBox.value = targetBox.value + "\n";
    }
    targetBox.value = targetBox.value + values;    
    return false;
}

/**
 * Takes a string, "name", that is possibly of the pattern
 * "LastName, FirstName" and returns "FirstName LastName".
 * If the String doesn't contain exactly one comma, this
 * method simply returns the provided string.
 */
function flipNames(name) {
    var names = name.split(", ");
    if (names.length == 2) {
        return names[1] + " " + names[0];
    } else {
        return name;
    }
}
