/********************************************************************** *
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

package org.matsim.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;

/**
* @author gmarburger
*/

public class RunBerlinScenarioWithVehicleCirculations {
	
	private static final Logger log = Logger.getLogger(RunBerlinScenario.class );

	// This main method can be used to run a standard config but with vehicle circulations.
	public static void main(String[] args) {		
		for (String arg : args) {
			log.info( arg );
		}
		
		if ( args.length==0 ) {
			args = new String[] {"scenarios/berlin-v5.5-10pct/input/berlin-v5.5-10pct.config.xml"}  ;
		}

		Config config = RunBerlinScenario.prepareConfig( args ) ;					
		config.network().setTimeVariantNetwork(true);
		config.network().setChangeEventsInputFile("/Users/Gero 1/git/matsim-berlin-MasterArbeit/scenarios/berlin-v5.5-10pct/input/networkChangeEventM44_120.xml");

		config.controler().setOutputDirectory("scenarios/berlin-v5.5-10pct/output-berlin-drt-v5.5-10pct_VehicleCirculation_M44_120");

		Scenario scenario = RunBerlinScenario.prepareScenario( config ) ;
		
		CreateVehicleCirculation.create(scenario, 10, false);

		Controler controler = RunBerlinScenario.prepareControler( scenario ) ;
		controler.run() ;
	}

}
