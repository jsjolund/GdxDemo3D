package com.mygdx.game.navmesh;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/**
 * Created by Johannes Sjolund on 9/26/15.
 */
public class NavMeshGraphPath extends DefaultGraphPath<Connection<Triangle>> {

	private final static Plane.PlaneSide Back = Plane.PlaneSide.Back;
	private final static Plane.PlaneSide OnPlane = Plane.PlaneSide.OnPlane;

	public Vector3 start;
	public Vector3 end;

	public NavMeshGraphPath(Vector3 start, Vector3 end) {
		this.start = new Vector3(start);
		this.end = new Vector3(end);
	}

	/**
	 * Calculate the shortest path through the navmesh path triangles, using the Simple Stupid Funnel Algorithm.
	 *
	 * @return
	 */
	public Array<Vector3> getDirectPath() {
		Array<Vector3> path = new Array<Vector3>();
		if (nodes.size <= 1) {
			path.add(new Vector3(end));
			path.add(new Vector3(start));
			return path;
		}
		nodes.add(new Edge(null, null, end, end));
		Funnel funnel = new Funnel();
		funnel.pivot.set(start);
		Edge edge = (Edge) nodes.get(0);
		funnel.setPlanes(funnel.pivot, edge);
		int leftIndex = 0;
		int rightIndex = 0;

		for (int i = 1; i < nodes.size; ++i) {
			edge = (Edge) nodes.get(i);

			Plane.PlaneSide leftPlaneLeftDP = funnel.sideLeftPlane(edge.leftVertex);
			Plane.PlaneSide leftPlaneRightDP = funnel.sideLeftPlane(edge.rightVertex);
			Plane.PlaneSide rightPlaneLeftDP = funnel.sideRightPlane(edge.leftVertex);
			Plane.PlaneSide rightPlaneRightDP = funnel.sideRightPlane(edge.rightVertex);

			if (rightPlaneRightDP == Back || rightPlaneRightDP == OnPlane) {
				if (leftPlaneRightDP == Back || leftPlaneRightDP == OnPlane) {
					// Tighten the funnel.
					funnel.setRightPlane(funnel.pivot, edge.rightVertex);
					rightIndex = i;
				} else {
					// Right over left, insert left to path and restart scan from portal left point.
					path.add(new Vector3(funnel.pivot));
					funnel.pivot.set(funnel.leftDoorPoint);
					rightIndex = leftIndex;
					i = leftIndex;
					if (i < nodes.size - 1) {
						funnel.setPlanes(funnel.pivot, (Edge) nodes.get(i + 1));
						continue;
					}
					break;
				}
			}
			if (leftPlaneLeftDP == Back || leftPlaneLeftDP == OnPlane) {
				if (rightPlaneLeftDP == Back || rightPlaneLeftDP == OnPlane) {
					// Tighten the funnel.
					funnel.setLeftPlane(funnel.pivot, edge.leftVertex);
					leftIndex = i;
				} else {
					// Left over right, insert right to path and restart scan from portal right point.
					path.add(new Vector3(funnel.pivot));
					funnel.pivot.set(funnel.rightDoorPoint);
					leftIndex = rightIndex;
					i = rightIndex;
					if (i < nodes.size - 1) {
						funnel.setPlanes(funnel.pivot, (Edge) nodes.get(i + 1));
						continue;
					}
					break;
				}
			}
		}
		path.add(new Vector3(funnel.pivot));
		path.add(new Vector3(end));
		path.reverse();
		nodes.removeIndex(nodes.size - 1);
		return path;
	}

	private class Funnel {
		private final Vector3 up = Vector3.Y;
		public final Plane leftPlane = new Plane();
		public final Plane rightPlane = new Plane();
		public final Vector3 leftDoorPoint = new Vector3();
		public final Vector3 rightDoorPoint = new Vector3();
		public final Vector3 pivot = new Vector3();

		private Vector3 tmp = new Vector3();

		public void setLeftPlane(Vector3 pivot, Vector3 leftEdgeVertex) {
			leftPlane.set(pivot, tmp.set(pivot).add(up), leftEdgeVertex);
			leftDoorPoint.set(leftEdgeVertex);
		}

		public void setRightPlane(Vector3 pivot, Vector3 rightEdgeVertex) {
			rightPlane.set(pivot, tmp.set(pivot).add(up), rightEdgeVertex);
			rightPlane.normal.scl(-1);
			rightPlane.d = -rightPlane.d;
			rightDoorPoint.set(rightEdgeVertex);
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
