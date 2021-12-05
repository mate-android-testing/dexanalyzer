package de.uni_passau.fim.auermich.android_analysis.scanner;

import com.google.common.collect.Lists;
import de.uni_passau.fim.auermich.android_analysis.component.*;
import de.uni_passau.fim.auermich.android_analysis.utility.ClassUtils;
import de.uni_passau.fim.auermich.android_analysis.utility.ComponentUtils;
import de.uni_passau.fim.auermich.android_analysis.utility.MethodUtils;
import de.uni_passau.fim.auermich.android_analysis.utility.Utility;
import de.uni_passau.fim.auermich.android_analysis.component.bundle.Extra;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.iface.value.StringEncodedValue;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Scans the dex files for the relevant information.
 */
public final class DexScanner {

    private static final Logger LOGGER = LogManager.getLogger(DexScanner.class);

    // the classes.dex files
    private final List<DexFile> dexFiles;

    // stores per class the variables and their values
    private final Map<ClassDef, Map<String, String>> variables = new HashMap<>();

    // all strings that are getting collected during scanning
    private final Set<String> strings = new HashSet<>();

    /**
     * Initialises the scanner.
     *
     * @param dexFiles The list of classes.dex files.
     */
    public DexScanner(List<DexFile> dexFiles) {
        this.dexFiles = dexFiles;
    }

    /**
     * Look ups dynamic broadcast receivers.
     *
     * @param components The list of components.
     */
    public void lookUpDynamicBroadcastReceivers(List<Component> components) {

        Pattern exclusionPattern = Utility.readExcludePatterns();

        for (DexFile dexFile : dexFiles) {
            List<ClassDef> classes = Lists.newArrayList(dexFile.getClasses());

            for (ClassDef classDef : classes) {

                // skip ART classes
                String className = ClassUtils.dottedClassName(classDef.toString());
                if (exclusionPattern != null && exclusionPattern.matcher(className).matches()) {
                    continue;
                }

                for (Method method : classDef.getMethods()) {
                    scanMethodForDynamicBroadcastReceiver(components, method);
                }
            }
        }
    }

    /**
     * Scans the given method for a dynamic broadcast receiver registration invocation.
     *
     * @param components The list of components.
     * @param method The method to be inspected.
     */
    private void scanMethodForDynamicBroadcastReceiver(List<Component> components, Method method) {

        MethodImplementation implementation = method.getImplementation();

        if (implementation != null) {

            List<Instruction> instructions = Lists.newArrayList(implementation.getInstructions());

            for (int i = 0; i < instructions.size(); i++) {

                Instruction instruction = instructions.get(i);

                // only invoke-virtual instructions can refer to registering a broadcast receiver dynamically
                if (instruction.getOpcode() == Opcode.INVOKE_VIRTUAL) {

                    Instruction35c invoke = (Instruction35c) instruction;
                    Reference targetMethod = invoke.getReference();

                    // check whether Context.registerReceiver() is called
                    if (targetMethod.toString().endsWith("registerReceiver(Landroid/content/BroadcastReceiver;" +
                            "Landroid/content/IntentFilter;)Landroid/content/Intent;")
                            // further overloaded registerReceiver() methods
                        || targetMethod.toString().endsWith("registerReceiver(Landroid/content/BroadcastReceiver;" +
                            "Landroid/content/IntentFilter;Ljava/lang/String;Landroid/os/Handler;I)" +
                            "Landroid/content/Intent;")
                        || targetMethod.toString().endsWith("registerReceiver(Landroid/content/BroadcastReceiver;" +
                            "Landroid/content/IntentFilter;Ljava/lang/String;Landroid/os/Handler;)" +
                            "Landroid/content/Intent;")
                        || targetMethod.toString().endsWith("registerReceiver(Landroid/content/BroadcastReceiver;" +
                            "Landroid/content/IntentFilter;I)Landroid/content/Intent;")) {

                        LOGGER.debug("Backtracking dynamic broadcast receiver registration in method: " + method);

                        // TODO: handle dynamic receivers that are stored in a class variable

                        /*
                         * A typical call to Context.registerReceiver() looks as follows:
                         *
                         * invoke-virtual {p0, v1, v0}, Landroid/content/Context;->
                         * registerReceiver(Landroid/content/BroadcastReceiver;Landroid/content/IntentFilter;)
                         * Landroid/content/Intent;
                         *
                         * where
                         *   p0 refers to the context object (register C)
                         *   v1 refers to the broadcast receiver instance (register D)
                         *   v0 refers to the attached intent filter (register E)
                         *
                         * Thus, we need to backtrack register D in order to derive the name of the broadcast receiver.
                         */

                        // start backtracking from preceding instruction
                        int index = i - 1;

                        // backtrack broadcast receiver instance first
                        Component receiver = backtrackReceiver(components, instructions,
                                index, invoke.getRegisterD());

                        if (receiver != null) {

                            // mark receiver as dynamic one
                            ((BroadcastReceiver) receiver).markAsDynamicReceiver();

                            // backtrack intent filter (inherently adds the filter)
                            backtrackIntentFilter(receiver, instructions, index, invoke.getRegisterE());
                        }
                    }
                }
            }
        }
    }

