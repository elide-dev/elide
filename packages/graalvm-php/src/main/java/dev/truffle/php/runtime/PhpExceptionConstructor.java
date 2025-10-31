package dev.truffle.php.runtime;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpMethodRootNode;
import dev.truffle.php.nodes.PhpStatementNode;

/**
 * Factory for creating the built-in Exception class constructor.
 * Constructor signature: __construct($message = "", $code = 0)
 */
public final class PhpExceptionConstructor {

    public static PhpMethodRootNode create(PhpLanguage language) {
        FrameDescriptor frameDescriptor = buildFrameDescriptor();
        return new PhpMethodRootNode(
            language,
            frameDescriptor,
            "Exception",
            "__construct",
            new String[]{"message", "code"},
            new int[]{1, 2}, // param slots
            0, // $this slot
            new ExceptionConstructorBody(),
            -1 // no variadic parameter
        );
    }

    private static FrameDescriptor buildFrameDescriptor() {
        FrameDescriptor.Builder builder = FrameDescriptor.newBuilder();
        builder.addSlot(FrameSlotKind.Illegal, "this", null); // slot 0
        builder.addSlot(FrameSlotKind.Illegal, "message", null); // slot 1
        builder.addSlot(FrameSlotKind.Illegal, "code", null); // slot 2
        return builder.build();
    }

    private static final class ExceptionConstructorBody extends PhpStatementNode {
        @Override
        public void executeVoid(VirtualFrame frame) {
            // Arguments: [0] = $this (PhpObject), [1] = $message (optional), [2] = $code (optional)
            Object[] arguments = frame.getArguments();

            if (arguments.length < 1) {
                throw new RuntimeException("Exception constructor requires $this parameter");
            }

            PhpObject thisObject = (PhpObject) arguments[0];

            // Get message parameter (default to empty string)
            String message = "";
            if (arguments.length > 1 && arguments[1] != null) {
                message = String.valueOf(arguments[1]);
            }

            // Get code parameter (default to 0)
            long code = 0L;
            if (arguments.length > 2 && arguments[2] != null) {
                if (arguments[2] instanceof Long) {
                    code = (Long) arguments[2];
                } else if (arguments[2] instanceof Integer) {
                    code = ((Integer) arguments[2]).longValue();
                } else if (arguments[2] instanceof Double) {
                    code = ((Double) arguments[2]).longValue();
                }
            }

            // Set properties on the exception object
            thisObject.writePropertyInternal("message", message);
            thisObject.writePropertyInternal("code", code);
        }
    }
}
