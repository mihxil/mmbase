package org.mmbase.remote.builders;

import java.net.*;
import java.lang.*;
import java.io.*;
import java.util.*;

import org.mmbase.remote.*;
import org.mmbase.service.interfaces.*;
import org.mmbase.service.implementations.*;


/**
 * @version  3 Okt 1999
 * @author Daniel Ockeloen
 */
public class g2encoders extends RemoteBuilder {

	private	String	_classname 	= getClass().getName();
	private boolean _debug = true;
	private void debug( String msg ) { System.out.println( _classname + ":" + msg ); } 

	private g2encoderInterface impl;
	private String classname;
	StringTagger tagger;

	public void init(MMProtocolDriver con,String servicefile) {
		super.init(con,servicefile);
		// the node was loaded allready so check what the state was
		// and put us in ready/waiting state
		String state=getStringValue("state");
		getConfig();
		if (!state.equals("waiting")) {
			// maybe add 'what' happened code ? but for now
			// just put the service on waiting state
			setValue("state","waiting");
			commit();
		}
	}

	public void nodeRemoteChanged(String nodenr,String buildername,String ctype) {		
		nodeChanged(nodenr,buildername,ctype);
	}

	public void nodeLocalChanged(String nodenr,String buildername,String ctype) {		
		nodeChanged(nodenr,buildername,ctype);
	}

	public void nodeChanged(String nodenr,String buildername,String ctype) {		
		// get the node
		getNode();
				
		String state=getStringValue("state");
		debug("nodeChanged("+nodenr+","+buildername+","+ctype+"): state("+state+")");
		if (state.equals("version")) {
			doVersion();
		} else if (state.equals("encode")) {
			doEncode();
		}
	}

	private void doVersion() {
		setValue("state","busy");
		commit();
	
		// set the version we got from the encoder	
		if (impl!=null) {
			setValue("info",impl.getVersion());	
		} else {
			setValue("info","result=err reason=nocode");	
		}

		// signal that we are done
		setValue("state","waiting");
		commit();
		debug("doVersion(): commit done");
	}

	private void doEncode() {
		setValue("state","busy");
		commit();
	
		// set the version we got from the encoder	

		if (impl!=null) {
			String cmds=getStringValue("info");
			if( _debug ) debug("doEncode(): starting imp.doEncode("+cmds+")");
		    setValue("info",impl.doEncode(cmds));	
		} else {
			debug("doEncode(): ERROR: cannot encode! No implementation!");
			setValue("info","result=err reason=nocode");	
		}

		// signal that we are done
		setValue("state","waiting");
		commit();
	}

	void getConfig() {
		classname=(String)props.get("implementation");
		if( _debug ) debug("getConfig(): loading("+classname+")");
		try {
			Class newclass=Class.forName(classname);
			impl = (g2encoderInterface)newclass.newInstance();
		} catch (Exception f) {
			debug("getConfig(): ERROR: Can't load class("+classname+")");
		}
	}

}
