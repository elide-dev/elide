package dev.truffle.php.nodes.statement;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.nodes.PhpNodeFactory;
import dev.truffle.php.nodes.PhpStatementNode;
import dev.truffle.php.runtime.PhpArray;
import dev.truffle.php.runtime.PhpBreakException;
import dev.truffle.php.runtime.PhpContinueException;

import java.util.List;

/**
 * Node for foreach loops in PHP.
 * Syntax:
 *   foreach ($array as $value) { body }
 *   foreach ($array as $key => $value) { body }
 */
public final class PhpForeachNode extends PhpStatementNode {

    @Child
    private LoopNode loopNode;

    public PhpForeachNode(PhpExpressionNode arrayExpr, int valueSlot, Integer keySlot, PhpStatementNode body) {
        this.loopNode = Truffle.getRuntime().createLoopNode(
            new PhpForeachRepeatingNode(arrayExpr, valueSlot, keySlot, body)
        );
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        loopNode.execute(frame);
    }

    private static final class PhpForeachRepeatingNode extends Node implements RepeatingNode {

        @Child
        private PhpExpressionNode arrayExpr;

        private final int valueSlot;
        private final Integer keySlot; // null if no key variable

        @Child
        private PhpStatementNode body;

        private List<Object> keys;
        private int currentIndex;
        private PhpArray array;

        PhpForeachRepeatingNode(PhpExpressionNode arrayExpr, int valueSlot, Integer keySlot, PhpStatementNode body) {
            this.arrayExpr = arrayExpr;
            this.valueSlot = valueSlot;
            this.keySlot = keySlot;
            this.body = body;
            this.currentIndex = 0;
        }

        @Override
        public boolean executeRepeating(VirtualFrame frame) {
            // First iteration: evaluate array and get keys
            if (keys == null) {
                Object arrayObj = arrayExpr.execute(frame);
                if (!(arrayObj instanceof PhpArray)) {
                    return false; // Not an array, exit loop
                }
                array = (PhpArray) arrayObj;
                keys = array.keys();
                currentIndex = 0;
            }

            // Check if we're done iterating
            if (currentIndex >= keys.size()) {
                return false;
            }

            // Get current key and value
            Object key = keys.get(currentIndex);
            Object value = array.get(key);

            // Set key variable if needed
            if (keySlot != null) {
                frame.setObject(keySlot, key);
            }

            // Set value variable
            frame.setObject(valueSlot, value);

            // Execute body
            try {
                body.executeVoid(frame);
            } catch (PhpContinueException e) {
                // Continue to next iteration
                currentIndex++;
                return true;
            } catch (PhpBreakException e) {
                // Exit loop
                return false;
            }

            // Move to next element
            currentIndex++;
            return true;
        }
    }
}
