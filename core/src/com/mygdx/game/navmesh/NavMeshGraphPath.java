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
		nodes.add(new Edge(null, null, end, end));

		Array<Vector3> path = new Array<Vector3>();

		Corridor corridor = new Corridor();
		Vector3 pivot = new Vector3(start);
		Edge edge = (Edge) nodes.get(0);
		corridor.setLeftPlane(pivot, edge.leftVertex);
		corridor.setRightPlane(pivot, edge.rightVertex);

		for (int i = 1; i < nodes.size; i++) {
			String out = i+": ";
			edge = (Edge) nodes.get(i);
			Plane.PlaneSide leftPlaneLeftDP = corridor.sideLeftPlane(edge.leftVertex);
			Plane.PlaneSide leftPlaneRightDP = corridor.sideLeftPlane(edge.rightVertex);
			Plane.PlaneSide rightPlaneLeftDP = corridor.sideRightPlane(edge.leftVertex);
			Plane.PlaneSide rightPlaneRightDP = corridor.sideRightPlane(edge.rightVertex);
			String state = String.format("%s %s %s %s", leftPlaneLeftDP, leftPlaneRightDP, rightPlaneLeftDP, rightPlaneRightDP);

			if (leftPlaneLeftDP == Back && (rightPlaneLeftDP == Front || rightPlaneLeftDP == OnPlane)) {
				out += "collapse right";
				path.add(new Vector3(pivot));
				pivot.set(corridor.rightDoorPoint);
				corridor.setLeftPlane(pivot, edge.leftVertex);
				corridor.setRightPlane(pivot, edge.rightVertex);

			} else if (rightPlaneRightDP == Back && (leftPlaneRightDP == Front || leftPlaneRightDP == OnPlane)) {
				out += "collapse left";
				path.add(new Vector3(pivot));
				pivot.set(corridor.leftDoorPoint);
				corridor.setLeftPlane(pivot, edge.leftVertex);
				corridor.setRightPlane(pivot, edge.rightVertex);

			} else {
				if (leftPlaneLeftDP == Back || leftPlaneLeftDP == OnPlane) {
					out += "set left, ";
					corridor.setLeftPlane(pivot, edge.leftVertex);
				}
				if (rightPlaneRightDP == Back || rightPlaneRightDP == OnPlane) {
					out += "set right, ";
					corridor.setRightPlane(pivot, edge.rightVertex);
				}
			}
			System.out.println(out + ": "+ state);
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
//			rightPlane.set(pivot, tmp.setZero().sub(pivot).sub(up), rightEdgeVertex);
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
