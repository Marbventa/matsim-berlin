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

package org.matsim.run.linTimVehicleCirculation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
* @author gmarburger
*/

public class TerminalCommandsHelper {
	/** Runs LinTim in the background. Will use CPU but not show the terminal.
	 * 
	 * @param pathToMATSimExportDir Path to the directory where the export-files were written
	 * @param modeRule Input what kind of modes were run.
	 */
	static void runLinTimInConsole(String pathToMATSimExportDir, String modeRule) {
		ProcessBuilder runLinTim = new ProcessBuilder();
		runLinTim.directory(new File(pathToMATSimExportDir)).command("bash", "-c",
				"(export CLASSPATH=\"$CLASSPATH:/Library/gurobi902/mac64/lib/gurobi.jar\"" +
				" ; export GUROBI_HOME=/Library/gurobi902/mac64" +
				" ; make vs-vehicle-schedules)");
		
		try {
			Process process = runLinTim.start();
			StringBuilder outputString = new StringBuilder();
			BufferedReader bReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String outputLine;
			
			while((outputLine = bReader.readLine()) != null) {
				outputString.append(outputLine + "\n");
			}
			
			int exitVal = process.waitFor();
			if(exitVal == 0) {
				System.out.println("\n" + "Sucessfully ran LinTim with the configuration: " + modeRule);
				System.out.println(outputString);
				System.out.println("If nothing useful was written above. Check Logs if nessesary!");
			} else {
				System.out.println("\n" + "Failed running LinTim! Please check the following for more information: " + "\n");
				System.out.println(exitVal);
				System.out.println(outputString);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/** Creates a List of Strings to prepare LinTim runs. Sets the nessesary home paths.
	 * 
	 * @return List of Strings with nessesary codes for preperation.
	 */
	private static List<String> prepareLinTim() {
		List<String> prepareCommands = new ArrayList<>();
		prepareCommands.add("export CLASSPATH=\"$CLASSPATH:/Library/gurobi902/mac64/lib/gurobi.jar\"");
		prepareCommands.add("export GUROBI_HOME=/Library/gurobi902/mac64");
		prepareCommands.add("source /Library/gurobi902/mac64/bin/gurobi.sh");
		prepareCommands.add("exit()");
		return prepareCommands;
	}
}
