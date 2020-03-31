package org.matsim.analysis;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

public class ArrivesTooLateHandler implements VehicleDepartsAtFacilityEventHandler{

	List<VehicleDepartsAtFacilityEvent> listeAllerEvents = new ArrayList<>();
	
	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		listeAllerEvents.add(event);
	}
	
	public List<VehicleDepartsAtFacilityEvent> returnList() {
		return listeAllerEvents;
	}
	
	public List<VehicleDepartsAtFacilityEvent> returnCheckedList(Set<Id<TransitStopFacility>> zooIds,
			List<Id<Vehicle>> bahnhofsVehicles) {
		List<VehicleDepartsAtFacilityEvent> checkedEvents = new ArrayList<>();
		
		for(VehicleDepartsAtFacilityEvent event: listeAllerEvents) {
			if(zooIds.contains(event.getFacilityId())) {
				if(bahnhofsVehicles.contains(event.getVehicleId())) {
					checkedEvents.add(event);
				}			
			}
		}
		return checkedEvents;
	}
	
	public Map <Id<Departure>, Departure> returnMapScheduleAbfahrten(ArrayList<VehicleDepartsAtFacilityEvent> bahnhofEvents,
			Map <Id<Departure>, Departure> routeDepartures){
		Map <Id<Departure>, Departure> relevanteScheduleAbfahrten= new HashMap<>();
		return relevanteScheduleAbfahrten;
	}


}