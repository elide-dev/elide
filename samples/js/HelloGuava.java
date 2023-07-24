package samples.js;

import com.google.common.collect.ImmutableList;
import jsinterop.annotations.JsType;

/** A simple example for using Guava in J2CL. */
@JsType
public class HelloGuava {
    public static String getHelloWorld() {
        ImmutableList<String> list = ImmutableList.of("Hello", "World");
        return list.toString();
    }
}
