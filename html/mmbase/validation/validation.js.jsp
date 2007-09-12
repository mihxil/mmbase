// -*- mode: javascript; -*-
<%@taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm"
%><mm:content type="text/javascript"
      expires="3600">

/**
 * See test.jspx for example usage.

 * new MMBaseValidator(window, root): attaches events to all elements in root when loading window.
 * new MMBaseValidator(window): attaches events to all elements in window when loading window.
 * new MMBaseValidator():       attaches no events yet. You could replace some function first or so.
 *
 * @author Michiel Meeuwissen
 * @version $Id: validation.js.jsp,v 1.31 2007-09-12 07:41:23 michiel Exp $
 */
function Key() {
    this.string = function() {
	return this.dataType + "," + this.field + "," + this.nodeManager;
    }

}
function MMBaseValidator(w, root) {

    this.logEnabled   = true;
    this.traceEnabled = false;

    this.dataTypeCache   = new Object();
    this.invalidElements = 0;
    this.elements        = new Array();
    this.validateHook;

    this.setup(w);
    this.root = root;

}

MMBaseValidator.prototype.log = function (msg) {
    if (this.logEnabled) {
	// firebug console"
	console.log(msg);
    }
}

MMBaseValidator.prototype.trace = function (msg) {
    if (this.traceEnabled && this.logEnabled) {
	console.log(msg);
    }
}

/**
 * Whether the element is a 'required' form input
 */
MMBaseValidator.prototype.isRequired = function(el) {
    if (el.mm_isrequired != null) return el.mm_isrequired;
    el.mm_isrequired = "true" == "" + this.getDataTypeXml(el).selectSingleNode('//dt:datatype/dt:required/@value').nodeValue;
    return el.mm_isrequired;
}

/**
 * Whether the value in the form element obeys the restrictions on length (minLength, maxLength, length)
 */
MMBaseValidator.prototype.lengthValid = function(el) {
    if (! this.isRequired(el) && el.value.length == 0) return true;
    var xml = this.getDataTypeXml(el);

    var minLength = xml.selectSingleNode('//dt:datatype/dt:minLength');
    if (minLength != null && el.value.length < minLength.getAttribute("value")) {
        return false;
    }
    var maxLength = xml.selectSingleNode('//dt:datatype/dt:maxLength');
    if (maxLength != null && el.value.length > maxLength.getAttribute("value")) {
        return false;
    }

    var length = xml.selectSingleNode('//dt:datatype/dt:length');
    if (length != null && el.value.length != length.getAttribute("value")) {
        return false;
    }
    return true;
}

// much much, too simple
MMBaseValidator.prototype.javaScriptPattern = function(javaPattern) {
    try {
        var flags = "";
        if (javaPattern.indexOf("(?i)") == 0) {
            flags += "i";
            javaPattern = javaPattern.substring(4);
        }
        if (javaPattern.indexOf("(?s)") == 0) {
            //this.log("dotall, not supported");
            javaPattern = javaPattern.substring(4);
            // I only hope this is always right....
            javaPattern = javaPattern.replace(/\./g, "(.|\\n)");
        }
        javaPattern = javaPattern.replace(/\\A/g, "\^");
        javaPattern = javaPattern.replace(/\\z/g, "\$");

        var reg = new RegExp(javaPattern, flags);
        return reg;
    } catch (ex) {
        this.log(ex);
        return null;
    }
}

MMBaseValidator.prototype.patternValid = function(el) {
    if (this.isString(el)) {
        var xml = this.getDataTypeXml(el);
        var javaPattern = xml.selectSingleNode('//dt:datatype/dt:pattern').getAttribute("value");
        var regex = this.javaScriptPattern(javaPattern);
        if (regex == null) return true;
        this.log("pattern : " + regex + " " + el.value);
        return regex.test(el.value);
    } else {
        return true;
    }
}

MMBaseValidator.prototype.hasJavaClass = function(el, javaClass) {
    var pattern = new RegExp(javaClass);
    var xml = this.getDataTypeXml(el);
    var javaClassElement = xml.selectSingleNode('//dt:datatype/dt:class');
    var name = javaClassElement.getAttribute("name");
    if (pattern.test(name)) {
        return true;
    }
    var ex = javaClassElement.getAttribute("extends");
    var javaClasses = ex.split(",");
    for (i = 0; i < javaClasses.length; i++) {
        if (pattern.test(javaClasses[i])) {
            return true;
        }
    }
    //this.log("" + el + " is not numeric");
    return false;
}

/**
 * Whether the form element represents a numeric value. There is made no difference between float,
 * double, integer and long. This means that we don't care about loss of precision only.
 */
