/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2010, OPeNDAP, Inc.
// Copyright (c) 2002,2003 OPeNDAP, Inc.
//
// Author: James Gallagher <jgallagher@opendap.org>
//
// All rights reserved.
//
// Redistribution and use in source and binary forms,
// with or without modification, are permitted provided
// that the following conditions are met:
//
// - Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//
// - Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
//
// - Neither the name of the OPeNDAP nor the names of its contributors may
// be used to endorse or promote products derived from this software
// without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
/////////////////////////////////////////////////////////////////////////////

package opendap.servlet.www;

public class jscriptCore {
  private static boolean _Debug = false;
  public static String jScriptCode = "\n" + "// -*- Java -*-\n" + "\n"
      + "// $Id: jscriptCore.java 15901 2007-02-28 23:57:28Z jimg $\n" + "\n" + "// (c) COPYRIGHT URI/MIT 1999\n"
      + "// Please read the full copyright statement in the file COPYRIGHT.\n" + "//\n" + "// Authors:\n"
      + "//  jhrg,jimg  James Gallagher (jgallagher@gso.url.edu)\n" + "\n"
      + "var reflection_cgi = \"http://unidata.ucar.edu/cgi-bin/dods/posturl.pl\";\n" + "\n"
      + "// Event handlers for the disposition button.\n" + "\n"
      + "// The ascii_button handler sends data to a new window. The user can then \n" + "// save the data to a file.\n"
      + "\n" + "function ascii_button() {\n" + "    var url = new String(document.forms[0].url.value);\n" + "\n"
      + "    var url_parts = url.split(\"?\");\n" + "    /* handle case where constraint is null. */\n"
      + "    if (url_parts[1] != null) {\n"
      + "        var ascii_url = url_parts[0] + \".ascii?\" + encodeURI(url_parts[1]);\n" + "    }\n" + "    else {\n"
      + "        var ascii_url = url_parts[0] + \".ascii?\";\n" + "    }\n" + "\n"
      + "    window.open(ascii_url, \"ASCII_Data\");\n" + "}\n" + "\n"
      + "/* The binary_button handler loads the data to the current window. Since it \n"
      + "   is binary, Netscape will ask the user for a filename and save the data\n" + "   to that file. */\n" + "\n"
      + "function binary_button() {\n" + "    var url = new String(document.forms[0].url.value);\n" + "\n"
      + "    var url_parts = url.split(\"?\");\n" + "    /* handle case where constraint is null. */\n"
      + "    if (url_parts[1] != null) {\n"
      + "        var binary_url = url_parts[0] + \".dods?\" + encodeURI(url_parts[1]);\n" + "    }\n" + "    else {\n"
      + "        var binary_url = url_parts[0] + \".dods?\";\n" + "    }\n" + "\n"
      + "    window.location = binary_url;\n" + "}\n" + "\n"
      + "/* Route the URL to Matlab, IDL, .... Users must add an entry into their mime\n"
      + "   types file (aka list of Netscape helper applications) so that the URL will\n"
      + "   be fedd into Matlab which must, in addition, be running loadopendap.\n" + "\n"
      + "   Note that reflection_cgi is a global JavaScript variable set at the \n" + "   begining of this `file'. */\n"
      + "\n" + "function program_button() {\n" + "    var program_url = new String(document.forms[0].url.value);\n"
      + "\n" + "    /* Build a call to the reflector CGI. */\n"
      + "    var CGI = reflection_cgi + \"?\" + \"url=\" + program_url + \"&disposition=matlab\";\n" + "\n"
      + "    window.location = CGI;\n" + "}\n" + "\n" + "var help = 0;      // Our friend, the help window.\n" + "\n"
      + "function help_button() {\n" + "    // Check the global to keep from opening the window again if it is\n"
      + "    // already visible. I think Netscape handles this but I know it will\n"
      + "    // write the contents over and over again. This preents that, too.\n" + "    // 10/8/99 jhrg\n"
      + "    if (help && !help.closed)\n" + "  return;\n" + "\n"
      + "    // Resize on Netscape 4 is hosed. When enabled, if a user resizes then\n"
      + "    // the root window's document gets reloaded. This does not happen on IE5.\n"
      + "    // regardless, with scrollbars we don't absolutely need to be able to\n" + "    // resize. 10/8/99 jhrg\n"
      + "    help = window.open(\"\", \"help\", \"scrollbars,dependent,width=600,height=400\");\n"
      + "    write_help_contents(help);\n" + "}\n" + "\n" + "function write_help_contents() {\n"
      + "    help.document.writeln(\"<html><head><title> \" +\n"
      + "\"Help for the OPeNDAP Dataset Access Form</title></head><body><form> \" +\n"
      + "\"<center><h2>Help for the OPeNDAP Dataset Access Form</h2></center> \" +\n"
      + "\"This form displays information from the dataset whose URL is shown in \" +\n"
      + "\"the <em>DataURL</em> field. Each variable in this dataset is shown \" +\n"
      + "\"below in the section labeled <em>Variables</em>. \" +\n" + "\"<ul>\" +\n"
      + "\"<li>To select a variable click on the checkbox to its left. \" +\n"
      + "\"<li>To constrain a variable that you've selected, edit the information \" +\n"
      + "\"that appears in the text boxes below the variable. \" +\n"
      + "\"<li>To get ASCII or binary values for the variables you've selected, \" +\n"
      + "\"click on the <em>Get ASCII</em> or <em>Get Binary</em> buttons. \" +\n"
      + "\"Note that the URL displayed in the <em>DataURL</em> field is updated \" +\n"
      + "\"as you select and/or constrain variables. The URL in this field can be \" +\n"
      + "\"cut and pasted in various OPeNDAP clients such as the Matlab and IDL \" +\n"
      + "\"command extensions. See the <a \" +\n"
      + "\"href=\\\"https://www.opendap.org/support/OPeNDAP-clients\\\" target=\\\"_blank\\\"> \" +\n"
      + "\"OPeNDAP client page</a> for information about those clients. \" +\n" + "\"<p><hr><p> \" + \n"
      + "\"<center> \" +\n" + "\"<input type=\\\"button\\\" value=\\\"Close\\\" onclick=\\\"self.close()\\\"> \" +\n"
      + "\"</center></form></body></html>\");\n" + "}\n" + "\n" + "function open_dods_home() {\n"
      + "    window.open(\"https://www.opendap.org/\", \"DODS_HOME_PAGE\");\n" + "}\n" + "\n" + "\n"
      + "// Helper functions for the form.\n" + "\n" + "function describe_index() {\n"
      + "   defaultStatus = \"Enter start, stride and stop for the array dimension.\";\n" + "}\n" + "\n"
      + "function describe_selection() {\n" + "   defaultStatus = \"Enter a relational expression (e.g., <20).\";\n"
      + "}\n" + "\n" + "function describe_operator() {\n"
      + "   defaultStatus = \"Choose a relational operator. Use - to enter a function name).\";\n" + "}\n" + "\n"
      + "function describe_projection() {\n" + "   defaultStatus = \"Add this variable to the projection.\";\n" + "}\n"
      + "\n" + "///////////////////////////////////////////////////////////\n" + "// The dods_url object.\n"
      + "///////////////////////////////////////////////////////////\n" + "\n" + "// CTOR for dods_url\n"
      + "// Create the DODS URL object.\n" + "function dods_url(base_url) {\n" + "    this.url = base_url;\n"
      + "    this.projection = \"\";\n" + "    this.selection = \"\";\n" + "    this.num_dods_vars = 0;\n"
      + "    this.dods_vars = new Array();\n" + "        \n" + "    this.build_constraint = build_constraint;\n"
      + "    this.add_dods_var = add_dods_var;\n" + "    this.update_url = update_url;\n" + "}\n" + "\n"
      + "// Method of dods_url\n" + "// Add the projection and selection to the displayed URL.\n"
      + "function update_url() {\n" + "    this.build_constraint();\n" + "    var url_text = this.url;\n"
      + "    // Only add the projection & selection (and ?) if there really are\n" + "    // constraints! \n"
      + "    if (this.projection.length + this.selection.length > 0)\n"
      + "        url_text += \"?\" + this.projection + this.selection;\n"
      + "    document.forms[0].url.value = url_text;\n" + "}\n" + "\n" + "// Method of dods_url\n"
      + "// Scan all the form elements and pick out the various pieces of constraint\n"
      + "// information. Add these to the dods_url instance.\n" + "function build_constraint() {\n"
      + "    var p = \"\";\n" + "    var s = \"\";\n" + "    for (var i = 0; i < this.num_dods_vars; ++i) {\n"
      + "        if (this.dods_vars[i].is_projected == 1) {\n" + "      // The comma is a clause separator.\n"
      + "      if (p.length > 0)\n" + "          p += \",\";\n"
      + "            p += this.dods_vars[i].get_projection();\n" + "  }\n"
      + "  var temp_s = this.dods_vars[i].get_selection();\n" + "  if (temp_s.length > 0)\n"
      + "      s += \"&\" + temp_s;    // The ampersand is a prefix to the clause.\n" + "    }\n" + "\n"
      + "    this.projection = p;\n" + "    this.selection = s;\n" + "}\n" + "\n" + "// Method of dods_url\n"
      + "// Add the variable to the dods_var array of dods_vars. The var_index is the\n"
      + "// number of *this particular* variable in the dataset, zero-based.\n" + "function add_dods_var(dods_var) {\n"
      + "    this.dods_vars[this.num_dods_vars] = dods_var;\n" + "    this.num_dods_vars++;\n" + "}\n" + "\n"
      + "/////////////////////////////////////////////////////////////////\n" + "// dods_var\n"
      + "/////////////////////////////////////////////////////////////////\n" + "\n" + "// CTOR for dods_var\n"
      + "// name: the name of the variable from DODS' perspective.\n"
      + "// js_var_name: the name of the variable within the form.\n"
      + "// is_array: 1 if this is an array, 0 otherwise.\n" + "function dods_var(name, js_var_name, is_array) {\n"
      + "    // Common members\n" + "    this.name = name;\n" + "    this.js_var_name = js_var_name;\n"
      + "    this.is_projected = 0;\n" + "    if (is_array > 0) {\n" + "        this.is_array = 1;\n"
      + "        this.num_dims = 0;        // Holds the number of dimensions\n"
      + "        this.dims = new Array(); // Holds the length of the dimensions\n" + "\n"
      + "        this.add_dim = add_dim;\n" + "        this.display_indices = display_indices;\n"
      + "        this.erase_indices = erase_indices;\n" + "    }\n" + "    else\n" + "        this.is_array = 0;\n"
      + "\n" + "    this.handle_projection_change = handle_projection_change;\n"
      + "    this.get_projection = get_projection;\n" + "    this.get_selection = get_selection;\n" + "}\n" + "\n"
      + "// Method of dods_var\n" + "// Add a dimension to a DODS Array object.\n" + "function add_dim(dim_size) {\n"
      + "    this.dims[this.num_dims] = dim_size;\n" + "    this.num_dims++;\n" + "}\n" + "\n"
      + "// Method of dods_var\n" + "// Add the array indices to the text widgets associated with this DODS\n"
      + "// array object. The text widgets are names <var_name>_0, <var_name>_1, ...\n"
      + "// <var_name>_n for an array with size N+1.\n" + "function display_indices() {\n"
      + "    for (var i = 0; i < this.num_dims; ++i) {\n" + "        var end_index = this.dims[i]-1;\n"
      + "        var s = \"0:1:\" + end_index.toString();\n"
      + "  var text_widget = \"document.forms[0].\" + this.js_var_name + \"_\" + i.toString();\n"
      + "  eval(text_widget).value = s;\n" + "    }\n" + "}\n" + "\n" + "// Method of dods_var\n"
      + "// Use this to remove index information from a DODS array object.\n" + "function erase_indices() {\n"
      + "    for (var i = 0; i < this.num_dims; ++i) {\n"
      + "  var text_widget = \"document.forms[0].\" + this.js_var_name + \"_\" + i.toString();\n"
      + "  eval(text_widget).value = \"\";\n" + "    }\n" + "}\n" + "\n" + "// Method of  dods_var\n"
      + "function handle_projection_change(check_box) {\n" + "    if (check_box.checked) {\n"
      + "        this.is_projected = 1;\n" + "  if (this.is_array == 1)\n" + "      this.display_indices();\n"
      + "    }\n" + "    else {\n" + "        this.is_projected = 0;\n" + "  if (this.is_array == 1)\n"
      + "      this.erase_indices();\n" + "    }\n" + "\n" + "    DODS_URL.update_url();\n" + "}\n" + "\n" + "\n"
      + "// Method of dods_var\n" + "// Get the projection sub-expression for this variable.\n"
      + "function get_projection() {\n" + "    var p = \"\";\n" + "    if (this.is_array == 1) {\n"
      + "        p = this.name;    // ***\n" + "        for (var i = 0; i < this.num_dims; ++i) {\n"
      + "      var text_widget = \"document.forms[0].\" + this.js_var_name + \"_\" + i.toString();\n"
      + "      p += \"[\" + eval(text_widget).value + \"]\";\n" + "  }\n" + "    }\n" + "    else {\n"
      + "  p = this.name;    // ***\n" + "    }\n" + "\n" + "    return p;\n" + "}\n" + "\n" + "// Method of dods_var\n"
      + "// Get the selection (which is null for arrays).\n" + "function get_selection() {\n" + "    var s = \"\";\n"
      + "    if (this.is_array == 1) {\n" + "        return s;\n" + "    }\n" + "    else {\n"
      + "  var text_widget = \"document.forms[0].\" + this.js_var_name + \"_selection\";\n"
      + "        if (eval(text_widget).value != \"\") {\n"
      + "            var oper_widget_name = \"document.forms[0].\" + this.js_var_name + \"_operator\";\n"
      + "            var oper_widget = eval(oper_widget_name);\n"
      + "      var operator = oper_widget.options[oper_widget.selectedIndex].value;\n"
      + "            // If the operator is `-' then don't prepend the variable name!\n"
      + "            // This provides a way for users to enter function names as\n"
      + "            // selection clauses. \n" + "            if (operator == \"-\")\n"
      + "                s = eval(text_widget).value;\n" + "            else\n"
      + "          s = this.name + operator + eval(text_widget).value; // ***\n" + "        }\n" + "    }\n" + "\n"
      + "    return s;\n" + "}    \n" + "\n" + "// : jscriptCore.tmpl,v $\n"
      + "// Revision 1.4  2001/09/17 23:05:53  ndp\n" + "// *** empty log message ***\n" + "//\n"
      + "// Revision 1.1.2.3  2001/09/10 21:48:07  jimg\n"
      + "// Removed the `Send to Program' button and its help text.\n" + "//\n"
      + "// Revision 1.1.2.2  2001/09/10 19:32:28  jimg\n"
      + "// Fixed two problems: 1) Variable names in the JavaScript code sometimes\n"
      + "// contained spaces since they were made using the dataset's variable name.\n"
      + "// The names are now filtered through id2www and esc2underscore. 2) The CE\n"
      + "// sometimes contained spaces, again, because dataset variable names were\n"
      + "// used to build the CE. I filtered the names with id2www_ce before passing\n"
      + "// them to the JavaScript code.\n" + "//\n" + "// Revision 1.1.2.1  2001/01/26 04:01:13  jimg\n" + "// Added\n"
      + "//\n" + "// Revision 1.5  2000/11/09 21:04:37  jimg\n"
      + "// Merged changes from release-3-1. There was a goof and a bunch of the\n"
      + "// changes never made it to the branch. I merged the entire branch.\n" + "// There maybe problems still...\n"
      + "//\n" + "// Revision 1.4  2000/10/03 20:07:21  jimg\n" + "// Moved Logs to the end of each file.\n" + "//\n"
      + "// Revision 1.3  1999/05/18 20:08:18  jimg\n"
      + "// Fixed massive problems introduced by the String to string changes.\n" + "//\n"
      + "// Revision 1.2  2000/11/09 21:04:37  jimg\n"
      + "// Merged changes from release-3-1. There was a goof and a bunch of the\n"
      + "// changes never made it to the branch. I merged the entire branch.\n" + "// There maybe problems still...\n"
      + "//\n" + "// Revision 1.1.2.3  1999/10/13 17:02:55  jimg\n" + "// Changed location of posturl.pl.\n" + "//\n"
      + "// Revision 1.1.2.2  1999/10/11 17:57:32  jimg\n"
      + "// Fixed a bug which showed up in IE 5. Objects in IE 5 cannot use eval() to\n"
      + "// name a field and access a property of that field in the same statement.\n"
      + "// Instead, the use of eval to name a field and the access to that (new)\n"
      + "// field must be broken up. I think this is the case because IE 5's parser\n"
      + "// thinks `eval' is, in this situation, an object property. Of course,\n"
      + "// there's no eval property per se, so script execution halts. See the use of\n"
      + "// the document.forms[0].<text_widget> stuff in the method display_indices().\n" + "//\n"
      + "// Revision 1.1.2.1  1999/10/09 00:30:36  jimg\n" + "// Created.\n";
}


