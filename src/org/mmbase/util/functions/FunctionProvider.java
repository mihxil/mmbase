/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

 */
package org.mmbase.util.functions;

import java.util.*;

/**
 * A function provider maintains a set of {@link Function} objects.
 *
 * @since MMBase-1.8
 * @author Pierre van Rooden
 * @version $Id: FunctionProvider.java,v 1.4 2005-05-03 19:59:27 michiel Exp $
 */
public class FunctionProvider {

    /**
     * @javadoc
     */
    protected Function getFunctions = new AbstractFunction("getFunctions", Parameter.EMPTY, ReturnType.SET) {
        public Object getFunctionValue(Parameters arguments) {
            return getFunctions();
        }
    };

    protected Map functions = Collections.synchronizedMap(new HashMap());

    /**
     * @javadoc
     */
    public FunctionProvider() {
        // determine parameters through reflection
        Map parameterDefinitions =  Functions.getParameterDefinitonsByReflection(this.getClass(), new HashMap());
        for (Iterator i = parameterDefinitions.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry) i.next();
            Function fun = newFunctionInstance((String)entry.getKey(), (Parameter[])entry.getValue(), ReturnType.UNKNOWN);
            fun.setDescription("Function automaticly found by reflection on public Parameter[] members");
            addFunction(fun);
        }
        addFunction(getFunctions);
    }

    /**
     * @javadoc
     */
    protected Function newFunctionInstance(String name, Parameter[] parameters, ReturnType returnType) {
        return new ProviderFunction(name, parameters, returnType, this);
    }

    /**
     * @javadoc
     */
    public void addFunction(Function function) {
        functions.put(function.getName(), function);
    }

    /**
     * @javadoc
     */
    public Parameters createParameters(String functionName) {
        Function function = getFunction(functionName);
        if (function != null) {
            return function.createParameters();
        } else {
            return null;
        }
    }

    /**
     * @javadoc
     */
    public Object getFunctionValue(String functionName, List parameters) {
        Function function = getFunction(functionName);
        if (function != null) {
            return function.getFunctionValueWithList(parameters);
        } else {
            return null;
        }
    }

    /**
     * @javadoc
     */
    protected Object executeFunction(String functionName, Parameters parameters) {
        return null;
    }

    /**
     * @javadoc
     */
    public Function getFunction(String functionName) {
        return (Function)functions.get(functionName);
    }

    /**
     * @javadoc
     */
    public Set getFunctions() {
        Set set = new HashSet(functions.values());
        set.remove(null); // remove null values
        return set;
    }
}
