package org.matsim.run.linTimVehicleCirculation;

//# link_index; from_stop; to_stop; length; lower_bound; upper_bound
class LinTimEdge {
	int link_index;
	int from_stop;
	int to_stop;
	int length;
	int lower_bound;
	int upper_bound;
	int headway;
	
	public LinTimEdge(int link_index, int from_stop, int to_stop,
			int length, int lower_bound, int upper_bound, int headway) {
		this.link_index = link_index;
		this.from_stop = from_stop;
		this.to_stop = to_stop;
		this.length = length;
		this.lower_bound = lower_bound;
		this.upper_bound = upper_bound;
		this.headway = headway;
			
	}
	
	public int getUpperBound() {
		return this.upper_bound;
	}
	
	@Override
	public String toString(){
		return ExportToLintimUtils.combineStringsWithSemicolon(String.valueOf(link_index), String.valueOf(from_stop), String.valueOf(to_stop), String.valueOf(length), 
				String.valueOf(lower_bound), String.valueOf(upper_bound));
	}
}

// # stop-id; short-name; long-name; x-coordinate; y-coordinate
/** Object for LinTimStops which contain the nessesary information for importint into LinTim.
 *
 */
class LinTimStop {
	int stop_id;
	String short_name;
	String long_name;
	int x_coordinate;
	int y_coordinate;
	
	public LinTimStop(int stop_id, String short_name, String long_name,
			int x_coordinate, int y_coordinate) {
		this.stop_id = stop_id;
		this.short_name = short_name;
		this.long_name = long_name;
		this.x_coordinate = x_coordinate;
		this.y_coordinate = y_coordinate;
	}
	
	public static final LinTimStop ultimateNode() {
		LinTimStop ultimateNode = new LinTimStop(Integer.MAX_VALUE, "ultimate Node", "ultimate Node", 0, 0);
		return ultimateNode;
	}
	
	public int getStopId() {
		return stop_id;
	}
}

class LinTimTripEntry {
	int startId;
	int periodicStartId;
	int stationId;
	int startTime;
	int endId;
	int periodicEndId;
	int endStation;
	int endTime;
	int line;
	
	public LinTimTripEntry(int startId, int periodicStartId, int stationId, int startTime, int endId, int periodicEndId, int endStation, int endTime, int line) {
		this.startId = startId;
		this.periodicStartId = periodicStartId;
		this.stationId = stationId;
		this.startTime = startTime;
		this.endId = endId;
		this.periodicEndId = periodicEndId;
		this.endStation = endStation;
		this.endTime = endTime;
		this.line = line;
	}
	
	@Override
	public String toString() {
		String tripString = ExportToLintimUtils.combineStringsWithSemicolon(String.valueOf(startId), String.valueOf(periodicStartId), String.valueOf(stationId), String.valueOf(startTime),
				String.valueOf(endId), String.valueOf(periodicEndId), String.valueOf(endStation), String.valueOf(endTime), String.valueOf(line));		
		return tripString;
	}
}