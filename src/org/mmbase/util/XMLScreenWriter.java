/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;

import java.io.*;
import java.util.*;

import org.xml.sax.*;
import org.apache.xerces.parsers.*;
import org.w3c.dom.*;
import org.w3c.dom.traversal.*;

import org.mmbase.module.corebuilders.*;

/**
 * 
 * Writes XML as pretty printed HTML
 *
 * cjr@dds.nl 
 */
public class XMLScreenWriter  {

    Document document;
    DOMParser parser;

    static String tag_color = "#007700";
    static String attribute_color = "#DD0000";
    static String comment_color = "#FF8000";


    public XMLScreenWriter(String filename) {
        try {
            parser = new DOMParser();
            parser.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", true);
            parser.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
            //Errors errors = new Errors();
            //parser.setErrorHandler(errors);
            parser.parse(filename);
            document = parser.getDocument();

	} catch(Exception e) {
	    e.printStackTrace();
	}
    }

    public void write(PrintStream out) {
	write(out,document,-1);
    }

    public void write(PrintStream out, Node node, int level) {
	if (node != null) {
	    if (node.getNodeType() == node.COMMENT_NODE) {
		out.print(indent(level));
		out.println("<font color=\""+comment_color+"\">&lt;!--"+node.getNodeValue()+"--&gt;</font><br>");
	    } else if (node.getNodeType() == node.DOCUMENT_NODE) {
		NodeList nl = node.getChildNodes();
		for (int i=0; i < nl.getLength(); i++ ) {
		    write(out,nl.item(i),level+1);
		}
	    } else if (node.getNodeType() == node.TEXT_NODE) {
		out.println(node.getNodeValue());
	    } else {
		boolean is_end_node = isEndNode(node);
		NamedNodeMap nnm = node.getAttributes();
		if (nnm == null || nnm.getLength()==0) {
		    out.print(indent(level));
		    out.println("<font color=\""+tag_color+"\">&lt;"+node.getNodeName()+"&gt;</font>");
		} else {
		    out.print(indent(level));
		    out.print("<font color=\""+tag_color+"\">&lt;"+node.getNodeName()+"</font>");
		    for (int i=0; i < nnm.getLength(); i++) {
			Node attribute = nnm.item(i);
			out.print(" <font color=\""+attribute_color+"\">"+attribute.getNodeName()+"=\""+attribute.getNodeValue()+"\"</font>");			
			if (i < nnm.getLength()-1) {
			    out.print(" ");
			}
		    }
		    out.print("<font color=\""+tag_color+"\">&gt;</font>");
		}
		NodeList nl = node.getChildNodes();
		if (!is_end_node) {
		    out.println("<br>");
		}
		for (int i=0; i < nl.getLength(); i++ ) {
		    write(out,nl.item(i),level+1);
		}
		if (!is_end_node) {
		    out.print(indent(level));
		}
		out.println("<font color=\""+tag_color+"\">&lt;/"+node.getNodeName()+"&gt;</font><br>");
	    }
	}	
    }

    /**
     * @param level Indentation level
     * @return String of hard HTML spaces (&nbsp;) that are multiple of level
     */
    protected String indent(int level) {
	StringBuffer buf = new StringBuffer();
	for (int i=0; i < level; i++) {
	    buf.append("&nbsp;&nbsp;");
	}
	return buf.toString();
    }
    
    /**
     * @param node 
     * @return Whether the node contains only a text node, or possibly also an attribute node
     */
    protected boolean isEndNode(Node node) {
	int countTextNodes = 0;
	NodeList nl = node.getChildNodes();
	for (int i=0; i < nl.getLength(); i++ ) {
	    if (nl.item(i).getNodeType() == node.TEXT_NODE) {
		countTextNodes++;
	    } else if (nl.item(i).getNodeType() == node.ATTRIBUTE_NODE) {
		// do nothing
	    } else {
		return false;
	    }
	}
	return (countTextNodes >0);
    }
}
