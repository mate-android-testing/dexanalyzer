package de.uni_passau.fim.component;

import de.uni_passau.fim.component.bundle.Extra;
import org.jf.dexlib2.iface.Method;

import java.util.*;

public class BroadcastReceiver extends Component {

    private List<Extra> onReceiveExtras;
    private Set<String> onReceiveStrings;

    private boolean isDynamicReceiver = false;

    public BroadcastReceiver(String name) {
        super(name);
        onReceiveExtras = new ArrayList<>();
        onReceiveStrings = new LinkedHashSet<>();
    }

    /**
     * Marks the broadcast receiver as a dynamic broadcast receiver.
     */
    public void markAsDynamicReceiver() {
        isDynamicReceiver = true;
    }

    public List<Extra> getOnReceiveExtras() {
        return onReceiveExtras;
    }

    public Set<String> getOnReceiveStrings() {
        return onReceiveStrings;
    }

    public String toXml() {
        StringBuilder output = new StringBuilder("<receiver name=\"" + makeXmlConform(name) + "\">\n");
        output.append("    <dynamic value=\"" + makeXmlConform(String.valueOf(isDynamicReceiver)) + "\"/>\n");
        output.append(super.toXml());
        output.append(onReceiveToXml());
        output.append("</receiver>\n");

        return output.toString();
    }

    private String onReceiveToXml() {
        finalizeOnReceive();
        if(!onReceiveExtras.isEmpty() || !onReceiveStrings.isEmpty()) {
            StringBuilder output = new StringBuilder();
            output.append("    <on_receive>\n");
            for(String string : onReceiveStrings) {
                output.append("        <string value=\"" + makeXmlConform(string) + "\"/>\n");
            }
            for(int i = 0; i < onReceiveExtras.size(); i++) {
                output.append("        <extra key=\"" + makeXmlConform(onReceiveExtras.get(i).getKey()) + "\" type=\"" + makeXmlConform(onReceiveExtras.get(i).getValueType()) + "\"/>\n");
            }
            output.append("    </on_receive>\n");
            return output.toString();
        }
        return "";
    }

    public void finalizeMethods() {
        finalizeOnReceive();
    }

    private void finalizeOnReceive() {
        for(int i = 0 ; i < onReceiveExtras.size(); i++) {
            for(int n = i + 1; n < onReceiveExtras.size(); n++) {
                if(onReceiveExtras.get(i).getKey().equals(onReceiveExtras.get(n).getKey())) {
                    if(onReceiveExtras.get(i).getValueType().equals(""))
                        onReceiveExtras.get(i).setValueType(onReceiveExtras.get(n).getValueType());
                    onReceiveExtras.remove(n);
                    n--;
                }
            }

            Iterator ocIt = onReceiveStrings.iterator();
            while(ocIt.hasNext()) {
                String string = (String)ocIt.next();
                if(onReceiveExtras.get(i).getKey().equals(string))
                    ocIt.remove();
            }
            Iterator it = globalStrings.iterator();
            while (it.hasNext()) {
                String string = (String)it.next();
                if(onReceiveExtras.get(i).getKey().equals(string))
                    it.remove();
            }
        }
    }
}
