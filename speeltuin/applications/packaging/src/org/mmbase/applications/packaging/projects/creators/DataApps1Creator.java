/*
 *  This software is OSI Certified Open Source Software.
 *  OSI Certified is a certification mark of the Open Source Initiative.
 *  The license (Mozilla version 1.0) can be read at the MMBase site.
 *  See http://www.MMBase.org/license
 */
package org.mmbase.applications.packaging.projects.creators;

import org.mmbase.bridge.*;
import org.mmbase.module.core.*;
import org.mmbase.util.logging.*;
import org.mmbase.util.*;
import org.mmbase.module.builders.Versions;
import org.mmbase.applications.packaging.*;
import org.mmbase.applications.packaging.projects.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

import org.w3c.dom.*;

/**
 * DisplayHtmlPackage, Handler for html packages
 *
 * @author     Daniel Ockeloen (MMBased)
 * @created    July 20, 2004
 */
public class DataApps1Creator extends BasicCreator implements CreatorInterface {

    private static Logger log = Logging.getLoggerInstance(DataApps1Creator.class);

    /**
     *  Description of the Field
     */
    public final static String DTD_PACKAGING_DATA_APPS1_1_0 = "packaging_data_apps1_1_0.dtd";
    /**
     *  Description of the Field
     */
    public final static String PUBLIC_ID_PACKAGING_DATA_APPS1_1_0 = "-//MMBase//DTD packaging_data_apps1 config 1.0//EN";


    /**
     *  Description of the Method
     */
    public static void registerPublicIDs() {
        XMLEntityResolver.registerPublicID(PUBLIC_ID_PACKAGING_DATA_APPS1_1_0, "DTD_PACKAGING_DATA_APPS1_1_0", DataApps1Creator.class);
    }


    /**
     *Constructor for the DataApps1Creator object
     */
    public DataApps1Creator() {
        cl = DataApps1Creator.class;
        prefix = "packaging_data_apps1";
    }


    /**
     *  Description of the Method
     *
     * @param  target      Description of the Parameter
     * @param  newversion  Description of the Parameter
     * @return             Description of the Return Value
     */
    public boolean createPackage(Target target, int newversion) {

        clearPackageSteps();

        // step1
        packageStep step = getNextPackageStep();
        step.setUserFeedBack("data/apps1 packager started");

        String datafile = target.getBaseDir() + getItemStringValue(target, "datafile");
        String datadir = target.getBaseDir() + getItemStringValue(target, "datadir");

        step = getNextPackageStep();
        step.setUserFeedBack("used datafile : " + datafile);
        step = getNextPackageStep();
        step.setUserFeedBack("used datadir : " + datadir);

        String newfilename = getBuildPath() + getName(target).replace(' ', '_') + "@" + getMaintainer(target) + "_data_apps1_" + newversion;
        try {
            JarOutputStream jarfile = new JarOutputStream(new FileOutputStream(newfilename + ".tmp"), new Manifest());

            step = getNextPackageStep();
            step.setUserFeedBack("creating package.xml file...");
            createPackageMetaFile(jarfile, target, newversion);
            step.setUserFeedBack("creating package.xml file...done");
            step = getNextPackageStep();
            step.setUserFeedBack("creating depends.xml file...");
            createDependsMetaFile(jarfile, target);
            step.setUserFeedBack("creating depends.xml file...done");

            addFile(jarfile, datafile, "data.xml", "data", "");

            addFiles(jarfile, datadir, ".xml", "", "data", "data");
            addFiles(jarfile, datadir, ".handle", "", "data", "data");
            jarfile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        step = getNextPackageStep();
        step.setUserFeedBack("data/apps1 packager ended : " + getErrorCount() + " errors and " + getWarningCount() + " warnings");

        // update the build file to reflect the last build, should only be done if no errors
        if (getErrorCount() == 0) {
            File f1 = new File(newfilename + ".tmp");
            File f2 = new File(newfilename + ".mmp");
            if (f1.renameTo(f2)) {
                updatePackageTime(target, new Date(), newversion);
                target.save();
            }
        }
        return true;
    }


    /**
     *  Description of the Method
     *
     * @param  target  Description of the Parameter
     * @return         Description of the Return Value
     */
    public boolean decodeItems(Target target) {
        super.decodeItems(target);
        decodeStringItem(target, "datafile");
        decodeStringItem(target, "datadir");
        return true;
    }


    /**
     *  Gets the xMLFile attribute of the DataApps1Creator object
     *
     * @param  target  Description of the Parameter
     * @return         The xMLFile value
     */
    public String getXMLFile(Target target) {
        String body = getDefaultXMLHeader(target);
        body += getDefaultXMLMetaInfo(target);
        body += "\t<datafile>" + getItemStringValue(target, "datafile") + "</datafile>\n";
        body += "\t<datadir>" + getItemStringValue(target, "datadir") + "</datadir>\n";
        body += getPackageDependsXML(target);
        body += getRelatedPeopleXML("initiators", "initiator", target);
        body += getRelatedPeopleXML("supporters", "supporter", target);
        body += getRelatedPeopleXML("developers", "developer", target);
        body += getRelatedPeopleXML("contacts", "contact", target);
        body += getDefaultXMLFooter(target);
        return body;
    }


    /**
     *  Sets the defaults attribute of the DataApps1Creator object
     *
     * @param  target  The new defaults value
     */
    public void setDefaults(Target target) {
        target.setItem("datafile", "datasets/example/data.xml");
        target.setItem("datadir", "datasets/example/data/");
    }
}

