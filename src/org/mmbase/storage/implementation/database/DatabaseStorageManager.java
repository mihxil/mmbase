/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.storage.implementation.database;

import java.io.*;
import java.sql.*;
import java.util.*;

import javax.sql.DataSource;

import org.mmbase.module.core.*;
import org.mmbase.module.corebuilders.FieldDefs;
import org.mmbase.storage.*;
import org.mmbase.storage.util.*;
import org.mmbase.util.Casting;
import org.mmbase.util.logging.*;

/**
 * A JDBC implementation of an object related storage manager.
 * @javadoc
 *
 * @author Pierre van Rooden
 * @since MMBase-1.7
 * @version $Id: DatabaseStorageManager.java,v 1.73 2004-09-20 12:00:26 pierre Exp $
 */
public class DatabaseStorageManager implements StorageManager {

    private static final Logger log = Logging.getLoggerInstance(DatabaseStorageManager.class);

    // conatisn a list of buffered keys
    protected static final List sequenceKeys = new LinkedList();

    // maximum size of the key buffer
    protected static Integer bufferSize = null;

    /**
     * Whether the warning about blob on legacy location was given.
     */
    private static boolean legacyWarned = false;

    /**
     * The factory that created this manager
     */
    protected DatabaseStorageManagerFactory factory;

    /**
     * The datasource through which to access the database.
     */
    protected DataSource dataSource;

    /**
     * The currently active Connection.
     * This member is set by {!link #getActiveConnection()} and unset by {@link #releaseActiveConnection()}
     */
    protected Connection activeConnection;

    /**
     * <code>true</code> if a transaction has been started.
     * This member is for state maitenance and may be true even if the storage does not support transactions
     */
    protected boolean inTransaction = false;

    /**
     * The transaction issolation level to use when starting a transaction.
     * This value is retrieved from the factory's {@link Attributes#TRANSACTION_ISOLATION_LEVEL} attribute, which is commonly set
     * to the highest (most secure) transaction isolation level available.
     */
    protected int transactionIsolation = Connection.TRANSACTION_NONE;

    /**
     * Pool of changed nodes in a transaction
     */
    protected Map changes;

    /**
     * Constructor
     */
    public DatabaseStorageManager() {}

    // for debug purposes
    protected final void logQuery(String msg) {
        if (log.isDebugEnabled()) {
            log.debug("Query :" + msg);
            log.trace(Logging.stackTrace());
        }
    }

    // javadoc is inherited
    public double getVersion() {
        return 1.0;
    }

    // javadoc is inherited
    public void init(StorageManagerFactory factory) throws StorageException {
        this.factory = (DatabaseStorageManagerFactory)factory;
        dataSource = (DataSource)factory.getAttribute(Attributes.DATA_SOURCE);
        if (factory.supportsTransactions()) {
            transactionIsolation = ((Integer)factory.getAttribute(Attributes.TRANSACTION_ISOLATION_LEVEL)).intValue();
        }
        // determine generated key buffer size
        if (bufferSize==null) {
            bufferSize = new Integer(1);
            Object bufferSizeAttribute = factory.getAttribute(Attributes.SEQUENCE_BUFFER_SIZE);
            if (bufferSizeAttribute != null) {
                try {
                    bufferSize = Integer.valueOf(bufferSizeAttribute.toString());
                } catch (NumberFormatException nfe) {
                    // remove the SEQUENCE_BUFFER_SIZE attribute (invalid value)
                    factory.setAttribute(Attributes.SEQUENCE_BUFFER_SIZE,null);
                    log.error("The attribute 'SEQUENCE_BUFFER_SIZE' has an invalid value(" +
                        bufferSizeAttribute + "), will be ignored.");
                }
            }
        }
    }

    /**
     * Obtains an active connection, opening a new one if needed.
     * This method sets and then returns the {@link #activeConnection} member.
     * If an active connection was allready open, and the manager is in a database transaction, that connection is returned instead.
     * Otherwise, the connection is closed before a new one is opened.
     * @throws SQLException if opening the connection failed
     */
    protected Connection getActiveConnection() throws SQLException {
        if (activeConnection != null) {
            if (factory.supportsTransactions() && inTransaction) {
                return activeConnection;
            } else {
                releaseActiveConnection();
            }
        }
        activeConnection = dataSource.getConnection();
        // set autocommit to true
        activeConnection.setAutoCommit(true);
        return activeConnection;
    }

    /**
     * Safely closes the active connection.
     * If a transaction has been started, the connection is not closed.
     */
    protected void releaseActiveConnection() {
        if (!(inTransaction && factory.supportsTransactions()) && activeConnection != null) {
            try {
                // ensure that future attempts to obtain a connection (i.e.e if it came from a pool)
                // start with autocommit set to true
                // needed because Query interface does not use storage layer to obtain transactions
                activeConnection.setAutoCommit(true);
                activeConnection.close();
            } catch (SQLException se) {
                // if something went wrong, log, but do not throw exceptions
                log.error("Failure when closing connection: " + se.getMessage());
            }
            activeConnection = null;
        }
    }

    // javadoc is inherited
    public void beginTransaction() throws StorageException {
        if (inTransaction) {
            throw new StorageException("Cannot start Transaction when one is already active.");
        } else {
            if (factory.supportsTransactions()) {
                try {
                    getActiveConnection();
                    activeConnection.setTransactionIsolation(transactionIsolation);
                    activeConnection.setAutoCommit(false);
                } catch (SQLException se) {
                    releaseActiveConnection();
                    inTransaction = false;
                    throw new StorageException(se);
                }
            }
            inTransaction = true;
            changes = new HashMap();
        }

    }

    // javadoc is inherited
    public void commit() throws StorageException {
        if (!inTransaction) {
            throw new StorageException("No transaction started.");
        } else {
            inTransaction = false;
            if (factory.supportsTransactions()) {
                if (activeConnection == null) {
                    throw new StorageException("No active connection");
                }

                try {
                    activeConnection.commit();
                } catch (SQLException se) {
                    throw new StorageException(se);
                } finally {
                    releaseActiveConnection();
                    factory.getChangeManager().commit(changes);
                }
            }
        }
    }

    // javadoc is inherited
    public boolean rollback() throws StorageException {
        if (!inTransaction) {
            throw new StorageException("No transaction started.");
        } else {
            inTransaction = false;
            if (factory.supportsTransactions()) {
                try {
                    activeConnection.rollback();
                } catch (SQLException se) {
                    throw new StorageException(se);
                } finally {
                    releaseActiveConnection();
                    changes.clear();
                }
            }
            return factory.supportsTransactions();
        }
    }

    /**
     * Commits the change to a node.
     * If the manager is in a transaction (and supports it), the change is stored in a
     * {@link #changes} object (to be committed after the transaction ends).
     * Otherwise it directly commits and broadcasts the changes
     * @param node the node to register
     * @param change the type of change: "n": new, "c": commit, "d": delete, "r" : relation changed
     */
    protected void commitChange(MMObjectNode node, String change) {
        if (inTransaction && factory.supportsTransactions()) {
            changes.put(node, change);
        } else {
            factory.getChangeManager().commit(node, change);
        }
    }