    /**
     * Backtracks for a possible added intent filter to the dynamically added broadcast receiver.
     *
     * @param receiver The dynamically registered broadcast receiver.
     * @param instructions The set of instructions for a given method.
     * @param currentInstructionIndex The instruction index where to start backtracking from.
     * @param registerID The register id referring to the intent filter instance specified in registerReceiver().
     */
    private void backtrackIntentFilter(Component receiver, List<Instruction> instructions, int currentInstructionIndex, int registerID) {

        // TODO: support backtracking of multiple actions and multiple intent filters
        Component.IntentFilter intentFilter = receiver.new IntentFilter();

        // unless we haven't reached the first instruction
        while (currentInstructionIndex >= 0) {

            Instruction instruction = instructions.get(currentInstructionIndex);

            // check for invocations on the intent filter instance
            if (instruction.getOpcode() == Opcode.INVOKE_VIRTUAL) {

                Instruction35c invoke = (Instruction35c) instruction;

                // TODO: check for calls to add data URI

                // check for possible actions
                if (invoke.getReference().toString()
                        .equals("Landroid/content/IntentFilter;->addAction(Ljava/lang/String;)V")) {

                    /*
                     * A possible invocation looks as follows:
                     *  invoke-virtual {v0, v1}, Landroid/content/IntentFilter;->addAction(Ljava/lang/String;)V
                     * where
                     *  v0 refers to the intent filter instance (register C)
                     *  v1 refers to the action string (register D)
                     */

                    // check whether we inspect the intent filter object specified in the call registerReceiver()
                    if (invoke.getRegisterC() == registerID) {

                        // now backtrack again for string constant specifying action (register D)
                        String action = backtrackStringConstant(instructions, currentInstructionIndex-1,
                                invoke.getRegisterD());

                        if (action != null) {
                            intentFilter.addAction(action);
                        }
                    }
                    // check for possible categories
                } else if (invoke.getReference().equals("Landroid/content/IntentFilter;->addCategory(Ljava/lang/String;)V")) {

                    /*
                     * A possible invocation looks as follows:
                     *  invoke-virtual {v0, v1}, Landroid/content/IntentFilter;->addCategory(Ljava/lang/String;)V
                     * where
                     *  v0 refers to the intent filter instance (register C)
                     *  v1 refers to the category string (register D)
                     */

                    // check whether we inspect the intent filter object specified in the call registerReceiver()
                    if (invoke.getRegisterC() == registerID) {

                        // now backtrack again for string constant specifying category (register D)
                        String category = backtrackStringConstant(instructions, currentInstructionIndex-1,
                                invoke.getRegisterD());

                        if (category != null) {
                            intentFilter.addCategory(category);
                        }
                    }
                    // check for possible data scheme specific part (ssp) + type (int)
                } else if (invoke.getReference().equals("Landroid/content/IntentFilter;->addDataSchemeSpecificPart(Ljava/lang/String;I)V")) {

                    /*
                     * A possible invocation looks as follows:
                     *  invoke-virtual {v0, v1, v3}, Landroid/content/IntentFilter;->addDataSchemeSpecificPart(Ljava/lang/String;I)V
                     * where
                     *  v0 refers to the intent filter instance (register C)
                     *  v1 refers to the data scheme specific part (string, register D)
                     *  v3 refers to the type (int, registerE)
                     */

                    // check for possible data path
                } else if (invoke.getReference().equals("Landroid/content/IntentFilter;->addDataPath(Ljava/lang/String;I)V")) {

                    /*
                     * A possible invocation looks as follows:
                     *  invoke-virtual {v0, v1, v3}, Landroid/content/IntentFilter;->addDataPath(Ljava/lang/String;I)V
                     * where
                     *  v0 refers to the intent filter instance (register C)
                     *  v1 refers to the data path (string, register D)
                     *  v3 refers to the type (int, registerE)
                     */

                    // check for possible data type
                } else if (invoke.getReference().equals("Landroid/content/IntentFilter;->addDataType(Ljava/lang/String;)V")) {

                    /*
                     * A possible invocation looks as follows:
                     *  invoke-virtual {v0, v1}, Landroid/content/IntentFilter;->addDataType(Ljava/lang/String;)V
                     * where
                     *  v0 refers to the intent filter instance (register C)
                     *  v1 refers to the data type string (register D)
                     */

                    // check for possible data authority
                } else if (invoke.getReference().equals("Landroid/content/IntentFilter;" +
                        "->addDataAuthority(Ljava/lang/String;Ljava/lang/String;)V")) {

                    /*
                     * A possible invocation looks as follows:
                     *  invoke-virtual {v0, v1, v2}, Landroid/content/IntentFilter;
                     *          ->addDataAuthority(Ljava/lang/String;Ljava/lang/String;)V
                     * where
                     *  v0 refers to the intent filter instance (register C)
                     *  v1 refers to the host (string, register D)
                     *  v2 refers to the port (string, registerE)
                     */

                    // check for possible data scheme
                } else if (invoke.getReference().equals("Landroid/content/IntentFilter;->addDataScheme(Ljava/lang/String;)V")) {

                    /*
                     * A possible invocation looks as follows:
                     *  invoke-virtual {v0, v1}, Landroid/content/IntentFilter;->addDataScheme(Ljava/lang/String;)V
                     * where
                     *  v0 refers to the intent filter instance (register C)
                     *  v1 refers to the data scheme string (register D)
                     */

                }
            } else if (instruction.getOpcode() == Opcode.INVOKE_DIRECT) {

                Instruction35c invoke = (Instruction35c) instruction;

                // check for intent filter constructor with action parameter
                if (invoke.getReference().toString()
                        .equals("Landroid/content/IntentFilter;-><init>(Ljava/lang/String;)V")) {

                    /*
                     * A possible invocation looks as follows:
                     *  invoke-direct {v0, v1}, Landroid/content/IntentFilter;-><init>(Ljava/lang/String;)V
                     * where
                     *  v0 refers to the intent filter instance (register C)
                     *  v1 refers to the action string (register D)
                     */

                    // check whether we inspect the intent filter object specified in the call registerReceiver()
                    if (invoke.getRegisterC() == registerID) {

                        // now backtrack again for string constant specifying action (register D)
                        String action = backtrackStringConstant(instructions, currentInstructionIndex-1,
                                invoke.getRegisterD());

                        if (action != null) {
                            intentFilter.addAction(action);
                        }
                    }
                }
            }
            currentInstructionIndex--;
        }

        // add intent filter to component
        receiver.addIntentFilter(intentFilter);
    }

