<?xml version="1.0" encoding="utf-8" ?>
<xsl:stylesheet 
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:node="org.mmbase.bridge.util.xml.NodeFunction"
  xmlns:date="org.mmbase.bridge.util.xml.DateFormat"
  extension-element-prefixes="node date">
  <!--
    wizard.xsl
    
    @since  MMBase-1.6
    @author Kars Veling
    @author Michiel Meeuwissen
    @author Pierre van Rooden
    @author Nico Klasens
    @version $Id: wizard.xsl,v 1.2 2003-12-14 16:20:55 nico Exp $
    
    This xsl uses Xalan functionality to call java classes
    to format dates and call functions on nodes
    See the xmlns attributes of the xsl:stylesheet
  -->

  <xsl:import href="xsl/base.xsl" />

  <xsl:variable name="date-pattern">dd MMMM yyyy HH:mm</xsl:variable>
  <xsl:param name="objectnumber" />

  <!-- ================================================================================
    The following things can be overriden to customize the appearance of wizard
    ================================================================================ -->

  <xsl:variable name="BodyOnLoad">doOnLoad_ew(); start_validator();</xsl:variable>
  <xsl:variable name="BodyOnunLoad">doOnUnLoad_ew();</xsl:variable>

  <xsl:template name="javascript">
    <script type="text/javascript" src="{$javascriptdir}tools.js">
      <xsl:comment>help IE</xsl:comment>
    </script>
    <script type="text/javascript" src="{$javascriptdir}date.js">
      <xsl:comment>help IE</xsl:comment>
    </script>
    <script type="text/javascript" src="{$javascriptdir}validator.js">
      <xsl:comment>help IE</xsl:comment>
    </script>
    <script type="text/javascript" src="{$javascriptdir}editwizard.jsp{$sessionid}?referrer={$referrer}&amp;language={$language}">
      <xsl:comment>help IE</xsl:comment>
    </script>
    <script type="text/javascript">
      <xsl:text disable-output-escaping="yes">
      <![CDATA[
      <!--
        // Mozilla does not like this.
        var hist = window.history;
        if (hist) {
            hist.go(1);
        }
        window.onresize = function(e){ resizeEditTable(); }
      -->
      ]]>
      </xsl:text>
    </script>

    <!-- SEARCH_LIST_TYPE is defined in the base.xsl-->
    <xsl:choose>
      <xsl:when test="$SEARCH_LIST_TYPE=&apos;IFRAME&apos;">
        <script type="text/javascript">
          <xsl:text disable-output-escaping="yes">
          <![CDATA[
          <!--
            document.writeln('<div id="searchframe" class="searchframe"><iframe onblur="removeModalIFrame();" src="searching.html" id="modaliframe" class="searchframe" scrolling="no"></iframe></div>');
          -->
          ]]>
          </xsl:text>
        </script>
        <script type="text/javascript" src="{$javascriptdir}searchiframe.js">
          <xsl:comment>help IE</xsl:comment>
        </script>
      </xsl:when>
      <xsl:otherwise>
        <script type="text/javascript" src="{$javascriptdir}searchwindow.js">
          <xsl:comment>help IE</xsl:comment>
        </script>
      </xsl:otherwise>	
    </xsl:choose>
    
    <xsl:if test="//*[@ftype=&apos;html&apos;]">
	    <xsl:call-template name="javascript-html"/>
    </xsl:if>
  </xsl:template>

  <xsl:template name="javascript-html">
      <!-- no need to add the wysiwyg button bar if there are not fields of this type -->
      <script type="text/javascript" src="{$javascriptdir}wysiwyg.js">
        <xsl:comment>help IE</xsl:comment>
      </script>
  </xsl:template>

  <xsl:template name="htmltitle">
    <xsl:value-of select="$wizardtitle" />
    -
    <xsl:call-template name="i18n">
      <xsl:with-param name="nodes" select="form[@id=/wizard/curform]/title" />
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="style">
    <link rel="stylesheet" type="text/css" href="{$cssdir}layout/wizard.css" />
  </xsl:template>

  <xsl:template name="colorstyle">
    <link rel="stylesheet" type="text/css" href="{$cssdir}color/wizard.css" />
  </xsl:template>

  <xsl:template name="title">
    <td>
        <xsl:value-of select="$wizardtitle" />
    </td>
    <td>
        <xsl:call-template name="help" />
        <xsl:call-template name="debug" />
    </td>
  </xsl:template>

  <xsl:template name="subtitle">
    <td colspan="2">
      <xsl:call-template name="i18n">
        <xsl:with-param name="nodes" select="/wizard/form[@id=/wizard/curform]/subtitle" />
      </xsl:call-template>
    </td>
  </xsl:template>

  <xsl:template name="help">
		<xsl:if test="/wizard/description">
			<span class="imgbutton" title="{$tooltip_help}" onclick="doHelp();">
				<xsl:call-template name="prompt_help" />
			</span>
			<div id="help_text" style="position:absolute; display:none;">
				<xsl:call-template name="i18n">
					<xsl:with-param name="nodes" select="/wizard/description" />
				</xsl:call-template>
			</div>
		</xsl:if>
  </xsl:template>

  <xsl:template name="debug">
		<xsl:if test="$debug=&apos;true&apos;">
			<a href="debug.jsp{$sessionid}?sessionkey={$sessionkey}&amp;popupid={$popupid}" target="_blank">
				<xsl:call-template name="prompt_debug" />
			</a>
		</xsl:if>
  </xsl:template>

  <xsl:template name="body">
    <xsl:apply-templates select="wizard" />
  </xsl:template>

  <xsl:template match="wizard">
    <xsl:call-template name="beforeform" />
    <xsl:call-template name="bodyform" />
  </xsl:template>

  <xsl:template name="beforeform" >
    <!--tr class="beforeformcanvas">
        <td>
        </td>
      </tr-->
  </xsl:template>

  <!-- Every wizard is based on one 'form', 
  	with a bunch of attributes and a few hidden entries.
    Those are set here -->
  <xsl:template name="bodyform">
		<tr class="formcanvas">
			<td>
				<form
					name="form"
					method="post"
					action="{$formwizardpage}"
					id="{/wizard/curform}"
					message_pattern="{$message_pattern}"
					message_required="{$message_required}"
					message_minlength="{$message_minlength}"
					message_maxlength="{$message_maxlength}"
					message_min="{$message_min}"
					message_max="{$message_max}"
					message_mindate="{$message_mindate}"
					message_maxdate="{$message_maxdate}"
					message_dateformat="{$message_dateformat}"
					message_thisnotvalid="{$message_thisnotvalid}"
					message_notvalid="{$message_notvalid}"
					message_listtooshort="{$message_listtooshort}"
					invalidlist="{/wizard/form[@invalidlist]/@invalidlist}"
					filter_required="{$filter_required}">
					<input type="hidden" name="curform" value="{/wizard/curform}" />
					<input type="hidden" name="cmd" value="" id="hiddencmdfield" />
		
					<xsl:call-template name="formwizardargs" />
					<xsl:call-template name="formcontent" />
				</form>
			</td>
		</tr>
  </xsl:template>

  <xsl:template name="formcontent">
		<xsl:apply-templates select="/*/steps-validator" />
		<div id="edit_table" class="editform">
			<table class="formcontent">
				<xsl:apply-templates select="form[@id=/wizard/curform]" />
			</table>
		</div>
  </xsl:template>

