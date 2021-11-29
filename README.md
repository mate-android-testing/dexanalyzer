# DexAnalyzer

This tool performs various static analyses directly on the dalvik bytecode. The current functionality entails:

* The extraction of static data necessary for the `ExecuteMATERandomExplorationIntent` testing strategy.

## How to run DexAnalyzer

The only mandatory input is the path to the APK file. The name of the APK file must conform to the usual convention:
<package-name>.apk. Note that you should use the plain APK and not any instrumented APK, otherwise there might be 
static data extracted, which is not part of the original app. Read the `Main` class for further optional parameters.
The tool will output a folder called `<package-name>/static_data` in the same directory as the APK. Inside this folder
there will be the relevant static data files. Copy this folder within the app folder (`apps/<package-name>`) that is used
by `MATE`.