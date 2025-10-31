package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;
import dev.truffle.php.runtime.PhpArray;
import dev.truffle.php.runtime.PhpContext;

import java.io.PrintWriter;

/**
 * Built-in function: print_r
 * Prints human-readable information about a variable.
 */
public final class PrintRBuiltin extends PhpBuiltinRootNode {

    public PrintRBuiltin(PhpLanguage language) {
        super(language, "print_r");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length == 0) {
            return null;
        }

        PrintWriter out = new PrintWriter(PhpContext.get(null).getOutput(), true);
        printValue(out, args[0], 0);
        out.println();

        return true;
    }

    private void printValue(PrintWriter out, Object value, int indent) {
        if (value == null) {
            // null prints nothing in print_r
        } else if (value instanceof PhpArray) {
            PhpArray array = (PhpArray) value;
            out.println("Array");
            out.println(spaces(indent) + "(");
            for (Object key : array.keys()) {
                out.print(spaces(indent + 4) + "[" + key + "] => ");
                Object val = array.get(key);
                if (val instanceof PhpArray) {
                    printValue(out, val, indent + 4);
                } else {
                    out.println(val);
                }
            }
            out.println(spaces(indent) + ")");
        } else {
            out.print(value);
        }
    }

    private String spaces(int count) {
        return " ".repeat(count);
    }
}
