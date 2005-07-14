/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.implementation.datatypes;

import java.util.*;

import org.mmbase.bridge.*;
import org.mmbase.bridge.datatypes.NodeDataType;
import org.mmbase.bridge.implementation.AbstractDataType;
import org.mmbase.module.core.MMBase;
import org.mmbase.module.core.MMObjectNode;
import org.mmbase.util.Casting;

/**
 * @javadoc
 *
 * @author Pierre van Rooden
 * @version $Id: BasicNodeDataType.java,v 1.5 2005-07-14 11:37:53 pierre Exp $
 * @see org.mmbase.bridge.DataType
 * @see org.mmbase.bridge.datatypes.NodeDataType
 * @since MMBase-1.8
 */
public class BasicNodeDataType extends AbstractDataType implements NodeDataType {

    public static final String PROPERTY_MUSTEXIST = "mustExist";
    public static final Boolean PROPERTY_MUSTEXIST_DEFAULT = Boolean.TRUE;

    /**
     * Constructor for node field.
     */
    public BasicNodeDataType(String name) {
        super(name, MMObjectNode.class);
    }

    public DataType.Property getMustExistProperty() {
        return getProperty(PROPERTY_MUSTEXIST, PROPERTY_MUSTEXIST_DEFAULT);
    }

    public void validate(Object value, Cloud cloud) {
        super.validate(value);
        if (value !=null) {
            MMObjectNode nodeValue = Casting.toNode(value,MMBase.getMMBase().getTypeDef());
            if (nodeValue == null) {
                failOnValidate(getMustExistProperty(), value, cloud);
            }
        }
    }

}
