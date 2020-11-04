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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

/**
* @author gmarburger
*/

public class ImportUtils {
	/** Creates a conversion Table from File. MATSim Vehicle Id to LinTimLine.
	 * 
	 * @param fileName fileName from 
	 * @return Map which maps LinTimLine Ids as integers to Matsim Vehicle Ids.
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	static Map<Id<Vehicle>, Integer> readConversionTableVehicle(String fileName, String matsimConversionExportDir) throws NumberFormatException, IOException{
		Map<Id<Vehicle>, Integer> conversionMapToMatsin = new HashMap<>();
		
		String file = matsimConversionExportDir + fileName;
		
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			
			br.readLine();
			String lineIterator;
			while((lineIterator = br.readLine())!= null) {
				String[] sT = lineIterator.split("; ");
				conversionMapToMatsin.put(Id.create(sT[1], Vehicle.class), Integer.valueOf(sT[0]));
			}
			
			br.close();
			fr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return conversionMapToMatsin;
	}

	/* del */
	static Map<Integer, Coord> readConversionTableStops(String fileName) throws NumberFormatException, IOException{
		Map<Integer,Coord> conversionTableStops = new HashMap<>();
		
		String matsimConversionDir = "../../git/OpenLinTimMac/OpenLinTim/datasets/MATSIMexport/ConversionTables/";
		String file = matsimConversionDir + fileName;
		
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			
			br.readLine();
			String lineIterator;
			while((lineIterator = br.readLine())!= null) {
				String[] sT = lineIterator.split("; ");
				double xCoord = Double.valueOf(sT[1]);
				double yCoord = Double.valueOf(sT[2]);
				Coord coord = new Coord(xCoord, yCoord);
				conversionTableStops.put(Integer.valueOf(sT[0]), coord);
			}
			
			br.close();
			fr.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return conversionTableStops;
	}
	
	/** reads the VS output from LinTim and creates new MATSim Vehicle Ids corresponding to 
	 * 
	 * @param fileName name of VS file.
	 * @param vehicleWorkingCounter Wrapper to create new MATSim vehicle working Ids.
	 * @param limitToGrossraumBerlin If bus, is it ouside or inside of Berlin.
	 * @param mode Which mode is currently being imported.
	 * @param matsimConversionExportDir Directory of the ConversionTables. Expectation is, that under "../vehicle-scheduling/" the vehicle-scheduling dir may be found.
	 * @return
	 * @throws IOException
	 */
	// # circulation-ID; vehicle-ID[1]; trip-number of this vehicle; type; aperiodic-start-ID; periodic-start-ID; start-stop-id; start-time; aperiodic-end-ID; periodic-end-ID; end-stop-id; end-time; line-id[12]
	static Map<Integer, Id<Vehicle>> readLinTimVSOutput(String fileName, VehicleWorkingIntegerWrapper vehicleWorkingCounter, boolean limitToGrossraumBerlin, 
			String mode, String matsimConversionExportDir) throws IOException{
		Map<Integer, Id<Vehicle>> mapLinTimLineNewMatsimCirculationVehicleId = new HashMap<>();
		String file = matsimConversionExportDir + "../vehicle-scheduling/" + fileName;
		
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			
			String lineIterator;
			br.readLine();
			while((lineIterator = br.readLine())!= null) {
				String[] sT = lineIterator.split("; ");
				Integer lineId = Integer.valueOf(sT[12]);
				if(!mapLinTimLineNewMatsimCirculationVehicleId.keySet().contains(lineId) && lineId > 0) {
					mapLinTimLineNewMatsimCirculationVehicleId.put(lineId, createLinTimImportVehicleId(mode, limitToGrossraumBerlin, sT[1]));
					vehicleWorkingCounter.increaseVehicleWorkingCounter();
				}
			}
			
			br.close();
			fr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return mapLinTimLineNewMatsimCirculationVehicleId;
	}


