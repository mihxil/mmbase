/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders;

import java.util.*;
import java.sql.*;
import java.io.*;

import org.mmbase.module.database.*;
import org.mmbase.module.core.*;
import org.mmbase.util.*;

/**
 * @author Rob Vermeulen  
 * @version March 22, 1998
 */
public class RAStatistics extends MMObjectBuilder {

	public RAStatistics(MMBase m) {
		this.mmb=m;
		this.tableName="rastatistics";
		this.description="Rastatistics";
		init();
	}

	
	/**
	* insert a new object, normally not used (only subtables are used)
	*/
	public boolean create() {
		// create the main object table
		try {
			Statement stmt=mmb.getConnection().createStatement();
			stmt.executeUpdate("create table "+mmb.baseName+"_rastatistics  ("
								 	+"	ip text not null "
									+", startTime int"
									+", raName text"
									+", bytesSent int"
									+", timeSent int"
									+") under "+mmb.baseName+"_object");
			return(true);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return(false);
	}


	public String getGUIIndicator(MMObjectNode node) {
		String str=node.getStringValue("title");
		if (str.length()>15) {
			return(str.substring(0,12)+"...");
		} else {
			return(str);
		}
	}

	public String getGUIIndicator(String field,MMObjectNode node) {
		return(null);
	}

}
