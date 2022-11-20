/*
 * Copyright 2009, UCAR/Unidata and OPeNDAP, Inc.
 * See the LICENSE file for more information.
 */

package dap4.servlet;

import dap4.core.util.DapConstants;
import dap4.core.util.DapContext;
import dap4.core.util.IndentWriter;
import dap4.core.util.ResponseFormat;
import dap4.dap4lib.DapProtocol;
import dap4.dap4lib.RequestMode;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * Generate the DSR for a dataset.
 * Currently only generates a minimal DSR.
 */

public class DapDSR {

  //////////////////////////////////////////////////
  // Constants

  static final boolean DEBUG = false;

  static final String DSRXMLTEMPLATE = "/resources/dsr.xml.template";
  static final String DSRHTMLTEMPLATE = "/resources/dsr.html.template";

  //////////////////////////////////////////////////
  // Instance Variables
  DapRequest drq;
  DapContext cxt;

  //////////////////////////////////////////////////
  // Constructor(s)

  public DapDSR(DapRequest drq, DapContext cxt) throws IOException {
    this.drq = drq;
    this.cxt = cxt;
  }

  //////////////////////////////////////////////////
  // Accessors

  //////////////////////////////////////////////////
  // API

  public String generate(ResponseFormat format, String dataset) throws IOException {
    // Get the DSR template
    String template = getTemplate(format);
    String datasetname = dataset;
    StringBuilder dsr = new StringBuilder(template);
    substitute(dsr,"DAP_VERSION",DapConstants.X_DAP_VERSION);
    substitute(dsr,"DAP_SERVER",DapConstants.X_DAP_SERVER);
    substitute(dsr,"DATASET",dataset);
    return dsr.toString();
  }

  protected String getTemplate(ResponseFormat format) throws IOException {
    StringBuilder buf = new StringBuilder();
    // Get template as resource stream
    String template = null;
    switch (format) {
    case XML: template = DSRXMLTEMPLATE; break;
    case HTML: template = DSRHTMLTEMPLATE; break;
    default: throw new IOException("Unsupported DSR Response Format: "+format.toString());
    }
    try (InputStream stream = drq.getServletContext().getResourceAsStream(template)) {
      int ch;
      while((ch = stream.read()) >= 0) {buf.append((char)ch);}
    }
    return buf.toString();
  }

  protected void substitute(StringBuilder buf, String macro, String value) {
    int from = 0;
    String tag = "${"+macro+"}";
    int taglen = tag.length();
    int valuelen = value.length();
    for(;;) {
	int index = buf.indexOf(tag,from);
	if (index < 0) break;
	buf.replace(index,index+taglen,value);
	from = index+valuelen;
    }
  }


} // DapDSR
