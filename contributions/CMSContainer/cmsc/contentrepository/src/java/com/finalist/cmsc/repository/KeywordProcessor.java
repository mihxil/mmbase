/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package com.finalist.cmsc.repository;

import java.util.*;

import net.sf.mmapps.commons.util.KeywordUtil;
import net.sf.mmapps.commons.util.StringUtil;

import org.mmbase.bridge.*;
import org.mmbase.datatypes.StringDataType;
import org.mmbase.datatypes.processors.CommitProcessor;


@SuppressWarnings("serial")
public class KeywordProcessor implements CommitProcessor {

    public void commit(Node node, Field field) {
        if (!ContentElementUtil.isContentElement(node)) {
            throw new IllegalArgumentException(
                "Processor only works on ContentElement types, not on: "
                        + node.getNodeManager().getName());
        }
        if (StringUtil.isEmpty(node.getStringValue(field.getName()))) {
            List<String> textFields = new ArrayList<String>();
            
            FieldList fields = node.getNodeManager().getFields();
            for (Iterator<Field> iter = fields.iterator(); iter.hasNext();) {
                Field managerField = iter.next();
                
                if(managerField.getDataType() instanceof StringDataType
                    && !((StringDataType)managerField.getDataType()).isPassword() 
                    && !managerField.isVirtual()
                    && !ContentElementUtil.isContentElementField(managerField)){

                    String text = node.getStringValue(managerField.getName());
                    if (!StringUtil.isEmptyOrWhitespace(text)) {
                        text = text.replaceAll("<.+?>", "");
                        textFields.add(text);
                    }
                }
            }
            String title = node.getStringValue(ContentElementUtil.TITLE_FIELD);
            if (!StringUtil.isEmptyOrWhitespace(title)) {
                textFields.add(title);
            }
            
            List<String> keywords = KeywordUtil.getKeywords(textFields, 50);
            String keywordStr = KeywordUtil.keywordsToString(keywords);
            if (!StringUtil.isEmpty(keywordStr)) {
                node.setValue(field.getName(), keywordStr);
            }
        }
    }

}
