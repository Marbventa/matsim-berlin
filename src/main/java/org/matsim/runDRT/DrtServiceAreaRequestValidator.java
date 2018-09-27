/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.runDRT;

import java.util.HashSet;
import java.util.Set;

import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.data.validator.DefaultDrtRequestValidator;
import org.matsim.contrib.drt.data.validator.DrtRequestValidator;

/**
* @author ikaddoura
*/

public class DrtServiceAreaRequestValidator implements DrtRequestValidator {

	public static final String FROM_LINK_NOT_IN_SERVICE_AREA_CAUSE = "from_link_not_in_service_area";
	public static final String TO_LINK_NOT_IN_SERVICE_AREA_CAUSE = "to_link_not_in_service_area";

	private final DefaultDrtRequestValidator delegate = new DefaultDrtRequestValidator();

	@Override
	public Set<String> validateDrtRequest(DrtRequest request) {
		
		Set<String> invalidRequestCauses = new HashSet<>();
		
		invalidRequestCauses.addAll(this.delegate.validateDrtRequest(request));
		
		boolean fromLinkInServiceArea = (boolean) request.getFromLink().getAttributes().getAttribute(RunBerlinDrtScenario1.drtServiceAreaAttribute);
		boolean toLinkInServiceArea = (boolean) request.getToLink().getAttributes().getAttribute(RunBerlinDrtScenario1.drtServiceAreaAttribute);

		if (!fromLinkInServiceArea ) {
			invalidRequestCauses.add(FROM_LINK_NOT_IN_SERVICE_AREA_CAUSE);
		}
		if (!toLinkInServiceArea) {
			invalidRequestCauses.add(TO_LINK_NOT_IN_SERVICE_AREA_CAUSE);
		}
		
		return invalidRequestCauses;
	}

}

