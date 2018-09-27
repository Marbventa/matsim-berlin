/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.run;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.runTaxi.RunBerlinTaxiScenario0;
import org.matsim.runTaxi.RunBerlinTaxiScenario1;
import org.matsim.runTaxi.RunBerlinTaxiScenario2;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ikaddoura
 *
 */
public class RunBerlinTaxiScenarioTest {
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	

	// private car mode not allowed in berlin
	@Test
	public final void testTaxiBerlinScenario1() {
		try {
			
			String configFileName ;
			String overridingConfigFileName;
			String berlinShapeFile;
			String serviceAreaShapeFile;
			String transitStopCoordinatesSFile;
			String transitStopCoordinatesRBFile;
			
			configFileName = "scenarios/berlin-v5.2-1pct/input/berlin-taxi1-v5.2-1pct.config.xml";
			overridingConfigFileName = null;
			
			berlinShapeFile = "scenarios/berlin-v5.2-10pct/input/berlin-shp/berlin.shp";
			serviceAreaShapeFile = "scenarios/berlin-v5.2-10pct/input/berliner-ring-area-shp/service-area.shp";

			transitStopCoordinatesSFile = "scenarios/berlin-v5.2-10pct/input/berlin-v5.2.transit-stop-coordinates_S-zoneC.csv";
			transitStopCoordinatesRBFile = "scenarios/berlin-v5.2-10pct/input/berlin-v5.2.transit-stop-coordinates_RB-zoneC.csv";
						
			RunBerlinTaxiScenario1 berlin = new RunBerlinTaxiScenario1(configFileName, overridingConfigFileName, berlinShapeFile, serviceAreaShapeFile, transitStopCoordinatesSFile, transitStopCoordinatesRBFile);
			
			Config config =  berlin.prepareConfig() ;
			config.plans().setInputFile("../../../test/input/berlin-v5.2-1pct.plans_test-agents.xml");
			config.controler().setLastIteration(0);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory( utils.getOutputDirectory() + "run_output/" );
			
			Scenario scenario = berlin.prepareScenario();
			new NetworkWriter(scenario.getNetwork()).write(utils.getOutputDirectory() + "taxi-berlin-v5.0.network.xml.gz");
			new PopulationWriter(scenario.getPopulation()).write(utils.getOutputDirectory() + "taxi-berlin-v5.2-1pct.plans.xml.gz");	
			
			berlin.run() ;
			
		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
	}
	
	// private car in berlin still allowed, additional taxi mode, reward if taxi instead of private car
	@Test
	public final void testTaxiBerlinScenario2() {
		try {
			
			String configFileName ;
			String overridingConfigFileName;
			String berlinShapeFile;
			String serviceAreaShapeFile;
			
			configFileName = "scenarios/berlin-v5.2-1pct/input/berlin-taxi2-v5.2-1pct.config.xml";
			overridingConfigFileName = null;
			
			berlinShapeFile = "scenarios/berlin-v5.2-10pct/input/berlin-shp/berlin.shp";
			serviceAreaShapeFile = "scenarios/berlin-v5.2-10pct/input/berliner-ring-area-shp/service-area.shp";
			
			RunBerlinTaxiScenario2 berlin = new RunBerlinTaxiScenario2(configFileName, overridingConfigFileName, berlinShapeFile, serviceAreaShapeFile, 5.);
			
			Config config =  berlin.prepareConfig() ;
			config.plans().setInputFile("../../../test/input/berlin-v5.2-1pct.plans_test-agents.xml");
			config.controler().setLastIteration(5);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory( utils.getOutputDirectory() + "run_output/" );
			
			Scenario scenario = berlin.prepareScenario();
			new NetworkWriter(scenario.getNetwork()).write(utils.getOutputDirectory() + "taxi-berlin-v5.0.network.xml.gz");
			new PopulationWriter(scenario.getPopulation()).write(utils.getOutputDirectory() + "taxi-berlin-v5.2-1pct.plans.xml.gz");	
			
			berlin.run() ;
			
		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
	}
	
	@Test
	public final void testTaxiBerlinScenario0() {
		try {
			
			String configFileName ;
			String overridingConfigFileName;	
			
			configFileName = "scenarios/berlin-v5.2-1pct/input/berlin-taxi1-v5.2-1pct.config.xml";
			overridingConfigFileName = null;
						
			RunBerlinTaxiScenario0 berlin = new RunBerlinTaxiScenario0(configFileName, overridingConfigFileName, 1000., "car_bb");
			
			Config config =  berlin.prepareConfig() ;
			config.plans().setInputFile("../../../test/input/taxi-berlin-v5.2-1pct.plans_test-agents.xml");
			config.network().setInputFile("../../../test/input/taxi-berlin-v5.0.network.xml.gz");
			config.transit().setUseTransit(false); // might lead to a runtime exception if agents switch to pt

			config.controler().setLastIteration(0);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory( utils.getOutputDirectory() + "run_output/" );
			
			berlin.run() ;
			
		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
	}
	
}