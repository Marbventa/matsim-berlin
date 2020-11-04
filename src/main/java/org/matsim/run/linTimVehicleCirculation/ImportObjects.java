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

/**
* @author gmarburger
*/

class VehicleWorkingIntegerWrapper {
	int vehicleWorkingCounter;
	
	public void increaseVehicleWorkingCounter() {
		this.vehicleWorkingCounter +=1;
	}
	
	public int getVehicleWorkingCounter() {
		return this.vehicleWorkingCounter;
	}
	
	public VehicleWorkingIntegerWrapper(int counter) {
		this.vehicleWorkingCounter = counter;
	}
	
	@Override
	public String toString() {
		return String.valueOf(this.vehicleWorkingCounter);
	}
}

class PublicTransportLinkIntegerWrapper {
	int pTLinkCounter;
	
	public void increasePTLinkCounter() {
		this.pTLinkCounter +=1;
	}
	
	public int getPTLinkCounter() {
		return this.pTLinkCounter;
	}
	
	public PublicTransportLinkIntegerWrapper(int counter) {
		this.pTLinkCounter = counter;
	}
	
	@Override
	public String toString() {
		return String.valueOf(this.pTLinkCounter);
	}
}