MMBaseValidator.prototype.isNumeric = function(el) {
    if (el.mm_isnumeric != null) return el.mm_isnumeric;
    el.mm_isnumeric = this.hasJavaClass(el, "org\.mmbase\.datatypes\.NumberDataType");
    return el.isnumeric;
}
MMBaseValidator.prototype.isInteger = function(el) {
    if (el.mm_isinteger != null) return el.mm_isinteger;
    el.mm_isinteger = this.hasJavaClass(el, "(org\.mmbase\.datatypes\.IntegerDataType|org\.mmbase\.datatypes\.LongDataType)");
    return el.mm_isinteger;
}
MMBaseValidator.prototype.isFloat = function(el) {
    if (el.mm_isfloat != null) return el.mm_isfloat;
    el.mm_isfloat = this.hasJavaClass(el, "(org\.mmbase\.datatypes\.FloatDataType|org\.mmbase\.datatypes\.DoubleDataType)");
    return el.mm_isfloat;
}
MMBaseValidator.prototype.isString = function(el) {
    if (el.mm_isstring != null) return el.mm_isstring;
    el.mm_issstring =  this.hasJavaClass(el, "org\.mmbase\.datatypes\.StringDataType");
    return el.mm_isstring;
}

MMBaseValidator.prototype.isDateTime = function(el) {
    if (el.mm_isdatetime != null) return el.mm_isdatetime;
    el.mm_isdatetime = this.hasJavaClass(el, "org\.mmbase\.datatypes\.DateTimeDataType");
    return el.mm_isdatetime;
}

MMBaseValidator.prototype.INTEGER = /^[+-]?\d+$/;

MMBaseValidator.prototype.FLOAT   = /^[+-]?(\d+|\d+\.\d*|\d*\.\d+)(e[+-]?\d+|)$/i;

MMBaseValidator.prototype.typeValid = function(el) {
    if (el.value == "") return true;

    if (this.isInteger(el)) {
        if (! this.INTEGER.test(el.value)) return false;
    }
    if (this.isFloat(el)) {
        if (! this.FLOAT.test(el.value)) return false;
    }
    return true;

}

/**
 * Small utility to just get the dom attribute 'value', but also parse to float, if 'numeric' is true.
 */
MMBaseValidator.prototype.getValueAttribute = function(numeric, el) {
    if (el == null) return null;
    var value = el.getAttribute("value");
    var eval = el.getAttribute("eval");
    if (! eval == "") value = eval;

    if (numeric) {
        if (value == "") return null;
        return parseFloat(value);
    } else {
        return value;
    }
}

/**
 * Whether the value of the given form element satisfies possible restrictions on minimal and
 * maximal values. This takes into account whether it is a numeric value, which is quite important
 * for this.
 */
MMBaseValidator.prototype.minMaxValid  = function(el) {
    this.trace("validating : " + el);
    try {
        var xml = this.getDataTypeXml(el);

        var value = el.value;
        var numeric = this.isNumeric(el);
        if (numeric) {
            value = parseFloat(value);
        }

        {
            var minInclusive = xml.selectSingleNode('//dt:datatype/dt:minInclusive');
            var compare = this.getValueAttribute(numeric, minInclusive);
            if (compare != null && value <  compare) {
                this.log("" + value + " < " + compare);
                return false;
            }
        }

        {
            var minExclusive = xml.selectSingleNode('//dt:datatype/dt:minExclusive');
            var compare = this.getValueAttribute(numeric, minExclusive);
            if (compare != null && value <=  compare) {
                this.log("" + value + " <= " + compare);
                return false;
            }
        }
        {
            var maxInclusive = xml.selectSingleNode('//dt:datatype/dt:maxInclusive');
            var compare = this.getValueAttribute(numeric, maxInclusive);
            if (compare != null && value >  compare) {
                this.log("" + value + " > " + compare);
                return false;
            }
        }

        {
            var maxExclusive = xml.selectSingleNode('//dt:datatype/dt:maxExclusive');
            var compare = this.getValueAttribute(numeric, maxExclusive);
            if (compare != null && value >=  compare) {
                this.log("" + value + " >= " + compare);
                return false;
            }
        }
    } catch (ex) {
        this.log(ex);
        throw ex;
    }
    return true;

}


/**
 * Given a certain form element, this returns an XML representing its mmbase Data Type.
 * This will do a request to MMBase, unless this XML was cached already.
 */
