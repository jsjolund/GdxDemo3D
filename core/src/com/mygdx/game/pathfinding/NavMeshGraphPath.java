package com.mygdx.game.pathfinding;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/**
 * Created by Johannes Sjolund on 9/26/15.
 */
public class NavMeshGraphPath extends DefaultGraphPath<Connection<Triangle>> {

	public Vector3 start;
	public Vector3 end;

	public void setStartEnd(Vector3 start, Vector3 end) {
		this.start = new Vector3(start);
		this.end = new Vector3(end);
	}

	public void clear() {
		nodes.clear();
		start = null;
		end = null;
	}

	/**
	 * Calculate the shortest path through the path triangles with {@link #getDirectPath() getDirectPath()},
	 * then divide up long path lines into smaller.
	 * <p/>
	 * TODO: The path should store which triangle each waypoint belongs to
	 *
	 * @return
	 */
	public Array<Vector3> getSmoothPath() {
//		return getDirectPath();
		Array<Vector3> directPath = getDirectPath();
		Array<Vector3> smoothPath = new Array<Vector3>();

		float pathPointMaxDst = 1.2f;
		Vector3 q = directPath.get(0);
		Vector3 p;
		for (int i = 1; i < directPath.size; i++) {
			p = directPath.get(i);
			float dst = p.dst(q);
			smoothPath.add(q);
			if (dst > pathPointMaxDst) {
				int divisions = (int) (dst / pathPointMaxDst);
				Vector3 dirVec = new Vector3(p).sub(q).nor();
				for (int j = 1; j < divisions; j++) {
					Vector3 r = new Vector3(dirVec).scl(pathPointMaxDst * j).add(q);
					smoothPath.add(r);
				}
			}
			q = p;
		}
		smoothPath.add(q);
		return smoothPath;
	}

	/**
	 * Calculate the shortest path through the path triangles, using the Simple Stupid Funnel Algorithm.
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

			if (rightPlaneRightDP != Plane.PlaneSide.Front) {
				if (leftPlaneRightDP != Plane.PlaneSide.Front) {
					// Tighten the funnel.
					funnel.setRightPlane(funnel.pivot, edge.rightVertex);
					rightIndex = i;
				} else {
					// Right over left, insert left to path and restart scan from portal left point.
					path.add(new Vector3(funnel.pivot));
					funnel.pivot.set(funnel.leftPortal);
					rightIndex = leftIndex;
					i = leftIndex;
					if (i < nodes.size - 1) {
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
					path.add(new Vector3(funnel.pivot));
					funnel.pivot.set(funnel.rightPortal);
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
		public final Plane leftPlane = new Plane();
		public final Plane rightPlane = new Plane();
		public final Vector3 leftPortal = new Vector3();
		public final Vector3 rightPortal = new Vector3();
		public final Vector3 pivot = new Vector3();
		private final Vector3 up = Vector3.Y;
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
