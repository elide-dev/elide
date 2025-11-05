/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */
package elide.lang.php.nodes;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import elide.lang.php.nodes.types.PhpTypes;

/**
 * Base class for all PHP AST nodes.
 *
 * <p>All executable nodes in PHP extend this class and implement the execute method.
 */
@TypeSystemReference(PhpTypes.class)
public abstract class PhpNode extends Node {

  /** Execute this node and return the result. */
  public abstract Object execute(VirtualFrame frame);

  /**
   * Execute this node expecting a long result. Throws UnexpectedResultException if the result is
   * not a long.
   */
  public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
    Object result = execute(frame);
    if (result instanceof Long) {
      return (Long) result;
    }
    throw new UnexpectedResultException(result);
  }

  /**
   * Execute this node expecting a double result. Throws UnexpectedResultException if the result is
   * not a double.
   */
  public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
    Object result = execute(frame);
    if (result instanceof Double) {
      return (Double) result;
    }
    throw new UnexpectedResultException(result);
  }

  /**
   * Execute this node expecting a boolean result. Throws UnexpectedResultException if the result is
   * not a boolean.
   */
  public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
    Object result = execute(frame);
    if (result instanceof Boolean) {
      return (Boolean) result;
    }
    throw new UnexpectedResultException(result);
  }

  /**
   * Execute this node expecting a String result. Throws UnexpectedResultException if the result is
   * not a String.
   */
  public String executeString(VirtualFrame frame) throws UnexpectedResultException {
    Object result = execute(frame);
    if (result instanceof String) {
      return (String) result;
    }
    throw new UnexpectedResultException(result);
  }
}
