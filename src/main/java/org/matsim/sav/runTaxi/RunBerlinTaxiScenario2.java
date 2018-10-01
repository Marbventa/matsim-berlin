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

package org.matsim.sav.runTaxi;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.taxi.run.TaxiControlerCreator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.sav.DailyRewardHandlerSAVInsteadOfCar;
import org.matsim.sav.SAVPassengerTracker;
import org.matsim.sav.SAVPassengerTrackerImpl;
import org.matsim.sav.prepare.BerlinNetworkModification;
import org.matsim.sav.prepare.BerlinPlansModificationTagFormerCarUsers;
import org.matsim.sav.prepare.BerlinShpUtils;

/**
 * This class starts a simulation run with taxis.
 * 
 *  - The input taxi vehicles file specifies the number of vehicles and the vehicle capacity.
 * 	- The DRT service area is set to the the inner-city Berlin area (see input shape file).
 * 	- The private car mode is still allowed in the Berlin city area.
 * 	- Initial plans are not modified.
 * 
 * @author ikaddoura
 */

public class RunBerlinTaxiScenario2 {

	private static final Logger log = Logger.getLogger(RunBerlinTaxiScenario2.class);

	static final String taxiServiceAreaAttribute = "taxiServiceArea";
	public static final String modeToReplaceCarTripsInBrandenburg = TransportMode.car;
	private final String taxiNetworkMode = TransportMode.car;

	private final String serviceAreaShapeFile;
	
	private Config config;
	private Scenario scenario;
	private Controler controler;
	private RunBerlinTaxiScenario berlin;
	
	private boolean hasPreparedConfig = false ;
	private boolean hasPreparedScenario = false ;
	private boolean hasPreparedControler = false ;

	private double dailyRewardTaxiInsteadOfPrivateCar;

	public static void main(String[] args) {
		
		String configFileName ;
		String overridingConfigFileName;
		String drtServiceAreaShapeFile;
		double dailyRewardDrtInsteadOfPrivateCar;
		
		if (args.length > 0) {
			throw new RuntimeException();
			
		} else {		
			configFileName = "scenarios/berlin-v5.2-1pct/input/berlin-taxi2-v5.2-1pct.config.xml";
			overridingConfigFileName = null;
			drtServiceAreaShapeFile = "scenarios/berlin-v5.2-10pct/input/berliner-ring-area-shp/service-area.shp";
			dailyRewardDrtInsteadOfPrivateCar = 0.;
		}		
		
		new RunBerlinTaxiScenario2( configFileName, overridingConfigFileName, drtServiceAreaShapeFile, dailyRewardDrtInsteadOfPrivateCar).run() ;
	}
	
	public RunBerlinTaxiScenario2( String configFileName, String overridingConfigFileName, String drtServiceAreaShapeFile, double dailyRewardTaxiInsteadOfPrivateCar) {
		
		this.serviceAreaShapeFile = drtServiceAreaShapeFile;
		this.dailyRewardTaxiInsteadOfPrivateCar = dailyRewardTaxiInsteadOfPrivateCar;
		this.berlin = new RunBerlinTaxiScenario( configFileName, overridingConfigFileName );
	}

	public Controler prepareControler() {
		if ( !hasPreparedScenario ) {
			prepareScenario() ;
		}
		
		controler = berlin.prepareControler();
		
		if (dailyRewardTaxiInsteadOfPrivateCar != 0.) {
			// rewards for no longer owning a car
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					this.addEventHandlerBinding().toInstance(new DailyRewardHandlerSAVInsteadOfCar(dailyRewardTaxiInsteadOfPrivateCar, modeToReplaceCarTripsInBrandenburg));			
					
					SAVPassengerTrackerImpl tracker = new SAVPassengerTrackerImpl(TransportMode.drt);		
					this.bind(SAVPassengerTracker.class).toInstance(tracker);
					this.addEventHandlerBinding().toInstance(tracker);
				}
			});
		}
		
		hasPreparedControler = true ;
		return controler;
	}
	
	public Scenario prepareScenario() {
		if ( !hasPreparedConfig ) {
			prepareConfig( ) ;
		}
		
		scenario = berlin.prepareScenario();
		
		BerlinShpUtils shpUtils = new BerlinShpUtils(serviceAreaShapeFile);
		new BerlinNetworkModification(shpUtils).addSAVmode(scenario, taxiNetworkMode, taxiServiceAreaAttribute);
		new BerlinPlansModificationTagFormerCarUsers().run(scenario);

		hasPreparedScenario = true ;
		return scenario;
	}
	
	public Config prepareConfig(ConfigGroup... modulesToAdd) {
		
		config = berlin.prepareConfig(modulesToAdd);		
		TaxiControlerCreator.adjustTaxiConfig(config);
		
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

}

