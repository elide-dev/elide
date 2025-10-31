package dev.truffle.php.nodes;

import dev.truffle.php.nodes.expression.*;

/**
 * Factory class for creating PHP nodes.
 * This provides a bridge between the parser and the node classes.
 * Uses static factory methods on the node classes themselves.
 */
public final class PhpNodeFactory {

    private PhpNodeFactory() {
        // Utility class
    }

    public static PhpAddNode createAdd(PhpExpressionNode left, PhpExpressionNode right) {
        return PhpAddNode.create(left, right);
    }

    public static PhpSubNode createSub(PhpExpressionNode left, PhpExpressionNode right) {
        return PhpSubNode.create(left, right);
    }

    public static PhpMulNode createMul(PhpExpressionNode left, PhpExpressionNode right) {
        return PhpMulNode.create(left, right);
    }

    public static PhpDivNode createDiv(PhpExpressionNode left, PhpExpressionNode right) {
        return PhpDivNode.create(left, right);
    }

    public static PhpConcatNode createConcat(PhpExpressionNode left, PhpExpressionNode right) {
        return PhpConcatNode.create(left, right);
    }

    public static PhpEqualNode createEqual(PhpExpressionNode left, PhpExpressionNode right) {
        return PhpEqualNode.create(left, right);
    }

    public static PhpLessThanNode createLessThan(PhpExpressionNode left, PhpExpressionNode right) {
        return PhpLessThanNode.create(left, right);
    }

    public static PhpGreaterThanNode createGreaterThan(PhpExpressionNode left, PhpExpressionNode right) {
        return PhpGreaterThanNode.create(left, right);
    }

    public static PhpLessOrEqualNode createLessOrEqual(PhpExpressionNode left, PhpExpressionNode right) {
        return PhpLessOrEqualNode.create(left, right);
    }

    public static PhpGreaterOrEqualNode createGreaterOrEqual(PhpExpressionNode left, PhpExpressionNode right) {
        return PhpGreaterOrEqualNode.create(left, right);
    }

    public static PhpNotEqualNode createNotEqual(PhpExpressionNode left, PhpExpressionNode right) {
        return PhpNotEqualNode.create(left, right);
    }

    public static PhpLogicalAndNode createLogicalAnd(PhpExpressionNode left, PhpExpressionNode right) {
        return PhpLogicalAndNode.create(left, right);
    }

    public static PhpLogicalOrNode createLogicalOr(PhpExpressionNode left, PhpExpressionNode right) {
        return PhpLogicalOrNode.create(left, right);
    }

    public static PhpLogicalNotNode createLogicalNot(PhpExpressionNode operand) {
        return PhpLogicalNotNode.create(operand);
    }

    public static PhpReadVariableNode createReadVariable(int slot) {
        return PhpReadVariableNode.create(slot);
    }

    public static PhpWriteVariableNode createWriteVariable(PhpExpressionNode value, int slot) {
        return PhpWriteVariableNode.create(value, slot);
    }
}
