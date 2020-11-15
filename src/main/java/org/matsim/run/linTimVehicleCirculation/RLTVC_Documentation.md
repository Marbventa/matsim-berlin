---
title: "RunLinTimVehicleCirculation Documentation"
author: "gmarburger"
date: "11/16/2020"
output: markdown
---

## Content

### 1. Installation Guide

### 2. Clarification of most important Java classes

---

## 1. Installation Guide
Since this is the documentation for the interface MATSim -- LinTim please also check the [LinTim Documentation](https://kluedo.ub.uni-kl.de/frontdoor/deliver/index/docId/5913/file/lintim_2020-02_documentation.pdf). 
To be able to run RunLinTimVehicleScheduling you should please follow these instructions:
1. Download the [LinTim Code](https://gitlab.rlp.net/lintim/OpenLinTim/-/tree/1-unable-to-run-lintim-and-a-few-questions) preferably via Git. If you are planning on running this on MacOs please make sure you seelct the tree '1-unable-to-run-lintim-and-a-few-questions' since it contains modifications to the code. Else LinTim will possibly not work flawlessly on MacOs.

2. Follow the installation guide from the [LinTim Documentation](https://kluedo.ub.uni-kl.de/frontdoor/deliver/index/docId/5913/file/lintim_2020-02_documentation.pdf). Alternatively follow these steps:
    1. Make sure these programming languages are installed:
        * Python3 (minimum is 3.5)
        * C and C++
        * Apache ANT
        * Java. According to the [LinTim Documentation](https://kluedo.ub.uni-kl.de/frontdoor/deliver/index/docId/5913/file/lintim_2020-02_documentation.pdf) at least java8 is required. I found that having java14 installed worked for me.
    2. To be able to run the optimizations in LinTim, you are required to install a LP-solver. The following are suggested by LinTim: 
        * Xpress,
        * Cplex,
        * [Gurobi](https://www.gurobi.com).<br>
        I was using Gurobi for LinTim. It offers a free academic/research license, which however has to be renewed every three months. 
    3. Set up your LP-solver. These are the required steps for Gurobi:
        1. Set the CLASSPATH for Gurobi.
        2. Set GUROBI_HOME.
        3. Please run the following code to set up Gurobi:
            > source /opt/gurobi/linux64/bin/gurobi.sh

        For ease of use I suggest using the following code
        > export CLASSPATH="$CLASSPATH:/opt/gurobi/linux64/lib/gurobi.jar" <br>
        > export GUROBI_HOME=/opt/gurobi/linux64/ (for MacOs it is /Mac64/ ) <br>
        > source /opt/gurobi/linux64/bin/gurobi.sh <br>
        > exit() <br>

        On MacOs I suggest adding the following to your .profile:
        > export CLASSPATH="$CLASSPATH:/Library/gurobi902/mac64/lib/gurobi.jar" <br>
        > export GUROBI_HOME=/Library/gurobi902/mac64

        Alternatively the following function also holds the required information:
        org.matsim.run.linTimVehicleCirculation.TerminalCommandsHelper.runLinTimConsole has to be adjusted regarding CLASSPATH and GUROBI_HOME.
3. Setting up LinTim:
After LinTim has been downloaded a couple of things need to be set-up. Please follow them.
    1. RunLinTimVehicleCirculation expects a directory
        > OpenLinTim/datasets/MATSIMexport

        The easiest way to create this directory would be to duplicate a preexisting dataset-directory (for example OpenLinTim/datasets/athens)

    2. LinTim will be running in an IDE. I had an issue with my PATH variable when running LinTim, so I suggest adding the following to the file
        > OpenLinTim/src/base.sh

    > export PATH=/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:/opt/X11/bin

    This is the PATH-variable which contains the information about Python and C/C++. The easiest way to retrieve this information is to write

    > $PATH

    in the Terminal.
4. The MATSim export needs a place to write conversion tables. Please create the following directory.
    > OpenLinTim/datasets/MATSIMexport/ConversionTables/

5. Please change the following variables in the RunLinTimVehicleCirculation code:
    * RunLinTimVehcileCirculation.main() <br>
    Line 55: String pathToMatsimExportDir <br>
    Set this variable to the path of the created MATSim Export Path.
    * ExportToLintimUtils.writeListToCSV2() <br>
    Line 45: String pathToMatsimExportDir <br>
    Set this variable to the path of the created MATSim Export Path.
6. Configuring Vehicle Scheduling. <br>
Vehicle Scheduling is the programmed which is being called by RunLinTimVehicleCirculation. To set parameters for this other program please adjust the file
    > OpenLinTim/datasets/MATSIMexport/<br>

    > rollout_whole_trips;true<br>
DM_debug;true <br>
DM_verbose;true<br>
time_units_per_minute;60<br>
period_length;3600<br>
#vs_model;SIMPLE<br>
vs_depot_index; -1<br>
vs_eval_cost_factor_empty_trips_duration;0<br>
vs_eval_cost_factor_empty_trips_length;0<br>
console_log_level; DEBUG<br>

    This will make sure you have the correct configuration to run the interface MATSim - LinTim. Also add the parameter
    > vs_turn_over_time;  <br>

    The value is in minutes. If you require a turn-over time of ten minutes simply add "10" after the semicolon.

For a more in-depth guide to installing LinTim please consult the [LinTim Documentation](https://kluedo.ub.uni-kl.de/frontdoor/deliver/index/docId/5913/file/lintim_2020-02_documentation.pdf).

***

## 2. Clarification of most important Java classes

The following will clarify the most important Java classes for the interface RunLinTimVehicleCirculation. The package org.matsim.run.linTimVehicleCirculation was written as part of my Master Thesis. The methods of each class will be presented and design choices will be explained. Input for each function will be in italics. Some background information to LinTim will also be given. These classes will be explained in more detail:

<details>
<summary><b>ExportObjects.java</b></summary>
<br>

Each object is used to export MATSim data sets to be imported into LinTim. Each object has a constructor method.

<details>
<summary>LinTimEdge</summary>

<p align="justify">This object contains necessary information for creating an edge in LinTim. LinTim edges have the following characteristics:</p>
<i>link_index</i> represents the ID from the edge. <br>
<i>from_stop</i> represents the beginning stop of the edge.<br>
<i>to_stop</i> represents the ending stop of the edge.<br>
<i>length</i> represents length of the edge.<br>
<i>lower_bound</i> represents minimal travel duration across the edge.<br>
<i>upper_bound</i> represents maximum travel duration across the edge.<br>
<i>headway</i> is set to default to 5 in LinTim and was kept at 5. <br>
<br>
The functions are:<br>
<ul>
<li>getUpperBound() returns the upper bound for the edge. </li>
<li>toString() returns a String of all of the characteristics separated by semicolon.</li>
</ul>
</details>

<details>
<summary>LinTimStop</summary>
<br>
<p align="justify">This object represents a stop in LinTim. These stops have the following characteristics:</p>
<i>stop_id</i> is the ID from the stop. <br>
<i>short_name</i> is a name for the stop. <br>
<i>long_name</i> is a name for the stop. Please note, the name of a stop is not relevant for Vehicle Scheduling. <br>
<i>x_coordinate</i> is the X-Coordinate of a stop. <br>
<i>y_coordinate</i> is the Y-Coordinate of a stop. <br>
<br>
Functions are:<br>
<ul>
<li>ultimateNode() returns a stop for LinTim. This stop has the maximum value of integers as ID.</li>
<li>getStopId() returns the ID for a LinTim stop.</li>
</details>

<details>
<summary>LinTimTripEntry</summary>
<br>
<p align="justify"> This object represents an entry in the LinTim Trip data set. Each trip has the following characteristics:</p>
<i>startId</i> represents the ID of the entry. It has to be unique.<br>
<i>periodicStartId</i> represents the ID of a periodic-trip-ID.<br>
<i>stationId</i> represents the ID of the stop where the strip begins. <br>
<i>startTime</i> beginning time of the trip. <br>
<i>endId</i> represents an ID in LinTim. <br>
<i>periodicEndId</i> represents the ID of a periodic-trip-ID. <br>
<i>endStation</i> represents the last station of the trip. <br>
<i>endTime</i> represents the time at which the trip ends. <br>
<i>line</i> represents the line on which the trip is taking place.<br>
<br>

The functions are:<br>
<ul>
<li>toString() returns all characteristics of an entry separated by semicolon.</li>
</ul>
</details>
</details>

---

<details>
<summary><b>ExportToLinTimUtils</b></summary>
<br>
<p align="justify">This class assists in creating the necessary files for running LinTim.</p>
<br>
<details>
<summary>writeListToLinTimCSV2()</summary>
<br>
<i>fileName</i> expects a String as input. It has to be one of the following: "Activities-periodic, Events-periodic, Timetable-periodic, Line-Concept, Edge, Stop, Headway, Trips." <br>
<i>collectionOfStrings</i> is preferably a List of Strings, the Strings will be pasted row by row into the file. Separation by semicolon is preferred. <br>
<br>
<p align="justify">The method is able to create files which can be used for running LinTim operations. Remember to adjust the output directory in this function.</p>
</details>

<details>
<summary>writeFile()</summary>
<br>
<i>file</i> name of the file. It is expected to be a path, separations by slash marks the actual file-name. <br>
<i>collectionOfStrings</i> preferably a List of Strings each separated by semicolon. <br>
<i>header</i> is the first row in the written File.<br>
<br>
<p align="justify">Writes the actual file, each entry of the collection marks a row in the document.</p>
</details>

<details>
<summary>combineStringsWithSemicolon()</summary>
<br>
<i>strings</i> is an undefined amount of Strings. <br>
<br>
<p align="justify">Adds all Strings together as one. Each String is appended to the first by semicolon.</p>
</details>
</details>

---

<details>
<summary><b>ImportUtils</b></summary>
<br>
<p align="justify">This class helps with reading the output from LinTim as well as some conversion tables which were previously created.</p>
<br>

<details>
<summary>readConversionTableVehicle()</summary>
<i>fileName</i> the name of the conversion map.<br>
<i>matsimConversionExportDir</i> the path to the file.<br>
<br>
<p align="justify">The function creates a mapping of MATSim VehicleIDs with their corresponding LinTim line.</p>
</details>

<details>
<summary>readLinTimVSOutput()</summary>
<br>
<i>fileName</i> is the name of the Vehicle Scheduling output file.<br>
<i>vehicleWorkingCounter</i> is a wrapper class for naming the vehicles and creating unique ones.<br>
<i>limitToGrossraumBerlin</i> is a Boolean, if set to true it will create a different naming for the new vehicles.<br>
<i>mode</i> is a String that is used for creating VehicleID tags.<br>
<i>matsimConversionExportDir</i>is the path to the conversion tables. It is expected, that the Vehicle Scheduling output directory is located on the same hierarchy as this path.<br>
<br>
<p align="justify">This method reads the output from LinTim's Vehicle Scheduling. Columns 2 and 13 relevant, column 2 indicates which vehicle serves the MATSim Departure, column 13 is the equivalent of MATSim VehicleID represented by a substitute integer.</p>
</details>

<details>
<summary>createLinTimImportVehicleID()</summary>
<br>
<i>mode</i> is a String that is used for creating VehicleID tags.<br>
<i>limitToGrossraumBerlin</i> is a Boolean, if set to true it will create a different naming for the new vehicles.<br>
<i>vehicleNumber</i> represents a unique String that results in an ID. <br>
<br>
<p align="justify">Creates a new MATSim VehicleID corresponding to the vehicleNumber. Mode and limitToGrossraumBerlin are also added to the IDs as Strings.</p>
</details>

<details>
<summary>readLinTimVSVehicleRoutesOutput()</summary>
<br>
<i>fileName</i> is the name of the Vehicle Scheduling output file.<br>
<i>matsimConversionExportDir</i> is the path to the conversion tables. It is expected, that the Vehicle Scheduling output directory is located on the same hierarchy as this path.<br>
<br>
<p align="justify">Maps a List of each MATSim VehicleID as well as empty trips (Leerfahrten) to their new LinTim Vehicle which is represented as an Integer as key in the map.</p>
</details>

<details>
<summary>readConversionTableLinTimLineToVecId()</summary>
<br>
<i>fileName</i> is the name of the Vehicle Scheduling output file.<br>
<i>matsimConversionExportDir</i> is the path to the conversion tables. It is expected, that the Vehicle Scheduling output directory is located on the same hierarchy as this path.<br>
<br>
<p align = "justify"> Reads the conversion map of MATSim VehicleID to their replacing Integers. Returns a Map that has the replacing Integers as keys.</p>
</details>
</details>

---

<details>
<summary><b>MapToLinTimStops</b></summary>
<br>
<p align="justify">Creates Maps concerning LinTim stops.</p>
<br>
<details>
<summary>mapTrsToLinTimStop()</summary>
<br>
<i>transitSchedule</i> is a MATSim TransitSchedule.<br>
<i>network</i> is a MATSim Network.<br>
<i>conversionMapOfStopIds</i> is a map which contains the coordinates of every TransitRouteStop in a network and an integer representing these coordinates.<br>
<i>isMetropolitianArea</i> is a Boolean set to true, if the network should be split into different transit-modes.<br>
<i>predefinedFilter</i> should be set to "mode", if "isMetropolitianArea = true". <br>
<i>limitToGrossraumBerlin</i> is a Boolean set to true, if buses should be split into running inside and running outside of Berlin.<br>
<i>filterObject</i> is the transport-mode which the network is filtered by.<br>
<br>
<p align="justify">Firstly filters the network and transitSchedule (see SelectRoutesForLinTim.filterByModeAndId()). The remaining TransitRouteStops are then transformed into LinTim stops, the ID correlates to the coordinates of the TransitRouteStop, also adding a fictional stop, the ultimateNode. This ultimate node is needed to ensure LinTim works, since the dijkstra from LinTim requires a single network. The Berlin Scenario does not have a single network for every transport-mode, for example the U55 is not officially connected to any other TransitLine.</p>
</details>

<details>
<summary>getMapOfCoordAndLintimStopId()</summary>
<br>
<i>transitSchedule</i> is a MATSim TransitSchedule.<br>
<i>network</i> is a MATSim Network.<br>
<i>isMetropolitianArea</i> is a Boolean set to true, if the network should be split into different transit-modes.<br>
<i>predefinedFilter</i> should be set to "mode", if "isMetropolitianArea = true". <br>
<i>limitToGrossraumBerlin</i> is a Boolean set to true, if buses should be split into running inside and running outside of Berlin.<br>
<i>filterObject</i> is the transport-mode which the network is filtered by.<br>
<br>
<p align="justify">Will create a map of TransitRouteStop coordinates and an integer that substitutes these coordinates. Stops with the same coordinates will have the same corresponding integer.</p>

<details>
<summary>Design Choice</summary>
<p align="justify">Having an intermediary step between MATSim TransitRouteStop and an Integer for LinTim stops allows me to have two identical stops on the same LinTim node, which would otherwise not have existed. Multiple stops can now be on one node, this may allow transit vehicles to interchange, if the station is exactly identical.</p>
</details>
</details>

<details>
<summary>writeStopsToCSV2()</summary>
<br>
<i>conversionMapOfStopIds</i> is a map which contains the coordinates of every TransitRouteStop in a network and an integer.<br>
<i>matsimConversionExportDir</i> is the path to the conversion directory.<br>
<br>
<p align="justify">This function will create a csv2 document of the mapping from coordinates to LinTim stop IDs.</p>
</details>
</details>

---

<details>
<summary><b>MatsimExportForLinTim</b></summary>
<br>
<p align="justify">MatsimExportForLinTim is the class which calls other functions to assist in creating the necessary output for LinTim and then exports these to a directory.</p>
<br>
<details>
<summary>export()</summary>
<br>
<i>scenario</i> expects a MATSim scenario. <br>
<i>isMetropolitianArea</i> is a Boolean set to true, if the network should be split into different transit-modes.<br>
<i>predefinedFilter</i> is a String giving information about how the TransitRoutes should be filtered. Expected is "mode", currently no alternative is implemented.<br>
<i>limitToGrossraumBerlin</i>is a Boolean set to true, if buses should be split into running inside and running outside of Berlin.<br>
<i>filterObject</i> defines the transport-mode which should be exported.<br>
<i>matsimConversionExportDir</i> is the path to the conversion directory.<br>
<br>
<p align="justify">This function is called by RunLinTimVehicleCirculation.main(). It is used to export all necessary files for LinTim's Vehicle Scheduling. For this process two Maps are created: firstly mapping VehicleIDs to a LinTim line ID, secondly creating a Map for Coordinates of each TransitRouteStops and an equivalent LinTim stop ID. Afterwards methods to create is required file are called. </p>

<details>
<summary>Design Choice</summary>
<p align = "justify">Creating these two maps helps with the conversion process. Converting the MATSim Vehicles into LinTim lines helps with creating vehicle workings when importing. More importantly it is not possible as simple to create the same TransitLine from MATSim as a LinTim line, since the travel duration along the links, waiting times at stops as well as the length of the line varies strongly in MATSim TransitRoutes. This issue would need to be addressed when creating LinTim lines. </p>
<p align="justify">Creating a mapping onto the coordinates instead of the TransitRouteStop itself helps since some TransitRouteStops in MATSim have different IDs but yet the same coordinate, do to the GTFS2MATSim conversion. When using the TransitRouteStop coordinates it circumvents some of these issues.</p>
</details>
</details>

<details>
<summary>createTripsFile()</summary>
<br>
<i>transitSchedule</i> is a MATSim TransitSchedule.<br>
<i>network</i> is a MATSim Network.<br>
<i>conversionMapVecIdToLinTimLineId</i> contains a mapping of MATSim VehicleIDs with their corresponding integer of LinTim line ID.<br>
<i>conversionMapOfStop</i> is a map which contains the coordinates of every TransitRouteStop in a network and an integer representing these coordinates.<br>
<i>isMetropolitianArea</i> is a Boolean set to true, if the network should be split into different transit-modes.<br>
<i>predefinedFilter</i> is a String giving information about how the TransitRoutes should be filtered. Expected is "mode", currently no alternative is implemented.<br>
<i>limitToGrossraumBerlin</i>is a Boolean set to true, if buses should be split into running inside and running outside of Berlin.<br>
<i>filterObject</i> defines the transport-mode which should be exported.<br>
<i>matsimConversionExportDir</i> is the path to the conversion directory.<br>
<br>
<p align="justify">The function iterates through selected TransitRoutes and creates a LinTimTripEntry for every Departure in each TransitRoute. The following factors are set to -1, since they are not evaluated in Vehicle Scheduling: startId, periodicStartId, endId, periodicEndId. Trips in LinTim describe the drive of a vehicle from one stop to another. An exported trip entry connects thus the first TransitRouteStop of a TransitRoute with the last TransitRouteStop of a TransitRoute. As such the startTime and endTime are equivalent to the DepartureTime and the arrival time at the TransitRouteStops. The equivalent for the MATSim VehicleID is the LinTim line. The map is then exported as a file.</p>
</details>

<details>
<summary>createStopFile()</summary>
<br>
<i>transitSchedule</i> is a MATSim TransitSchedule.<br>
<i>network</i> is a MATSim Network.<br>
<i>conversionMapOfStop</i> is a map which contains the coordinates of every TransitRouteStop in a network and an integer representing these coordinates.<br>
<i>isMetropolitianArea</i> is a Boolean set to true, if the network should be split into different transit-modes.<br>
<i>predefinedFilter</i> is a String giving information about how the TransitRoutes should be filtered. Expected is "mode", currently no alternative is implemented.<br>
<i>limitToGrossraumBerlin</i>is a Boolean set to true, if buses should be split into running inside and running outside of Berlin.<br>
<i>filterObject</i> defines the transport-mode which should be exported.<br>
<i>matsimConversionExportDir</i> is the path to the conversion directory.<br>
<br>
<p align="justify">This function firstly calls MapToLintimStops.mapTrsToLintimStop() and creates a Map of every LinTimStop. The stop attributes are then combined to a single String in a List. This List is then created as a file.</p>
</details>

<details>
<summary>createEdgeFile()</summary>
<br>
<i>transitSchedule</i> is a MATSim TransitSchedule.<br>
<i>network</i> is a MATSim Network.<br>
<i>conversionMapVecIdToLinTimLineId</i> contains a mapping of MATSim VehicleIDs with their corresponding integer of LinTim line ID.<br>
<i>conversionMapOfStop</i> is a map which contains the coordinates of every TransitRouteStop in a network and an integer representing these coordinates.<br>
<i>isMetropolitianArea</i> is a Boolean set to true, if the network should be split into different transit-modes.<br>
<i>predefinedFilter</i> is a String giving information about how the TransitRoutes should be filtered. Expected is "mode", currently no alternative is implemented.<br>
<i>limitToGrossraumBerlin</i>is a Boolean set to true, if buses should be split into running inside and running outside of Berlin.<br>
<i>filterObject</i> defines the transport-mode which should be exported.<br>
<i>matsimConversionExportDir</i> is the path to the conversion directory.<br>
<br>
<p align="justify">This function creates the Edge file for LinTim, the resulting file contains a link in every row. The function iterates through every selected TransitRoute and creates an ordered Map of TransitRoutes by their occupancy in the TransitRoute, first Stop is the first entry of the Map, last Stop the last entry with the key equal to the position in the TransitRoute.</p>
<p align = "justify">After the function iterates through this newly created Map, skipping the first entry. The LinTimEdge consists of an ID, which is increased after each new entry in the  Map of every LinTim Edge. The from stop is the entry integer from the Coordinates corresponding to the TransitRouteStop of the previous map entry of all ordered TransitRouteStops, the toStop is the  equivalent of the currentTransitRouteStop. Headway is always set to 5, since it is the default in LinTim. Both bounds are equal, they are determined by the arrival - departure offset from the corresponding TransitRouteStops. </p>
<p align="justify">If the combination of the fromStop and toStop are not yet contained in the key set of Map of all LinTim edges, a new entry is added. If however the entry already exists, it is replaced, if the bound is bigger then the previous bound.</p>
<p align="justify">To the already created edges another Map of edges is added, which ensure the usability of the created network. These edges combine every LinTim stop with a node, even if the travel-duration is the maximum value of integers. Otherwise, it is not ensured, that a every node may be able to be reached from any other node.</p>
<p align="justify">The resulting Map is then used to create a List which is turned into the Edge file.</p>
<details>
<summary>Design Choice</summary>
<p align="justify">The larger bound was added to ensure that the slowest speed on the edge is considered, when creating LinTim's Vehicle Scheduling.</p>
</details>
</details>

<details>
<summary>createUltimateLinks()</summary>
<br>
<i>conversionMapOfStops</i> is a Map which contains all LinTim stops.<br>
<i>minLinTimEdgeIdNr</i> ensures that no edge ID is used twice, it is expected, that this value has not yet been used to create an edge.<br>
<br>
<p align="justify">This function creates a Map which connects every LinTim Stop with the fictional stop ultimateNode, with a travel-time of Integer.MAX_VALUE.</p>
</details>

<details>
<summary>getTreeMapOfTransitRouteStops()</summary>
<br>
<i>transitRoute</i> a MATSim TransitRoute.<br>
<br>
<p align="justifY">Creates a Map of TransitRouteStops, in which every TransitRouteStop is identifiable by key. This key indicates which position the TransitRouteStop has during the TransitRoute. First stops have the key "1", last stops have the length of the TransitRoute as key.</p>
</details>
</details>

---

<details>
<summary><b>MatsimImportFromLinTim</b></summary>
<br>
MatsimImportFromLinTim is the class containing functions to import the output from Vehicle Scheduling into MATSim. 
<br>

<details>
<summary>run()</summary>
<i>scenario</i> a MATSim Scenario.<br>
<i>isMetropolitianArea</i> is a Boolean set to true, if the network should be split into different transit-modes.<br>
<i>predefinedFilter</i> is a String giving information about how the TransitRoutes should be filtered. Expected is "mode", currently no alternative is implemented.<br>
<i>limitToGrossraumBerlin</i>is a Boolean set to true, if buses should be split into running inside and running outside of Berlin.<br>
<i>filterObject</i> defines the transport-mode which should be exported.<br>
<i>vehicleWorkingCounter</i> is a wrapper for an integer that is used to reference vehicle IDs.<br>
<i>ptLinkCounter</i> is a wrapper for an integer that is used to reference link IDs.<br>
<i>matsimConversionDir</i> is the path to the conversion directory.<br>
<br>
<p align="justify">This function creates Maps by calling functions, they are partly for conversion and reading the output from LinTim.<p>
<p align="justify">Then preexisting MATSim Vehicles are replaced by the equivalent of LinTim vehicles to serve more then one departure.</p>
</details>

<details>
<summary>returnVehicleCirculationLinTimConfig()</summary> 
<br>
<i>scenarioManipulated</i> is a MATSim Scenario. The TransitVehicles in it will be replaced with ones which serve more then a single departure.<br>
<i>conversionVecIdToLines</i> contains a mapping of MATSim VehicleIDs with their corresponding integer of LinTim line ID.<br>
<i>outputConversionLineToMATSimVehicle</i> is a mapping of LinTim's line IDs and which MATSim Vehicles they represent.<br>
<i>umlaufVecIdsWithRoutes</i> is a Map with LinTim's line IDs as keys. Their values are a list of integers, positives represent MATSim VehicleIDs in the sequence in which they need to be served by the LinTim vehicle.<br>
<i>isMetropolitianArea</i> is a Boolean set to true, if the network should be split into different transit-modes.<br>
<i>predefinedFilter</i> is a String giving information about how the TransitRoutes should be filtered. Expected is "mode", currently no alternative is implemented.<br>
<i>limitToGrossraumBerlin</i>is a Boolean set to true, if buses should be split into running inside and running outside of Berlin.<br>
<i>filterObject</i> defines the transport-mode which should be exported.<br>
<i>matsimConversionExportDir</i> is the path to the conversion directory.<br>
<br>
<p align="justify">Iterates over every selected TransitRoute and each Departure. The function will replace the relevant MATSim VehicleIDs with their new vehicle circulation vehicles as well as add them to TransitVehicles. The function calls addConnectingLinksToNetwork().</p>
</details>

<details>
<summary>addConnectingLinksToNetwork()</summary>
<br>
<i>scenario</i> a MATSim Scenario.<br>
<i>umlaufVecIdsWithRoutes</i> is a Map with LinTim's line IDs as keys. Their values are a list of integers, positives represent MATSim VehicleIDs in the sequence in which they need to be served by the LinTim vehicle.<br>
<i>conversionLineToMATSimVehicle</i> is a mapping of LinTim's line IDs and which MATSim Vehicles they represent.<br>
<i>ptLinkCounter</i>  is a wrapper for an integer that is used to reference link IDs.<br>
<br>
<p align="justify">This function adds links to the MATSim network. Each trip of a LinTim vehicle is empty the function will add a link from the end TransitRouteStop of the last served TransitRoute to the following starting TransitRouteStop of the new TransitRoute.</p>
</details>
</details>

---

<details>
<summary><b>RunLinTimVehicleCircualtion</b></summary>
<br>
<p align="justify"´>This is the class with the main() function. Running it will replace the normal MATSim Vehicles with new vehicles, created by LinTim's Vehicle Scheduling which run in circulations.</p>
<br>
<details>
<summary>main()</summary>

<br>
<i>No input is expected.</i><br>
<br>
<p align="justify">The function will iterate through different types of transport. It will firstly call a function to create a List of transport modes. The function will export, run Vehicle Circulation and import the MATSim TransitSchedule separately for each transport mode.</p>
<p align="justify">Exporting the transport mode <i>bus</i> functions differently. The transport mode bus has to be run twice. The first time the function runs the mode bus, it will only select TransitRoutes that run inside of Berlin, meaning inside a 30km radius from Berlin Brandenburger Tor. The second time it will export all not previously selected bus TransitRoutes.</p>
<p align="justify">Separating the TransitSchedule into multiple processes was necessary, since LinTim's Vehicle Scheduling does not differentiate between transit modes. Additionally, separating Berlin's buses into two processes aids in reducing process time, it reduces the amount of processing time by 25%.</p>
</details>

<details>
<summary>removesLinksWithZeroLength()</summary>
<br>
<i>scenario</i> is a MATSim Scenario.<br>
<br>
<p align="justify">Some links in the current Berlin Scenario have length 0. Usually this is not an issue, but public transport considers these in their Dijkstra operation. These links seem to cause a loop, which results in a non-ending "creating vehicle umlaeufe" during MATSim.</p>
</details>

<details>
<summary>createListOfModes()</summary>
</br>
<i>No input is expected.</i><br>
<br>
<p align="justify">Prepares and creates transit-modes for the LinTim TransitRoute selection if isMetropolitianArea is set to true. Best Case the following strings are contained:</p>
<ul>
<li>railS: selects TransitRoutes with mode rail and contain an "S" in their ID.</li>
<li>railU: selects TransitRoutes with mode rail and contain an "U" in their ID.</li>
<li>railRE: selects TransitRoutes with mode rail and contain an "RE" in their ID.</li>
<li>railZ: selects TransitRoutes with mode rail and that do not contain any of the above Stings in their id.</li>
<li>rail-: selects all TransitRoutes with rail. This mode was added if no differentiation between rail-types should happen.</li>
<li>tram: selects TransitRoutes with mode tram.</li>
<li>bus: selects TransitRoutes with mode bus. However please take care of the Boolean limitToGrossraumBerlin. If set to true only buses with every TransitRouteStop inside of a 30km radius of the Berlin Brandenburger Tor are selected. If set to false, the other TransitRoutes are selected.</li>
</ul>
</details>

<details>
<summary>writeOutput()</summary>
<br>
<i>scenario</i> is a MATSim Scenario.<br>
<br>
<p align="justify">Writes the TransitSchedule, Network and TransitVehicles for the scenario.</p>
</details>
</details>

---

<details>
<summary><b>SelectRoutesForLinTim</b></summary>
<br>
<p align="justify">This class was created to help in selecting specific TransitRoutes for the processes used to create LinTim vehicle circulations in MATSim. For a cleaner code this was done in a separate class.</p>
<br>

<details>
<summary>filterByModeAndId()</summary>
<br>
<i>transitRoute</i> is a MATSim TransitRoute.<br>
<i>network</i> is a MATSim Network.<br>
<i>isMetropolitianArea</i> is a Boolean set to true, if the network should be split into different transit-modes.<br>
<i>predefinedFilter</i> is a String giving information about how the TransitRoutes should be filtered. Expected is "mode", currently no alternative is implemented.<br>
<i>limitToGrossraumBerlin</i>is a Boolean set to true, if buses should be split into running inside and running outside of Berlin.<br>
<i>filterObject</i> defines the transport-mode which should be exported.<br>
<br>
<p align="justify">During the process of creating LinTim's Vehicle Scheduling the TransitSchedule is split into different sections and processed separately. the filterObject defines which TransitRoutes are supposed to be selected. This is the function that filters these modes. The three main transport-modes rail, tram and bus are supported. The mode rail and bus are split into two or more categories. For more information on rail read the function <i>idIndicatesWhichRailToFilter</i>.</p>
</p align="justify">Buses in the process of creating LinTim's Vehicle Scheduling are split into two parties: TransitRoutes with mode bus that have all of their TransitRouteStops inside of Berlin, and then the other TransitRoutes. </p>
<br>
<details>
<summary>Design Choice</summary>
<p align="justify">Separating bus TransitRoutes into two sections was not fully voluntary. Processing all of Berlin Scenario took about 20 hours. Splitting the bus TransitRoutes reduced the time by about 4 hours, also it seemed to make sense from a transport engineering standpoint, that bus providers would not usually serve both inside of Berlin and outside.</p>
</details>
</details>

<details>
<summary>idIndicatesWhichRailToFilter()</summary>
<br>
<i>transitRoute</i> is a MATSim TransitRoute.<br>
<i>filterObject</i> is a String that indicates after which additional criteria should be selected by. Here it expects that all Strings start with "rail", else this function will return false.<br>
<br>
<p align="justify">This function is used to differentiate between different kinds of rail vehicles. Subway lines should not work together with city-rail lines. Hence, the following classifications are implemented:</p>
<ul>
<li>railS: selects TransitRoutes with mode rail and contain an "S" in their ID.</li>
<li>railU: selects TransitRoutes with mode rail and contain an "U" in their ID.</li>
<li>railRE: selects TransitRoutes with mode rail and contain an "RE" in their ID.</li>
<li>railZ: selects TransitRoutes with mode rail and that do not contain any of the above Stings in their ID.</li>
<li>rail-: selects all TransitRoutes with rail. This mode was added if no differentiation between rail-types should occur.</li>
</ul>
</details>

<details>
<summary>booleanLessThen30kmFromBBTor()</summary>
<br>
<i>coordiantesOfTRS</i> a pair of Coordinates from a TransitRouteStop.</br>
<br>
<p align="justify">This function returns true, if the coordinates are inside of a certain distance. Currently, the distance is set to 30 km. To make calculating easier, Pythagoras is used.</p>
</details>

<details>
<summary>nineHundredQkmOfCoordsAsDouble()</summary>
<br>
<i>No input is expected.</i><br>
<br>
<p align="justify">This function returns a distance of 30 km as a Square. For this two points of interest were used: Berlin Brandenburger Tor and Kaserne Gatow. Google Maps says that the points are about 30 km apart. The square of the difference is returned, since it is compared against the difference in coordinates from another TransitRouteStop using Pythagoras.</p>
<details>
<summary>Design Choice</summary>
<p align="justify">30 km was chosen, because [Hans Heuer](https://link.springer.com/chapter/10.1007%2F978-3-663-05165-7_1) defines Berlin with a 60 km diameter.</p>
</details>
</details>
</details>

---

<details>
<summary><b>TerminalCommandsHelper</b></summary>
<br>
<p align="justify">This class runs LinTim from a console inside an IDE. </p>
<br>

<details>
<summary>runLinTimInConsole()</summary>
<br>
<i>pathToMATSimExportDir</i> is the path to the export directory.<br>
<i>modeRule</i> is a String containing information about which transit mode is being processed.<br>
<br>
<p align="justify">This function starts LinTim's Vehicle Scheduling. Please make sure, that the CLASSPATH and GUROBI_HOME are set correctly here. </p>
<p align="justify">After Vehicle Scheduling has finished a String will be printed in the IDEs Terminal. If the exit value is 0, the process continues. If the exit value is anything else, it is printed into the console.</p>
</details>
</details>

---

<details>
<summary><b>TransitUtils</b></summary>
<br>
<p align="justify">While writing this package I came across two issues. Both of them are being addressed by this class. Firstly, returning the last TransitRouteStop of a line was not simple. Secondly, getting the TransitRoute from a Vehicle ID.</p>
<br>

<details>
<summary>getEndStation()</summary>
<br>
<i>transitRoute</i> is a MATSim TransitRoute.<br>
<br>
<p align="justify">Returns the last TransitRouteStop from a TransitRoute.</p>
</details>

<details>
<summary>returnTransitRoute()</summary>
<br>
<i>transitVehicleID</i> a MATSim TransitVehicle ID.<br>
<br>
<p align="justify">This function only works in the Berlin Scenario and is hence not in use currently. It crops to string of the TransitVehicle ID to evaluate the substrings and create a TransitRoute ID.</p>
</details>

<details>
<summary>returnTransitLine()</summary>
<br>
<i>transitVehicleID</i> a MATSim TransitVehicleID.<br>
<br>
<p align="justify">This function only works in the Berlin Scenario and is hence not in use currently. It crops to string of the TransitVehicleID to evaluate the substrings and create a TransitLine ID.</p>
</details>
</details>

---

<details>
<summary><b>VehicleIDToLintimLineConverter</b></summary>
<br>
<p align="justify">This class is used to create the conversion table MATSim Vehicle ID -> LinTim Line ID. </p>

<details>
<summary>createAndPrintConversionTableLintimLineId()</summary>
<br>
<i>transitSchedule</i> is a MATSim TransitSchedule.<br>
<i>network</i> is a MATSim Network.<br>
<i>isMetropolitianArea</i> is a Boolean set to true, if the network should be split into different transit-modes.<br>
<i>predefinedFilter</i> is a String giving information about how the TransitRoutes should be filtered. Expected is "mode", currently no alternative is implemented.<br>
<i>limitToGrossraumBerlin</i>is a Boolean set to true, if buses should be split into running inside and running outside of Berlin.<br>
<i>filterObject</i> defines the transport-mode which should be exported.<br>
<br>
<p align="justify">This function assigns every selected TransitRoute an Integer. The integer represents the ID of a LinTim line.</p>
</details>

<details>
<summary>writeMapToCSV2()</summary>
<br>
<i>vehicleConversionMap</i> is the Map that maps MATSim VehicleIDs to LinTim's line IDs.<br>
<i>matsimConversionExportDir</i> is the path to the directory in which the Conversions are exported to.<br>
<br>
<p align="justify">Writes the conversion map into a file at the designated directory.</p>
</details>
</details>