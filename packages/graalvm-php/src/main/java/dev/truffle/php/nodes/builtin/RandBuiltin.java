package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;

import java.util.Random;

/**
 * Built-in function: rand
 * Generates a random integer.
 * rand() or rand(min, max)
 */
public final class RandBuiltin extends PhpBuiltinRootNode {

    private static final Random random = new Random();

    public RandBuiltin(PhpLanguage language) {
        super(language, "rand");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length == 0) {
            // Return random int
            return (long) random.nextInt();
        } else if (args.length >= 2) {
            // rand(min, max)
            int min = toInt(args[0]);
            int max = toInt(args[1]);
            if (min > max) {
                int temp = min;
                min = max;
                max = temp;
            }
            return (long) (random.nextInt(max - min + 1) + min);
        }

        return 0L;
    }

    private int toInt(Object obj) {
        if (obj instanceof Long) return ((Long) obj).intValue();
        if (obj instanceof Double) return ((Double) obj).intValue();
        return 0;
    }
}
