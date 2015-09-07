/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ft2.coverage.writer;

import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.*;
import ucar.nc2.constants.*;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.geoloc.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Write CF Compliant Grid file from a Coverage.
 * First, single coverage only.
 * - The idea is to subset the coordsys, use that for the file's metadata.
 * - Then subset the grid, and write out the data. chack that the grid's metadata matches.
 *
 * @author caron
 * @since 5/8/2015
 */
public class CFGridCoverageWriter2 {

  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CFGridCoverageWriter2.class);
  static private final boolean show = true;

  static private final String BOUNDS = "_bounds";
  static private final String BOUNDS_DIM = "bounds_dim"; // dimension of length 2, can be used by any bounds coordinate

  /**
   * Write a netcdf/CF file from a CoverageDataset

   * @param gdsOrg       the CoverageDataset
   * @param gridNames    the list of coverage names to be written, or null for all
   * @param subset       defines the requested subset
   * @param addLatLon    add 2D lat/lon coordinates if needed
   * @param writer       this does the actual writing
   * @return total bytes written
   * @throws IOException
   * @throws InvalidRangeException
   */
  public static long writeFile(CoverageDataset gdsOrg, List<String> gridNames,
                               SubsetParams subset,
                               boolean addLatLon,
                               NetcdfFileWriter writer) throws IOException, InvalidRangeException {

    CFGridCoverageWriter2 writer2 = new CFGridCoverageWriter2();
    return writer2.writeOrTestSize(gdsOrg, gridNames, subset, addLatLon, false, writer);
  }


  /**
   * @param gdsOrg       the CoverageDataset
   * @param gridNames    the list of coverage names to be written, or null for all
   * @param subsetParams       the desired subset
   * @param addLatLon    add 2D lat/lon coordinates if needed
   * @param testSizeOnly dont write, just return size
   * @param writer       this does the actual writing
   * @return total bytes written
   * @throws IOException
   * @throws InvalidRangeException
   */
  private long writeOrTestSize(CoverageDataset gdsOrg, List<String> gridNames, SubsetParams subsetParams, boolean addLatLon, boolean testSizeOnly,
                               NetcdfFileWriter writer) throws IOException, InvalidRangeException {

    // we need global atts, subsetted axes, the transforms, and the coverages with attributes and referencing subsetted axes
    CoverageDataset subsetDataset = new CoverageSubsetter2().makeCoverageDatasetSubset(gdsOrg, gridNames, subsetParams);

    long total_size = 0;
    for (Coverage grid : subsetDataset.getCoverages()) {
      total_size += grid.getSizeInBytes();
    }

    if (testSizeOnly)
      return total_size;

    ////////////////////////////////////////////////////////////////////

    // check size is ok
    boolean isLargeFile = isLargeFile(total_size);
    writer.setLargeFile(isLargeFile);

    addGlobalAttributes(subsetDataset, writer);

    // each independent coordinate is a dimension
    Map<String, Dimension> dimHash = new HashMap<>();
    for (CoverageCoordAxis axis : subsetDataset.getCoordAxes()) {
      if (axis.getDependenceType() == CoverageCoordAxis.DependenceType.independent) {
        Dimension d = writer.addDimension(null, axis.getName(), axis.getNcoords());
        dimHash.put(axis.getName(), d);
      }

      if (axis.isInterval()) {
        if (null == dimHash.get(BOUNDS_DIM)) {
          Dimension d = writer.addDimension(null, BOUNDS_DIM, 2);
          dimHash.put(BOUNDS_DIM, d);
        }
      }
    }

    // add coordinates
    for (CoverageCoordAxis axis : subsetDataset.getCoordAxes()) {
      String dims;

      if (axis.getDependenceType() == CoverageCoordAxis.DependenceType.independent) {
        dims = axis.getName();

      } else if (axis.getDependenceType() == CoverageCoordAxis.DependenceType.scalar) {
        dims = "";

      } else {
        dims = axis.getDependsOn();
      }

      boolean hasBounds = false;
      if (axis.isInterval()) {
        Variable vb = writer.addVariable(null, axis.getName()+BOUNDS, axis.getDataType(), dims+" "+BOUNDS_DIM);
        vb.addAttribute(new Attribute(CDM.UNITS, axis.getUnits()));
        hasBounds = true;
      }

      Variable v = writer.addVariable(null, axis.getName(), axis.getDataType(), dims);
      addVariableAttributes(v, axis.getAttributes());
      if (hasBounds)
        v.addAttribute(new Attribute(CF.BOUNDS, axis.getName()+BOUNDS));

    }

    // add grids
    for (Coverage grid : subsetDataset.getCoverages()) {
      Variable v = writer.addVariable(null, grid.getName(), grid.getDataType(), grid.getIndependentAxisNamesOrdered());
      addVariableAttributes(v, grid.getAttributes());
    }

    // coordTransforms
    for (CoverageTransform ct : subsetDataset.getCoordTransforms()) {
      Variable ctv = writer.addVariable(null, ct.getName(), DataType.INT, ""); // scalar coordinate transform variable - container for transform info
      for (Attribute att : ct.getAttributes())
        ctv.addAttribute(att);
    }

    addCFAnnotations(subsetDataset, writer, addLatLon);

    // finish define mode
    writer.create();

    // write the coordinate data
    for (CoverageCoordAxis axis : subsetDataset.getCoordAxes()) {
      Variable v = writer.findVariable(axis.getName());
      if (v != null) {
        if (show) System.out.printf("CFGridCoverageWriter write axis %s%n", v.getNameAndDimensions());
        writer.write(v, axis.getCoordsAsArray());
      } else {
        logger.error("CFGridCoverageWriter No variable for %s%n", axis.getName());
      }

      if (axis.isInterval()) {
        Variable vb = writer.findVariable(axis.getName() + BOUNDS);
        writer.write(vb, axis.getCoordBoundsAsArray());
      }
    }

    // write the grid data
    for (Coverage grid : subsetDataset.getCoverages()) {
      // LOOK - we need to call readData on the original
      Coverage gridOrg = gdsOrg.findCoverage(grid.getName());
      GeoReferencedArray array = gridOrg.readData(subsetParams);  // LOOK must conform to whatever axis.getCoordsAsArray() returns

      Variable v = writer.findVariable(grid.getName());
      if (show) System.out.printf("CFGridCoverageWriter write grid %s%n", v.getNameAndDimensions());
      writer.write(v, array.getData());
    }

    //updateGeospatialRanges(writer, llrect );
    writer.close();

    // this writes the data to the new file.
    return total_size; // ok
  }

  private boolean isLargeFile(long total_size) {
    boolean isLargeFile = false;
    long maxSize = Integer.MAX_VALUE;
    if (total_size > maxSize) {
      logger.debug("Request size = {} Mbytes", total_size / 1000 / 1000);
      isLargeFile = true;
    }
    return isLargeFile;
  }

  private void addGlobalAttributes(CoverageDataset gds, NetcdfFileWriter writer) {
    // global attributes
    for (Attribute att : gds.getGlobalAttributes()) {
      if (att.getShortName().equals(CDM.FILE_FORMAT)) continue;
      if (att.getShortName().equals(_Coordinate._CoordSysBuilder)) continue;
      writer.addGroupAttribute(null, att);
    }

    Attribute att = gds.findAttributeIgnoreCase(CDM.CONVENTIONS);
    if (att == null || !att.getStringValue().startsWith("CF-"))  // preserve previous version of CF Convention if it exists
      writer.addGroupAttribute(null, new Attribute(CDM.CONVENTIONS, "CF-1.0"));

    writer.addGroupAttribute(null, new Attribute("History",
            "Translated to CF-1.0 Conventions by Netcdf-Java CDM (CFGridCoverageWriter)\n" +
                    "Original Dataset = " + gds.getName() + "; Translation Date = " + CalendarDate.present()));


    LatLonRect llbb = gds.getLatLonBoundingBox();
    if (llbb != null) {
      // this will replace any existing
      writer.addGroupAttribute(null, new Attribute(ACDD.LAT_MIN, llbb.getLatMin()));
      writer.addGroupAttribute(null, new Attribute(ACDD.LAT_MAX, llbb.getLatMax()));
      writer.addGroupAttribute(null, new Attribute(ACDD.LON_MIN, llbb.getLonMin()));
      writer.addGroupAttribute(null, new Attribute(ACDD.LON_MAX, llbb.getLonMax()));
    }
  }

  private void addVariableAttributes(Variable v, List<Attribute> atts) {
    // global attributes
    for (Attribute att : atts) {
      if (att.getShortName().startsWith("_Coordinate")) continue;
      v.addAttribute(att);
    }
  }

  private void addCFAnnotations(CoverageDataset gds, NetcdfFileWriter writer, boolean addLatLon) {

    for (Coverage grid : gds.getCoverages()) {
      CoverageCoordSys gcs = grid.getCoordSys();

      Variable newV = writer.findVariable(grid.getName());
      if (newV == null) {
        logger.error("CFGridCoverageWriter2 cant find " + grid.getName() + " in writer ");
        continue;
      }

      // annotate Variable for CF
      StringBuilder sbuff = new StringBuilder();
      sbuff.append(grid.getCoordSys().getAxisNames());
      if (addLatLon) sbuff.append("lat lon");
      newV.addAttribute(new Attribute(CF.COORDINATES, sbuff.toString()));

      // add reference to coordinate transform variables
      CoverageTransform ct = gcs.getHorizTransform();
      if (ct != null && ct.isHoriz())
        newV.addAttribute(new Attribute(CF.GRID_MAPPING, ct.getName()));

      // LOOK what about vertical ?
    }

    for (CoverageCoordAxis axis : gds.getCoordAxes()) {
      Variable newV = writer.findVariable(axis.getName());
      if (newV == null) {
        logger.error("CFGridCoverageWriter2 cant find " + axis.getName() + " in writer ");
        continue;
      }
      /* if ((axis.getAxisType() == AxisType.Height) || (axis.getAxisType() == AxisType.Pressure) || (axis.getAxisType() == AxisType.GeoZ)) {
        if (null != axis.getPositive())
          newV.addAttribute(new Attribute(CF.POSITIVE, axis.getPositive()));
      } */
      if (axis.getAxisType() == AxisType.Lat) {
        newV.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));
        newV.addAttribute(new Attribute(CF.STANDARD_NAME, CF.LATITUDE));
      }
      if (axis.getAxisType() == AxisType.Lon) {
        newV.addAttribute(new Attribute(CDM.UNITS, CDM.LON_UNITS));
        newV.addAttribute(new Attribute(CF.STANDARD_NAME, CF.LONGITUDE));
      }
      if (axis.getAxisType() == AxisType.GeoX) {
        newV.addAttribute(new Attribute(CF.STANDARD_NAME, CF.PROJECTION_X_COORDINATE));
      }
      if (axis.getAxisType() == AxisType.GeoY) {
        newV.addAttribute(new Attribute(CF.STANDARD_NAME, CF.PROJECTION_Y_COORDINATE));
      }
      if (axis.getAxisType() == AxisType.Ensemble) {
        newV.addAttribute(new Attribute(CF.STANDARD_NAME, CF.ENSEMBLE));
      }
    }
  }
}