    /**
     * Checks for an string constant, e.g. an action, corresponding to the invocation of IntentFilter.addAction(),
     * IntentFilter.addCategory(), etc.
     *
     * @param instructions The set of instructions for a given method.
     * @param currentInstructionIndex The index where to start backtracking from.
     * @param registerID The register ID which refers to the register holding the string constant.
     * @return Returns the string constant or {@code null} if the constant couldn't be derived.
     */
    private String backtrackStringConstant(List<Instruction> instructions, int currentInstructionIndex, int registerID) {

        // unless we haven't reached the first instruction
        while (currentInstructionIndex >= 0) {

            Instruction instruction = instructions.get(currentInstructionIndex);

            // actions are typically hold in string constants
            if (instruction.getOpcode() == Opcode.CONST_STRING) {

                Instruction21c constString = (Instruction21c) instruction;

                // check whether the const string refers to the right register
                if (constString.getRegisterA() == registerID) {
                    LOGGER.debug("Found String Constant: " + constString.getReference());
                    return constString.getReference().toString();
                }
            }
            currentInstructionIndex--;
        }
        return null;
    }

    /**
     * Backtracks the broadcast receiver to its instance creation.
     *
     * @param components The list of components.
     * @param instructions The set of instructions of the given method.
     * @param currentInstructionIndex The instruction index where to start backtracking from.
     * @param registerID The register id that refers to the register holding the broadcast receiver instance.
     * @return Returns a {@link BroadcastReceiver} instance or {@code null} if the broadcast receiver couldn't be derived.
     */
    private Component backtrackReceiver(List<Component> components, List<Instruction> instructions,
                                        int currentInstructionIndex, int registerID) {

        // unless we haven't reached the first instruction
        while (currentInstructionIndex >= 0) {

            Instruction instruction = instructions.get(currentInstructionIndex);

            // TODO: there might be several ways to get a broadcast receiver instance

            // check whether the corresponding broadcast receiver has been initialised by the current instruction
            if (instruction.getOpcode() == Opcode.NEW_INSTANCE) {

                Instruction21c newInstance = (Instruction21c) instruction;
                LOGGER.debug("Register A: " + newInstance.getRegisterA());

                // check whether the register id matches the broadcast receiver parameter register id
                if (newInstance.getRegisterA() == registerID) {
                    LOGGER.debug("Receiver: " + newInstance.getReference());

                    String receiverName = ClassUtils.dottedClassName(newInstance.getReference().toString());

                    // lookup receiver in the list of components and copy derived constants etc.
                    for (Component component : components) {

                        if (component.getName().equals(receiverName)) {
                            LOGGER.debug("Found Receiver: " + component);
                            return component;
                        }
                    }
                }
            }
            currentInstructionIndex--;
        }
        return null;
    }