<!--
	Below this comment are the templates which create the navigational element of the
	wizard screen. The navigational elements consists of the steps and buttons.
	A wizard can be split up into multiple forms. Every form is a step in the wizard.
-->

  <xsl:template match="steps-validator">
    <!-- when multiple steps, otherwise do nothing -->
    <xsl:if test="count(step) > 1">
        <xsl:call-template name="steps" />
    </xsl:if>
    <xsl:call-template name="buttons" />
  </xsl:template>

  <!-- The steps buttons are only created (in steps-validator) if there is more than one step -->
  <xsl:template name="steps">
    <table class="stepscontent">
      <tr>
        <td class="stepprevious">
          <xsl:call-template name="previousbutton" />
        </td>
        <xsl:for-each select="step">
          <xsl:call-template name="steptemplate" />
        </xsl:for-each>
        <td class="stepnext">
          <xsl:call-template name="nextbutton" />
        </td>
        <td/>
      </tr>
    </table>
  </xsl:template>

  <xsl:template name="steptemplate">
    <td>
      <xsl:variable name="schemaid" select="@form-schema" />
      <xsl:attribute name="class">
        <xsl:choose>
          <xsl:when test="@form-schema=/wizard/curform">stepcurrent</xsl:when>
          <xsl:otherwise>stepother</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <xsl:call-template name="step" />
    </td>
  </xsl:template>

  <!-- The appearance of one 'step' button -->
  <xsl:template name="step">
    <a>
      <!-- xsl:attribute name="class">
        <xsl:choose>
          <xsl:when test="@form-schema=/wizard/curform">current</xsl:when>
          <xsl:otherwise>other</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute -->
      <xsl:call-template name="stepsattributes" />

      <xsl:call-template name="i18n">
        <xsl:with-param name="nodes" select="/*/form[@id=current()/@form-schema]/title" />
      </xsl:call-template>
      <!-- xsl:call-template name="prompt_step" /-->
    </a>
  </xsl:template>

  <xsl:template name="stepsattributes">
    <xsl:attribute name="href">javascript:doGotoForm('<xsl:value-of select="@form-schema" />');</xsl:attribute>
    <xsl:attribute name="titlevalid"><xsl:value-of select="$tooltip_valid" /></xsl:attribute>
    <xsl:attribute name="id">step-<xsl:value-of select="@form-schema" /></xsl:attribute>
    <xsl:attribute name="titlenotvalid"><xsl:value-of select="$tooltip_not_valid" /></xsl:attribute>
    <xsl:attribute name="title"><xsl:value-of select="/*/form[@id=current()/@form-schema]/title" />
    	<xsl:if test="@valid=&apos;false&apos;">
        <xsl:value-of select="$tooltip_step_not_valid" />
      </xsl:if>
    </xsl:attribute>
    <xsl:attribute name="class">
      <xsl:if test="@valid=&apos;true&apos;">valid</xsl:if>
      <xsl:if test="@valid=&apos;false&apos;">notvalid</xsl:if>
    </xsl:attribute>
  </xsl:template>

  <xsl:template name="previousbutton">
    <xsl:choose>
      <xsl:when test="/wizard/form[@id=/wizard/prevform]">
        <a class="step"
          href="javascript:doGotoForm(&apos;{/wizard/prevform}&apos;)"
          title="{$tooltip_previous} &apos;{/wizard/form[@id=/wizard/prevform]/title}&apos;">
          <xsl:call-template name="prompt_previous" />
        </a>
      </xsl:when>
      <xsl:otherwise>
        <span class="step-disabled" title="{$tooltip_no_previous}">
          <!-- xsl:call-template name="prompt_previous" / -->
        </span>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="nextbutton">
    <xsl:choose>
      <xsl:when test="/wizard/form[@id=/wizard/nextform]">
        <a class="step"
          href="javascript:doGotoForm(&apos;{/wizard/nextform}&apos;)"
          title="{$tooltip_next} &apos;{/wizard/form[@id=/wizard/nextform]/title}&apos;">
          <xsl:call-template name="prompt_next" />
        </a>
      </xsl:when>
      <xsl:otherwise>
        <span class="step-disabled" title="{$tooltip_no_next}">
          <!-- xsl:call-template name="prompt_next" / -->
        </span>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- Buttons are (always) called in the steps-validator -->
  <xsl:template name="buttons">
    <div id="commandbuttonbar" class="buttonscontent">
      <table class="buttonscontent">
        <tr>
          <td>
						<!-- cancel -->
						<xsl:call-template name="cancelbutton" />
						-
						<!-- commit  -->
						<xsl:call-template name="savebutton" />
						-
						<!-- Saveonly  -->
						<xsl:call-template name="saveonlybutton"/>
          </td>
        </tr>
      </table>
    </div>
  </xsl:template>

  <xsl:template name="savebutton">
    <a href="javascript:doSave();" id="bottombutton-save" unselectable="on" titlesave="{$tooltip_save}" titlenosave="{$tooltip_no_save}">
      <xsl:if test="@allowsave='true'">
        <xsl:attribute name="class">bottombutton</xsl:attribute>
        <xsl:attribute name="title">
          <xsl:value-of select="$tooltip_save"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="@allowsave='false'">
        <xsl:attribute name="class">bottombutton-disabled</xsl:attribute>
        <xsl:attribute name="title">
          <xsl:value-of select="$tooltip_no_save"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:choose>
        <xsl:when test="step[@valid='false'][not(@form-schema=/wizard/curform)]">
          <xsl:attribute name="otherforms">invalid</xsl:attribute>
        </xsl:when>
        <xsl:otherwise>
          <xsl:attribute name="otherforms">valid</xsl:attribute>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:call-template name="prompt_save"/>
    </a>
  </xsl:template>

  <!-- don't want the save-only button?. Put this in your wizard.xsl extension 
  <xsl:template name="saveonlybutton">
    <a id="bottombutton-saveonly" />
  </xsl:template>
  -->

  <xsl:template name="saveonlybutton">
    <a href="javascript:doSaveOnly();" id="bottombutton-saveonly" unselectable="on" titlesave="{$tooltip_save_only}" titlenosave="{$tooltip_no_save}">
      <xsl:if test="@allowsave='true'">
        <xsl:attribute name="class">bottombutton</xsl:attribute>
        <xsl:attribute name="title">
          <xsl:value-of select="$tooltip_save_only"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="@allowsave='false'">
        <xsl:attribute name="class">bottombutton-disabled</xsl:attribute>
        <xsl:attribute name="title">
          <xsl:value-of select="$tooltip_no_save"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:choose>
        <xsl:when test="step[@valid='false'][not(@form-schema=/wizard/curform)]">
          <xsl:attribute name="otherforms">invalid</xsl:attribute>
        </xsl:when>
        <xsl:otherwise>
          <xsl:attribute name="otherforms">valid</xsl:attribute>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:call-template name="prompt_save_only"/>
    </a>
  </xsl:template>

  <xsl:template name="cancelbutton">
    <a href="javascript:doCancel();" id="bottombutton-cancel" class="bottombutton" title="{$tooltip_cancel}">
      <xsl:call-template name="prompt_cancel"/>
    </a>
  </xsl:template>

