/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.servlet;

import org.mmbase.module.core.MMBase;
import org.mmbase.module.core.MMBaseContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;


import org.mmbase.util.logging.Logging;
import org.mmbase.util.logging.Logger;


/**
 * MMBaseServlet is a base class for other MMBase servlets (like ImageServlet). Its main goal is to
 * store a MMBase instance for all its descendants, but it can also be used as a serlvet itself, to
 * show MMBase version information.
 *
 * @version $Id: MMBaseServlet.java,v 1.4 2002-04-02 12:50:53 michiel Exp $
 * @author Michiel Meeuwissen
 * @since  MMBase-1.6
 */
public class MMBaseServlet extends  HttpServlet {
    
    private   static Logger log;
    protected static MMBase mmbase;
    // private   static String context;


    // ----------------
    // members needed for refcount functionality.

    /**
     * To keep track of the currently running servlets
     * switch the following boolean to true.
     *
     * @bad-constant
     */
    private static final boolean logServlets = true;
    private static int servletCount; // Number of running servlets
    /**
     *  Lock to sync add and remove of threads
     */
    private static Object servletCountLock = new Object();
    /**
     * Hashtable containing currently running servlets
     */
    private static Hashtable runningServlets = new Hashtable();
    /**
     * Toggle to print running servlets to log.
     * @javadoc Not clear, I don't understand it.
     */
    private static int printCount;


    /**
     * The init of an MMBaseServlet checks if MMBase is running. It not then it is started.
     */

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        if (! MMBaseContext.isInitialized()) {
            ServletContext servletContext = getServletConfig().getServletContext();
            MMBaseContext.init(servletContext);
            MMBaseContext.initHtmlRoot();
        }
        if (log == null) {
            // Initializing log here because log4j has to be initialized first.
            log = Logging.getLoggerInstance(MMBaseServlet.class.getName());
        }

        /*
        if (context == null) {
            try {
                context = config.getServletContext().getServletContextName(); // only availabe in higher serlvet versions (e.g. not supported by Orion 1.5.4)
            } catch (Exception e) {
                log.error("hooi");
            }
        }
        log.info("found context: " + context);
        */
        log.info("Init of servlet " + config.getServletName() + ".");
        if (mmbase == null) {
            mmbase = (MMBase) org.mmbase.module.Module.getModule("MMBASEROOT");
            if (mmbase == null) {
                log.error("Could not find module with name 'MMBASEROOT'!");
            }
        }

    }

    /**
     * Serves MMBase version information. This doesn't do much usefull
     * yet, but one could image lots of cool stuff here. Any other
     * MMBase servlet will probably override this method.
     */

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/plain");
        PrintWriter pw = res.getWriter();
        pw.print(org.mmbase.Version.get());
        pw.close();
    }

    /**
     * The service method is extended with calls for the refCount
     * functionality (for performance related debugging).  So you can
     * simply override doGet in extension classes, and this stays
     * working, without having to think about it.
     */
    public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException,IOException {
        incRefCount(req);
        try {
            super.service(req, res);
        } finally { // whatever happens, decrease the refcount:
            decRefCount(req);
        }
    }
    
    /**
     * Returns information about this servlet. Don't forget to override it.
     */

    public String getServletInfo()  {
        return "Serves MMBase version information";
    }



    // ----------------------
    // functions needed for refcount functionality.

    /**
     * Return URI with QueryString appended
     * @param req The HttpServletRequest.
     */
    protected static String getRequestURL(HttpServletRequest req) {
        String result = req.getRequestURI();
        String queryString = req.getQueryString();
        if (queryString!=null) result += "?" + queryString;
        return result;
    }

    
    /**
     * Decrease the reference count of the servlet
     * @param req The HttpServletRequest.
     * @scopy private
     */

    protected void decRefCount(HttpServletRequest req) {
        if (logServlets) {
            String url = getRequestURL(req);
            url += " " + req.getMethod();
            synchronized (servletCountLock) {
                servletCount--;
                ReferenceCountServlet s = (ReferenceCountServlet) runningServlets.get(this);
                if (s!=null) {
                    if (s.refCount==0) runningServlets.remove(this);
                    else {
                        s.refCount--;
                        int i = s.uris.indexOf(url);
                        if (i>=0) s.uris.removeElementAt(i);
                    }
                }// s!=null
            }//sync
        }// if (logServlets)
    }
   
    /**
     * Increase the reference count of the servlet (for debugging)
     * and send running servlets to log once every 32 requests
     * @param req The HttpServletRequest.
     * @scope private
     * @bad-constant  31 should be configurable.
     */
    
    protected void incRefCount(HttpServletRequest req) {
        if (logServlets) {
            String url = getRequestURL(req);
            url += " " + req.getMethod();
            int curCount;
            synchronized (servletCountLock) {
                servletCount++; curCount=servletCount; printCount++;
                ReferenceCountServlet s = (ReferenceCountServlet) runningServlets.get(this);
                if (s==null) runningServlets.put(this, new ReferenceCountServlet(this, url, 0));
                else { s.refCount++; s.uris.addElement(url); }
            }// sync

            if ((printCount & 31) == 0) { // Why not (printCount % <configurable number>) == 0?
                if (curCount > 0) {
                    log.info("Running servlets: "+curCount);
                    for(Enumeration e=runningServlets.elements(); e.hasMoreElements();) {
                        log.info(e.nextElement());
                    }
                }// curCount>0
            }
        }
    }
   
    public void destroy() {
        log.info("Servlet " + getServletName() + " is taken out of service");
        super.destroy();
    }

    /**
     * This class maintains current state information for a running servlet.
     * It contains a reference count, as well as a list of URI's being handled by the servlet.
     */
    private class ReferenceCountServlet {
        /**
         * The servlet do debug
         * @scope private
         */
        MMBaseServlet servlet;
        /**
         * List of URIs that call the servlet
         * @scope private
         */
        Vector uris = new Vector();
        /**
         * Nr. of references
         * @scope private
         */
        int refCount;

        /**
         * Create a new ReferenceCountServlet using the jamesServlet
         */
        ReferenceCountServlet(MMBaseServlet servlet, String uri, int refCount) {
            this.servlet = servlet;
            uris.add(uri);
            this.refCount = refCount;
        }

        /**
         * Return a description containing servlet info and URI's
         */
        public String toString() {
            return "servlet("+servlet+"), refcount("+(refCount+1)+"), uri's("+uris+")";
        }
    }


}
