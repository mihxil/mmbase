/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.database.support;

import java.util.*;
import java.net.*;
import java.sql.*;

import org.mmbase.module.core.*;


/**
* MMJdbc2NodeInterface interface needs to be implemented to support a new database
* it is used to abstact the query's needed for mmbase for each database.
*/
public interface MMJdbc2NodeInterface {
	public MMObjectNode decodeDBnodeField(MMObjectNode node,String fieldtype,String fieldname, ResultSet rs,int i);
	public MMObjectNode decodeDBnodeField(MMObjectNode node,String fieldtype,String fieldname, ResultSet rs,int i,String prefix);
	public String getMMNodeSearch2SQL(String where,MMObjectBuilder bul);
	public String getShortedText(String tableName,String fieldname,int number);
	public byte[] getShortedByte(String tableName,String fieldname,int number);
	public byte[] getDBByte(ResultSet rs,int idx);
	public String getDBText(ResultSet rs,int idx);
	public int insert(MMObjectBuilder bul,String owner, MMObjectNode node);
	public boolean create(String tableName);
	public boolean commit(MMObjectBuilder bul,MMObjectNode node);
	public void removeNode(MMObjectBuilder bul,MMObjectNode node);
	public int getDBKey();
	public void init(MMBase mmb);
	public void setDBByte(int i, PreparedStatement stmt,byte[] bytes);
	public boolean created(String tableName);
}
