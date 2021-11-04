package de.uni_passau.fim.auermich.android_analysis.component;

import java.util.*;

public abstract class Component {

    protected String name;
    protected Set<String> globalStrings;
    protected List<IntentFilter> intentFilters;

    public Component(String name) {
        this.name = name;
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
     * Provides the XML representation of the collected global strings.
     *
     * @return Returns the XML representation of the global strings.
     */
    private String globalStringsToXml() {
        if (!globalStrings.isEmpty()) {
            StringBuilder output = new StringBuilder();
            output.append("    <global>\n");
            for (String string : globalStrings) {
                output.append("        <string value='" + makeXmlConform(string) + "'/>\n");
            }
            output.append("    </global>\n");
            return output.toString();
        }
        return "";
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

        private Set<String> actions = new HashSet<>();
        private Set<String> categories = new HashSet<>();

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
                output.append("        <action name='" + makeXmlConform(action) + "'/>\n");
            }

            for (String category : categories) {
                output.append("        <category name='" + makeXmlConform(category) + "'/>\n");
            }

            output.append("    </intent-filter>\n");
            return output.toString();
        }
    }
}