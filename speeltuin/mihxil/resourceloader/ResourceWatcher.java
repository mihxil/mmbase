/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.util;

import java.io.File;
import java.net.URL;
import java.util.*;


import org.mmbase.module.core.MMBaseObserver;
import org.mmbase.module.core.MMObjectNode;
import org.mmbase.module.core.MMBase;
import org.mmbase.module.builders.Resources;

import org.mmbase.util.logging.*;

/**
 *  Like {@link #FileWatcher} but for Resources. If (one of the) file(s) to which the resource resolves
 *  to is added or changed, it onChange will be triggered, if not a 'more important' wil was
 *  existing already. If a file is removed, and was the most important one, it will be removed from the filewatcher. 
 *
 * @author Michiel Meeuwissen
 * @since  MMBase-1.8
 * @version $Id: ResourceWatcher.java,v 1.5 2004-10-12 21:19:58 michiel Exp $
 * @see    FileWatcher
 * @see    ResourceLoader
 */
public abstract class ResourceWatcher implements MMBaseObserver {
    private static final Logger log = Logging.getLoggerInstance(ResourceWatcher.class);

    /**
     * All instantiated ResourceWatchers. Only used until setResourceBuilder is called. Then it
     * is set to null, and not used any more (also used in ResourceLoader).
     * 
     */
    static  Set resourceWatchers = new HashSet();

    /**
     * Considers all resource-watchers. Perhaps onChange must be called, because there is node for this resource available now.
     */
    static void setResourceBuilder() {
        Iterator i = resourceWatchers.iterator();
        while (i.hasNext()) {
            ResourceWatcher rw = (ResourceWatcher) i.next();
            if (rw.running) {
                rw.observe();
            }
            Iterator j = rw.resources.iterator();
            while (j.hasNext()) {
                String resource = (String) j.next();
                if (rw.mapNodeNumber(rw.resourceLoader.getResolver(resource), resource)) {
                    log.service("ResourceBuilder is available now. Resource " + resource + " must be reloaded.");
                    rw.onChange(resource);

                }                
            }
        }
        resourceWatchers = null; // no need to store those any more.
    }

    /**
     * Delay setting used for the filewatchers.
     */
    private long delay = -1;

    /**
     * All resources watched by this ResourceWatcher. A Set of Strings. Often, a ResourceWatcher would watch only one resource.
     */
    protected SortedSet resources = new TreeSet();

    /**
     * When a resource is loaded from a Node, we must node which Nodes correspond to which
     * resource. You could ask the node itself, but if the node happens to be deleted, then you
     * can't know that any more. Used in {@link #nodeChanged}
     */
    protected Map       nodeNumberToResourceName = new HashMap();

    /**
     * Whether this ResourceWatchers has been started (see {@link #start}
     */
    private boolean running = false;

    /**
     * Wrapped FileWatcher for watching the file-resources. ResourceName -> FileWatcher.
     */
    protected Map fileWatchers = new HashMap();

    /**
     * The resource-loader associated with this ResourceWatcher.
     */
    protected ResourceLoader resourceLoader;


    /** 
     * Constructor.
     */
    protected ResourceWatcher(ResourceLoader rl) {
        resourceLoader = rl;
        if (resourceWatchers != null) {
            resourceWatchers.add(this);
        }
    }
    /** 
     * Constructor, defaulting to the Root ResourceLoader (see {@link ResourceLoader#getRoot}).
     */
    protected ResourceWatcher() {
        this(ResourceLoader.getRoot());
    }


    /**
     * {@inheritDoc} 
     */
    public boolean nodeRemoteChanged(String machine, String number, String builder, String ctype) {
        return nodeChanged(number, ctype);
    }
    /**
     * {@inheritDoc} 
     */
    public boolean nodeLocalChanged(String machine, String number, String builder, String ctype) {
        return nodeChanged(number, ctype);
    }

    /**
     * If a node (of the type 'resourceBuilder' changes, checked if it is a node belonging to one of the resource of this resource-watcher.
     * If so, {@link #onChange} is called.
     */
    protected boolean nodeChanged(String number, String ctype) {       
        try {
            if (ctype.equals("d")) { 
                // hard..
                String name = (String) nodeNumberToResourceName.get(number);
                if (name != null && resources.contains(name)) {
                    nodeNumberToResourceName.remove(number);
                    log.service("Resource " + name + " changed (node removed)");
                    onChange(name);
                }
            } else {
                MMObjectNode node = ResourceLoader.resourceBuilder.getNode(number);
                String name = node.getStringValue(Resources.RESOURCENAME_FIELD);
                if (resources.contains(name)) {
                    if (ctype.equals("n")) {
                        log.service("Resource " + name + " changed (node added)");
                        nodeNumberToResourceName.put(number, name);
                        return true; // TODO, Should not return here.
                        //WHY DO I GET A N AND A C? Further more, the onChange after N sometimes leads to issue #6602 (perhaps because it's earlier?)

                    } else {
                        log.service("Resource " + name + " changed (node changed)");
                    }
                    onChange(name);
                }
            }
        } catch (Exception e) {
            log.error(e);
        }

        return true;
    }