    /**
     * Extracts the relevant data, e.g. string constants, for the ExecuteMATERandomExplorationIntent strategy. Only
     * considers data from activities, services and broadcast receivers.
     *
     * @param components The list of components.
     */
    public void extractIntentInfo(List<Component> components) {
        extractInfo(components, true);
    }

    /**
     * Extracts the relevant data, e.g. string constants, for the ExecuteMATERandomExplorationIntent strategy. Only
     * considers data from activities, services and broadcast receivers.
     *
     * @param components The list of components.
     * @param intentInfo Indicates if intentInfo or all static strings should be
     *                   considered.
     */
    private void extractInfo(List<Component> components, boolean intentInfo) {

        for (Component component : components) {
            if (intentInfo) {
                if (component instanceof Fragment) {
                    // we are not interested in fragments
                    continue;
                }
            } else {
                if (component instanceof Service
                        || component instanceof BroadcastReceiver) {
                    // we only want to have string constants in Fragment or
                    // Activity
                    continue;
                }
            }

            ClassDef classDef = component.getClazz();
            Map<String, String> classVariables = new HashMap<>();

            // look up the constructor(s) for variable assignments
            for (Method method : classDef.getMethods()) {
                if (method.getName().equals("<init>") || method.getName()
                        .equals("<clinit>")) {
                    classVariables.putAll(lookupConstructor(method));
                }
            }

            variables.put(classDef, classVariables);

            // lookup the classes' fields for string constants
            lookupStringConstants(component, classDef);
            if (intentInfo) {
                // scan each method
                for (Method method : classDef.getMethods()) {
                    scanMethod(component, method, classVariables);
                }
            } else {
                for (Method method : classDef.getMethods()) {
                    if (component instanceof Activity) {
                        scanComponentMethod(method, classVariables,
                                ((Activity) component).getMethodStrings(),
                                ((Activity) component).getOnCreateExtras());
                        continue;
                    }
                    if (component instanceof Fragment) {
                        scanComponentMethod(method, classVariables,
                                ((Fragment) component).getMethodStrings(),
                                ((Fragment) component).getOnCreateExtras());
                    }
                }
            }
        }
    }

    /**
     * Extract all static string constants of the code in activity or fragment classes. All methods are considered.
     *
     * @param components All existing components.
     */
    public void extractStringConstants(List<Component> components) {
        extractInfo(components, false);
        for (Component com : components) {
            if (com instanceof Activity) {
                com.addStaticStrings(((Activity) com).getMethodStrings());
                com.addStaticStrings(com.getGlobalStrings());
            } else if (com instanceof Fragment) {
                com.addStaticStrings(((Fragment) com).getMethodStrings());
                com.addStaticStrings(com.getGlobalStrings());
            }
        }
    }

    /**
     * Extracts the components, i.e. activities, services, fragments and broadcast receivers, residing in the
     * application package.
     *
     * @param packageName       The application package name.
     * @param resolveAllClasses Whether all classes should be resolved or not.
     * @return Returns the list of retrieved components.
     */
    public List<Component> lookUpComponents(final String packageName, final boolean resolveAllClasses) {

        // exclude certain classes from inspection, e.g. ART classes
        Pattern exclusionPattern = Utility.readExcludePatterns();

        List<Component> components = new ArrayList<>();

        for (DexFile dexFile : dexFiles) {

            List<ClassDef> classes = Lists.newArrayList(dexFile.getClasses());

            for (ClassDef classDef : classes) {

                String className = ClassUtils.dottedClassName(classDef.toString());

                // skip classes that are belonging to the app package
                if ((exclusionPattern != null && exclusionPattern.matcher(className).matches())
                        || (!resolveAllClasses && !className.startsWith(packageName))) {
                    continue;
                }

                Component component = findComponent(classes, classDef);

                if (component != null) {
                    components.add(component);
                }
            }
        }
        return components;
    }

