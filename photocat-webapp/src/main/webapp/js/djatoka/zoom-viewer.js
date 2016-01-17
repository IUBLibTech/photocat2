    jQuery.noConflict();
    jQuery(document).ready(function() {
      jQuery("#zoom-link").click(function() {
          alert("test");
          jQuery("#targetborder").show();
      });
    });