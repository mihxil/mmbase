/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.jsp.taglib.typehandler;

import javax.servlet.jsp.JspTagException;
import java.util.*;

import org.mmbase.bridge.jsp.taglib.edit.FormTag;
import org.mmbase.util.*;
import org.mmbase.util.transformers.Xml;
import org.mmbase.bridge.*;
import org.mmbase.bridge.util.Queries;
import org.mmbase.bridge.jsp.taglib.*;
import org.mmbase.storage.search.*;
import org.mmbase.datatypes.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * @javadoc
 *
 * @author Gerard van de Looi
 * @author Michiel Meeuwissen
 * @since  MMBase-1.6
 * @version $Id$
 */

public abstract class AbstractTypeHandler implements TypeHandler {
    private static final Logger log = Logging.getLoggerInstance(AbstractTypeHandler.class);

    protected FieldInfoTag tag;
    protected EnumHandler eh;
    protected boolean gotEnumHandler = false;

    /**
     * Constructor for AbstractTypeHandler.
     */
    public AbstractTypeHandler(FieldInfoTag tag) {
        super();
        this.tag = tag;

    }
    @Override
    public void init() {
        eh = null;
        gotEnumHandler = false;
    }


    protected EnumHandler getEnumHandler(Node node, Field field) throws JspTagException {
        if (gotEnumHandler) return eh;
        gotEnumHandler = true;
        DataType<?> dt = field.getDataType();

        if (dt.getEnumerationValues(tag.getLocale(), tag.getCloudVar(), node, field) != null) {
            return new EnumHandler(tag, node, field);
        }

        // XXX: todo the following stuff may peraps be somehow wrapped to IntegerDataType itself;
        // but what to do with 200L??
        if (dt instanceof IntegerDataType) {
            IntegerDataType idt = (IntegerDataType) dt;
            final int min = idt.getMin() + (idt.isMinInclusive() ? 0 : 1);
            final int max = idt.getMax() - (idt.isMaxInclusive() ? 0 : 1);
            if ((long) max - min < 200L) {
                return new EnumHandler(tag, node, field) {
                    @Override
                    protected Iterator<Entry<Integer, Integer>> getIterator(Node node, Field field) {
                        return new Iterator<Entry<Integer, Integer>>() {
                            int i = min;

                            @Override
                            public boolean hasNext() {
                                return i <= max;
                            }
                            @Override
                            public Entry<Integer, Integer> next() {
                                Integer value = i++;
                                return new Entry<Integer, Integer>(value, value);
                            }
                            @Override
                            public void remove() {
                                throw new UnsupportedOperationException();
                            }
                        };
                    }
                };
            }
        }
        if (dt instanceof LongDataType) {
            LongDataType ldt = (LongDataType) dt;
            final long min = ldt.getMin() + (ldt.isMinInclusive() ? 0 : 1);
            final long max = ldt.getMax() - (ldt.isMaxInclusive() ? 0 : 1);
            if ((double) max - min < 200.0) {
                return new EnumHandler(tag, node, field) {
                    @Override
                    protected Iterator<Entry<Long, Long>> getIterator(Node node, Field field) {
                        return new Iterator<Entry<Long, Long>>() {
                            long i = min;

                            @Override
                            public boolean hasNext() {
                                return i <= max;
                            }
                            @Override
                            public Entry<Long, Long> next() {
                                Long value = i++;
                                return new Entry<Long, Long>(value, value);
                            }
                            @Override
                            public void remove() {
                                throw new UnsupportedOperationException();
                            }
                        };
                    }
                };
            }
        }

        return null;
    }

    protected StringBuilder addExtraAttributes(StringBuilder buf) throws JspTagException {
        String options = tag.getOptions();
        if (options != null) {
            int i = options.indexOf("extra:");
            if (i > -1) {
                buf.append(" ").append(options.substring(i + 6)).append(" ");
            }
        }
        return buf;
    }


    /**
     * @since MMBase-1.9.3
     */
    protected String getClassName(Class<?> c) {
        String name = c.getName();
        name = name.substring(c.getPackage().getName().length() + 1);
        if (name.endsWith("DataType")) {
            name = name.substring(0, name.length() - "DataType".length());
        }
        return name.toLowerCase();
    }

