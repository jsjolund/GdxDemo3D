package com.mygdx.game.pathfinding;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import java.util.Iterator;

/**
 * Created by Johannes Sjolund on 10/24/15.
 */
public class NavMeshPointPath implements Iterable<Vector3> {

	@Override
	public Iterator<Vector3> iterator() {
		return vectors.iterator();
	}

	private class PathPoint {
		/**
		 * Triangle which must be crossed to reach the next path point.
		 */
		public Triangle toNode;
		public Triangle fromNode;
		/**
		 * Path edges connected to this point. Can be used for spline generation at some point...
		 */
		public Array<Edge> connectingEdges = new Array<Edge>();

		public Vector3 point;

		public float orientation = 0;

		public PathPoint(Vector3 point, Triangle toNode) {
			this.point = point;
			this.toNode = toNode;
		}
	}

	private Vector3 up = Vector3.Y;
	private Vector3 start;
	private Vector3 end;
	private Triangle startTriangle;
	private final Plane crossingPlane = new Plane();
	private final Vector3 tmp = new Vector3();
	private PathPoint lastPoint;
	Array<Connection<Triangle>> nodes = new Array<Connection<Triangle>>();
//	GraphPath<Connection<Triangle>> nodes;

	private Array<Vector3> vectors = new Array<Vector3>();
	private Array<PathPoint> pathPoints = new Array<PathPoint>();


	public void calculateForGraphPath(NavMeshGraphPath trianglePath) {
		clear();
		for (Connection<Triangle> c : trianglePath) {
			nodes.add(c);
		}
		this.start = new Vector3(trianglePath.start);
		this.end = new Vector3(trianglePath.end);
		this.startTriangle = trianglePath.startTri;

		// Check that the start point is actually inside the start triangle,
		// if not, project it to the closest triangle edge.
		// Otherwise the funnel calculation might generate spurious path segments.


		calculatePathPoints();
	}

	public void clear() {
		vectors.clear();
		pathPoints.clear();

		nodes.clear();
		start = null;
		end = null;
		startTriangle = null;
		lastPoint = null;
	}

	public Vector3 getVector(int index) {
		return vectors.get(index);
	}

	public int getSize() {
		return vectors.size;
	}

	public Array<Vector3> getVectors() {
		return vectors;
	}

	public Triangle getToTriangle(int index) {
		return pathPoints.get(index).toNode;
	}

	public Triangle getFromTriangle(int index) {
		return pathPoints.get(index).fromNode;
	}

	public Array<Edge> getCrossedEdges(int index) {
		return pathPoints.get(index).connectingEdges;
	}

	private void addPoint(Vector3 point, Triangle toNode) {
		addPoint(new PathPoint(point, toNode));
	}

	private void addPoint(PathPoint pathPoint) {
		vectors.add(pathPoint.point);
		pathPoints.add(pathPoint);
		lastPoint = pathPoint;
	}

	/**
	 * Calculate the shortest path through the path triangles, using the Simple Stupid Funnel Algorithm.
	 *
	 * @return
	 */
	private void calculatePathPoints() {
		if (nodes.size == 0) {
			addPoint(start, startTriangle);
			addPoint(end, startTriangle);
			return;
		}
		nodes.add(new Edge(nodes.get(nodes.size - 1).getToNode(), nodes.get(nodes.size - 1).getToNode(), end, end));
		Edge edge = (Edge) nodes.get(0);
		addPoint(start, edge.fromNode);
		lastPoint.fromNode = edge.fromNode;

		Funnel funnel = new Funnel();
		funnel.pivot.set(start);
		funnel.setPlanes(funnel.pivot, edge);

		int leftIndex = 0;
		int rightIndex = 0;
		int lastRestart = 0;

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
					calculateEdgeCrossings(lastRestart, leftIndex, funnel.pivot, funnel.leftPortal);
					funnel.pivot.set(funnel.leftPortal);
					i = leftIndex;
					rightIndex = i;
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
					calculateEdgeCrossings(lastRestart, rightIndex, funnel.pivot, funnel.rightPortal);
					funnel.pivot.set(funnel.rightPortal);
					i = rightIndex;
					leftIndex = i;
					if (i < nodes.size - 1) {
						lastRestart = i;
						funnel.setPlanes(funnel.pivot, (Edge) nodes.get(i + 1));
						continue;
					}
					break;
				}
			}
		}
		calculateEdgeCrossings(lastRestart, nodes.size - 1, funnel.pivot, end);
		nodes.removeIndex(nodes.size - 1);

		for (int i = 1; i < pathPoints.size; i++) {
			PathPoint p = pathPoints.get(i);
			p.fromNode = pathPoints.get(i-1).toNode;
		}
		return;
	}

	private void calculateEdgeCrossings(int startIndex, int endIndex,
										Vector3 startPoint, Vector3 endPoint) {

		if (startIndex >= nodes.size || endIndex >= nodes.size) {
			return;
		}
		crossingPlane.set(startPoint, tmp.set(startPoint).add(up), endPoint);

		PathPoint previousLast = lastPoint;

		Edge edge = (Edge) nodes.get(endIndex);
		PathPoint end = new PathPoint(new Vector3(endPoint), edge.toNode);

		for (int i = startIndex; i < endIndex; i++) {
			edge = (Edge) nodes.get(i);
			Vector3 xPoint = new Vector3();

			if (edge.rightVertex.equals(startPoint) || edge.leftVertex.equals(startPoint)) {
				previousLast.toNode = edge.toNode;
				if (!previousLast.connectingEdges.contains(edge, true)) {
					previousLast.connectingEdges.add(edge);
				}

			} else if (edge.leftVertex.equals(endPoint) || edge.rightVertex.equals(endPoint)) {
				if (!end.connectingEdges.contains(edge, true)) {
					end.connectingEdges.add(edge);
				}

			} else if (Intersector.intersectSegmentPlane(edge.leftVertex, edge.rightVertex, crossingPlane, xPoint)
					&& !Float.isNaN(xPoint.x) && !Float.isNaN(xPoint.y) && !Float.isNaN(xPoint.z)) {
				if (i != startIndex || i == 0) {
					lastPoint.toNode = edge.fromNode;
					PathPoint crossing = new PathPoint(xPoint, edge.toNode);
					crossing.connectingEdges.add(edge);
					addPoint(crossing);
				}
			}
		}
		if (endIndex < nodes.size - 1) {
			end.connectingEdges.add((Edge) nodes.get(endIndex));
		}
		if (!lastPoint.equals(end)) {
			addPoint(end);
		}
	}

	private class Funnel {

		public final Plane leftPlane = new Plane();
		public final Plane rightPlane = new Plane();
		public final Vector3 leftPortal = new Vector3();
		public final Vector3 rightPortal = new Vector3();
		public final Vector3 pivot = new Vector3();

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
