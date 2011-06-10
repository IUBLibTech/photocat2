function toggleVersionInformation(id, targetId) {
    var link = jQuery('#' + id);
    var target = jQuery('#' + targetId);
    if (target.html() == "") {
        var url = link.attr('href');
        var extraData = link.attr('id') + '=1';
        //alert("Making ajax call: " + url + extraData);
        jQuery.get(url, extraData, function(data) {
            update(data, targetId);
        });
    }
    target.toggle();
}
function update(data, targetId) {
    jQuery('#' + targetId).html(data);
}
