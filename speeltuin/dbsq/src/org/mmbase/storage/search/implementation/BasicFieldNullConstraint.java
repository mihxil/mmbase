package org.mmbase.storage.search.implementation;

import org.mmbase.storage.search.*;

/**
 * Basic implementation.
 *
 * @author Rob van Maris
 * @version $Revision: 1.6 $
 */
public class BasicFieldNullConstraint extends BasicFieldConstraint 
implements FieldNullConstraint {
    
    /**
     * Constructor.
     *
     * @param field The associated field.
     * @throws IllegalArgumentException when an invalid argument is supplied.
     */
    public BasicFieldNullConstraint(StepField field) {
        super(field);
    }
    
    // javadoc is inherited
    public boolean equals(Object obj) {
        // Must be same class (subclasses should override this)!
        if (obj != null && obj.getClass() == getClass()) {
            BasicFieldNullConstraint constraint = (BasicFieldNullConstraint) obj;
            return isInverse() == constraint.isInverse()
                && isCaseSensitive() == constraint.isCaseSensitive()
                && getField().getFieldName().equals(constraint.getField().getFieldName())
                && getField().getStep().getAlias().equals(
                    constraint.getField().getStep().getAlias());
        } else {
            return false;
        }
    }
    
    // javadoc is inherited
    public int hashCode() {
        return super.hashCode();
    }

    // javadoc is inherited
    public String toString() {
        StringBuffer sb = new StringBuffer("FieldNullConstraint(inverse:").
        append(isInverse()).
        append(", field:").
        append(getField().getAlias()).
        append(", casesensitive:").
        append(isCaseSensitive()).
        append(")");
        return sb.toString();
    }
}
