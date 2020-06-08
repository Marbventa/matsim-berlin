package org.matsim.vehicleCirculation;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Assert;
import org.matsim.analysis.ScoreStatsControlerListener.ScoreItem;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.run.RunBerlinScenario;
import org.matsim.run.drt.ptRoutingModes.RunDrtOpenBerlinScenarioWithPtRoutingModesTest;
import org.matsim.testcases.MatsimTestUtils;

public class testScoreForProcessedSchedule {
	
	private static final Logger log = Logger.getLogger(RunDrtOpenBerlinScenarioWithPtRoutingModesTest.class);
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	@Test
	public final void testSCoreForProcessedScheduler(){
		try {
		
			final String[] args = {"scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml"};
			Config config =  RunBerlinScenario.prepareConfig( args );
			
			int setDelayToNextDeparture = 20 * 60;
			testVehicleCirulation.createVehicleCirculation(config, setDelayToNextDeparture);

			config.controler().setLastIteration(0);
			config.strategy().setFractionOfIterationsToDisableInnovation(0);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory(utils.getOutputDirectory());
			
			// read the created output files back into the simulation
			config.transit().setTransitScheduleFile("/Users/Gero 1/Desktop/matsim-berlin-5.5.x/test/input/transitSchedule_umlauf.xml");
			config.transit().setVehiclesFile("/Users/Gero 1/Desktop/matsim-berlin-5.5.x/test/input/transitVehicles_umlauf.xml");
			config.network().setInputFile("/Users/Gero 1/Desktop/matsim-berlin-5.5.x/test/input/network_umlauf.xml");

			Scenario scenario = RunBerlinScenario.prepareScenario( config );

			org.matsim.core.controler.Controler controler = RunBerlinScenario.prepareControler( scenario ) ;

			controler.run() ;
			
			// MATSimUtils wurde auf 0.2 geändert, da 10pct besser läuft. -> noch einmal 5 Pt-Agenten
			Assert.assertEquals("Different avg. executed score in iteration 0 .", 115.94951289934534, controler.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(0), 0.2);

			log.info("") ;


		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	
	}
}