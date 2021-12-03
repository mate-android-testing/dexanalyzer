package de.uni_passau.fim.auermich.android_analysis.component;

import de.uni_passau.fim.auermich.android_analysis.component.bundle.Extra;
import org.jf.dexlib2.iface.ClassDef;

import java.util.*;

public class Service extends Component{

    private final List<Extra> onStartCommandExtras;
    private final Set<String> onStartCommandStrings;

    private final List<Extra> onHandleIntentExtras;
    private final Set<String> onHandleIntentStrings;

    public Service(ClassDef clazz) {
        super(clazz);
        onStartCommandExtras = new ArrayList<>();
        onStartCommandStrings = new LinkedHashSet<>();
        onHandleIntentExtras = new ArrayList<>();
        onHandleIntentStrings = new LinkedHashSet<>();
    }

    public List<Extra> getOnStartCommandExtras() {
        return onStartCommandExtras;
    }

    public Set<String> getOnStartCommandStrings() {
        return onStartCommandStrings;
    }

    public List<Extra> getOnHandleIntentExtras() {
        return onHandleIntentExtras;
    }

    public Set<String> getOnHandleIntentStrings() {
        return onHandleIntentStrings;
    }

    public String toXml() {
        StringBuilder output = new StringBuilder("<service name=\"" + makeXmlConform(name) + "\">\n");
        output.append(super.toXml());
        output.append(onStartCommandToXml());
        output.append(onHandleIntentToXml());
        output.append("</service>\n");

        return output.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getType() {
        return "service";
    }

    private String onStartCommandToXml() {
        finalizeOnStartCommand();
        if(!onStartCommandExtras.isEmpty() || !onStartCommandStrings.isEmpty()) {
            StringBuilder output = new StringBuilder();
            output.append("    <on_start_command>\n");
            for(String string : onStartCommandStrings) {
                output.append("        <string value=\"" + makeXmlConform(string) + "\"/>\n");
            }
            for(int i = 0; i < onStartCommandExtras.size(); i++) {
                output.append("        <extra key=\"" + makeXmlConform(onStartCommandExtras.get(i).getKey())
                        + "\" type=\"" + makeXmlConform(onStartCommandExtras.get(i).getValueType()) + "\"/>\n");
            }
            output.append("    </on_start_command>\n");
            return output.toString();
        }
        return "";
    }

    private String onHandleIntentToXml() {
        finalizeOnStartCommand();
        if(!onHandleIntentExtras.isEmpty() || !onHandleIntentStrings.isEmpty()) {
            StringBuilder output = new StringBuilder();
            output.append("    <on_handle_intent>\n");
            for(String string : onHandleIntentStrings) {
                output.append("        <string value=\"" + makeXmlConform(string) + "\"/>\n");
            }
            for(int i = 0; i < onHandleIntentExtras.size(); i++) {
                output.append("        <extra key=\"" + makeXmlConform(onHandleIntentExtras.get(i).getKey())
                        + "\" type=\"" + makeXmlConform(onHandleIntentExtras.get(i).getValueType()) + "\"/>\n");
            }
            output.append("    </on_handle_intent>\n");
            return output.toString();
        }
        return "";
    }

    public void finalizeMethods() {
        finalizeOnStartCommand();
        finalizeOnHandleIntent();
    }

    private void finalizeOnStartCommand() {
        for(int i = 0 ; i < onStartCommandExtras.size(); i++) {
            for(int n = i + 1; n < onStartCommandExtras.size(); n++) {
                if(onStartCommandExtras.get(i).getKey().equals(onStartCommandExtras.get(n).getKey())) {
                    if(onStartCommandExtras.get(i).getValueType().equals(""))
                        onStartCommandExtras.get(i).setValueType(onStartCommandExtras.get(n).getValueType());
                    onStartCommandExtras.remove(n);
                    n--;
                }
            }

            Iterator<String> oscsIt = onStartCommandStrings.iterator();
            while(oscsIt.hasNext()) {
                String string = oscsIt.next();
                if(onStartCommandExtras.get(i).getKey().equals(string))
                    oscsIt.remove();
            }
            Iterator<String> it = globalStrings.iterator();
            while (it.hasNext()) {
                String string = it.next();
                if(onStartCommandExtras.get(i).getKey().equals(string))
                    it.remove();
            }
        }
    }

    private void finalizeOnHandleIntent() {
        for(int i = 0 ; i < onHandleIntentExtras.size(); i++) {
            for(int n = i + 1; n < onHandleIntentExtras.size(); n++) {
                if(onHandleIntentExtras.get(i).getKey().equals(onHandleIntentExtras.get(n).getKey())) {
                    if(onHandleIntentExtras.get(i).getValueType().equals(""))
                        onHandleIntentExtras.get(i).setValueType(onHandleIntentExtras.get(n).getValueType());
                    onHandleIntentExtras.remove(n);
                    n--;
                }
            }

            Iterator<String> hisIt = onHandleIntentStrings.iterator();
            while(hisIt.hasNext()) {
                String string = hisIt.next();
                if(onHandleIntentExtras.get(i).getKey().equals(string))
                    hisIt.remove();
            }
            Iterator<String> it = globalStrings.iterator();
            while (it.hasNext()) {
                String string = it.next();
                if(onHandleIntentExtras.get(i).getKey().equals(string))
                    it.remove();
            }
        }
    }
}
