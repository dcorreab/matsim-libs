/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.southafrica;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.network.algorithms.NetworkCleaner;
import org.matsim.utils.geometry.geotools.MGC;
import org.matsim.utils.gis.ShapeFileReader;
import org.opengis.referencing.FactoryException;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

/**
 * NetworkGenerator kann aus einem speziell aufbereiteten *.shp-File von 
 * LineStrings ein MATSim Netzwerk generieren. Insbesondere m�ssen die 
 * LineStrings in ihren Attributen fromNode und toNode ID haben. 
 * 
 * @author laemmel
 *
 */
public class NetworkGenerator {
	private static final Logger log = Logger.getLogger(NetworkGenerator.class);
	private final NetworkLayer network;
	private final Collection<Feature> l;

	
	
	public NetworkGenerator(final Collection<Feature> l, final NetworkLayer network) {
		this.network = network;
		this.l = l;
	}

	private NetworkLayer constructNetwork() {
		createNodes();
		createLinks();
		new NetworkCleaner().run(this.network);
		
		return this.network;
	}
	
	private void createLinks() {
		for (final Feature link : this.l) {
			final int id = (Integer) link.getAttribute(1);
			final int from = (Integer) link.getAttribute(2);
			final int to = (Integer) link.getAttribute(3);
			final double minWidth =  3.75; //(Double) link.getAttribute(4);
			final double area = link.getDefaultGeometry().getLength() * minWidth; // (Double) link.getAttribute(5);
			final double length = link.getDefaultGeometry().getLength(); //(Double) link.getAttribute(6);
			final double avgWidth = Math.max(area/length, minWidth);
			
//			if (minWidth < 0.71 ){
//				log.warn("wrong flowcap!");
//				minWidth = 200;
//			}
			final double permlanes = Math.max(avgWidth,minWidth) / 3.75;
			final double flowcap = Math.max(minWidth / 3.75,1);

			this.network.createLink(Integer.toString(id), Integer.toString(from), Integer.toString(to), Double.toString(length), "1.66", Double.toString(flowcap), Double.toString(permlanes), Integer.toString(id), "");
			
			this.network.createLink(Integer.toString(id+1000000), Integer.toString(to), Integer.toString(from), Double.toString(length), "1.66", Double.toString(flowcap), Double.toString(permlanes), Integer.toString(id), "");
			
		}
			
		
	}
		
	private void createNodes() {
		for (final Feature link : this.l) {
			final LineString ls = (LineString) link.getDefaultGeometry().getGeometryN(0); 
			final Integer from = (Integer)link.getAttribute(2);
			final Integer to = (Integer)link.getAttribute(3);
			final Coordinate fromC = ls.getStartPoint().getCoordinate();
			final Coordinate toC = ls.getEndPoint().getCoordinate();
			if ( this.network.getNode(from.toString()) == null){
				this.network.createNode(from.toString(),Double.toString(fromC.x), Double.toString(fromC.y), "");
			}

			if ( this.network.getNode(to.toString()) == null){
				this.network.createNode(to.toString(),Double.toString(toC.x), Double.toString(toC.y), "");
			}
			
		}
		
	}

	public static void main(final String [] args) throws FactoryRegistryException, IOException, FactoryException, SchemaException, IllegalAttributeException, Exception {
		
		

		final String links = "./southafrica/net_graph.shp";
		
		FeatureSource l = null;
		try {
			l = ShapeFileReader.readDataFile(links);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		final Collection<Feature> ls = getPolygons(l);
		
		
		final NetworkLayer network = new NetworkLayer();

		network.setCapacityPeriod(1);
		new NetworkGenerator(ls, network).constructNetwork();
		
		new NetworkWriter(network,"southafrica/matsim_net.xml").write();
//		ShapeFileWriter.writeGeometries(genFeatureCollection((Collection<Link>) network.getLinks().values(), n),"./padang/network_" + VERSION + "/matsim_net.shp" );
		
	}

	private static Collection<Feature> genFeatureCollection(final Collection<Link> links, final FeatureSource fs) throws FactoryRegistryException, SchemaException, IllegalAttributeException, Exception{
		

//		dummy.id = -1;
		final GeometryFactory geofac = new GeometryFactory();
		final Collection<Feature> features = new ArrayList<Feature>();
		
		final AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("MultiLineString",MultiLineString.class, true, null, null, fs.getSchema().getDefaultGeometry().getCoordinateSystem());
		final AttributeType id = AttributeTypeFactory.newAttributeType(
				"ID", Integer.class);
		final AttributeType fromNode = AttributeTypeFactory.newAttributeType(
				"fromID", Integer.class);
		final AttributeType toNode = AttributeTypeFactory.newAttributeType(
				"toID", Integer.class);		
		final FeatureType ftRoad = FeatureTypeFactory.newFeatureType(
				new AttributeType[] { geom, id, fromNode, toNode }, "link");
		int ID = 0;
		for (final Link link : links){
			final Coordinate c1 = MGC.coord2Coordinate(link.getFromNode().getCoord());
			final Coordinate c2 = MGC.coord2Coordinate(link.getToNode().getCoord());
			final LineString ls = geofac.createLineString(new Coordinate[] {c1,c2});
			
			final Feature ft = ftRoad.create(new Object [] {new MultiLineString(new LineString []{ls},geofac) , ID++, link.getFromNode().getId(),link.getToNode().getId()},"network");
			features.add(ft);
				
		}


		return features;
	}

	private static Collection<Feature> getPolygons(final FeatureSource n) {
		final Collection<Feature> polygons = new ArrayList<Feature>();
		FeatureIterator it = null;
		try {
			it = n.getFeatures().features();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while (it.hasNext()) {
			final Feature feature = it.next();
//			int id = (Integer) feature.getAttribute(1);
//			MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
//			if (multiPolygon.getNumGeometries() > 1) {
//				log.warn("MultiPolygons with more then 1 Geometry ignored!");
//				continue;
//			}
//			Polygon polygon = (Polygon) multiPolygon.getGeometryN(0);
			polygons.add(feature);
	}
	
		return polygons;
	}
}
