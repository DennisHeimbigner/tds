/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */


package dap4.servlet;

import dap4.core.ce.CEConstraint;
import dap4.core.util.ChecksumMode;
import dap4.core.dmr.*;
import dap4.core.util.*;
import dap4.dap4lib.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.MalformedURLException;
import java.nio.ByteOrder;
import java.util.Map;

abstract public class DapController extends HttpServlet {

  //////////////////////////////////////////////////
  // Constants

  static public boolean DEBUG = false;
  static public boolean DUMPDMR = false;
  static public boolean DUMPDATA = false;

  static public boolean PARSEDEBUG = false;

  static protected final String BIG_ENDIAN = "Big-Endian";
  static protected final String LITTLE_ENDIAN = "Little-Endian";

  // Is this machine big endian?
  static protected boolean IS_BIG_ENDIAN = (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN);

  static protected final String DMREXT = ".dmr";
  static protected final String DATAEXT = ".dap";
  static protected final String DSREXT = ".dsr";
  static protected final String[] ENDINGS = {DMREXT, DATAEXT, DSREXT};

  static protected final String FAVICON = "favicon.ico"; // relative to resource dir

  static public final long DEFAULTBINARYWRITELIMIT = 100 * 1000000; // in bytes

  //////////////////////////////////////////////////
  // static variables

  // We need a singleton instance of a DapCache in order
  // To avoid re-opening the same NetcdfFile instance.
  // Assume:
  // 1. This is subclassed only once and that class will fill in
  // this DapCache instance.

  static protected DapCache cache = null;

  //////////////////////////////////////////////////
  // Static accessors

  static protected void setCache(DapCache cache) {
    DapController.cache = cache;
  }

  static protected DapCache getCache() {
    return DapController.cache;
  }

  //////////////////////////////////////////////////
  // Instance variables

  protected boolean initialized = false; // Was initialize() called?

  // Cache the value of getWebDontentRoot()
  protected String webContentRoot = null;

  // Cache the value of getTestDataRoot()
  protected String testDataRoot = null;

  //////////////////////////////////////////////////
  // Constructor(s)

  public DapController() {
    ChunkWriter.DUMPDATA = DUMPDATA; // pass it on
  }

  //////////////////////////////////////////////////////////
  // Abstract methods

  /**
   * Process a favicon request.
   *
   * @param icopath The path to the icon
   * @param cxt The dap context
   */

  abstract protected void doFavicon(String icopath, DapContext cxt) throws IOException;

  /**
   * Process a capabilities request.
   * Currently, does nothing (but see D4TSServlet.doCapabilities).
   *
   * @param cxt The dapontext
   */

  abstract protected void doCapabilities(DapRequest drq, DapContext cxt) throws IOException;

  /**
   * Get the maximum # of bytes per request
   *
   * @return size
   */
  abstract public long getBinaryWriteLimit();

  /**
   * Get the servlet name (with no leading or trailing slashes)
   *
   * @return name
   */
  abstract public String getServletID();

  /**
   * The DAP4 code requires access to two absolute paths.
   * 1. The path to a directory containing web content:
   * - .template files
   * - .ico files
   * 2. The path to the directory containing the test data
   */

  /**
   * Get the absolute address of the web-content directory
   *
   * @param drq dap request
   * @return the web content directory absolute path
   * @throws IOException
   */
  abstract protected String getWebContentRoot(DapRequest drq) throws DapException;

  /**
   * Convert a URL path for a dataset into an absolute file path
   *
   * @param drq dap request
   * @param location suffix of url path
   * @return path in a string builder so caller can extend.
   * @throws IOException
   */
  abstract protected String getResourcePath(DapRequest drq, String location) throws DapException;

  //////////////////////////////////////////////////////////

  public void init() {
    org.slf4j.Logger logServerStartup = org.slf4j.LoggerFactory.getLogger("serverStartup");
    logServerStartup.info(getClass().getName() + " initialization start");
    initialize();
  }

  /**
   * Initialize servlet/controller
   */
  public void initialize() {
    if (this.initialized)
      return;
    this.initialized = true;
  }


  //////////////////////////////////////////////////////////
  // Primary Controller Entry Point

