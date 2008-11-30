/* *********************************************************************** *
 * project: org.matsim.*
 * CompressedRoute.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.marcel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.routes.AbstractRoute;
import org.matsim.population.routes.CarRoute;

public class CompressedCarRoute extends AbstractRoute implements CarRoute {

	private final ArrayList<Link> route = new ArrayList<Link>(0);
	private final Map<Link, Link> subsequentLinks;
	private double travelCost = Double.NaN;
	/** number of links in uncompressed route */
	private int uncompressedLength = 0;

	public CompressedCarRoute(final Map<Link, Link> subsequentLinks) {
		this.subsequentLinks = subsequentLinks;
	}

	public List<Link> getLinks() {
		ArrayList<Link> links = new ArrayList<Link>(this.uncompressedLength);
		Link previousLink = null;
		for (Link link : this.route) {
			if (previousLink == null) {
				links.add(link);
				previousLink = link;
			} else {
				boolean found = false;
				do {
					Node node = previousLink.getToNode();
					for (Link outLink : node.getOutLinks().values()) {
						if (link.getId().equals(outLink.getId())) {
							found = true;
							links.add(link);
							previousLink = link;
						}
					}
					if (!found) {
						// the link in the route was not part of the current outgoing links, so follow the subsequent Links
						Link tmpLink = this.subsequentLinks.get(previousLink);
						links.add(tmpLink);
						previousLink = tmpLink;
					}
				} while (!found);
			}
		}

		return links;
	}

	@Override
	public List<Id> getLinkIds() {
		List<Link> links = getLinks();
		List<Id> ids = new ArrayList<Id>(links.size());
		for (Link link : links) {
			ids.add(link.getId());
		}
		return ids;
	}

	public List<Node> getNodes() {
		ArrayList<Node> nodes = new ArrayList<Node>(this.uncompressedLength + 1);

		if (this.route.size() == 0) {
			if (getStartLink() != getEndLink()) {
				nodes.add(getStartLink().getToNode());
			}
		} else {
			Link previousLink = null;
			for (Link link : this.route) {
				if (previousLink == null) {
					nodes.add(link.getFromNode());
					nodes.add(link.getToNode());
					previousLink = link;
				} else {
					boolean found = false;
					do {
						Node node = previousLink.getToNode();
						for (Link outLink : node.getOutLinks().values()) {
							if (link.getId().equals(outLink.getId())) {
								found = true;
								nodes.add(link.getToNode());
								previousLink = link;
							}
						}
						if (!found) {
							// the link in the route was not part of the current outgoing links, so follow the subsequent Links
							Link tmpLink = this.subsequentLinks.get(previousLink);
							nodes.add(tmpLink.getToNode());
							previousLink = tmpLink;
						}
					} while (!found);
				}
			}
		}

		return nodes;
	}

	public CarRoute getSubRoute(final Node fromNode, final Node toNode) {
		List<Node> nodes = getNodes();
		boolean foundFromNode = false;
		boolean foundToNode = false;
		for (Iterator<Node> iter = nodes.iterator(); iter.hasNext(); ) {
			Node node = iter.next();
			if (!foundFromNode) {
				if (fromNode.equals(node)) {
					foundFromNode = true;
				} else {
					iter.remove();
				}
			} else if (foundToNode) {
				iter.remove();
			} else if (toNode.equals(node)) {
				foundToNode = true;
			}
		}
		if (!foundFromNode || !foundToNode) {
			throw new IllegalArgumentException("fromNode or toNode are not part of this route.");
		}
		CarRoute subRoute = new CompressedCarRoute(this.subsequentLinks);
		subRoute.setNodes(nodes);
		return subRoute;
	}

	public double getTravelCost() {
		return this.travelCost;
	}

	public void setLinks(final Link startLink, final List<Link> srcRoute, final Link endLink) {
		this.route.clear();
		setStartLink(startLink);
		setEndLink(endLink);
		if (srcRoute == null || srcRoute.size() == 0) {
			this.uncompressedLength = 0;
			return;
		}
		Link previousLink = null;
		for (Link link : srcRoute) {
			if (previousLink != null) {
				if (!this.subsequentLinks.get(previousLink).equals(link)) {
					this.route.add(link);
				}
			} else {
				this.route.add(link);
			}
			previousLink = link;
		}
		if (this.route.get(this.route.size()-1) != previousLink) {
			// add the last link to mark end of route
			this.route.add(previousLink);
		}
		this.route.trimToSize();
		this.uncompressedLength = srcRoute.size();
//		System.out.println("uncompressed size: \t" + this.uncompressedLength + "\tcompressed size: \t" + this.route.size());
	}

	public void setNodes(final String route) {
		this.route.clear();
		String[] parts = route.trim().split("[ \t\n]+");
		if (parts.length == 1) {
			// exactly one node, so the route must be defined by the start and end leg
			this.uncompressedLength = 0;
			return;
		}

		Node previousNode = null;
		Link previousLink = null;
		for (String id : parts) {
			if (previousNode != null) {
				// find link from prevNode to node
				Link link = null;
				for (Link tmpLink : previousNode.getOutLinks().values()) {
					if (id.equals(tmpLink.getToNode().getId().toString())) {
						link = tmpLink;
						break;
					}
				}

				if (previousLink != null) {
					if (!this.subsequentLinks.get(previousLink).equals(link)) {
						this.route.add(link);
					}
				} else {
					this.route.add(link);
				}
				previousLink = link;
				previousNode = link.getToNode();
			} else {
				previousNode = ((NetworkLayer) Gbl.getWorld().getLayer(NetworkLayer.LAYER_TYPE)).getNode(id);
			}
		}
		if (this.route.size() == 0 || this.route.get(this.route.size()-1) != previousLink) {
			// add the last link to mark end of route
			this.route.add(previousLink);
		}
		this.route.trimToSize();
		this.uncompressedLength = parts.length - 1;
//		System.out.println("uncompressed size: \t" + this.uncompressedLength + "\tcompressed size: \t" + this.route.size());
	}

	public void setNodes(final List<Node> srcRoute) {
		setNodes(null, srcRoute, null);
	}
	
	public void setNodes(final Link startLink, final List<Node> srcRoute, final Link endLink) {
		this.route.clear();
		setStartLink(startLink);
		setEndLink(endLink);
		Node previousNode = null;
		Link previousLink = null;
		for (Node node : srcRoute) {
			if (previousNode != null) {
				// find link from prevNode to node
				Link link = null;
				for (Link tmpLink : previousNode.getOutLinks().values()) {
					if (tmpLink.getToNode().equals(node)) {
						link = tmpLink;
						break;
					}
				}

				if (previousLink != null) {
					if (!this.subsequentLinks.get(previousLink).equals(link)) {
						this.route.add(link);
					}
				} else {
					this.route.add(link);
				}
				previousLink = link;
			}
			previousNode = node;
		}
		if (this.route.get(this.route.size()-1) != previousLink) {
			// add the last link to mark end of route
			this.route.add(previousLink);
		}
		this.route.trimToSize();
		this.uncompressedLength = srcRoute.size() - 1;
//		System.out.println("uncompressed size: \t" + this.uncompressedLength + "\tcompressed size: \t" + this.route.size());
	}

	@Override
	public double getDist() {
		double dist = super.getDist();
		if (Double.isNaN(dist)) {
			dist = calcDistance();
		}
		return dist;
	}

	private double calcDistance() {
		double dist = 0;
		for (Link link : getLinks()) {
			dist += link.getLength();
		}
		setDist(dist);
		return dist;
	}

	public void setNodes(final List<Node> route, final double travelTime, final double travelCost) {
		setNodes(route);
		setTravelTime(travelTime);
		this.travelCost = travelCost;
	}

}