    /**
     * @since MMBase-1.8
     */
    protected String getClasses(Node node, Field field, boolean search) throws JspTagException {
        StringBuilder buf = new StringBuilder("mm_validate ");
        if (search) buf.append("mm_search ");
        DataType dt = field.getDataType();
        for (String styleClass : dt.getStyleClasses()) {
            buf.append(styleClass);
        }
        {
            Class<?> sup = dt.getClass();
            while(DataType.class.isAssignableFrom(sup)) {
                if (sup.equals(org.mmbase.datatypes.BasicDataType.class)) {
                    break;
                }
                buf.append(" mm_dtclass_");
                buf.append(getClassName(sup));
                sup = sup.getSuperclass();
            }
            for (Class<?> c : dt.getClass().getInterfaces()) {
                if (DataType.class.isAssignableFrom(c)) {
                    buf.append(" mm_dtclass_");
                    buf.append(getClassName(c));
                }
            }
            buf.append(' ');
        }
        if (field instanceof org.mmbase.bridge.util.DataTypeField) {
            buf.append("mm_dt_");
            buf.append(field.getDataType().getName());
        } else {
            buf.append("mm_f_");
            buf.append(field.getName());
            buf.append(" mm_nm_");
            buf.append(field.getNodeManager().getName());
        }
        FieldInfoTag.DataTypeOrigin o = tag.getOrigin();
        if (o != FieldInfoTag.DataTypeOrigin.FIELD) {
            buf.append(" mm_dto_").append(o.toString().toLowerCase());
        }
        if (node != null) {
            buf.append(" mm_n_");
            buf.append(node.getNumber());
            if (dt instanceof org.mmbase.datatypes.LengthDataType) {
                buf.append(" mm_length_");
                Object value  = getFieldValue(node, field);
                if (value != null) {
                    try {
                        buf.append(((org.mmbase.datatypes.LengthDataType) dt).getLength(value));
                    } catch (Exception e) {
                        log.warn(e.getMessage(), e);
                        buf.append(node.getSize(field.getName()));
                    }
                } else {
                    buf.append(node.getSize(field.getName()));
                }
            }
            if (dt instanceof org.mmbase.datatypes.BinaryDataType) {
                buf.append(" mm_mimetype_");
                Object value  = getFieldValue(node, field, false);
                if (value != null) {
                    try {
                        buf.append(((org.mmbase.datatypes.BinaryDataType) dt).getMimeType(value, node, field));
                    } catch (Exception e) {
                        log.warn(e.getMessage(), e);
                    }
                } else {
                    buf.append("NULL");
                }

            }
        }
        return buf.toString();
    }

    /**
     * @see TypeHandler#htmlInput(Node, Field, boolean)
     */
    @Override
    public String htmlInput(Node node, Field field, boolean search) throws JspTagException {
        eh = getEnumHandler(node, field);
        if (eh != null) {
            log.debug("using enum handler");
            return eh.htmlInput(node, field, search);
        }
        // default implementation.
        StringBuilder show =  new StringBuilder("<input type=\"text\" class=\"small " + getClasses(node, field, search) + "\" size=\"80\" ");
        addExtraAttributes(show);
        Object value = getFieldValue(node, field, ! search);
        show.append("name=\"").append(prefix(field.getName())).append("\" ");
        show.append("id=\"").append(prefixID(field.getName())).append("\" ");
        show.append("value=\"");
        show.append((value == null ? "" : Casting.toString(value)));
        show.append("\" />");
        return show.toString();
    }
    /**
     */

    @Override
    public String htmlInputId(Node node, Field field) throws JspTagException {
        return prefixID(field.getName());
    }

    /**
     * Returns the field value as specified by the client's post. This is only <code>null</code> if
     * the client didn't post a thing. It can be empty if the client means <code>null</code>
     * (depending on the value of {@link #interpretEmptyAsNull}).
     * @param node This parameter could be used if the client does not fully specify the field's
     * value (possible e.g. with Date fields). The existing specification could be used then.
     * @return <code>null</code> if the client did not post, something else if it did. If the client
     * meant to set <code>null</code>, this method should return the empty string.
     */
    protected Object getFieldValue(Node node, Field field) throws JspTagException {
        Object found = tag.getContextProvider().getContextContainer().find(tag.getPageContext(), prefix(field.getName()));
        log.debug("found " + field.getName() + " " +  found);
        return found;
    }

    protected boolean interpretEmptyAsNull(Field field) {
        return true;
    }

    protected Object cast(Object value, Node node, Field field) {
        return field.getDataType().cast(value, node, field);
    }


