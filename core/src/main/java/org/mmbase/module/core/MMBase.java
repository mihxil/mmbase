/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.core;

import java.io.File;
import java.util.*;
import java.text.DateFormat;
import javax.naming.*;
import javax.sql.DataSource;
import javax.servlet.ServletContext;

import org.mmbase.core.event.*;
import org.mmbase.datatypes.DataTypes;
import org.mmbase.module.ProcessorModule;
import org.mmbase.module.builders.Versions;
import org.mmbase.module.corebuilders.*;
import org.mmbase.security.MMBaseCop;
import org.mmbase.storage.*;
import org.mmbase.storage.search.RelationStep;
import org.mmbase.storage.search.CoreSearchQueryHandler;
import org.mmbase.util.ResourceLoader;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.xml.BuilderReader;
import org.mmbase.util.xml.BuilderWriter;
import org.mmbase.util.xml.EntityResolver;
import org.mmbase.util.functions.*;
import org.mmbase.util.LocalizedString;
import org.mmbase.util.MMBaseContext;

import org.xml.sax.SAXException;

import java.util.concurrent.ConcurrentHashMap;

/**
 * The module which provides access to the MMBase storage defined
 * by the provided name/setup.
 * It holds the overal object cloud made up of builders, objects and relations and
 * all the needed tools to use them.
 *
 * @author Daniel Ockeloen
 * @author Pierre van Rooden
 * @author Johannes Verelst
 * @author Ernst Bunders
 * @version $Id$
 */
public class MMBase extends ProcessorModule {

    /**
     * State of MMBase after shutdown
     * @since MMBase-1.7
     */
    private static final int STATE_SHUT_DOWN = -3;

    /**
     * State of MMBase before the beginning of startup
     * @since MMBase-1.6
     */
    private static final int STATE_START_UP = -2;

    /**
     * State of MMBase at the start of the initialization
     * @since MMBase-1.8
     */
    private static final int STATE_STARTED_INIT = -1;

    /**
     * State of MMBase before builders are loaded
     * @since MMBase-1.6
     */
    private static final int STATE_LOAD = 0;
    /**
     * State of MMBase before builders are initialized
     * @since MMBase-1.6
     */
    private static final int STATE_INITIALIZE = 1;
    /**
     * State of MMBase after startup is completed
     * @since MMBase-1.6
     */
    private static final int STATE_UP = 2;

    // logging
    private static final Logger LOG = Logging.getLoggerInstance(MMBase.class);

    /**
     * Reference to the MMBase singleton. Used for quick reference by getMMBase();
     */
    private static MMBase mmbaseroot = null;

    /**
     * Time in seconds, when mmbase was started.
     * @since MMBase-1.7
     */
    public static final int startTime = MMBaseContext.startTime;

    /**
     * Base name for the storage  to be accessed using this instance of MMBase.
     * Retrieved from the mmbaseroot module configuration.
     * If not specified the default is "def1".
     * Should be made private and accessed instead using getBaseName()
     * @scope private
     */
    public String baseName = "def1";

    /**
     * Reference to the TypeDef builder.
     */
    private TypeDef typeDef;
    /**
     * Reference to the RelDef builder.
     */
    private RelDef relDef;
    /**
     * Reference to the OALias builder.
     */
    private OAlias oAlias;
    /**
     * Reference to the InsRel builder.
     */
    private InsRel insRel;
    /**
     * Reference to the TypeRel builder.
     */
    private TypeRel typeRel;

    /**
     * The map that contains all loaded builders. Includes virtual builders.
     * A collection of builders from this map can be accessed by calling {@link #getBuilders}
     */
    private final Map<String, MMObjectBuilder> mmobjs = new ConcurrentHashMap<String, MMObjectBuilder>();

    /**
     * Determines whether MMBase is in development mode.
     * @see #inDevelopment()
     * @since MMBase-1.8.1
     */
    private boolean inDevelopment = false;


    /**
     * Authorisation type. Access using getAuthType()
     */
    private String authtype = "none";

    /**
     * The storage manager factory to use. Retrieve using getStorageManagerFactory();
     */
    private StorageManagerFactory<? extends StorageManager> storageManagerFactory = null;

    /**
     * Reference to the Root builder (the most basic builder, aka 'object').
     * This can be null (does not exist) in older systems
     */
    private MMObjectBuilder rootBuilder;

    /**
     * our securityManager (MMBaseCop)
     */
    private MMBaseCop mmbaseCop = null;

    /**
     * Reference to the cluster builder, a virtual builder used to perform
     * multilevel searches.
     * @see ClusterBuilder
     */
    private  ClusterBuilder clusterBuilder;


    private TimeZone timeZone = TimeZone.getDefault();

    /**
     * MMBase 'up state. Access using getState()
     *
     * @since MMBase-1.6
     */
    private int mmbaseState = STATE_START_UP;

    /**
     * This set contains the names of buidlers that are in the process of being loaded.
     * The map is used to prevent circular references when extending builders.
     *
     * @since MMBase-1.6
     */
    private Set<String> loading = new HashSet<String>();

    /**
     * Constructor to create the MMBase root module.
     */
    public MMBase(String name) {
        super(name);
        if (mmbaseroot != null) LOG.error("Tried to instantiate a second MMBase");
        LOG.debug("MMBase constructed");
    }

    /**
     * This method tries to configure the persistence directory of OSCache, if possible (OSCache is
     * available, and necessary (no 'cache.path' property is configured for OSCache). Then, a
     * directory named 'oscache' in the mmbase data directory is used to set the 'cache.path'
     * property of the ServletCacheAdminidstrator class of OSCache.
     * @since MMBase-1.9
     */
    protected  void configureOSCache() {
        try {
            Properties p = new Properties();
            java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("oscache.properties");
            if (is != null) {
                p.load(is);
            }
            if (p.getProperty("cache.path") == null || "".equals(p.getProperty("cache.path"))) {
                String oscache = getInitParameter("oscache.template");
                if (oscache == null || oscache.length() == 0) {
                    oscache = "$datadir/oscache/$contextpath/";
                }
                oscache = oscache.replaceAll("\\$datadir", getDataDir().toString());
                oscache = oscache.replaceAll("\\$machinename", getMachineName());
                oscache = oscache.replaceAll("\\$contextpath", MMBaseContext.getServletContext().getContextPath());
                oscache = oscache.replaceAll("/+", File.separator);
                p.setProperty("cache.path", oscache);
            }

            Class osCache = Class.forName("com.opensymphony.oscache.web.ServletCacheAdministrator");
            java.lang.reflect.Method m = osCache.getMethod("getInstance", javax.servlet.ServletContext.class, Properties.class);
            m.invoke(null, MMBaseContext.getServletContext(), p);
            LOG.service("Using " + p + " for oscache");
        } catch (Throwable e) {
            LOG.service(e.getMessage());
        }
    }

