/*

VPRO (C)

This source file is part of mmbase and is (c) by VPRO until it is being
placed under opensource. This is a private copy ONLY to be used by the
MMBase partners.

*/
package org.mmbase.module.builders;

import java.util.*;
import java.sql.*;
import java.io.*;

import javax.servlet.http.*;
import org.mmbase.module.database.*;
import org.mmbase.module.core.*;
import org.mmbase.util.*;

/**
 * @author Daniel Ockeloen
 * @author David V van Zeventer
 * @version 25 Jan 1999
 */
public class pccarddrvs extends MMObjectBuilder implements MMBaseObserver {

	public final static String buildername = "pccarddrvs";

	public pccarddrvs() {
	}

	public boolean nodeRemoteChanged(String number, String builder, String ctype) {
		super.nodeRemoteChanged(number, builder, ctype);
		return (nodeChanged(number, builder, ctype));
	} 

	public boolean nodeLocalChanged(String number, String builder, String ctype) {
		super.nodeLocalChanged(number, builder, ctype);
		return (nodeChanged(number, builder, ctype));
	} 

	public boolean nodeChanged(String number, String builder, String ctype) {
		MMObjectNode node = getNode(number);
		if (node != null) {
			System.out.println("pccarddrvs node=" + node.toString());
			// is if for me ?, enum related mmservers to check
			Enumeration e = mmb.getInsRel().getRelated(number, "mmservers");
			while (e.hasMoreElements()) {
				MMObjectNode node2 = (MMObjectNode) e.nextElement();
				String wantedServer = node2.getStringValue("name");
				if (wantedServer.equals(mmb.getMachineName())) {
					doAction(node);
				}
			}
		}
		return (true);
	}

	/**
	* handles commands for pccarddrvs found in this machine
	*/
	private void doAction(MMObjectNode node) {
		String name = node.getStringValue("name");
		String cdtype = node.getStringValue("pccardtype");
		String state = node.getStringValue("state");
		String info = node.getStringValue("info");

		if (!state.equals("busy")) {
			System.out.println("Action called on pccarddrive : " + name);
			// start a thread to handle command
			new pccarddrvsProbe(this, node);
		} else {
			System.out.println("Problem action called on pccarddrive : " + name + " while it was busy");
		}
	}

	public Vector getList(HttpServletRequest req, StringTagger tagger, StringTokenizer tok) {
		if (tok.hasMoreTokens()) {
			String cmd = tok.nextToken();
			if (cmd.equals("getdir"))
				 return (getHTMLDir(tagger, tok));
		}
		return (null);
	}

	/**
	 * getHTMLDir: This method transforms the listing sothat it looks 'nice' inside a browser.
	 */
	public Vector getHTMLDir(StringTagger tagger, StringTokenizer tok) {
		Vector result = new Vector();
		String id = tagger.Value("NODE");
		MMObjectNode node = getNode(id);	//Get pccardnodenr determined by user via audiodevice? selector.

		if (node != null) {
			String info = (String) getValue(node, "getdir(info)");	//Call getValue to retrieve list.

			StringTokenizer info_st = new StringTokenizer(info, "\n\r");
			while (info_st.hasMoreTokens()) {	//Create vector from dir listing.

				result.addElement(info_st.nextToken());
			}
		}
		tagger.setValue("ITEMS", "1");
		return (result);
	}

	/**
	 * getDriveProps: This method retrieves the driveprops using the nodes' pccardtype field.
	 */
	private java.util.Properties getDriveProps(MMObjectNode pnode) throws DrivePropsNotFoundException {
		java.util.Properties driveprops = new java.util.Properties();
		String pccardtype = pnode.getStringValue("pccardtype");
		StringTagger tagger = new StringTagger(pccardtype);
		String OS = tagger.Value("OS");
		String FS = tagger.Value("FS");
		String DEV = tagger.Value("DEV");

		if (OS.equals("linux") && FS.equals("dosfs")) {		//have to be changed.

			driveprops.put("mountdir", DEV);
			driveprops.put("mountcmd", "/bin/mount");
			driveprops.put("unmountcmd", "/bin/umount");
			driveprops.put("dircmd", "/bin/ls -lpR");
			driveprops.put("copycmd", "/bin/cp");
			driveprops.put("mounterr", "is not a valid block device");
			driveprops.put("unmounterr", "not mounted");
			driveprops.put("direrr", "total 0");
/* no destination. cp: missing destination file Try `cp --help' for more information. */
/* directory doesn't exist. cp: cannot create regular file `tralali/cptest': No such file or directory */
			driveprops.put("copyerr", "cp: ");
			driveprops.put("copyerr2", "No such file or directory");
		} else {
			String jname = pnode.getStringValue("name");

			System.out.println(buildername + ": fillDriveProperties: pnode:" + jname + " has unknown pccardtype OS=" + OS + " , FS=" + FS);
			String errval = "pccardnode:" + jname + " has unknown pccardtype OS=" + OS + " , FS=" + FS;
			String explanation = "User hasn't specified pccardtype field correct.";
			throw new DrivePropsNotFoundException(errval, explanation);
		}
		return driveprops;
	}