MMBaseValidator.prototype.getDataTypeXml = function(el) {
    var key = this.getDataTypeKey(el);
    var dataType = this.dataTypeCache[key.string()];
    if (dataType == null) {

        var xmlhttp = new XMLHttpRequest();
        xmlhttp.open("GET", '<mm:url page="/mmbase/validation/datatype.jspx" />' + this.getDataTypeArguments(key), false);
        xmlhttp.send(null);
        dataType = xmlhttp.responseXML;
        try {
            dataType.setProperty("SelectionNamespaces", "xmlns:dt='http://www.mmbase.org/xmlns/datatypes'");
            dataType.setProperty("SelectionLanguage", "XPath");
        } catch (ex) {
            // happens in safari
        }
        this.dataTypeCache[key.string()] = dataType;
    }
    return dataType;
}


/**
 * All server side JSP's with which this javascript talks, can run in 2 modes. They either accept the
 * one 'datatype' parameter, or a 'field' and a 'nodemanager' parameters.
 * The result of {@link #getDataTypeKey} serves as input, and returned is a query string which can
 * be appended to the servlet path.
 */
MMBaseValidator.prototype.getDataTypeArguments = function(key) {
    if (key.dataType != null) {
        return "?datatype=" + key.dataType;
    } else {
        return "?field=" + key.field + "&nodemanager=" + key.nodeManager;
    }
}

/**
 * Given an element, returns the associated MMBase DataType as a structutre. This structure has three fields:
 * field, nodeManager and dataType. Either dataType is null or field and nodeManager are null. They
 * are all null of the given element does not contain the necessary information to identify an
 * MMBase DataType.
 */
MMBaseValidator.prototype.getDataTypeKey = function(el) {
    if (el.dataTypeStructure == null) {
        this.log("getting datatype for " + el.className);
        var classNames = el.className.split(" ");
        var result = new Key();
        for (i = 0; i < classNames.length; i++) {
            var className = classNames[i];
            if (className.indexOf("mm_dt_") == 0) {
                result.dataType = className.substring(6);
                break;
            } else if (className.indexOf("mm_f_") == 0) {
                result.field = className.substring(5);
            } else if (className.indexOf("mm_nm_") == 0) {
                result.nodeManager = className.substring(6);
            }
            if (result.field != null && result.nodeManager != null) {
                break;
            }

        }
        this.log("got " + result);
        el.dataTypeStructure = result;
    }
    return el.dataTypeStructure;
}

/**
 * If it was determined that a certain form element was or was not valid, this function
 * can be used to set an appropriate css class, so that this status also can be indicated to the
 * user using CSS.
 */
MMBaseValidator.prototype.setClassName = function(valid, el) {
    this.trace("Setting classname on " + el);
    if (el.originalClass == null) el.originalClass = el.className;
    el.className = el.originalClass + (valid ? " valid" : " invalid");
}

MMBaseValidator.prototype.hasClass = function(el, searchClass) {
    var pattern = new RegExp("(^|\\s)" + searchClass + "(\\s|$)");
    return pattern.test(el.className);
}

MMBaseValidator.prototype.getDateValue = function(el) {
    if (this.hasClass(el, "mm_datetime")) {
        var year = 0;
        var month = 0;
        var day = 0;
        var hour = 0;
        var minute = 0;
        var second = 0;
        var els = el.childNodes;
        for (var  i = 0; i < els.length; i++) {
            var entry = els[i];
            if (this.hasClass(entry, "mm_datetime_year")) {
                year = entry.value;
            } else if (this.hasClass(entry, "mm_datetime_month")) {
                month = entry.value;
            } else if (this.hasClass(entry, "mm_datetime_day")) {
                day = entry.value;
            } else if (this.hasClass(entry, "mm_datetime_hour")) {
                hour = entry.value;
            } else if (this.hasClass(entry, "mm_datetime_minute")) {
                minute = entry.value;
            } else if (this.hasClass(entry, "mm_datetime_second")) {
                second = entry.value;
            }

        }
        var date = new Date(year, month - 1, day, hour , minute, second, 0);
        this.log("date " + date);
        return date.getTime() / 1000;
    } else {
        return e.value;
    }

}

/**
 * Returns whether a form element contains a valid value. I.e. in a fast way, validation is done in
 * javascript, and therefore cannot be absolute.
 */
MMBaseValidator.prototype.valid = function(el) {
    if (this.isDateTime(el)) {
        el.value = this.getDateValue(el);
    }
    if (typeof(el.value) == 'undefined') {
        this.log("Unsupported element " + el);
        return true; // not yet supported
    }

    if (this.isRequired(el)) {
        if (el.value == "") return false;
    } else {
        if (el.value == "") return true;
    }

    if (! this.typeValid(el)) return false;
    if (! this.lengthValid(el)) return false;
    if (! this.minMaxValid(el)) return false;
    if (! this.patternValid(el)) return false; // not perfect yet

    // @todo of course we can go a bit further here.

    // datetime validation is still broken. (those can have more fields and so on)

    // enumerations: but must of the time those would have given dropdowns and such, so it's hardly
    // possible to enter wrongly.
    //


    return true;
}

