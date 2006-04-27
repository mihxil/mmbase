/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders;

import java.util.*;
import org.mmbase.module.core.*;
import org.mmbase.util.functions.*;
import org.mmbase.util.images.*;
import org.mmbase.util.UriParser;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.*;
import org.mmbase.util.logging.*;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * ImageCaches (aka as 'icaches') is a system-like builder used by
 * builders with the 'Images' class. It contains the converted images.
 *
 * @author Daniel Ockeloen
 * @author Michiel Meeuwissen
 * @version $Id: ImageCaches.java,v 1.52 2006-04-27 16:40:48 pierre Exp $
 */
public class ImageCaches extends AbstractImages {

    private static final Logger log = Logging.getLoggerInstance(ImageCaches.class);

    public static final String FIELD_ID       = "id";

    public final static Parameter[] WAIT_PARAMETERS      =  Parameter.EMPTY;

    static final String GUI_IMAGETEMPLATE = "s(100x60)";

    private boolean checkLegacyCkey = true;


    public ImageCaches() {
    }

    public boolean init() {
        if (oType != -1) return true; // inited already
        if (!super.init()) return false;

        checkLegacyCkey = ! "false".equals(getInitParameter("LegacyCKey"));
        return true;
    }

    /**
     * Returns the original images, for which this node is a cached image.
     *
     * @since MMBase-1.6
     **/
    private MMObjectNode originalImage(MMObjectNode node) {
        return getNode(node.getIntValue(FIELD_ID));
    }

    protected StringBuffer getFileName(MMObjectNode node, StringBuffer buf) {        
        MMObjectNode originalImage = originalImage(node);
        Images images = (Images) originalImage.getBuilder();
        images.getFileName(originalImage, buf);
        String ext = getImageFormat(node);
        if (images.storesImageType()) { // otherwise too expensive
            if (! ext.equals(images.getImageFormat(originalImage))) {
                buf.append('.').append(ext);
            }
        } else {
            buf.append('.').append(ext);
        }
        return buf;
    }
    protected boolean addFileName(MMObjectNode node, String servlet) {
        if (super.addFileName(node, servlet)) return true;
        MMObjectNode originalImage = originalImage(node);
        Images images = (Images) originalImage.getBuilder();
        return images.addFileName(originalImage, servlet);
    }

    /**
     * The GUI indicator of an image can have an alt-text.
     *
     * @since MMBase-1.6
     **/

    protected String getGUIIndicatorWithAlt(MMObjectNode node, String alt, Parameters a) {
        StringBuffer servlet = new StringBuffer();
        HttpServletRequest req = (HttpServletRequest) a.get(Parameter.REQUEST);
        if (req != null) {
            servlet.append(getServletPath(UriParser.makeRelative(new java.io.File(req.getServletPath()).getParent(), "/")));
        } else {
            servlet.append(getServletPath());
        }
        String ses = (String) a.get("session");
        servlet.append(usesBridgeServlet && ses != null ? "session=" + ses + "+" : "");
        MMObjectNode origNode = originalImage(node);
        String imageThumb;
        HttpServletResponse res = (HttpServletResponse) a.get(Parameter.RESPONSE);
        String heightAndWidth = "";
        if (origNode != null) {

            List cacheArgs =  new Parameters(Images.CACHE_PARAMETERS).set("template", GUI_IMAGETEMPLATE);
            MMObjectNode thumb = (MMObjectNode) origNode.getFunctionValue("cachednode", cacheArgs);
            //heightAndWidth = "height=\"" + getHeight(thumb) + "\" with=\"" + getWidth(thumb) + "\" ";
            heightAndWidth = ""; // getHeight and getWidth not yet present in AbstractImages
            imageThumb = servlet.toString() + thumb.getNumber();
            if (res != null) {
                imageThumb = res.encodeURL(imageThumb);
            }
        } else {
            imageThumb = "";
        }
        String title;
        String field = (String) a.get("field");
        if (field == null || "".equals(field)) {
            // gui for the node itself.
            title = "";
        } else {
            if (storesDimension()) {
                title = " title=\"" + getMimeType(node) + " " + getDimension(node) + "\"";
            } else {
                title = " title=\"" + getMimeType(node) + "\"";
            }
        }

        String image      = servlet.toString() + node.getNumber();
        if (res != null) image = res.encodeURL(image);
        return "<a href=\"" + image + "\" target=\"_new\"><img src=\"" + imageThumb + "\" border=\"0\" " + heightAndWidth + "alt=\"" +
            org.mmbase.util.transformers.Xml.XMLAttributeEscape(alt, '\"') +
            "\"" + title + " /></a>";
    }

    // javadoc inherited
    protected String getSGUIIndicatorForNode(MMObjectNode node, Parameters a) {
        MMObjectNode origNode = originalImage(node);
        return getGUIIndicatorWithAlt(node, (origNode != null ? origNode.getStringValue("title") : ""), a);
    }

