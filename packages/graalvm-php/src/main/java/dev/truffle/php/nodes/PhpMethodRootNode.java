package dev.truffle.php.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.statement.PhpReturnNode;
import dev.truffle.php.runtime.PhpObject;

/**
 * Root node for PHP method execution.
 * Methods are similar to functions but have access to $this.
 */
public final class PhpMethodRootNode extends RootNode {

    private final String className;
    private final String methodName;
    private final String[] parameterNames;
    private final int[] parameterSlots;
    private final int thisSlot; // Slot for $this variable

    @Child
    private PhpStatementNode body;

    public PhpMethodRootNode(
        PhpLanguage language,
        FrameDescriptor frameDescriptor,
        String className,
        String methodName,
        String[] parameterNames,
        int[] parameterSlots,
        int thisSlot,
        PhpStatementNode body
    ) {
        super(language, frameDescriptor);
        this.className = className;
        this.methodName = methodName;
        this.parameterNames = parameterNames;
        this.parameterSlots = parameterSlots;
        this.thisSlot = thisSlot;
        this.body = body;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        // Arguments: [0] = $this, [1..n] = method parameters
        Object[] arguments = frame.getArguments();

        // First argument is $this
        if (arguments.length > 0 && arguments[0] instanceof PhpObject) {
            frame.setObject(thisSlot, arguments[0]);
        }

        // Initialize method parameters from remaining arguments
        for (int i = 0; i < parameterSlots.length && i + 1 < arguments.length; i++) {
            frame.setObject(parameterSlots[i], arguments[i + 1]);
        }

        try {
            body.executeVoid(frame);
            return null; // Methods without explicit return return null
        } catch (PhpReturnNode.PhpReturnException e) {
            return e.getResult();
        }
    }

    @Override
    public String getName() {
        return className + "::" + methodName;
    }

    public int getThisSlot() {
        return thisSlot;
    }
}
