/*
 * Copyright 2009, UCAR/Unidata and OPeNDAP, Inc.
 * See the LICENCE file for more information.
 */

package dap4.servlet;

import dap4.core.data.ChecksumMode;
import dap4.core.util.DapConstants;
import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.core.util.ResponseFormat;
import dap4.dap4lib.Dap4Util;
import dap4.dap4lib.DapLog;
import dap4.dap4lib.RequestMode;
import dap4.dap4lib.XURI;
import ucar.httpservices.HTTPUtil;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * User requests get cached here so that downstream code can access
 * the details of the request information.
 * <p>
 * Modified by Heimbigner for DAP4.
 *
 * @author Nathan Potter
 * @author Dennis Heimbigner
 */

public class DapRequest {
  //////////////////////////////////////////////////
  // Constants

  static final boolean DEBUG = false;

  static public final String WEBINFPATH = "/WEB-INF";
  static public final String RESOURCEDIRNAME = "resources";

  static public final String CONSTRAINTTAG = "dap4.ce";

  static public final ChecksumMode DEFAULTCSUM = ChecksumMode.NONE;

  //////////////////////////////////////////////////
  // Instance variables

  protected HttpServletRequest request = null;
  protected HttpServletResponse response = null;

  protected XURI xuri = null; // without any query and as with any modified dataset path

  protected RequestMode mode = null; // .dmr, .dap, or .dsr
  protected ResponseFormat format = null; // e.g. .xml when given .dmr.xml

  protected ByteOrder order = ByteOrder.nativeOrder();
  protected ChecksumMode checksummode = null;

  protected String datasetpath = null;

  //////////////////////////////////////////////////
  // Constructor(s)

  public DapRequest(DapController controller, HttpServletRequest request, HttpServletResponse response)
      throws DapException {
    this.request = request;
    this.response = response;
    try {
      parseURI(); // Pull Info from the URI
    } catch (IOException ioe) {
      throw new DapException(ioe);
    }
  }

  //////////////////////////////////////////////////
  // Request path parsing

  /**
   * The goal of parse() is to extract info
   * from the underlying HttpRequest and cache it
   * in this object.
   * <p>
   * In particular, the incoming URL needs to be decomposed
   * into multiple pieces. Certain assumptions are made:
   * 1. every incoming url is of the form
   * (a) http(s)://host:port/d4ts/
   * or
   * (b) http(s)://host:port/d4ts/<datasetpath>?query
   * Case a indicates that the front page is to be returned.
   * Case b indicates a request for a dataset (or dsr), and its
   * value is determined by its extensions. The query may be absent.
   * We want to extract the following pieces.
   * 1. (In URI parlance) The scheme plus the authority:
   * http://host:port
   * 3. The return type: depending on the last extension (e.g. ".txt").
   * 4. The requested value: depending on the next to last extension (e.g. ".dap").
   * 5. The suffix path specifying the actual dataset: datasetpath
   * with return and request type extensions removed.
   * 6. The url path = servletpath + datasetpath.
   * 7. The query part.
   */

  protected void parseURI() throws IOException {
    try {
      xuri = new XURI(request.getRequestURL().toString());
    } catch (URISyntaxException e) {
      throw new IOException(e);
    }
    this.datasetpath = request.getPathInfo();
    if (this.datasetpath == null) {
      // Eventually make this a capabilities request
      this.mode = RequestMode.CAPABILITIES;
    } else {
      // Decompose path by '.'
      String[] pieces = this.datasetpath.split("[.]");
      // Search backward looking for the mode (dmr or dap)
      // meanwhile capturing the format extension
      int modepos = 0;
      for (int i = pieces.length - 1; i >= 1; i--) {// ignore first piece
        String ext = pieces[i];
        // We assume that the set of response formats does not interset the set of request modes
        RequestMode mode = RequestMode.modeFor(ext);
        ResponseFormat format = ResponseFormat.formatFor(ext);
        if (mode != null) {
          // Stop here
          this.mode = mode;
          modepos = i;
          break;
        } else if (format != null) {
          if (this.format != null)
            throw new DapException("Multiple response formats specified: " + ext)
                .setCode(HttpServletResponse.SC_BAD_REQUEST);
          this.format = format;
        }
      }
      // Set the datasetpath to the entire path before the mode defining extension.
      if (modepos > 0)
        this.datasetpath = DapUtil.join(pieces, ".", 0, modepos);
    }

    if (this.mode == null)
      this.mode = RequestMode.DSR;
    if (this.format == null)
      this.format = ResponseFormat.NONE;

    // For testing purposes, get the desired endianness to use with replies
    String p = queryLookup(Dap4Util.DAP4ENDIANTAG);
    if (p != null) {
      Integer oz = DapUtil.stringToInteger(p);
      if (oz == null)
        this.order = ByteOrder.LITTLE_ENDIAN;
      else
        this.order = (oz != 0 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
    }

    // Ditto for checksum
    p = queryLookup(DapConstants.CHECKSUMTAG);
    if (p != null) {
      this.checksummode = ChecksumMode.modeFor(p);
    }
  }

  //////////////////////////////////////////////////
  // Accessor(s)

  public ByteOrder getOrder() {
    return this.order;
  }
  public ChecksumMode getChecksumMode() {
    return this.checksummode;
  }

  public HttpServletRequest getRequest() {
    return request;
  }

  public HttpServletResponse getResponse() {
    return response;
  }

  public OutputStream getOutputStream() throws IOException {
    return response.getOutputStream();
  }

  public String getURL() {
    return this.xuri.toString();
  }

  public String getOriginalURL() {
    return this.xuri.getOriginal();
  }

  public String getDataset() {
    return this.datasetpath;
  }

  public ServletContext getServletContext() {
    return this.request.getServletContext();
  }

  public RequestMode getMode() {
    return this.mode;
  }

  public ResponseFormat getFormat() {
    return this.format;
  }

  /**
   * Set a request header
   *
   * @param name the header name
   * @param value the header value
   */
  public void setResponseHeader(String name, String value) {
    this.response.setHeader(name, value);
  }

  public String queryLookup(String name) {
    return this.xuri.getQueryFields().get(name.toLowerCase());
  }

  public Map<String, String> getQueries() {
    return this.xuri.getQueryFields();
  }

  public String getResourcePath(String relpath) throws IOException {
    if(relpath.length() == 0 || (relpath.charAt(0) != '/' && relpath.charAt(0) != '\\'))
      relpath = '/' + relpath;
    String servletpath = this.getServletContext().getResource("/").getPath();
    String path = servletpath+"WEB-INF/resources" + relpath;
    path = DapUtil.canonicalpath(path);
    return path;
  }

  public String getDatasetPath() {
    return this.datasetpath;
  }

  static String makeQueryString(HttpServletRequest req) {
    Map<String, String[]> map = req.getParameterMap();
    if (map == null || map.size() == 0)
      return null;
    StringBuilder q = new StringBuilder();
    for (Map.Entry<String, String[]> entry : map.entrySet()) {
      String[] values = entry.getValue();
      if (values == null || values.length == 0) {
        q.append("&");
        q.append(entry.getKey());
      } else
        for (int i = 0; i < values.length; i++) {
          q.append("&");
          q.append(entry.getKey());
          q.append("=");
          q.append(values[i]);
        }
    }
    if (q.length() > 0)
      q.deleteCharAt(0);// leading &
    return q.toString();
  }
}

