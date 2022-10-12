package de.uni_passau.fim.auermich.android_analysis.component;

import de.uni_passau.fim.auermich.android_analysis.component.bundle.Extra;
import org.jf.dexlib2.iface.ClassDef;

import java.util.*;

public class BroadcastReceiver extends Component {

    private final List<Extra> onReceiveExtras;
    private final Set<String> onReceiveStrings;

    private boolean isDynamicReceiver = false;

    public BroadcastReceiver(String className) {
        super(className);
        onReceiveExtras = new ArrayList<>();
        onReceiveStrings = new LinkedHashSet<>();
    }

    public BroadcastReceiver(ClassDef clazz) {
        super(clazz);
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

    // a simplified xml representation of a component solely containing the name and the attributes exported and enabled
    @Override
    public String toXmlSimple() {
        return "<" + getType()
                + " name=\"" + makeXmlConform(name) + "\""
                + " enabled=\"" + enabled +"\""
                + " exported=\"" + exported +"\""
                + " dynamic=\"" + isDynamicReceiver +"\""
                + "></" + getType() + ">";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getType() {
        return "receiver";
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
                output.append("        <extra key=\"" + makeXmlConform(onReceiveExtras.get(i).getKey()) + "\" type=\""
                        + makeXmlConform(onReceiveExtras.get(i).getValueType()) + "\"/>\n");
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

            Iterator<String> ocIt = onReceiveStrings.iterator();
            while(ocIt.hasNext()) {
                String string = ocIt.next();
                if(onReceiveExtras.get(i).getKey().equals(string))
                    ocIt.remove();
            }
            Iterator<String> it = globalStrings.iterator();
            while (it.hasNext()) {
                String string = it.next();
                if(onReceiveExtras.get(i).getKey().equals(string))
                    it.remove();
            }
        }
    }
}
