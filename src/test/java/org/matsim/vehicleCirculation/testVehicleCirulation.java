package org.matsim.vehicleCirculation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV2;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

public class testVehicleCirulation {
	static void createVehicleCirculation(Config config, int minTimeToWaitAtEndstop) {
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();
		Vehicles transitVehicles = scenario.getTransitVehicles();
		
		Map<Id<Vehicle>, VehicleType> mapOfVecOnLine = new HashMap<>();
		Map<Id<Link>, Set<Node>> mapOfCreatedLinks = new HashMap<>();
		
		Map<Id<Vehicle>, Id<Vehicle>> mapOfSubstitutedVehicles = new HashMap<>();
		
		for(TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
			TreeMap<String, Departure> mapOfAllDeparture = getMapOfAllDeparturesOnLine(line);
			
			//Problematic Lines to Jump
//			if(line.getId().toString().contains("481-")|| (line.getId().toString().contains("551-")) || line.getId().toString().contains("519-") || line.getId().toString().contains("587-")) {
//				continue;
//			}

			int iterator = 0;
			int iteratorLinkId = 0;
			for(Departure departureOnLine : mapOfAllDeparture.values()) {
						
				if(!mapOfVecOnLine.keySet().contains(departureOnLine.getVehicleId())) {
					mapOfSubstitutedVehicles.put(departureOnLine.getVehicleId(), null);
					VehicleType vecType = transitVehicles.getVehicles().get(departureOnLine.getVehicleId()).getType();
					departureOnLine.setVehicleId(getUmlaufVecId(line, iterator));
					mapOfVecOnLine.put(departureOnLine.getVehicleId(), vecType);
					iterator +=1;
				}

				getNextDepartureFromEndstation(mapOfAllDeparture, line, departureOnLine, mapOfVecOnLine.keySet(), minTimeToWaitAtEndstop, network, iteratorLinkId,
						mapOfCreatedLinks);

			}
		
		}
		
		//Starting to write Files
		TransitScheduleWriterV2 scheduleWriter = new TransitScheduleWriterV2(scenario.getTransitSchedule());
		String filenameSchedule ="test/input/transitSchedule_umlauf.xml";
		scheduleWriter.write(filenameSchedule);
		
		TransitScheduleValidator.validateAll(scenario.getTransitSchedule(), network);
		NetworkWriter networkWriter = new NetworkWriter(scenario.getNetwork());
		String filenameNetwork = "test/input/network_umlauf.xml";
		networkWriter.write(filenameNetwork);
		
		addTransitVehicles(transitVehicles, mapOfVecOnLine);
		MatsimVehicleWriter transitVehiclesWriter = new MatsimVehicleWriter(scenario.getTransitVehicles());
		String filenameTransitVehicles = "test/input/transitVehicles_umlauf.xml";
		transitVehiclesWriter.writeFile(filenameTransitVehicles);
	}
	
	static TreeMap<String, Departure> getMapOfAllDeparturesOnLine (TransitLine transitLine){
		TreeMap<String, Departure> mapOfAllDeparturesOnLine = new TreeMap<>();
		
		int i = 0;
		for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
			for(Departure departure : transitRoute.getDepartures().values()) {
				String idBuilder = departure.getId().toString();
				while(idBuilder.length() < 20) {
					idBuilder = "0" + idBuilder;
				}
				
				String timeBuilder = Integer.toString((int)departure.getDepartureTime());
				while(timeBuilder.length() < 7) {
					timeBuilder = "0" + timeBuilder;
				}
				String stringBuilder = timeBuilder + idBuilder;
				mapOfAllDeparturesOnLine.put(stringBuilder, departure);
				i +=1;
			}
		}
		
