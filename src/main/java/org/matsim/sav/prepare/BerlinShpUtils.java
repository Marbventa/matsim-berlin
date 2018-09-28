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

	private Map<Integer, Geometry> berlinAreaGeometries;
	private Map<Integer, Geometry> drtServiceAreaGeometries;

	public BerlinShpUtils(String berlinShapeFile, String drtServiceAreaShapeFile) {
		this.berlinAreaGeometries = loadShapeFile(berlinShapeFile);
		this.drtServiceAreaGeometries = loadShapeFile(drtServiceAreaShapeFile);
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

	public boolean isCoordInBerlinArea(Coord coord) {
		return isCoordInArea(coord, berlinAreaGeometries);
	}

	public boolean isCoordInDrtServiceArea(Coord coord) {
		return isCoordInArea(coord, drtServiceAreaGeometries);
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