    /**
     * Invokes the correct scan method depending on the component's type.
     *
     * @param component The given component.
     * @param method    The current method to be inspected.
     * @param variables The variable assignments of the component/class.
     */
    private void scanMethod(Component component, Method method, Map<String, String> variables) {

        if (component instanceof Activity) {
            scanActivity((Activity) component, method, variables);
        } else if (component instanceof Service) {
            scanService((Service) component, method, variables);
        } else if (component instanceof BroadcastReceiver) {
            scanReceiver((BroadcastReceiver) component, method, variables);
        }
    }

    /**
     * Looks up each constructor in a given class for variable assignments. Returns a map containing for each variable
     * its initial value.
     *
     * @param classDef The class to be inspected.
     * @return Returns the map of variables and its values.
     */
    private Map<String, String> lookupConstructorsForVariables(ClassDef classDef) {

        Map<String, String> classVariables = new HashMap<>();

        // look up the constructor(s) for variable assignments
        for (Method method : classDef.getMethods()) {
            if (method.getName().equals("<init>") || method.getName().equals("<clinit>")) {
                classVariables.putAll(lookupConstructor(method));
            }
        }
        return classVariables;
    }


    /**
     * Scans a component's interesting methods, e.g. the onCreate method of an activity, for strings and extras.
     *
     * @param method         The method to be inspected.
     * @param classVariables The variable assignments of the entire class.
     * @param methodStrings  The method strings that are getting collected during scanning.
     * @param extras         The extras that are getting collected during scanning.
     */
    private void scanComponentMethod(Method method, Map<String, String> classVariables,
                                     Set<String> methodStrings, List<Extra> extras) {

        MethodImplementation implementation = method.getImplementation();

        if (implementation != null) {

            List<Instruction> instructions = Lists.newArrayList(implementation.getInstructions());

            for (int i = 0; i < instructions.size(); i++) {

                Instruction instruction = instructions.get(i);

                // check for invoke instruction
                if (instruction instanceof Instruction35c && instruction.getOpcode() != Opcode.FILLED_NEW_ARRAY
                        && instruction.getOpcode() != Opcode.FILLED_NEW_ARRAY_RANGE) {

                    Instruction35c invoke = (Instruction35c) instruction;
                    MethodReference methodReference = (MethodReference) invoke.getReference();

                    // check whether a method is called that expects as parameter an Intent
                    if (methodReference.getParameterTypes().contains("Landroid/content/Intent;")) {

                        String methodSignature = invoke.getReference().toString();
                        String className = methodSignature.split("->")[0];
                        Optional<Method> targetMethod = MethodUtils.searchForTargetMethod(dexFiles, methodSignature);
                        Optional<ClassDef> targetClass = ClassUtils.searchForTargetClass(dexFiles, className);

                        if (targetMethod.isPresent() && targetClass.isPresent()) {

                            ClassDef targetClassDef = targetClass.get();

                            // check whether the target class has been inspected already for variable assignments
                            if (!variables.containsKey(targetClassDef)) {
                                variables.put(targetClassDef, lookupConstructorsForVariables(targetClassDef));
                            }

                            // inspect target method
                            scanComponentMethod(targetMethod.get(), variables.get(targetClassDef), methodStrings, extras);
                        }
                    }

                    // look if the target method is some Intent class method
                    if (methodReference.getDefiningClass().equals("Landroid/content/Intent;")
                            // we are only interested in the methods get$TYPE$
                            && ((methodReference.getName().contains("get")
                            && ((methodReference.getName().contains("Extra")))
                            && !(methodReference).getName().contains("getExtras"))
                            || (methodReference).getName().contains("hasExtra"))) {

                        // get the type of extra, e.g. getStringExtra -> String, see the class Intent for its getter methods
                        String extraType = methodReference.getName().substring(3, methodReference.getName().length() - 5);

                        // get the key of the extra
                        String extraKey = getExtraKey(instructions, i, invoke.getRegisterD(), classVariables);
                        if (extraKey != null)
                            extras.add(new Extra(extraKey, convertExtraType(extraType)));

                        // look if the target method is some Bundle class method
                    } else if (methodReference.getDefiningClass().equals("Landroid/os/Bundle;")
                            && (methodReference.getName().contains("get")
                            // can only derive the key from it, and only if the key is present -> may remove
                            || (methodReference).getName().contains("containsKey"))
                            && isIntentBundle(instructions, i, invoke.getRegisterC())) {

                        // get the type of extra, e.g. getString -> String, see the class Bundle for its getter methods
                        String extraType = methodReference.getName().substring(3);

                        // only tells us the name of the key if the key is present in the bundle, nothing about its value type
                        if (methodReference.getName().contains("containsKey"))
                            extraType = "";

                        // get the key of the extra
                        String extraKey = getExtraKey(instructions, i, invoke.getRegisterD(), classVariables);
                        if (extraKey != null)
                            extras.add(new Extra(extraKey, convertExtraType(extraType)));
                    }
                } else if (instruction.getOpcode() == Opcode.CONST_STRING
                        || instruction.getOpcode() == Opcode.CONST_STRING_JUMBO) {

                    ReferenceInstruction referenceInstruction = (ReferenceInstruction) instruction;

                    if (!referenceInstruction.getReference().toString().isEmpty()) {

                        String methodString = ((StringReference) (referenceInstruction).getReference()).getString();
                        if (!stringUsedForOwnIntents(instructions, i, ((OneRegisterInstruction) instruction).getRegisterA())) {
                            methodStrings.add(methodString);
                        }

                    }
                }
            }
        }
    }

