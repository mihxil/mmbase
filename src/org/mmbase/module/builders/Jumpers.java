/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders;

import java.util.*;
import java.sql.*;
import org.mmbase.module.core.*;
import org.mmbase.module.database.*;
import org.mmbase.util.*;
import org.mmbase.util.logging.*;

/**
 * @author Daniel Ockeloen
 * @version 12 Mar 1997
 */
public class Jumpers extends MMObjectBuilder {
    private static Logger log = Logging.getLoggerInstance(Jumpers.class.getName());
	LRUHashtable jumpCache = new LRUHashtable(1000);
	
	private static String jumperNotFoundURL = "/index.html"; 

	public boolean init() {
		super.init();

		String tmp;
		tmp=getInitParameter("JumperNotFoundURL");
		if (tmp!=null) {
			jumperNotFoundURL = tmp;
		}
		return(true);
	}

	public String getGUIIndicator(String field,MMObjectNode node) {
		if (field.equals("url")) {
			String url=node.getStringValue("url");
			return("<A HREF=\""+url+"\" TARGET=\"extern\">"+url+"</A>");
		} else if (field.equals("id")) {
			return(""+node.getIntValue(field));
		} else if (field.equals("name")) {
			return(""+node.getStringValue(field));
		}
		return(null);
	}


	public String getJump(StringTokenizer tok) {
		String key = tok.nextToken();
		return(getJump(key));
	}

	public void delJumpCache(String key) {
		if (key!=null) {
			log.debug("Jumper builder - Removing "+key+" from jumper cache");
			jumpCache.remove(key);
		}
	}

											 
	public String getJump(String key) {
		String url = null;
		MMObjectNode node;
		int ikey;

		if (key.equals("")) {
			url=jumperNotFoundURL;
		} else {
			try {
				ikey=Integer.parseInt(key);
			} catch (NumberFormatException e) {
				ikey=-1;
			}
			url = (String)jumpCache.get(key);
			if (log.isDebugEnabled()) {
				if (url!=null) { 
					log.debug("Jumper - Cache hit on "+key);
				} else {
					log.debug("Jumper - Cache miss on "+key);
				}
			}
			if (url==null) {
				// Search jumpers with name;
				log.debug("Search jumpers with name="+key);
				//Enumeration res=search("WHERE name='"+key+"'");
				Enumeration res=search("name=='"+key+"'");
				if (res.hasMoreElements()) {
					node=(MMObjectNode)res.nextElement();	
					url=node.getStringValue("url");
				}
			}
			if (url==null) {
				// Search jumpers with number (parent);
				log.debug("Search jumpers with id="+ikey);
				if (ikey>=0) {
					Enumeration res=search("WHERE id="+ikey);
					if (res.hasMoreElements()) {
						node=(MMObjectNode)res.nextElement();	
						url=node.getStringValue("url");
					}
				}
			}
			if (url==null) {
				// no direct url call its builder
				if (ikey>=0) {
					node=getNode(ikey);
					if (node!=null) {
						String buln=mmb.getTypeDef().getValue(node.getIntValue("otype"));
						MMObjectBuilder bul=mmb.getMMObject(buln);
						log.debug("getUrl through builder with name="+buln+" and id "+ikey);
						if (bul!=null) url=bul.getDefaultUrl(ikey);
					}
				}
			}
			if (url!=null && url.length()>0 && !url.equals("null")) {
				jumpCache.put(key,url);
			} else {
				log.debug("No jumper found for key '"+key+"'");
				url=jumperNotFoundURL;
			}
		}
		return (url);		
	}


	public boolean nodeRemoteChanged(String number,String builder,String ctype) {
		super.nodeRemoteChanged(number,builder,ctype);
		return(nodeChanged(number,builder,ctype));
	}

	public boolean nodeLocalChanged(String number,String builder,String ctype) {
		super.nodeLocalChanged(number,builder,ctype);
		return(nodeChanged(number,builder,ctype));
	}

	public boolean nodeChanged(String number,String builder,String ctype) {
		log.debug("Jumpers="+builder+" no="+number+" "+ctype);
		jumpCache.clear();
		return(true);
	}
}
