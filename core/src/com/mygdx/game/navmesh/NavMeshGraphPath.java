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
	private final static Plane.PlaneSide Front = Plane.PlaneSide.Front;
	private final static Plane.PlaneSide OnPlane = Plane.PlaneSide.OnPlane;

	public Vector3 start;
	public Vector3 end;

	public NavMeshGraphPath(Vector3 start, Vector3 end) {
		this.start = new Vector3(start);
		this.end = new Vector3(end);
	}

	/**
	 * Calculate the shortest path through the navmesh path triangles, using the collapsing corridor algorithm
	 * described in: Brand, Sandy (2009), "Efficient obstacle avoidance using autonomously generated navigation meshes".
	 *
	 * @return
	 */
	public Array<Vector3> getDirectPath() {
		nodes.add(new Edge(null, null, end, end));

		Array<Vector3> path = new Array<Vector3>();
		Vector3 pivot = new Vector3(start);
		Corridor corridor = new Corridor();

		Edge edge = (Edge) nodes.get(0);
		corridor.setLeftPlane(pivot, edge.leftVertex);
		corridor.setRightPlane(pivot, edge.rightVertex);

		for (int i = 1; i < nodes.size; i++) {
			edge = (Edge) nodes.get(i);
			Plane.PlaneSide leftPlaneLDP = corridor.sideLeftPlane(edge.leftVertex);
			Plane.PlaneSide rightPlaneLDP = corridor.sideRightPlane(edge.leftVertex);
			Plane.PlaneSide leftPlaneRDP = corridor.sideLeftPlane(edge.rightVertex);
			Plane.PlaneSide rightPlaneRDP = corridor.sideRightPlane(edge.rightVertex);

			if (leftPlaneLDP == Back && (rightPlaneLDP == Front || rightPlaneLDP == OnPlane)) {
				path.add(new Vector3(pivot));
				pivot.set(corridor.rightDoorPoint);
				corridor.setLeftPlane(pivot, edge.leftVertex);
				corridor.setRightPlane(pivot, edge.rightVertex);

			} else if (rightPlaneRDP == Back && (leftPlaneRDP == Front || leftPlaneRDP == OnPlane)) {
				path.add(new Vector3(pivot));
				pivot.set(corridor.leftDoorPoint);
				corridor.setLeftPlane(pivot, edge.leftVertex);
				corridor.setRightPlane(pivot, edge.rightVertex);

			} else {
				if (leftPlaneLDP == Back || leftPlaneLDP == OnPlane) {
					corridor.setLeftPlane(pivot, edge.leftVertex);
				}
				if (rightPlaneRDP == Back || rightPlaneRDP == OnPlane) {
					corridor.setRightPlane(pivot, edge.rightVertex);
				}
			}
		}
		nodes.removeIndex(nodes.size - 1);
		path.add(pivot);
		path.add(end);
		path.reverse();

		return path;
	}

	public void setStartEnd(Vector3 start, Vector3 end) {
		this.start = new Vector3(start);
		this.end = new Vector3(end);
	}

	private class Corridor {
		public Vector3 up = new Vector3(Vector3.Y);
		public Plane leftPlane = new Plane();
		public Plane rightPlane = new Plane();
		public Vector3 leftDoorPoint = new Vector3();
		public Vector3 rightDoorPoint = new Vector3();

		private Vector3 tmp = new Vector3();

		public void setLeftPlane(Vector3 pivot, Vector3 leftEdgeVertex) {
			leftPlane.set(pivot, tmp.set(pivot).add(up), leftEdgeVertex);
			leftDoorPoint.set(leftEdgeVertex);
		}

		public void setRightPlane(Vector3 pivot, Vector3 rightEdgeVertex) {
			rightPlane.set(rightEdgeVertex, tmp.set(pivot).add(up), pivot);
			rightDoorPoint.set(rightEdgeVertex);
		}

		public Plane.PlaneSide sideLeftPlane(Vector3 point) {
			return leftPlane.testPoint(point);
		}

		public Plane.PlaneSide sideRightPlane(Vector3 point) {
			return rightPlane.testPoint(point);
		}
	}

}
