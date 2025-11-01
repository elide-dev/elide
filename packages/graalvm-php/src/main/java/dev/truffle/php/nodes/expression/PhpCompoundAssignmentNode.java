package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpReference;

/**
 * Node for compound assignment operators like +=, -=, *=, /=, .=
 *
 * These operators combine an operation with assignment:
 * $x += 5 is equivalent to $x = $x + 5
 */
public final class PhpCompoundAssignmentNode extends PhpExpressionNode {

    public enum CompoundOp {
        ADD_ASSIGN,      // +=
        SUB_ASSIGN,      // -=
        MUL_ASSIGN,      // *=
        DIV_ASSIGN,      // /=
        CONCAT_ASSIGN,   // .=
        MOD_ASSIGN       // %=
    }

    private final String variableName;
    private final int variableSlot;
    private final CompoundOp operation;

    @Child
    private PhpExpressionNode rightNode;

    public PhpCompoundAssignmentNode(String variableName, int variableSlot, CompoundOp operation, PhpExpressionNode rightNode) {
        this.variableName = variableName;
        this.variableSlot = variableSlot;
        this.operation = operation;
        this.rightNode = rightNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        // Read current value
        Object slotValue = frame.getObject(variableSlot);

        // Check if the slot contains a reference
        PhpReference reference = null;
        Object currentValue;
        if (slotValue instanceof PhpReference) {
            reference = (PhpReference) slotValue;
            currentValue = reference.getValue();
        } else {
            currentValue = slotValue;
        }

        // Execute right side
        Object rightValue = rightNode.execute(frame);

        // Perform operation
        Object result = performOperation(currentValue, rightValue);

        // Write back to variable
        if (reference != null) {
            // Update the reference's value
            reference.setValue(result);
        } else {
            // Normal assignment
            frame.setObject(variableSlot, result);
        }

        return result;
    }

    private Object performOperation(Object left, Object right) {
        switch (operation) {
            case ADD_ASSIGN:
                return add(left, right);
            case SUB_ASSIGN:
                return subtract(left, right);
            case MUL_ASSIGN:
                return multiply(left, right);
            case DIV_ASSIGN:
                return divide(left, right);
            case CONCAT_ASSIGN:
                return concat(left, right);
            case MOD_ASSIGN:
                return modulo(left, right);
            default:
                throw new UnsupportedOperationException("Unknown compound operation: " + operation);
        }
    }

    private Object add(Object left, Object right) {
        // Handle numeric addition
        if (left instanceof Long && right instanceof Long) {
            return (Long) left + (Long) right;
        }
        if (left instanceof Long && right instanceof Double) {
            return ((Long) left).doubleValue() + (Double) right;
        }
        if (left instanceof Double && right instanceof Long) {
            return (Double) left + ((Long) right).doubleValue();
        }
        if (left instanceof Double && right instanceof Double) {
            return (Double) left + (Double) right;
        }

        // Convert to numbers if needed
        return toNumber(left) + toNumber(right);
    }

    private Object subtract(Object left, Object right) {
        if (left instanceof Long && right instanceof Long) {
            return (Long) left - (Long) right;
        }
        if (left instanceof Long && right instanceof Double) {
            return ((Long) left).doubleValue() - (Double) right;
        }
        if (left instanceof Double && right instanceof Long) {
            return (Double) left - ((Long) right).doubleValue();
        }
        if (left instanceof Double && right instanceof Double) {
            return (Double) left - (Double) right;
        }

        return toNumber(left) - toNumber(right);
    }

    private Object multiply(Object left, Object right) {
        if (left instanceof Long && right instanceof Long) {
            return (Long) left * (Long) right;
        }
        if (left instanceof Long && right instanceof Double) {
            return ((Long) left).doubleValue() * (Double) right;
        }
        if (left instanceof Double && right instanceof Long) {
            return (Double) left * ((Long) right).doubleValue();
        }
        if (left instanceof Double && right instanceof Double) {
            return (Double) left * (Double) right;
        }

        return toNumber(left) * toNumber(right);
    }

    private Object divide(Object left, Object right) {
        double leftNum = toNumber(left);
        double rightNum = toNumber(right);

        if (rightNum == 0) {
            throw new ArithmeticException("Division by zero");
        }

        return leftNum / rightNum;
    }

    private Object concat(Object left, Object right) {
        String leftStr = left == null ? "" : String.valueOf(left);
        String rightStr = right == null ? "" : String.valueOf(right);
        return leftStr + rightStr;
    }

    private Object modulo(Object left, Object right) {
        long leftNum = (long) toNumber(left);
        long rightNum = (long) toNumber(right);

        if (rightNum == 0) {
            throw new ArithmeticException("Division by zero");
        }

        return leftNum % rightNum;
    }

    private double toNumber(Object value) {
        if (value instanceof Long) {
            return ((Long) value).doubleValue();
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? 1.0 : 0.0;
        }
        if (value == null) {
            return 0.0;
        }
        return 0.0;
    }
}
