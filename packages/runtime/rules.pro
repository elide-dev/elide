-libraryjars <java.home>/jmods/java.base.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.compiler.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.datatransfer.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.desktop.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.instrument.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.logging.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.management.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.management.rmi.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.naming.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.net.http.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.prefs.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.rmi.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.scripting.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.se.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.security.jgss.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.security.sasl.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.smartcardio.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.sql.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.sql.rowset.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.transaction.xa.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.xml.crypto.jmod(!**.jar;!module-info.class)
-libraryjars <java.home>/jmods/java.xml.jmod(!**.jar;!module-info.class)

-dontoptimize
-dontshrink

-keep class !com.adierebel.**, !com.adierebel.** { *; }
-keepclasseswithmembers public class com.javafx.main.Main, org.eclipse.jdt.internal.jarinjarloader.*, com.adierebel.MainKt {
    public static void main(java.lang.String[]);
}

-dontwarn okio.**
-dontwarn org.apache.**
-dontwarn com.sun.**
-dontwarn com.google.protobuf.**
-dontwarn com.google.common.**
-dontwarn java.util.**
-dontwarn javax.xml.**
-dontwarn java.sql.**

-ignorewarnings

-allowaccessmodification
-keep class kotlin.Metadata { *; }
-keepclasseswithmembers public class elide.tool.cli.ElideToolKt {
    public static void main(java.lang.String[]);
}
