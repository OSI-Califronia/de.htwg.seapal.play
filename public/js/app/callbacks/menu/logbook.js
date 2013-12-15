/**
 * logbook.js
 *
 * Define the actions for the logbook entry's.
 * 
 */

$(document).ready(function() {
    menu.addCallback('leftclick', 'logbook', function (self) {
        self.button('toggle');
        $('.active-logbook').removeClass('active-logbook').addClass('inactive-logbook');
        $(self.data('name')).removeClass('inactive-logbook').addClass('active-logbook');
    });
    
    menu.addCallback('leftclick', 'icon-selectedTrack', function (self) {
        self.removeClass('icon-selectedTrack').addClass('icon-notSelectedTrack');
        console.log(self.data('id'));
    });
    
    menu.addCallback('leftclick', 'icon-notSelectedTrack', function (self) {
        $('.icon-selectedTrack').removeClass('icon-selectedTrack').addClass('icon-notSelectedTrack');
        self.removeClass('icon-notSelectedTrack').addClass('icon-selectedTrack');
        console.log(self.data('id'));
    });
    
    menu.addCallback('leftclick', 'icon-signInSeapal', function (self) {
        menu.closeMenu();
        window.location = "/login";
    });
    
    menu.addCallback('leftclick', 'icon-signUpSeapal', function (self) {
        menu.closeMenu();
        window.location = "/signup";
    });
    
    menu.addCallback('leftclick', 'icon-notSelectedBoat', function (self) {
        $('.icon-selectedBoat').removeClass('icon-selectedBoat').addClass('icon-notSelectedBoat');
        self.removeClass('icon-notSelectedBoat').addClass('icon-selectedBoat');
        map.selectBoat(self.data('id'));
    });  

    menu.addCallback('rightclick', ['icon-notSelectedBoat', 'icon-selectedBoat'], function (self) {
        $('#modal-form_trip').modal('show');
    });    
});