$(window).load(function() {
    $(".featured-items-internal").jCarouselLite({
          visible: 5,
          btnNext: ".prev",
          btnPrev: ".next",
          auto: 2500,
          speed: 800,
          circular:true
    });
});