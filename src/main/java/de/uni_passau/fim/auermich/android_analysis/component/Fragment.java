package de.uni_passau.fim.auermich.android_analysis.component;

import de.uni_passau.fim.auermich.android_analysis.component.bundle.Extra;
import org.jf.dexlib2.iface.ClassDef;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Fragment extends Component {

    private Set<String> methodStrings;
    private List<Extra> onCreateExtras;

    public Fragment(ClassDef clazz) {
        super(clazz);
        methodStrings = new HashSet<>();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected String getType() {
        return "fragment";
    }

    public Set<String> getMethodStrings() {
        return methodStrings;
    }

    public List<Extra> getOnCreateExtras() {
        return onCreateExtras;
    }
}
