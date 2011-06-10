function showItemPreview(selectId, targetId) {
    var target = jQuery('#' + targetId);
    var select = jQuery('#' + selectId);
    var extraData = selectId + '=1&' + select.attr('name') + '=' + select.val();
    target.hide();
    jQuery.get(Document.URL, extraData, function(data) {
        update(data, target);
    });
}
function update(data, target) {
    target.html(data);
    target.show();
}