    public int createKey() throws StorageException {
        synchronized (sequenceKeys) {
            // if sequenceKeys conatins (buffered) keys, return this
            if (sequenceKeys.size() > 0) {
                return ((Integer)sequenceKeys.remove(0)).intValue();
            } else try {
                getActiveConnection();
                Statement s;
                String query;
                Scheme scheme = factory.getScheme(Schemes.UPDATE_SEQUENCE, Schemes.UPDATE_SEQUENCE_DEFAULT);
                if (scheme != null) {
                    query = scheme.format(new Object[] { this, factory.getStorageIdentifier("number"), bufferSize });
                    logQuery(query);
                    s = activeConnection.createStatement();
                    s.executeUpdate(query);
                    s.close();
                }
                scheme = factory.getScheme(Schemes.READ_SEQUENCE, Schemes.READ_SEQUENCE_DEFAULT);
                query = scheme.format(new Object[] { this, factory.getStorageIdentifier("number"), bufferSize });
                logQuery(query);
                s = activeConnection.createStatement();
                try {
                    ResultSet result = s.executeQuery(query);
                    try {
                        if (result.next()) {
                            int keynr = result.getInt(1);
                            // add remaining keys to sequenceKeys
                            for (int i = 1; i < bufferSize.intValue(); i++) {
                                sequenceKeys.add(new Integer(keynr+i));
                            }
                            return keynr;
                        } else {
                            throw new StorageException("The sequence table is empty.");
                        }
                    } finally {
                        result.close();
                    }
                } finally {
                    s.close();
                }
            } catch (SQLException se) {
                log.error(Logging.stackTrace(se));
                // wait 2 seconds, so any locks that were clainmed are released.
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException re) {}
                throw new StorageException(se);
            } finally {
                releaseActiveConnection();
            }
        }
    }

    // javadoc is inherited
    public String getStringValue(MMObjectNode node, FieldDefs field) throws StorageException {
        try {
            MMObjectBuilder builder = node.getBuilder();
            Scheme scheme = factory.getScheme(Schemes.GET_TEXT_DATA, Schemes.GET_TEXT_DATA_DEFAULT);
            String query = scheme.format(new Object[] { this, builder, field, builder.getField("number"), node });
            getActiveConnection();
            Statement s = activeConnection.createStatement();
            ResultSet result = s.executeQuery(query);
            try {
                if ((result != null) && result.next()) {
                    String rvalue = getStringValue(result, 1, field);
                    result.close();
                    s.close();
                    return rvalue;
                } else {
                    if (result != null) result.close();
                    s.close();
                    throw new StorageException("Node with number " + node.getNumber() + " not found.");
                }
            } finally {
                result.close();
            }
        } catch (SQLException se) {
            throw new StorageException(se);
        } finally {
            releaseActiveConnection();
        }
    }

    /**
     * Retrieve a text for a specified object field.
     * The default method uses {@link ResultSet#getString(int)} to obtain text.
     * Override this method if you want to optimize retrieving large texts,
     * i.e by using clobs or streams.

     * @param result the resultset to retrieve the text from
     * @param index the index of the text in the resultset
     * @param field the (MMBase) fieldtype. This value can be null
     * @return the retrieved text, <code>null</code> if no text was stored
     * @throws SQLException when a database error occurs
     * @throws StorageException when data is incompatible or the function is not supported
     */
    protected String getStringValue(ResultSet result, int index, FieldDefs field) throws StorageException, SQLException {
        String untrimmedResult = null;
        if (field.getStorageType() == Types.CLOB || field.getStorageType() == Types.BLOB || factory.hasOption(Attributes.FORCE_ENCODE_TEXT)) {
            InputStream inStream = result.getBinaryStream(index);
            if (result.wasNull()) {
                return null;
            }
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                int c = inStream.read();
                while (c != -1) {
                    bytes.write(c);
                    c = inStream.read();
                }
                inStream.close();
                untrimmedResult = new String(bytes.toByteArray(), factory.getMMBase().getEncoding());
            } catch (IOException ie) {
                throw new StorageException(ie);
            }
        } else {
            untrimmedResult = result.getString(index);
        }
        if(untrimmedResult!=null && factory.hasOption(Attributes.TRIM_STRINGS)) {
             return untrimmedResult.trim();
        }
        return untrimmedResult;
    }

    /*
     * Retrieve the XML (as a string) for a specified object field.
     * The default method uses {@link ResultSet#getString(int)} to obtain text.
     * Unlike
     * Override this method if you want to optimize retrieving large texts,
     * i.e by using clobs or streams.

     * @param result the resultset to retrieve the xml from
     * @param index the index of the xml in the resultset
     * @param field the (MMBase) fieldtype. This value can be null
     * @return the retrieved xml as text, <code>null</code> if nothing was stored
     * @throws SQLException when a database error occurs
     * @throws StorageException when data is incompatible or the function is not supported
     */
     protected String getXMLValue(ResultSet result, int index, FieldDefs field) throws StorageException, SQLException {
         return getStringValue(result, index, field);
     }




    /**
     * Determine whether a field (such as a large text or a blob) should be shortened or not.
     * A 'shortened' field contains a placeholder text ('$SHORTED') to indicate that the field is expected to be of large size
     * and should be retrieved by an explicit call to {@link #getStringValue(MMObjectNode, FieldDefs)} or.
     * {@link #getBinaryValue(MMObjectNode, FieldDefs)}.
     * The default implementation returns <code>true</code> for binaries, and <code>false</code> for other
     * types.
     * Override this method if you want to be able to change the placeholder strategy.
     * @param field the (MMBase) fieldtype
     * @return <code>true</code> if the field should be shortened
     * @throws SQLException when a database error occurs
     * @throws StorageException when data is incompatible or the function is not supported
     */
    protected boolean shorten(FieldDefs field) throws StorageException, SQLException {
        return field.getDBType() == FieldDefs.TYPE_BYTE;
    }

    /**
     * Read a binary (blob) from a field in the database
     * @param node the node the binary data belongs to
     * @param field the binary field
     * @return the byte array containing the binary data, <code>null</code> if no binary data was stored
     */
    protected byte[] readBinaryFromDatabase(MMObjectNode node, FieldDefs field) {
        try {
            MMObjectBuilder builder = node.getBuilder();
            Scheme scheme = factory.getScheme(Schemes.GET_BINARY_DATA, Schemes.GET_BINARY_DATA_DEFAULT);
            String query = scheme.format(new Object[] { this, builder, field, builder.getField("number"), node });
            getActiveConnection();
            Statement s = activeConnection.createStatement();
            ResultSet result = s.executeQuery(query);
            try {
                if ((result != null) && result.next()) {
                    byte[] retval = getBinaryValue(result, 1, field);
                    result.close();
                    s.close();
                    return retval;
                } else {
                    if (result != null) result.close();
                    s.close();
                    throw new StorageException("Node with number " + node.getNumber() + " not found.");
                }
            } finally {
                result.close();
            }
        } catch (SQLException se) {
            throw new StorageException(se);
        } finally {
            releaseActiveConnection();
        }
    }

    // javadoc is inherited
    public byte[] getBinaryValue(MMObjectNode node, FieldDefs field) throws StorageException {
        if (factory.hasOption(Attributes.STORES_BINARY_AS_FILE)) {
            return readBinaryFromFile(node, field);
        } else
            return readBinaryFromDatabase(node, field);
    }

    /**
     * Retrieve a large binary object (byte array) for a specified object field.
     * The default method uses {@link ResultSet#getBytes(int)} to obtain text.
     * Override this method if you want to optimize retrieving large objects,
     * i.e by using clobs or streams.
     * @param result the resultset to retrieve the text from
     * @param index the index of the text in the resultset
     * @param field the (MMBase) fieldtype. This value can be null
     * @return the retrieved data, <code>null</code> if no binary data was stored
     * @throws SQLException when a database error occurs
     * @throws StorageException when data is incompatible or the function is not supported
     */
    protected byte[] getBinaryValue(ResultSet result, int index, FieldDefs field) throws StorageException, SQLException {
        if (factory.hasOption(Attributes.SUPPORTS_BLOB)) {
            Blob blob = result.getBlob(index);
            if (result.wasNull()) {
                return null;
            }
            return blob.getBytes(1, (int)blob.length());
        } else {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                InputStream inStream = result.getBinaryStream(index);
                if (result.wasNull()) {
                    return null;
                }
                int c = inStream.read();
                while (c != -1) {
                    bytes.write(c);
                    c = inStream.read();
                }
                inStream.close(); // this also closes the underlying stream
                return bytes.toByteArray();
            } catch (IOException ie) {
                throw new StorageException(ie);
            }
        }
    }

    /**
     * Defines how binary (blob) data files must look like.
     * @param node the node the binary data belongs to
     * @param fieldName the name of the binary field
     * @return The File where to store or read the binary data
     */
    protected File getBinaryFile(MMObjectNode node, String fieldName) {
        String basePath = factory.getBinaryFileBasePath();
        StringBuffer pathBuffer = new StringBuffer();
        int number = node.getNumber() / 1000;
        while (number > 0) {
            int num = number % 100;
            pathBuffer.insert(0, num);
            if (num < 10) {
                pathBuffer.insert(0, 0);
            }
            pathBuffer.insert(0, File.separator);
            number /= 100;
        }
        pathBuffer.insert(0, basePath + File.separator + factory.getCatalog() + File.separator + node.getBuilder().getFullTableName());
        return new File(pathBuffer.toString(), "" + node.getNumber() + '.' + fieldName);
    }

    /**
     * Tries legacy paths
     * @returns such a File if found and readable, 'null' otherwise.
     */
    private File getLegacyBinaryFile(MMObjectNode node, String fieldName) {
        // the same basePath, so you so need to set that up right.
        String basePath = factory.getBinaryFileBasePath();

        File f = new File(basePath, node.getBuilder().getTableName() + File.separator + node.getNumber() + '.' + fieldName);
        if (f.exists()) { // 1.6 storage or 'support' blobdatadir
            if (!f.canRead()) {
                log.warn("Found '" + f + "' but it cannot be read");
            } else {
                return f;
            }
        }

        f = new File(basePath + File.separator + factory.getCatalog() + File.separator + node.getBuilder().getFullTableName() + File.separator + node.getNumber() + '.' + fieldName);
        if (f.exists()) { // 1.7.0.rc1 blob data dir
            if (!f.canRead()) {
                log.warn("Found '" + f + "' but it cannot be read");
            } else {
                return f;
            }
        }

        // don't know..
        return null;

    }

    /**
     * Store a binary (blob) data file
     * @todo how to do this in a transaction???
     * @param node the node the binary data belongs to
     * @param field the binary field
     */
    protected void storeBinaryAsFile(MMObjectNode node, FieldDefs field) throws StorageException {
        try {
            String fieldName = field.getDBName();
            File binaryFile = getBinaryFile(node, fieldName);
            binaryFile.getParentFile().mkdirs(); // make sure all directory exist.
            Object value = node.getValue(fieldName);

            if (value instanceof byte[]) {
                DataOutputStream byteStream = new DataOutputStream(new FileOutputStream(binaryFile));
                byteStream.write((byte[])value);
                byteStream.flush();
                byteStream.close();
            }
        } catch (IOException ie) {
            throw new StorageException(ie);
        }
    }

    /**
     * Checks whether file is readable and existing. Warns if not.
     * If non-existing it checks older locations.
     * @return the file to be used, or null if no existing readable file could be found, also no 'legacy' one.
     */

    protected File checkFile(File binaryFile, MMObjectNode node, FieldDefs field) {
        String fieldName = field.getDBName();
        if (!binaryFile.canRead()) {
            String desc = "while it should contain the byte data for node '" + node.getNumber() + "' field '" + fieldName + "'. Returning null.";
            if (!binaryFile.exists()) {
                // try legacy
                File legacy = getLegacyBinaryFile(node, fieldName);
                if (legacy == null) {
                    if (!binaryFile.getParentFile().exists()) {
                        log.warn("The file '" + binaryFile + "' does not exist, " + desc);
                        log.info(
                            "If you upgraded from older MMBase version, it might be that the blobs were stored on a different location. Make sure your blobs are in '"
                                + factory.getBinaryFileBasePath()
                                + "' (perhaps use symlinks?). If you changed configuration to 'blobs-on-disk' while it was blobs-in-database. Go to admin-pages.");

                    } else if (log.isDebugEnabled()) {
                        log.debug("The file '" + binaryFile + "' does not exist. Probably the blob field is simply 'null'");
                    }
                } else {
                    if (!legacyWarned) {
                        log.warn("Using the legacy location '" + legacy + "' rather then '" + binaryFile + "'. You might want to convert this dir.");
                        legacyWarned = true;
                    }
                    return legacy;
                }
            } else {
                log.error("The file '" + binaryFile + "' can not be read, " + desc);
            }
            return null;
        } else {
            return binaryFile;
        }
    }

    /**
     * Read a binary (blob) data file
     * @todo how to do this in a transaction???
     * @param node the node the binary data belongs to
     * @param field the binary field
     * @return the byte array containing the binary data, <code>null</code> if no binary data was stored
     */
    protected byte[] readBinaryFromFile(MMObjectNode node, FieldDefs field) throws StorageException {
        try {
            String fieldName = field.getDBName();
            File binaryFile = checkFile(getBinaryFile(node, fieldName), node, field);
            if (binaryFile == null) {
                return null;
            }
            int fileSize = (int)binaryFile.length();
            byte[] buffer = new byte[fileSize];
            if (fileSize > 0) {
                FileInputStream byteStream = new FileInputStream(binaryFile);
                int len = byteStream.read(buffer, 0, fileSize);
                byteStream.close();
            }
            return buffer;
        } catch (IOException ie) {
            throw new StorageException(ie);
        }
        //TODO: find you why is this code only here and not in other methods/
    }

    // javadoc is inherited
    public int create(MMObjectNode node) throws StorageException {
        // assign a new number if the node has not yet been assigned one
        int nodeNumber = node.getNumber();
        if (nodeNumber == -1) {
            nodeNumber = createKey();
            node.setValue("number", nodeNumber);
        }
        MMObjectBuilder builder = node.getBuilder();
        // precommit call, needed to convert or add things before a save
        // Should be done in MMObjectBuilder
        builder.preCommit(node);
        create(node, builder);
        commitChange(node, "n");
        return nodeNumber;
    }

    /**
     * This method inserts a new object in a specific builder, and registers the change.
     * This method makes it easier to implement relational databases, where you may need to update the node
     * in more than one builder.
     * Call this method for all involved builders if you use a relational database.
     * @param node The node to insert. The node already needs to have a (new) number assigned
     * @param builder the builder to store the node
     * @throws StorageException if an error occurred during creation
     */
    protected void create(MMObjectNode node, MMObjectBuilder builder) throws StorageException {
        // Create a String that represents the fields and values to be used in the insert.
        StringBuffer fieldNames = null;
        StringBuffer fieldValues = null;
        // get a builders fields
        List builderFields = builder.getFields(FieldDefs.ORDER_CREATE);
        List fields = new ArrayList();
        for (Iterator f = builderFields.iterator(); f.hasNext();) {
            FieldDefs field = (FieldDefs)f.next();
            if (field.inStorage()) {
                // skip bytevalues that are written to file
                if (factory.hasOption(Attributes.STORES_BINARY_AS_FILE) && (field.getDBType() == FieldDefs.TYPE_BYTE)) {
                    storeBinaryAsFile(node, field);
                    // do not handle this field further
                } else {
                    // store the fieldname and the value parameter
                    fields.add(field);
                    String fieldName = (String)factory.getStorageIdentifier(field);
                    if (fieldNames == null) {
                        fieldNames = new StringBuffer(fieldName);
                        fieldValues = new StringBuffer("?");
                    } else {
                        fieldNames.append(',').append(fieldName);
                        fieldValues.append(",?");
                    }
                }
            }
        }
        if (fields.size() > 0) {
            Scheme scheme = factory.getScheme(Schemes.INSERT_NODE, Schemes.INSERT_NODE_DEFAULT);
            try {
                String query = scheme.format(new Object[] { this, builder, fieldNames.toString(), fieldValues.toString()});
                getActiveConnection();
                executeUpdateCheckConnection(query, node, fields);
            } catch (SQLException se) {
                throw new StorageException(se);
            } finally {
                releaseActiveConnection();
            }
        }
    }

    /**
     * Executes an update query for given node and fields. It will close the connection which are no
     * good, which it determins by trying "SELECT 1 FROM <OBJECT TABLE>" after failure. If that happens, the connection
     * is explicitely closed (in case the driver has not done that), which will render is unusable
     * and at least GenericDataSource will automaticly try to get new ones.
     *
     * @throws SQLException If something wrong with the query, or the database is down or could not be contacted.
     * @since MMBase-1.7.1
     */
    protected void executeUpdateCheckConnection(String query, MMObjectNode node,  List fields) throws SQLException {
        try {
            executeUpdate(query, node, fields);
        } catch (SQLException sqe) {
            while (true) {
                Statement s = null;
                ResultSet rs = null;
                try {
                    s = activeConnection.createStatement();
                    rs = s.executeQuery("SELECT 1 FROM " + factory.getMMBase().getBuilder("object").getFullTableName() + " WHERE 1 = 0"); // if this goes wrong too it can't be the query
                } catch (SQLException isqe) {
                    // so, connection must be broken.
                    log.service("Found broken connection, closing it");
                    if (activeConnection instanceof org.mmbase.module.database.MultiConnection) {
                        ((org.mmbase.module.database.MultiConnection) activeConnection).realclose();
                    } else {
                        activeConnection.close();
                    }
                    getActiveConnection();
                    if (activeConnection.isClosed()) {
                        // don't know if that can happen, but if it happens, this would perhaps avoid an infinite loop (and exception will get thrown in stead)
                        break;
                    }
                    continue;
                 } finally {
                     if (s != null) s.close();
                     if (rs != null) rs.close();
                 }
                break;
            }
            executeUpdate(query, node, fields);
        }
    }

    /**
     * Executes an update query for given node and fields.  This is wrapped in a function because it
     * is repeatedly called in {@link #executeUpdateCheckConnection} which in turn is called from
     * several spots in this class.
     *
     * @since MMBase-1.7.1
     */
    protected void executeUpdate(String query, MMObjectNode node, List fields) throws SQLException {
        PreparedStatement ps = activeConnection.prepareStatement(query);
        for (int fieldNumber = 0; fieldNumber < fields.size(); fieldNumber++) {
            FieldDefs field = (FieldDefs)fields.get(fieldNumber);
            setValue(ps, fieldNumber + 1, node, field);
        }
        logQuery(query);
        ps.executeUpdate();
        ps.close();

    }

    // javadoc is inherited
    public void change(MMObjectNode node) throws StorageException {
        MMObjectBuilder builder = node.getBuilder();
        // precommit call, needed to convert or add things before a save
        // Should be done in MMObjectBuilder
        builder.preCommit(node);
        change(node, builder);
        commitChange(node, "c");
    }

    /**
     * Change this node in the specified builder.
     * This method makes it easier to implement relational databses, where you may need to update the node
     * in more than one builder.
     * Call this method for all involved builders if you use a relational database.
     * @param node The node to change
     * @param builder the builder to store the node
     * @throws StorageException if an error occurred during change
     */
    protected void change(MMObjectNode node, MMObjectBuilder builder) throws StorageException {
        // Create a String that represents the fields to be used in the commit
        StringBuffer setFields = null;
        // obtain the node's changed fields
        List fieldNames = node.getChanged();
        List fields = new ArrayList();
        for (Iterator f = fieldNames.iterator(); f.hasNext();) {
            String key = (String)f.next();
            // changing number is not allowed
            if (key.equals("number") || key.equals("otype")) {
                throw new StorageException("trying to change the '" + key + "' field");
            }
            FieldDefs field = builder.getField(key);
            if ((field != null) && field.inStorage()) {
                // skip bytevalues that are written to file
                if (factory.hasOption(Attributes.STORES_BINARY_AS_FILE) && (field.getDBType() == FieldDefs.TYPE_BYTE)) {
                    storeBinaryAsFile(node, field);
                } else {
                    // handle this field - store it in fields
                    fields.add(field);
                    // store the fieldname and the value parameter
                    String fieldName = (String)factory.getStorageIdentifier(field);
                    if (setFields == null) {
                        setFields = new StringBuffer(fieldName + "=?");
                    } else {
                        setFields.append(',').append(fieldName).append("=?");
                    }
                }
            }
        }
        if (fields.size() > 0) {
            Scheme scheme = factory.getScheme(Schemes.UPDATE_NODE, Schemes.UPDATE_NODE_DEFAULT);
            try {
                String query = scheme.format(new Object[] { this, builder, setFields.toString(), builder.getField("number"), node });
                getActiveConnection();
                executeUpdateCheckConnection(query, node, fields);
            } catch (SQLException se) {
                throw new StorageException(se);
            } finally {
                releaseActiveConnection();
            }
        }
    }


    /**
     * Store the value of a field in a prepared statement
     * @todo Note that this code contains some code that should really be implemented in FieldDefs.
     * In particular, casting should be done in FieldDefs, IMO.
     * @param statement the prepared statement
     * @param index the index of the field in the prepared statement
     * @param node the node from which to retrieve the value
     * @param field the MMBase field, containing meta-information
     * @throws StorageException if the fieldtype is invalid, or data is invalid or missing
     * @throws SQLException if an error occurred while filling in the fields
     */
    protected void setValue(PreparedStatement statement, int index, MMObjectNode node, FieldDefs field) throws StorageException, SQLException {
        String fieldName = field.getDBName();
        Object value = node.getValue(fieldName);
        switch (field.getDBType()) {
            // Store numeric values
        case FieldDefs.TYPE_INTEGER :
        case FieldDefs.TYPE_FLOAT :
        case FieldDefs.TYPE_DOUBLE :
        case FieldDefs.TYPE_LONG :
            setNumericValue(statement, index, value, field);
            break;
        case FieldDefs.TYPE_BOOLEAN :
            setBooleanValue(statement, index, value, field);
        case FieldDefs.TYPE_DATETIME :
            setDateTimeValue(statement, index, value, field);
            break;
            // Store nodes
        case FieldDefs.TYPE_NODE :
            // cannot do getNodeValue here because that might cause a new connection to be needed -> deadlocks
            setNodeValue(statement, index, value, field);
            break;
            // Store strings
        case FieldDefs.TYPE_XML :
            setXMLValue(statement, index, value, field);
            break;
        case FieldDefs.TYPE_STRING :
            // note: do not use getStringValue, as this may attempt to
            // retrieve a (old, or nonexistent) value from the storage
            setStringValue(statement, index, value, field);
            break;
            // Store binary data
        case FieldDefs.TYPE_BYTE : {
            // note: do not use getByteValue, as this may attempt to
            // retrieve a (old, or nonexistent) value from the storage
            setBinaryValue(statement, index, value, field);
            break;
        }
        case FieldDefs.TYPE_LIST : {
            setListValue(statement, index, value, field);
            break;
        }
        default :    // unknown field type - error
            throw new StorageException("unknown fieldtype");
        }
    }


    /**
     * Stores the 'null' value in the statement if appopriate (the value is null or unset, and the
     * value may indeed be NULL, according to the configuration). If the value is null or unset,
     * but the value may not be NULL, then -1 is stored.
     * @param statement the prepared statement
     * @param index the index of the field in the prepared statement
     * @param value the numeric value to store, which will be checked for null.
     * @param field the MMBase field, containing meta-information
     * @param type  java.sql.Type constant
     * @throws StorageException if the data is invalid or missing
     * @throws SQLException if an error occurred while filling in the fields
     * @return true if a null value was set, false otherwise
     * @since MMBase-1.7.1
     */
    protected boolean setNullValue(PreparedStatement statement, int index, Object value, FieldDefs field, int type) throws StorageException, SQLException {
        boolean mayBeNull = ! field.getDBNotNull();
        if (value == null) { // value unset
            if (mayBeNull) {
                statement.setNull(index, type);
                return true;
            }
        } else if (value == MMObjectNode.VALUE_NULL) { // value explicitely set to 'null'
            if (mayBeNull) {
                statement.setNull(index, type);
                return true;
            } else {
                log.warn("Tried to set 'null' in field '" + field.getDBName() + "' but the field is 'NOT NULL', it will be casted.");
            }
        }
        return false;
    }

    /**
     * Store a numeric value of a field in a prepared statement
     * The method uses the Casting class to convert to the appropriate value.
     * Null values are stored as NULL if possible, otherwise they are stored as -1.
     * Override this method if you want to override this behavior.
     * @param statement the prepared statement
     * @param index the index of the field in the prepared statement
     * @param value the numeric value to store. This may be a String, MMObjectNode, Numeric, or other value - the
     *        method will convert it to the appropriate value.
     * @param field the MMBase field, containing meta-information
     * @throws StorageException if the data is invalid or missing
     * @throws SQLException if an error occurred while filling in the fields
     */
    protected void setNumericValue(PreparedStatement statement, int index, Object value, FieldDefs field) throws StorageException, SQLException {
        // Store integers, floats, doubles and longs
        switch (field.getDBType()) { // it does this switch part twice now?
        case FieldDefs.TYPE_INTEGER :
            if (setNullValue(statement, index, value, field, java.sql.Types.INTEGER)) break;
            statement.setInt(index, Casting.toInt(value));
            break;
        case FieldDefs.TYPE_FLOAT :
            if (setNullValue(statement, index, value, field, java.sql.Types.REAL)) break;
            statement.setFloat(index, Casting.toFloat(value));
            break;
        case FieldDefs.TYPE_DOUBLE :
            if (setNullValue(statement, index, value, field, java.sql.Types.DOUBLE)) break;
            statement.setDouble(index, Casting.toDouble(value));
            break;
        case FieldDefs.TYPE_LONG :
            if (setNullValue(statement, index, value, field, java.sql.Types.BIGINT)) break;
            statement.setLong(index, Casting.toLong(value));
            break;
        }
    }


    /**
     * Store a node value of a field in a prepared statement
     * Nodes are stored in the database as numeric values.
     * Since a node value can be a (referential) key (depending on implementation),
     * Null values should be stored as NULL, not -1. If a field cannot be null when a
     * value is not given, an exception is thrown.
     * Override this method if you want to override this behavior.
     * @param statement the prepared statement
     * @param index the index of the field in the prepared statement
     * @param node the node to store
     * @param field the MMBase field, containing meta-information
     * @throws StorageException if the data is invalid or missing
     * @throws SQLException if an error occurred while filling in the fields
     */
    protected void setNodeValue(PreparedStatement statement, int index, Object node, FieldDefs field) throws StorageException, SQLException {
        if (setNullValue(statement, index, node, field, java.sql.Types.INTEGER)) return;

        int nodeNumber = Casting.toInt(node);

        if (nodeNumber < 0 && field.getDBNotNull()) { // node numbers cannot be negative
            throw new StorageException("The NODE field with name " + field.getDBName() + " of type " + field.getParent().getTableName() + " can not be NULL.");
        }

        // retrieve node as a numeric value
        statement.setInt(index, nodeNumber);
    }

    /**
     * @since MMBase-1.8
     */
    protected void setBooleanValue(PreparedStatement statement, int index, Object value, FieldDefs field) throws StorageException, SQLException {
        if (setNullValue(statement, index, value, field, java.sql.Types.BOOLEAN)) return;
        boolean bool = Casting.toBoolean(value);
        statement.setBoolean(index, bool);
    }


    /**
     * @since MMBase-1.8
     */
    protected void setDateTimeValue(PreparedStatement statement, int index, Object value, FieldDefs field) throws StorageException, SQLException {
        if (setNullValue(statement, index, value, field, java.sql.Types.TIMESTAMP)) return;
        java.util.Date date = Casting.toDate(value);
        statement.setTimestamp(index, new Timestamp(date.getTime()));
    }
    /**
     * @since MMBase-1.8
     */
    protected void setListValue(PreparedStatement statement, int index, Object value, FieldDefs field) throws StorageException, SQLException {
        if (setNullValue(statement, index, value, field, java.sql.Types.BOOLEAN)) return;
        List list = Casting.toList(value);
        statement.setObject(index, list);
    }

    /**
     * Store binary data of a field in a prepared statement.
     * This basic implementation uses a binary stream to set the data.
     * Null values are stored as NULL if possible, otherwise they are stored as an empty byte-array.
     * Override this method if you use another way to store binaries (i.e. Blobs).
     * @param statement the prepared statement
     * @param index the index of the field in the prepared statement
     * @param objectValue the data (byte array) to store
     * @param field the MMBase field, containing meta-information. This value can be null
     * @throws StorageException if the data is invalid or missing
     * @throws SQLException if an error occurred while filling in the fields
     */
    protected void setBinaryValue(PreparedStatement statement, int index, Object objectValue, FieldDefs field) throws StorageException, SQLException {
        if (setNullValue(statement, index, objectValue, field, java.sql.Types.VARBINARY)) return;
        byte[] value = Casting.toByte(objectValue);

        try {
            InputStream stream = new ByteArrayInputStream(value);
            statement.setBinaryStream(index, stream, value.length);
            stream.close();
        } catch (IOException ie) {
            throw new StorageException(ie);
        }
    }

    /**
     * Store the text value of a field in a prepared statement.
     * Null values are stored as NULL if possible, otherwise they are stored as an empty string.
     * If the FORCE_ENCODE_TEXT option is set, text is encoded (using the MMBase encoding) to a byte array
     * and stored as a binary stream.
     * Otherwise it uses {@link PreparedStatement#setString(int, String)} to set the data.
     * Override this method if you use another way to store large texts (i.e. Clobs).
     * @param statement the prepared statement
     * @param index the index of the field in the prepared statement
     * @param objectValue the text to store
     * @param field the MMBase field, containing meta-information
     * @throws StorageException if the data is invalid or missing
     * @throws SQLException if an error occurred while filling in the fields
     */
    protected void setStringValue(PreparedStatement statement, int index, Object objectValue, FieldDefs field) throws StorageException, SQLException {
        if (setNullValue(statement, index, objectValue, field, java.sql.Types.VARCHAR)) return;
        String value = Casting.toString(objectValue);

        // Store data as a binary stream when the code is a clob or blob, or
        // when database-force-encode-text is true.
        if (field.getStorageType() == Types.CLOB || field.getStorageType() == Types.BLOB || factory.hasOption(Attributes.FORCE_ENCODE_TEXT)) {
            byte[] rawchars = null;
            try {
                rawchars = value.getBytes(factory.getMMBase().getEncoding());
                ByteArrayInputStream stream = new ByteArrayInputStream(rawchars);
                statement.setBinaryStream(index, stream, rawchars.length);
                stream.close();
            } catch (IOException ie) {
                throw new StorageException(ie);
            }
        } else {
            statement.setString(index, value);
        }
    }

    /**
     * This default implementation calls {@link #setStringValue}.
     * Override this method if you want to override this behavior.
     * @since MMBase-1.7.1
     */
    protected void setXMLValue(PreparedStatement statement, int index, Object objectValue, FieldDefs field) throws StorageException, SQLException {
        setStringValue(statement, index, objectValue, field);
    }


    // javadoc is inherited
    public void delete(MMObjectNode node) throws StorageException {
        // determine parent
        if (node.hasRelations()) {
            throw new StorageException("cannot delete node " + node.getNumber() + ", it still has relations");
        }
        delete(node, node.getBuilder());
        commitChange(node, "d");
    }

    /**
     * Delete a node from a specific builder
     * This method makes it easier to implement relational databses, where you may need to remove the node
     * in more than one builder.
     * Call this method for all involved builders if you use a relational database.
     * @param node The node to delete
     * @throws StorageException if an error occurred during delete
     */
    protected void delete(MMObjectNode node, MMObjectBuilder builder) throws StorageException {
        try {
            Scheme scheme = factory.getScheme(Schemes.DELETE_NODE, Schemes.DELETE_NODE_DEFAULT);
            String query = scheme.format(new Object[] { this, builder, builder.getField("number"), node });
            getActiveConnection();
            Statement s = activeConnection.createStatement();
            logQuery(query);
            s.executeUpdate(query);
            s.close();
            // delete blob files too
            List builderFields = builder.getFields(FieldDefs.ORDER_CREATE);
            for (Iterator f = builderFields.iterator(); f.hasNext();) {
                FieldDefs field = (FieldDefs)f.next();
                if (field.inStorage()) {
                    if (factory.hasOption(Attributes.STORES_BINARY_AS_FILE) && (field.getDBType() == FieldDefs.TYPE_BYTE)) {
                        String fieldName = field.getDBName();
                        File binaryFile = checkFile(getBinaryFile(node, fieldName), node, field);
                        if (binaryFile == null) {
                            log.warn("Could not find blob for field '" + fieldName + "' of node " + node.getNumber());
                        } else if (! binaryFile.delete()) {
                            log.warn("Could not delete '" + binaryFile + "'");
                        } else {
                            log.debug("Deleted '" + binaryFile + "'");
                        }
                    }
                }
            }
        } catch (SQLException se) {
            throw new StorageException(se);
        } finally {
            releaseActiveConnection();
        }
    }

    // javadoc is inherited
    public MMObjectNode getNode(MMObjectBuilder builder, int number) throws StorageException {
        Scheme scheme = factory.getScheme(Schemes.SELECT_NODE, Schemes.SELECT_NODE_DEFAULT);
        try {
            // create a new node (must be done before acquiring the connection, because this code might need a connection)
            MMObjectNode node = builder.getNewNode("system");

            getActiveConnection();
            // get a builders fields
            List builderFields = builder.getFields(FieldDefs.ORDER_CREATE);
            List fields = new ArrayList();
            StringBuffer fieldNames = null;
            for (Iterator f = builderFields.iterator(); f.hasNext();) {
                FieldDefs field = (FieldDefs)f.next();
                if (field.inStorage() && !shorten(field)) {
                    // store the fieldname and the value parameter
                    String fieldName = (String)factory.getStorageIdentifier(field);
                    if (fieldNames == null) {
                        fieldNames = new StringBuffer(fieldName);
                    } else {
                        fieldNames.append(',').append(fieldName);
                    }
                }
            }
            String query = scheme.format(new Object[] { this, builder, fieldNames.toString(), builder.getField("number"), new Integer(number)});
            Statement s = activeConnection.createStatement();
            ResultSet result = null;
            try {
                result = s.executeQuery(query);
                fillNode(node, result, builder);
            } finally {
                if (result != null) result.close();
                s.close();
            }
            return node;
        } catch (SQLException se) {
            throw new StorageException(se);
        } finally {
            releaseActiveConnection();
        }
    }

    /**
     * Fills a single Node from the resultset of a query.
     * You can use this method to iterate through a query, creating multiple nodes, provided the resultset still contains
     * members (that is, <code>result.isAfterLast</code> returns <code>false</code>)
     * @param node The MMObjectNode to be filled
     * @param result the resultset
     * @param builder the builder to use for creating the node
     * @return void
     * @throws StorageException if the resultset is exhausted or a database error occurred
     */
    protected void fillNode(MMObjectNode node, ResultSet result, MMObjectBuilder builder) throws StorageException {
        try {
            if ((result != null) && result.next()) {

                // iterate through all a builder's fields, and retrieve the value for that field
                // Note that if we would do it the other way around (iterate through the recordset's fields)
                // we might get inconsistencies if we 'remap' fieldnames that need not be mapped.
                // this also guarantees the number field is set first, which we  may need when retrieving blobs
                // from disk
                for (Iterator i = builder.getFields(FieldDefs.ORDER_CREATE).iterator(); i.hasNext();) {
                    FieldDefs field = (FieldDefs)i.next();
                    if (field.getDBState() == FieldDefs.DBSTATE_PERSISTENT || field.getDBState() == FieldDefs.DBSTATE_SYSTEM) {
                        if (shorten(field)) {
                            node.setValue(field.getDBName(), "$SHORTED");
                        } else if (field.getDBType() == FieldDefs.TYPE_BYTE && factory.hasOption(Attributes.STORES_BINARY_AS_FILE)) {
                            node.setValue(field.getDBName(), readBinaryFromFile(node, field));
                        } else {
                            String id = (String)factory.getStorageIdentifier(field);
                            node.setValue(field.getDBName(), getValue(result, result.findColumn(id), field));
                        }
                    }
                }
                // clear the changed signal on the node
                node.clearChanged();
                return;
            } else {
                throw new StorageException("Node not found");
            }
        } catch (SQLException se) {
            throw new StorageException(se);
        }
    }

    /**
     * Attempts to return a single field value from the resultset of a query.
     * @param result the resultset
     * @param index the index of the field in the resultset
     * @param field the expected MMBase field type. This can be null
     * @return the value
     * @throws StorageException if the value cannot be retrieved from the resultset
     */
    protected Object getValue(ResultSet result, int index, FieldDefs field) throws StorageException {
        try {
            int dbtype = FieldDefs.TYPE_UNKNOWN;
            if (field != null) {
                dbtype = field.getDBType();
            }
            switch (dbtype) {
                // string-type fields
            case FieldDefs.TYPE_XML : {
                return getXMLValue(result, index, field);
            }
            case FieldDefs.TYPE_STRING : {
                return getStringValue(result, index, field);
            }
            case FieldDefs.TYPE_BYTE : {
                return getBinaryValue(result, index, field);
            }
            default : {
                return result.getObject(index);
            }
            }
        } catch (SQLException se) {
            throw new StorageException(se);
        }
    }

    // javadoc is inherited
    public int getNodeType(int number) throws StorageException {
        Scheme scheme = factory.getScheme(Schemes.SELECT_NODE_TYPE, Schemes.SELECT_NODE_TYPE_DEFAULT);
        try {
            getActiveConnection();
            MMBase mmbase = factory.getMMBase();
            String query = scheme.format(new Object[] { this, mmbase, mmbase.getTypeDef().getField("number"), new Integer(number)});
            Statement s = activeConnection.createStatement();
            try {
                ResultSet result = s.executeQuery(query);
                if (result != null) {
                    try {
                        if (result.next()) {
                            int retval = result.getInt(1);
                            return retval;
                        } else {
                            return -1;
                        }
                    } finally {
                        result.close();
                    }
                } else {
                    return -1;
                }
            } finally {
                s.close();
            }
        } catch (SQLException se) {
            throw new StorageException(se);
        } finally {
            releaseActiveConnection();
        }
    }

    /**
     * Returns whether tables inherit fields form parent tables.
     * this determines whether fields that are inherited in mmbase builders
     * are redefined in the database tables.
     */
    protected boolean tablesInheritFields() {
        return true;
    }

    /**
     * Determines whether the storage should make a field definition in a builder table for a
     * specified field.
     */
    protected boolean isPartOfBuilderDefinition(FieldDefs field) {
        // persistent field?
        // skip binary fields when values are written to file
        boolean isPart = field.inStorage() && (field.getDBType() != FieldDefs.TYPE_BYTE || !factory.hasOption(Attributes.STORES_BINARY_AS_FILE));
        // also, if the database is OO, and the builder has a parent,
        // skip fields that are in the parent builder
        MMObjectBuilder parentBuilder = field.getParent().getParentBuilder();
        if (isPart && parentBuilder != null) {
            isPart = !tablesInheritFields() || parentBuilder.getField(field.getDBName()) == null;
        }
        return isPart;
    }

    // javadoc is inherited
    public void create(MMObjectBuilder builder) throws StorageException {
        log.debug("Creating a table for " + builder);
        // use the builder to get the fields and create a
        // valid create SQL string
        // for backward compatibility, fields are to be created in the order defined
        List fields = builder.getFields(FieldDefs.ORDER_CREATE);
        log.debug("found fields " + fields);
        StringBuffer createFields = new StringBuffer();
        StringBuffer createIndices = new StringBuffer();
        StringBuffer createFieldsAndIndices = new StringBuffer();
        StringBuffer createCompositeIndices = new StringBuffer();
        List compositeIndices = new ArrayList();
        // obtain the parentBuilder
        MMObjectBuilder parentBuilder = builder.getParentBuilder();
        Scheme rowtypeScheme;
        Scheme tableScheme;
        // if the builder has no parent, it is an object table,
        // so use CREATE_OBJECT_ROW_TYPE and CREATE_OBJECT_TABLE schemes.
        // Otherwise use CREATE_ROW_TYPE and CREATE_TABLE schemes.
        //
        if (parentBuilder == null) {
            rowtypeScheme = factory.getScheme(Schemes.CREATE_OBJECT_ROW_TYPE);
            tableScheme = factory.getScheme(Schemes.CREATE_OBJECT_TABLE, Schemes.CREATE_OBJECT_TABLE_DEFAULT);
        } else {
            rowtypeScheme = factory.getScheme(Schemes.CREATE_ROW_TYPE);
            tableScheme = factory.getScheme(Schemes.CREATE_TABLE, Schemes.CREATE_TABLE_DEFAULT);
        }
        for (Iterator f = fields.iterator(); f.hasNext();) {
            FieldDefs field = (FieldDefs)f.next();
            // convert a fielddef to a field SQL createdefinition
            if (isPartOfBuilderDefinition(field)) {
                String fieldDef = getFieldDefinition(field);
                if (createFields.length() > 0) {
                    createFields.append(", ");
                }
                createFields.append(fieldDef);
                // test on other indices
                String constraintDef = getConstraintDefinition(field);
                if (constraintDef != null) {
                    // note: the indices are prefixed with a comma, as they generally follow the fieldlist.
                    // if the database uses rowtypes, however, fields are not included in the CREATE TABLE statement,
                    // and the comma should not be prefixed.
                    if (rowtypeScheme == null || createIndices.length() > 0) {
                        createIndices.append(", ");
                    }

                    createIndices.append(constraintDef);
                    if (createFieldsAndIndices.length() > 0) {
                        createFieldsAndIndices.append(", ");
                    }
                    createFieldsAndIndices.append(fieldDef + ", " + constraintDef);
                } else {
                    if (createFieldsAndIndices.length() > 0) {
                        createFieldsAndIndices.append(", ");
                    }
                    createFieldsAndIndices.append(fieldDef);
                }
            }
        }
        //  composite constraints

        String compConstraintDef = getCompositeConstraintDefinition(builder);
        if (compConstraintDef != null) {
            // note: the indices are prefixed with a comma, as they generally follow the fieldlist.
            // if the database uses rowtypes, however, fields are not included in the CREATE TABLE statement,
            // and the comma should not be prefixed.
            if (rowtypeScheme == null || createIndices.length() > 0) {
                createCompositeIndices.append(", ");
            }
            createCompositeIndices.append(compConstraintDef);
        }
        try {
            getActiveConnection();
            // create a rowtype, if a scheme has been given
            // Note that creating a rowtype is optional
            if (rowtypeScheme != null) {
                String query = rowtypeScheme.format(new Object[] { this, builder, createFields.toString(), parentBuilder });
                // remove parenthesis with empty field definitions -
                // unfortunately Schems don't take this into account
                if (factory.hasOption(Attributes.REMOVE_EMPTY_DEFINITIONS)) {
                    query = query.replaceAll("\\(\\s*\\)", "");
                }
                Statement s = activeConnection.createStatement();
                logQuery(query);
                s.executeUpdate(query);
                s.close();
            }
            // create the table
            String query = tableScheme.format(new Object[] { this, builder, createFields.toString(), createIndices.toString(), createFieldsAndIndices.toString(), createCompositeIndices.toString(), parentBuilder });
            // remove parenthesis with empty field definitions -
            // unfortunately Schemes don't take this into account
            if (factory.hasOption(Attributes.REMOVE_EMPTY_DEFINITIONS)) {
                query = query.replaceAll("\\(\\s*\\)", "");
            }

            Statement s = activeConnection.createStatement();
            logQuery(query);
            s.executeUpdate(query);
            s.close();
            // TODO: use CREATE_SECONDARY_INDEX to create indices for all fields that have one
            // has to be done seperate
        } catch (SQLException se) {
            log.error(Logging.stackTrace(se));
            throw new StorageException(se);
        } finally {
            releaseActiveConnection();
        }
        verify(builder);

        //create indices for key's that are not primary keys
        Scheme createIndex = factory.getScheme(Schemes.CREATE_INDEX, Schemes.CREATE_INDEX_DEFAULT);
        if (createIndex != null) {
            for (Iterator f = fields.iterator(); f.hasNext();) {
                FieldDefs field = (FieldDefs)f.next();
                if (
                    (field.getDBState() == FieldDefs.DBSTATE_PERSISTENT || field.getDBState() == FieldDefs.DBSTATE_SYSTEM) &&
                    field.getDBType() == FieldDefs.TYPE_NODE &&
                    ! field.getDBName().equals("number")) {
                    String query = createIndex.format(new Object[] { this, builder, field.getDBName()});
                    try {
                        getActiveConnection();
                        Statement s = activeConnection.createStatement();
                        logQuery(query);
                        s.executeUpdate(query);
                        s.close();
                    } catch (SQLException se) {
                        log.error(Logging.stackTrace(se));
                        throw new StorageException(se);
                    } finally {
                        releaseActiveConnection();
                    }
                }
            }
        }
    }

    /**
     * Creates a fielddefinition, of the format '[fieldname] [fieldtype] NULL' or
     * '[fieldname] [fieldtype] NOT NULL' (depending on whether the field is nullable).
     * The fieldtype is taken from the type mapping in the factory.
     * @param field the field
     * @return the typedefiniton as a String
     * @throws StorageException if the field type cannot be mapped
     */
    protected String getFieldDefinition(FieldDefs field) throws StorageException {
        // create the type mapping to search for
        String typeName = field.getDBTypeDescription();
        int size = field.getDBSize();
        TypeMapping mapping = new TypeMapping();
        mapping.name = typeName;
        mapping.setFixedSize(size);
        // search type mapping
        List typeMappings = factory.getTypeMappings();
        int found = typeMappings.indexOf(mapping);
        if (found > -1) {
            String fieldDef = factory.getStorageIdentifier(field) + " " + ((TypeMapping)typeMappings.get(found)).getType(size);
            if (field.getDBNotNull()) {
                fieldDef += " NOT NULL";
            }
            return fieldDef;
        } else {
            throw new StorageException("Type for field " + field.getDBName() + ": " + typeName + " (" + size + ") undefined.");
        }
    }

    /**
     * Creates an index definition string to be passed when creating a table.
     * @param field the field for which to make the index definition
     * @return the index definition as a String, or <code>null</code> if no definition is available
     */
    protected String getConstraintDefinition(FieldDefs field) throws StorageException {
        String definitions = null;
        Scheme scheme = null;
        if (field.getDBName().equals("number")) {
            scheme = factory.getScheme(Schemes.CREATE_PRIMARY_KEY, Schemes.CREATE_PRIMARY_KEY_DEFAULT);
            if (scheme != null) {
                definitions = scheme.format(new Object[] { this, field.getParent(), field });
            }
        } else {
            if (field.isKey() && !factory.hasOption(Attributes.SUPPORTS_COMPOSITE_INDEX)) {
                scheme = factory.getScheme(Schemes.CREATE_UNIQUE_KEY, Schemes.CREATE_UNIQUE_KEY_DEFAULT);
                if (scheme != null) {
                    definitions = scheme.format(new Object[] { this, field.getParent(), field, field });
                }
            }
            if (field.getDBType() == FieldDefs.TYPE_NODE) {
                scheme = factory.getScheme(Schemes.CREATE_FOREIGN_KEY, Schemes.CREATE_FOREIGN_KEY_DEFAULT);
                if (scheme != null) {
                    String definition = scheme.format(new Object[] { this, field.getParent(), field, factory.getMMBase(), factory.getStorageIdentifier("number")});
                    if (definitions != null) {
                        definitions += ", " + definition;
                    } else {
                        definitions = definition;
                    }
                }
            }
        }
        return definitions;
    }

    /**
     * Creates a composite index definition string (an index over one or more fields) to be
     * passed when creating a table.
     * @param builder The builder for which to make the index definition
     * @return the index definition as a String, or <code>null</code> if no definition is available
     */
    protected String getCompositeConstraintDefinition(MMObjectBuilder builder) throws StorageException {
        Scheme scheme = factory.getScheme(Schemes.CREATE_COMPOSITE_KEY, Schemes.CREATE_COMPOSITE_KEY_DEFAULT);
        if (scheme != null) {
            StringBuffer indices = new StringBuffer();
            List fields = builder.getFields(FieldDefs.ORDER_CREATE);
            List compositeIndices = new ArrayList();
            // obtain the parentBuilder
            MMObjectBuilder parentBuilder = builder.getParentBuilder();
            for (Iterator f = fields.iterator(); f.hasNext();) {
                FieldDefs field = (FieldDefs)f.next();
                if (isPartOfBuilderDefinition(field) && !"number".equals(field.getDBName()) && field.isKey() && factory.hasOption(Attributes.SUPPORTS_COMPOSITE_INDEX)) {
                    if (indices.length() > 0)
                        indices.append(", ");
                    indices.append(factory.getStorageIdentifier(field));
                }
            }
            if (indices.length() > 0) {
                return scheme.format(new Object[] { this, builder, indices.toString()});
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    // javadoc is inherited
    public void change(MMObjectBuilder builder) throws StorageException {
        // test if you can make changes
        // iterate through the fields,
        // use metadata.getColumns(...)  to select fields
        //      (incl. name, datatype, size, null)
        // use metadata.getImportedKeys(...) to get foreign keys
        // use metadata.getIndexInfo(...) to get composite and other indexes
        // determine changes and run them
        throw new StorageException("Operation not supported");
    }

    // javadoc is inherited
    public void delete(MMObjectBuilder builder) throws StorageException {
        int size = size(builder);
        if (size != 0) {
            throw new StorageException("Can not drop builder, it still contains " + size + " node(s)");
        }
        try {
            getActiveConnection();
            Scheme scheme = factory.getScheme(Schemes.DROP_TABLE, Schemes.DROP_TABLE_DEFAULT);
            String query = scheme.format(new Object[] { this, builder });
            Statement s = activeConnection.createStatement();
            logQuery(query);
            s.executeUpdate(query);
            s.close();
            scheme = factory.getScheme(Schemes.DROP_ROW_TYPE);
            if (scheme != null) {
                query = scheme.format(new Object[] { this, builder });
                s = activeConnection.createStatement();
                logQuery(query);
                s.executeUpdate(query);
                s.close();
            }
        } catch (Exception e) {
            throw new StorageException(e.getMessage());
        } finally {
            releaseActiveConnection();
        }
    }

    // javadoc is inherited
    public void create() throws StorageException {
        MMBase mmbase = factory.getMMBase();
        create(factory.getMMBase().getRootBuilder());
        createSequence();
    }

    /**
     * Creates a means for the database to pre-create keys with increasing numbers.
     * A sequence can be a database routine, a number table, or anything else that can be used to create unique numbers.
     * Keys can be obtained from the sequence by calling {@link #createKey()}.
     * @throws StorageException when the sequence can not be created
     */
    protected void createSequence() throws StorageException {
        synchronized (sequenceKeys) {
            try {
                getActiveConnection();
                // create the type mapping to search for
                String typeName = FieldDefs.getDBTypeDescription(FieldDefs.TYPE_INTEGER);
                TypeMapping mapping = new TypeMapping();
                mapping.name = typeName;
                // search type mapping
                List typeMappings = factory.getTypeMappings();
                int found = typeMappings.indexOf(mapping);
                if (found == -1) {
                    throw new StorageException("Type " + typeName + " undefined.");
                }
                String fieldName = (String)factory.getStorageIdentifier("number");
                String fieldDef = fieldName + " " + ((TypeMapping)typeMappings.get(found)).type + " NOT NULL, PRIMARY KEY(" + fieldName + ")";
                String query;
                Statement s;
                Scheme scheme = factory.getScheme(Schemes.CREATE_SEQUENCE, Schemes.CREATE_SEQUENCE_DEFAULT);
                if (scheme != null) {
                    query = scheme.format(new Object[] { this, fieldDef });
                    logQuery(query);
                    s = activeConnection.createStatement();
                    s.executeUpdate(query);
                    s.close();
                }
                scheme = factory.getScheme(Schemes.INIT_SEQUENCE, Schemes.INIT_SEQUENCE_DEFAULT);
                if (scheme != null) {
                    query = scheme.format(new Object[] { this, factory.getStorageIdentifier("number"), new Integer(1), bufferSize });
                    logQuery(query);
                    s = activeConnection.createStatement();
                    s.executeUpdate(query);
                    s.close();
                }
            } catch (SQLException se) {
                throw new StorageException(se);
            } finally {
                releaseActiveConnection();
            }
        }
    }

    // javadoc is inherited
    public boolean exists(MMObjectBuilder builder) throws StorageException {
        boolean result = exists((String)factory.getStorageIdentifier(builder));
        if (result) {
            verify(builder);
        }
        return result;
    }

    /**
     * Queries the database metadata to test whether a given table exists.
     * @param tableName name of the table to look for
     * @throws StorageException when the metadata could not be retrieved
     * @return <code>true</code> if the table exists
     */
    protected boolean exists(String tableName) throws StorageException {
        try {
            getActiveConnection();
            DatabaseMetaData metaData = activeConnection.getMetaData();
            ResultSet res = metaData.getTables(null, null, tableName, null);
            try {
                boolean result = res.next();
                return result;
            } finally {
                res.close();
            }
        } catch (Exception e) {
            throw new StorageException(e.getMessage());
        } finally {
            releaseActiveConnection();
        }
    }

    // javadoc is inherited
    public boolean exists() throws StorageException {
        return exists(factory.getMMBase().getRootBuilder());
    }

    // javadoc is inherited
    public int size(MMObjectBuilder builder) throws StorageException {
        try {
            getActiveConnection();
            Scheme scheme = factory.getScheme(Schemes.GET_TABLE_SIZE, Schemes.GET_TABLE_SIZE_DEFAULT);
            String query = scheme.format(new Object[] { this, builder });
            Statement s = activeConnection.createStatement();
            ResultSet res = s.executeQuery(query);
            int retval;
            try {
                res.next();
                retval = res.getInt(1);
            } finally {
                res.close();
            }
            s.close();
            return retval;
        } catch (Exception e) {
            throw new StorageException(e);
        } finally {
            releaseActiveConnection();
        }
    }

    // javadoc is inherited
    public int size() throws StorageException {
        return size(factory.getMMBase().getRootBuilder());
    }

    /**
     * Guess the (mmbase) type in storage using the JDNC type.
     * Because a JDBC type can represent more than one mmbase Type,
     * the current type is also passed - if the current type matches, that type
     * is returned, otherwise the method returns the closest matching MMBase type.
     */
    protected int getJDBCtoMMBaseType(int jdbcType, int mmbaseType) {
        switch (jdbcType) {
        case Types.INTEGER :
        case Types.SMALLINT :
        case Types.TINYINT :
        case Types.BIGINT :
            if (mmbaseType == FieldDefs.TYPE_INTEGER || mmbaseType == FieldDefs.TYPE_LONG || mmbaseType == FieldDefs.TYPE_NODE) {
                return mmbaseType;
            } else {
                return FieldDefs.TYPE_INTEGER;
            }
        case Types.FLOAT :
        case Types.REAL :
        case Types.DOUBLE :
        case Types.NUMERIC :
        case Types.DECIMAL :
            if (mmbaseType == FieldDefs.TYPE_FLOAT || mmbaseType == FieldDefs.TYPE_DOUBLE) {
                return mmbaseType;
            } else {
                return FieldDefs.TYPE_DOUBLE;
            }
        case Types.BINARY :
        case Types.LONGVARBINARY :
        case Types.VARBINARY :
        case Types.BLOB :
            if (mmbaseType == FieldDefs.TYPE_BYTE || mmbaseType == FieldDefs.TYPE_STRING || mmbaseType == FieldDefs.TYPE_XML) {
                return mmbaseType;
            } else {
                return FieldDefs.TYPE_BYTE;
            }
        case Types.CHAR :
        case Types.CLOB :
        case Types.LONGVARCHAR :
        case Types.VARCHAR :
            if (mmbaseType == FieldDefs.TYPE_STRING || mmbaseType == FieldDefs.TYPE_XML) {
                return mmbaseType;
            } else {
                return FieldDefs.TYPE_STRING;
            }
        case Types.BIT :
        case Types.BOOLEAN :
            return FieldDefs.TYPE_BOOLEAN;
        case Types.DATE :
        case Types.TIME :
        case Types.TIMESTAMP :
            return FieldDefs.TYPE_DATETIME;
        case Types.ARRAY :
            return FieldDefs.TYPE_LIST;
        case Types.JAVA_OBJECT :
        case Types.OTHER :
            if (mmbaseType == FieldDefs.TYPE_LIST) {
                return mmbaseType;
            }  else {
                return FieldDefs.TYPE_UNKNOWN;
            }
        default :
            return FieldDefs.TYPE_UNKNOWN;
        }
    }

    /**
     * Tests whether a builder and the table present in the database match.
     */
    public void verify(MMObjectBuilder builder) throws StorageException {
        try {
            getActiveConnection();
            String tableName = (String)factory.getStorageIdentifier(builder);
            DatabaseMetaData metaData = activeConnection.getMetaData();
            // skip if does not support inheritance, or if this is the object table
            if (tablesInheritFields()) {
                MMObjectBuilder parent = builder.getParentBuilder();
                try {
                    ResultSet superTablesSet = metaData.getSuperTables(null, null, tableName);
                    try {
                        if (superTablesSet.next()) {
                            String parentName = superTablesSet.getString("SUPERTABLE_NAME");
                            if (parent == null || !parentName.equals(factory.getStorageIdentifier(parent))) {
                                log.error("VERIFY: parent builder in storage for builder " + builder.getTableName() + " should be " + parent.getTableName() + " but defined as " + parentName);
                            } else {
                                log.debug("VERIFY: parent builder in storage for builder " + builder.getTableName() + " defined as " + parentName);
                            }
                        } else if (parent != null) {
                            log.error("VERIFY: no parent builder defined in storage for builder " + builder.getTableName());
                        }
                    } finally {
                        superTablesSet.close();
                    }
                } catch (AbstractMethodError ae) {
                    // ignore: the method is not implemented by the JDBC Driver
                    log.debug("VERIFY: Driver does not fully implement the JDBC 3.0 API, skipping inheritance consistency tests for " + tableName);
                } catch (UnsupportedOperationException uoe) {
                    // ignore: the operation is not supported by the JDBC Driver
                    log.debug("VERIFY: Driver does not support all JDBC 3.0 methods, skipping inheritance consistency tests for " + tableName);
                } catch (SQLException se) {
                    // ignore: the method is likely not implemented by the JDBC Driver
                    // (should be one of the above errors, but postgresql returns this as an SQLException. Tsk.)
                    log.debug("VERIFY: determining super tables failed, skipping inheritance consistency tests for " + tableName);
                }
            }
            Map columns = new HashMap();
            ResultSet columnsSet = metaData.getColumns(null, null, tableName, null);
            try {
                // get column information
                while (columnsSet.next()) {
                    Map colInfo = new HashMap();
                    colInfo.put("DATA_TYPE", new Integer(columnsSet.getInt("DATA_TYPE")));
                    colInfo.put("TYPE_NAME", columnsSet.getString("TYPE_NAME"));
                    colInfo.put("COLUMN_SIZE", new Integer(columnsSet.getInt("COLUMN_SIZE")));
                    colInfo.put("NULLABLE", new Boolean(columnsSet.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls));
                    columns.put(columnsSet.getString("COLUMN_NAME"), colInfo);
                }
            } finally {
                columnsSet.close();
            }
            // iterate through fields and check all fields present
            int pos = 0;
            List builderFields = builder.getFields(FieldDefs.ORDER_CREATE);
            for (Iterator i = builderFields.iterator(); i.hasNext();) {
                FieldDefs field = (FieldDefs)i.next();
                if (field.inStorage() && (field.getDBType() != FieldDefs.TYPE_BYTE || !factory.hasOption(Attributes.STORES_BINARY_AS_FILE))) {
                    pos++;
                    Object id = field.getStorageIdentifier();
                    Map colInfo = (Map)columns.get(id);
                    if ((colInfo == null)) {
                        log.error("VERIFY: Field '" + field.getDBName() + "' of builder '" + builder.getTableName() + "' does NOT exist in storage! Field will be considered virtual.");
                        // set field to virtual so it will not be stored -
                        // prevents future queries or statements from failing
                        field.setDBState(FieldDefs.DBSTATE_VIRTUAL);
                    } else {
                        // compare type
                        int curtype = field.getDBType();
                        int storageType = ((Integer)colInfo.get("DATA_TYPE")).intValue();
                        field.setStorageType(storageType);
                        int type = getJDBCtoMMBaseType(storageType, curtype);
                        if (type != curtype) {
                            log.error(
                                "VERIFY: Field '"
                                    + field.getDBName()
                                    + "' of builder '"
                                    + builder.getTableName()
                                    + "' mismatch : type defined as "
                                    + FieldDefs.getDBTypeDescription(curtype)
                                    + ", but in storage "
                                    + FieldDefs.getDBTypeDescription(type)
                                    + " ("
                                    + colInfo.get("TYPE_NAME")
                                    + ")");
                        } else {
                            boolean nullable = ((Boolean)colInfo.get("NULLABLE")).booleanValue();
                            if (nullable == field.getDBNotNull()) {
                                // only correct if storage is more restrictive
                                if (!nullable) {
                                    field.setDBNotNull(!nullable);
                                    log.warn("VERIFY: Field '" + field.getDBName() + "' of builder '" + builder.getTableName() + "' mismatch : notnull in storage is " + !nullable + " (value corrected for this session)");
                                } else {
                                    log.debug("VERIFY: Field '" + field.getDBName() + "' of builder '" + builder.getTableName() + "' mismatch : notnull in storage is " + !nullable);
                                }
                            }
                            // compare size
                            int size = ((Integer)colInfo.get("COLUMN_SIZE")).intValue();
                            int cursize = field.getDBSize();
                            // ignore the size difference for large fields (generally blobs or memo texts)
                            // since most databases do not return accurate sizes for these fields
                            if (cursize != -1 && size != -1 && size != cursize && cursize <= 255) {
                                if (size < cursize) {
                                    // only correct if storage is more restrictive
                                    field.setDBSize(size);
                                    log.warn(
                                        "VERIFY: Field '" + field.getDBName() + "' of builder '" + builder.getTableName() + "' mismatch : size defined as " + cursize + ", but in storage " + size + " (value corrected for this session)");
                                } else {
                                    log.debug("VERIFY: Field '" + field.getDBName() + "' of builder '" + builder.getTableName() + "' mismatch : size defined as " + cursize + ", but in storage " + size);
                                }
                            }
                        }
                        columns.remove(id);
                    }
                }
            }
            // if any are left, these fields were removed!
            for (Iterator i = columns.keySet().iterator(); i.hasNext();) {
                String column = (String)i.next();
                log.warn("VERIFY: Column '" + column + "' for builder '" + builder.getTableName() + "' in Storage but not defined!");
            }
        } catch (Exception e) {
            log.error("Error during check of table (Assume table is correct.):" + e.getMessage());
            log.error(Logging.stackTrace(e));
        } finally {
            releaseActiveConnection();
        }
    }

    /**
     * Drop a constraint for a composite index.
     * You should have an active connection before calling this method.
     * @param builder the builder for which to drop the composite key
     * @throws StorageException if the composite index cannot be deleted
     * @throws SQLException when a database error occurs
     * @throws
     */
    protected void deleteCompositeIndex(MMObjectBuilder builder) throws StorageException, SQLException {
        if (factory.hasOption(Attributes.SUPPORTS_COMPOSITE_INDEX)) {
            //  TODO: We should determine if there IS an index before removing it...
            //  Scheme: DELETE_CONSTRAINT
            Scheme deleteIndexScheme = factory.getScheme(Schemes.DELETE_CONSTRAINT_SCHEME, Schemes.DELETE_CONSTRAINT_SCHEME_DEFAULT);
            if (deleteIndexScheme != null) {
                DatabaseMetaData metaData = activeConnection.getMetaData();
                ResultSet indexSet = metaData.getIndexInfo(null, null, builder.getTableName(), true, false);
                try {
                    // get index information
                    String indexName = null;
                    while (indexSet.next()) {
                        int indexType = indexSet.getInt("TYPE");
                        if (indexType == DatabaseMetaData.tableIndexClustered) {
                            indexName = indexSet.getString("INDEX_NAME");
                        }
                    }
                    indexSet.close();
                    // remove index if found
                    if (indexName != null) {
                        Statement s = activeConnection.createStatement();
                        String query = deleteIndexScheme.format(new Object[] { this, builder, indexName });
                        logQuery(query);
                        s.executeUpdate(query);
                        s.close();
                    }
                } finally {
                    indexSet.close();
                }
            }
        }
    }

    /**
     * Create a constraint for a composite index.
     * @param builder the builder for which to add the composite key
     */
    protected void createCompositeIndex(MMObjectBuilder builder) throws StorageException, SQLException {
        if (factory.hasOption(Attributes.SUPPORTS_COMPOSITE_INDEX)) {
            //  Scheme: CREATE_CONSTRAINT
            Scheme createIndexScheme = factory.getScheme(Schemes.CREATE_CONSTRAINT_SCHEME, Schemes.CREATE_CONSTRAINT_SCHEME_DEFAULT);
            if (createIndexScheme != null) {
                Statement s = activeConnection.createStatement();
                String constraintDef = getCompositeConstraintDefinition(builder);
                String query = createIndexScheme.format(new Object[] { this, builder, constraintDef });
                logQuery(query);
                s.executeUpdate(query);
                s.close();
            }
        }
    }

    // javadoc is inherited
    public void create(FieldDefs field) throws StorageException {
        if (!factory.hasOption(Attributes.SUPPORTS_DATA_DEFINITION)) {
            throw new StorageException("Data definiton statements (create new field) are not supported.");
        }
        if (factory.getScheme(Schemes.CREATE_OBJECT_ROW_TYPE) != null) {
            throw new StorageException("Can not use data definiton statements (create new field) on row types.");
        }
        if (field.inStorage() && (field.getDBType() != FieldDefs.TYPE_BYTE || !factory.hasOption(Attributes.STORES_BINARY_AS_FILE))) {
            Scheme scheme = factory.getScheme(Schemes.CREATE_FIELD_SCHEME, Schemes.CREATE_FIELD_SCHEME_DEFAULT);
            if (scheme == null) {
                throw new StorageException("Storage layer does not support the dynamic creation of fields");
            } else {
                try {
                    getActiveConnection();
                    // add field
                    String fieldDef = getFieldDefinition(field);
                    String query = scheme.format(new Object[] { this, field.getParent(), fieldDef });
                    Statement s = activeConnection.createStatement();
                    logQuery(query);
                    s.executeUpdate(query);
                    s.close();
                    // add constraints
                    String constraintDef = getConstraintDefinition(field);
                    if (constraintDef != null) {
                        scheme = factory.getScheme(Schemes.CREATE_CONSTRAINT_SCHEME, Schemes.CREATE_CONSTRAINT_SCHEME_DEFAULT);
                        if (scheme != null) {
                            query = scheme.format(new Object[] { this, field.getParent(), constraintDef });
                            s = activeConnection.createStatement();
                            logQuery(query);
                            s.executeUpdate(query);
                            s.close();
                        }
                    }
                    // if the field is a key, redefine the composite key
                    if (field.isKey()) {
                        deleteCompositeIndex(field.getParent());
                        createCompositeIndex(field.getParent());
                    }
                } catch (SQLException se) {
                    throw new StorageException(se);
                } finally {
                    releaseActiveConnection();
                }
            }
        }
    }

    // javadoc is inherited
    public void change(FieldDefs field) throws StorageException {
        if (!factory.hasOption(Attributes.SUPPORTS_DATA_DEFINITION)) {
            throw new StorageException("Data definiton statements (change field) are not supported.");
        }
        if (factory.getScheme(Schemes.CREATE_OBJECT_ROW_TYPE) != null) {
            throw new StorageException("Can not use data definiton statements (change field) on row types.");
        }
        if (field.inStorage() && (field.getDBType() != FieldDefs.TYPE_BYTE || !factory.hasOption(Attributes.STORES_BINARY_AS_FILE))) {
            Scheme scheme = factory.getScheme(Schemes.CHANGE_FIELD_SCHEME, Schemes.CHANGE_FIELD_SCHEME_DEFAULT);
            if (scheme == null) {
                throw new StorageException("Storage layer does not support the dynamic changing of fields");
            } else {
                try {
                    getActiveConnection();
                    // if the field is a key, delete the composite key
                    // Note: changes in whether a field is part of a unique key or not cannot be
                    // made at this moment.
                    if (field.isKey()) {
                        deleteCompositeIndex(field.getParent());
                    }
                    // Todo: explicitly remove indices ??
                    String fieldDef = getFieldDefinition(field);
                    String query = scheme.format(new Object[] { this, field.getParent(), fieldDef });
                    Statement s = activeConnection.createStatement();
                    logQuery(query);
                    s.executeUpdate(query);
                    s.close();
                    // add constraints
                    String constraintDef = getConstraintDefinition(field);
                    if (constraintDef != null) {
                        scheme = factory.getScheme(Schemes.CREATE_CONSTRAINT_SCHEME, Schemes.CREATE_CONSTRAINT_SCHEME_DEFAULT);
                        if (scheme != null) {
                            query = scheme.format(new Object[] { this, field.getParent(), constraintDef });
                            s = activeConnection.createStatement();
                            logQuery(query);
                            s.executeUpdate(query);
                            s.close();
                        }
                    }
                    // if the field is a key, add the composite key
                    if (field.isKey()) {
                        createCompositeIndex(field.getParent());
                    }
                } catch (SQLException se) {
                    throw new StorageException(se);
                } finally {
                    releaseActiveConnection();
                }
            }
        }
    }

    // javadoc is inherited
    public void delete(FieldDefs field) throws StorageException {
        if (!factory.hasOption(Attributes.SUPPORTS_DATA_DEFINITION)) {
            throw new StorageException("Data definiton statements (delete field) are not supported.");
        }
        if (factory.getScheme(Schemes.CREATE_OBJECT_ROW_TYPE) != null) {
            throw new StorageException("Can not use data definiton statements (delete field) on row types.");
        }
        if (field.inStorage() && (field.getDBType() != FieldDefs.TYPE_BYTE || !factory.hasOption(Attributes.STORES_BINARY_AS_FILE))) {
            Scheme scheme = factory.getScheme(Schemes.DELETE_FIELD_SCHEME, Schemes.DELETE_FIELD_SCHEME_DEFAULT);
            if (scheme == null) {
                throw new StorageException("Storage layer does not support the dynamic deleting of fields");
            } else {
                try {
                    getActiveConnection();
                    // if the field is a key, delete the composite key
                    if (field.isKey()) {
                        deleteCompositeIndex(field.getParent());
                    }
                    // Todo: explicitly remove indices ??
                    String query = scheme.format(new Object[] { this, field.getParent(), field });
                    Statement s = activeConnection.createStatement();
                    logQuery(query);
                    s.executeUpdate(query);
                    s.close();
                    // if the field is a key, add the composite key
                    if (field.isKey()) {
                        createCompositeIndex(field.getParent());
                    }
                } catch (SQLException se) {
                    throw new StorageException(se);
                } finally {
                    releaseActiveConnection();
                }
            }
        }
    }


    /**
     * Convert legacy file
     * @return Number of converted fields. Or -1 if not storing binaries as files
     */

    public int convertLegacyBinaryFiles() throws org.mmbase.storage.search.SearchQueryException {
        if (factory.hasOption(Attributes.STORES_BINARY_AS_FILE)) {
            synchronized(factory) { // there is only on factory. This makes sure that there is only one conversion running
                int result = 0;
                int fromDatabase = 0;
                Enumeration builders = factory.getMMBase().getMMObjects();
                while (builders.hasMoreElements()) {
                    MMObjectBuilder builder = (MMObjectBuilder)builders.nextElement();
                    Iterator fields = builder.getFields().iterator();
                    while (fields.hasNext()) {
                        FieldDefs field = (FieldDefs)fields.next();
                        String fieldName = field.getDBName();
                        if (field.getDBType() == FieldDefs.TYPE_BYTE) { // check all binaries

                            // check whether it might be in a column
                            boolean foundColumn = false;
                            {
                                try {
                                    getActiveConnection();
                                    String tableName = (String)factory.getStorageIdentifier(builder);
                                    DatabaseMetaData metaData = activeConnection.getMetaData();
                                    ResultSet columnsSet = metaData.getColumns(null, null, tableName, null);
                                    try {
                                        while (columnsSet.next()) {
                                            if (columnsSet.getString("COLUMN_NAME").equals(fieldName)) {
                                                foundColumn = true;
                                                break;
                                            }
                                        }
                                    } finally {
                                        columnsSet.close();
                                    }
                                } catch (java.sql.SQLException sqe) {
                                    log.error(sqe.getMessage());
                                } finally {
                                    releaseActiveConnection();
                                }
                            }

                            List nodes = builder.getNodes(new org.mmbase.storage.search.implementation.NodeSearchQuery(builder));
                            log.service("Checking all " + nodes.size() + " nodes of '" + builder.getTableName() + "'");
                            Iterator i = nodes.iterator();
                            while (i.hasNext()) {
                                MMObjectNode node = (MMObjectNode)i.next();
                                File storeFile = getBinaryFile(node, fieldName);
                                if (!storeFile.exists()) { // not found!
                                    File legacyFile = getLegacyBinaryFile(node, fieldName);
                                    if (legacyFile != null) {
                                        storeFile.getParentFile().mkdirs();
                                        if (legacyFile.renameTo(storeFile)) {
                                            log.service("Renamed " + legacyFile + " to " + storeFile);
                                            result++;
                                        } else {
                                            log.warn("Could not rename " + legacyFile + " to " + storeFile);
                                        }
                                    } else {
                                        if (foundColumn) {
                                            byte[] bytes = readBinaryFromDatabase(node, field);
                                            node.setValue(fieldName, bytes);
                                            storeBinaryAsFile(node, field);
                                            node.setValue(fieldName, null); // remove to avoid filling node-cache with lots of handles and cause out-of-memory
                                            // node.commit(); no need, because we only changed blob (so no database updates are done)
                                            result++;
                                            fromDatabase++;
                                            log.service("( " + result + ") Found bytes in database while configured to be on disk. Stored to " + storeFile);
                                        }
                                    }
                                }
                            } // nodes
                        } // if type = byte
                    } // fields
                } // builders
                if (result > 0) {
                    log.info("Converted " + result + " fields " + ((fromDatabase > 0 && fromDatabase < result) ? " of wich  " + fromDatabase + " from database" : ""));
                    if (fromDatabase > 0) {
                        log.info("You may drop byte array columns from the database now. See the the VERIFY warning during initialisation.");
                    }

                } else {
                    log.service("Converted no fields");
                }
                return result;
            } // synchronized
        } else {
            // not configured to store blobs as file
            return -1;
        }
    }
}
