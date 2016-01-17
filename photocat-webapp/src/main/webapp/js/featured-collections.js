$(window).load(function() {
    $(".featured-collections-internal").jCarouselLite({
          visible: 1,
          btnNext: ".prev",
          btnPrev: ".next",
          auto: 15000,
          speed: 1000,
          circular:true
    });
});