	/**
	 * getDir: Returns a string containing all the filepaths from the pccarddisk.
	 *	 eg. /PCCARD-1/data/songs/primus/hitme.wav
	 *   		   /PCCARD-1/data/jingles/welcome.wav
	 * 
	 *  Note 26 jan 1999: See note in same method in jazzdrives.java
	 */
	public String getDir(MMObjectNode pnode) throws GetDirFailedException {
		String methodname = "getDir";
		Vector entries = new Vector();
		Runtime rt = null;
		 java.util.Properties driveprops = null;

		 System.out.println(buildername + ": " + methodname + ": Executing getDir method.");
		 driveprops = new java.util.Properties();	//Create properties object.

		 try {
			driveprops = getDriveProps(pnode);
		}
		catch(DrivePropsNotFoundException dnfe) {
			String Exc = "DrivePropsNotFoundException -> ";

			System.out.println(buildername + ": " + methodname + ": " + Exc + dnfe.errval + "  " + dnfe.explanation);
			String error_found = buildername + ": " + methodname + ": " + Exc + dnfe.errval;
			String explanation = dnfe.explanation;
			throw new GetDirFailedException(error_found, explanation);
		}

		String mountdir = driveprops.getProperty("mountdir");
		String mountcmd = driveprops.getProperty("mountcmd");
		String unmountcmd = driveprops.getProperty("unmountcmd");
		String dircmd = driveprops.getProperty("dircmd"+" "+mountdir);
		String mounterr = driveprops.getProperty("mounterr");
		String unmounterr = driveprops.getProperty("unmounterr");
		String direrr = driveprops.getProperty("direrr");

		try {
			rt = Runtime.getRuntime();
			try {
				doMount(mountcmd, mountdir, mounterr, rt);
				entries = doDir(dircmd, direrr, rt);
				doUnmount(unmountcmd, mountdir, unmounterr, rt);
			}
			catch(CmdFailedException cmdfe) {
				String Exc = "CmdFailedException -> ";

				System.out.println(buildername + ": " + methodname + ": " + Exc + cmdfe.errval + "  " + cmdfe.explanation);
				String error_found = buildername + ": " + methodname + ": " + Exc + cmdfe.errval;
				String explanation = cmdfe.explanation;
				throw new GetDirFailedException(error_found, explanation);
			}
			//System.out.println(buildername+": "+methodname+": Entries: "+entries.size());
			entries = alterEntries(entries, "remove whitespace", "", "");	// Remove whitespace

			entries = alterEntries(entries, "remove empty entries", "", "");	// Revove empty entries

			entries = alterEntries(entries, "remove ending on", "/", "");	// Remove ending on

			entries = alterEntries(entries, "add paths", "", "");	// Add paths
			//System.out.println(buildername+": "+methodname+": Entries: "+entries.size());

		}
		catch(IOException ioe) {
			String Exc = "IOException ->";

			System.out.println(buildername + ": " + methodname + ": " + Exc + ioe);
			String error_found = buildername + ": " + methodname + ": " + Exc + ioe;
			String explanation = "Something went wrong during IO on pccarddisk";
			throw new GetDirFailedException(error_found, explanation);
		}
		String list = new String();
		String drivename = pnode.getStringValue("name");

		for (Enumeration e = entries.elements(); e.hasMoreElements();) {
			list += "/" + drivename + (String) e.nextElement();
		}
		//System.out.println(buildername+": "+methodname+": Final list contains:"+list);
		return list;
	}

