package org.mmbase.util;
import org.mmbase.util.transformers.CharTransformer;
import java.util.*;

/**
 * @since MMBase-2.0
 */
public interface Caster {

    <C> C toType(Class<C> type, Object cloud, Object value) throws NotRecognized;
    Object wrap(final Object o, final CharTransformer escaper) throws NotRecognized;
    Object unWrap(final Object o) throws NotRecognized;
    Map toMap(Object o) throws NotRecognized;
    int toInt(Object i) throws NotRecognized;
    long toLong(Object i) throws NotRecognized;
    float toFloat(Object i) throws NotRecognized;
    double toDouble(Object i) throws NotRecognized;

    public static class NotRecognized extends Exception {
        static NotRecognized INSTANCE = new NotRecognized();
        protected NotRecognized() {
        }

    }

    /**
     * Clases implementing this will not be wrapped by {@link #wrap}, even if the e.g. are CharSequence.
     * @since MMBase-1.9
     */
    public static interface Unwrappable {
    }

}