		System.out.println(i + "/" + mapOfAllDeparturesOnLine.size());
		return mapOfAllDeparturesOnLine;
	}

	static Id<Vehicle> getUmlaufVecId (TransitLine transitLine, int iteration){
		String umlaufVec = "pt_" + transitLine.getId().toString() + "_umlauf_" + iteration;
		Id<Vehicle> umlaufVecId = Id.create(umlaufVec, Vehicle.class);
		return umlaufVecId;
	}
	
	static TransitRoute getRouteFromDeparture (TransitLine transitLine, Departure departure) {
		Id<Departure> departureId = departure.getId();
		
		TransitRoute transitRouteToReturn = null;
		for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
			if(transitRoute.getDepartures().keySet().contains(departureId)) {
				transitRouteToReturn = transitRoute;
			} 
		}
		return transitRouteToReturn;
	}
	
	static TransitRouteStop getEndStationFromRoute (TransitRoute transitRoute) {
		int numberOfStopsOnRoute = transitRoute.getStops().size();
		TransitRouteStop endStationName = transitRoute.getStops().get(numberOfStopsOnRoute -1);
		return endStationName;
	}
	
	static Departure getNextDepartureFromEndstation(TreeMap<String, Departure> mapOfAllDeparturesOnLine, TransitLine transitLine,
			Departure currentDeparture, Set<Id<Vehicle>> setOfVecOnLine, int minWaitTimeAtEndStation, Network network, int iteratorLinkId, Map<Id<Link>, Set<Node>> mapOfCreatedLinks) {
		Departure departureOnOtherTransitRoute = null;
		
		Id<Vehicle> currentVecId = currentDeparture.getVehicleId();
		Double departureTimeFromCurrentDeparture = currentDeparture.getDepartureTime();
		
		TransitRoute currentTransitRoute = getRouteFromDeparture(transitLine, currentDeparture);
		TransitRouteStop endStop = getEndStationFromRoute(currentTransitRoute);
		String endStationNameOnCurrentLine = endStop.getStopFacility().getName();
		Double travelTimeForCurrentRoute = endStop.getDepartureOffset();
		
		Double earliestDepartureFromEndStation = travelTimeForCurrentRoute + minWaitTimeAtEndStation + departureTimeFromCurrentDeparture;
		
		Set<Entry<String, Departure>> otherDepartures = mapOfAllDeparturesOnLine.entrySet();
        for(Iterator<Entry<String, Departure>> depatureIterator = otherDepartures.iterator(); depatureIterator.hasNext();){
            Entry<String, Departure> departureToChange = depatureIterator.next();
            
            if(setOfVecOnLine.contains(departureToChange.getValue().getVehicleId())) continue;
            
            Double departureTimeForDepartureToChange = departureToChange.getValue().getDepartureTime();
            TransitRoute transitRouteFromDepartureToChange = getRouteFromDeparture(transitLine, departureToChange.getValue());
            TransitRouteStop startStop= transitRouteFromDepartureToChange.getStops().get(0);
            String startStopName = startStop.getStopFacility().getName();
            
            
            if(startStopName.equals(endStationNameOnCurrentLine) && departureTimeForDepartureToChange > earliestDepartureFromEndStation) {
            	departureToChange.getValue().setVehicleId(currentVecId);
            	
            	// TEST
            	if(!endStop.getStopFacility().getId().equals(startStop.getStopFacility().getId())) {
//            		######### TEST to See if adding a link helps ##########
            		addLinkBetweenEndAndStart(network, startStop, endStop, iteratorLinkId, mapOfCreatedLinks);
            	}
            	
            	break;
            }
            
        }
		return departureOnOtherTransitRoute;
	}
	
	static void addTransitVehicles (Vehicles transitVehicles, Map<Id<Vehicle>, VehicleType> mapOfVehicles) {
		for(Id<Vehicle> vehicleId : mapOfVehicles.keySet()) {
			Vehicle vehicle = VehicleUtils.getFactory().createVehicle(vehicleId, mapOfVehicles.get(vehicleId));
			transitVehicles.addVehicle(vehicle);
		}
	}

	static void addLinkBetweenEndAndStart (Network network, TransitRouteStop startStop, TransitRouteStop endStop, int iterator, Map<Id<Link>, Set<Node>> mapOfCreatedLinks) {

		Id<Link> startStopLink = startStop.getStopFacility().getLinkId();
		Id<Link> endStopLink = endStop.getStopFacility().getLinkId();
		
		Node endStopNode = network.getLinks().get(endStopLink).getToNode();
		Node startStopNode = network.getLinks().get(startStopLink).getToNode();
		
//		for(Link searchLinks : network.getLinks().values()) {
//			if(searchLinks.getFromNode() == endStopNode && searchLinks.getToNode() == startStopNode) {
//				return;
//			}
//		}
		
		System.out.println("Warning! Adding Route between: " + endStop.getStopFacility().getId() + " and " + startStop.getStopFacility().getId() + "!");
		
//		####### I still need to figure out how to name the links properly! #####		
		Id<Link> possibleNeededLink= Id.createLinkId("pt_umlauf_" + ThreadLocalRandom.current().nextInt());
		Set<Node> setOfNodes = new HashSet<>();
		setOfNodes.add(endStopNode);
		setOfNodes.add(startStopNode);

		Link linkBetweenEndAndStart = NetworkUtils.createLink(possibleNeededLink, endStopNode, startStopNode, network, 50.0, 10.0, 5000.0, 10.0);
		network.addLink(linkBetweenEndAndStart);

	}
}
