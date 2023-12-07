package de.uni_passau.fim.auermich.android_analysis.component.bundle;

import de.uni_passau.fim.auermich.android_analysis.component.Component;
import com.android.tools.smali.dexlib2.iface.ClassDef;

public class ActivityAlias extends Component {

    public ActivityAlias(ClassDef clazz) {
        super(clazz);
    }

    public ActivityAlias(String className) {
        super(className);
    }

    @Override
    protected String getType() {
        return "activity-alias";
    }
}
