package de.uni_passau.fim.auermich.android_analysis.component;

import de.uni_passau.fim.auermich.android_analysis.utility.ClassUtils;
import org.jf.dexlib2.iface.ClassDef;

import java.util.*;

public abstract class Component {

    protected final ClassDef clazz;
    protected final String name;

    // components are enabled by default but not exported by default
    protected boolean exported = false;
    protected boolean enabled = true;

    protected final Set<String> globalStrings;
    protected final List<IntentFilter> intentFilters;

    private final Set<String> staticStrings = new HashSet<>();

    public Component(ClassDef clazz) {
        this.clazz = clazz;
        this.name = ClassUtils.dottedClassName(clazz.toString());
        globalStrings = new LinkedHashSet<>();
        intentFilters = new ArrayList<>();
    }

    /**
     * Necessary for parsing components from the manifest.
     *
     * @param className The class name of the component.
     */
    public Component(String className) {
        this.clazz = null;
        this.name = className;
        globalStrings = new LinkedHashSet<>();
        intentFilters = new ArrayList<>();
    }

    public void addIntentFilter(IntentFilter intentFilter) {
        intentFilters.add(intentFilter);
    }

    public void addStringConstant(String constant) {
        assert constant != null;
        globalStrings.add(constant);
    }

    public boolean isExported() {
        return exported;
    }

    public void setExported(boolean exported) {
        this.exported = exported;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the class definition representing the component.
     *
     * @return Returns the class definition.
     */
    public ClassDef getClazz() {
        return clazz;
    }

    /**
     * Returns the transformed (dotted) class name.
     *
     * @return Returns the name of the component.
     */
    public String getName() {
        return name;
    }

    // a simplified xml representation of a component solely containing the name and the attributes exported and enabled
    public String toXmlSimple() {
        return "<" + getType()
                + " name=\"" + makeXmlConform(name) + "\""
                + " enabled=\"" + enabled +"\""
                + " exported=\"" + exported +"\""
                + "></" + getType() + ">";
    }

    /**
     * Provides the generic part of a component as XML representation.
     *
     * @return Returns the component's partial XML representation.
     */
    public String toXml() {
        StringBuilder output = new StringBuilder();
        output.append(intentFiltersToXml());
        output.append(globalStringsToXml());
        return output.toString();
    }

    /**
     * Gets the type of a component. (Is needed for type in xml file)
     *
     * @return The type of a component.
     */
    protected abstract String getType();

    /**
     * Provides the XML representation of the collected global strings.
     *
     * @return Returns the XML representation of the global strings.
     */
    private String globalStringsToXml() {
        if (!globalStrings.isEmpty()) {
            StringBuilder output = new StringBuilder();
            output.append("    <global>\n");
            for (String string : globalStrings) {
                output.append("        <string value='" + makeXmlConform(string)
                        + "'/>\n");
            }
            output.append("    </global>\n");
            return output.toString();
        }
        return "";
    }

    /**
     * Converts the collected static strings of a component and converts them to a valid XML representation.
     *
     * @return Returns a XML representation of the collected static strings per component.
     */
    public String staticStringsToXml() {
        if (!staticStrings.isEmpty()) {
            StringBuilder output = new StringBuilder();
            output.append("<strings class='")
                    .append(name)
                    .append("' type='")
                    .append(getType())
                    .append("'>\n");
            for (String string : staticStrings) {
                output.append("    <string value='")
                        .append(makeXmlConform(string)).append("'/>\n");
            }
            output.append("</strings>\n");
            return output.toString();
        } else {
            return "";
        }
    }

    /**
     * Provides the XML representation of the attached intent-filters.
     *
     * @return Returns the XML representation of the intent-filters.
     */
    private String intentFiltersToXml() {

        StringBuilder output = new StringBuilder();

        for (IntentFilter intentFilter : intentFilters) {
            output.append(intentFilter.toXml());
        }

        return output.toString();
    }

    public void finalizeMethods() {

    }

    protected String makeXmlConform(String input) {
        input = input.replace("&", "&amp;");
        input = input.replace("\"", "&quot;");
        input = input.replace("'", "&apos;");
        input = input.replace(">", "&gt;");
        input = input.replace("<", "&lt;");
        return input;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Component component = (Component) o;
        return name.equals(component.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public class IntentFilter {

        private final Set<String> actions = new HashSet<>();
        private final Set<String> categories = new HashSet<>();

        public void addAction(String action) {
            actions.add(action);
        }

        public void addCategory(String category) {
            categories.add(category);
        }

        public boolean hasAction() {
            return !actions.isEmpty();
        }

        /**
         * Converts an intent-filter to a custom xml representation.
         *
         * @return Returns the xml representation of the intent-filter.
         */
        public String toXml() {

            StringBuilder output = new StringBuilder();
            output.append("    <intent-filter>\n");

            for (String action : actions) {
                output.append("        <action name='" + makeXmlConform(action)
                        + "'/>\n");
            }

            for (String category : categories) {
                output.append(
                        "        <category name='" + makeXmlConform(category)
                                + "'/>\n");
            }

            output.append("    </intent-filter>\n");
            return output.toString();
        }
    }

    public Set<String> getGlobalStrings() {
        return globalStrings;
    }

    /**
     * Adds a set of string to the {@code allStrings} Set.
     *
     * @param strings The set to be added.
     */
    public void addStaticStrings(Set<String> strings) {
        staticStrings.addAll(strings);
    }
}
