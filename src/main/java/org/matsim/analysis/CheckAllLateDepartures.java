package org.matsim.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

public class CheckAllLateDepartures {

	public static void main(String[] args) throws IOException {
		Config config = ConfigUtils.loadConfig("scenarios/berlin-v5.5-10pct/input/berlin-v5.5-10pct.config.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);

		EventsManager eventsManager = EventsUtils.createEventsManager();
		ArrivesTooLateHandler myHandler = new ArrivesTooLateHandler();
		eventsManager.addHandler(myHandler);
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile("scenarios/berlin-v5.5-10pct/output-berlin-drt-v5.5-10pct/berlin-drt-v5.5-10pct.output_events.xml.gz");
		List<VehicleDepartsAtFacilityEvent> eventsHandled = myHandler.returnList();
		
		List<String> gatheredPtEventInformation = new ArrayList<>();
		
		for(VehicleDepartsAtFacilityEvent event: eventsHandled) {
			// dieser Passus gilt nur dann, wenn man nur einen Bahnhof sucht:
//			if(!scenario.getTransitSchedule().getFacilities().get(event.getFacilityId()).getName().contains("Zoologischer Garten")) {
//				continue;
//			}
			
			gatheredPtEventInformation.add(getInformationForEvent(event, scenario));
		}
		
		writeListToFile(gatheredPtEventInformation, "alleEvents", "csv");
	}
		
	private static String getRouteSemicolonBearing(TransitRoute route){
		String lineName = route.getId().toString().substring(0, route.getId().toString().indexOf("-"));
		String bearing;
		
		Coord coordStartPos = route.getStops().get(0).getStopFacility().getCoord();
		Coord coordEndPos = route.getStops().get(route.getStops().size()-1).getStopFacility().getCoord();
		
		// vergleiche Koordinate
		// x ist Ost-West, wobei X>0 dann gilt Richtung Osten
		double distanceBetweenX = coordEndPos.getX() - coordStartPos.getX();
		
		// y ist Nord-Süd, wobei Y>0 dann gilt Richtung Norden;
		double distanceBetweenY = coordEndPos.getY() - coordStartPos.getY();
		
		// gilt vermutlich nur für die Berliner UbahnLinien und fast alle Sbahn-Linien. Muss jedoch geprüft werden. Wird als gegeben angenommen
		if(Math.abs(distanceBetweenX) > Math.abs(distanceBetweenY)) {
			if(distanceBetweenX > 0) bearing = "Osten";
			else bearing = "Westen";
		} else {
			if(distanceBetweenY > 0) bearing = "Norden";
			else if(distanceBetweenY < 0) bearing = "Sueden";
			else bearing = "vermutlich Ringbahn";
		}
		
		return lineName + "; " + bearing;
	}
	
	private static void writeListToFile(List<String> list, String fileName, String fileExtension) throws IOException{
		
		String fileTitle = "scenarios/berlin-v5.5-10pct/output-berlin-drt-v5.5-10pct/" + fileName + "." + fileExtension;

		try (
			FileWriter fw = new FileWriter(fileTitle);
			BufferedWriter bw = new BufferedWriter(fw);
		) {
			for(String string: list) 
			{
				bw.write(string + "\n");
			}
		}
	}
	
	private static String getInformationForEvent(VehicleDepartsAtFacilityEvent event, Scenario scenario) {
		// event.getVehicleId() = pt_RE2---16869_100_6_1 oder pt_RE5---19059_100_12_4
		// event.getTime() <- gibt aktuelle Zeit in s aus
		Id<Vehicle> eventVehicleId = event.getVehicleId();
		
		Id<TransitStopFacility> eventFacilityId = event.getFacilityId();
		TransitStopFacility eventFacility = scenario.getTransitSchedule().getFacilities().get(eventFacilityId);
		
		double eventTime = event.getTime();
		
		String routeIdString = eventVehicleId.toString().substring(3, 
				eventVehicleId.toString().lastIndexOf("_"));
		
		Id<TransitRoute> routeId = Id.create(routeIdString, TransitRoute.class);
		
		// mittels routeIdString kann man die Linie filtern
		String lineIdString = routeIdString.substring(0, routeIdString.lastIndexOf("_"));
		Id<TransitLine> lineId= Id.create(lineIdString, TransitLine.class);
		
		TransitRoute route = scenario.getTransitSchedule().getTransitLines().get(lineId).getRoutes().get(routeId);
		double departureOffSetForFacility = route.getStop(eventFacility).getDepartureOffset();
		
		// ermitteln der Abfahrtszeit des Fahrzeugs
		double depatureTimeForRoute = 0;
		for(Departure departure: route.getDepartures().values()) {
			if(departure.getVehicleId().equals(eventVehicleId)){
				depatureTimeForRoute = departure.getDepartureTime();
			}
		}
		double expectedDepartureTime = depatureTimeForRoute + departureOffSetForFacility;
		double delay = eventTime - expectedDepartureTime;
		
		// Matsim;Schedule;Delay
		String timeInformation = eventTime + "; " + expectedDepartureTime + "; " + delay;
		
		// Linie;Richtung
		String routeBearingInformation = getRouteSemicolonBearing(route);
		
		// wo fand das Event statt
		String facilityInformation = eventFacility.getName();
		
		String information = timeInformation + "; " + routeBearingInformation + "; " + facilityInformation;
		return information;
	}
	
}