  public void handleRequest(HttpServletRequest req, HttpServletResponse res) throws IOException {
    DapLog.debug("doGet(): User-Agent = " + req.getHeader("User-Agent"));
    if (!this.initialized)
      initialize();
    DapRequest daprequest = getRequestState(req, res);
    DapContext dapcxt = new DapContext();
    // Add entries to the context
    dapcxt.put(HttpServletRequest.class, req);
    dapcxt.put(HttpServletResponse.class, res);
    dapcxt.put(DapRequest.class, daprequest);

    ByteOrder order = daprequest.getOrder();
    ChecksumMode checksummode = daprequest.getChecksumMode();
    dapcxt.put(DapConstants.DAP4ENDIANTAG, order);
    dapcxt.put(DapConstants.CHECKSUMTAG, checksummode);
    // Transfer all other queries
    Map<String, String> queries = daprequest.getQueries();
    for (Map.Entry<String, String> entry : queries.entrySet()) {
      if (dapcxt.get(entry.getKey()) == null) {
        dapcxt.put(entry.getKey(), entry.getValue());
      }
    }

    if (this.webContentRoot == null) {
      this.webContentRoot = getWebContentRoot(daprequest);
      this.webContentRoot = DapUtil.canonicalpath(this.webContentRoot);
    }

    String url = daprequest.getURL();
    if (url.endsWith(FAVICON)) {
      doFavicon(FAVICON, dapcxt);
      return;
    }
    String datasetpath = daprequest.getDatasetPath();
    datasetpath = DapUtil.nullify(DapUtil.canonicalpath(datasetpath));
    try {
      if (datasetpath == null) {
        // This is the case where a request was made without a dataset;
        // According to the spec, I think we should return the
        // services/capabilities document
        doCapabilities(daprequest, dapcxt);
      } else {
        RequestMode mode = daprequest.getMode();
        if (mode == null)
          throw new DapException("Unrecognized request extension").setCode(HttpServletResponse.SC_BAD_REQUEST);
        switch (mode) {
          case DMR:
            doDMR(daprequest, dapcxt);
            break;
          case DAP:
            doData(daprequest, dapcxt);
            break;
          case DSR:
            doDSR(daprequest, dapcxt);
            break;
          default:
            throw new DapException("Unrecognized request extension").setCode(HttpServletResponse.SC_BAD_REQUEST);
        }
      }
    } catch (Throwable t) {
      t.printStackTrace();
      int code = HttpServletResponse.SC_BAD_REQUEST;
      if (t instanceof DapException) {
        DapException e = (DapException) t;
        code = e.getCode();
        if (code <= 0)
          code = DapCodes.SC_BAD_REQUEST;
        e.setCode(code);
      } else if (t instanceof FileNotFoundException)
        code = DapCodes.SC_NOT_FOUND;
      else if (t instanceof UnsupportedOperationException)
        code = DapCodes.SC_FORBIDDEN;
      else if (t instanceof MalformedURLException)
        code = DapCodes.SC_NOT_FOUND;
      else if (t instanceof IOException)
        code = DapCodes.SC_BAD_REQUEST;
      else
        code = DapCodes.SC_INTERNAL_SERVER_ERROR;
      senderror(daprequest, code, t);
    } // catch
  }

  //////////////////////////////////////////////////////////
  // Extension processors

  /**
   * Process a DSR request.
   * * @param cxt The dap context
   */

