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
import org.matsim.runDRT.RunBerlinDrtScenario;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ikaddoura
 *
 */
public class RunBerlinDrtScenarioTest {
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	

	@Test
	public final void testDrtBerlinWithScenarioAdjustment() {
		try {
			
			String configFileName ;
			String overridingConfigFileName;
			String berlinShapeFile;
			String drtServiceAreaShapeFile;
			String transitStopCoordinatesSFile;
			String transitStopCoordinatesRBFile;
			boolean turnDefaultBerlinScenarioIntoDRTscenario;		
			
			configFileName = "scenarios/berlin-v5.2-1pct/input/berlin-drt-v5.2-1pct.config.xml";
			overridingConfigFileName = null;
			
			berlinShapeFile = "scenarios/berlin-v5.2-10pct/input/berlin-shp/berlin.shp";
			drtServiceAreaShapeFile = "scenarios/berlin-v5.2-10pct/input/berlin_bb_area/service-area.shp";

			transitStopCoordinatesSFile = "scenarios/berlin-v5.2-10pct/input/berlin-v5.2.transit-stop-coordinates_S-zoneC.csv";
			transitStopCoordinatesRBFile = "scenarios/berlin-v5.2-10pct/input/berlin-v5.2.transit-stop-coordinates_RB-zoneC.csv";
			
			turnDefaultBerlinScenarioIntoDRTscenario = true;
			
			RunBerlinDrtScenario berlin = new RunBerlinDrtScenario(configFileName, overridingConfigFileName, turnDefaultBerlinScenarioIntoDRTscenario, berlinShapeFile, drtServiceAreaShapeFile, transitStopCoordinatesSFile, transitStopCoordinatesRBFile);
			
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
	
	@Test
	public final void testDrtBerlinWithoutScenarioAdjustment() {
		try {
			
			String configFileName ;
			String overridingConfigFileName;
			String berlinShapeFile;
			String drtServiceAreaShapeFile;
			String transitStopCoordinatesSFile;
			String transitStopCoordinatesRBFile;
			boolean turnDefaultBerlinScenarioIntoDRTscenario;		
			
			configFileName = "scenarios/berlin-v5.2-1pct/input/berlin-drt-v5.2-1pct.config.xml";
			
			overridingConfigFileName = null;
			
			berlinShapeFile = null;
			drtServiceAreaShapeFile = null;

			transitStopCoordinatesSFile = null;
			transitStopCoordinatesRBFile = null;
			
			turnDefaultBerlinScenarioIntoDRTscenario = false;
			
			RunBerlinDrtScenario berlin = new RunBerlinDrtScenario(configFileName, overridingConfigFileName, turnDefaultBerlinScenarioIntoDRTscenario, berlinShapeFile, drtServiceAreaShapeFile, transitStopCoordinatesSFile, transitStopCoordinatesRBFile);
			
			Config config =  berlin.prepareConfig() ;
			config.plans().setInputFile("../../../test/input/drt-berlin-v5.2-1pct.plans_test-agents.xml");
			config.network().setInputFile("../../../test/input/drt-berlin-v5.0.network.xml.gz");
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
