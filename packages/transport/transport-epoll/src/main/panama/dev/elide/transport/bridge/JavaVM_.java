// Generated by jextract

package dev.elide.transport.bridge;

import java.lang.invoke.*;
import java.lang.foreign.*;
import java.nio.ByteOrder;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static java.lang.foreign.ValueLayout.*;
import static java.lang.foreign.MemoryLayout.PathElement.*;

/**
 * {@snippet lang=c :
 * struct JavaVM_ {
 *     const struct JNIInvokeInterface_ *functions;
 * }
 * }
 */
public class JavaVM_ {

    JavaVM_() {
        // Should not be called directly
    }

    private static final GroupLayout $LAYOUT = MemoryLayout.structLayout(
        netty_unix_socket_h.C_POINTER.withName("functions")
    ).withName("JavaVM_");

    /**
     * The layout of this struct
     */
    public static final GroupLayout layout() {
        return $LAYOUT;
    }

    private static final AddressLayout functions$LAYOUT = (AddressLayout)$LAYOUT.select(groupElement("functions"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * const struct JNIInvokeInterface_ *functions
     * }
     */
    public static final AddressLayout functions$layout() {
        return functions$LAYOUT;
    }

    private static final long functions$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * const struct JNIInvokeInterface_ *functions
     * }
     */
    public static final long functions$offset() {
        return functions$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * const struct JNIInvokeInterface_ *functions
     * }
     */
    public static MemorySegment functions(MemorySegment struct) {
        return struct.get(functions$LAYOUT, functions$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * const struct JNIInvokeInterface_ *functions
     * }
     */
    public static void functions(MemorySegment struct, MemorySegment fieldValue) {
        struct.set(functions$LAYOUT, functions$OFFSET, fieldValue);
    }

    /**
     * Obtains a slice of {@code arrayParam} which selects the array element at {@code index}.
     * The returned segment has address {@code arrayParam.address() + index * layout().byteSize()}
     */
    public static MemorySegment asSlice(MemorySegment array, long index) {
        return array.asSlice(layout().byteSize() * index);
    }

    /**
     * The size (in bytes) of this struct
     */
    public static long sizeof() { return layout().byteSize(); }

    /**
     * Allocate a segment of size {@code layout().byteSize()} using {@code allocator}
     */
    public static MemorySegment allocate(SegmentAllocator allocator) {
        return allocator.allocate(layout());
    }

    /**
     * Allocate an array of size {@code elementCount} using {@code allocator}.
     * The returned segment has size {@code elementCount * layout().byteSize()}.
     */
    public static MemorySegment allocateArray(long elementCount, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(elementCount, layout()));
    }

    /**
     * Reinterprets {@code addr} using target {@code arena} and {@code cleanupAction} (if any).
     * The returned segment has size {@code layout().byteSize()}
     */
    public static MemorySegment reinterpret(MemorySegment addr, Arena arena, Consumer<MemorySegment> cleanup) {
        return reinterpret(addr, 1, arena, cleanup);
    }

    /**
     * Reinterprets {@code addr} using target {@code arena} and {@code cleanupAction} (if any).
     * The returned segment has size {@code elementCount * layout().byteSize()}
     */
    public static MemorySegment reinterpret(MemorySegment addr, long elementCount, Arena arena, Consumer<MemorySegment> cleanup) {
        return addr.reinterpret(layout().byteSize() * elementCount, arena, cleanup);
    }
}
