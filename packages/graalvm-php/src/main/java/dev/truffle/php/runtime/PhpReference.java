package dev.truffle.php.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/**
 * Represents a PHP reference (by-reference variable).
 * This is a mutable box that allows multiple variables to share the same value.
 * Used for implementing by-reference capture in closures and by-reference parameters.
 */
@ExportLibrary(InteropLibrary.class)
public final class PhpReference implements TruffleObject {

    private Object value;

    public PhpReference(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @ExportMessage
    boolean hasMembers() {
        return true;
    }

    @ExportMessage
    Object getMembers(@SuppressWarnings("unused") boolean includeInternal) {
        return new String[] { "value" };
    }

    @ExportMessage
    boolean isMemberReadable(String member) {
        return "value".equals(member);
    }

    @ExportMessage
    Object readMember(String member) {
        if ("value".equals(member)) {
            return value;
        }
        throw new UnsupportedOperationException("Unknown member: " + member);
    }

    @ExportMessage
    boolean isMemberModifiable(String member) {
        return "value".equals(member);
    }

    @ExportMessage
    boolean isMemberInsertable(@SuppressWarnings("unused") String member) {
        return false;
    }

    @ExportMessage
    void writeMember(String member, Object newValue) {
        if ("value".equals(member)) {
            this.value = newValue;
        } else {
            throw new UnsupportedOperationException("Unknown member: " + member);
        }
    }

    @Override
    public String toString() {
        return "PhpReference(" + value + ")";
    }
}