	/** Creates a new Vehicle Id for vehicle working. 
	 * 
	 * @param mode Type of Transportation mode which is being imported.
	 * @param limitToGrossraumBerlin only relevent, if Bus-vehicles are being created. If true then will create a suffix 'Berlin', otherwise 'Umwelt'.
	 * @param vehicleNumber unique identifier for the new vehicles.
	 * @return the new vehicleId for vehicle circulations
	 */
	static Id<Vehicle> createLinTimImportVehicleId(String mode, boolean limitToGrossraumBerlin, String vehicleNumber) {
		if(mode == "bus") {
			if(limitToGrossraumBerlin) { 
				return Id.create("pt_" + mode+ "_Berlin_" + vehicleNumber + "_umlauf", Vehicle.class);
			} else {
				return Id.create("pt_" + mode + "_Umwelt_" + vehicleNumber + "_umlauf", Vehicle.class);
			}
		} else {
			return Id.create("pt_" + mode + "_" + vehicleNumber + "_umlauf", Vehicle.class);
		}
	}
	
	/* del */
	static Set<List<Integer>> readLinTimVSEmptyRoutesOutput(String fileName) throws IOException{
		Set<List<Integer>> setLinkedNodes = new HashSet<>();
		
		String vehicleSchedulingDir = "../../git/OpenLinTimMac/OpenLinTim/datasets/MATSIMexport/vehicle-scheduling/";
		String file = vehicleSchedulingDir + fileName;
		
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			
			String lineIterator;
			br.readLine();
			while((lineIterator = br.readLine())!= null) {
				String[] sT = lineIterator.split("; ");
				int lineId = Integer.valueOf(sT[12]);
				if(lineId < 0) {
					int fromStop = Integer.valueOf(sT[6]);
					int toStop = Integer.valueOf(sT[10]);
					
					List<Integer> connectedNodes = new ArrayList<>();
					connectedNodes.add(fromStop);
					connectedNodes.add(toStop);
					
					if(!setLinkedNodes.contains(connectedNodes)) {
						setLinkedNodes.add(connectedNodes);
					}
				}
			}
			
			br.close();
			fr.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		return setLinkedNodes;
	}

	/** Creates a map which orders the MATSim vehicles to the LinTimLines. Since linTimLines are the new Matsim-Vehicle-Working-Ids that means this represents all 
	 * departures which the new vehicle has to serve.
	 * 
	 * @param fileName Name Output file.
	 * @param matsimConversionExportDir Directory of the ConversionTables. Expectation is, that under "../vehicle-scheduling/" the vehicle-scheduling dir may be found.
	 * @returna Map which contains LinTimeLinIds as key, Values are lists of MATSim Ids which are being replaced.
	 * @throws IOException
	 */
	static Map<Integer, List<Integer>> readLinTimVSVehicleRoutesOutput (String fileName, String matsimConversionExportDir) throws IOException{
		Map<Integer, List<Integer>> mapUmlaufVehiclesRoutes = new HashMap<>();
		String file = matsimConversionExportDir + "../vehile-scheduling/" + fileName;
		
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			
			String lineIterator;
			br.readLine();
			while((lineIterator = br.readLine())!= null) {
				String[] sT = lineIterator.split("; ");

				int umlaufVec = Integer.valueOf(sT[1]);
				int matsimVecId = Integer.valueOf(sT[12]);
				
				if(mapUmlaufVehiclesRoutes.containsKey(umlaufVec)) {
					mapUmlaufVehiclesRoutes.get(umlaufVec).add(matsimVecId);
				} else {
					List<Integer> drivenMatsimVecIds = new ArrayList<>();
					drivenMatsimVecIds.add(matsimVecId);
						
					mapUmlaufVehiclesRoutes.put(umlaufVec, drivenMatsimVecIds);
				}
			}
			
			br.close();
			fr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}		
		return mapUmlaufVehiclesRoutes;
	}

	/** Will create a Map which maps LinTimLineIds to MATSim vehicle Ids.
	 * 
	 * @param fileName from the Conversion table
	 * @param matsimConversionExportDir Directory of MATSim exports
	 * @return a map which maps the LinTimLine Id to their corresponding MATSim Vehicle Ids.
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	static Map<Integer, Id<Vehicle>> readConversionTableLinTimLineToVecId(String fileName, String matsimConversionExportDir) throws NumberFormatException, IOException{
		Map<Integer, Id<Vehicle>> conversionMapToMatsin = new HashMap<>();
		String file = matsimConversionExportDir + fileName;
		
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			
			br.readLine();
			String lineIterator;
			while((lineIterator = br.readLine())!= null) {
				String[] sT = lineIterator.split("; ");
				conversionMapToMatsin.put(Integer.valueOf(sT[0]), Id.create(sT[1], Vehicle.class));
			}
			br.close();
			fr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return conversionMapToMatsin;
	}
}