/**
 * Determins whether a form element contains a valid value, according to the server.
 * Returns an XML containing the reasons why it would not be valid.
 */
MMBaseValidator.prototype.serverValidation = function(el) {
    try {
        var key = this.getDataTypeKey(el);
        var xmlhttp = new XMLHttpRequest();
        var value = this.getDateValue(el);
        xmlhttp.open("GET", '<mm:url page="/mmbase/validation/valid.jspx" />' + this.getDataTypeArguments(key) + "&value=" + value, false);
        xmlhttp.send(null);
        return xmlhttp.responseXML;
    } catch (ex) {
        this.log(ex);
        throw ex;
    }
}

/**
 * The result of {@link #serverValidation} is parsed, and converted to a simple boolean
 */
MMBaseValidator.prototype.validResult = function(xml) {
    try {
        return "true" == "" + xml.selectSingleNode('/result/@valid').nodeValue;
    } catch (ex) {
        this.log(ex);
        throw ex;
    }
}

/**
 * Cross browser hack.
 */
MMBaseValidator.prototype.target = function(event) {
    return event.target || event.srcElement;
}
/**
 * The event handler which is linked to form elements
 * A 'validateHook' is called in this function, which you may want to set, in stead of
 * overriding this function.
 */
MMBaseValidator.prototype.validate = function(event) {
    this.log("event" + event + " on " + this.target(event));
    var target = this.target(event);
    if (this.hasClass(target, "mm_validate")) {
        this.validateElement(target);
    } else if (this.hasClass(target.parentNode, "mm_validate")) {
        this.validateElement(target.parentNode);
    }
}

MMBaseValidator.prototype.validateElement = function(element, server) {
    var valid;
    this.log("Validating" + element);
    if (server) {
        valid = this.validResult(this.serverValidation(element));
    } else {
        valid = this.valid(element);
    }
    if (valid != element.prevValid) {
        if (valid) {
            this.invalidElements--;
        } else {
            this.invalidElements++;
        }
    }
    element.prevValid = valid;
    this.setClassName(valid, element);
    if (this.validateHook) {
        this.validateHook(valid, element);
    }
}

/**
 * Validates al mm_validate form entries which were marked for validation with addValidation.
 */
MMBaseValidator.prototype.validatePage = function(server) {
    var els = this.elements;
    for (var  i = 0; i < els.length; i++) {
        var entry = els[i];
        this.validateElement(entry, server);
    }
    return this.invalidElements == 0;
}

/**
 * Adds event handlers to all mm_validate form entries
 */
MMBaseValidator.prototype.addValidation = function(el) {
    if (el == null) {
        el = document.documentElement;
    }
    this.log("Will validate " + el);

    var els = getElementsByClass(el, "mm_validate");
    for (var i = 0; i < els.length; i++) {
        var entry = els[i];
        if (entry.type == "textarea") {
            entry.value = entry.value.replace(/^\s+|\s+$/g, "");
        }
        // switch stolen from editwizards, not all cases are actually supported already here.
        switch(entry.type) {
        case "text":
        case "textarea":
            addEventHandler(entry, "keyup", this.validate, this);
            addEventHandler(entry, "change", this.validate, this);
            addEventHandler(entry, "blur", this.validate, this);
            // IE calls this when the user does a right-click paste
            addEventHandler(entry, "paste", this.validate, this);
            // FireFox calls this when the user does a right-click paste
            addEventHandler(entry, "input", this.validate, this);
            break;
        case "radio":
        case "checkbox":
            addEventHandler(entry, "click", this.validate, this);
            addEventHandler(entry, "blur", this.validate, this);
            break;
        case "select-one":
        case "select-multiple":
        default:
            this.log("Adding eventhandler to " + entry);
            addEventHandler(entry, "change", this.validate, this);
            addEventHandler(entry, "blur", this.validate, this);
        }

        var valid = this.valid(entry);
        entry.prevValid = valid;
        this.elements.push(entry);
        this.setClassName(this.valid(entry), entry);
        if (!valid) {
            this.invalidElements++;
        }
        if (this.validateHook) {
            this.validateHook(valid, entry);
        }

    }
    el = null;
}
MMBaseValidator.prototype.onLoad = function(event) {
    if (this.root == null) {
        this.root = event.target || event.srcElement;
    }

    this.addValidation(this.root);
    //validatePage(target);
}


MMBaseValidator.prototype.setup = function(w) {
    if (w != null) {
        addEventHandler(w, "load", this.onLoad, this);
    }
}



</mm:content>