    /**
     * Converts the type of some extra into an internal representation. That is, the phrase 'ArrayList' is replaced
     * by '<>' and the phrase 'Array' is replaced by '[]'; the
     * remaining part is left unchanged.
     *
     * @param extraType The extra type to be converted.
     * @return Returns the converted extra type.
     */
    private String convertExtraType(String extraType) {

        String convertedType = extraType;

        if (extraType.endsWith("ArrayList")) {
            convertedType = extraType.replace("ArrayList", "<>");
        } else if (extraType.endsWith("Array")) {
            convertedType = extraType.replace("Array", "[]");
        }

        return convertedType;
    }


    /**
     * Scans an activity's onCreate and onNewIntent method.
     *
     * @param activity       The activity component.
     * @param method         The current method.
     * @param classVariables The variable assignments of the entire class.
     */
    private void scanActivity(Activity activity, Method method, Map<String, String> classVariables) {
        if (method.getName().equals("onCreate")) {
            scanComponentMethod(method, classVariables, activity.getOnCreateStrings(), activity.getOnCreateExtras());
        } else if (method.getName().equals("onNewIntent")) {
            scanComponentMethod(method, classVariables, activity.getOnNewIntentStrings(), activity.getOnNewIntentExtras());
        }
    }

    /**
     * Scans a service's onStartCommand and onHandleIntent method.
     *
     * @param service        The service component.
     * @param method         The current method.
     * @param classVariables The variable assignments of the entire class.
     */
    private void scanService(Service service, Method method, Map<String, String> classVariables) {
        if (method.getName().equals("onStartCommand")) {
            scanComponentMethod(method, classVariables, service.getOnStartCommandStrings(), service.getOnStartCommandExtras());
        } else if (method.getName().equals("onHandleIntent")) {
            scanComponentMethod(method, classVariables, service.getOnHandleIntentStrings(), service.getOnHandleIntentExtras());
        }
    }

    /**
     * Scans a receiver's onReceive method.
     *
     * @param receiver       The broadcast receiver component.
     * @param method         The current method.
     * @param classVariables The variable assignments of the entire class.
     */
    private void scanReceiver(BroadcastReceiver receiver, Method method, Map<String, String> classVariables) {
        if (method.getName().equals("onReceive")) {
            scanComponentMethod(method, classVariables, receiver.getOnReceiveStrings(), receiver.getOnReceiveExtras());
        }
    }

    // TODO: documentation (I haven't got the idea of this method yet)
    private boolean stringUsedForOwnIntents(List<Instruction> instructions, int currentIndex, int register) {

        // TODO: check all the methods whether this really works
        for (int i = currentIndex + 1; i < instructions.size(); i++) {

            Instruction instruction = instructions.get(i);

            // checks whether we deal with an invoke instruction that uses a certain register as part of its parameters
            if (instruction.getOpcode() == Opcode.INVOKE_VIRTUAL
                    && isInvokeInstructionUsingRegister(instruction, register)) {

                Instruction35c invoke = (Instruction35c) instruction;
                MethodReference methodReference = ((MethodReference) invoke.getReference());

                // check whether the invocation refers to the Intent or Bundle class
                return methodReference.getDefiningClass().equals("Landroid/content/Intent;")
                        || methodReference.getDefiningClass().equals("Landroid/os/Bundle;");

                // if there are several strings / objects passed to a method -> inspect next instruction, otherwise abort
            } else if (!instruction.getOpcode().name.contains("const")
                    && !instruction.getOpcode().name.contains("get")) {
                break;
            }
        }
        return false;
    }

