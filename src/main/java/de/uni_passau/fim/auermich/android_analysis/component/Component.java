package de.uni_passau.fim.auermich.android_analysis.component;

import de.uni_passau.fim.auermich.android_analysis.utility.ClassUtils;
import org.jf.dexlib2.iface.ClassDef;

import java.util.*;

public abstract class Component {

    protected final ClassDef clazz;
    protected final String name;

    protected final Set<String> globalStrings;
    protected final List<IntentFilter> intentFilters;

    private final Set<String> allStrings = new HashSet<>();

    public Component(ClassDef clazz) {
        this.clazz = clazz;
        this.name = ClassUtils.dottedClassName(clazz.toString());
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
     * Takes all static strings a component has and introduced them into a xml
     * valid structure.
     *
     * @return The xml structure of all static strings. The file contains the
     * type and the class of the component.
     */
    public String allStringsToXml() {
        if (!allStrings.isEmpty()) {
            StringBuilder output = new StringBuilder();
            output.append("<allstrings class='")
                    .append(name)
                    .append("' type='")
                    .append(getType())
                    .append("'>\n");
            for (String string : allStrings) {
                output.append("    <string value='")
                        .append(makeXmlConform(string)).append("'/>\n");
            }
            output.append("</allstrings>\n");
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


    public class IntentFilter {

        private final Set<String> actions = new HashSet<>();
        private final Set<String> categories = new HashSet<>();

        public void addAction(String action) {
            actions.add(action);
        }

        public void addCategory(String category) {
            categories.add(category);
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
    public void addAll(Set<String> strings) {
        allStrings.addAll(strings);
    }
}
