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

package org.matsim.sav.prepare;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
* @author ikaddoura
*/

public class BerlinShpUtils {

	private Map<Integer, Geometry> carRestrictedAreaGeometries;
	private Map<Integer, Geometry> serviceAreaGeometries;

	public BerlinShpUtils(String carRestrictedAreaShpFile, String drtServiceAreaShapeFile) {
		
		if (carRestrictedAreaShpFile != null && carRestrictedAreaShpFile != "" && carRestrictedAreaShpFile != "null" ) {
			this.carRestrictedAreaGeometries = loadShapeFile(carRestrictedAreaShpFile);
		}
		
		if (drtServiceAreaShapeFile != null && drtServiceAreaShapeFile != "" && drtServiceAreaShapeFile != "null" ) {
			this.serviceAreaGeometries = loadShapeFile(drtServiceAreaShapeFile);
		}
	}

	public BerlinShpUtils(String serviceAreaShapeFile) {
		this(null, serviceAreaShapeFile);
	}

	private Map<Integer, Geometry> loadShapeFile(String shapeFile) {
		Map<Integer, Geometry> geometries = new HashMap<>();

		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
		int featureCounter = 0;
		for (SimpleFeature feature : features) {
			geometries.put(featureCounter, (Geometry) feature.getDefaultGeometry());
			featureCounter++;
		}
		return geometries;
	}

	public boolean isCoordInCarRestrictedArea(Coord coord) {
		return isCoordInArea(coord, carRestrictedAreaGeometries);
	}

	public boolean isCoordInDrtServiceArea(Coord coord) {
		return isCoordInArea(coord, serviceAreaGeometries);
	}

	private boolean isCoordInArea(Coord coord, Map<Integer, Geometry> areaGeometries) {
		boolean coordInArea = false;
		for (Geometry geometry : areaGeometries.values()) {
			Point p = MGC.coord2Point(coord);

			if (p.within(geometry)) {
				coordInArea = true;
			}
		}
		return coordInArea;
	}
	
}

