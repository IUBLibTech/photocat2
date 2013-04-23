$(document).ready(function() {
    $('.facet-box-contents ul li').each(function(){
        var target = $(this);
    
        // hide everything that has nothing selected and isn't the unit listing
        if($('li.selected', target).length == 0 && $('ul.unit-facet', target).length == 0) {
          target.addClass("collapsed");
          $('div.facet-listing', target).hide();
        } else {
          target.addClass("expanded");
        }

        // add functions to expand/collapse major categories
        $('h4', target).click(function(){
             target.toggleClass("expanded collapsed");
             $('div.facet-listing', target).slideToggle();
        });
    
        // add hover to show that they're links for major categories
        $('h4', target).hover(
             function () {
               $(this).addClass("hover");
             }, 
             function () {
              $(this).removeClass("hover");
             });
    });
    
    // for each first level facet value, we want to add 
    // a way to collapse and expand sub lists
    $('ul.facet-values li').each(function(){
        var target = $(this);
    
        if ($('ul.sub-category', target).length == 0) { 
            // do nothing, there are no sub-elements
        } else {
             // add arrow links to expand collapse the row
             $('div.facet-value', target).first().prepend('<img src="images/triangle-red-down.png" alt="collapse sub elements" class="link collapse-link" />');
             $('div.facet-value', target).first().prepend('<img src="images/triangle-red-right.png" alt="expand sub elements" class="link expand-link" />');

             // add functions to expand/collapse minor categories
             $('img.link', target).click(function(){
                   target.toggleClass("sub-category-expanded sub-category-collapsed");
                   $('ul.sub-category', target).slideToggle();
                   $('img.link', target).toggle();
                 });
                 
            // add hover to show that they're links for minor categories
            $('img.link', target).hover(
                  function () {
                    target.addClass("hover");
                  }, 
                  function () {
                    target.removeClass("hover");
                  });
                  
            // hide everything that has nothing selected and isn't the unit listing
            if ($('ul li.selected', target).length == 0 && $('ul.unit-facet', target).length == 0) {
                target.addClass("sub-category-collapsed");
                $('ul', target).hide();
                $('img.collapse-link', target).hide();
            } else {
               target.addClass("sub-category-expanded");
               $('img.expand-link', target).hide();
            }
        }
    });
    
    // For each facet, show the summary if present and
    // add a link to switch to the full view
    $('.facet-box-contents ul li').each(function(){
        var target = $(this);
        var full = $('div.full', target);
        var summary = $('div.summary', target);

        if (summary.length != 0) {
          // hide the full item
          full.hide();
          
          // show the summary
          summary.show();
          
          // add a link to switch to the full view 
          summary.append('<span class="switch-to-full-view"><a href="#">full listing</a></span>');
          $('span.switch-to-full-view a', summary).click(
               function() {
                 summary.slideToggle(500, function() {
                   full.slideToggle();
                 });
               });
               
          // add a link to revert to the summary view 
          full.append('<span class="switch-to-summary-view"><a href="#">summary listing</a></span>');
          $('span.switch-to-summary-view a', full).click(
               function() {
                 full.slideToggle(500, function() {
                   summary.slideToggle();
                   });
               });
        }
          
    });
});
