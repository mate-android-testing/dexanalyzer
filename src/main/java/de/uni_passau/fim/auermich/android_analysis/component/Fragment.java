package de.uni_passau.fim.auermich.android_analysis.component;

import org.jf.dexlib2.iface.ClassDef;

public class Fragment extends Component {

    public Fragment(ClassDef clazz) {
        super(clazz);
    }

    @Override
    protected String getType() {
        return "fragment";
    }
}
