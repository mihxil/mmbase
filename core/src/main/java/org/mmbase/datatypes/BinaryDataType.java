/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.datatypes;

import org.mmbase.util.logging.*;
import org.mmbase.util.SerializableInputStream;
import org.mmbase.bridge.*;
import org.mmbase.util.*;
import java.util.Collection;
import java.io.InputStream;
import java.util.regex.Pattern;
import org.apache.commons.fileupload.FileItem;

/**
 * The datatype associated with byte arrays ('blobs').
 *
 * @author Pierre van Rooden
 * @version $Id$
 * @since MMBase-1.8
 */
public class BinaryDataType extends AbstractLengthDataType<InputStream> {

    private static final Logger log = Logging.getLoggerInstance(BinaryDataType.class);

    private static final long serialVersionUID = 1L;

    protected MimeTypeRestriction mimeTypeRestriction = new MimeTypeRestriction(Pattern.compile(".*"));

    /**
     * Constructor for binary field.
     * @param name the name of the data type
     */
    public BinaryDataType(String name) {
        super(name, InputStream.class);
    }


    @Override
    protected String castToPresent(Object value, Node node, Field field) {
        return "BINARY";
    }


    @Override
    protected InputStream cast(Object value, Cloud cloud, Node node, Field field) throws CastException {
        Object preCast = preCast(value, cloud, node, field);
        if (preCast == null) return null;
        return org.mmbase.util.Casting.toSerializableInputStream(preCast);
    }

    @Override
    protected void cloneRestrictions(BasicDataType origin) {
        super.cloneRestrictions(origin);
        if (origin instanceof BinaryDataType) {
            BinaryDataType dataType = (BinaryDataType)origin;
            mimeTypeRestriction = new MimeTypeRestriction(dataType.mimeTypeRestriction);
        }
    }


    @Override
    public long getLength(Object value) {
        if (value == null) return 0;
        if (log.isDebugEnabled()) {
            log.debug("Finding lenth for " + value);
        }
        if (value instanceof byte[]) {
            byte[] bytes = (byte[]) value;
            if (log.isDebugEnabled()) {
                StringBuilder buf = new StringBuilder("[");
                for (int i = 0 ; i < bytes.length; i++) {
                    buf.append((char) bytes[i]);
                    if (i + 1 < bytes.length) {
                        buf.append(", ");
                    }
                }
                buf.append("]");
                log.debug("Getting length of " + buf);
            }
            return bytes.length;
        } else if (value instanceof FileItem) {
            FileItem fi = (FileItem) value;
            return fi.getSize();
        } else if (value instanceof SerializableInputStream) {
            SerializableInputStream sis = (SerializableInputStream) value;
            return sis.getSize();
        } else {
            throw new RuntimeException("Value " + value + " of " + getName() + " is not a byte array but" + (value == null ? "null" : value.getClass().getName()));
        }
    }

    /**
     * @since MMBase-2.0
     */
    public MimeType getMimeType(Object value, Node node, Field field) {
        if (value instanceof byte[]) {
            byte[] array = (byte[]) value;
            return new MimeType(org.mmbase.util.magicfile.MagicFile.getInstance().getMimeType(array));
        } else if (value instanceof FileItem) {
            FileItem fi = (FileItem) value;
            return new MimeType(fi.getContentType());
        } else if (value instanceof SerializableInputStream) {
            SerializableInputStream sis = (SerializableInputStream) value;
            return new MimeType(sis.getContentType());
        } else {
            return new MimeType("*/*");
            //throw new RuntimeException("Value " + value + " of " + getName() + " is not a byte array but" + (value == null ? "null" : value.getClass().getName()));
        }
    }

    @Override
    protected Collection<LocalizedString> validateRequired(Collection<LocalizedString> errors, Object castValue, Object value, Node  node, Field field) {
        return requiredRestriction.validate(errors, castValue, node, field);
    }


    /**
     * Returns a regular expression which describes wich mime-types are valid for blobs with this
     * DataType. This is not yet available as a Restriction, only as a property.
     */
    public Pattern getValidMimeTypes() {
        return mimeTypeRestriction.getValue();
    }

    public void setValidMimeTypes(Pattern pattern) {
        mimeTypeRestriction.setValue(pattern);
    }

    /**
     * @since MMBase-2.0
     */
    protected class MimeTypeRestriction extends AbstractRestriction<Pattern> {
        private static final long serialVersionUID = 0L;

        MimeTypeRestriction(MimeTypeRestriction source) {
            super(source);
        }
        MimeTypeRestriction(Pattern p) {
            super("mimetype", p);
        }
        @Override
        protected boolean simpleValid(Object v, Node node, Field field) {
            MimeType s = BinaryDataType.this.getMimeType(v, node, field);
            boolean res = value == null || s == null ? true : value.matcher(s.toString()).matches();
            if (log.isDebugEnabled()) {
                log.debug("VALIDATING " + v + "->" + s + " with " + getValue() + " -> " + res);
            }
            return res;
        }
    }


}
