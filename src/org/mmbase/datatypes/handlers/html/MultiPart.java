/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.datatypes.handlers.html;

import java.util.*;
import javax.servlet.http.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.apache.commons.fileupload.*;

/**
 * Taglib needs to read Multipart request sometimes. Functionallity is centralized here.
 * @author Michiel Meeuwissen
 * @version $Id: MultiPart.java,v 1.1 2009-04-17 17:37:54 michiel Exp $
 **/

public class MultiPart {
    private static final Logger log = Logging.getLoggerInstance(MultiPart.class);

    static String MULTIPARTREQUEST_KEY = MultiPart.class.getName();

    public static boolean isMultipart(HttpServletRequest request) {
        String ct = request.getContentType();
        if (ct == null) {
            return false;
        }
        return ct.startsWith("multipart/");
    }

    /**
     * @since MMBase-1.8.7
     */
    public static MMultipartRequest getMultipartRequest(HttpServletRequest request, String encoding) {
        MMultipartRequest multipartRequest = (MMultipartRequest) request.getAttribute(MULTIPARTREQUEST_KEY);
        if (multipartRequest == null) {
            log.debug("Creating new MultipartRequest");
            multipartRequest = new MMultipartRequest(request, encoding);
            log.debug("have it");

            if (log.isDebugEnabled()) {
                if (multipartRequest != null) {
                    StringBuilder params = new StringBuilder();
                    for (String paramName : (Collection<String>) multipartRequest.getParameterNames()) {
                        params.append(paramName).append(",");
                    }
                    log.debug("multipart parameters: " + params);
                } else {
                    log.debug("not a multipart request");
                }
            }
            request.setAttribute(MULTIPARTREQUEST_KEY, multipartRequest);
        } else {
            log.debug("Found multipart request on pageContext" + multipartRequest);
        }
        return multipartRequest;
    }

    public static MMultipartRequest getMultipartRequest(HttpServletRequest request, HttpServletResponse response) {

        String charEnc = response.getCharacterEncoding();
        if(charEnc == null) {
            log.error("page encoding not specified, using iso-8859-1");
            charEnc =  "iso-8859-1";
        }
        return getMultipartRequest(request, charEnc);

    }

    static public class MMultipartRequest {

        private Map<String, Object> parametersMap = new HashMap<String, Object>();
        private String coding = null;

        MMultipartRequest(HttpServletRequest req, String c) {
            try {
                DiskFileUpload fu =  new DiskFileUpload();
                fu.setHeaderEncoding("ISO-8859-1"); // if incorrect, it will be fixed later.
                List fileItems = fu.parseRequest(req);
                for (Iterator i = fileItems.iterator(); i.hasNext(); ) {
                    FileItem fi = (FileItem)i.next();
                    if (fi.isFormField()) {
                        String value;
                        try {
                            value = fi.getString("ISO-8859-1");
                        } catch(java.io.UnsupportedEncodingException uee) {
                            log.error("could not happen, ISO-8859-1 is supported");
                            value = fi.getString();
                        }
                        Object oldValue = parametersMap.get(fi.getFieldName());
                        if (oldValue == null ) {
                            parametersMap.put(fi.getFieldName(), value);
                        } else if (!(oldValue instanceof FileItem)) {
                            List<Object> values;
                            if (oldValue instanceof String) {
                                values = new ArrayList<Object>();
                                values.add(oldValue);
                            } else {
                                values = (List<Object>)oldValue;
                            }
                            values.add(value);
                            parametersMap.put(fi.getFieldName(), values);
                        }
                    } else {
                        parametersMap.put(fi.getFieldName(), fi);
                    }
                }
            } catch (FileUploadException e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
            coding = c;
            log.debug("Created with encoding: " + coding);
        }

        /**
         * Method to retrieve the bytes of an uploaded file.
         * @param param The name of the parameter
         * @return <code>null</code> if parameter not found, otherwise the bytes from the parameter
         */
        public byte[] getBytes(String param) {
            log.debug("Getting bytes for " + param);
            Object value = parametersMap.get(param);
            if (value instanceof FileItem) {
                return ((FileItem)value).get();
            } else {
                return null;
            }
        }
        public FileItem getFileItem(String param)  {
            log.debug("Getting outputstream for " + param);
            Object value = parametersMap.get(param);
            if (value instanceof FileItem) {
                return (FileItem)value;
            } else {
                return null;
            }
        }

        /**
         * Method to retrieve the bytes of an uploaded file as a string using eitehr the encoding specified in the file or
         * the default encoding.
         * @return <code>null</code> if parameter not found, otherwise the bytes from the parameter
         */
        protected String encodeBytesAsString(byte[] data) throws java.io.UnsupportedEncodingException {
            String encoding = coding;
            // get first 60 bytes to determine if this is a xml type
            byte[] xmlbytes = new byte[60];
            int sz = data.length;
            if (sz > 60) sz = 60;
            System.arraycopy(data, 0, xmlbytes, 0, sz);
            String xmltext = new String(xmlbytes);
            if (xmltext.startsWith("<?xml")) {
                int i = xmltext.indexOf("encoding");
                log.debug("i=*" + i + "*");
                if (i > 0) {
                    int j = xmltext.indexOf("?>", i);
                    log.debug("j=*" + j + "*");
                    if (j > i) {
                        // get trimmed attribute value
                        encoding = xmltext.substring(i + 8, j).trim();
                        // trim '='
                        encoding = encoding.substring(1).trim();
                        // trim quotes
                        encoding = encoding.substring(1, encoding.length() - 1).trim();
                    }
                }
            }
            return new String(data, encoding);
        }

        /**
         * @since MMBase-1.8
         */
        public boolean isFile(String param) {
            Object value = parametersMap.get(param);
            return value instanceof FileItem;
        }

        /**
         * Method to retrieve the parameter.
         * @param param The name of the parameter
         * @return <code>null</code> if parameter not found, when a single occurence of the parameter
         * the result as a <code>String</code> using the encoding specified. When if was a MultiParameter parameter, it will return
         * a <code>List</code> of <code>String</code>'s
         */
        public Object getParameterValues(String param) throws java.io.UnsupportedEncodingException {
            // this method will return null, if the parameter is not set...
            Object value = parametersMap.get(param);
            //log.debug("Got param " + param + " " + (value == null ? "NULL" : value.getClass().getName()) + " " + value);

            if (value instanceof FileItem) {
                return encodeBytesAsString(((FileItem)value).get());
            } else {
                return value;
            }
        }

        public Collection<String> getParameterNames() {
            return parametersMap.keySet();
        }
    }

}