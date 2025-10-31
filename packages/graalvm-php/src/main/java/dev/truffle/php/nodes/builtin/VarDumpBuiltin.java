package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;
import dev.truffle.php.runtime.PhpArray;
import dev.truffle.php.runtime.PhpContext;

import java.io.PrintWriter;

/**
 * Built-in function: var_dump
 * Dumps information about a variable.
 */
public final class VarDumpBuiltin extends PhpBuiltinRootNode {

    public VarDumpBuiltin(PhpLanguage language) {
        super(language, "var_dump");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        PrintWriter out = new PrintWriter(PhpContext.get(null).getOutput(), true);

        for (Object arg : args) {
            dumpValue(out, arg, 0);
        }

        return null;
    }

    private void dumpValue(PrintWriter out, Object value, int indent) {
        if (value == null) {
            out.println("NULL");
        } else if (value instanceof Boolean) {
            out.println("bool(" + value + ")");
        } else if (value instanceof Long) {
            out.println("int(" + value + ")");
        } else if (value instanceof Double) {
            out.println("float(" + value + ")");
        } else if (value instanceof String) {
            String str = (String) value;
            out.println("string(" + str.length() + ") \"" + str + "\"");
        } else if (value instanceof PhpArray) {
            PhpArray array = (PhpArray) value;
            out.println("array(" + array.size() + ") {");
            for (Object key : array.keys()) {
                out.print("  [" + key + "]=>\n  ");
                dumpValue(out, array.get(key), indent + 2);
            }
            out.println("}");
        } else {
            out.println(value.getClass().getSimpleName() + "(" + value + ")");
        }
    }
}
