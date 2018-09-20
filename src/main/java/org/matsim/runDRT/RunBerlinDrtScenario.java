/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.runDRT;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareHandler;
import org.matsim.contrib.drt.data.validator.DrtRequestValidator;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.run.RunBerlinScenario;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * This class...
 * 
 * ... modifies the berlin scenario:
 * 		1) network adjustments:
 * 		1a) add drt service-area attribute to the links (based on provided shapefile)
 * 		1b) (private) car mode not allowed in the Berlin city area (based on provided shapefile)
 * 		
 * 		2) plans adjustments:
 * 		2a) plans adjustment: inner-city trips: drt instead of car trips in city area
 * 		2b) plans adjustment: from-/to-city: pt / car+drt / car+S / car+RB trips
 * 		   --> four initial plans!
 * 
 * ... and starts a simulation run without any additional functionality (default analysis, default routing, default scoring, no pricing).
 * 
* @author ikaddoura
*/

public class RunBerlinDrtScenario {

	private static final Logger log = Logger.getLogger(RunBerlinDrtScenario.class);

	private Config config;
	private Scenario scenario;
	private Controler controler;
	private RunBerlinScenario berlin;
	private final boolean turnDefaultBerlinScenarioIntoDRTscenario;

	private final StageActivityTypes stageActivities = new StageActivityTypesImpl("pt interaction", "car interaction", "ride interaction");
	private final String inputPersonAttributesSubpopulationPerson = "person";
	
	private final String modeToReplaceCarTripsInBerlin = TransportMode.drt;
	private final String modeToReplaceCarTripsToFromBerlin = TransportMode.pt;
	public static final String modeToReplaceCarTripsInBrandenburg = "car_bb"; // needs to match the mode specifications in the config file
	private final String taxiNetworkMode = TransportMode.car; // needs to match the mode specification in the config file
	
	private final boolean splitTripsS = true; 
	private final boolean splitTripsRB = true; 
	private final boolean splitTripsTaxi = true; 
	private final String parkAndRideActivity = "park-and-ride";
	private final double parkAndRideDuration = 60.;

	private Map<Integer, Geometry> berlinAreaGeometries = new HashMap<Integer, Geometry>();
	private Map<Integer, Geometry> drtServiceAreaGeometries = new HashMap<Integer, Geometry>();
	private List<Coord> prCoordinatesS = new ArrayList<>();
	private List<Coord> prCoordinatesRB = new ArrayList<>();
	
	static final String drtServiceAreaAttribute = "drtServiceArea";
		
	private boolean hasPreparedConfig = false ;
	private boolean hasPreparedScenario = false ;
	private boolean hasPreparedControler = false ;
		
	public static void main(String[] args) {
		
		String configFileName ;
		String overridingConfigFileName;
		String berlinShapeFile;
		String drtServiceAreaShapeFile;
		String transitStopCoordinatesSFile;
		String transitStopCoordinatesRBFile;
		boolean turnDefaultBerlinScenarioIntoDRTscenario;		
		
		if (args.length > 0) {
			throw new RuntimeException();	
		} else {		
			
			configFileName = "scenarios/berlin-v5.2-1pct/input/berlin-drt-v5.2-1pct.config.xml"; // berlin 1pct
			
			overridingConfigFileName = null;
			
			berlinShapeFile = "scenarios/berlin-v5.2-10pct/input/berlin-shp/berlin.shp";
			drtServiceAreaShapeFile = "scenarios/berlin-v5.2-10pct/input/berliner-ring-area-shp/service-area.shp";

			transitStopCoordinatesSFile = "scenarios/berlin-v5.2-10pct/input/berlin-v5.2.transit-stop-coordinates_S-zoneC.csv";
			transitStopCoordinatesRBFile = "scenarios/berlin-v5.2-10pct/input/berlin-v5.2.transit-stop-coordinates_RB-zoneC.csv";
			
			turnDefaultBerlinScenarioIntoDRTscenario = true;
		}		
		
		new RunBerlinDrtScenario( configFileName, overridingConfigFileName, turnDefaultBerlinScenarioIntoDRTscenario, berlinShapeFile, drtServiceAreaShapeFile, transitStopCoordinatesSFile, transitStopCoordinatesRBFile).run() ;
	}
	
