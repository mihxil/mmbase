/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.framework;

import java.util.*;
import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;
import org.mmbase.module.core.MMBase;
import org.mmbase.util.functions.*;
import org.mmbase.util.GenericResponseWrapper;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Currently this generates a link to an editwizard list page, but I think it should render the list
 * (or wizard) itself (and frame it in a div, as requested by the contract of a HEAD block).
 *
 * @author Michiel Meeuwissen
 * @version $Id: EditwizardRenderer.java,v 1.8 2006-12-08 15:19:16 johannes Exp $
 * @since MMBase-1.9
 */
public class EditwizardRenderer extends AbstractRenderer {
    private static final Logger log = Logging.getLoggerInstance(EditwizardRenderer.class);

    protected String list;
    protected String path;

    public EditwizardRenderer(String t, Block parent) {
        super(t, parent);
    }

    public void setList(String l) {
        list = l;
    }
    public void setNodepath(String p) {
        path = p;
    }

    public Parameter[] getParameters() {
        return new Parameter[] {Parameter.RESPONSE, Parameter.REQUEST, Parameter.LOCALE};
    }

    public void render(Parameters blockParameters, Parameters frameworkParameters, Writer w, Renderer.WindowState state) throws IOException {
        HttpServletRequest request   = blockParameters.get(Parameter.REQUEST);
        HttpServletResponse response = blockParameters.get(Parameter.RESPONSE);
        Locale  locale = blockParameters.get(Parameter.LOCALE);
        w.write(response.encodeUrl(request.getContextPath() +
                                   "/mmbase/edit/wizard/jsp/list.jsp?wizard=" + list +
                                   "&amp;nodepath=" + path +
                                   "&amp;language=" + locale.getLanguage()));
    }
    public String toString() {
        return "EW " + list + " &nodepath= " + path;
    }

}
