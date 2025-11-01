package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;
import dev.truffle.php.runtime.PhpContext;

/**
 * Built-in PHP function: constant($constant_name)
 * Returns the value of a constant.
 */
public final class ConstantBuiltin extends PhpBuiltinRootNode {

    public ConstantBuiltin(PhpLanguage language) {
        super(language, "constant");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {

        // Require exactly 1 argument
        if (args.length < 1) {
            throw new RuntimeException("constant() expects exactly 1 argument");
        }

        // Get constant name (must be string)
        Object nameArg = args[0];
        if (!(nameArg instanceof String)) {
            throw new RuntimeException("constant() expects parameter 1 to be string");
        }
        String name = (String) nameArg;

        // Get constant value from context
        PhpContext context = PhpContext.get(this);
        Object value = context.getConstant(name);

        if (value == null) {
            throw new RuntimeException("Undefined constant: " + name);
        }

        return value;
    }
}