    /**
     * @since MMBase-2.0
     */
    protected void setMMEntities(boolean logEnts) {
        StringBuilder sb = new StringBuilder();
        try {
            Set<Object> added = new HashSet<Object>();
            EntityResolver.appendEntities(sb, org.mmbase.framework.Framework.getInstance(), "framework", 0, added);
            EntityResolver.appendEntities(sb, org.mmbase.framework.ComponentRepository.getInstance(), "componentRepository", 0, added);

            org.mmbase.module.Module  mmbase = org.mmbase.module.Module.getModule("mmbaseroot", false);
            if (mmbase != null) {
                EntityResolver.appendEntities(sb, mmbase, "mmbase", 0, added);
            }
        } catch (Throwable ie) {
            LOG.warn(ie.getMessage());
            EntityResolver.setMMEntities(sb.toString());
        }
        String ents = sb.toString();
        if (logEnts) {
            LOG.debug("Using entities\n" + ents);
        }
        EntityResolver.setMMEntities(ents);

    }
    /**
     * Initializes the MMBase module. Evaluates the parameters loaded from the configuration file.
     * Sets parameters (authorization, language), loads the builders, and starts MultiCasting.
     */
    @Override
    public void init() {
        synchronized(MMBase.class) {
            if (mmbaseState >= STATE_STARTED_INIT) {
                LOG.debug("Already initialized or initializing");
                return;
            } else if (mmbaseState == STATE_SHUT_DOWN) {
                LOG.warn("was shutdown... should NOT restart again!");
                return;
            }
            MMBaseContext.setDataDir(getInitParameter("datadir"));
            LOG.service("Init of " + org.mmbase.Version.get() + " (" + this + ")");

            configureOSCache();

            mmbaseState = STATE_STARTED_INIT;

            // Set the mmbaseroot singleton var
            // This prevents recursion if MMBase.getMMBase() is called while
            // this method is run
            mmbaseroot = this;

            super.init();

            // is there a basename defined in MMBASE.properties ?
            String tmp = getInitParameter("BASENAME");
            if (tmp != null) {
                // yes then replace the default name (def1)
                baseName = tmp;
            } else {
                LOG.info("init(): No name defined for mmbase using default (def1)");
            }

            tmp = getInitParameter("AUTHTYPE");
            if (tmp != null && !tmp.equals("")) {
                authtype = tmp;
            }

            tmp = getInitParameter("TIMEZONE");
            if (tmp != null && !tmp.equals("")) {
                timeZone = TimeZone.getTimeZone(tmp);
            }
            DateFormat format = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.US);
            format.setTimeZone(timeZone);
            MMBaseContext.INITLOG.info("MMBase Time zone   : " + timeZone.getDisplayName(Locale.US) + " (it's now " + format.format(new Date()) + ")");
            org.mmbase.util.dateparser.DateParser.setDefault(timeZone);

            tmp = getInitParameter("LANGUAGE");
            if (tmp != null && !tmp.equals("")) {
                ServletContext sx = MMBaseContext.getServletContext();
                if (sx != null) {
                    String fmtLocale = sx.getInitParameter(LocalizedString.FMT_DEFAULT_PARAM);
                    if (fmtLocale != null && ! fmtLocale.equals(tmp)) {
                        LOG.warn("The default fmt locale: " + fmtLocale + " is not equal to the MMBase Locale " + tmp + ". Consider removing the mmbaseroot.language setting.");
                    }
                }
                LocalizedString.setDefault(LocalizedString.getLocale(tmp));
            } else {
                LOG.service("No default specified using default " + LocalizedString.getDefault());
            }
            MMBaseContext.INITLOG.info("MMBase Locale      : " + getLocale());


            tmp = getInitParameter("DEVELOPMENT");
            if (tmp != null && !tmp.equals("")) {
                inDevelopment = "true".equals(tmp);
            }

            tmp = getInitParameter("ENCODING");
            if (tmp != null && !tmp.equals("")) {
                MMBaseContext.setEncoding(tmp);
            }
            setMMEntities(false);

            // default locale has to be known before initializing datatypes:
            DataTypes.initialize();

            String localHost;
            try {
                localHost = java.net.InetAddress.getLocalHost().getHostName();
            } catch (java.net.UnknownHostException uhe) {
                localHost = "localhost";
            }

            LOG.service("Localhost: " + localHost);

            String hostParam = getInitParameter("HOST");
            if (hostParam != null && !hostParam.equals("")) {
                // try to incorporate the localhost (if needed)
                int pos = hostParam.indexOf("${LOCALHOST}");
                if (pos != -1) {
                    hostParam =hostParam.substring(0, pos) +
                        localHost + hostParam.substring(pos + 12);
                }
                MMBaseContext.setHost(hostParam);
            } else {
                MMBaseContext.setHost(localHost);
            }

            setMMEntities(false);

            String machineNameParam = getInitParameter("MACHINENAME");
            if (machineNameParam != null) {
                // try to incorporate the hostname (if needed)
                int pos = machineNameParam.indexOf("${HOST}");
                if (pos != -1) {
                    machineNameParam =
                        machineNameParam.substring(0, pos) +
                        MMBaseContext.getHost() + machineNameParam.substring(pos + 7);
                }
                // you may also try to incorporate the username in the machine name
                pos = machineNameParam.indexOf("${USER}");
                if (pos != -1) {
                    machineNameParam = machineNameParam.substring(0, pos) + System.getProperty("user.name") + machineNameParam.substring(pos + 7);
                }

                pos = machineNameParam.indexOf("${CONTEXT}");
                if (pos != -1) {
                    if (! MMBaseContext.isHtmlRootInitialized()) {
                        LOG.warn("HTML root not yet known. MachineName will not be correct yet.");
                    }
                    machineNameParam = machineNameParam.substring(0, pos) + MMBaseContext.getHtmlRootUrlPath() + machineNameParam.substring(pos + 10);
                }

                MMBaseContext.setMachineName(machineNameParam);
            } else {
                if (! MMBaseContext.isHtmlRootInitialized()) {
                    LOG.warn("HTML root not yet known. MachineName will not be correct yet.");
                }
                // default machine name is the local host name plus context-path.
                // We suppose that that is sufficiently unique in most cases
                MMBaseContext.setMachineName(localHost + MMBaseContext.getHtmlRootUrlPath());

            }
            MMBaseContext.INITLOG.info("MMBase machine name used for clustering: '" + getMachineName() + "'");

            EventManager.getInstance().propagateEvent(new SystemEvent.MachineName(getMachineName()), true);

            setMMEntities(false);

            LOG.service("Initializing  storage");
            initializeStorage();
            EventManager.getInstance().propagateEvent(new SystemEvent.DataSourceAvailable(getDataSource(), getBaseName()));
            mmbaseState = STATE_LOAD;

            LOG.debug("Loading builders:");

            loadBuilders();

            if(Thread.currentThread().isInterrupted()) {
                LOG.info("Interrupted");
                return;
            }

            mmbaseState = STATE_INITIALIZE;

            LOG.debug("Checking MMBase");
            if (!checkMMBase()) {
                // there is no base defined yet, create the core objects
                createMMBase();
            }

            LOG.service("Initializing  builders:");
            setMMEntities(false);

            initBuilders();

            MMObjectBuilder resources = getBuilder("resources");
            if (resources != null && resources.getClass().getName().equals("org.mmbase.module.builders.Resources")) {
                org.mmbase.util.ResourceWatcher.setResourceBuilder("resources");
            } else {
                org.mmbase.util.ResourceWatcher.setResourceBuilder(null);
            }

            EventManager.getInstance().addEventListener(org.mmbase.cache.NodeCache.getCache());

            LOG.debug("Objects started");

            String writerpath = getInitParameter("XMLBUILDERWRITERDIR");
            if (writerpath != null && !writerpath.equals("")) {
                for (MMObjectBuilder builder : getBuilders()) {
                    if (!builder.isVirtual()) {
                        String builderName = builder.getTableName();
                        LOG.debug("WRITING BUILDER FILE =" + writerpath + File.separator + builderName);
                        try {
                            BuilderWriter builderOut = new BuilderWriter(builder);
                            builderOut.setIncludeComments(false);
                            builderOut.setExpandBuilder(false);
                            builderOut.writeToFile(writerpath + File.separator + builder.getTableName() + ".xml");
                        } catch (Exception ex) {
                            LOG.error(ex.getMessage(), ex);
                        }
                    }
                }
            }
            if(Thread.currentThread().isInterrupted()) {
                LOG.info("Interrupted");
                return;
            }

            setMMEntities(false);
            // try to load security...
            try {
                mmbaseCop = new MMBaseCop();
                EventManager.getInstance().propagateEvent(new SecurityLoaded(mmbaseCop));
            } catch (Exception e) {
                LOG.fatal("Error loading the mmbase cop: " + e.getMessage());
                LOG.error(e.getMessage(), e);
                LOG.error("MMBase will continue without security.");
                LOG.error("All future security invocations will fail.");
            }
            setMMEntities(true);
            typeRel.readCache();

            // signal that MMBase is up and running
            mmbaseState = STATE_UP;
            MMBaseContext.INITLOG.info("MMBase is up and running");

            org.mmbase.storage.implementation.database.DatabaseStorageManagerFactory dsmf =
                (org.mmbase.storage.implementation.database.DatabaseStorageManagerFactory) getStorageManagerFactory();
            EventManager.getInstance().propagateEvent(new SystemEvent.Up(dsmf.getDatabaseName(), dsmf.getBinaryFileBasePath()), true);
            MMBase.class.notifyAll();
        }
    }


    /**
     * @see org.mmbase.module.Module#shutdown()
     */
    @Override
    public void shutdown() {
        mmbaseState = STATE_SHUT_DOWN;

        //shutdown in the reverse order as init does

        mmbaseCop = null;

        if (mmobjs != null) {
            for (MMObjectBuilder builder : mmobjs.values()) {
                builder.shutdown();
            }
            mmobjs.clear();
        }

        oAlias = null;
        insRel = null;
        typeRel = null;
        relDef = null;
        typeDef = null;

        clusterBuilder = null;
        rootBuilder = null;

        storageManagerFactory = null;

        // there all over the place static references to mmbasroot are maintained, which I cannot
        // change presently. so let's clean up mmbaseroot itself as well as possible...
        mmbaseroot = null;
    }

    /**
     * @since MMBase-1.7
     */
    public boolean isShutdown() {
        return  mmbaseState == STATE_SHUT_DOWN;
    }

    /**
     * Returns <code>true</code> when MMBase is in development mode.
     * This can be used to determine behavior with regards to common errors,
     * such as whether or not to throw an exception when a non-existing field
     * in a buidler is referenced.
     * The value for this property ('true' or 'false') can be set in the "development"
     * property in the mmbaseroot.xml configuration file.
     * The default value is <code>false</code>.
     * @since MMBase-1.8.1
     */
    public boolean inDevelopment() {
        return inDevelopment;
    }

    /**
     * Checks whether the storage to be used exists.
     * The system determines whether the object table exists
     * for the baseName provided in the configuration file.
     * @return <code>true</code> if the storage exists and is accessible, <code>false</code> otherwise.
     */
    boolean checkMMBase() {
        return getStorageManager().exists();
    }

    /**
     * Create a new MMBase persistent storage instance.
     * The storage instance created is based on the baseName provided in the configuration file.
     * This call automatically creates an object table.
     * The fields in the table are either specified in an object builder xml,
     * or from a default setup existing of a number field and a owner field.
     * Note: If specified, the object builder is instantiated and its table created, but
     * the builder is not registered in the TypeDef builder, as this builder does not exist yet.
     * Registration happens when the other builders are registered.
     * @return <code>true</code> if the storage was succesfully created, otherwise a runtime exception is thrown
     *   (shouldn't it return <code>false</code> instead?)
     */
    boolean createMMBase() {
        LOG.debug(" creating new multimedia base : " + baseName);
        getStorageManager().create();
        return true;
    }

    /**
     * Determines whether a builder is in the process of being loaded,
     * but not yet finished. Needed to track down circular references.
     * @return true if the builder is being loaded
     *
     * @since MMBase-1.6
     */
    private boolean builderLoading(String name) {
        return loading.contains(name);
    }

    /**
     * Retrieves a specified builder.
     * If the builder is not loaded, but the system is in the 'startup'  state
     * (i.e. it is in the process of loading builders), an attempt is made to
     * directly load the builder.
     * This allows for dependencies between builders to exist (i.e. inheritance).
     * When circular reference occurs between two loading buidlers, an exception is thrown.
     *
     * @since MMBase-1.6
     * @param name The name of the builder to retrieve
     * @return a <code>MMObjectBuilder</code> for the specified builder
     * @throws CircularReferenceException when circular reference is detected
     * @throws BuilderConfigurationException if the builder config file does not exist
     */
    public MMObjectBuilder getBuilder(String name) throws CircularReferenceException {
        MMObjectBuilder builder = getMMObject(name);
        if (builder == null && (mmbaseState == STATE_LOAD)) {
            // MM:  odd way to check this. Could it not be done a bit more explicitely?
            if (builderLoading(name)) {
                throw new CircularReferenceException("Circular reference to builder with name '" + name + "': currently loading " + loading);
            }
            builder = loadBuilder(name);
        }
        return builder;
    }

    public String getBuilderNameForNode(final int number) {
        int nodeType = getMMBase().getStorageManager().getNodeType(number);
        if (nodeType < 0) {
            // the node does not exists, which according to javadoc should return null
            throw new StorageNotFoundException("Cannot determine node type of node with number " + number);
        }
        // if the type is not for the current builder, determine the real builder
        return getTypeDef().getValue(nodeType);
    }

    public MMObjectBuilder getBuilderForNode(final int number) {
        String builderName = getBuilderNameForNode(number);
        MMObjectBuilder nodeBuilder = null;
        if (builderName == null) {
            LOG.error("The nodetype name of node #" + number + " could not be found '");
        }
        else {
            nodeBuilder = getBuilder(builderName);
            if (nodeBuilder == null) {
                LOG.warn("Node #" + number + "'s builder " + builderName + " is not loaded, taking 'object'.");
                nodeBuilder = getBuilder("object");
            }
        }
        return nodeBuilder;
    }

    /**
     * @since MMBase-1.8
     */
    public MMObjectBuilder addBuilder(String name, MMObjectBuilder bul) {
        if (bul == null) {
            throw new IllegalArgumentException("Builder '" + name + "' was null");
        }
        return mmobjs.put(name, bul);
    }

    /**
     * Retrieves a specified builder.
     * @scope protected
     * @param name The name of the builder to retrieve
     * @return a <code>MMObjectBuilder</code> if found, <code>null</code> otherwise
     */
    public MMObjectBuilder getMMObject(String name) {
        if (name == null) throw new RuntimeException("Cannot get builder with name 'NULL' in " + MMBaseContext.getMachineName());
        MMObjectBuilder o = mmobjs != null ? mmobjs.get(name) : null;
        if (o == null) {
            LOG.trace("MMObject " + name + " could not be found"); // can happen...
        }
        return  o;
    }

    /**
     * Retrieves the MMBase module('mmbaseroot').
     * @return the active MMBase module
     */
    public static MMBase getMMBase() {
        if (mmbaseroot == null) {
            synchronized(MMBase.class) { // make sure only one mmbaseroot is instantiated (synchronized on random static member...)
                mmbaseroot = getModule(MMBase.class);
                if (mmbaseroot == null) {
                    LOG.fatal("The mmbaseroot module could not be found. Perhaps 'mmbaseroot.xml' is missing?", new Exception());
                } else {
                    mmbaseroot.startModule();
                }
                //MMBase.class.notifyAll();
            }
        }
        return mmbaseroot;
    }


    /**
     * Retrieves the loaded security manager(MMBaseCop).
     * @return the loaded security manager(MMBaseCop)
     */
    public MMBaseCop getMMBaseCop() {
        return mmbaseCop;
    }

    /**
     * Retrieves a Collection of loaded builders.
     */
    public Collection<MMObjectBuilder> getBuilders() {
        return mmobjs.values();
    }

    /**
     * Returns a reference to the InsRel builder.
     * @return the <code>InsRel</code> builder if defined, <code>null</code> otherwise
     */
    public InsRel getInsRel() {
        return insRel;
    }

    /**
     * Returns a reference to the RelDef builder.
     * @return the <code>RelDef</code> builder if defined, <code>null</code> otherwise
     */
    public RelDef getRelDef() {
        return relDef;
    }

    /**
     * Returns a reference to the TypeDef builder.
     * @return the <code>TypeDef</code> builder if defined, <code>null</code> otherwise
     */
    public TypeDef getTypeDef() {
        return typeDef;
    }

    /**
     * Returns a reference to the TypeRel builder.
     * @return the <code>TypeRel</code> builder if defined, <code>null</code> otherwise
     */
    public TypeRel getTypeRel() {
        return typeRel;
    }

    /**
     * Returns a reference to the OAlias builder.
     * @return the <code>OAlias</code> builder if defined, <code>null</code> otherwise
     */
    public OAlias getOAlias() {
        return oAlias;
    }

    /**
     * Returns a reference to the Object builder.
     * The Object builder is the builder from which all other builders eventually extend.
     * @return the <code>Object</code> builder.
     * @since MMBase-1.6
     */
    public MMObjectBuilder getRootBuilder() {
        if (rootBuilder == null) {
            rootBuilder = loadBuilder("object");
        }
        return rootBuilder;
    }

    /**
     * Returns the otype of the Object builder, or -1 if it is not known.
     * The Object builder is the builder from which all other builders eventually extend.
     * @since MMBase-1.6
     */
    public int getRootType() {
        if (rootBuilder == null) {
            return -1;
        } else {
            return rootBuilder.getNumber();
        }
    }

    /**
     * Returns a reference to the cluster builder, a virtual builder used to
     * perform multilevel searches.
     *
     * @return The cluster builder.
     * @see ClusterBuilder
     */
    public ClusterBuilder getClusterBuilder() {
        assertUp();
        return clusterBuilder;
    }

    /**
     * Locks until initialization of mmbase is finished.
     * @since MMBase-1.7
     */
    protected void assertUp() {
        if (! getState()) {
            synchronized(this) {
                // lock until up. (Init is synchronized on this too)
            }
        }
    }

    /**
     * Retrieves the storage base name
     * @return the base name as a <code>String</code>
     */
    public String getBaseName() {
        return baseName;
    }


    /**
     * Retrieves the machine name.
     * This value is set using the configuration file.
     * @return the machine name as a <code>String</code>. Or <code>null</code> if not yet determined.
     */
    public final String getMachineName() {
        return MMBaseContext.getMachineName();
    }

    /**
     * Retrieves the host name or ip number
     * This value is set using the configuration file.
     * @return the host name as a <code>String</code>
     */
    public String getHost() {
        return MMBaseContext.getHost();
    }

    /**
     * Loads a core Builder.
     * If the builder  does not exist, an exception is thrown.
     * @since MMBase-1.6
     * @param name the name of the builder to load
     * @return the builder
     * @throws BuilderConfigurationException if the builder config file does not exist or is inactive
     */
    private MMObjectBuilder loadCoreBuilder(String name) {
        MMObjectBuilder builder = loadBuilder(name);
        if (builder == null) {
            throw new BuilderConfigurationException("The core builder " + name + " is mandatory but inactive.");
        } else {
            LOG.debug("Loaded core builder " + builder + " with otype " + builder.getNumber());
            return builder;
        }
    }

    /**
     *@since MMBase-1.7
     */
    private void loadBuilders() {
        // first load the core builders
        // remarks:
        //  - If nodescaches inactive, in init of typerel reldef nodes are created wich uses InsRel.getNumber(), so typerel must be started after insrel and reldef. (bug #6237)

        getRootBuilder(); // loads object.xml if present.

        typeDef = (TypeDef) loadCoreBuilder("typedef");
        relDef  = (RelDef)  loadCoreBuilder("reldef");
        insRel  = (InsRel)  loadCoreBuilder("insrel");
        typeRel = (TypeRel) loadCoreBuilder("typerel");

        try {
            oAlias = (OAlias)loadBuilder("oalias");
        } catch (BuilderConfigurationException e) {
            // OALias  builder was not defined -
            // builder is optional, so this is not an error
        }

        Set<String> builders = getBuilderLoader().getResourcePaths(ResourceLoader.XML_PATTERN, true/* recursive*/);

        LOG.info("Loading builders: " + builders);
        for (String builderXml : builders) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            String resourceName = ResourceLoader.getName(builderXml);
            String resourceDirectory = ResourceLoader.getDirectory(builderXml) + "/";
            loadBuilderFromXML(resourceName, resourceDirectory);
        }
        EventManager.getInstance().propagateEvent(new BuildersRead(org.mmbase.bridge.LocalContext.getCloudContext()));

        LOG.debug("Starting Cluster Builder");
        clusterBuilder = new ClusterBuilder(this);
    }

    /**
     * Initializes the builders, using the builder xml files in the config directory
     * @return Always <code>true</code>
     */
    boolean initBuilders() {

        typeDef.init();
        typeDef.createIfNotExists();

        // first initialize versions, if available (table must exist for queries to succeed)
        LOG.debug("Versions:");
        Versions versions = (Versions)getBuilder("versions");
        if (versions != null) {
            versions.init();
            versions.createIfNotExists();
        }

        relDef.init();
        relDef.createIfNotExists();
        insRel.init();
        insRel.createIfNotExists();
        typeRel.init();
        typeRel.createIfNotExists();

        LOG.debug("mmobjects, inits");
        for (Iterator<MMObjectBuilder> bi = getBuilders().iterator(); bi.hasNext();) {
            MMObjectBuilder builder = bi.next();
            LOG.debug("init " + builder);
            try {
                initBuilder(builder);
            } catch (BuilderConfigurationException e) {
                // something bad with this builder or its parents - remove it
                LOG.error("Removed builder " + builder.getTableName() + " from the builderlist, as it cannot be initialized.");
                bi.remove();
            } catch (Exception ex) {
                LOG.error("Something went wrong while initializing builder " + builder.getTableName());
                LOG.info("This builder will be removed from active builder list");
                LOG.error(ex.getMessage(), ex);
                bi.remove();
            }
        }
        // every builder is now inited, create the tables.
        // This must happen at last because builder's fields may reference to each other
        for (MMObjectBuilder builder : getBuilders()) {
            builder.createIfNotExists();
        }

        LOG.debug("**** end of initBuilders");
        return true;
    }

    /**
     * inits a builder
     * @param builder The builder which has to be initialized
     */
    public void initBuilder(MMObjectBuilder builder) {
        if (!builder.isVirtual()) {
            try {
                builder.init();
            } catch (org.mmbase.storage.StorageException se) {
                LOG.warn(se);
                builder.createIfNotExists();
                builder.init();
            }

            if (typeDef == null) {
                throw new IllegalStateException("No typedef builder defined");
            }
            typeDef.loadTypeDef(builder.getTableName());
            Versions versions = (Versions)getBuilder("versions");
            if (versions != null && versions.created()) {
                checkBuilderVersion(builder.getTableName(), versions);
            }
        }
    }

    /**
     * Unloads a builders from MMBase. After this, the builder is gone
     * @param builder the builder which has to be unloaded
     */
    public void unloadBuilder(MMObjectBuilder builder) {
        if (mmobjs.remove(builder.getTableName()) == null) {
            throw new RuntimeException("builder with name: " + builder.getTableName() + " could not be unloaded, since it was not loaded.");
        }
        if (!builder.isVirtual()) {
            typeDef.unloadTypeDef(builder.getTableName());
            LOG.info("unloaded builder with name:" + builder.getTableName());
        } else {
            LOG.info("unloaded virtual builder with name:" + builder.getTableName());
        }
    }

    /**
     * The (base)path to the builder configuration files
     * @since MMBase-1.8
     */
    public ResourceLoader getBuilderLoader() {
        // this also calls the static init of BuilderReader and registers the system and public ids.
        return org.mmbase.util.xml.AbstractBuilderReader.getBuilderLoader();
    }

    /**
     * @since MMBase-1.8
     */
    public BuilderReader getBuilderReader(String builderName) {
        try {
            BuilderReader r = null;
            int urlWeight = -1;
            String protocol = null;
            for (java.net.URL url : getBuilderLoader().getResourceList(builderName + ".xml")) {
                if (! url.openConnection().getDoInput()) {
                    LOG.debug("Cannot read " + url);
                    continue;
                } else {
                    LOG.debug("Can read " + url);
                }
                if (protocol != null && ! url.getProtocol().equals(protocol)) {
                    LOG.debug(url + " less important than " + r.getSystemId() + " breaking now");
                    break;
                }
                org.w3c.dom.Document doc = ResourceLoader.getDocument(url, true, BuilderReader.class);
                BuilderReader prop = new BuilderReader(doc, this);
                int proposalWeight = ResourceLoader.getWeight(url);
                prop.setSystemId(url.toString());
                if (r == null) {
                    r = prop;
                    urlWeight = ResourceLoader.getWeight(url);
                    protocol = url.getProtocol();
                } else if (urlWeight == proposalWeight && prop.getVersion() > r.getVersion()) {
                    LOG.service(url.toString() + " has a higher version than " + r.getSystemId() + " so, using that in stead (weights are both " + urlWeight + ")");
                    r = prop;
                } else {
                    if (proposalWeight > urlWeight) {
                        LOG.warn("The weight of the proposal is bigger. This should never happen");
                    } else if (proposalWeight <  urlWeight) {
                        LOG.service(url.toString() + " has a lower weight than " + r.getSystemId() + ". Ignoring it.");
                    } else {
                        LOG.service(url.toString() + " has a lower or equals version than " + r.getSystemId() + ". Ignoring it (weights are both " + urlWeight + ")");
                    }
                }
            }
            if (r != null) {
                LOG.debug("Found " + r.getSystemId());
            }
            return r;
        } catch (SAXException se) {
            LOG.error(se);
            return null;
        } catch (java.io.IOException ioe) {
            LOG.error(ioe);
            return null;
        }
    }

    /**
     * Locate one specific builder withing the main builder config path, including sub-paths.
     * If the builder already exists, the existing object is returned instead.
     * If the builder cannot be found in this path, a BuilderConfigurationException is thrown.
     * @since MMBase-1.6
     * @param builderName name of the builder to initialize
     * @return the initialized builder object, or null if the builder could not be created (i.e. is inactive).
     * @throws BuilderConfigurationException if the builder config file does not exist
     */
    synchronized MMObjectBuilder loadBuilder(String builderName) { // synchronized to make sure that storage initialized only once
        return loadBuilder(builderName, "");
    }

    /**
     * Locate one specific builder within a given path, relative to the main builder config path, including sub-paths.
     * Return the actual path.
     * @param builderName name of the builder to find
     * @param path the path to start searching. The path need be closed with a '/ character
     * @return the file path to the builder xml, or null if the builder could not be created (i.e. is inactive).
     * @throws BuilderConfigurationException if the builder config file does not exist
     * @todo The second argument (and perhaps the whole function) is silly, only exists because this
     *       function used to be implemented recursively (now delegated to ResourceLoader).
     */
    public String getBuilderPath(String builderName, String path) {
        Set<String> builders = getBuilderLoader().getResourcePaths(java.util.regex.Pattern.compile(path + ResourceLoader.XML_PATTERN.pattern()), true /*recursive*/);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Found builders " + builders + " from " +  getBuilderLoader()  + " searching for " + builderName);
        }
        String xml = builderName + ".xml";
        for (String builderXml : builders) {
            if (builderXml.equals(xml)) {
                return "";
            } else if (builderXml.endsWith("/" + xml)) {
                return builderXml.substring(0, builderXml.length() - xml.length());
            }
        }
        return null;
    }

    /**
     * Locate one specific builder within a given path, relative to the main builder config path, including sub-paths.
     * If the builder already exists, the existing object is returned instead.
     * @param builderName name of the builder to initialize
     * @param ipath the path to start searching. The path need be closed with a File.seperator character.
     * @return the initialized builder object, or null if the builder could not be created (i.e. is inactive).
     * @throws BuilderConfigurationException if the builder config file does not exist
     */
    MMObjectBuilder loadBuilder(String builderName, String ipath) {
        MMObjectBuilder builder = getMMObject(builderName);
        if (builder != null) {
            LOG.debug("Builder '" + builderName + "' is already loaded");
            return builder;
        }
        String path = getBuilderPath(builderName, ipath);
        if (path != null) {
            return loadBuilderFromXML(builderName, path);
        } else {
            throw new BuilderConfigurationException("Cannot find specified builder " + builderName + " (" + path + ")");
        }
    }

    /**
     * Create a new builder object using a xml configfile located in a given path relative to the main builder config path,
     * and return the builder object.
     * If the builder already exists, the existing object is returned instead.
     * Note that the builder's init() method is NOT called (since some builders need other builders in memory when their init() is called,
     * this method is called seperately after all builders are loaded).
     * @deprecation-used uses deprecated builder methods, contains commented-out code
     * @param builderName name of the builder to initialize
     * @param ipath the path to start searching. The path need be closed with a '/' character.
     * @return the loaded builder object.
     */
    public MMObjectBuilder loadBuilderFromXML(String builderName, String ipath) {
        MMObjectBuilder builder = getMMObject(builderName);
        if (builder != null) {
            LOG.debug("Builder '" + builderName + "' is already loaded");
            return builder;
        }

        BuilderReader parser = null;
        try {
            // register the loading of this builder
            parser = getBuilderReader(ipath + builderName);
            loading.add(builderName);
            if (parser == null) {
                LOG.warn("Not found " + ipath + builderName);
                loading.remove(builderName);
                return null;
            }
            if (! parser.getRootElement().getTagName().equals("builder")) {
                LOG.debug(parser.getSystemId() + " does not represent a builder xml. Because the root element is not 'builder' but " + parser.getRootElement().getTagName() + ". This file is ignored.");
                loading.remove(builderName);
                return null;
            }

            String status = parser.getStatus();
            if (status.equals("active")) {
                LOG.service("Starting builder: " + parser.getSystemId());
                Class newclass;
                try {
                    String classname = parser.getClassName();
                    newclass = Class.forName(classname);
                } catch (ClassNotFoundException cnfe) {
                    MMObjectBuilder p = parser.getParentBuilder();
                    if(p != null) {
                        newclass = p.getClass();
                    } else {
                        newclass = MMObjectBuilder.class;
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.error(cnfe.toString() + " (for '" + parser.getClassName() + "' of builder '" + ipath + builderName + "')  Falling back to " + newclass.getName(), cnfe);
                    } else {
                        LOG.error(cnfe.toString() + " (for '" + parser.getClassName() + "' of builder '" + ipath + builderName + "')  Falling back to " + newclass.getName());
                    }
                } catch (NoClassDefFoundError ncdfe) {
                    MMObjectBuilder p = parser.getParentBuilder();
                    if(p != null) {
                        newclass = p.getClass();
                    } else {
                        newclass = MMObjectBuilder.class;
                    }
                    LOG.error(ncdfe.toString() + " (for '" + parser.getClassName() + "' of builder '" + ipath + builderName + "')  Falling back to " + newclass.getName(), ncdfe);
                }
                builder = (MMObjectBuilder)newclass.newInstance();

                addBuilder(builderName, builder);

                builder.setXMLPath(ipath);
                builder.setMMBase(this);
                builder.setTableName(builderName);

                // register the parent builder, if applicable
                MMObjectBuilder parent = parser.getParentBuilder();
                if (parent != null) {
                    builder.setParentBuilder(parent);
                } else if ((builder instanceof InsRel) && !builderName.equals("insrel")) {
                    builder.setParentBuilder(getInsRel());
                } else if (!builderName.equals("object")) {
                    builder.setParentBuilder(getRootBuilder());
                }

                Map<String, String> descriptions = parser.getDescriptions();
                builder.setDescriptions(descriptions);
                String desc = descriptions.get(getLocale().getLanguage());
                // XXX" set description by builder?
                builder.setDescription(desc);
                builder.setSingularNames(parser.getSingularNames());
                builder.setPluralNames(parser.getPluralNames());
                builder.setVersion(parser.getVersion());
                builder.setMaintainer(parser.getMaintainer());
                builder.setSearchAge("" + parser.getSearchAge());
                builder.setInitParameters(parser.getProperties());
                parser.getDataTypes(builder.getDataTypeCollector());
                builder.setFields(parser.getFields(builder, builder.getDataTypeCollector()));
                builder.getStorageConnector().addIndices(parser.getIndices(builder));
                for (Function func : parser.getFunctions(builder)) {
                    Function prev = builder.addFunction(func);
                    LOG.service((prev == null ? "Added " : "Replaced ") + func + " to " + builder);
                }
                if (parent != null) {
                    for (Function parentFunction : parent.getFunctions()) {
                        if (builder.getFunction(parentFunction.getName()) == null) {
                            builder.addFunction(parentFunction);
                        }
                    }
                }
            } else {
                LOG.service("Inactive builder: " + parser.getSystemId());
            }
        } catch (BuilderConfigurationException bcfe) {
            loading.remove(builderName);
            LOG.error((parser == null ? ipath + builderName : parser.getSystemId()) + ": " +
                      bcfe.getMessage() + " " + bcfe.getMessage());
            return null;
        } catch (Throwable e) { // what kind of exceptions are these?
            loading.remove(builderName);
            LOG.error((parser == null ? ipath + builderName : parser.getSystemId()) + ": " +
                      e.getClass() + " " + e.getMessage(), e);
            return null;
        }
        loading.remove(builderName);
        return builder;
    }

    /**
     * Loads either the storage manager factory or the appropriate support class using the configuration parameters.
     * @since MMBase-1.7
     */
    protected void initializeStorage() {
        if (storageManagerFactory != null) return; // initialized allready
        try {
            storageManagerFactory = StorageManagerFactory.newInstance(this);
            // print information about storage
            LOG.info("Using class: '" + storageManagerFactory.getClass().getName() + "'.");
        } catch (StorageException se) {
            throw new StorageError(se.getMessage());
        }
    }

    /**
     * Returns StorageManagerFactory class used to access the storage configuration.
     * @since MMBase-1.7
     * @return a StorageManagerFactory class, or <code>null</code> if not configured
     */
    public StorageManagerFactory<? extends StorageManager> getStorageManagerFactory() {
        return  storageManagerFactory;
    }

    /**
     * Returns a StorageManager to access the storage.. Equal to getStorageManagerFactory().getStorageManager().
     * @since MMBase-1.7
     * @return a StorageManager class
     * @throws StorageException if no storage manager could be instantiated
     */
    public StorageManager<?> getStorageManager() throws StorageException {
        if (storageManagerFactory == null) {
            throw new StorageConfigurationException("Storage manager factory not configured.");
        } else {
            return storageManagerFactory.getStorageManager();
        }
    }

    /**
     * Returns a SearchQueryHandler to access the storage.. Equal to getStorageManagerFactory().getSearchQueryHandler().
     * @since MMBase-1.8
     * @return a StorageManager class
     * @throws StorageException if no storage manager could be instantiated
     */
    public CoreSearchQueryHandler getSearchQueryHandler() throws StorageException {
        if (storageManagerFactory == null) {
            throw new StorageConfigurationException("Storage manager factory not configured.");
        } else {
            return (CoreSearchQueryHandler) storageManagerFactory.getSearchQueryHandler();
        }
    }

    /*
     * Retrieves the autorisation type.
     * This value is set using the configuration file.
     * Examples are 'none' or 'basic'.
     * @return a <code>String</code> identifying the type
     */
    public String getAuthType() {
        return authtype;
    }

    /**
     * Retrieves the current language.
     * This value is set using the configuration file.
     * Examples are 'en' or 'nl'.
     * @return the language as a <code>String</code>
     */
    public String getLanguage() {
        return getLocale().getLanguage();
    }

    /**
     * Retrieves the current locale.
     * @since MMBase-1.8
     */
    public Locale getLocale() {
        return LocalizedString.getDefault();
    }

    /**
     * Retrieves the timezone asociated with this MMBase's 'DateTime' objects. MMBase stores dates
     * in storage as 'Date' but without time-zone information, and therefore to a certain
     * degree open to interpretation.
     *
     * Together with this timezone the times can be defined absoletely (that is, of course, relative
     * to the time frame of out planet).
     (
     * @since MMBase-1.8
     */
    public TimeZone getTimeZone() {
        return timeZone;
    }

    /**
     * Retrieves the encoding.
     * This value is set using the configuration file.
     * Examples are 'UTF-8' (default) or 'ISO-8859-1'.
     *
     * @return the coding as a <code>String</code>
     * @since  MMBase-1.6
     */
    public String getEncoding() {
        return MMBaseContext.getEncoding();
    }

    /**
     * Retrieves whether this mmbase module is running.
     * @return <code>true</code> if the module has been initialized and all builders loaded, <code>false</code> otherwise.
     */
    public boolean getState() {
        return mmbaseState == STATE_UP;
    }

    /**
     * Checks the builder version and, if needed, updates the version table.
     * Queries the xml files instead of the builder itself (?)
     * @return Returns <code>true</code> if the builder XML could be read, <code>false</code> otherwise.
     */
    private boolean checkBuilderVersion(String builderName, Versions ver) {

        MMObjectBuilder tmp = mmobjs.get(builderName);

        if (tmp == null) {
            return false;
        }

        if (tmp != null) {
            int version = tmp.getVersion();
            String maintainer = tmp.getMaintainer();

            int installedversion = ver.getInstalledVersion(builderName, "builder");
            if (installedversion == -1 || version > installedversion) {
                ver.setInstalledVersion(builderName, "builder", maintainer, version);
            }
        }
        return true;
    }

    /**
     * This is a conveniance method to help you register listeners to node and
     * relation events. Becouse they are now separate listeners the method accepts
     * an object that may have implemented either NodeEvent
     * or RelationEvent. This method checks and registers accordingly. <br/>
     * the purpose of this method is that a straight node or relation event listeren
     * will listen to any node or relation event. This method will wrap your event
     * listener to make shure only the requested event types are forwarded.
     * @see TypedRelationEventListenerWrapper
     * @see TypedNodeEventListenerWrapper
     * @see NodeEventListener
     * @see RelationEventListener
     * @param builder should be a valid builder name, the type for which you want to
     * receive events
     * @param listener some object implementing NodeEventListener, RelationEventListener,
     * or both
     * @since MMBase-1.8
     */
    public void addNodeRelatedEventsListener(String builder, org.mmbase.core.event.EventListener listener) {
        MMObjectBuilder b = getBuilder(builder);
        if(b != null){
            if(listener instanceof NodeEventListener){
                TypedNodeEventListenerWrapper tnelr = new TypedNodeEventListenerWrapper(b, (NodeEventListener)listener, true);
                EventManager.getInstance().addEventListener(tnelr);
            }
            if(listener instanceof RelationEventListener){
                TypedRelationEventListenerWrapper trelr = new TypedRelationEventListenerWrapper(b, (RelationEventListener)listener, RelationStep.DIRECTIONS_BOTH, true);
                EventManager.getInstance().addEventListener(trelr);
            }
        }
    }

    /**
     * @see MMBase#addNodeRelatedEventsListener
     * @param builder
     * @param listener
     * @since MMBase-1.8
     */
    public void removeNodeRelatedEventsListener(String builder, org.mmbase.core.event.EventListener listener) {
        MMObjectBuilder b = getBuilder(builder);
        if(b != null){
            if(listener instanceof NodeEventListener){
                TypedNodeEventListenerWrapper tnelr = new TypedNodeEventListenerWrapper(b, (NodeEventListener)listener, true);
                EventManager.getInstance().removeEventListener(tnelr);
            }
            if(listener instanceof RelationEventListener){
                TypedRelationEventListenerWrapper trelr= new TypedRelationEventListenerWrapper(b, (RelationEventListener)listener, RelationStep.DIRECTIONS_BOTH, true);
                EventManager.getInstance().removeEventListener(trelr);

            }
        }
    }


    /**
     * A setting 'datadir' can be specified in mmbaseroot.xml (and hence in your context xml). This
     * serves as a default for the 'blobs on disk' directory, but it can be used on other spots as well.
     * @since MMBase-1.8.6
     */
    public File getDataDir() {
        return MMBaseContext.getDataDir();
    }
    /**
     * Returns the datasource as was configured with mmbaseroot.xml properties.
     * since MMBase-1.9
     */
    public DataSource getDataSource() {
        // get the Datasource for the database to use
        // the datasource uri (i.e. 'jdbc/xa/MMBase' )
        // is stored in the mmbaseroot module configuration file
        String dataSourceURI = getInitParameter("datasource");
        if (dataSourceURI != null) {
            try {
                String contextName = getInitParameter("datasource-context");
                if (contextName == null) {
                    contextName = "java:comp/env";
                }
                LOG.service("Using configured datasource " + dataSourceURI);
                Context initialContext = new InitialContext();
                Context environmentContext = (Context) initialContext.lookup(contextName);
                return (DataSource)environmentContext.lookup(dataSourceURI);
            } catch(NamingException ne) {
                LOG.warn("Datasource '" + dataSourceURI + "' not available. (" + ne.getMessage() + "). Will attempt to use JDBC Module to access database.");
            }
        }
        return null;
    }

    /**
     * Whether to perform sanity checks during startup. Most prominently used by
     * DatabaseStorageManager to check the database tables.
     * @since MMBase-1.8.7
     */
    public boolean runStartupChecks() {
        return ! "false".equals(getInitParameter("runStartupChecks"));
    }


}
