package de.uni_passau.fim.component;

import de.uni_passau.fim.component.bundle.Extra;

import java.util.*;

public class Activity extends Component {

    private List<Extra> onNewIntentExtras;
    private Set<String> onNewIntentStrings;

    private List<Extra> onCreateExtras;
    private Set<String> onCreateStrings;

    public Activity(String name) {
        super(name);
        onNewIntentExtras = new ArrayList<>();
        onNewIntentStrings = new LinkedHashSet<>();
        onCreateExtras = new ArrayList<>();
        onCreateStrings = new LinkedHashSet<>();
    }

    public List<Extra> getOnNewIntentExtras() {
        return onNewIntentExtras;
    }

    public Set<String> getOnNewIntentStrings() {
        return onNewIntentStrings;
    }

    public List<Extra> getOnCreateExtras() {
        return onCreateExtras;
    }

    public Set<String> getOnCreateStrings() {
        return onCreateStrings;
    }

    public String toXml() {
        StringBuilder output = new StringBuilder("<activity name=\"" + makeXmlConform(name) + "\">\n");
        output.append(super.toXml());
        output.append(onCreateToXml());
        output.append(onNewIntentToXml());
        output.append("</activity>\n");

        return output.toString();
    }

    private String onNewIntentToXml() {
        finalizeOnNewIntent();
        if(!onNewIntentExtras.isEmpty() || !onNewIntentStrings.isEmpty()) {
            StringBuilder output = new StringBuilder();
            output.append("    <on_new_intent>\n");
            for(String string : onNewIntentStrings) {
                output.append("        <string value=\"" + makeXmlConform(string) + "\"/>\n");
            }
            for(int i = 0; i < onNewIntentExtras.size(); i++) {
                output.append("        <extra key=\"" + makeXmlConform(onNewIntentExtras.get(i).getKey()) + "\" type=\"" + makeXmlConform(onNewIntentExtras.get(i).getValueType()) + "\"/>\n");
            }
            output.append("    </on_new_intent>\n");
            return output.toString();
        }
        return "";
    }

    private String onCreateToXml() {
        finalizeOnCreate();
        if(!onCreateExtras.isEmpty() || !onCreateStrings.isEmpty()) {
            StringBuilder output = new StringBuilder();
            output.append("    <on_create>\n");
            for(String string : onCreateStrings) {
                output.append("        <string value=\"" + makeXmlConform(string) + "\"/>\n");
            }
            for(int i = 0; i < onCreateExtras.size(); i++) {
                output.append("        <extra key=\"" + makeXmlConform(onCreateExtras.get(i).getKey()) + "\" type=\"" + makeXmlConform(onCreateExtras.get(i).getValueType()) + "\"/>\n");
            }
            output.append("    </on_create>\n");
            return output.toString();
        }
        return "";
    }

    public void finalizeMethods() {
        finalizeOnCreate();
        finalizeOnNewIntent();
    }

    private void finalizeOnNewIntent() {
        for(int i = 0 ; i < onNewIntentExtras.size(); i++) {
            for(int n = i + 1; n < onNewIntentExtras.size(); n++) {
                if(onNewIntentExtras.get(i).getKey().equals(onNewIntentExtras.get(n).getKey())) {
                    if(onNewIntentExtras.get(i).getValueType().equals(""))
                        onNewIntentExtras.get(i).setValueType(onNewIntentExtras.get(n).getValueType());
                    onNewIntentExtras.remove(n);
                    n--;
                }
            }

            Iterator nisIt = onNewIntentStrings.iterator();
            while(nisIt.hasNext()) {
                String string = (String)nisIt.next();
                if(onNewIntentExtras.get(i).getKey().equals(string))
                    nisIt.remove();
            }
            Iterator it = globalStrings.iterator();
            while (it.hasNext()) {
                String string = (String)it.next();
                if(onNewIntentExtras.get(i).getKey().equals(string))
                    it.remove();
            }
        }
    }

    private void finalizeOnCreate() {
        for(int i = 0 ; i < onCreateExtras.size(); i++) {
            for(int n = i + 1; n < onCreateExtras.size(); n++) {
                if(onCreateExtras.get(i).getKey().equals(onCreateExtras.get(n).getKey())) {
                    if(onCreateExtras.get(i).getValueType().equals(""))
                        onCreateExtras.get(i).setValueType(onCreateExtras.get(n).getValueType());
                    onCreateExtras.remove(n);
                    n--;
                }
            }

            Iterator ocIt = onCreateStrings.iterator();
            while(ocIt.hasNext()) {
                String string = (String)ocIt.next();
                if(onCreateExtras.get(i).getKey().equals(string))
                    ocIt.remove();
            }
            Iterator it = globalStrings.iterator();
            while (it.hasNext()) {
                String string = (String)it.next();
                if(onCreateExtras.get(i).getKey().equals(string))
                    it.remove();
            }
        }
    }
}