    /**
     * If a icache node is created with empty 'handle' field, then the handle field can be filled
     * automaticly. Sadly, getValue of MMObjectNode cannot be overriden, so it cannot be done
     * completely automaticly, but that may be more transparent anyway.  Call this method (perhaps
     * with node.getFunctionValue("wait", null)) before requesting the handle field. This will
     * method will block until the field is filled.
     * @param node A icache node.
     */
    public void waitForConversion(MMObjectNode node) {
        log.debug("Wating for conversion?");
        if (node.isNull(Imaging.FIELD_HANDLE)) {
            log.service("Waiting for conversion");
            // handle field not yet filled, but this is not a new node
            String ckey     = node.getStringValue(Imaging.FIELD_CKEY);
            String template = Imaging.parseCKey(ckey).template;
            List params     = Imaging.parseTemplate(template);
            MMObjectNode image = originalImage(node);
            // make sure the bytes don't come from the cache (e.g. multi-cast change!, new conversion could be triggered, but image-node not yet invalidated!)
            image.getBuilder().clearBlobCache(image.getNumber());
            byte[] bytes = image.getByteValue(Imaging.FIELD_HANDLE);
            String format = ((AbstractImages) image.getBuilder()).getImageFormat(image);
            // This triggers conversion, or waits for it to be ready.
            ImageConversionRequest req = Factory.getImageConversionRequest(params, bytes, format, node);
            req.waitForConversion();

        } else {
            log.debug("no");
        }
    }


    /**
     * Finds a icache node in the icaches table
     * @param imageNumber The node number of the image for which it must be searched
     * @param template     The image conversion template
     * @return The icache node or <code>null</code> if it did not exist yet.
     **/
    public MMObjectNode getCachedNode(int imageNumber, String template) {
        log.debug("Getting cached noded for " + template + " and image " + imageNumber);
        List nodes;
        try {
            NodeSearchQuery query = new NodeSearchQuery(this);
            query.setMaxNumber(2); // to make sure this is a cheap query.
            StepField ckeyField = query.getField(getField(Imaging.FIELD_CKEY));
            Object ckey = Factory.getCKey(imageNumber, template);
            BasicFieldValueConstraint bfvc = new BasicFieldValueConstraint(ckeyField, ckey.toString());
            bfvc.setCaseSensitive(true);
            query.setConstraint(bfvc);
            nodes = getNodes(query);
        } catch (SearchQueryException e) {
            log.error(e.toString());
            return null;
        }


        if (nodes.size() > 1) {
            log.warn("Found more then one cached image with key ("+ template +")");
        }

        if (nodes.size() == 0) {
            log.debug("Did not find cached images with key ("+ template +")");
            if (checkLegacyCkey) {
                return getLegacyCachedNode(imageNumber, template);
            } else {
                return null;
            }

        } else {
            return (MMObjectNode) nodes.get(0);
        }
    }
    /**
     * Finds a icache node in the icache table, supposing 'legacy' ckeys (where all +'s are removed).
     * @param imageNumber The node number of the image for which it must be searched
     * @param template     The image conversion template
     * @return The icache node or <code>null</code> if it did not exist.
     **/
    protected MMObjectNode getLegacyCachedNode(int imageNumber, String template) {
        List params = Imaging.parseTemplate(template);
        String legacyCKey = "" + imageNumber + getLegacyCKey(params);
        log.info("Trying legacy " + legacyCKey);
        List legacyNodes;
        try {
            NodeSearchQuery query = new NodeSearchQuery(this);
            query.setMaxNumber(2); // to make sure this is a cheap query.
            StepField ckeyField = query.getField(getField(Imaging.FIELD_CKEY));
            query.setConstraint(new BasicFieldValueConstraint(ckeyField, legacyCKey));
            legacyNodes = getNodes(query);
            if (legacyNodes.size() == 0) {
                log.debug("Did not find cached images with key (" +  legacyCKey + ")");
            }
            if (legacyNodes.size() > 1) {
                log.warn("Found more then one cached image with key (" + legacyCKey + ")");
            }
            MMObjectNode legacyNode = null;
            Iterator i = legacyNodes.iterator();
            // now fix the ckey to new value
            String ckey = Factory.getCKey(imageNumber, template).toString();
            while (i.hasNext()) {
                legacyNode = (MMObjectNode) i.next();
                legacyNode.setValue(Imaging.FIELD_CKEY, ckey); // fix to new format
                legacyNode.commit();
            }
            return legacyNode;
        } catch (SearchQueryException e) {
            log.error(e.toString());
            return null;
        }
    }

