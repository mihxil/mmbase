/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.module.core;
import java.util.Iterator;

/**
 * Modules are pieces of functionality that are not MMBase objects.
 * e.g. Session, Mail, Upload and other functionality
 *
 * @author Rob Vermeulen
 * @author Pierre van Rooden
 */
public interface ModuleInterface {

 	/**
     * Retrieves the CloudContext to which this module belongs
     */
    public CloudContextInterface getCloudContext();

 	/**
	 * Retrieve the Graphical User Interface name of the module
	 * @param language the language in which you want the name
	 */
	public String getGuiName(String language);

	/**
     * Retrieve the System name of the module
     */
    public String getName();

	/**
	 * Retrieve the description of the nodetype
	 * @param language the language in which you want the description
	 */
	public String getDescription(String language);

	/** 
	 * Retireve the description of the nodetype
	 */
	public String getDescription();

}
