/*********************************************************************** *
  project: org.matsim.
                                                                          
  ********************************************************************** *
                                                                          
  copyright       : (C) 2020 by the members listed in the COPYING,        
                    LICENSE and WARRANTY file.                            
  email           : info at matsim dot org                                
                                                                          
  ********************************************************************** *
                                                                          
    This program is free software; you can redistribute it and/or modify  
    it under the terms of the GNU General Public License as published by  
    the Free Software Foundation; either version 2 of the License, or     
    (at your option) any later version.                                   
    See also COPYING, LICENSE and WARRANTY file                           
                                                                          
  ********************************************************************** */

/**
* @author gmarburger
*/


package org.matsim.vehicleCirculation;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Assert;
import org.matsim.analysis.ScoreStatsControlerListener.ScoreItem;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.run.RunBerlinScenario;
import org.matsim.run.drt.ptRoutingModes.RunDrtOpenBerlinScenarioWithPtRoutingModesTest;
import org.matsim.testcases.MatsimTestUtils;

public class runVehicleCirculationScoreTest {
	
	private static final Logger log = Logger.getLogger(RunDrtOpenBerlinScenarioWithPtRoutingModesTest.class);
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	@Test
	public final void testSCoreForProcessedScheduler(){
		try {
		
			final String[] args = {"/Users/Gero 1/git/matsim-berlin-MasterArbeit/scenarios/berlin-v5.5-10pct/input/berlin-v5.5-10pct.config.xml"};
			
			Config configBaseCase = RunBerlinScenario.prepareConfig(args );
			configBaseCase.controler().setLastIteration(0);
			configBaseCase.strategy().setFractionOfIterationsToDisableInnovation(0);
			configBaseCase.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			configBaseCase.controler().setOutputDirectory(utils.getOutputDirectory());
			
			configBaseCase.network().setTimeVariantNetwork(true);
			configBaseCase.network().setChangeEventsInputFile("/Users/Gero 1/git/matsim-berlin-MasterArbeit/scenarios/berlin-v5.5-10pct/input/networkChangeEventU9_060.xml");
			
			configBaseCase.controler().setOutputDirectory("scenarios/berlin-v5.5-10pct/caseStudy/Delay/U9/Sims/BC/output-berlin-drt-v5.5-10pct_BC_060");

			
			Scenario scenarioBaseCase = RunBerlinScenario.prepareScenario( configBaseCase );
			changeMinLengthToOne(scenarioBaseCase); 		
			org.matsim.core.controler.Controler controlerBaseCase = RunBerlinScenario.prepareControler( scenarioBaseCase ) ;					
			controlerBaseCase.run();
			
			// starting the Planfall scenario here!
			Config configCreateVehicleWorking =  RunBerlinScenario.prepareConfig( args );
			
			int setDelayToNextDeparture = 10 * 60;
			boolean overrideMinDelay = false;
//			Scenario scenarioVehicleWorking = ScenarioUtils.createScenario(configCreateVehicleWorking);
			createVehicleCirculation.create(scenarioBaseCase, setDelayToNextDeparture, overrideMinDelay);

			
			configCreateVehicleWorking.controler().setLastIteration(0);
			configCreateVehicleWorking.strategy().setFractionOfIterationsToDisableInnovation(0);
			configCreateVehicleWorking.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			configCreateVehicleWorking.controler().setOutputDirectory("scenarios/berlin-v5.5-10pct/caseStudy/Delay/U9/Sims/RVC/output-berlin-drt-v5.5-10pct_BC_RVC_060");
			
			// read the created output files back into the simulation
			configCreateVehicleWorking.transit().setTransitScheduleFile("/Users/Gero 1/git/matsim-berlin-MasterArbeit/scenarios/berlin-v5.5-10pct/input/transitSchedule_umlauf.xml");
			configCreateVehicleWorking.transit().setVehiclesFile("/Users/Gero 1/git/matsim-berlin-MasterArbeit/scenarios/berlin-v5.5-10pct/input/transitVehicles_umlauf.xml");
			configCreateVehicleWorking.network().setInputFile("/Users/Gero 1/git/matsim-berlin-MasterArbeit/scenarios/berlin-v5.5-10pct/input/network_umlauf.xml");

			configCreateVehicleWorking.network().setTimeVariantNetwork(true);
			configCreateVehicleWorking.network().setChangeEventsInputFile("/Users/Gero 1/git/matsim-berlin-MasterArbeit/scenarios/berlin-v5.5-10pct/input/networkChangeEventU9_060.xml");

			Scenario scenarioUmlauf = RunBerlinScenario.prepareScenario( configCreateVehicleWorking );
			org.matsim.core.controler.Controler controlerUmlauf = RunBerlinScenario.prepareControler( scenarioUmlauf ) ;

			controlerUmlauf.run() ;
			
			// MATSimUtils wurde auf 0.2 geändert, da 10pct besser läuft. -> noch einmal 5 Pt-Agenten... 0.0000016 als Wert
			System.out.println("Score Base Case: "+ controlerBaseCase.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(0));
			System.out.println("Score Umlauf: " + controlerUmlauf.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(0));
			Assert.assertEquals("Different avg. executed score in iteration 0 .", controlerBaseCase.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(0), controlerUmlauf.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(0), MatsimTestUtils.EPSILON);

			log.info("") ;


		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	
	}
	
	Scenario changeMinLengthToOne(Scenario scenario) {
		Network network = scenario.getNetwork();
		double minLinkLength = 1.0;
		for(Link link : network.getLinks().values()) {
			if(link.getLength() == 0.0) {
				link.setLength(minLinkLength);
			}
		}
		return scenario;
	}
}