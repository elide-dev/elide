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
package elide.lang.php.nodes.builtin;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import elide.lang.php.PhpLanguage;
import elide.lang.php.nodes.PhpBuiltinRootNode;
import java.util.Random;

/** Built-in function: rand Generates a random integer. rand() or rand(min, max) */
public final class RandBuiltin extends PhpBuiltinRootNode {

  private static volatile Random random;

  private static Random getRandom() {
    if (random == null) {
      synchronized (RandBuiltin.class) {
        if (random == null) {
          random = new Random();
        }
      }
    }
    return random;
  }

  public RandBuiltin(PhpLanguage language) {
    super(language, "rand");
  }

  @Override
  @TruffleBoundary
  protected Object executeBuiltin(Object[] args) {
    Random rnd = getRandom();
    if (args.length == 0) {
      // Return random int
      return (long) rnd.nextInt();
    } else if (args.length >= 2) {
      // rand(min, max)
      int min = toInt(args[0]);
      int max = toInt(args[1]);
      if (min > max) {
        int temp = min;
        min = max;
        max = temp;
      }
      return (long) (rnd.nextInt(max - min + 1) + min);
    }

    return 0L;
  }

  private int toInt(Object obj) {
    if (obj instanceof Long) return ((Long) obj).intValue();
    if (obj instanceof Double) return ((Double) obj).intValue();
    return 0;
  }
}
