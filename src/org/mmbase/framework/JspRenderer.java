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
import org.mmbase.util.functions.*;
import org.mmbase.util.GenericResponseWrapper;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * A Renderer implmentation based on a jsp.
 *
 * @author Michiel Meeuwissen
 * @version $Id: JspRenderer.java,v 1.20 2007-04-13 13:21:05 michiel Exp $
 * @since MMBase-1.9
 */
public class JspRenderer extends AbstractRenderer {
    private static final Logger log = Logging.getLoggerInstance(JspRenderer.class);

    public static String JSP_ROOT = "/mmbase/components/";

    protected final String path;

    public JspRenderer(String t, String p, Block parent) {
        super(t, parent);
        path = p;
    }

    public String getPath() {
        return path.charAt(0) == '/' ? path : JSP_ROOT + getBlock().getComponent().getName() + '/' + path;
    }

    public  Parameter[] getParameters() {
        return new Parameter[] {Parameter.RESPONSE, Parameter.REQUEST};
    }

    public void render(Parameters blockParameters, Parameters frameworkParameters, Writer w, WindowState state) throws FrameworkException {
        try {
            HttpServletResponse response = blockParameters.get(Parameter.RESPONSE);
            HttpServletRequest request  = blockParameters.get(Parameter.REQUEST);
            GenericResponseWrapper respw = new GenericResponseWrapper(response);
            String url = getFramework().getInternalUrl(getPath(), this, getBlock().getComponent(), blockParameters, frameworkParameters).toString();
            RequestDispatcher requestDispatcher = request.getRequestDispatcher(url);
            for (Map.Entry<String, ?> entry : blockParameters.toMap().entrySet()) {
                request.setAttribute(entry.getKey(), entry.getValue());
            }
            requestDispatcher.include(request, respw);
            w.write(respw.toString());
        } catch (ServletException se) {
            throw new FrameworkException(se.getMessage(), se);
        } catch (IOException ioe) {
            throw new FrameworkException(ioe.getMessage(), ioe);
        }
    }

    public String toString() {
        Parameter.Wrapper wrapper = getBlock().specific;
        return getPath() + (wrapper == null ? "" : "?" + wrapper);
    }
}
