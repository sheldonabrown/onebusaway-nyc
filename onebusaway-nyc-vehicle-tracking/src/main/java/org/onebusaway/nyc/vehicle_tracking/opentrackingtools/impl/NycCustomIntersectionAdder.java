package org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.algorithm.LineIntersector;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.noding.NodedSegmentString;
import com.vividsolutions.jts.noding.SegmentIntersector;
import com.vividsolutions.jts.noding.SegmentString;
import com.vividsolutions.jts.noding.SegmentStringDissolver.SegmentStringMerger;

import java.util.List;

/**
 * Taken from JTS's IntersectionAdder. Modified to only add proper (interior)
 * intersections.
 * 
 * @version 1.7
 */
public class NycCustomIntersectionAdder implements SegmentIntersector {

  public static final class NycCustomSegmentStringMerger implements
      SegmentStringMerger {
    @Override
    public void merge(SegmentString mergeTarget, SegmentString ssToMerge,
        boolean isSameOrientation) {

      final List<NycTrackingGraph.SegmentInfo> newSegmentInfos = Lists.newArrayList();

      for (final NycTrackingGraph.SegmentInfo si : (List<NycTrackingGraph.SegmentInfo>) ssToMerge.getData()) {
        newSegmentInfos.add(new NycTrackingGraph.SegmentInfo(si.getShapeId(),
            si.getGeomNum(), si.getIsSameOrientation().equals(
                new Boolean(isSameOrientation))));
      }
      newSegmentInfos.addAll((List<NycTrackingGraph.SegmentInfo>) mergeTarget.getData());
      mergeTarget.setData(newSegmentInfos);
    }
  }

  public static boolean isAdjacentSegments(int i1, int i2) {
    return Math.abs(i1 - i2) == 1;
  }

  /**
   * These variables keep track of what types of intersections were found during
   * ALL edges that have been intersected.
   */
  private boolean hasIntersection = false;
  private boolean hasProper = false;
  private boolean hasProperInterior = false;
  private boolean hasInterior = false;

  // the proper intersection point found
  private final Coordinate properIntersectionPoint = null;

  private final LineIntersector li;
  public int numIntersections = 0;
  public int numInteriorIntersections = 0;
  public int numProperIntersections = 0;

  public NycCustomIntersectionAdder(LineIntersector li) {
    this.li = li;
  }

  public LineIntersector getLineIntersector() {
    return li;
  }

  /**
   * @return the proper intersection point, or <code>null</code> if none was
   *         found
   */
  public Coordinate getProperIntersectionPoint() {
    return properIntersectionPoint;
  }

  public boolean hasIntersection() {
    return hasIntersection;
  }

  /**
   * A proper intersection is an intersection which is interior to at least two
   * line segments. Note that a proper intersection is not necessarily in the
   * interior of the entire Geometry, since another edge may have an endpoint
   * equal to the intersection, which according to SFS semantics can result in
   * the point being on the Boundary of the Geometry.
   */
  public boolean hasProperIntersection() {
    return hasProper;
  }

  /**
   * A proper interior intersection is a proper intersection which is <b>not</b>
   * contained in the set of boundary nodes set for this SegmentIntersector.
   */
  public boolean hasProperInteriorIntersection() {
    return hasProperInterior;
  }

  /**
   * An interior intersection is an intersection which is in the interior of
   * some segment.
   */
  public boolean hasInteriorIntersection() {
    return hasInterior;
  }

  /**
   * A trivial intersection is an apparent self-intersection which in fact is
   * simply the point shared by adjacent line segments. Note that closed edges
   * require a special check for the point shared by the beginning and end
   * segments.
   */
  private boolean isTrivialIntersection(SegmentString e0, int segIndex0,
      SegmentString e1, int segIndex1) {
    if (e0 == e1) {
      if (li.getIntersectionNum() == 1) {
        if (isAdjacentSegments(segIndex0, segIndex1))
          return true;
        if (e0.isClosed()) {
          final int maxSegIndex = e0.size() - 1;
          if ((segIndex0 == 0 && segIndex1 == maxSegIndex)
              || (segIndex1 == 0 && segIndex0 == maxSegIndex)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * This method is called by clients of the {@link SegmentIntersector} class to
   * process intersections for two segments of the {@link SegmentString}s being
   * intersected. Note that some clients (such as {@link MonotoneChain}s) may
   * optimize away this call for segment pairs which they have determined do not
   * intersect (e.g. by an disjoint envelope test).
   */
  @Override
  public void processIntersections(SegmentString e0, int segIndex0,
      SegmentString e1, int segIndex1) {
    if (e0 == e1 && segIndex0 == segIndex1)
      return;
    final Coordinate p00 = e0.getCoordinates()[segIndex0];
    final Coordinate p01 = e0.getCoordinates()[segIndex0 + 1];
    final Coordinate p10 = e1.getCoordinates()[segIndex1];
    final Coordinate p11 = e1.getCoordinates()[segIndex1 + 1];

    li.computeIntersection(p00, p01, p10, p11);
    if (li.hasIntersection()) {
      numIntersections++;
      if (li.isInteriorIntersection()) {
        numInteriorIntersections++;
        hasInterior = true;
      }
      // if the segments are adjacent they have at least one trivial
      // intersection,
      // the shared endpoint. Don't bother adding it if it is the
      // only intersection.
      if (!isTrivialIntersection(e0, segIndex0, e1, segIndex1)) {
        hasIntersection = true;

        final double sAngle1 = Angle.angle(p00, p01);
        final double sAngle2 = Angle.angle(p10, p11);
        if (li.isInteriorIntersection() && Angle.diff(sAngle1, sAngle2) < 1e-4) {
          /*
           * the segments aren't equal, but overlap, and are in the same
           * direction.
           */
          ((NodedSegmentString) e0).addIntersections(li, segIndex0, 0);
          ((NodedSegmentString) e1).addIntersections(li, segIndex1, 1);
        } else if (segIndex0 > 0 && segIndex1 > 0
            && (p00.equals(p10) && !p01.equals(p11))) {
          /*
           * if the start points are the same but the ends are not, we check for
           * a split along the same prior path. we confirm the same prior path
           * by matching angles of the segments leading to the split point.
           */
          final Coordinate p00b = e0.getCoordinates()[segIndex0 - 1];
          final Coordinate p10b = e1.getCoordinates()[segIndex1 - 1];
          final double angle1 = Angle.angle(p00b, p00);
          final double angle2 = Angle.angle(p10b, p10);

          if (Angle.diff(angle1, angle2) < 1e-4) {
            ((NodedSegmentString) e0).addIntersections(li, segIndex0, 0);
            ((NodedSegmentString) e1).addIntersections(li, segIndex1, 1);
          }
        }

        if (li.isProper()) {
          numProperIntersections++;
          hasProper = true;
          hasProperInterior = true;
        }
      }
    }
  }

  /**
   * Always process all intersections
   * 
   * @return false always
   */
  @Override
  public boolean isDone() {
    return false;
  }
}
