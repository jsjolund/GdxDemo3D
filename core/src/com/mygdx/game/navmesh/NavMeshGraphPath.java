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
	 * described on page 45 in the linked document.
	 * See <a href="http://repository.tudelft.nl/assets/uuid:f558ade0-a168-42ff-a878-09d1cf1e5eb9/Thesis_Sandy_Brand_2009.pdf">
	 * Brand, Sandy (2009), "Efficient obstacle avoidance using autonomously generated navigation meshes"</a>
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

		Corridor corridor = new Corridor();
		Vector3 pivot = new Vector3(start);
		Edge edge = (Edge) nodes.get(0);
		corridor.setLeftPlane(pivot, edge.leftVertex);
		corridor.setRightPlane(pivot, edge.rightVertex);

		for (int i = 1; i < nodes.size; i++) {
			edge = (Edge) nodes.get(i);
			Plane.PlaneSide leftPlaneLeftDP = corridor.sideLeftPlane(edge.leftVertex);
			Plane.PlaneSide leftPlaneRightDP = corridor.sideLeftPlane(edge.rightVertex);
			Plane.PlaneSide rightPlaneLeftDP = corridor.sideRightPlane(edge.leftVertex);
			Plane.PlaneSide rightPlaneRightDP = corridor.sideRightPlane(edge.rightVertex);

			if (leftPlaneLeftDP == Back && (rightPlaneLeftDP == Front || rightPlaneLeftDP == OnPlane)) {
				path.add(new Vector3(pivot));
				pivot.set(corridor.rightDoorPoint);

				corridor.setLeftPlane(pivot, edge.leftVertex);
				corridor.setRightPlane(pivot, edge.rightVertex);

			} else if (rightPlaneRightDP == Back && (leftPlaneRightDP == Front || leftPlaneRightDP == OnPlane)) {
				path.add(new Vector3(pivot));
				pivot.set(corridor.leftDoorPoint);
				corridor.setLeftPlane(pivot, edge.leftVertex);
				corridor.setRightPlane(pivot, edge.rightVertex);

			} else {
				if (leftPlaneLeftDP == Back || leftPlaneLeftDP == OnPlane) {
					corridor.setLeftPlane(pivot, edge.leftVertex);
				}
				if (rightPlaneRightDP == Back || rightPlaneRightDP == OnPlane) {
					corridor.setRightPlane(pivot, edge.rightVertex);
				}
			}
		}
		path.add(new Vector3(pivot));

		Plane.PlaneSide leftPlaneEndPoint = corridor.sideLeftPlane(end);
		Plane.PlaneSide rightPlaneEndPoint = corridor.sideRightPlane(end);
		if (leftPlaneEndPoint == Back && (rightPlaneEndPoint == Front || rightPlaneEndPoint == OnPlane)) {
			path.add(new Vector3(corridor.rightDoorPoint));
			System.out.println("r");
		} else if (rightPlaneEndPoint == Back && (leftPlaneEndPoint == Front || leftPlaneEndPoint == OnPlane)) {
			path.add(new Vector3(corridor.leftDoorPoint));
			System.out.println("l");
		}
		path.add(new Vector3(end));
		path.reverse();

		return path;
	}

	public void setStartEnd(Vector3 start, Vector3 end) {
		this.start = new Vector3(start);
		this.end = new Vector3(end);
	}

	private class Corridor {
		public final Vector3 up = Vector3.Y;
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
			rightPlane.set(pivot, tmp.set(pivot).add(up), rightEdgeVertex);
			rightPlane.normal.scl(-1);
			rightPlane.d = -rightPlane.d;
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