	/**
	 * copy : This method will copy the selected pccardfile (srcfile) to the destination (dstfile).
	 * 	  Only Files WITHOUT Spaces In Filename OR Directory Can be Copied.
	 * 	  srcfile eg.	= /PCCARD-1/audio/file.wav
	 *  	  dstfile eg.	= /data/audio/wav/123456.wav
	 *
	 *  Note 26 jan 1999: See note in same method in jazzdrives.java
	 */
	public void copy(String srcfile, String dstfile) throws CopyFailedException {
		String methodname = "copy";
		Runtime rt = null;
		String drivename = null;
		String srcfilepath = null, dstfilepath = null;
		 java.util.Properties driveprops = null;

		 System.out.println(buildername + ": " + methodname + ": Executing Copy method.");
		 System.out.println(buildername + ": " + methodname + ": srcfile -> " + srcfile);
		 System.out.println(buildername + ": " + methodname + ": dstfile -> " + dstfile);

		if (dstfile == null) {
			System.out.println(buildername + ": " + methodname + ": Dstfile:" + dstfile + " EMPTY.");
			String error_found = buildername + ": " + methodname + ": Dstfile:" + dstfile + " EMPTY.";
			String explanation = "Hmmmmm!!!?!?! no idea.";
			throw new CopyFailedException(error_found, explanation);
		}
		dstfilepath = dstfile;	//Destination file can be used immediately.

		String delim = "/";
		StringTokenizer tok = new StringTokenizer(srcfile, delim);

		if (tok.hasMoreTokens()) {
			drivename = tok.nextToken();	//Retrieve drivename

		} else {
			System.out.println(buildername + ": " + methodname + ": Srcfile:" + srcfile + " INVALID.");
			System.out.println("Srcfile is empty OR doesn't contain any " + delim + " delimiters.");
			String error_found = buildername + ": " + methodname + ": Srcfile:" + srcfile + " INVALID.";
			String explanation = "Srcfile is empty OR doesn't contain any " + delim + " delimiters.";
			throw new CopyFailedException(error_found, explanation);
		}
		try {
			MMObjectNode pnode = null;
			pccarddrvs bul = (pccarddrvs) mmb.getMMObject("pccarddrvs");
			Enumeration e = bul.search("WHERE name='" + drivename + "'");

			if (e.hasMoreElements()) {
				pnode = (MMObjectNode) e.nextElement();
			}
			driveprops = getDriveProps(pnode);
		}
		catch(DrivePropsNotFoundException dnfe) {
			String Exc = "DrivePropsNotFoundException -> ";

			System.out.println(buildername + ": " + methodname + ": " + Exc + dnfe.errval + "  " + dnfe.explanation);
			String error_found = buildername + ": " + methodname + ": " + Exc + dnfe.errval;
			String explanation = dnfe.explanation;
			throw new CopyFailedException(error_found, explanation);
		}

		String mountdir = driveprops.getProperty("mountdir");
		String mountcmd = driveprops.getProperty("mountcmd");
		String copycmd = driveprops.getProperty("copycmd");
		String unmountcmd = driveprops.getProperty("unmountcmd");
		String mounterr = driveprops.getProperty("mounterr");
		String unmounterr = driveprops.getProperty("unmounterr");
		String copyerr = driveprops.getProperty("copyerr");
		String copyerr2 = driveprops.getProperty("copyerr2");

		int startPos = delim.length() + drivename.length();

		srcfilepath = srcfile.substring(startPos);	//Only use part following the drivename.
		//System.out.println(buildername+": "+methodname+": Cutting of drivename    srcfilepath -> "+srcfilepath);

		//System.out.println(buildername+": "+methodname+": srcfilepath -> "+srcfilepath);
		//System.out.println(buildername+": "+methodname+": dstfilepath -> "+dstfilepath);
		try {
			rt = Runtime.getRuntime();
			try {
				doMount(mountcmd, mountdir, mounterr, rt);
				doCopy(copycmd, copyerr, copyerr2, srcfilepath, dstfilepath, rt);
				doUnmount(unmountcmd, mountdir, unmounterr, rt);
			} catch(CmdFailedException cmdfe) {
				String Exc = "CmdFailedException -> ";

				System.out.println(buildername + ": " + methodname + ": " + Exc + cmdfe.errval + "  " + cmdfe.explanation);
				String error_found = buildername + ": " + methodname + ": " + Exc + cmdfe.errval;
				String explanation = cmdfe.explanation;
				throw new CopyFailedException(error_found, explanation);
			}
		} catch(IOException ioe) {
			String Exc = "IOException ->";

			System.out.println(buildername + ": " + methodname + ": " + ioe);
			String error_found = buildername + ": " + methodname + ": " + Exc + ioe;
			String explanation = "Something went wrong during IO on pccarddisk";
			throw new CopyFailedException(error_found, explanation);
		}
	}