    /**
     * Checks whether the given instruction, which must be of type invoke, uses as parts of its registers a given register.
     *
     * @param instruction The invoke instruction.
     * @param register    The register to check whether it is used in the given instruction.
     * @return Returns {@code true} if the invoke instruction uses the given register,
     * otherwise {@code false}.
     */
    private boolean isInvokeInstructionUsingRegister(Instruction instruction, int register) {

        Instruction35c invoke = (Instruction35c) instruction;
        return invoke.getRegisterD() == register
                || invoke.getRegisterE() == register
                || invoke.getRegisterF() == register
                || invoke.getRegisterG() == register;
    }


    /**
     * TODO: refactor + add description
     *
     * @param instructions The set of instructions.
     * @param currentIndex The instruction index of the current instruction.
     * @param register     The register ID containing the bundle.
     * @return Returns {@code true} if ..., otherwise {@code false}.
     */
    private boolean isIntentBundle(List<Instruction> instructions, int currentIndex, int register) {

        EnumSet<Opcode> opcodes = EnumSet.of(Opcode.MOVE_OBJECT, Opcode.MOVE_OBJECT_FROM16,
                Opcode.MOVE_OBJECT_16);

        for (int i = currentIndex - 1; i >= 0; i--) {

            Instruction instruction = instructions.get(i);

            if (opcodes.contains(instruction.getOpcode())) {

                // all of these move instructions share the TwoRegisterInstruction interface
                TwoRegisterInstruction twoRegisterInstruction = (TwoRegisterInstruction) instruction;

                if (twoRegisterInstruction.getRegisterA() == register) {
                    register = twoRegisterInstruction.getRegisterB();
                }
            } else if (instruction.getOpcode() == Opcode.MOVE_RESULT_OBJECT) {
                // we need a special treatment for this kind of move instruction

                OneRegisterInstruction oneRegisterInstruction = (OneRegisterInstruction) instruction;

                if (oneRegisterInstruction.getRegisterA() == register) {

                    // inspect the predecessor
                    if ((i - 1 >= 0) && instructions.get(i - 1).getOpcode() == Opcode.INVOKE_VIRTUAL) {

                        Instruction35c invoke = (Instruction35c) instructions.get(i - 1);
                        MethodReference methodReference = (MethodReference) invoke.getReference();

                        if (methodReference.getDefiningClass().equals("Landroid/content/Intent;")
                                && methodReference.getName().equals("getExtras")) {
                            return true;
                        }
                    } else {
                        return false;
                    }
                }
            }
        }
        return false;
    }


    /**
     * Searches through the predecessing instructions of the instruction located at index {@param currentIndex} for
     * assignments of the extra value hold in {@param register}. Returns the corresponding key, that is either
     * a local or global variable name or {@code null} if the key couldn't be found.
     *
     * @param instructions  The set of instructions.
     * @param currentIndex  The instruction index.
     * @param register      The register ID that contains the extra value.
     * @param globalStrings The set of strings for the a given class.
     * @return Returns the extra's key or {@code null} if the key couldn't be found.
     */
    private String getExtraKey(List<Instruction> instructions, int currentIndex, int register, Map<String, String> globalStrings) {

        EnumSet<Opcode> moveOpcodes = EnumSet.of(Opcode.MOVE_OBJECT, Opcode.MOVE_OBJECT_FROM16,
                Opcode.MOVE_OBJECT_16);

        for (int i = currentIndex - 1; i >= 0; i--) {

            Instruction instruction = instructions.get(i);

            if (instruction.getOpcode() == Opcode.CONST_STRING
                    || instruction.getOpcode() == Opcode.CONST_STRING_JUMBO) {

                OneRegisterInstruction oneRegisterInstruction = (OneRegisterInstruction) instruction;

                if (oneRegisterInstruction.getRegisterA() == register) {
                    // TODO: getReference().getString() might be the same as getReference().toString()
                    return ((StringReference) ((ReferenceInstruction) instruction).getReference()).getString();
                }
            } else if (instruction.getOpcode() == Opcode.SGET_OBJECT
                    || instruction.getOpcode() == Opcode.IGET_OBJECT) {

                OneRegisterInstruction oneRegisterInstruction = (OneRegisterInstruction) instruction;

                if (oneRegisterInstruction.getRegisterA() == register) {
                    return globalStrings.get(((FieldReference) ((ReferenceInstruction) instruction).getReference()).getName());
                }
            } else if (moveOpcodes.contains(instruction.getOpcode())) {

                // all of these move instructions share the TwoRegisterInstruction interface
                TwoRegisterInstruction twoRegisterInstruction = (TwoRegisterInstruction) instruction;

                if (twoRegisterInstruction.getRegisterA() == register) {
                    register = twoRegisterInstruction.getRegisterB();
                }
            }
        }
        return null;
    }

