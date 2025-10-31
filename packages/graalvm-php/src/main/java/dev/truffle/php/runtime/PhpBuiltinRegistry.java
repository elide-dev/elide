package dev.truffle.php.runtime;

import com.oracle.truffle.api.CallTarget;
import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;
import dev.truffle.php.nodes.builtin.*;

/**
 * Registry for built-in PHP functions.
 * This class provides static methods to initialize built-in functions for a context.
 */
public final class PhpBuiltinRegistry {

    private PhpBuiltinRegistry() {
        // Private constructor - utility class
    }

    /**
     * Initialize built-in functions for a given context.
     * This creates new CallTargets for each context to avoid sharing AST nodes.
     */
    public static void initializeBuiltins(PhpContext context, PhpLanguage language) {
        // String functions
        register(context, new StrlenBuiltin(language));
        register(context, new SubstrBuiltin(language));
        register(context, new StrtolowerBuiltin(language));
        register(context, new StrtoupperBuiltin(language));
        register(context, new TrimBuiltin(language));
        register(context, new StrReplaceBuiltin(language));
        register(context, new ExplodeBuiltin(language));
        register(context, new ImplodeBuiltin(language));

        // Array functions
        register(context, new CountBuiltin(language));
        register(context, new ArrayPushBuiltin(language));
        register(context, new ArrayPopBuiltin(language));
        register(context, new ArrayMergeBuiltin(language));
        register(context, new InArrayBuiltin(language));
        register(context, new ArrayKeysBuiltin(language));
        register(context, new ArrayValuesBuiltin(language));
        register(context, new ArraySliceBuiltin(language));
        register(context, new ArrayReverseBuiltin(language));
        register(context, new ArraySearchBuiltin(language));
        register(context, new ArrayKeyExistsBuiltin(language));

        // Type functions
        register(context, new IsArrayBuiltin(language));
        register(context, new IsStringBuiltin(language));
        register(context, new IsIntBuiltin(language));
        register(context, new IsNullBuiltin(language));
        register(context, new GettypeBuiltin(language));

        // Math functions
        register(context, new AbsBuiltin(language));
        register(context, new MaxBuiltin(language));
        register(context, new MinBuiltin(language));
        register(context, new RandBuiltin(language));

        // Output functions
        register(context, new VarDumpBuiltin(language));
        register(context, new PrintRBuiltin(language));
    }

    private static void register(PhpContext context, PhpBuiltinRootNode builtin) {
        context.registerBuiltin(builtin.getFunctionName(), builtin.getCallTarget());
    }
}
