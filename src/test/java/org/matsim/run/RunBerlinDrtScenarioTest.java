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
import org.matsim.sav.runDRT.RunBerlinDrtScenario1;
import org.matsim.sav.runDRT.RunBerlinDrtScenario2;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ikaddoura
 *
 */
public class RunBerlinDrtScenarioTest {
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	

	// private car mode not allowed in berlin
	@Test
	public final void testDrtBerlinScenario1() {
		try {
			
			String configFileName ;
			String overridingConfigFileName;
			String carRestrictedAreaShapeFile;
			String drtServiceAreaShapeFile;
			String transitStopCoordinatesSFile;
			
			configFileName = "scenarios/berlin-v5.2-1pct/input/berlin-drt1-v5.2-1pct.config.xml";
			overridingConfigFileName = null;
			
			carRestrictedAreaShapeFile = "scenarios/berlin-v5.2-10pct/input/shp-inner-city-area/inner-city-area.shp";
			drtServiceAreaShapeFile = "scenarios/berlin-v5.2-10pct/input/shp-inner-city-area/inner-city-area.shp";

			transitStopCoordinatesSFile = "scenarios/berlin-v5.2-10pct/input/berlin-v5.2.transit-stop-coordinates_S-ring.csv";
						
			RunBerlinDrtScenario1 berlin = new RunBerlinDrtScenario1(configFileName, overridingConfigFileName, carRestrictedAreaShapeFile, drtServiceAreaShapeFile, transitStopCoordinatesSFile);
			
			Config config =  berlin.prepareConfig() ;
			config.plans().setInputFile("../../../test/input/berlin-v5.2-1pct.plans_test-agents.xml");
			config.controler().setLastIteration(0);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory( utils.getOutputDirectory() + "run_output/" );
			
			Scenario scenario = berlin.prepareScenario();
			new NetworkWriter(scenario.getNetwork()).write(utils.getOutputDirectory() + "drt-berlin-v5.0.network.xml.gz");
			new PopulationWriter(scenario.getPopulation()).write(utils.getOutputDirectory() + "drt-berlin-v5.2-1pct.plans.xml.gz");	
			
			berlin.run() ;
			
		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
	}
	
	// private car in berlin still allowed, additional drt mode, reward if drt instead of private car
	@Test
	public final void testDrtBerlinScenario2() {
		try {
			
			String configFileName ;
			String overridingConfigFileName;
			String drtServiceAreaShapeFile;
			
			configFileName = "scenarios/berlin-v5.2-1pct/input/berlin-drt2-v5.2-1pct.config.xml";
			overridingConfigFileName = null;
			
			drtServiceAreaShapeFile = "scenarios/berlin-v5.2-10pct/input/shp-inner-city-area/inner-city-area.shp";
			
			RunBerlinDrtScenario2 berlin = new RunBerlinDrtScenario2(configFileName, overridingConfigFileName, drtServiceAreaShapeFile, 5.);
			
			Config config =  berlin.prepareConfig() ;
			config.plans().setInputFile("../../../test/input/berlin-v5.2-1pct.plans_test-agents.xml");
			config.controler().setLastIteration(1);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory( utils.getOutputDirectory() + "run_output/" );
			
			Scenario scenario = berlin.prepareScenario();
			new NetworkWriter(scenario.getNetwork()).write(utils.getOutputDirectory() + "drt-berlin-v5.0.network.xml.gz");
			new PopulationWriter(scenario.getPopulation()).write(utils.getOutputDirectory() + "drt-berlin-v5.2-1pct.plans.xml.gz");	
			
			berlin.run() ;
			
		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
	}
	
}
