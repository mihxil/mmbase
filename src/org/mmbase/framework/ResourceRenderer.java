/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.framework;

import java.util.*;
import java.net.*;
import java.io.*;
import javax.servlet.http.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.Source;
import javax.xml.transform.Result;
import org.mmbase.util.functions.*;
import org.mmbase.util.*;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * A {@link Renderer} implementation based on an MMBase's {@link org.mmbase.util.ResourceLoader}. Blocks rendered with this,
 * cannot have parameters.

 *
 * @author Michiel Meeuwissen
 * @version $Id: ResourceRenderer.java,v 1.4 2008-08-06 12:00:11 michiel Exp $
 * @since MMBase-1.9
 */
public class ResourceRenderer extends AbstractRenderer {
    private static final Logger log = Logging.getLoggerInstance(ResourceRenderer.class);


    protected String resource;
    protected String type = "web";

    public ResourceRenderer(String t, Block parent) {
        super(t, parent);
    }

    public  Parameter[] getParameters() {
        return new Parameter[] {};
    }

    public void setName(String r) {
        resource = r;
    }


    public void setType(String t) {
        type = t;
    }




    public void render(Parameters blockParameters, Parameters frameworkParameters,
                       Writer w, WindowState state) throws FrameworkException {


        try {
            Reader r = ResourceLoader.Type.valueOf(type.toUpperCase()).get().getReader(resource);
            if (r == null) throw new FrameworkException("No such resource " +  ResourceLoader.Type.valueOf(type.toUpperCase()).get().getResource(resource));
            char[] buf = new char[1000];
            int c;
            while ((c = r.read(buf, 0, 1000)) > 0) {
                w.write(buf, 0, c);
            }
        } catch (IOException ioe) {
            throw new FrameworkException(ioe);
        }
    }


    public String toString() {
        return resource;
    }

}
