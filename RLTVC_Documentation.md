---
title: "RunLinTimVehicleCirculation Documentation"
author: "gmarburger"
date: "11/8/2020"
output: html_document
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

        The easiest way to create this directory would be to duplicate a pre-existing dataset-directory (for example OpenLinTim/datasets/athens)

    2. LinTim will be running in an IDE. I had an issue with my PATH variable when running LinTim, so I suggest adding the following to the file
        > OpenLinTim/src/base.sh

    > export PATH=/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:/opt/X11/bin

    This is the PATH-variable which contains the information about Python and C/C++. easiest way to retrieve this information is to write

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
s

    This will make sure you have the correct configuration to run the interface MATSim - LinTim. Also add the parameter
    > vs_turn_over_time;  <br>

    The value is in minutes. If you require a turn-over time of ten minutes simply add "10" after the semicolon.

For a more in-depth guide to installing LinTim please consult the [LinTim Documentation](https://kluedo.ub.uni-kl.de/frontdoor/deliver/index/docId/5913/file/lintim_2020-02_documentation.pdf).

---

## 2. Clarification of most important Java classes

The following will clarify the most important java-classes for the interface RunLinTimVehicleCirculation. The Package org.matsim.run.linTimVehicleCirculation was written my Master Thesis. The methods of each class will be presented and design choices will be explained. Some background information to LinTim will also be given. These classes will be explained in more detail:

* **ExportObjects.java**
* **ExportToLinTimUtils.java**
* **ImportUtils.java**
* **MapToLinTimStops.java**
* **MatsimExportForLinTim.java**
* **MatsimImportForLinTim.java**
* **RunLinTimVehicleCirculation.java (main)**
* **SelectRoutesForLinTim.java**
* **TerminalCommandsHelper.java**
* **TransitUtils.java**
* **VehicleIdToLinTimLineConverter.java**


<details open>
<summary>Debugging</summary>
<br>
Debugging is
</details>

