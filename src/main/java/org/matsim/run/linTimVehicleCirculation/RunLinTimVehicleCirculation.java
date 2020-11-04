package org.matsim.run.linTimVehicleCirculation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV2;
import org.matsim.run.RunBerlinScenario;
import org.matsim.vehicles.MatsimVehicleWriter;

public class RunLinTimVehicleCirculation {

	public static void main(String[] args) throws IOException {
		// Please make sure you change ExportToLinTimUtils export directory before commencing. The Directory path is nessesary there.
		Config config = ConfigUtils.loadConfig("scenarios/berlin-v5.5-10pct/input/berlin-v5.5-10pct.config.xml");
		Scenario scenario = RunBerlinScenario.prepareScenario( config );
				
		String matsimConversionExportDir = "../../git/OpenLinTimMac/OpenLinTim/datasets/MATSIMexport/ConversionTables/";
		boolean limitGrossRaumBerlin = true;
		boolean isMetropolitianArea = true;
		
		String vehicleType =".*_700.*";
		
		List<String> listOfModeRules = createListModes();
		VehicleWorkingIntegerWrapper vehicleNumberCounter = new VehicleWorkingIntegerWrapper(1);
		PublicTransportLinkIntegerWrapper ptLinkNumberCounter = new PublicTransportLinkIntegerWrapper(1);
		
		String pathToMatsimExportDir = "../../git/OpenLinTimMac/OpenLinTim/datasets/MATSIMexport/";
		for(String modeRule : listOfModeRules) {
			MatsimExportForLinTim.export(scenario, isMetropolitianArea, "mode",limitGrossRaumBerlin, modeRule, matsimConversionExportDir);
			
			TerminalCommandsHelper.runLinTimInConsole(pathToMatsimExportDir, modeRule);
			
			scenario = MatsimImportFromLinTim.run(scenario, isMetropolitianArea, "mode",limitGrossRaumBerlin,  modeRule, vehicleNumberCounter, ptLinkNumberCounter, matsimConversionExportDir);
			
			// Will make it possible that the second run of bus will export the rest of the bus-schedules.
			if(modeRule.equals("bus")) limitGrossRaumBerlin = false;
		}
		
		/* Will remove all links of length 0. Length 0 may cause problems when running dijkstra from the 'creating umlauf vehicles' in MATSim. */
		scenario = removeLinksWithZeroLength(scenario);
		
		writeOutput(scenario);
//		config.transit().setTransitScheduleFile("/Users/Gero 1/Desktop/matsim-berlin-5.5.x/scenarios/berlin-v5.5-10pct/input/transitSchedule_linTim.xml");
//		config.transit().setVehiclesFile("/Users/Gero 1/Desktop/matsim-berlin-5.5.x/scenarios/berlin-v5.5-10pct/input/transitVehicles_linTim.xml");
//		config.network().setInputFile("/Users/Gero 1/Desktop/matsim-berlin-5.5.x/scenarios/berlin-v5.5-10pct/input/network_linTim.xml");
//		scenario = RunBerlinScenario.prepareScenario( config );
//				
		// Currently not working!
//		Controler controler = RunBerlinScenario.prepareControler( scenario ) ;
//		controler.run() ;
			
		System.out.println();
		System.out.println("Running vehicle working through Lintim has finished. A total of " + vehicleNumberCounter.toString() + " different vehicles were used!");
		System.out.println("A total of " + ptLinkNumberCounter.toString() + " links were added!");
	}

	/** To make sure that dijkstra works, no links are alloud to have 0 length.
	 * 
	 * @param scenario which might contain links with length zero.
	 * @return
	 */
	private static Scenario removeLinksWithZeroLength(Scenario scenario) {
		Network network = scenario.getNetwork();
		
		double minLinkLength = 1.0;
		for(Link link : network.getLinks().values()) {
			if(link.getLength() == 0.0) {
				link.setLength(minLinkLength);
			}
		}
		
		return scenario;
	}

	/** define which modes are supposed to be run as vehicle workings
	 * 
	 * @return a list of transport Modes
	 */
	private static List<String> createListModes() {
		List<String> listOfModes = new ArrayList<>();
		listOfModes.add("railS");
		listOfModes.add("railU");
		listOfModes.add("railRE");
		listOfModes.add("railZ");
		listOfModes.add("tram");
		listOfModes.add("bus");
		listOfModes.add("bus");
		return listOfModes;
	}

	/** writes the nessesary file to predefined location
	 * 
	 * @param scenario The scenario which should be written
	 */
	private static void writeOutput(Scenario scenario) {
//		String fileInputDir = "../../Desktop/matsim-berlin-5.5.x/scenarios/berlin-v5.5-10pct/input/";
		String fileInputDir = "scenarios/berlin-v5.5-10pct/input/";
		
		NetworkWriter networkWriter = new NetworkWriter(scenario.getNetwork());
		String filenameNetwork = fileInputDir + "network_linTim.xml";
		networkWriter.write(filenameNetwork);
		
		TransitScheduleWriterV2 scheduleWriter = new TransitScheduleWriterV2(scenario.getTransitSchedule());
		String filenameSchedule = fileInputDir + "transitSchedule_linTim.xml";
		scheduleWriter.write(filenameSchedule);
		
		System.out.println("writing Vehicles.. ");
		MatsimVehicleWriter transitVehiclesWriter = new MatsimVehicleWriter(scenario.getTransitVehicles());
		String filenameTransitVehicles = fileInputDir + "transitVehicles_linTim.xml";
		transitVehiclesWriter.writeFile(filenameTransitVehicles);		
	}

}