/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.cache;

import java.util.*;
import java.io.File;
import org.mmbase.util.LRUHashtable;
import org.mmbase.module.core.MMBaseContext;

import org.mmbase.util.XMLBasicReader;
import org.w3c.dom.Element;

import org.mmbase.util.FileWatcher;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * A base class for all Caches. Extend this class for other caches.  
 *
 * @author Michiel Meeuwissen
 * @version $Id: Cache.java,v 1.2 2002-05-15 16:45:33 michiel Exp $
 */
abstract public class Cache extends LRUHashtable {

    private static Logger log = Logging.getLoggerInstance(Cache.class.getName());
    private static Map caches = new Hashtable();
   
    /**
     * Configures the caches using a config File. There is only one
     * config file now so the argument is al little overdone, but it
     * doesn't harm.
     */

    private static void configure(File file) {
        configure(file, null);
    }
    /**
     * As configure, but it only changes the configuration of the cache 'only'.
     * This is called on first use of a cache.
     */
    private static void configure(File file, String only) {
        if (only == null) log.info("Configuring caches with " + file);
        if (! file.exists()) {
            return; // nothing can be done...
        }
        XMLBasicReader xmlReader = new XMLBasicReader(file.getAbsolutePath());
        Enumeration e =  xmlReader.getChildElements("caches", "cache");
        while (e.hasMoreElements()) {           
            Element cacheElement = (Element) e.nextElement();
            String cacheName =  cacheElement.getAttribute("name");
            if (only != null && ! only.equals(cacheName)) {
                continue;
            }
            Cache cache = getCache(cacheName);
            if (cache == null) {
                log.service("No cache " + cacheName + " is present (perhaps not used yet?)");
            } else {
                String status = xmlReader.getElementValue(xmlReader.getElementByPath(cacheElement, "cache.status"));
                cache.setActive(status.equalsIgnoreCase("active"));
                try {
                    Integer size = new Integer(xmlReader.getElementValue(xmlReader.getElementByPath(cacheElement, "cache.size")));
                    cache.setSize(size.intValue());
                    log.service("Setting " + cacheName + " " + status + " with size " + size);
                } catch (NumberFormatException nfe) {
                    log.error("Could not configure cache " + cacheName + " because the size was wrong: " + nfe.toString());
                }
            }
        }        
    }


    /** 
    * The caches can be configured with an XML file, this file can
    * be changed which causes the caches to be reconfigured automaticly.
    */
    private static FileWatcher configWatcher = new FileWatcher (true) {
            protected void onChange(File file) {
                configure(file);
            }
        };

    private static File configFile;
    static { // configure
        log.debug("Static init of Caches");
        configFile = new File(MMBaseContext.getConfigPath() + File.separator + "caches.xml");
        if (configFile.exists()) {
            configWatcher.add(configFile);
            // configure(configFile); never mind, no cache are present on start up
        } else {
            log.warn("No cache configuration file " + configFile + " found");
        }
        configWatcher.setDelay(10 * 1000); // check every 10 secs if config changed
        configWatcher.start();

    }


    private boolean active = true;


    public Cache(int size) {
        super(size);
        log.service("Creating cache " + getName() + ": " + getDescription());
    }

    /**
     * Returns a name for this cache type. Default it is the class
     * name, but this normally will be overriden.
     */
    public String getName() {
        return getClass().getName();
    }
    /**
     * Gives a description for this cache type. This can be used in
     * cache overviews.
     */
    public String getDescription() {
        return "An all purpose Cache";
    }

    /**
     * Puts a cache in the caches repository. This function will be
     * called in the static of childs, therefore it is protected.
     *
     * @param A cache.
     * @return The previous cache of the same type (stored under the same name)
     */
    protected static Cache putCache(Cache cache) {
        Cache old = (Cache) caches.put(cache.getName(), cache);
        configure(configFile, cache.getName());
        return old;
    }

    /**
     * Returns the Cache with a certain name. To be used in combination with getCaches().
     *
     * @see getCaches
     */
    public static Cache getCache(String name) {
        return (Cache) caches.get(name);
    }

    /**
     * Returns the names of all caches.
     *
     * @return A Set containing the names of all caches.
     */
    public static Set getCaches() {
        return caches.keySet();
    }

    /**
     * Like 'get' of LRUHashtable but considers if the cache is active or not.
     *
     */
    public synchronized Object get(Object key) {
        if (! active) return null;
        return super.get(key);
    }

    /**
     * Like 'put' of LRUHashtable but considers if the cache is active or not.
     *
     */
    public synchronized Object put(Object key, Object value) {
        if (! active) return null;
        return super.put(key, value);
    }

    /**
     * Sets this cache to active or passive.
     * TODO: Writing back to caches.xml if necessary (if this call was nog caused by change of caches.xml itself)
     */
    public void setActive(boolean a) {
        active = a;
    }

    /**
     * Wether this cache is active or not.
     */
    public boolean isActive() {
        return active;
    }
}
