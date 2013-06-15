$(document).ready(function() {	
	// lets initialize our fancy racemap on the #map_canvas container.
	
	var $inputData = $('input[name="raceData"]').val();
	$raceData = $.parseJSON($inputData);
	
	$("#map_canvas").racemap({
		height: $(window).height() - $(".header-wrapper .navbar-fixed-top").height() + 21,
		raceData  : $raceData,
		startLat  : $raceData.trips[0].waypoints[0].coord.lat,
		startLong : $raceData.trips[0].waypoints[0].coord.lng
	});
});