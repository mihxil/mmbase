/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge.jsp.taglib.util;

import javax.servlet.jsp.JspTagException;
import org.mmbase.bridge.jsp.taglib.TaglibException;
import org.mmbase.bridge.jsp.taglib.ContextReferrerTag;
import org.mmbase.bridge.BridgeList;
import org.mmbase.bridge.implementation.BasicList;
import javax.servlet.jsp.PageContext;
import java.util.*;
import java.text.Collator;

/**
 * A helper class for Lists, to implement an attribute 'comparator'
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 * @since MMBase-1.7
 */
public class  ListSorter  {


    public static <E extends Comparable<? super E>> List<E> sort(List<E> list, String comparator, ContextReferrerTag tag) throws JspTagException {
        if (comparator != null) {
            try {
                if (comparator.equals("SHUFFLE")) {
                    // for this entries need not be comparable
                    Collections.shuffle(list);
                }  else if (comparator.equals("REVERSE")) {
                    // for this entries need not be comparable
                    Collections.reverse(list);
                }  else if (comparator.equals("NATURAL")) {
                    Collections.sort(list);
                }  else if (comparator.equals("CASE_INSENSITIVE")) {
                    Collator col = Collator.getInstance(tag.getLocale());
                    col.setStrength(Collator.PRIMARY);
                    Collections.sort(list, col);
                } else {
                    PageContext pageContext = tag.getPageContext();
                    Class<? super E> claz = null;
                    boolean pageClass = false;
                    if (comparator.indexOf(".") == -1) {
                        Class[] classes = pageContext.getPage().getClass().getDeclaredClasses();
                        for (Class element : classes) {
                            if (element.toString().endsWith(comparator)) {
                                claz = element;
                                pageClass = true;
                                break;
                            }
                        }
                    }
                    if (claz == null) {
                        claz = (Class<E>) Class.forName(comparator);
                    }

                    if (pageClass && ! java.lang.reflect.Modifier.isStatic(claz.getModifiers())) {
                        throw new TaglibException("Don't know how to instantiate non-static inner class: " + comparator + " (make it static please)");
                    }
                    Comparator<? super E> comp = (Comparator<? super E>) claz.newInstance();
                    init(comp, pageContext);
                    Collections.sort(list, comp);
                }
            } catch (UnsupportedOperationException uoe) { // some unmodifiable list?
                // clone it.
                if (list instanceof BridgeList) {
                    list = new BasicList<E>((BridgeList<E>) list);
                } else {
                    list = new ArrayList<E>(list);
                }

                return ListSorter.sort(list, comparator, tag);
            } catch (Exception e) {
                throw new TaglibException(e);
            }
        }
        return list;

    }

    private static final Class[] PAGE_CONTEXT_ARRAY = { PageContext.class };

    private static void init(Comparator comp, PageContext pageContext) {
        try {
            java.lang.reflect.Method initMethod = comp.getClass().getMethod("init" , PAGE_CONTEXT_ARRAY);
            initMethod.invoke(comp, pageContext);
        } catch(Exception e){
            // never mind
        }
    }

    private ListSorter() {
    }


}
