function updateFieldOptions(selectionId, div1Id, div2Id, div3Id) {
	var selectedIndex = document.getElementById(selectionId).selectedIndex;
	var div1 = jQuery('#' + div1Id);
	if (selectedIndex == 1) {
		div1.show(0);
	} else {
		div1.hide(0);
	}
	
	var div2 = jQuery('#' + div2Id);
	if (selectedIndex == 2) {
		div2.show(0);
	} else {
		div2.hide(0);
	}
	
	var div3 = jQuery('#' + div3Id);
	if (selectedIndex == 3) {
		div3.show(0);
	} else {
		div3.hide(0);
	}
}

/*
function updateFieldOptions(selectionId, group1a, group1b, group2, group3) {
    var selectedIndex = document.getElementById(selectionId).selectedIndex;
    alert("selected index " + selectedIndex);
    if (selectedIndex == 0) {
        document.getElementById(group1a).disabled = 'true';
        document.getElementById(group1a).disabled = 'true';
        document.getElementById(group2).disabled = 'true';
        document.getElementById(group3).disabled = 'true';
        alert("disabled all fields");
    } else if (selectedIndex == 1) {
        document.getElementById(group1a).disabled = 'false';
        document.getElementById(group1a).disabled = 'false';
        document.getElementById(group2).disabled = 'true';
        document.getElementById(group3).disabled = 'true';
    } else if (selectedIndex == 2) {
        document.getElementById(group1a).disabled = 'true';
        document.getElementById(group1a).disabled = 'true';
        document.getElementById(group2).disabled = 'false';
        document.getElementById(group3).disabled = 'true';
    } else if (selectedIndex == 3) {
        document.getElementById(group1a).disabled = 'true';
        document.getElementById(group1a).disabled = 'true';
        document.getElementById(group2).disabled = 'true';
        document.getElementById(group3).disabled = 'false';
    }
}
*/