<!--
	Below this comment are the templates which create the edit part of the wizard screen.
	The edit part consists of input fields and list of related objects. The fields can be 
	grouped with fieldsets.
-->

  <xsl:template match="form">
		<xsl:for-each select="fieldset|field|list">
			<xsl:choose>
				<xsl:when test="name()='field'">
					<tr class="fieldcanvas">
						<xsl:apply-templates select="." />
					</tr>
				</xsl:when>
				<xsl:when test="name()='list'">
					<tr class="listcanvas">
						<td colspan="2">
							<xsl:apply-templates select="." />
						</td>
					</tr>
				</xsl:when>
				<xsl:when test="name()='fieldset'">
					<tr class="fieldsetcanvas">
						<xsl:apply-templates select="." />
					</tr>
				</xsl:when>
				<xsl:otherwise>
					<xsl:apply-templates select="." />
				</xsl:otherwise>
			</xsl:choose>
		</xsl:for-each>
  </xsl:template>

	<!-- How should we visualize the grouping of fields? -->
  <xsl:template match="fieldset">
			<td class="fieldprompt">
				<xsl:call-template name="prompt"/>
			</td>
			<td class="field">
				<xsl:for-each select="field">
					<xsl:call-template name="fieldintern" />
					<br/>
				</xsl:for-each>
			</td>
  </xsl:template>

  <xsl:template match="field">
		<td class="fieldprompt">
			<xsl:call-template name="prompt" />
			<xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
		</td>
		<td class="field">
			<xsl:call-template name="fieldintern" />
			<xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
		</td>
  </xsl:template>

  <xsl:template name="prompt">
    <span id="prompt_{@fieldname}" class="valid" prompt="{prompt}">
      <xsl:choose>
        <xsl:when test="description">
          <xsl:attribute name="title">
            <xsl:call-template name="i18n">
              <xsl:with-param name="nodes" select="description" />
            </xsl:call-template>
          </xsl:attribute>
          <xsl:attribute name="description">
            <xsl:call-template name="i18n">
              <xsl:with-param name="nodes" select="description" />
            </xsl:call-template>
          </xsl:attribute>
        </xsl:when>
        <xsl:otherwise>
          <xsl:attribute name="title">
            <xsl:call-template name="i18n">
              <xsl:with-param name="nodes" select="prompt" />
            </xsl:call-template>
          </xsl:attribute>
          <xsl:attribute name="description">
            <xsl:call-template name="i18n">
              <xsl:with-param name="nodes" select="prompt" />
            </xsl:call-template>
          </xsl:attribute>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:call-template name="i18n">
        <xsl:with-param name="nodes" select="prompt" />
      </xsl:call-template>
    </span>
  </xsl:template>

  <!-- ================================================================================
    fieldintern is called to draw the values
  -->
  <xsl:template name="fieldintern">
    <xsl:apply-templates select="prefix" />

    <xsl:choose>
      <xsl:when test="@ftype=&apos;function&apos;">
      	<xsl:call-template name="ftype-function"/>
      </xsl:when>
      <xsl:when test="@ftype=&apos;data&apos;">
      	<xsl:call-template name="ftype-data"/>
      </xsl:when>
      <xsl:when test="@ftype=&apos;line&apos;">
      	<xsl:call-template name="ftype-line"/>
      </xsl:when>
      <xsl:when test="@ftype=&apos;text&apos;">
      	<xsl:call-template name="ftype-text"/>
      </xsl:when>
      <xsl:when test="@ftype=&apos;html&apos;">
      	<xsl:call-template name="ftype-html"/>
      </xsl:when>
      <xsl:when test="@ftype=&apos;relation&apos;">
      	<xsl:call-template name="ftype-relation"/>      	
      </xsl:when>
      <xsl:when test="@ftype=&apos;enum&apos;">
      	<xsl:call-template name="ftype-enum"/>
      </xsl:when>
      <xsl:when test="@ftype=&apos;enumdata&apos;">
      	<xsl:call-template name="ftype-enumdata"/>
      </xsl:when>
      <xsl:when test="(@ftype=&apos;datetime&apos;) or (@ftype=&apos;date&apos;) or (@ftype=&apos;time&apos;)">
      	<xsl:call-template name="ftype-datetime"/>
      </xsl:when>
      <xsl:when test="@ftype=&apos;image&apos;">
      	<xsl:call-template name="ftype-image"/>
      </xsl:when>
      <xsl:when test="@ftype=&apos;file&apos;">
      	<xsl:call-template name="ftype-file"/>
      </xsl:when>
      <xsl:when test="@ftype=&apos;realposition&apos;">
      	<xsl:call-template name="ftype-realposition"/>
      </xsl:when>
      <xsl:otherwise>
      	<xsl:call-template name="ftype-other"/>
      </xsl:otherwise>
    </xsl:choose>
    
    <xsl:apply-templates select="postfix" />
  </xsl:template>

  <!-- 
    Prefix and postfix are subtags of 'field', and are put respectively before and after the presentation of the field.
    Useful in fieldsets.
  -->
  <xsl:template match="prefix|postfix">
    <xsl:value-of select="." />
  </xsl:template>

	<xsl:template name="ftype-function">
		<xsl:if test="not(string(number(@number)) = &apos;NaN&apos;)">
			<xsl:apply-templates select="value" mode="line">
				<xsl:with-param name="val">
					<xsl:value-of
						select="node:function($cloud, string(@number), string(value))"
						disable-output-escaping="yes" />
				</xsl:with-param>
			</xsl:apply-templates>
		</xsl:if>
	</xsl:template>

	<xsl:template name="ftype-data">
		<xsl:choose>
			<xsl:when test="@dttype=&apos;datetime&apos;">
				 <xsl:value-of
						select="date:format(string(value), $date-pattern)"
						disable-output-escaping="yes" />
			</xsl:when>
			<xsl:otherwise>
		<xsl:apply-templates select="value" mode="line" />
			 </xsl:otherwise>
		 </xsl:choose>
	</xsl:template>		

	<xsl:template name="ftype-line">
		<xsl:apply-templates select="value" mode="inputline" />
	</xsl:template>		

	<xsl:template name="ftype-html">
		<xsl:call-template name="ftype-text"/>
  </xsl:template>

	<xsl:template name="ftype-text">
		<span>
			<xsl:text disable-output-escaping="yes">&lt;textarea name="</xsl:text>
			<xsl:value-of select="@fieldname" />
			<xsl:text>" dttype="</xsl:text>
			<xsl:value-of select="@dttype" />
			<xsl:text>" ftype="</xsl:text>
			<xsl:value-of select="@ftype" />
			<xsl:text>" dtminlength="</xsl:text>
			<xsl:value-of select="@dtminlength" />
			<xsl:text>" dtmaxlength="</xsl:text>
			<xsl:value-of select="@dtmaxlength" />
			<xsl:text>" class="input" wrap="soft" fdatapath="</xsl:text>
			<xsl:value-of select="@fdatapath" />
			<xsl:text>"</xsl:text>
			<xsl:choose>
				<xsl:when test="@cols">
					<xsl:text>cols="</xsl:text>
					<xsl:value-of select="@cols" />
					<xsl:text>"</xsl:text>
				</xsl:when>
				<xsl:otherwise>
					<xsl:text>cols="80"</xsl:text>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:choose>
				<xsl:when test="@rows">
					<xsl:text>rows="</xsl:text>
					<xsl:value-of select="@rows" />
					<xsl:text>"</xsl:text>
				</xsl:when>
				<xsl:otherwise>
					<xsl:text>rows="10"</xsl:text>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:apply-templates select="@*" />
			<xsl:text disable-output-escaping="yes">></xsl:text>
			<xsl:value-of disable-output-escaping="yes" select="value" />
			<xsl:text disable-output-escaping="yes">&lt;/textarea></xsl:text>
			<xsl:apply-templates select="postfix" />
		</span>
	</xsl:template>		

	<xsl:template name="ftype-relation">
    <xsl:call-template name="ftype-enum"/>
	</xsl:template>		
		
	<xsl:template name="ftype-enum">
		<select
			name="{@fieldname}"
			class="selectinput">
			<xsl:apply-templates select="@*" />
			<xsl:choose>
				<xsl:when test="optionlist/option[@selected=&apos;true&apos;]" />
				<xsl:when test="@dtrequired=&apos;true&apos;" />
				<xsl:otherwise>
					<option value="-">
						<xsl:call-template name="prompt_select" />
					</option>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:for-each select="optionlist/option">
				<option value="{@id}">
					<xsl:if test="@selected=&apos;true&apos;">
						<xsl:attribute name="selected">true</xsl:attribute>
					</xsl:if>
					<xsl:value-of select="." />
				</option>
			</xsl:for-each>
		</select>
	</xsl:template>		

	<xsl:template name="ftype-enumdata">
		<xsl:if test="optionlist/option[@id=current()/value]">
			<xsl:apply-templates select="value" mode="line">
				<xsl:with-param name="val">
					<xsl:value-of select="optionlist/option[@id=current()/value]" />
				</xsl:with-param>
			</xsl:apply-templates>
		</xsl:if>
	</xsl:template>		

	<xsl:template name="ftype-datetime">
		<div>
			<input type="hidden" name="{@fieldname}" value="{value}" id="{@fieldname}">
				<xsl:apply-templates select="@*" />
			</input>

			<xsl:if test="(@ftype=&apos;datetime&apos;) or (@ftype=&apos;date&apos;)">
				<xsl:call-template name="ftype-datetime-date" />
			</xsl:if>

			<xsl:if test="@ftype=&apos;datetime&apos;">
				<span class="time_at">
					<xsl:value-of select="$time_at" />
				</span>
			</xsl:if>

			<xsl:if test="(@ftype=&apos;datetime&apos;) or (@ftype=&apos;time&apos;)">
				<xsl:call-template name="ftype-datetime-time" />
			</xsl:if>
		</div>
	</xsl:template>		

	<xsl:template name="ftype-datetime-date">
		<select
			name="internal_{@fieldname}_day"
			super="{@fieldname}">
			<option value="1">1</option>
			<option value="2">2</option>
			<option value="3">3</option>
			<option value="4">4</option>
			<option value="5">5</option>
			<option value="6">6</option>
			<option value="7">7</option>
			<option value="8">8</option>
			<option value="9">9</option>
			<option value="10">10</option>
			<option value="11">11</option>
			<option value="12">12</option>
			<option value="13">13</option>
			<option value="14">14</option>
			<option value="15">15</option>
			<option value="16">16</option>
			<option value="17">17</option>
			<option value="18">18</option>
			<option value="19">19</option>
			<option value="20">20</option>
			<option value="21">21</option>
			<option value="22">22</option>
			<option value="23">23</option>
			<option value="24">24</option>
			<option value="25">25</option>
			<option value="26">26</option>
			<option value="27">27</option>
			<option value="28">28</option>
			<option value="29">29</option>
			<option value="30">30</option>
			<option value="31">31</option>
		</select>
		<xsl:value-of select="$time_daymonth" />
		<xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
		<select
			name="internal_{@fieldname}_month"
			super="{@fieldname}">
			<xsl:call-template name="optionlist_months" />
		</select>
		<xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
		<input
			class="date"
			name="internal_{@fieldname}_year"
			super="{@fieldname}"
			type="text"
			value=""
			size="5"
			maxlength="4"/>
	</xsl:template>

	<xsl:template name="ftype-datetime-time">
		<select
			name="internal_{@fieldname}_hours"
			super="{@fieldname}">
			<option value="0">0</option>
			<option value="1">1</option>
			<option value="2">2</option>
			<option value="3">3</option>
			<option value="4">4</option>
			<option value="5">5</option>
			<option value="6">6</option>
			<option value="7">7</option>
			<option value="8">8</option>
			<option value="9">9</option>
			<option value="10">10</option>
			<option value="11">11</option>
			<option value="12">12</option>
			<option value="13">13</option>
			<option value="14">14</option>
			<option value="15">15</option>
			<option value="16">16</option>
			<option value="17">17</option>
			<option value="18">18</option>
			<option value="19">19</option>
			<option value="20">20</option>
			<option value="21">21</option>
			<option value="22">22</option>
			<option value="23">23</option>
		</select>
		<xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
		:
		<xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
		<select
			name="internal_{@fieldname}_minutes"
			super="{@fieldname}">
			<option value="0">00</option>
			<option value="1">01</option>
			<option value="2">02</option>
			<option value="3">03</option>
			<option value="4">04</option>
			<option value="5">05</option>
			<option value="6">06</option>
			<option value="7">07</option>
			<option value="8">08</option>
			<option value="9">09</option>
			<option value="10">10</option>
			<option value="11">11</option>
			<option value="12">12</option>
			<option value="13">13</option>
			<option value="14">14</option>
			<option value="15">15</option>
			<option value="16">16</option>
			<option value="17">17</option>
			<option value="18">18</option>
			<option value="19">19</option>
			<option value="20">20</option>
			<option value="21">21</option>
			<option value="22">22</option>
			<option value="23">23</option>
			<option value="24">24</option>
			<option value="25">25</option>
			<option value="26">26</option>
			<option value="27">27</option>
			<option value="28">28</option>
			<option value="29">29</option>
			<option value="30">30</option>
			<option value="31">31</option>
			<option value="32">32</option>
			<option value="33">33</option>
			<option value="34">34</option>
			<option value="35">35</option>
			<option value="36">36</option>
			<option value="37">37</option>
			<option value="38">38</option>
			<option value="39">39</option>
			<option value="40">40</option>
			<option value="41">41</option>
			<option value="42">42</option>
			<option value="43">43</option>
			<option value="44">44</option>
			<option value="45">45</option>
			<option value="46">46</option>
			<option value="47">47</option>
			<option value="48">48</option>
			<option value="49">49</option>
			<option value="50">50</option>
			<option value="51">51</option>
			<option value="52">52</option>
			<option value="53">53</option>
			<option value="54">54</option>
			<option value="55">55</option>
			<option value="56">56</option>
			<option value="57">57</option>
			<option value="58">58</option>
			<option value="59">59</option>
		</select>
	</xsl:template>

	<xsl:template name="ftype-image">
		<xsl:if test="@maywrite!=&apos;false&apos;">
			<xsl:choose>
				<xsl:when test="@dttype=&apos;binary&apos; and not(upload)">
					<div class="imageupload">
						<div>
							<input type="hidden" name="{@fieldname}" value="YES" />
							<a
								href="{$uploadpage}&amp;popupid={$popupid}&amp;did={@did}&amp;wizard={/wizard/@instance}&amp;maxsize={@dtmaxsize}"
								onclick="return doStartUpload(this);">
								<xsl:call-template name="prompt_image_upload" />
							</a>
							<br />
							<xsl:if test="not(starts-with(@number, &apos;n&apos;))">
								<img
									src="{node:function($cloud, string(@number), concat(&apos;servletpath(&apos;, $cloudkey, &apos;,cache(&apos;, $imagesize, &apos;))&apos;))}"
									hspace="0"
									vspace="0"
									border="0"
									title="{field[@name=&apos;description&apos;]}" />
								<br />
							</xsl:if>
						</div>
					</div>
				</xsl:when>
				<xsl:when test="@dttype=&apos;binary&apos; and upload">
					<div class="imageupload">
						<input type="hidden" name="{@fieldname}" value="YES" />
						<img src="{upload/path}" hspace="0" vspace="0" border="0" width="128" height="128" />
						<br />
						<span>
							<xsl:value-of select="upload/@name" />
							<xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
							(
							<xsl:value-of select="round((upload/@size) div 100) div 10" />
							K)
						</span>
						<br />
						<a
							href="{$uploadpage}&amp;popupid={$popupid}&amp;did={@did}&amp;wizard={/wizard/@instance}&amp;maxsize={@dtmaxsize}"
							onclick="return doStartUpload(this);">
							<xsl:call-template name="prompt_image_upload" />
						</a>
					</div>
				</xsl:when>
				<xsl:otherwise>
					<span>
						<img
							src="{node:function($cloud, string(@number), concat(&apos;servletpath(&apos;, $cloudkey, &apos;,cache(&apos;, $imagesize, &apos;))&apos;))}"
							hspace="0"
							vspace="0"
							border="0"
							title="{field[@name=&apos;description&apos;]}" />
						<br />
					</span>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
		<xsl:if test="@maywrite=&apos;false&apos;">
			<span class="readonly">
				<img
					src="{node:function($cloud, string(@number), concat(&apos;servletpath(&apos;, $cloudkey, &apos;,cache(&apos;, $imagesize, &apos;))&apos;))}"
					hspace="0"
					vspace="0"
					border="0" />
			</span>
		</xsl:if>
	</xsl:template>		

	<xsl:template name="ftype-file">
		<xsl:choose>
			<xsl:when test="@dttype=&apos;data&apos; or @maywrite=&apos;false&apos;">
				<a
					target="_blank"
					href="{node:function($cloud, string(@number), concat(&apos;servletpath(&apos;, $cloudkey, &apos;,number)&apos;))}">
					<xsl:call-template name="prompt_do_download" />
				</a>
			</xsl:when>
			<xsl:otherwise>
				<input type="hidden" name="{@fieldname}" value="YES" />
				<xsl:choose>
					<xsl:when test="not(upload)">
						<xsl:call-template name="prompt_no_file" />
						<br />
						<xsl:if test="not(starts-with(@number, &apos;n&apos;))">
							<a
								target="_blank"
								href="{node:function($cloud, string(@number), concat(&apos;servletpath(&apos;, $cloudkey, &apos;,number)&apos;))}">
								<xsl:call-template name="prompt_do_download" />
							</a>
							<br />
						</xsl:if>
					</xsl:when>
					<xsl:otherwise>
						<xsl:call-template name="prompt_uploaded" />
						<xsl:value-of select="upload/@name" />
						<xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
						(<xsl:value-of select="round((upload/@size) div 100) div 10" /> K)
						<br />
						<a target="_blank" href="file://{upload/path}">
							<xsl:call-template name="prompt_do_download" />
						</a>
					</xsl:otherwise>
				</xsl:choose>
				<a href="{$uploadpage}&amp;popupid={$popupid}&amp;did={@did}&amp;wizard={/wizard/@instance}&amp;maxsize={@dtmaxsize}"
					onclick="return doStartUpload(this);">
					<xsl:call-template name="prompt_do_upload" />
				</a>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>		

	<xsl:template name="ftype-realposition">
		<xsl:call-template name="realposition" />
	</xsl:template>

	<xsl:template name="ftype-other">
		<xsl:if test="@maywrite!=&apos;false&apos;">
			<xsl:apply-templates select="value" mode="inputline" />
		</xsl:if>
		<xsl:if test="@maywrite=&apos;false&apos;">
			<span class="readonly">
				<xsl:apply-templates select="value" mode="value" />
			</span>
		</xsl:if>
	</xsl:template>

  <!--
    Prints values with 'ftype=data'.
    truncate size
  -->
  <xsl:template match="value" mode="line">
    <span>
      <xsl:call-template name="writeCurrentField" />
    </span>
  </xsl:template>

  <xsl:template match="value" mode="inputline">
    <xsl:param name="val" select="." />
    <input
      type="text"
      size="80"
      name="{../@fieldname}"
      value="{$val}"
      class="input">
      <xsl:apply-templates select="../@*" />
    </input>
  </xsl:template>

  <xsl:template name="realposition">
    <span style="width:128">
			<input
				type="text"
				name="{@fieldname}"
				value="{value}"
				class="input">
				<xsl:apply-templates select="@*" />
			</input>
			<input
				type="button"
				value="get"
				onClick="document.forms[&apos;form&apos;].{@fieldname}.value = document.embeddedplayer.GetPosition();" />
    </span>
  </xsl:template>

  <!-- On default.  All attributes must be copied -->
  <xsl:template match="@*">
    <!-- THIS GOES WRONG IN RESIN -->
    <xsl:copy>
      <xsl:value-of select="." />
    </xsl:copy>
  </xsl:template>

  <!-- but not the name-attribute? -->
  <xsl:template match="@name" />

  <!--
    What to do with 'lists'.
  -->
  <xsl:template match="list">
  	<table class="listcontent">
  		<tr>
				<td class="listprompt">
					<xsl:call-template name="listprompt" />
				</td>
			</tr>
			<!-- List of items -->
			<tr class="itemcanvas">
				<td>
          <xsl:call-template name="listitems"/>
				</td>
			</tr>
  		<tr>
				<!-- SEARCH input and button -->
				<td class="listsearch">
					<xsl:call-template name="listsearch" />
				</td>
			</tr>
  		<tr>
				<!-- NEW button -->
				<td class="listnewbuttons">
					<xsl:call-template name="listnewbuttons" />
				</td>
			</tr>
		</table>

    <p>
      <xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
    </p>
  </xsl:template>

  <xsl:template name="listprompt">
		<span>
			<xsl:attribute name="title">
				<xsl:call-template name="i18n">
					<xsl:with-param name="nodes" select="description" />
				</xsl:call-template>
			</xsl:attribute>
			<xsl:if test="@status=&apos;invalid&apos;">
				<xsl:attribute name="class">notvalid</xsl:attribute>
			</xsl:if>
			<xsl:call-template name="i18n">
				<xsl:with-param name="nodes" select="title" />
			</xsl:call-template>
		</span>
  </xsl:template>

  <xsl:template name="listnewbuttons">
		<xsl:if test="command[@name=&apos;add-item&apos;]">
			<!-- only if less then maxoccurs -->
			<xsl:if test="not(@maxoccurs) or (@maxoccurs = &apos;*&apos;) or count(item) &lt; @maxoccurs">
				<!-- create action and startwizard command are present. Open the object into the start wizard -->
				<xsl:if test="command[@name=&apos;startwizard&apos;]">
					<xsl:for-each select="command[@name=&apos;startwizard&apos;]">

            <!-- The prompts.xsl adds this as a tooltip -->
						<!-- xsl:if test="prompt">
              <xsl:value-of select="prompt"/>
            </xsl:if -->
						
						<!-- Moved prompt to the "prompt_add_wizard" template as a tooltip -->
						<xsl:if test="@inline=&apos;true&apos;">
							<a
								href="javascript:doStartWizard(&apos;{../@fid}&apos;,&apos;{../command[@name=&apos;add-item&apos;]/@value}&apos;,&apos;{@wizardname}&apos;,&apos;{@objectnumber}&apos;,&apos;{@origin}&apos;);">
								<xsl:call-template name="prompt_add_wizard" />
							</a>
						</xsl:if>
						<xsl:if test="not(@inline=&apos;true&apos;)">
							<a
								href="{$popuppage}&amp;fid={../@fid}&amp;did={../command[@name=&apos;add-item&apos;]/@value}&amp;popupid={@wizardname}_{@objectnumber}&amp;wizard={@wizardname}&amp;objectnumber={@objectnumber}&amp;origin={@origin}"
								target="_blank">
								<xsl:call-template name="prompt_add_wizard" />
							</a>
						</xsl:if>
					</xsl:for-each>
				</xsl:if>

				<!-- create action is present, but no startwizard command. Add the object into the wizard -->
				<xsl:if test="not(command[@name=&apos;startwizard&apos;])">
					<xsl:for-each select="command[@name=&apos;add-item&apos;]">
						<a href="javascript:doSendCommand(&apos;{@cmd}&apos;);">
							<xsl:call-template name="prompt_new" />
						</a>
					</xsl:for-each>
				</xsl:if>
			</xsl:if>
		</xsl:if>
  </xsl:template>

  <xsl:template name="listsearch">
		<!-- if 'add-item' command and a search, then make a search util-box -->
		<xsl:if test="command[@name=&apos;add-item&apos;]">
			<xsl:for-each select="command[@name=&apos;search&apos;]">
				<table class="searchcontent">
					<tr>
						<td>
              <xsl:call-template name="listsearch-age" />
						</td>
						<td>
              <xsl:call-template name="listsearch-fields" />
						</td>
						<td>
							<input
								type="text"
								name="searchterm_{../command[@name=&apos;add-item&apos;]/@cmd}"
								value="{search-filter[1]/default}"
								class="search"
								onChange="var s = form[&apos;searchfields_{../command[@name=&apos;add-item&apos;]/@cmd}&apos;]; s[s.selectedIndex].setAttribute(&apos;default&apos;, this.value); " />
							<!-- on change the current value is copied back to the option's default, because of that, the user's search is stored between different types of search-actions -->
						</td>
						<td>
							<span
								title="{$tooltip_search}"
								onClick="doSearch(this,&apos;{../command[@name=&apos;add-item&apos;]/@cmd}&apos;,&apos;{$sessionkey}&apos;);">
								<xsl:for-each select="@*">
									<xsl:copy />
								</xsl:for-each>
								<xsl:call-template name="prompt_search" />
							</span>
						</td>
					</tr>
				</table>
			</xsl:for-each>
		</xsl:if>
  </xsl:template>

  <xsl:template name="listsearch-age">
		<xsl:choose>
			<xsl:when test="$searchagetype=&apos;none&apos;"></xsl:when>
		<!-- alway make searching on age possible -->
			<xsl:when test="$searchagetype=&apos;edit&apos;">
				<xsl:call-template name="prompt_age" />
				<input
					type="text"
					name="searchage_{../command[@name=&apos;add-item&apos;]/@cmd}"
					class="age"
					value="{@age}" />
			</xsl:when>
			<xsl:otherwise>
				<select
					name="searchage_{../command[@name=&apos;add-item&apos;]/@cmd}"
					class="age">
					<xsl:call-template name="searchageoptions">
		        <xsl:with-param name="selectedage" select="@age" />
		      </xsl:call-template>
				</select>
			</xsl:otherwise>
		</xsl:choose>
  </xsl:template>

  <xsl:template name="listsearch-fields">
		<!-- other search-possibilities are given in the xml -->
		<select
			name="searchfields_{../command[@name=&apos;add-item&apos;]/@cmd}"
			class="searchpossibilities"
			onChange="form[&apos;searchterm_{../command[@name=&apos;add-item&apos;]/@cmd}&apos;].value = this[this.selectedIndex].getAttribute(&apos;default&apos;); form[&apos;searchtype_{../command[@name=&apos;add-item&apos;]/@cmd}&apos;].value = this[this.selectedIndex].getAttribute(&apos;searchtype&apos;);">
			<xsl:choose>
				<xsl:when test="search-filter">
					<xsl:for-each select="search-filter">
						<option
							value="{search-fields}"
							default="{default}"
							searchtype="{search-fields/@search-type}">
							<xsl:call-template name="i18n">
								<xsl:with-param name="nodes" select="name" />
							</xsl:call-template>
						</option>
					</xsl:for-each>
				</xsl:when>
				<!-- if nothing, then search on title -->
				<xsl:otherwise>
					<option value="title">
						<xsl:call-template name="prompt_search_title" />
					</option>
				</xsl:otherwise>
			</xsl:choose>
      <xsl:call-template name="listsearch-fields-default" />
		</select>
		<input
			type="hidden"
			name="searchtype_{../command[@name=&apos;add-item&apos;]/@cmd}"
			value="{search-filter[1]/search-fields/@searchtype}" />
  </xsl:template>

  <xsl:template name="listsearch-fields-default">
		<!-- always search on owner and number too -->
		<option value="number" searchtype="equals" ><xsl:call-template name="prompt_search_number" /></option>
		<option value="owner" searchtype="like"><xsl:call-template name="prompt_search_owner" /></option>
  </xsl:template>

  <xsl:template name="listitems">
		<xsl:if test="item">
			<table class="itemcontent">
				<xsl:apply-templates select="item" />
			</table>
		</xsl:if>
  </xsl:template>

  <xsl:template match="item">
  	<xsl:call-template name="itemprefix"/>
      <!-- here we figure out how to draw this repeated item. It depends on the displaytype -->
    <xsl:choose>
      <xsl:when test="@displaytype='link'">
				<xsl:call-template name="item-link"/>
      </xsl:when>
      <xsl:when test="@displaytype='image'">
				<xsl:call-template name="item-image"/>
      </xsl:when>
      <xsl:when test="count(field|fieldset) &lt; 2">
				<xsl:call-template name="item-onefield"/>
      </xsl:when>
      <xsl:when test="field[@ftype=&apos;startwizard&apos;] and count(field|fieldset) &lt; 3">
				<xsl:call-template name="item-onefield"/>
      </xsl:when>
      <xsl:otherwise>
				<xsl:call-template name="item-other"/>
      </xsl:otherwise>
    </xsl:choose>
      
		<xsl:for-each select="list">
			<tr class="listcanvas">
				<td colspan="3">
					<xsl:apply-templates select="." />
				</td>
			</tr>
		</xsl:for-each>
		<xsl:call-template name="itempostfix"/>
  </xsl:template>

  <xsl:template name="itemprefix"/>

  <xsl:template name="item-link">
		<!-- simply make the link, there must be a field name and number -->
		<tr>
			<td colspan="3">
				<a href="{$wizardpage}&amp;wizard={@wizardname}&amp;objectnumber={field[@name='number']/value}">- <xsl:value-of select="field[@name='title']/value"/>
				</a>
			</td>
		</tr>
  </xsl:template>

  <xsl:template name="item-image">
	  <!-- first column is the image, show the fields in the second column -->
		<tr>
			<td colspan="3">
				<xsl:call-template name="itembuttons"/>
			</td>
		</tr>
		<tr style="vartical-align: top;">
			<td style="vartical-align: top; width: 1%;">
		    <!-- the image -->
				<img src="{node:function($cloud, string(field/@number), concat('servletpath(', $cloudkey, ',cache(', $imagesize, '))'))}" hspace="0" vspace="0" border="0" title="{field[@name='description']}"/>
			</td>
			<td colspan="2">
				<xsl:call-template name="itemfields"/>
			</td>
		</tr>
  </xsl:template>

  <xsl:template name="item-onefield">
		<tr>
			<td colspan="2" class="itemfields">
				<xsl:call-template name="itemfields"/>
			</td>
			<td class="itembuttons">
				<xsl:call-template name="itembuttons" />
			</td>
		</tr>
  </xsl:template>
  
  <xsl:template name="item-other">
		<!-- more fields -->
		<tr>
			<td colspan="3">
				<xsl:call-template name="itembuttons"/>
			</td>
		</tr>
		<tr>
			<td colspan="3">
				<xsl:call-template name="itemfields"/>
			</td>
		</tr>
  </xsl:template>

  <xsl:template name="itemfields">
		<table class="fieldscontent">
      <xsl:call-template name="itemfields-hover" />

			<xsl:for-each select="fieldset|field">
				<!-- Don't show the startwizrd field here. -->
				<xsl:if test="not(@ftype=&apos;startwizard&apos;)">
					<tr class="fieldcanvas">
            <xsl:call-template name="itemfields-click" />
						<xsl:apply-templates select="." />
					</tr>
				</xsl:if>
			</xsl:for-each>
		</table>
  </xsl:template>

  <xsl:template name="itemfields-hover">
			<xsl:if
				test="field[@ftype=&apos;startwizard&apos;] or fieldset/field[@ftype=&apos;startwizard&apos;]">
				<xsl:attribute name="class">itemrow</xsl:attribute>
				<xsl:attribute name="onMouseOver">objMouseOver(this);</xsl:attribute>
				<xsl:attribute name="onMouseOut">objMouseOut(this);</xsl:attribute>

				<xsl:for-each select="fieldset/field[@ftype=&apos;startwizard&apos;]">
					<xsl:if test="not(@inline) or @inline=&apos;true&apos;">
						<xsl:attribute name="href">
							<xsl:text disable-output-escaping="yes">javascript:doStartWizard('</xsl:text>
							<xsl:value-of select="../../../@fid" />
							<xsl:text disable-output-escaping="yes">','</xsl:text>
							<xsl:value-of select="../../../command[@name=&apos;add-item&apos;]/@value" />
							<xsl:text disable-output-escaping="yes">','</xsl:text>
							<xsl:value-of select="@wizardname" />
							<xsl:text disable-output-escaping="yes">','</xsl:text>
							<xsl:value-of select="@objectnumber" />
							<xsl:text disable-output-escaping="yes">','</xsl:text>
							<xsl:value-of select="@origin" />
							<xsl:text disable-output-escaping="yes">');</xsl:text>
						</xsl:attribute>
					</xsl:if>
					<xsl:if test="@inline=&apos;false&apos;">
						<xsl:attribute name="href"><xsl:value-of select="$popuppage" />&amp;fid=<xsl:value-of select="../../../@fid" />&amp;did=<xsl:value-of select="../../../command[@name=&apos;add-item&apos;]/@value" />&amp;popupid=<xsl:value-of select="@wizardname" />_<xsl:value-of select="@objectnumber" />&amp;wizard=<xsl:value-of select="@wizardname" />&amp;objectnumber=<xsl:value-of select="@objectnumber" />&amp;origin=<xsl:value-of select="@origin" /></xsl:attribute>
						<xsl:attribute name="target">_blank</xsl:attribute>
					</xsl:if>
				</xsl:for-each>

				<xsl:for-each select="field[@ftype=&apos;startwizard&apos;]">
					<xsl:if test="not(@inline) or @inline=&apos;true&apos;">
						<xsl:attribute name="href">
							<xsl:text disable-output-escaping="yes">javascript:doStartWizard('</xsl:text>
							<xsl:value-of select="../../@fid" />
							<xsl:text disable-output-escaping="yes">','</xsl:text>
							<xsl:value-of select="../../command[@name=&apos;add-item&apos;]/@value" />
							<xsl:text disable-output-escaping="yes">','</xsl:text>
							<xsl:value-of select="@wizardname" />
							<xsl:text disable-output-escaping="yes">','</xsl:text>
							<xsl:value-of select="@objectnumber" />
							<xsl:text disable-output-escaping="yes">','</xsl:text>
							<xsl:value-of select="@origin" />
							<xsl:text disable-output-escaping="yes">');</xsl:text>
						</xsl:attribute>
					</xsl:if>
					<xsl:if test="@inline=&apos;false&apos;">
						<xsl:attribute name="href"><xsl:value-of select="$popuppage" />&amp;fid=<xsl:value-of select="../../@fid" />&amp;did=<xsl:value-of select="../../command[@name=&apos;add-item&apos;]/@value" />&amp;popupid=<xsl:value-of select="@wizardname" />_<xsl:value-of select="@objectnumber" />&amp;wizard=<xsl:value-of select="@wizardname" />&amp;objectnumber=<xsl:value-of select="@objectnumber" />&amp;origin=<xsl:value-of select="@origin" /></xsl:attribute>
						<xsl:attribute name="target">_blank</xsl:attribute>
					</xsl:if>
				</xsl:for-each>
			</xsl:if>
  </xsl:template>

  <xsl:template name="itemfields-click">
		<!-- This little choose code makes it possible to click on the 
			fieldcanvas to edit the list item. The fieldcanvas shouldn't
			be clickable when the field is editable.
		-->
		<xsl:choose>
			<xsl:when
				test="(../field[@ftype=&apos;startwizard&apos;] or ../../field[@ftype=&apos;startwizard&apos;]) and @ftype=&apos;data&apos;">
				<xsl:attribute name="onMouseDown">objClick(this.offsetParent);</xsl:attribute>
			</xsl:when>
			<xsl:otherwise>
				<xsl:attribute name="class">fieldcanvas-edit</xsl:attribute>
			</xsl:otherwise>
		</xsl:choose>
  </xsl:template>

  <xsl:template name="itempostfix" />

  <xsl:template name="itembuttons">
    <table>
      <tr>
        <xsl:call-template name="mediaitembuttons" />
        <xsl:call-template name="selectitembuttons" />
			  <xsl:call-template name="deleteitembuttons" />
			  <xsl:call-template name="positembuttons" />
      </tr>
    </table>
  </xsl:template>

  <!-- Media-items must be overridable, because there is no good generic sollution forewards compatible yet -->
  <xsl:template name="mediaitembuttons">
		<td>
			<xsl:if test="@displaytype=&apos;audio&apos;">
				<span	class="imgbutton"	title="{$tooltip_audio}">
					<a href="{$ew_context}/rastreams.db?{field/@number}">
						<xsl:call-template name="prompt_audio" />
					</a>
				</span>
			</xsl:if>
			<xsl:if test="@displaytype=&apos;video&apos;">
				<span	class="imgbutton"	title="{$tooltip_video}">
					<a href="{$ew_context}/rmstreams.db?{field/@number}">
						<xsl:call-template name="prompt_video" />
					</a>
				</span>
			</xsl:if>
		</td>
  </xsl:template>

  <xsl:template name="selectitembuttons">
		<!-- The item row is the trigger for startwizard at the moment. -->
		<td> 	
			<xsl:for-each select="fieldset/field[@ftype='startwizard']">
				<xsl:if test="@inline='true'">
					<span	class="imgbutton">
						<a href="javascript:doStartWizard('{../../../@fid}','{../../../command[@name='add-item']/@value}','{@wizardname}','{@objectnumber}','{@origin}');">
							<xsl:call-template name="prompt_edit_wizard" />
						</a>
					</span>
				</xsl:if>
				<xsl:if test="not(@inline='true')">
					<span	class="imgbutton">
						<a href="{$popuppage}&amp;fid={../../../@fid}&amp;did={../../../command[@name='add-item']/@value}&amp;popupid={@wizardname}_{@objectnumber}&amp;wizard={@wizardname}&amp;objectnumber={@objectnumber}&amp;origin={@origin}"
							target="_blank">
							<xsl:call-template name="prompt_edit_wizard" />
						</a>
					</span>
				</xsl:if>
			</xsl:for-each>
			<xsl:for-each select="field[@ftype='startwizard']">
				<xsl:if test="@inline='true'">
					<span	class="imgbutton">
						<a href="javascript:doStartWizard('{../../@fid}','{../../command[@name='add-item']/@value}','{@wizardname}','{@objectnumber}','{@origin}');">
							<xsl:call-template name="prompt_edit_wizard" />
						</a>
					</span>
				</xsl:if>
				<xsl:if test="not(@inline='true')">
					<span	class="imgbutton">
						<a href="{$popuppage}&amp;fid={../../@fid}&amp;did={../../command[@name='add-item']/@value}&amp;popupid={@wizardname}_{@objectnumber}&amp;wizard={@wizardname}&amp;objectnumber={@objectnumber}&amp;origin={@origin}"
							target="_blank">
							<xsl:call-template name="prompt_edit_wizard" />
						</a>
					</span>
				</xsl:if>
			</xsl:for-each>
		</td>  
		<!-- -->
  </xsl:template>
  
  <xsl:template name="deleteitembuttons">
		<td>
			<xsl:if test="command[@name=&apos;delete-item&apos;]">
				<xsl:if test="@maydelete!=&apos;false&apos;">
					<span
						class="imgbutton"
						title="{$tooltip_remove}"
						onclick="doSendCommand(&apos;{command[@name=&apos;delete-item&apos;]/@cmd}&apos;);">
						<xsl:call-template name="prompt_remove" />
					</span>
				</xsl:if>
			</xsl:if>
		</td>
  </xsl:template>
  
  <xsl:template name="positembuttons">
		<td>
			<xsl:choose>
				<xsl:when test="@maywrite=&apos;true&apos; and command[@name=&apos;move-up&apos;]">
					<span
						class="imgbutton"
						title="{$tooltip_up}"
						onclick="doSendCommand(&apos;{command[@name=&apos;move-up&apos;]/@cmd}&apos;);">
						<xsl:call-template name="prompt_up" />
					</span>
				</xsl:when>
				<xsl:otherwise>
					<img src="{$mediadir}nix.gif" border="0" width="20" />
				</xsl:otherwise>
			</xsl:choose>
		</td>
		<td>
			<xsl:choose>
				<xsl:when test="@maywrite=&apos;true&apos; and command[@name=&apos;move-down&apos;]">
					<span
						class="imgbutton"
						title="{$tooltip_down}"
						onclick="doSendCommand(&apos;{command[@name=&apos;move-down&apos;]/@cmd}&apos;);">
						<xsl:call-template name="prompt_down" />
					</span>
				</xsl:when>
				<xsl:otherwise>
					<img src="{$mediadir}nix.gif" border="0" width="20" />
				</xsl:otherwise>
			</xsl:choose>
		</td>
  </xsl:template>

</xsl:stylesheet>