  protected void doDSR(DapRequest drq, DapContext cxt) throws IOException {
    try {
      DapDSR dsrbuilder = new DapDSR(drq, cxt);
      String dsr = dsrbuilder.generate(drq.getFormat(), drq.getDatasetPath(), drq.getDataset());
      OutputStream out = drq.getOutputStream();
      addCommonHeaders(drq);// Add relevant headers
      // Wrap the outputstream with a Chunk writer
      ByteOrder order = (ByteOrder) cxt.get(DapConstants.DAP4ENDIANTAG);
      ChunkWriter cw = new ChunkWriter(out, RequestMode.DSR, order);
      cw.writeDSR(dsr);
      cw.close();
    } catch (IOException ioe) {
      throw new DapException("DSR generation error", ioe).setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Process a DMR request.
   *
   * @param cxt The dap context
   */

  protected void doDMR(DapRequest drq, DapContext cxt) throws IOException {
    // Convert the url to an absolute path
    String realpath = drq.getResourcePath(drq.getDatasetPath());

    CDMWrap c4 = DapCache.open(realpath, cxt);
    DapDataset dmr = c4.getDMR();

    /* Annotate with our endianness */
    ByteOrder order = (ByteOrder) cxt.get(DapConstants.DAP4ENDIANTAG);
    setEndianness(dmr, order);

    // Process any constraint view
    CEConstraint ce = null;
    String sce = drq.queryLookup(DapConstants.CONSTRAINTTAG);
    ce = CEConstraint.compile(sce, dmr);
    if(sce != null) // only show if client supplied a constraint expression
      setConstraint(dmr, ce);

    // Provide a PrintWriter for capturing the DMR.
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    // Get the DMR as a string
    DMRPrinter dapprinter = new DMRPrinter(dmr, ce, pw, drq.getFormat());
    dapprinter.print();
    pw.close();
    sw.close();

    String sdmr = sw.toString();
    if (DEBUG)
      System.err.println("Sending: DMR:\n" + sdmr);

    addCommonHeaders(drq);// Add relevant headers

    // Wrap the outputstream with a Chunk writer
    OutputStream out = drq.getOutputStream();
    ChunkWriter cw = new ChunkWriter(out, RequestMode.DMR, order);
    cw.cacheDMR(sdmr);
    cw.close();
  }

  /**
   * Process a DataDMR request.
   * Note that if this throws an exception,
   * then it has not yet started to output
   * a response. It a response had been initiated,
   * then the exception would produce an error chunk.
   * <p>
   * * @param cxt The dap context
   */

  protected void doData(DapRequest drq, DapContext cxt) throws IOException {
    // Convert the url to an absolute path
    String realpath = drq.getResourcePath(drq.getDatasetPath());

    CDMWrap c4 = DapCache.open(realpath, cxt);
    if (c4 == null)
      throw new DapException("No such file: " + realpath);
    DapDataset dmr = c4.getDMR();
    if (DUMPDMR) {
      printDMR(dmr);
      System.err.println(printDMR(dmr));
      System.err.flush();
    }

    /* Annotate with our endianness */
    ByteOrder order = (ByteOrder) cxt.get(DapConstants.DAP4ENDIANTAG);
    setEndianness(dmr, order);

    // Process any constraint
    CEConstraint ce = null;
    String sce = drq.queryLookup(DapConstants.CONSTRAINTTAG);
    ce = CEConstraint.compile(sce, dmr);
    setConstraint(dmr, ce);

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    // Get the DMR as a string
    DMRPrinter dapprinter = new DMRPrinter(dmr, ce, pw, drq.getFormat());
    dapprinter.print();
    pw.close();
    sw.close();

    String sdmr = sw.toString();
    if (DEBUG || DUMPDMR)
      System.err.println("Sending: Data DMR:\n" + sdmr);

    // Wrap the outputstream with a Chunk writer
    OutputStream out = drq.getOutputStream();
    ChunkWriter cw = new ChunkWriter(out, RequestMode.DAP, order);
    cw.setWriteLimit(getBinaryWriteLimit());
    cw.cacheDMR(sdmr);
    cw.flush();

    addCommonHeaders(drq);

    // Dump the databuffer part
    switch (drq.getFormat()) {
      case TEXT:
      case XML:
      case HTML:
        throw new IOException("Unsupported return format: " + drq.getFormat());
      /*
       * sw = new StringWriter();
       * DAPPrint dp = new DAPPrint(sw);
       * dp.print(dsp.getDataset(), ce);
       * break;
       */
      case NONE:
      default:
        DapSerializer writer = new DapSerializer(c4, ce, cw, order, drq.getChecksumMode());
        writer.write(c4.getDMR());
        cw.flush();
        cw.close();
        break;
    }
    // Should we dump data?
    if (DUMPDATA) {
      byte[] data = cw.getDump();
      if (data != null)
        DapDump.dumpbytesraw(data, cw.getWriteOrder(), "ChunkWriter.write");
    }
  }

  //////////////////////////////////////////////////////////
  // Utility Methods

  protected void addCommonHeaders(DapRequest drq) throws DapException {
    // Add relevant headers
    ResponseFormat format = drq.getFormat();
    if (format == null)
      format = ResponseFormat.NONE;
    RequestMode mode = drq.getMode();
    if (mode == null)
      mode = RequestMode.CAPABILITIES;
    DapProtocol.ContentType contenttype = DapProtocol.contenttypes.get(DapProtocol.contentKey(mode, format));
    if (contenttype == null)
      throw new DapException("Cannot find Content-Type for: " + mode.id() + "," + format.id())
          .setCode(DapCodes.SC_BAD_REQUEST);
    // If we go by what OPeNDAP does, then the header rules are as follow:
    // 1. Set the Content-Description to the application/vnd.opendap.dap4...
    // 2. If the Response type is not-null, then use the appropriate mime type such as text/xml.
    // 3. If the Response type is null, then use the same value as Content-Description.
    // Unfortunately, my HTTP-foo is not good enough to handle rule 3; a web-browser does
    // not seem to figure out the actual mime-type from the "application/vnd.opendap.dap4..."
    // content type. So, we use a different rule:
    // 3a. If the response type is null, then set Content-Type to the proper mime-type.

    // Rule 1.
    drq.setResponseHeader("Content-Description", contenttype.contenttype);

    // Rules 2 and 3.
    String header = contenttype.mimetype;
    header = header + "; charset=utf-8";
    drq.setResponseHeader("Content-Type", header);
  }

  /**
   * Merge the servlet inputs into a single object
   * for easier transport as well as adding value.
   *
   * @param rq A Servlet request object
   * @param rsp A Servlet response object
   * @return the union of the
   *         servlet request and servlet response arguments
   *         from the servlet engine.
   */

  protected DapRequest getRequestState(HttpServletRequest rq, HttpServletResponse rsp) throws IOException {
    return new DapRequest(this, rq, rsp);
  }

  //////////////////////////////////////////////////////////
  // Error Methods

  /*
   * Note that these error returns are assumed to be before
   * any DAP4 response has been generated. So they will
   * set the header return code and an Error Response as body.
   * Error chunks are handled elsewhere.
   */

  /**
   * Generate an error based on the parameters
   *
   * @param drq DapRequest
   * @param httpcode 0=>no code specified
   * @param t exception that caused the error; may be null
   * @throws IOException
   */
  protected void senderror(DapRequest drq, int httpcode, Throwable t) throws IOException {
    if (httpcode == 0)
      httpcode = HttpServletResponse.SC_BAD_REQUEST;
    ErrorResponse err = new ErrorResponse();
    err.setCode(httpcode);
    if (t == null) {
      err.setMessage("Servlet error: " + drq.getURL());
    } else {
      StringWriter sw = new StringWriter();
      PrintWriter p = new PrintWriter(sw);
      t.printStackTrace(p);
      p.close();
      sw.close();
      err.setMessage(sw.toString());
    }
    err.setContext(drq.getURL());
    String errormsg = err.buildXML();
    drq.getResponse().sendError(httpcode, errormsg);
  }

  /**
   * Set special attribute: endianness : overwrite exiting value
   *
   * @param dmr
   * @param order
   * @throws DapException
   */
  void setEndianness(DapDataset dmr, ByteOrder order) throws DapException {
    DapAttribute a = dmr.findAttribute(DapConstants.LITTLEENDIANATTRNAME);
    if (a == null) {
      a = new DapAttribute(DapConstants.LITTLEENDIANATTRNAME, DapType.UINT8);
      dmr.addAttribute(a);
    }
    // ByteOrder order = (ByteOrder) cxt.get(Dap4Util.DAP4ENDIANTAG);
    String oz = (order == ByteOrder.BIG_ENDIAN ? "0" : "1");
    a.setValues(new String[] {oz});
  }

  /**
   * Set special attribute: constraint : overwrite exiting value
   *
   * @param dmr dmr to annotate
   * @param ce the new constraint
   * @throws DapException
   */
  void setConstraint(DapDataset dmr, CEConstraint ce) throws DapException {
    if (ce == null)
      return;
    if (ce.isUniversal())
      return;
    DapAttribute a = dmr.findAttribute(DapConstants.CEATTRNAME);
    if (a == null) {
      a = new DapAttribute(DapConstants.CEATTRNAME, DapType.STRING);
      dmr.addAttribute(a);
    }
    String sce = ce.toConstraintString();
    a.setValues(new String[] {sce});
  }

  static public String printDMR(DapDataset dmr) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    DMRPrinter printer = new DMRPrinter(dmr, pw);
    try {
      printer.print();
      pw.close();
      sw.close();
    } catch (IOException e) {
    }
    return sw.toString();
  }

}