    /**
     * Invalidate the Image Cache for a specific Node
     * method only accessable on package level, since only Images should call it..
     *
     * @param imageNode The image node, which is the original of the cached modifications
     * @since MMBase-1.7
     */
    protected void invalidate(MMObjectNode imageNode) {
        if (log.isDebugEnabled()) {
            log.debug("Going to invalidate the node, where the original node # " + imageNode.getNumber());
        }
        // first get all the nodes, which are currently invalid....
        // this means all nodes from icache where the field 'ID' == node it's number
        List nodes;
        try {
            NodeSearchQuery query = new NodeSearchQuery(this);
            StepField idField = query.getField(getField(FIELD_ID));
            query.setConstraint(new BasicFieldValueConstraint(idField, new Integer(imageNode.getNumber())));
            nodes = getNodes(query);
        } catch (SearchQueryException e) {
            log.error(e.toString());
            nodes = new java.util.ArrayList(); // do nothing
        }

        Iterator i = nodes.iterator();
        while(i.hasNext()) {
            // delete the icache node
            MMObjectNode invalidNode = (MMObjectNode) i.next();
            removeNode(invalidNode);
            if (log.isDebugEnabled()) {
                log.debug("deleted node with number#" + invalidNode.getNumber());
            }
        }



    }

    /**
     * Override the MMObjectBuilder removeNode, to invalidate the LRU ImageCache, when a node gets deleted.
     * Remove a node from the cloud.
     *
     * @param node The node to remove.
     */
    public void removeNode(MMObjectNode node) {
        String ckey = node.getStringValue(Imaging.FIELD_CKEY);
        log.service("Icaches: removing node " + node.getNumber() + " " + ckey);
        ((Images) mmb.getBuilder("images")).invalidateTemplateCacheNumberCache(node.getIntValue(FIELD_ID));
        // also delete from LRU Cache
        super.removeNode(node);

    }

    /**
     * Returns the image format.
     */
    protected String getImageFormat(MMObjectNode node) {
        if (storesImageType()) {
            String iType = node.getStringValue(FIELD_ITYPE);
            if(iType.equals("")) {
                if (! node.isNull(Imaging.FIELD_HANDLE)) {        // handle present
                    // determin using the blob
                    return super.getImageFormat(node);
                }
            } else {
                return iType;
            }
        }
        // determin using the ckey
        log.debug("Not found, using ckey");

        // stupid method, for if the format is not a field of the icaches table.
        String ckey    = node.getStringValue(Imaging.FIELD_CKEY);
        int fi = ckey.indexOf("f(");
        if (fi > -1) {
            int fi2 = ckey.indexOf(")", fi);
            return ckey.substring(fi + 2, fi2);
        } else {
            String r = Factory.getDefaultImageFormat();
            if (r.equals("asis")) {
                MMObjectNode original = originalImage(node);
                if (original != null) {
                    return ((AbstractImages) original.getBuilder()).getImageFormat(original);
                } else {
                    log.warn("Could not find original image for " + node);
                    return r;
                }
            } else {
                return r;
            }
        }
        // not storing the result of parsing on ckey, it is cheap, and determining by handle is more
        // correct.

    }


    public String getMimeType(List params) {
        return getMimeType(getNode("" + params.get(0)));
    }

    public int insert(String owner, MMObjectNode node) {
        int res = super.insert(owner, node);
        // make sure there is no such thing with this ckey cached
        ((Images) mmb.getMMObject("images")).invalidateTemplateCacheNumberCache(node.getStringValue(Imaging.FIELD_CKEY));
        return res;
    }


    /**
     * Every image of course has a format and a mimetype. Two extra functions to get them.
     *
     */

    protected Object executeFunction(MMObjectNode node, String function, List args) {
        if (function.equals("wait")) {
            waitForConversion(node);
            return node;
        } else {
            return super.executeFunction(node, function, args);
        }
    }

    /**
     * This function will flatten the parameters to the legacy unique key, so that an image can be found in the cache.
     *
     * This function used to be called 'flattenParameters'.
     *
     * @param params a <code>List</code> of <code>String</code>s, with a size greater then 0 and not null
     * @return a string containing the key for this List, or <code>null</code>,....
     */
    private String getLegacyCKey(List params) {
        if (params == null || params.size() == 0) {
            log.debug("no parameters");
            return null;
        }
        // flatten parameters as a 'hashed' key;
        StringBuffer sckey = new StringBuffer("");
        Iterator enumeration=params.iterator();
        while(enumeration.hasNext()) {
            sckey.append(enumeration.next().toString());
        }
        // skip spaces at beginning and ending, URL param escape to avoid everything strange in it.
        String ckey = "";
        try {
            ckey = new String(sckey.toString().trim().getBytes("US-ASCII")).replace('"', 'X').replace('\'', 'X');
        } catch (java.io.UnsupportedEncodingException e) {
            log.error(e.toString());
        }
        // of course it is not a very good idea to convert to US-ASCII, but
        // in ImageCaches this string is used in a select statement, without using
        // a database layer. So we must have something which works always.
        // Some texts, however will lead to the same ckey now.

        if(log.isDebugEnabled()) log.debug("using ckey " + ckey);
        if(ckey.length() > 0) {
            return ckey;
        } else {
            log.debug("empty parameters");
            return null;
        }
    }

}

