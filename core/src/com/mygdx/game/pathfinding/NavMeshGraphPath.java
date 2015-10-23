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

	public static Vector3 up = Vector3.Y;
	public Vector3 start;
	public Vector3 end;
	public Triangle startTriangle;

	private final Plane crossingPlane = new Plane();
	private final Vector3 tmp = new Vector3();

	public Array<PathPoint> debugPathPoints = new Array<PathPoint>();

	public void setStartEnd(Vector3 start, Vector3 end) {
		this.start = new Vector3(start);
		this.end = new Vector3(end);
	}

	public void setStartTriangle(Triangle startTriangle) {
		this.startTriangle = startTriangle;
	}

	public void clear() {
		debugPathPoints.clear();
		nodes.clear();
		start = null;
		end = null;
		startTriangle = null;
	}

	/**
	 * Calculate the shortest path through the path triangles, using the Simple Stupid Funnel Algorithm.
	 *
	 * @return
	 */
	public Array<PathPoint> calculatePathPoints() {
		Array<PathPoint> pathPoints = new Array<PathPoint>();
		if (nodes.size == 0) {
			pathPoints.add(new PathPoint(end, startTriangle));
			pathPoints.add(new PathPoint(start, startTriangle));
			debugPathPoints.addAll(pathPoints);
			return pathPoints;
		}
		nodes.add(new Edge(nodes.get(nodes.size - 1).getToNode(), nodes.get(nodes.size - 1).getToNode(), end, end));
		Edge edge = (Edge) nodes.get(0);
		pathPoints.add(new PathPoint(start, edge.fromNode));

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
					calculateEdgeCrossings(lastRestart, leftIndex, funnel.pivot, funnel.leftPortal, pathPoints);
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
					calculateEdgeCrossings(lastRestart, rightIndex, funnel.pivot, funnel.rightPortal, pathPoints);
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
		calculateEdgeCrossings(lastRestart, nodes.size - 1, funnel.pivot, end, pathPoints);
		nodes.removeIndex(nodes.size - 1);
		pathPoints.reverse();
		debugPathPoints.addAll(pathPoints);
		return pathPoints;
	}

	private void calculateEdgeCrossings(int startIndex, int endIndex,
										Vector3 startPoint, Vector3 endPoint, Array<PathPoint> out) {

		if (startIndex >= nodes.size || endIndex >= nodes.size) {
			return;
		}
		crossingPlane.set(startPoint, tmp.set(startPoint).add(up), endPoint);

		PathPoint previousLast = out.get(out.size - 1);
		Edge edge = (Edge) nodes.get(endIndex);
		PathPoint end = new PathPoint(endPoint, edge.toNode);

		for (int i = startIndex; i < endIndex; i++) {
			edge = (Edge) nodes.get(i);
			Vector3 xPoint = new Vector3();

			if (edge.rightVertex.equals(startPoint) || edge.leftVertex.equals(startPoint)) {
				previousLast.crossingTriangle = edge.toNode;
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
					out.get(out.size - 1).crossingTriangle = edge.fromNode;
					PathPoint crossing = new PathPoint(xPoint, edge.toNode);
					crossing.connectingEdges.add(edge);
					out.add(crossing);
				}
			}
		}
		if (endIndex < nodes.size - 1) {
			end.connectingEdges.add((Edge) nodes.get(endIndex));
		}
		if (!out.get(out.size - 1).point.equals(end.point)) {
			out.add(end);
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
