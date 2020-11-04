package org.matsim.run.linTimVehicleCirculation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

public class ExportToLintimUtils {
	/* Class should be used to writing input Data for LinTim in the /ci folder of LinTim. It expects Edges
	 * and Stops to be unique -> Sets. The other collections should be Lists. */
	/** Creates a CSV2 File from a Set of Strings which are seperated by ;
	 * 
	 * @param fileName Name of the File which should be created
	 * @param collectionOfStrings Collection of Strings. These should consist of information stored seperated by ;
	 */
	public static void writeListToLinTimCSV2(String fileName, Collection<String> collectionOfStrings) {
		String fileDirectory;
		String fileEncoding;
		String header;
		
		//dir seems to be MATSIM5.5x in my case
		String matsimExportDir = "../../git/OpenLinTimMac/OpenLinTim/datasets/MATSIMexport/ConversionTables/" ;
		
	    if(fileName.equals("Activities-periodic")) {
			fileEncoding = ".giv";
			fileDirectory = "timetabling/";
			header = "# activity_index; type; from_event; to_event; lower_bound; upper_bound; passengers";
			String file = matsimExportDir + fileDirectory + fileName + fileEncoding;
			writeFile(file, collectionOfStrings, header);
			
		} else if(fileName.equals("Events-periodic")) {
			fileEncoding = ".giv";
			fileDirectory = "timetabling/";
			header = "# event_id; type; stop-id; line-id; passengers; line-direction; line-freq-repetition";
			String file = matsimExportDir + fileDirectory + fileName + fileEncoding;
			writeFile(file, collectionOfStrings, header);
			
		} else if(fileName.equals("Timetable-periodic")) {
			fileEncoding = ".tim";
			fileDirectory = "timetabling/";
			header = "#event-id; time";
			String file = matsimExportDir + fileDirectory + fileName + fileEncoding;
			writeFile(file, collectionOfStrings, header);
			
		} else if(fileName.equals("Line-Concept")) {
			fileEncoding = ".lin";
			fileDirectory = "line-planning/";
			header = "# line-id; edge-order; edge-id; frequency";
			String file = matsimExportDir + fileDirectory + fileName + fileEncoding;
			writeFile(file, collectionOfStrings, header);
			
		} else if(fileName.equals("Edge")){
			fileEncoding = ".giv";
			fileDirectory = "basis/";
			header = "# link_index; from_stop; to_stop; length; lower_bound; upper_bound";
			String file = matsimExportDir + fileDirectory + fileName + fileEncoding;
			writeFile(file, collectionOfStrings, header);
			
		} else if(fileName.equals("Stop")){
			fileEncoding = ".giv";
			fileDirectory = "basis/";
			header = "# stop-id; short-name; long-name; x-coordinate; y-coordinate";
			String file = matsimExportDir + fileDirectory + fileName + fileEncoding;
			writeFile(file, collectionOfStrings, header);
			
		} else if(fileName.equals("Headway")) {
			fileEncoding = ".giv";
			fileDirectory = "basis/";
			header = "#edge-id; headway";
			String file = matsimExportDir + fileDirectory + fileName + fileEncoding;
			writeFile(file, collectionOfStrings, header);
		} else if(fileName.equals("Trips")){
			fileEncoding = ".giv";
			fileDirectory = "delay-management/";
			header ="# start-ID; periodic-start-ID; start-station; start-time; end-ID; periodic-end-ID; end-station; end-time; line";
			String file = matsimExportDir + fileDirectory + fileName + fileEncoding;
			writeFile(file, collectionOfStrings, header);
		} else { System.out.println("Fatal error: This format is currently not supported!"); }
	}
	
	/** Writes a File which belongs to LinTims file-system with the correct information.
	 * 
	 * @param file Filename
	 * @param collectionOfStrings A collectiong of Strings which should be printed
	 * @param header First row which contains information for Users and not for LinTim
	 */
	private static void writeFile(String file, Collection<String> collectionOfStrings, String header) {
    	try (
    		FileWriter fw = new FileWriter(file);
        	BufferedWriter bw = new BufferedWriter(fw);
    		) {
    			bw.write(header);
    			bw.newLine();
    			collectionOfStrings.forEach(string -> {
    				try {
    					bw.write(string);
    					bw.newLine();
    				} catch (IOException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
    			});
    		bw.flush();
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	int fileNameBeginIndex = file.toString().lastIndexOf("/") + 1;
    	int fileNameEndIndex = file.toString().lastIndexOf(".");
    	String fileName = file.toString().substring(fileNameBeginIndex, fileNameEndIndex	);
    	System.out.println(fileName + " was susessfully written!");
	}

	/** Creates an undifined amount of Strings together to a sintle String, seperated by ;
	 * 
	 * @param strings undefined amount of Strings
	 * @return a single String
	 */
	static String combineStringsWithSemicolon(String...strings) {
		StringBuilder stringBuilderCSV2 = new StringBuilder();
		for(int i = 0; i < strings.length; i++) {
			stringBuilderCSV2 = stringBuilderCSV2.append(strings[i]);
			if(i == (strings.length - 1)) continue;
			stringBuilderCSV2 = stringBuilderCSV2.append("; ");
		}
		return stringBuilderCSV2.toString();
	}
}