	/**
	 * findError: This method compares the 'errval with 'known_error'
	 * 		  retval 		 = 'known_error' if 2 args are equal 
	 *		  else retval	= 'unknown_error'
	 */
	private String findError(String errval, String cmd, String known_error) {
		String methodname = "findError";
		String stripped = null;
		String unknown_error = "Unknown error -->";

		 System.out.println(buildername + ": " + methodname + ": Finding error.");
		 stripped = Strip.Whitespace(errval, errval.length() - 3);	//Go back 3 chars and strip whitespace.

		if (stripped.endsWith(known_error)) {	//If the error value returned is known to me.

			System.out.println(buildername + ": " + methodname + ": I know this error.");
		} else {
			System.out.println(buildername + ": " + methodname + ": Unknown error.");
			stripped = unknown_error + stripped;
		}
		return stripped;
	}

	/**
	 * doMount: This method performs the mount command.
	 *
	 *  Note 26 jan 1999: See note in same method in jazzdrives.java
	 */
	private void doMount(String mountcmd, String mountdir, String mounterr, Runtime rt) throws CmdFailedException, IOException {
		String methodname = "doMount";
		String volname = null;
		String retval = null, errval = null;
		String error_found = null, explanation = null;
		String unknown_error = "Unknown error -->";

		 System.out.println(buildername + ": " + methodname + ": Now executing " + mountcmd + " " + mountdir);
		Process p = rt.exec(mountcmd + " " + mountdir);		//hmount /dev/sda

		DataInputStream in = new DataInputStream(p.getInputStream());
		DataInputStream err = new DataInputStream(p.getErrorStream());

		while ((retval = in.readLine()) != null) {
			//System.out.println(buildername+": "+methodname+": retval = "+retval);
			if (retval.startsWith("Volume name is")) {
				StringTokenizer tok = new StringTokenizer(retval, "\"");
				if (tok.hasMoreTokens()) {
					volname = tok.nextToken();
					volname = tok.nextToken();	//volname now contains volumename.
					//System.out.println(buildername+": "+methodname+": Volumename retrieved: "+volname);

				}
			}
		} while ((errval = err.readLine()) != null) {
			System.out.println(buildername + ": " + methodname + ": errval = " + errval);
			error_found = findError(errval, mountcmd, mounterr);
			if (!error_found.startsWith(unknown_error)) {
				explanation = "This means that there's probably no pccarddisk in drive";
			} else {
				explanation = "Something went seriously wrong.";
			}
			throw new CmdFailedException(error_found, explanation);
		}
	}

	/**
	 * doUnMount: This method performs the unmount command.
	 *
	 *  Note 26 jan 1999: See note in same method in jazzdrives.java
	 */
	private void doUnmount(String unmountcmd, String mountdir, String unmounterr, Runtime rt) throws CmdFailedException, IOException {
		String methodname = "doUnmount";
		String retval = null, errval = null;
		String error_found = null, explanation = null;
		String unknown_error = "Unknown error -->";

		 System.out.println(buildername + ": " + methodname + ": Now executing " + unmountcmd + " " + mountdir);
		Process p = rt.exec(unmountcmd + " " + mountdir);	//humount /dev/sda

		DataInputStream in = new DataInputStream(p.getInputStream());
		DataInputStream err = new DataInputStream(p.getErrorStream());

		while ((retval = in.readLine()) != null) {
			System.out.println(buildername + ": " + methodname + ": retval = " + retval);
		} while ((errval = err.readLine()) != null) {
			System.out.println(buildername + ": " + methodname + ": errval = " + errval);
			error_found = findError(errval, unmountcmd, unmounterr);
			if (!error_found.startsWith(unknown_error)) {
				explanation = "This probably means that there's been a pccarddisk-change during session.";
			} else {
				explanation = "Something went seriously wrong.";
			}
			throw new CmdFailedException(error_found, explanation);
		}
	}

