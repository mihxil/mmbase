/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import org.mmbase.module.builders.*;
import org.mmbase.module.core.*;
import org.mmbase.util.logging.*;

/**
 * Redirects request based on information supplied by the jumpers builder.
 *
 * @author Jaco de Groot
 */
public class JumpersFilter implements Filter {
    private static Logger log;
    private static MMBase mmb;
    private static Jumpers bul;
    private static String name;

    public void init(javax.servlet.FilterConfig filterConfig)
            throws ServletException {
        name = filterConfig.getFilterName();
        MMBaseContext.init(filterConfig.getServletContext());
        MMBaseContext.initHtmlRoot();
        // Initializing log here because log4j has to be initialized first.
        log = Logging.getLoggerInstance(JumpersFilter.class.getName());
        log.info("Init of " + name + ".");
        mmb = (MMBase)org.mmbase.module.Module.getModule("MMBASEROOT");
        if (mmb == null) {
            String message = "Could not start MMBase.";
            log.error(message);
            throw new ServletException(message);
        }
        bul = (Jumpers)mmb.getMMObject("jumpers");
        if (bul == null) {
            String message = "Could not find jumpers builder.";
            log.error(message);
            throw new ServletException(message);
        }
        log.info("Filter " + name + " initialized.");
    }

    public void doFilter(ServletRequest servletRequest,
            ServletResponse servletResponse, FilterChain filterChain) 
            throws java.io.IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest)servletRequest;
        HttpServletResponse res = (HttpServletResponse)servletResponse;
        String key = req.getRequestURI().substring(1);
        if (key.indexOf('.') == -1 && !key.endsWith("/")) {
            String url = bul.getJump(key);
            if (url != null) {
                res.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                res.setContentType("text/html");
                res.setHeader("Location",url);
                return;
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    public void destroy() {
        log.info("Filter " + name + " destroyed.");
    }

}


