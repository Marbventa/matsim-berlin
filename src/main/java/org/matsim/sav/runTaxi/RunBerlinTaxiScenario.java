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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareHandler;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxi.data.validator.TaxiRequestValidator;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiControlerCreator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.run.RunBerlinScenario;

/**
 * This class starts a simulation run with taxis.
 * 
 * 	- The input taxi vehicles file specifies the number of vehicles and the vehicle capacity.
 * 	- The taxi service area is specified via the network link attributes.
 * 
 * @author ikaddoura
 */

public class RunBerlinTaxiScenario {

	private static final Logger log = Logger.getLogger(RunBerlinTaxiScenario.class);
	
	private Config config;
	private Scenario scenario;
	private Controler controler;
	private RunBerlinScenario berlin;
	
	private boolean hasPreparedConfig = false ;
	private boolean hasPreparedScenario = false ;
	private boolean hasPreparedControler = false ;
	
	public RunBerlinTaxiScenario( String configFileName, String overridingConfigFileName) {			
		this.berlin = new RunBerlinScenario( configFileName, overridingConfigFileName);
	}

	public Controler prepareControler() {
		if ( !hasPreparedScenario ) {
			prepareScenario() ;
		}
		
		controler = berlin.prepareControler();
		
		// taxi + dvrp module
		TaxiControlerCreator.addTaxiAsSingleDvrpModeToControler(controler);
		
		// reject taxi requests outside the service area
		controler.addOverridingModule(new AbstractModule() {	
			@Override
			public void install() {
				this.bind(TaxiRequestValidator.class).toInstance(new TaxiServiceAreaRequestValidator());
			}
		});
		
		// taxi fares
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
	
		hasPreparedScenario = true ;
		return scenario;
	}
	
	public Config prepareConfig(ConfigGroup... modulesToAdd) {
		OutputDirectoryLogging.catchLogEntries();
		
		// dvrp, drt and taxiFare config groups
		List<ConfigGroup> drtModules = new ArrayList<>();
		drtModules.add(new DvrpConfigGroup());
		drtModules.add(new TaxiConfigGroup());
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

