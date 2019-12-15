package de.uni_passau.fim.component;

import java.util.*;

public abstract class Component {

    protected String name;
    protected Set<String> globalStrings;

    public Component(String name) {
        this.name = name;
        globalStrings = new LinkedHashSet<>();
    }

    public void addStringConstant(String constant) {
        assert constant != null;
        globalStrings.add(constant);
    }

    public String getName() {
        return name;
    }

    public String toXml() {
        StringBuilder output = new StringBuilder("<component name='" + makeXmlConform(name) + "'>\n");
        output.append(globalToXml());
        output.append("</component>\n");

        return output.toString();
    }

    protected String globalToXml() {
        if(!globalStrings.isEmpty()) {
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
}
