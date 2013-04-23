function displayElement(elId) {
    var el = document.getElementById(elId);
    el.className = 'unexpected_values_div_displayed';
}

function hideElement(elId) {
    var el = document.getElementById(elId);
    el.className = 'unexpected_values_div_hidden';
}