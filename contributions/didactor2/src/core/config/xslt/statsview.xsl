<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<!-- stats -->
<xsl:template match="stats">
<html>
<head><title>Stats</title></head>
<body bgcolor="#FFFFFF">
<table border="2">
<tr>
  <td colspan="2"><font size="+1"><b>Stats</b></font></td>
</tr>
<xsl:apply-templates/>
</table>
</body>
</html>
</xsl:template>

<!-- AllowCache -->
<xsl:template match="AllowCache">
<tr>
  <td valign="top"><b>Allow cache:</b></td>
  <td>
<xsl:value-of select="."/>
  </td>
</tr>
</xsl:template>

</xsl:stylesheet>

