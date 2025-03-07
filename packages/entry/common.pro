-keep public class com.github.ajalt.clikt.** { *; }
-keep public class com.oracle.truffle.regex.chardata.UnicodeCharacterAliases { *; }
-keep public class elide.entry.MainKt {
    public static void main(java.lang.String[]);
}
-keep public class elide.entry.NativeEntry { *; }
-keepclassmembers class * implements java.io.Serializable {
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepparameternames
-keepattributes Exceptions,InnerClasses,Signature,Record,PermittedSubclasses,
                SourceFile,LineNumberTable,*Annotation*,EnclosingMethod,Synthetic,
                MethodParameters,LocalVariableTable,LocalVariableTypeTable

-adaptresourcefilecontents **.properties,META-INF/MANIFEST.MF,META-INF/services/*,META-INF/native-image/*,META-INF/native-image/*/*

-dontwarn org.graalvm.**
-dontwarn com.github.ajalt.mordant.terminal.terminalinterface.nativeimage.**
-dontwarn com.github.ajalt.mordant.internal.**
-dontwarn kotlinx.coroutines.debug.internal.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn android.**
-dontwarn sun.misc.**
-dontwarn javax.annotation.**
-dontwarn org.slf4j.**
-dontwarn org.pkl.server.**
-dontwarn java.lang.invoke.**
-dontwarn javax.tools.**
-dontwarn javax.naming.**
-dontwarn kotlin.concurrent.**

-dontnote android.**
-dontnote sun.misc.**
-dontnote jdk.internal.**
-dontnote javax.tools.**
-dontnote kotlinx.coroutines.internal.**
-dontnote kotlin.coroutines.jvm.internal.**
-dontnote kotlin.internal.**