    /**
     * @return Unmodifiable set of String of watched resources 
     */
    public Set getResources() {
        return Collections.unmodifiableSortedSet(resources);
    }

    /**
     * The associated ResourceLoader
     */
    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    /**
     *
     * @param resourceName The resource to be monitored.
     */
    public synchronized void add(String resourceName) { 
        resources.add(resourceName);
        if (running) {
            createFileWatcher(resourceName);
            ResourceLoader.Resolver resolver = resourceLoader.getResolver(resourceName);
            mapNodeNumber(resolver, resourceName);
        }
    }

    /**
     * When a resource is added to this ResourceWatcher, this method is called to create a
     * FileWatcher, and add all Files associated with the resource to it.
     */
    protected synchronized void createFileWatcher(String resource) {
        FileWatcher fileWatcher = new ResourceFileWatcher(resource);
        if (delay != -1) {
            fileWatcher.setDelay(delay);
        }
        fileWatcher.getFiles().addAll(resourceLoader.getFiles(resource));
        fileWatcher.start(); // filewatchers are only created on start, so must always be started themselves.
        fileWatchers.put(resource, fileWatcher);
    }

    /**
     * When a resource is added to this ResourceWatcher, this method is called to check wheter a
     * ResourceBuilder node is assocated with this resource. If so, this methods maps the number of
     * the node to the resource name. This is needed in {@link #nodeChanged} in case of a
     * node-deletion.
     * @return Whether a Node as found to map.
     */
    protected synchronized boolean mapNodeNumber(ResourceLoader.Resolver resolver, String resource) {
        MMObjectNode node = resolver.getResourceNode();
        if (node != null) {
            nodeNumberToResourceName.put("" + node.getNumber(), resource);
            return true;
        } else {
            return false;
        }
            
    }

    /**
     * Add this MMBaseObserver/ResourceWatcher as an observer of the resource-builder.
     */
    protected synchronized void observe() {
        if (ResourceLoader.resourceBuilder != null) {
            ResourceLoader.resourceBuilder.addLocalObserver(this);
            ResourceLoader.resourceBuilder.addRemoteObserver(this);
        } else {
            log.debug("No rseource-builder to register to.");
        }
    }


    public synchronized void start() {
        // create and start all filewatchers.
        Iterator i = resources.iterator();
        while (i.hasNext()) {
            String resource = (String) i.next();
            ResourceLoader.Resolver resolver = resourceLoader.getResolver(resource);
            resolver.checkShadowedNewerResources();
            mapNodeNumber(resolver, resource);
            createFileWatcher(resource);     

        }

        observe();

        running = true;
    }


    /**
     * Put here the stuff that has to be executed, when a file has been changed.
     * @param resourceName The resource that was changed.
     */
    abstract public void onChange(String resourceName);

    /**
     * Set the delay to observe between each check of the file changes.
     */
    public synchronized void setDelay(long delay) {
        this.delay = delay;
        Iterator i = fileWatchers.values().iterator();
        while (i.hasNext()) {
            FileWatcher fw = (FileWatcher) i.next();
            fw.setDelay(delay);
        }
    }


    /**
     */
    public synchronized void remove(String resourceName) {
        boolean wasRunning = running;
        if (running) { // it's simplest like this.
            exit();
        }
        resources.remove(resourceName);
        if (wasRunning) {
            start();
        }
    }

    /**
     * Removes all resources. 
     */
    public synchronized  void clear() {
        if (running) {
            exit();
            resources.clear();
            start();
        } else {
            resources.clear();
        }
    }

    /**
     * Completely restarts this ResourceWatcher.
     */
    protected synchronized void restart() {
        if (running) {
            log.service("Restarting " + this);
            exit();
        }
        start();
    }

    /**
     * Stops watching. Stops all filewatchers, removes observers.
     */
    public synchronized void exit() {
        Iterator i = fileWatchers.values().iterator();
        while (i.hasNext()) {
            FileWatcher fw = (FileWatcher) i.next();
            fw.exit();
            i.remove();
        }
        if (ResourceLoader.resourceBuilder != null) {
            ResourceLoader.resourceBuilder.removeLocalObserver(this);
            ResourceLoader.resourceBuilder.removeRemoteObserver(this);
        } 
        running = false;
    }


    /**
     * Shows the 'contents' of the filewatcher. It shows a list of files/last modified timestamps.
     */
    public String toString() {
        return "" + resources + " " + fileWatchers;
    }


    /**
     * A FileWatcher associated with a certain resource of this ResourceWatcher.
     */

    protected class ResourceFileWatcher extends FileWatcher {
        private String resource;
        ResourceFileWatcher(String resource) {
            this.resource = resource;
        }
        protected void onChange(File f) {
            URL shadower = ResourceWatcher.this.resourceLoader.getResolver(resource).shadowed(f);
            if (shadower == null) {
                ResourceWatcher.this.onChange(resource);
            } else {
                log.warn("File " + f + " changed, but it is shadowed by " + shadower);
            }
        }
    }
            
 
}
