/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.util.functions;

import java.util.*;

/**
 * Entry for Parameters. A (function) argument is specified by a name and type.
 *
 * @author Michiel Meeuwissen
 * @author Daniel Ockeloen (MMFunctionParam)
 * @since  MMBase-1.7
 * @version $Id: Parameter.java,v 1.2 2004-01-24 21:16:26 daniel Exp $
 * @see Parameters
 */

public class Parameter { 

    /**
     * Parameter which might be needed in lots of Parameter definitions. 
     */
    public static final Parameter LANGUAGE = new Parameter("language", String.class);
    public static final Parameter USER     = new Parameter("user",     org.mmbase.bridge.User.class);
    public static final Parameter RESPONSE = new Parameter("response", javax.servlet.http.HttpServletResponse.class);
    public static final Parameter REQUEST  = new Parameter("request",  javax.servlet.http.HttpServletRequest.class);



    // package for Parameters (direct access avoids function calls)
    String key;
    Class type;
    String description  = "";
    Object defaultValue = null;
    boolean required    = false;

    protected Parameter() {}

    public Parameter(String k, Class c) {
        key = k;
        type = c;
    }

    public Parameter(String k, Class c, boolean req) {
        key = k;
        type = c;
        required = req;
    }

    public Parameter(String k, Class c, Object def) {
        key = k;
        type = c;
        defaultValue = def;
        required = false;
    }

    public String getName () {
        return key;
    }

    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setDefaultValue(Object dev) {
        defaultValue = dev;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }
    public Class getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean checkType(Object value) {
        if (! type.isInstance(value)) {
	  	return false; 
        }
	return true;
    }

    

    public boolean equals(Object o) {
        if (o instanceof Parameter) {
            Parameter a = (Parameter) o;
            return a.key.equals(key) && a.type.equals(type);
        }
        return false;
    }

    public String toString() {
        return type.getName() + " " + key;
    }



    /** 
     * An Parameter.Wrapper wraps one Parameter around an Parameter[]
     * (then you can put it in a Parameter[]).  Parameters will
     * recognize this.
     */
    public static class Wrapper extends Parameter {
        Parameter[] arguments;

        public Wrapper(Parameter[] arg) {
            key = "[ARRAYWRAPPER]";
            arguments = arg;
            type = Parameter[].class; // should not remain null, (equals of Parameter depends on that)
        }
        
        public String toString() {
            return "WRAPPED" + Arrays.asList(arguments).toString();
        }
    }

}
