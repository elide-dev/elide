FROM us-docker.pkg.dev/elide-fw/tools/builder:latest AS graalvm
WORKDIR /home/app
COPY layers/libs /home/app/libs
COPY layers/classes /home/app/classes
COPY layers/resources /home/app/resources
COPY layers/application.jar /home/app/application.jar
RUN mkdir /home/app/config-dirs
COPY config-dirs/generateResourcesConfigFile /home/app/config-dirs/generateResourcesConfigFile
COPY config-dirs/1.7.10 /home/app/config-dirs/1.7.10
COPY config-dirs/1.2.11 /home/app/config-dirs/1.2.11
RUN native-image -cp /home/app/libs/*.jar:/home/app/resources:/home/app/application.jar --no-fallback -H:Name=elide -Dpicocli.ansi="tty" -J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.configure=ALL-UNNAMED -J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.jdk=ALL-UNNAMED -J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.jni=ALL-UNNAMED -J--add-exports=org.graalvm.sdk/org.graalvm.nativeimage.impl=ALL-UNNAMED -H:ConfigurationFileDirectories=/home/app/config-dirs/generateResourcesConfigFile,/home/app/config-dirs/1.7.10,/home/app/config-dirs/1.2.11 -H:Class=elide.tool.cli.ElideTool --language:js --language:regex --enable-all-security-services -Dpolyglot.image-build-time.PreinitializeContexts=js --language:js --gc=serial --no-fallback --enable-http --enable-https --enable-all-security-services --install-exit-handlers --initialize-at-build-time=org.slf4j.LoggerFactory --initialize-at-build-time=org.slf4j.simple.SimpleLogger --initialize-at-build-time=org.slf4j.impl.StaticLoggerBinder -Duser.country=US -Duser.language=en -H:IncludeLocales=en --libc=glibc -H:+StaticExecutableWithDynamicLibC
FROM us-docker.pkg.dev/elide-fw/tools/runtime/native@sha256:61a383bbfa2a3d5fa7ffcd4254b60fdef251db120e4705ee715c43ebae52eb04
COPY --from=graalvm /home/app/elide /app/elide
ENTRYPOINT ["/app/elide"]