	/**
	 * doDir: This method performs the dir command and returns a vector with filepaths.
	 *
	 *  Note 26 jan 1999: See note in same method in jazzdrives.java
	 */
	private Vector doDir(String dircmd, String direrr, Runtime rt) throws CmdFailedException, IOException {
		String methodname = "doDir";
		Vector entries = new Vector();
		String entry = null, errval = null;
		String error_found = null, explanation = null;
		String unknown_error = "Unknown error -->";

		 System.out.println(buildername + ": " + methodname + ": Now executing " + dircmd);
		Process p = rt.exec(dircmd);	//eg ls -1pR

		DataInputStream in = new DataInputStream(p.getInputStream());
		DataInputStream err = new DataInputStream(p.getErrorStream());

		//Add all entries to a vector
		 System.out.println(buildername + ": " + methodname + ": Adding all entries to a vector.");
		while ((entry = in.readLine()) != null) {
			//System.out.println(buildername+": "+methodname+": entry = "+entry);
			entries.addElement(entry);
		} while ((errval = err.readLine()) != null) {
			System.out.println(buildername + ": " + methodname + ": errval = " + errval);
			error_found = findError(errval, dircmd, direrr);
			if (!error_found.startsWith(unknown_error)) {;
				explanation = "This probably means that there's been a pccarddisk change during session OR no disk in drive at all.";
			} else {
				explanation = "Something went seriously wrong.";
			}
			throw new CmdFailedException(error_found, explanation);
		}
		return entries;
	}

	/**
	 * doCopy: This method performs the actual copying, by using a copycommand outside java (copy).
	 * 		Only Files WITHOUT Spaces In Filename OR Directory Can be Copied.
	 * 		The srcfilepath = :audio:jingles:boing.wav
	 * 		The dstfilepath = /data/import/123456.wav
	 *
	 *  Note 26 jan 1999: See note in same method in jazzdrives.java
	 */
	private void doCopy(String copycmd, String copyerr, String copyerr2, String srcfilepath, String dstfilepath, Runtime rt) throws CmdFailedException, IOException {
		String methodname = "doCopy";
		String retval = null, errval = null;
		String error_found = null, explanation = null;
		String unknown_error = "Unknown error -->";
		boolean error_occured = false;

//		TESt
		dstfilepath="/tmp";
		System.out.println(buildername+": "+methodname+": Now executing "+copycmd+" "+srcfilepath+" "+dstfilepath);
		Process p = rt.exec(copycmd+" "+srcfilepath+" "+dstfilepath);

		DataInputStream in = new DataInputStream(p.getInputStream());
		DataInputStream err = new DataInputStream(p.getErrorStream());

		while ((retval = in.readLine()) != null) {	//Get data from stdin.

			System.out.println(buildername + ": " + methodname + ": stdin retval = " + retval);
		} while ((errval = err.readLine()) != null) {	//Get data from stderr.

			error_occured = true;
			System.out.println(buildername + ": " + methodname + ": errval = " + errval);
			error_found = findError(errval, copycmd, copyerr);
			if (!error_found.startsWith(unknown_error)) {
				explanation = "This means that there's an error in the copy command syntax.";
			} else {
				error_found = findError(errval, copycmd, copyerr2);		//Check for another error possibility.

				if (!error_found.startsWith(unknown_error)) {
					explanation = "This means that the destination doesn't exist.";
				} else {
					explanation = "Something went seriously wrong.";
				}
			}
		}
		if (error_occured) {
			throw new CmdFailedException(error_found, explanation);
		}
		System.out.println(buildername + ": " + methodname + ": Copy done.");
	}