    /**
     * Returns the field value to be used in the page. This is either the values returned by {@link
     * #getFieldValue(Node, Field)}, or if that returns <code>null</code>, it returns the current
     * value in the node, or if there is no node specified, the default value specified by the
     * datatype.
     */
    protected Object getFieldValue(Node node, Field field, boolean useDefault) throws JspTagException {
        Object value = getFieldValue(node, field);
        if (value == null) {
            String fieldName = field.getName();
            log.debug("No value found in context for " + fieldName);
            if (node != null) {
                value = node.isNull(fieldName) ? null : getValue(node, fieldName);
                if (log.isDebugEnabled()) {
                    log.debug("Value for " + fieldName + " found in node " + value);
                }
            } else if (useDefault) {
                value = field.getDataType().getDefaultValue(tag.getLocale(), tag.getCloudVar(), field);
                log.debug("No Node, defaultvalue found in field " + value);
            } else {
                log.debug("Using null");
            }
        }
        return value;
    }

    /**
     * @since MMBase-1.9.2
     */
    protected Object convertToValidate(Object value, Node node, Field field) throws JspTagException {
        return value;
    }


    @Override
    public String checkHtmlInput(Node node, Field field, boolean errors) throws JspTagException {
        eh = getEnumHandler(node, field);
        if (eh != null) {
            return eh.checkHtmlInput(node, field, errors);
        }
        Object fieldValue = getFieldValue(node, field);
        final DataType<?> dt = field.getDataType();
        if (fieldValue == null) {
            log.debug("Field value '" + field.getName() + "' not found in context, using existing value ");
            fieldValue = getFieldValue(node, field, node == null);
        } else if (fieldValue.equals("") && ! field.isRequired()) {
            log.debug("Field value found in context is empty, interpreting as null");
            fieldValue = null;
        }
        if (log.isDebugEnabled()) {
            log.debug("Value for field " + field + ": " + fieldValue + " and node " + node);
        }
        Collection<LocalizedString> col = dt.castAndValidate(convertToValidate(fieldValue, node, field), node, field);
        if (col.isEmpty()) {
            // do actually set the field, because some datatypes need cross-field checking
            // also in an mm:form, you can simply commit.
            if (node != null && ! field.isReadOnly()) {
                String fieldName = field.getName();
                Object oldValue = node.getValue(fieldName);
                if (fieldValue == null ? oldValue != null : ! fieldValue.equals(oldValue)) {
                    try {
                        if(log.isDebugEnabled()) {
                            log.debug("Setting " + fieldName + " to " + (fieldValue == null ? "" : fieldValue.getClass().getName()) + " " + fieldValue);
                        }
                        if ("".equals(fieldValue) && interpretEmptyAsNull(field)) {
                            setValue(node, fieldName,  null);
                        } else {
                            setValue(node, fieldName,  fieldValue);
                        }
                    } catch (Throwable t) {
                        // may throw exception like 'You cannot change the field"
                        log.warn(t.getMessage(), t);
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("not Setting " + fieldName + " to " + fieldValue + " because already has that value");
                    }
                }
            }
            if (errors && ! field.isReadOnly()) {
                return "<div id=\"" + prefixError(field.getName()) + "\" class=\"mm_check_noerror\"> </div>";
            } else {
                return "";
            }
        } else {
            FormTag form = tag.getFormTag(false, null);
            if (form != null &&  ! field.isReadOnly()) {
                form.setValid(false);
            }
            if (errors && ! field.isReadOnly()) {
                StringBuilder show = new StringBuilder("<div id=\"");
                show.append(prefixError(field.getName()));
                show.append("\" class=\"mm_check_error\">");
                Locale locale =  tag.getLocale();
                for (LocalizedString error : col) {
                    // keys typically contain dots, which is used the
                    // split the css class here.
                    show.append("<span class='").append(error.getKey().replaceAll("\\.+", " ")).append("'>");
                    // More specificly:
                    // http://www.w3.org/TR/CSS21/grammar.html#scanner
                    // Basically, a name may start with an underscore (_), a dash (-), or a letter(a–z), and then be immediately followed1) by a letter, or underscore, and THEN have any number of dashes, underscores, letters, or numbers2):

                    //-?[_a-zA-Z]+[_a-zA-Z0-9-]*
                    //

                    Xml.XMLEscape(error.get(locale), show);
                    show.append("</span>");
                }
                show.append("</div>");
                return show.toString();
            } else {
                return "";
            }
        }
    }