    /**
     * Looks up for the succeeding instruction that stores a value in a variable.
     *
     * @param successor The successor instruction.
     * @return Returns the name of the variable or {@code null} if the name
     * couldn't be found.
     */
    private String lookupVariableName(Instruction successor) {

        EnumSet<Opcode> opcodes = EnumSet.of(Opcode.SPUT_OBJECT, Opcode.IPUT_OBJECT);

        if (opcodes.contains(successor.getOpcode())) {

            ReferenceInstruction instruction = (ReferenceInstruction) successor;
            return ((FieldReference) instruction.getReference()).getName();
        }
        return null;
    }

    /**
     * Looks up the constructor of relevant components for global variable assignments.
     *
     * @param method The method representing the constructor.
     * @return Returns a mapping for variables and their values.
     */
    private Map<String, String> lookupConstructor(Method method) {

        Map<String, String> variables = new HashMap<>();

        MethodImplementation methodImplementation = method.getImplementation();

        if (methodImplementation != null) {

            List<Instruction> instructions = Lists.newArrayList(methodImplementation.getInstructions());

            for (int i = 0; i < instructions.size(); i++) {

                Instruction instruction = instructions.get(i);

                EnumSet<Opcode> opcodes = EnumSet.of(Opcode.CONST_STRING, Opcode.CONST_STRING_JUMBO);

                if (opcodes.contains(instruction.getOpcode())) {
                    String value = ((ReferenceInstruction) instruction).getReference().toString();
                    if (!value.isEmpty()) {
                        // check which variable holds the given value -> should be the successor instruction
                        Instruction successor = instructions.get(i + 1);
                        String varName = lookupVariableName(successor);
                        if (varName != null) {
                            variables.put(varName, value);
                        }
                    }
                }
            }
        }
        return variables;
    }

    /**
     * Looks up all fields in a given class for string constants.
     *
     * @param component The component representing the class.
     * @param classDef  The class file.
     */
    private void lookupStringConstants(Component component, ClassDef classDef) {

        // search through all instance and static fields
        for (Field field : classDef.getFields()) {

            EncodedValue encodedValue = field.getInitialValue();

            if (encodedValue instanceof StringEncodedValue) {

                String value = ((StringEncodedValue) encodedValue).getValue();

                if (!value.isEmpty()) {
                    component.addStringConstant(value);
                    strings.add(value);
                }
            }
        }
    }

    /**
     * Looks up all fields in a given class for string constants.
     *
     * @param classDef The class file.
     */
    private Set<String> lookupStringConstants(ClassDef classDef) {

        Set<String> stringConstants = new HashSet<>();

        // search through all instance and static fields
        for (Field field : classDef.getFields()) {

            EncodedValue encodedValue = field.getInitialValue();

            if (encodedValue instanceof StringEncodedValue) {

                String value = ((StringEncodedValue) encodedValue).getValue();

                if (!value.isEmpty()) {
                    stringConstants.add(value);
                }
            }
        }
        return stringConstants;
    }


    /**
     * Checks whether the given class represents an activity, a service, a broadcast receiver or a fragment.
     *
     * @param classes      The set of classes.
     * @param currentClass The current class.
     * @return Returns the corresponding {@link Component} or {@code null} if the current class doesn't represent
     * an activity, a service or a broadcast receiver.
     */
    private Component findComponent(List<ClassDef> classes, ClassDef currentClass) {

        if (ComponentUtils.isActivity(classes, currentClass)) {
            return new Activity(currentClass);
        } else if (ComponentUtils.isService(classes, currentClass)) {
            return new Service(currentClass);
        } else if (ComponentUtils.isBroadcastReceiver(classes, currentClass)) {
            return new BroadcastReceiver(currentClass);
        } else if (ComponentUtils.isFragment(classes, currentClass)) {
            return new Fragment(currentClass);
        } else {
            return null;
        }
    }

}