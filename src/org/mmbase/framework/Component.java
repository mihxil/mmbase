/*
This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.framework;
import java.util.*;
import java.net.URI;
import org.mmbase.util.LocalizedString;
import org.mmbase.security.Action;

/**
 * A component is a piece of pluggable functionality that typically has dependencies on other
 * components, and may be requested several views.
 *
 * @author Michiel Meeuwissen
 * @version $Id: Component.java,v 1.16 2008-01-21 17:29:10 michiel Exp $
 * @since MMBase-1.9
 */
public interface Component {

    /**
     * Every component has a (universally) unique name
     */
    String getName();


    /**
     * A component has a version number.
     */
    int getVersion();

    /**
     * All (satisfied) depedencies of this Component. See als {@link
     * #getUnsatisfiedDependencies}.
     *
     */
    Collection<Component> getDependencies();

    /**
     * The unsatisfied dependencies, so this should return an empty collection. Unless the framework
     * is still initialing, because initially dependencies can be temporary added as 'unsatisfied'
     * because perhaps this other component is simply <em>not yet</em> loaded. The
     * ComponentRepository will call {@link #resolve(VirtualComponent, Component)} when a
     * dependency is satisfied after all.
     */
    Collection<VirtualComponent> getUnsatisfiedDependencies();

    /**
     * Used during bootstrapping. See also {@link #getUnsatisfiedDependencies()}
     */
    void resolve(VirtualComponent unsatified, Component satisfied);

    /**
     * The description can contain further information about the component, mainly to be displayed
     * in pages about components generally.
     */
    LocalizedString getDescription();

    /**
     * An URI which may identify the configuration of this Component.
     */
    URI getUri();

    /**
     * Configures the component, by XML.
     * @param element A 'component' element from the 'components' XSD.
     */
    void configure(org.w3c.dom.Element element);

    /**
     * An unmodifiable collection of all blocks associated with the component
     */
    Collection<Block> getBlocks();

    /**
     * Gets a specific block. If there is no such block, then <code>null</code> is returned.
     * @param name The name of the block. If this parameter is <code>null</code>, then {@link #getDefaultBlock} can
     * be returned.
     */
    Block getBlock(String name);

    /**
     * Gets the one block that is the 'default' block of this component
     */
    Block getDefaultBlock();


    /**
     * The baseName of the resource bundle associated with i18n messages for this component.
     * See {@link java.util.ResourceBundle#getBundle(String, Locale)}.
     */
    String getBundle();

    /**
     * An unmodifiable collection of all settings associated with this component
     */
    Collection<Setting<?>> getSettings();

    /**
     * Retrieves a setting (a definition, not a value, for that, use
     * {@link Framework#getSettingValue(Setting, Parameters)}) with a certain name. Or
     * <code>null</code> if no such setting in this component.
     */
    Setting<?> getSetting(String name);

    Map<String, Action> getActions();

}
