package com.mygdx.game.pathfinding;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/**
 * Created by Johannes Sjolund on 9/26/15.
 */
public class NavMeshGraphPath extends DefaultGraphPath<Connection<Triangle>> {

	private final Vector3 up = Vector3.Y;
	public Vector3 start;
	public Vector3 end;
	public Triangle startTriangle;

	public Array<PathPoint> pathPoints = new Array<PathPoint>();

	public void setStartEnd(Vector3 start, Vector3 end) {
		this.start = new Vector3(start);
		this.end = new Vector3(end);
	}

	public void setStartTriangle(Triangle startTriangle) {
		this.startTriangle = startTriangle;
	}

	public void clear() {
		pathPoints.clear();
		nodes.clear();
		start = null;
		end = null;
		startTriangle = null;
	}

	public class PathPoint {
		// Point where the path crosses a navmesh edge
		public Vector3 edgeCrossPoint;
		// Index of the navmesh triangle which the path enters after crossing
		public int crossedTriIndex;

		public PathPoint(Vector3 edgeCrossPoint, int crossedTriIndex) {
			this.edgeCrossPoint = edgeCrossPoint;
			this.crossedTriIndex = crossedTriIndex;
		}
	}


	/**
	 * Calculate the shortest path through the path triangles, using the Simple Stupid Funnel Algorithm.
	 *
	 * @return
	 */
	public Array<PathPoint> getDirectPath() {
		Array<PathPoint> rePathPoints = new Array<PathPoint>();
		if (nodes.size == 0) {
			pathPoints.add(new PathPoint(new Vector3(end), startTriangle.getIndex()));
			pathPoints.add(new PathPoint(new Vector3(start), startTriangle.getIndex()));
			rePathPoints.addAll(pathPoints);
			return rePathPoints;
		}
		Edge edge = (Edge) nodes.get(0);

		nodes.add(new Edge(nodes.get(nodes.size - 1).getToNode(), nodes.get(nodes.size - 1).getToNode(), end, end));
		Funnel funnel = new Funnel();
		funnel.pivot.set(start);
		funnel.setPlanes(funnel.pivot, edge);
		int leftIndex = 0;
		int rightIndex = 0;
		int lastRestart = 0;

		pathPoints.add(new PathPoint(new Vector3(start), edge.fromNode.getIndex()));

		for (int i = 1; i < nodes.size; ++i) {
			edge = (Edge) nodes.get(i);

			Plane.PlaneSide leftPlaneLeftDP = funnel.sideLeftPlane(edge.leftVertex);
			Plane.PlaneSide leftPlaneRightDP = funnel.sideLeftPlane(edge.rightVertex);
			Plane.PlaneSide rightPlaneLeftDP = funnel.sideRightPlane(edge.leftVertex);
			Plane.PlaneSide rightPlaneRightDP = funnel.sideRightPlane(edge.rightVertex);

			if (rightPlaneRightDP != Plane.PlaneSide.Front) {
				if (leftPlaneRightDP != Plane.PlaneSide.Front) {
					// Tighten the funnel.
					funnel.setRightPlane(funnel.pivot, edge.rightVertex);
					rightIndex = i;
				} else {
					// Right over left, insert left to path and restart scan from portal left point.
					storeEdgeCrossings(lastRestart, leftIndex, funnel.pivot, funnel.leftPortal);
					funnel.pivot.set(funnel.leftPortal);
					rightIndex = leftIndex;
					i = leftIndex;

					if (i < nodes.size - 1) {
						lastRestart = i;
						funnel.setPlanes(funnel.pivot, (Edge) nodes.get(i + 1));
						continue;
					}

					break;
				}
			}
			if (leftPlaneLeftDP != Plane.PlaneSide.Front) {
				if (rightPlaneLeftDP != Plane.PlaneSide.Front) {
					// Tighten the funnel.
					funnel.setLeftPlane(funnel.pivot, edge.leftVertex);
					leftIndex = i;
				} else {
					// Left over right, insert right to path and restart scan from portal right point.
					storeEdgeCrossings(lastRestart, rightIndex, funnel.pivot, funnel.rightPortal);
					funnel.pivot.set(funnel.rightPortal);
					leftIndex = rightIndex;
					i = rightIndex;

					if (i < nodes.size - 1) {
						lastRestart = i;
						funnel.setPlanes(funnel.pivot, (Edge) nodes.get(i + 1));
						continue;
					}
					break;
				}
			}
		}

		storeEdgeCrossings(lastRestart, nodes.size - 1, funnel.pivot, end);
		pathPoints.add(new PathPoint(new Vector3(end), nodes.get(nodes.size - 1).getToNode().getIndex()));
		nodes.removeIndex(nodes.size - 1);
		pathPoints.reverse();
		rePathPoints.addAll(pathPoints);
		return rePathPoints;

	}

	public final Plane crossingPlane = new Plane();
	private final Vector3 tmp = new Vector3();

	private void storeEdgeCrossings(int startIndex, int endIndex, Vector3 startPoint, Vector3 endPoint) {
		if (startPoint.equals(endPoint)) {
			System.out.println("ahahahah");
			return;
		}
		crossingPlane.set(startPoint, tmp.set(startPoint).add(up), endPoint);

		if (endIndex < nodes.size) {
			for (int j = startIndex; j < endIndex; j++) {
				Edge edge = (Edge) nodes.get(j);
				Vector3 x = new Vector3();
				if (edge.rightVertex.equals(startPoint) || edge.rightVertex.equals(endPoint)) {
					x.set(edge.rightVertex);
				} else if (edge.leftVertex.equals(startPoint) || edge.leftVertex.equals(endPoint)) {
					x.set(edge.leftVertex);
				} else if (Intersector.intersectSegmentPlane(edge.leftVertex, edge.rightVertex, crossingPlane, x)) {
					// Calculated edge intersection
				} else if (crossingPlane.distance(edge.rightVertex) < crossingPlane.distance(edge.leftVertex)) {
					x.set(edge.rightVertex);
				} else {
					x.set(edge.leftVertex);
				}
				pathPoints.add(new PathPoint(x, edge.toNode.getIndex()));
			}
		}
	}

	private class Funnel {
		public final Plane leftPlane = new Plane();
		public final Plane rightPlane = new Plane();
		public final Vector3 leftPortal = new Vector3();
		public final Vector3 rightPortal = new Vector3();
		public final Vector3 pivot = new Vector3();
		private Vector3 tmp = new Vector3();

		public void setLeftPlane(Vector3 pivot, Vector3 leftEdgeVertex) {
			leftPlane.set(pivot, tmp.set(pivot).add(up), leftEdgeVertex);
			leftPortal.set(leftEdgeVertex);
		}

		public void setRightPlane(Vector3 pivot, Vector3 rightEdgeVertex) {
			rightPlane.set(pivot, tmp.set(pivot).add(up), rightEdgeVertex);
			rightPlane.normal.scl(-1);
			rightPlane.d = -rightPlane.d;
			rightPortal.set(rightEdgeVertex);
		}

		public void setPlanes(Vector3 pivot, Edge edge) {
			setLeftPlane(pivot, edge.leftVertex);
			setRightPlane(pivot, edge.rightVertex);
		}

		public Plane.PlaneSide sideLeftPlane(Vector3 point) {
			return leftPlane.testPoint(point);
		}

		public Plane.PlaneSide sideRightPlane(Vector3 point) {
			return rightPlane.testPoint(point);
		}
	}
}
