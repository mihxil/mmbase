package nl.didactor.taglib.table;
import java.io.IOException;
import java.io.*;
import java.util.*;
import java.text.*;
import javax.servlet.jsp.tagext.*;
import javax.servlet.jsp.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.mmbase.bridge.jsp.taglib.*;
import org.mmbase.module.core.*;

class Util {
    private static ArrayList defaultDisallowedParameters;
    private static Properties defaultHtml;

    static {
        defaultDisallowedParameters = new ArrayList();
        defaultDisallowedParameters.add("username");
        defaultDisallowedParameters.add("password");
        defaultDisallowedParameters.add("authenticate");
        defaultDisallowedParameters.add("command");

        defaultHtml = new Properties();
        defaultHtml.setProperty("table.start",  "<table class=\"didactortable\">");
        defaultHtml.setProperty("table.end",  "</table>");
        defaultHtml.setProperty("surrounding.start", "<div>");
        defaultHtml.setProperty("surrounding.end", "</div>");
        defaultHtml.setProperty("paging.top.start", "<div>");
        defaultHtml.setProperty("paging.top.end", "</div>");
        defaultHtml.setProperty("paging.bottom.start", "<div>");
        defaultHtml.setProperty("paging.bottom.end", "</div>");
        defaultHtml.setProperty("paging.arrow.class", "paging");
        defaultHtml.setProperty("paging.number.class", "paging");
        defaultHtml.setProperty("cell.start", "<td>");
        defaultHtml.setProperty("cell.end", "</td>");
        defaultHtml.setProperty("row.start", "<tr>");
        defaultHtml.setProperty("row.end", "</tr>");
        defaultHtml.setProperty("headercell.start", "<th>");
        defaultHtml.setProperty("headercell.end", "</th>");
        defaultHtml.setProperty("sorting.up.label", "[up]");
        defaultHtml.setProperty("sorting.down.label", "[down]");
        defaultHtml.setProperty("sorting.none.label", "[none]");
    }

    public static Hashtable getHtmlLabels(String configpath) {
        Properties retval = (Properties) defaultHtml.clone();
        try {
            retval.load(new FileInputStream(new File(configpath)));
        } catch (IOException e) {

        }
        return retval;
    }

    protected static StringBuffer getCurrentUrl(PageContext pageContext, ArrayList disallowed) {
        HttpServletRequest req = (HttpServletRequest)pageContext.getRequest();
        StringBuffer url = req.getRequestURL();
        Map m = req.getParameterMap();
        Iterator params = m.entrySet().iterator();
        char connector = '?';
        while (params.hasNext()) {
            Map.Entry entry = (Map.Entry)params.next();
            String key = (String)entry.getKey();
            if (!disallowed.contains(key) && !defaultDisallowedParameters.contains(key)) {
                String[] values = (String[])entry.getValue();
                for (int i=0; i<values.length; i++) {
                    url.append(connector).append(key).append('=').append(values[i]);
                    connector = '&';
                }
            }
        }
        url.append(connector);
        return url;
    }
}