    /**
     * @since MMBase-1.8.6
     */
    protected void setValue(Node node, String fieldName, Object value) {
        node.setValue(fieldName, value);
    }
    /**
     * @since MMBase-1.8.6
     */
    protected Object getValue(Node node, String fieldName) {
        Object v = node.getValue(fieldName);
        if (log.isDebugEnabled()) {
            log.debug("Value for " + node.getNumber() + ":" + fieldName + ": " + v.getClass() + " " + v + " of " + node.getClass());
        }
        return v;
    }
    /**
     * @see TypeHandler#useHtmlInput(Node, Field)
     */
    @Override
    public boolean useHtmlInput(Node node, Field field) throws JspTagException {
        String fieldName = field.getName();
        Object fieldValue = getFieldValue(node, field, false);
        if (interpretEmptyAsNull(field) && "".equals(fieldValue)) fieldValue = null;
        Object oldValue = node.getValue(fieldName);
        if (fieldValue == null ? oldValue == null : fieldValue.equals(oldValue)) {
            return false;
        }  else {
            if ("".equals(fieldValue) && interpretEmptyAsNull(field)) {
                setValue(node, fieldName,  null);
            } else {
                setValue(node, fieldName,  fieldValue);
            }
            return true;
        }
    }



    /**
     * @see TypeHandler#whereHtmlInput(Field)
     */
    @Override
    public String whereHtmlInput(Field field) throws JspTagException {
        eh = getEnumHandler(null, field);
        if (eh != null) {
            return eh.whereHtmlInput(field);
        }
        String string = findString(field);
        if (string == null) return null;
        return "( [" + field.getName() + "] =" + getSearchValue(string, field, getOperator(field)) + ")";
    }



    // unused, only finalized to enforce using {@link #getOperator(Field)}
    protected final void getOperator() {
    }


    /**
     * The operator to be used by whereHtmlInput(field, query)
     * @since MMBase-1.7
     */
    protected int getOperator(Field field) {
        return FieldCompareConstraint.EQUAL;
    }

    protected final void getSearchValue(String string) {
    }
    /**
     * Converts the value to the actual value to be searched. (mainly targeted at StringHandler).
     * @since MMBase-1.7
     */
    protected String getSearchValue(String string, Field field, int operator) {
        if (operator == FieldCompareConstraint.LIKE) {
            return "%" + string + "%";
        } else {
            return string;
        }
    }

    /**
     * @since MMBase-1.7
     */
    final protected String findString(Field field) throws JspTagException {
        String fieldName = field.getName();

        String search = Casting.toString(tag.getContextProvider().getContextContainer().find(tag.getPageContext(), prefix(fieldName)));
        if (search == null || "".equals(search)) {
            return null;
        }
        return search;
    }


    @Override
    public void paramHtmlInput(ParamHandler handler, Field field) throws JspTagException  {
        eh = getEnumHandler(null, field);
        if (eh != null) {
            eh.paramHtmlInput(handler, field);
            return;
        }
        handler.addParameter(prefix(field.getName()), findString(field));
    }


    /**
     * Adds search constraint to Query object.
     * @return null if nothing to be searched, the constraint if constraint added
     */

    @Override
    public Constraint whereHtmlInput(Field field, Query query) throws JspTagException {
        eh = getEnumHandler(null, field);
        if (eh != null) {
            log.debug("Using enum");
            return eh.whereHtmlInput(field, query);
        }
        String value = findString(field);
        if (value != null) {

            String fieldName = field.getName();
            if (query.getSteps().size() > 1) {
                fieldName = field.getNodeManager().getName()+"."+fieldName;
            }
            int operator = getOperator(field);
            String searchValue = getSearchValue(value, field, operator);
            if (log.isDebugEnabled()) {
                log.debug("Found value " + value + " -> " + searchValue + " for field " + fieldName);
            }
            Constraint con = Queries.createConstraint(query, fieldName, operator, searchValue);
            Queries.addConstraint(query, con);
            return con;
        } else {
            return null;
        }
    }

    /**
     * Puts a prefix before a name. This is used in htmlInput and
     * useHtmlInput, they need it to get a reasonably unique value for
     * the name attribute of form elements.
     *
     */
    protected String prefix(String s) throws JspTagException {
        return tag.getPrefix() + "_" + s;
    }

    /**
     * Puts a prefix 'mm_' before an id in form fields. To be used in ccs etc..
     *
     * @param   s   Fieldname
     * @return  String with the id, like f.e. 'mm_title'
     */
    protected String prefixID(String s) throws JspTagException {
        String prefix = tag.getPrefix();
        return "mm_" + prefix + (prefix.length() != 0 ? "_" : "") + s;
    }

    protected String prefixError(String s) throws JspTagException {
        String prefix = tag.getPrefix();
        return "mm_check_" + prefix + (prefix.length() != 0 ? "_" : "") + s;
    }



}
