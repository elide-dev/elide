package dev.truffle.php.runtime;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the global scope for PHP execution.
 *
 * This class maintains:
 * - A shared FrameDescriptor for all global variables
 * - A map of variable names to frame slots
 * - The materialized global frame that persists across file inclusions
 *
 * Key design:
 * - All top-level PHP code shares the same global frame descriptor
 * - Variables declared at the top level go into the global frame
 * - Included files can access and modify variables from the parent scope
 * - Functions and methods have their own local frames but can access globals via the global keyword
 */
public final class PhpGlobalScope {

    private final FrameDescriptor.Builder frameBuilder;
    private final Map<String, Integer> globalVariables;
    private MaterializedFrame globalFrame;

    public PhpGlobalScope() {
        this.frameBuilder = FrameDescriptor.newBuilder();
        this.globalVariables = new HashMap<>();
        this.globalFrame = null;
    }

    /**
     * Get or create a slot for a global variable.
     * This is called during parsing to allocate frame slots for top-level variables.
     *
     * @param varName the variable name (without the $ prefix)
     * @return the frame slot index
     */
    public int getOrCreateGlobalSlot(String varName) {
        return globalVariables.computeIfAbsent(varName, n -> {
            return frameBuilder.addSlot(FrameSlotKind.Illegal, n, null);
        });
    }

    /**
     * Check if a variable is already defined in global scope.
     *
     * @param varName the variable name (without the $ prefix)
     * @return true if the variable exists in global scope
     */
    public boolean hasGlobalVariable(String varName) {
        return globalVariables.containsKey(varName);
    }

    /**
     * Get the frame slot for an existing global variable.
     *
     * @param varName the variable name (without the $ prefix)
     * @return the frame slot index, or null if not found
     */
    public Integer getGlobalSlot(String varName) {
        return globalVariables.get(varName);
    }

    /**
     * Build the frame descriptor.
     * This should be called once after parsing the main script to finalize the frame descriptor.
     *
     * @return the built FrameDescriptor
     */
    public FrameDescriptor build() {
        return frameBuilder.build();
    }

    /**
     * Get the global frame.
     * The global frame is created when the first script executes and is reused for all includes.
     *
     * @return the materialized global frame
     */
    public MaterializedFrame getGlobalFrame() {
        return globalFrame;
    }

    /**
     * Set the global frame.
     * This is called when the first script starts executing to establish the global frame.
     *
     * @param frame the frame to materialize and store as the global frame
     */
    public void setGlobalFrame(VirtualFrame frame) {
        if (this.globalFrame == null) {
            this.globalFrame = frame.materialize();
        }
    }

    /**
     * Get the number of global variables defined.
     *
     * @return the count of global variables
     */
    public int getGlobalVariableCount() {
        return globalVariables.size();
    }

    /**
     * Get all global variable names.
     * This is useful for debugging and introspection.
     *
     * @return a map of variable names to their frame slots
     */
    public Map<String, Integer> getGlobalVariables() {
        return new HashMap<>(globalVariables);
    }
}