	public RunBerlinDrtScenario( String configFileName, String overridingConfigFileName, boolean turnDefaultBerlinScenarioIntoDRTscenario, String berlinShapeFile, String drtServiceAreaShapeFile, String transitStopCoordinatesSFile, String transitStopCoordinatesRBFile) {
		
		this.turnDefaultBerlinScenarioIntoDRTscenario = turnDefaultBerlinScenarioIntoDRTscenario;
		
		if (turnDefaultBerlinScenarioIntoDRTscenario) {
			this.berlinAreaGeometries = loadShapeFile(berlinShapeFile);
			this.drtServiceAreaGeometries = loadShapeFile(drtServiceAreaShapeFile);

			this.prCoordinatesS = readCSVFile(transitStopCoordinatesSFile);
			this.prCoordinatesRB = readCSVFile(transitStopCoordinatesRBFile);
			
		} else {
			log.warn("Expecting network and plans to be prepared for the DRT scenario.");
		}
				
		this.berlin = new RunBerlinScenario( configFileName, overridingConfigFileName );
	}

	public Controler prepareControler() {
		if ( !hasPreparedScenario ) {
			prepareScenario() ;
		}
		
		controler = berlin.prepareControler();
		
		// drt + dvrp module
		DrtControlerCreator.addDrtToControler(controler);
		
		// reject drt requests outside the service area
		controler.addOverridingModule(new AbstractModule() {	
			@Override
			public void install() {
				this.bind(DrtRequestValidator.class).toInstance(new ServiceAreaRequestValidator());
			}
		});
		
		// drt fares
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().to(TaxiFareHandler.class).asEagerSingleton();
			}
		});
		
		hasPreparedControler = true ;
		return controler;
	}
	
	public Scenario prepareScenario() {
		if ( !hasPreparedConfig ) {
			prepareConfig( ) ;
		}
		
		scenario = berlin.prepareScenario();
		
		if (turnDefaultBerlinScenarioIntoDRTscenario) {
			adjustNetwork(scenario);
			adjustPlans(scenario);
		}	
			
		RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
		routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());

		hasPreparedScenario = true ;
		return scenario;
	}
	
	public Config prepareConfig(ConfigGroup... modulesToAdd) {
		OutputDirectoryLogging.catchLogEntries();
		
		List<ConfigGroup> drtModules = new ArrayList<>();
		drtModules.add(new DvrpConfigGroup());
		drtModules.add(new DrtConfigGroup());
		drtModules.add(new TaxiFareConfigGroup());
		
		List<ConfigGroup> modules = new ArrayList<>();		
		for (ConfigGroup module : drtModules) {
			modules.add(module);
		}	
		for (ConfigGroup module : modulesToAdd) {
			modules.add(module);
		}
		
		ConfigGroup[] modulesArray = new ConfigGroup[modules.size()];
		config = berlin.prepareConfig(modules.toArray(modulesArray));		
		
		DrtControlerCreator.adjustDrtConfig(config);
		
		hasPreparedConfig = true ;
		return config ;
	}
	
	 public void run() {
		if ( !hasPreparedControler ) {
			prepareControler() ;
		}
		controler.run();
		log.info("Done.");
	}
	
	 private void adjustPlans(Scenario scenario) {
			
		 log.info("Number of persons before adjusting plans: " + scenario.getPopulation().getPersons().size());
			tagCarUsers(scenario);
			splitTrips(scenario);
			
			// Delete all link information and (hopefully) do everything via the coordinates...
			for (Person person : scenario.getPopulation().getPersons().values()) {
				for (Plan plan : person.getPlans()) {
					for (PlanElement pE : plan.getPlanElements()) {
						if (pE instanceof Activity) {
							Activity act = (Activity) pE;
							act.setLinkId(null);
						}
					}
				}
			}
			
			log.info("Number of persons after adjusting plans: " + scenario.getPopulation().getPersons().size());
		}

	 private void splitTrips(Scenario scenario) {
			
		log.info("Adjusting plans (change modes; split trips and insert park-and-right activities...");
		
		int tripsBBtoBER = 0;
		int tripsBERtoBB = 0;
		int tripsBBtoBB = 0;
		int tripsBERtoBER = 0;
		int tripsTotal = 0;

		int counter = 0;
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (counter % 10000 == 0)
				log.info("person # " + counter);
			counter++;

			if (person.getPlans().size() > 1)
				throw new RuntimeException("More than one plan per person. Aborting...");

			if (scenario.getPopulation().getPersonAttributes()
					.getAttribute(person.getId().toString(),
							scenario.getConfig().plans().getSubpopulationAttributeName())
					.equals(inputPersonAttributesSubpopulationPerson)) {

				PopulationFactory factory = scenario.getPopulation().getFactory();

				Plan directTripPlan = factory.createPlan(); // without split activities
				Plan splitTripPlanPT1 = factory.createPlan(); // with split activities (previous mode + S-Bahn)
				Plan splitTripPlanPT2 = factory.createPlan(); // with split activities (previous mode + RB/RE)
				Plan splitTripPlanCar = factory.createPlan(); // with split activities (previous mode + Taxi)

				// add first activity
				directTripPlan.addActivity((Activity) person.getSelectedPlan().getPlanElements().get(0));
				splitTripPlanPT1.addActivity((Activity) person.getSelectedPlan().getPlanElements().get(0));
				splitTripPlanPT2.addActivity((Activity) person.getSelectedPlan().getPlanElements().get(0));
				splitTripPlanCar.addActivity((Activity) person.getSelectedPlan().getPlanElements().get(0));

				for (Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan().getPlanElements(),
						stageActivities)) {

					tripsTotal++;

					String mainMode = new MainModeIdentifierImpl().identifyMainMode(trip.getTripElements());

					if (isCoordInBerlinArea(trip.getOriginActivity().getCoord())
							&& isCoordInBerlinArea(trip.getDestinationActivity().getCoord())) {
						// berlin --> berlin
						tripsBERtoBER++;

						String berlinTripMode = null;
						if (mainMode.equals(TransportMode.car)) {
							berlinTripMode = modeToReplaceCarTripsInBerlin;
						} else {
							berlinTripMode = mainMode;
						}

						directTripPlan.addLeg(factory.createLeg(berlinTripMode));
						directTripPlan.addActivity(trip.getDestinationActivity());

						splitTripPlanPT1.addLeg(factory.createLeg(berlinTripMode));
						splitTripPlanPT1.addActivity(trip.getDestinationActivity());

						splitTripPlanPT2.addLeg(factory.createLeg(berlinTripMode));
						splitTripPlanPT2.addActivity(trip.getDestinationActivity());

						splitTripPlanCar.addLeg(factory.createLeg(berlinTripMode));
						splitTripPlanCar.addActivity(trip.getDestinationActivity());

					} else if (isCoordInBerlinArea(trip.getOriginActivity().getCoord())
							&& !isCoordInBerlinArea(trip.getDestinationActivity().getCoord())) {
						// berlin --> brandenburg
						tripsBERtoBB++;

						if (mainMode.equals(TransportMode.car)) {
							
							String berlinTripmode = this.modeToReplaceCarTripsInBerlin;
							String brandenburgTripmode = modeToReplaceCarTripsInBrandenburg;
							String directTripMode = this.modeToReplaceCarTripsToFromBerlin;
							
							// only split trips for former car trips
							directTripPlan.addLeg(factory.createLeg(directTripMode));
							directTripPlan.addActivity(trip.getDestinationActivity());

							splitTripPlanPT1.addLeg(factory.createLeg(berlinTripmode));
							Activity prActivity1 = factory.createActivityFromCoord(parkAndRideActivity, getNearestCoord(trip.getOriginActivity().getCoord(), prCoordinatesS));
							prActivity1.setMaximumDuration(parkAndRideDuration);
							splitTripPlanPT1.addActivity(prActivity1);
							splitTripPlanPT1.addLeg(factory.createLeg(brandenburgTripmode));
							splitTripPlanPT1.addActivity(trip.getDestinationActivity());

							splitTripPlanPT2.addLeg(factory.createLeg(berlinTripmode));
							Activity prActivity2 = factory.createActivityFromCoord(parkAndRideActivity, getNearestCoord(trip.getOriginActivity().getCoord(), prCoordinatesRB));
							prActivity2.setMaximumDuration(parkAndRideDuration);
							splitTripPlanPT2.addActivity(prActivity2);
							splitTripPlanPT2.addLeg(factory.createLeg(brandenburgTripmode));
							splitTripPlanPT2.addActivity(trip.getDestinationActivity());

							splitTripPlanCar.addLeg(factory.createLeg(berlinTripmode));
							Coord firstCarLink = getFirstCarLinkFromPreviousRoute(trip, scenario.getNetwork());
							if (firstCarLink != null) {
								Activity prActivity3 = factory.createActivityFromCoord(parkAndRideActivity, firstCarLink);
								prActivity3.setMaximumDuration(parkAndRideDuration);
								splitTripPlanCar.addActivity(prActivity3);
								splitTripPlanCar.addLeg(factory.createLeg(brandenburgTripmode));
							} else {
								throw new RuntimeException("couldn't find car link. Aborting...");
							}
							splitTripPlanCar.addActivity(trip.getDestinationActivity());
							
						} else {
							String directTripMode = mainMode;
							
							directTripPlan.addLeg(factory.createLeg(directTripMode));
							directTripPlan.addActivity(trip.getDestinationActivity());

							splitTripPlanPT1.addLeg(factory.createLeg(directTripMode));
							splitTripPlanPT1.addActivity(trip.getDestinationActivity());

							splitTripPlanPT2.addLeg(factory.createLeg(directTripMode));
							splitTripPlanPT2.addActivity(trip.getDestinationActivity());

							splitTripPlanCar.addLeg(factory.createLeg(directTripMode));
							splitTripPlanCar.addActivity(trip.getDestinationActivity());
						}

					} else if (!isCoordInBerlinArea(trip.getOriginActivity().getCoord())
							&& isCoordInBerlinArea(trip.getDestinationActivity().getCoord())) {
						// brandenburg --> berlin
						tripsBBtoBER++;

						if (mainMode.equals(TransportMode.car)) {
							
							String berlinTripmode = this.modeToReplaceCarTripsInBerlin;
							String brandenburgTripmode = modeToReplaceCarTripsInBrandenburg;
							String directTripMode = this.modeToReplaceCarTripsToFromBerlin;
						
							directTripPlan.addLeg(factory.createLeg(directTripMode));
							directTripPlan.addActivity(trip.getDestinationActivity());

							splitTripPlanPT1.addLeg(factory.createLeg(brandenburgTripmode));
							Activity prActivity1 = factory.createActivityFromCoord(parkAndRideActivity, getNearestCoord(trip.getOriginActivity().getCoord(), prCoordinatesS));
							prActivity1.setMaximumDuration(parkAndRideDuration);
							splitTripPlanPT1.addActivity(prActivity1);
							splitTripPlanPT1.addLeg(factory.createLeg(berlinTripmode));
							splitTripPlanPT1.addActivity(trip.getDestinationActivity());

							splitTripPlanPT2.addLeg(factory.createLeg(brandenburgTripmode));
							Activity prActivity2 = factory.createActivityFromCoord(parkAndRideActivity, getNearestCoord(trip.getOriginActivity().getCoord(), prCoordinatesRB));
							prActivity2.setMaximumDuration(parkAndRideDuration);
							splitTripPlanPT2.addActivity(prActivity2);
							splitTripPlanPT2.addLeg(factory.createLeg(berlinTripmode));
							splitTripPlanPT2.addActivity(trip.getDestinationActivity());

							Coord lastCarLink = getLastCarLinkFromPreviousRoute(trip, scenario.getNetwork());
							if (lastCarLink != null) {
								splitTripPlanCar.addLeg(factory.createLeg(brandenburgTripmode));
								Activity prActivity3 = factory.createActivityFromCoord(parkAndRideActivity, lastCarLink);
								prActivity3.setMaximumDuration(parkAndRideDuration);
								splitTripPlanCar.addActivity(prActivity3);
							} else {
								throw new RuntimeException("couldn't find car link. Aborting...");
							}
							splitTripPlanCar.addLeg(factory.createLeg(berlinTripmode));
							splitTripPlanCar.addActivity(trip.getDestinationActivity());
						
						} else {
							String directTripMode = mainMode;
							
							directTripPlan.addLeg(factory.createLeg(directTripMode));
							directTripPlan.addActivity(trip.getDestinationActivity());

							splitTripPlanPT1.addLeg(factory.createLeg(directTripMode));
							splitTripPlanPT1.addActivity(trip.getDestinationActivity());

							splitTripPlanPT2.addLeg(factory.createLeg(directTripMode));
							splitTripPlanPT2.addActivity(trip.getDestinationActivity());

							splitTripPlanCar.addLeg(factory.createLeg(directTripMode));
							splitTripPlanCar.addActivity(trip.getDestinationActivity());
						}

					} else if (!isCoordInBerlinArea(trip.getOriginActivity().getCoord())
							&& !isCoordInBerlinArea(trip.getDestinationActivity().getCoord())) {
						// brandenburg --> brandenburg
						tripsBBtoBB++;

						String brandenburgTripmode = null;
						if (mainMode.equals(TransportMode.car)) {
							brandenburgTripmode = modeToReplaceCarTripsInBrandenburg;
						} else {
							brandenburgTripmode = mainMode;
						}

						directTripPlan.addLeg(factory.createLeg(brandenburgTripmode));
						directTripPlan.addActivity(trip.getDestinationActivity());

						splitTripPlanPT1.addLeg(factory.createLeg(brandenburgTripmode));
						splitTripPlanPT1.addActivity(trip.getDestinationActivity());

						splitTripPlanPT2.addLeg(factory.createLeg(brandenburgTripmode));
						splitTripPlanPT2.addActivity(trip.getDestinationActivity());

						splitTripPlanCar.addLeg(factory.createLeg(brandenburgTripmode));
						splitTripPlanCar.addActivity(trip.getDestinationActivity());

					} else {
						throw new RuntimeException("Aborting...");
					}
				}

				person.removePlan(person.getSelectedPlan());

				if (splitTripsS)
					person.addPlan(splitTripPlanPT1);
				if (splitTripsRB)
					person.addPlan(splitTripPlanPT2);
				if (splitTripsTaxi)
					person.addPlan(splitTripPlanCar);
				person.addPlan(directTripPlan);
			}
		}
		log.info("Adjusting plans (change modes; split trips and insert park-and-right activities... Done.");

		log.info("Trips BB to BER: " + tripsBBtoBER);
		log.info("Trips BER to BB: " + tripsBERtoBB);
		log.info("Trips BB to BB: " + tripsBBtoBB);
		log.info("Trips BER to BER: " + tripsBERtoBER);
		log.info("Trips TOTAL: " + tripsTotal);
	}

	private Coord getFirstCarLinkFromPreviousRoute(Trip trip, Network network) {

		NetworkRoute networkRoute = getNetworkRoute(trip, network);
		if (networkRoute != null) {
			// car-trip
			Link firstCarLinkWithAllowedCarMode = null;

			for (Id<Link> linkId : networkRoute.getLinkIds()) {
				if (network.getLinks().get(linkId).getAllowedModes().contains(modeToReplaceCarTripsInBrandenburg)) {
					firstCarLinkWithAllowedCarMode = network.getLinks().get(linkId);
					return firstCarLinkWithAllowedCarMode.getFromNode().getCoord();
				}
			}

			return network.getLinks().get(networkRoute.getStartLinkId()).getFromNode().getCoord();

		} else {
			// non-car trip
			return null;
		}
	}

	private Coord getLastCarLinkFromPreviousRoute(Trip trip, Network network) {

		NetworkRoute networkRoute = getNetworkRoute(trip, network);
		if (networkRoute != null) {
			// car-trip
			Link lastCarLinkWithAllowedCarMode = null;

			for (Id<Link> linkId : networkRoute.getLinkIds()) {
				if (network.getLinks().get(linkId).getAllowedModes().contains(modeToReplaceCarTripsInBrandenburg)) {
					lastCarLinkWithAllowedCarMode = network.getLinks().get(linkId);
				}
			}

			if (lastCarLinkWithAllowedCarMode != null) {
				return lastCarLinkWithAllowedCarMode.getToNode().getCoord();
			} else {
				return network.getLinks().get(networkRoute.getEndLinkId()).getToNode().getCoord();
			}

		} else {
			// non-car trip
			return null;
		}
	}

	private NetworkRoute getNetworkRoute(Trip trip, Network network) {
		NetworkRoute networkRoute = null;
		for (Leg leg : trip.getLegsOnly()) {

			if (leg.getMode().equals(TransportMode.car)) {
				if (networkRoute == null) {
					networkRoute = (NetworkRoute) leg.getRoute();
				} else {
					throw new RuntimeException("More than one car leg during a single trip. Aborting...");
				}
			}
		}
		return networkRoute;
	}

	private Coord getNearestCoord(Coord coordOrigin, List<Coord> prCoordinatesS2) {

		double minDistance = Double.MAX_VALUE;
		Coord minDistanceCoord = null;
		for (Coord coord : prCoordinatesS2) {
			double distance = NetworkUtils.getEuclideanDistance(coordOrigin, coord);
			if (distance < minDistance) {
				minDistance = distance;
				minDistanceCoord = coord;
			}
		}

		return minDistanceCoord;
	}

	private void adjustNetwork(Scenario scenario) {
		log.info("Adjusting network...");
		// network mode adjustments
		int counter = 0;
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (counter % 10000 == 0)
				log.info("link #" + counter);
			counter++;
			if (link.getAllowedModes().contains(TransportMode.car)
					&& link.getAllowedModes().contains(TransportMode.ride)
					&& link.getAllowedModes().contains("freight")) {
				Set<String> allowedModes = new HashSet<>();
				allowedModes.add("freight");
				allowedModes.add(TransportMode.ride);
				allowedModes.add(taxiNetworkMode);

				// cars are only allowed on links with from- and to-node outside of berlin
				if (!isCoordInBerlinArea(link.getFromNode().getCoord())
						&& !isCoordInBerlinArea(link.getToNode().getCoord())) {
					allowedModes.add(modeToReplaceCarTripsInBrandenburg);
				}
				link.setAllowedModes(allowedModes);

				if (isCoordInDrtServiceArea(link.getFromNode().getCoord())
						|| isCoordInDrtServiceArea(link.getToNode().getCoord())) {
					link.getAttributes().putAttribute(drtServiceAreaAttribute, true);
				} else {
					link.getAttributes().putAttribute(drtServiceAreaAttribute, false);
				}

			} else if (link.getAllowedModes().contains(TransportMode.pt)) {
				// skip pt links
			} else {
				throw new RuntimeException("Aborting...");
			}
		}

		// clean the network
		{
			Set<String> modes = new HashSet<>();
			modes.add(this.taxiNetworkMode);
			new MultimodalNetworkCleaner(scenario.getNetwork()).run(modes);
		}
		{
			Set<String> modes = new HashSet<>();
			modes.add(modeToReplaceCarTripsInBrandenburg);
			new MultimodalNetworkCleaner(scenario.getNetwork()).run(modes);
		}
	}

	private List<Coord> readCSVFile(String transitStopCoordinatesFile) {

		List<Coord> coordinates = new ArrayList<>();

		BufferedReader br = IOUtils.getBufferedReader(transitStopCoordinatesFile);
		log.info("Reading coordinates csv file...");
		String line = null;
		try {
			while ((line = br.readLine()) != null) {
				String[] columns = line.split(";");
				Coord coord = new Coord(Double.parseDouble(columns[0]), Double.parseDouble(columns[1]));
				log.info("Adding coordinate link ID " + coord.toString());
				coordinates.add(coord);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		log.info("Reading coordinates csv file... Done.");
		return coordinates;
	}

	private Map<Integer, Geometry> loadShapeFile(String shapeFile) {
		Map<Integer, Geometry> geometries = new HashMap<>();

		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
		int featureCounter = 0;
		for (SimpleFeature feature : features) {
			geometries.put(featureCounter, (Geometry) feature.getDefaultGeometry());
			featureCounter++;
		}
		return geometries;
	}

	private boolean isCoordInBerlinArea(Coord coord) {
		return isCoordInArea(coord, berlinAreaGeometries);
	}

	private boolean isCoordInDrtServiceArea(Coord coord) {
		return isCoordInArea(coord, drtServiceAreaGeometries);
	}

	private boolean isCoordInArea(Coord coord, Map<Integer, Geometry> areaGeometries) {
		boolean coordInArea = false;
		for (Geometry geometry : areaGeometries.values()) {
			Point p = MGC.coord2Point(coord);

			if (p.within(geometry)) {
				coordInArea = true;
			}
		}
		return coordInArea;
	}

	private void tagCarUsers(Scenario scenario) {
		int carUsers = 0;
		int noCarUsers = 0;

		log.info("Tagging car users...");

		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan selectedPlan = person.getSelectedPlan();
			if (selectedPlan == null) {
				throw new RuntimeException("No selected plan. Aborting...");
			}

			boolean personHasCarTrip = false;

			for (PlanElement pE : selectedPlan.getPlanElements()) {

				if (pE instanceof Leg) {
					Leg leg = (Leg) pE;
					if (leg.getMode().equals(TransportMode.car)) {
						personHasCarTrip = true;
					}
				}
			}
			person.getAttributes().putAttribute("CarOwnerInBaseCase", personHasCarTrip);
			if (personHasCarTrip) {
				carUsers++;
			} else {
				noCarUsers++;
			}
		}
		log.info("Tagging car users... Done.");
		log.info("Number of car users: " + carUsers);
		log.info("Number of non-car users: " + noCarUsers);
	}

}