	/**
	 * alterEntries: This method contains several code to alter a vector with string entries.
	 */
	public Vector alterEntries(Vector entries, String cmd, String param1, String param2) {
		String methodname = "alterEntries";
		Vector altered = new Vector();

		if (cmd.equals("remove whitespace")) {
			//System.out.println(buildername+": "+methodname+": Removing whitespace from entries.");
			for (Enumeration e = entries.elements(); e.hasMoreElements();) {
				String s = (String) e.nextElement();
				 altered.addElement(Strip.Whitespace(s, 0));
			}
		} else if (cmd.equals("remove starting with")) {
			//System.out.println(buildername+": "+methodname+": Removing entries starting with a "+param1);
			for (Enumeration e = entries.elements(); e.hasMoreElements();) {
				String s = (String) e.nextElement();

				if (!s.startsWith(param1)) {	//Only add file entries.

					altered.addElement(s);
				} else {
					//System.out.println(buildername+": "+methodname+": Removing element "+s);
				}
			}
		} else if (cmd.equals("remove ending on")) {
			//System.out.println(buildername+": "+methodname+": Removing entries ending on a "+param1);
			for (Enumeration e = entries.elements(); e.hasMoreElements();) {
				String s = (String) e.nextElement();

				if (!s.endsWith(param1)) {	//Only add file entries.

					altered.addElement(s);
				} else {
					//System.out.println(buildername+": "+methodname+": Removing element "+s);
				}
			}
		} else if (cmd.equals("remove empty entries")) {
			//System.out.println(buildername+": "+methodname+": Removing empty entries.");
			for (Enumeration e = entries.elements(); e.hasMoreElements();) {
				String s = (String) e.nextElement();

				if (s != null) {
					altered.addElement(s);
				} else {
					//System.out.println(buildername+": "+methodname+": Removing empty entry.");
				}
			}
		} else if (cmd.equals("keep token")) {
			//System.out.println(buildername+": "+methodname+": Keeping "+param1+" token only.");
			if (param1.equals("last")) {
				for (Enumeration e = entries.elements(); e.hasMoreElements();) {
					String s = (String) e.nextElement();
					String token = null;
					StringTokenizer entry_st = new StringTokenizer(s, " ");

					while (entry_st.hasMoreTokens()) {
						token = entry_st.nextToken();
						if (token.startsWith(":")) {
							while (entry_st.hasMoreTokens()) {
								token += " " + entry_st.nextToken();
							}
							break;
						} else if (token.startsWith("f")) {
							int tokens = 7;
							int i = 0;

							while (i < tokens && entry_st.hasMoreTokens()) {	//Go to the right token.

								token = entry_st.nextToken();
								//System.out.println("i:"+i+" token: "+token);
								i++;
							}
							while (entry_st.hasMoreTokens()) {	//Needed if filename contains spaces.

								token += " " + entry_st.nextToken();
							}
							break;
						}
					}
					//System.out.println(buildername+": "+methodname+": Adding: "+token);
					altered.addElement(token);
				}
			}
		} else if (cmd.equals("add paths")) {
			//System.out.println(buildername+": "+methodname+": Adding paths to entries.");
			String path = null;

			for (Enumeration e = entries.elements(); e.hasMoreElements();) {
				String s = (String) e.nextElement();

				if (s.startsWith(".")) {
					path = new String(s);
				} else {
					StringBuffer sb = new StringBuffer(s);

					sb.insert(0, path);		//insert /bla/ to a.wav

					sb.append("\n\r");	//Only used for layout while testing.

					altered.addElement(new String(sb));
				}
			}
		} else if (cmd.equals("replace")) {
			//System.out.println(buildername+": "+methodname+": Replacing all "+param1+" with "+param2);
			for (Enumeration e = entries.elements(); e.hasMoreElements();) {
				String s = (String) e.nextElement();

				altered.addElement(s.replace(param1.charAt(0), param2.charAt(0)));
			}
		}
		return altered;
	}

	public Object getValue(MMObjectNode node, String field) {
		if (field.equals("getdir(info)")) {
			// send the command to get the dir
			node.setValue("state", "getdir");
			node.commit();
			boolean changed = false;
			MMObjectNode newnode = null;
			while (!changed) {
				waitUntilNodeChanged(node);
				newnode = getNode(node.getIntValue("number"));
				String state = newnode.getStringValue("state");
				 System.out.println("WAIT RESULT=" + state);
				if (state.equals("waiting") || state.equals("error"))
					 changed = true;
			} String val = newnode.getStringValue("info");

			System.out.println(buildername + ": getValue: val=" + val);
			return (val);
		}
		return (null);
	}
}
