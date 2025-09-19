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
package elide.runtime.node

import kotlin.test.Test
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.runtime.node.stream.NodeStreamPromisesModule
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `stream/promises` built-in module. */
@TestCase internal class NodeStreamPromisesTest : NodeModuleConformanceTest<NodeStreamPromisesModule>() {
  override val moduleName: String get() = "stream/promises"
  override fun provide(): NodeStreamPromisesModule = NodeStreamPromisesModule()
  @Inject lateinit var stream: NodeStreamPromisesModule

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("pipeline")
    yield("finished")
  }

  @Test override fun testInjectable() {
    assertNotNull(stream)
  }

  @Test fun `finished resolves on end and cleans listeners`() {
    executeGuest {
      """
      const { finished } = require('node:stream/promises');

      function mkStream() {
        const events = Object.create(null);
        return {
          _events: events,
          readableEnded: false,
          writableFinished: false,
          errored: null,
          once(event, listener) {
            (events[event] || (events[event] = [])).push({ listener, once: true });
            return this;
          },
          off(event, listener) {
            const arr = events[event];
            if (!arr) return this;
            events[event] = arr.filter(e => e.listener !== listener);
            return this;
          },
          emit(event, ...args) {
            const arr = events[event];
            if (!arr) return false;
            for (const e of [...arr]) {
              try { e.listener(...args); } finally {
                if (e.once) this.off(event, e.listener);
              }
            }
            return true;
          },
          listenerCount(event) { return (events[event] ? events[event].length : 0); },
          pipe(dest) { this._dest = dest; return dest; },
        };
      }

      const s = mkStream();
      let resolved = false;
      const p = finished(s);
      test(typeof p?.then === 'function').shouldBeTrue('finished returns a promise');
      p.then(() => {
        resolved = true;
        test(s.listenerCount('end')).isEqualTo(0, 'end listener cleaned');
        test(s.listenerCount('finish')).isEqualTo(0, 'finish listener cleaned');
        test(s.listenerCount('error')).isEqualTo(0, 'error listener cleaned');
      });
      s.emit('end');
      Promise.resolve().then(() => {
        test(resolved).shouldBeTrue('promise resolved after end');
      });
      """
    }.doesNotFail()
  }

  @Test fun `finished resolves on finish and cleans listeners`() {
    executeGuest {
      """
      const { finished } = require('node:stream/promises');

      function mkStream() {
        const events = Object.create(null);
        return {
          _events: events,
          readableEnded: false,
          writableFinished: false,
          errored: null,
          once(event, listener) {
            (events[event] || (events[event] = [])).push({ listener, once: true });
            return this;
          },
          off(event, listener) {
            const arr = events[event];
            if (!arr) return this;
            events[event] = arr.filter(e => e.listener !== listener);
            return this;
          },
          emit(event, ...args) {
            const arr = events[event];
            if (!arr) return false;
            for (const e of [...arr]) {
              try { e.listener(...args); } finally {
                if (e.once) this.off(event, e.listener);
              }
            }
            return true;
          },
          listenerCount(event) { return (events[event] ? events[event].length : 0); },
          pipe(dest) { this._dest = dest; return dest; },
        };
      }

      const s = mkStream();
      let resolved = false;
      const p = finished(s);
      p.then(() => {
        resolved = true;
        test(s.listenerCount('end')).isEqualTo(0);
        test(s.listenerCount('finish')).isEqualTo(0);
        test(s.listenerCount('error')).isEqualTo(0);
      });
      s.emit('finish');
      Promise.resolve().then(() => {
        test(resolved).shouldBeTrue('promise resolved after finish');
      });
      """
    }.doesNotFail()
  }

  @Test fun `finished rejects on error and cleans listeners`() {
    executeGuest {
      """
      const { finished } = require('node:stream/promises');

      function mkStream() {
        const events = Object.create(null);
        return {
          _events: events,
          readableEnded: false,
          writableFinished: false,
          errored: null,
          once(event, listener) {
            (events[event] || (events[event] = [])).push({ listener, once: true });
            return this;
          },
          off(event, listener) {
            const arr = events[event];
            if (!arr) return this;
            events[event] = arr.filter(e => e.listener !== listener);
            return this;
          },
          emit(event, ...args) {
            const arr = events[event];
            if (!arr) return false;
            for (const e of [...arr]) {
              try { e.listener(...args); } finally {
                if (e.once) this.off(event, e.listener);
              }
            }
            return true;
          },
          listenerCount(event) { return (events[event] ? events[event].length : 0); },
          pipe(dest) { this._dest = dest; return dest; },
        };
      }

      const s = mkStream();
      let rejected = false;
      finished(s).then(
        () => { throw new Error('should not resolve'); },
        (err) => {
          rejected = true;
          test(!!err).shouldBeTrue('error provided');
          test(s.listenerCount('end')).isEqualTo(0);
          test(s.listenerCount('finish')).isEqualTo(0);
          test(s.listenerCount('error')).isEqualTo(0);
        }
      );
      s.emit('error', new Error('boom'));
      Promise.resolve().then(() => {
        test(rejected).shouldBeTrue('promise rejected after error');
      });
      """
    }.doesNotFail()
  }

  @Test fun `finished settles immediately if already ended`() {
    executeGuest {
      """
      const { finished } = require('node:stream/promises');

      const s = {
        readableEnded: true,
        writableFinished: false,
        errored: null,
        once() { throw new Error('should not add listeners when already ended'); },
        off() { /* no-op */ },
        listenerCount() { return 0; },
      };
      let resolved = false;
      finished(s).then(() => { resolved = true; });
      Promise.resolve().then(() => {
        test(resolved).shouldBeTrue('resolved immediately');
      });
      """
    }.doesNotFail()
  }

  @Test fun `finished rejects immediately if already errored`() {
    executeGuest {
      """
      const { finished } = require('node:stream/promises');

      const s = {
        readableEnded: false,
        writableFinished: false,
        errored: new Error('pre-error'),
        once() { throw new Error('should not add listeners when already errored'); },
        off() { /* no-op */ },
        listenerCount() { return 0; },
      };
      let rejected = false;
      finished(s).then(
        () => { throw new Error('should not resolve'); },
        (err) => { rejected = true; test(!!err).shouldBeTrue('has error'); }
      );
      Promise.resolve().then(() => {
        test(rejected).shouldBeTrue('rejected immediately');
      });
      """
    }.doesNotFail()
  }

  @Test fun `pipeline resolves on last end and cleans listeners`() {
    executeGuest {
      """
      const { pipeline } = require('node:stream/promises');

      function mkStream() {
        const events = Object.create(null);
        return {
          _events: events,
          readableEnded: false,
          writableFinished: false,
          errored: null,
          once(event, listener) {
            (events[event] || (events[event] = [])).push({ listener, once: true });
            return this;
          },
          off(event, listener) {
            const arr = events[event];
            if (!arr) return this;
            events[event] = arr.filter(e => e.listener !== listener);
            return this;
          },
          emit(event, ...args) {
            const arr = events[event];
            if (!arr) return false;
            for (const e of [...arr]) {
              try { e.listener(...args); } finally {
                if (e.once) this.off(event, e.listener);
              }
            }
            return true;
          },
          listenerCount(event) { return (events[event] ? events[event].length : 0); },
          pipe(dest) { this._dest = dest; return dest; },
        };
      }

      const s1 = mkStream(), s2 = mkStream(), s3 = mkStream();
      let resolved = false;
      const p = pipeline(s1, s2, s3);
      test(typeof p?.then === 'function').shouldBeTrue('pipeline returns a promise');
      // verify pipe chaining occurred
      test(s1._dest === s2).shouldBeTrue('s1 piped to s2');
      test(s2._dest === s3).shouldBeTrue('s2 piped to s3');
      p.then(() => {
        resolved = true;
        [s1, s2, s3].forEach(s => test(s.listenerCount('error')).isEqualTo(0));
        test(s3.listenerCount('end')).isEqualTo(0);
        test(s3.listenerCount('finish')).isEqualTo(0);
      });
      s3.emit('end');
      Promise.resolve().then(() => {
        test(resolved).shouldBeTrue('pipeline resolved on last end');
      });
      """
    }.doesNotFail()
  }

  @Test fun `pipeline rejects on intermediate error and cleans listeners`() {
    executeGuest {
      """
      const { pipeline } = require('node:stream/promises');

      function mkStream() {
        const events = Object.create(null);
        return {
          _events: events,
          readableEnded: false,
          writableFinished: false,
          errored: null,
          once(event, listener) {
            (events[event] || (events[event] = [])).push({ listener, once: true });
            return this;
          },
          off(event, listener) {
            const arr = events[event];
            if (!arr) return this;
            events[event] = arr.filter(e => e.listener !== listener);
            return this;
          },
          emit(event, ...args) {
            const arr = events[event];
            if (!arr) return false;
            for (const e of [...arr]) {
              try { e.listener(...args); } finally {
                if (e.once) this.off(event, e.listener);
              }
            }
            return true;
          },
          listenerCount(event) { return (events[event] ? events[event].length : 0); },
          pipe(dest) { this._dest = dest; return dest; },
        };
      }

      const s1 = mkStream(), s2 = mkStream(), s3 = mkStream();
      let rejected = false;
      pipeline(s1, s2, s3).then(
        () => { throw new Error('should not resolve'); },
        (err) => {
          rejected = true;
          test(!!err).shouldBeTrue('error provided');
          [s1, s2, s3].forEach(s => test(s.listenerCount('error')).isEqualTo(0));
          test(s3.listenerCount('end')).isEqualTo(0);
          test(s3.listenerCount('finish')).isEqualTo(0);
        }
      );
      s2.emit('error', new Error('EPIPE'));
      Promise.resolve().then(() => {
        test(rejected).shouldBeTrue('pipeline rejected on error');
      });
      """
    }.doesNotFail()
  }

  @Test fun `pipeline settles immediately if last already ended`() {
    executeGuest {
      """
      const { pipeline } = require('node:stream/promises');

      function mkStream() {
        const events = Object.create(null);
        return {
          _events: events,
          readableEnded: false,
          writableFinished: false,
          errored: null,
          once(event, listener) {
            (events[event] || (events[event] = [])).push({ listener, once: true });
            return this;
          },
          off(event, listener) {
            const arr = events[event];
            if (!arr) return this;
            events[event] = arr.filter(e => e.listener !== listener);
            return this;
          },
          emit(event, ...args) {
            const arr = events[event];
            if (!arr) return false;
            for (const e of [...arr]) {
              try { e.listener(...args); } finally {
                if (e.once) this.off(event, e.listener);
              }
            }
            return true;
          },
          listenerCount(event) { return (events[event] ? events[event].length : 0); },
          pipe(dest) { this._dest = dest; return dest; },
        };
      }

      const s1 = mkStream(), s2 = mkStream(), s3 = mkStream();
      s3.readableEnded = true;
      let resolved = false;
      pipeline(s1, s2, s3).then(() => { resolved = true; });
      Promise.resolve().then(() => {
        test(resolved).shouldBeTrue('pipeline resolved immediately on last ended');
      });
      """
    }.doesNotFail()
  }

  @Test fun `pipeline rejects immediately if last already errored`() {
    executeGuest {
      """
      const { pipeline } = require('node:stream/promises');

      function mkStream() {
        const events = Object.create(null);
        return {
          _events: events,
          readableEnded: false,
          writableFinished: false,
          errored: null,
          once(event, listener) {
            (events[event] || (events[event] = [])).push({ listener, once: true });
            return this;
          },
          off(event, listener) {
            const arr = events[event];
            if (!arr) return this;
            events[event] = arr.filter(e => e.listener !== listener);
            return this;
          },
          emit(event, ...args) {
            const arr = events[event];
            if (!arr) return false;
            for (const e of [...arr]) {
              try { e.listener(...args); } finally {
                if (e.once) this.off(event, e.listener);
              }
            }
            return true;
          },
          listenerCount(event) { return (events[event] ? events[event].length : 0); },
          pipe(dest) { this._dest = dest; return dest; },
        };
      }

      const s1 = mkStream(), s2 = mkStream(), s3 = mkStream();
      s3.errored = new Error('pre-error');
      let rejected = false;
      pipeline(s1, s2, s3).then(
        () => { throw new Error('should not resolve'); },
        (err) => { rejected = true; test(!!err).shouldBeTrue('has error'); }
      );
      Promise.resolve().then(() => {
        test(rejected).shouldBeTrue('pipeline rejected immediately on last errored');
      });
      """
    }.doesNotFail()
  }

  @Test fun `finished settles immediately if already finished (writable)`() {
    executeGuest {
      """
      const { finished } = require('node:stream/promises');
      const s = {
        readableEnded: false,
        writableFinished: true,
        errored: null,
        once() { throw new Error('should not add listeners when already finished'); },
        off() { /* no-op */ },
        listenerCount() { return 0; },
      };
      let resolved = false;
      finished(s).then(() => { resolved = true; });
      Promise.resolve().then(() => {
        test(resolved).shouldBeTrue('resolved immediately');
      });
      """
    }.doesNotFail()
  }

  @Test fun `finished rejects immediately if errored even when ended`() {
    executeGuest {
      """
      const { finished } = require('node:stream/promises');
      const s = {
        readableEnded: true,
        writableFinished: false,
        errored: new Error('pre-error'),
        once() { throw new Error('should not add listeners when already errored'); },
        off() { /* no-op */ },
        listenerCount() { return 0; },
      };
      let rejected = false;
      finished(s).then(
        () => { throw new Error('should not resolve'); },
        (err) => { rejected = true; test(!!err).shouldBeTrue('has error'); },
      );
      Promise.resolve().then(() => {
        test(rejected).shouldBeTrue('rejected immediately');
      });
      """
    }.doesNotFail()
  }

  @Test fun `pipeline resolves with no streams`() {
    executeGuest {
      """
      const { pipeline } = require('node:stream/promises');
      let resolved = false;
      const p = pipeline();
      test(typeof p?.then === 'function').shouldBeTrue('pipeline returns a promise');
      p.then(() => { resolved = true; });
      Promise.resolve().then(() => {
        test(resolved).shouldBeTrue('resolved immediately for empty pipeline');
      });
      """
    }.doesNotFail()
  }

  @Test fun `pipeline with single stream resolves on end and cleans listeners`() {
    executeGuest {
      """
      const { pipeline } = require('node:stream/promises');

      function mkStream() {
        const events = Object.create(null);
        return {
          _events: events,
          readableEnded: false,
          writableFinished: false,
          errored: null,
          once(event, listener) {
            (events[event] || (events[event] = [])).push({ listener, once: true });
            return this;
          },
          off(event, listener) {
            const arr = events[event];
            if (!arr) return this;
            events[event] = arr.filter(e => e.listener !== listener);
            return this;
          },
          emit(event, ...args) {
            const arr = events[event];
            if (!arr) return false;
            for (const e of [...arr]) {
              try { e.listener(...args); } finally {
                if (e.once) this.off(event, e.listener);
              }
            }
            return true;
          },
          listenerCount(event) { return (events[event] ? events[event].length : 0); },
          pipe(dest) { this._dest = dest; return dest; },
        };
      }

      const s = mkStream();
      let resolved = false;
      const p = pipeline(s);
      test(typeof p?.then === 'function').shouldBeTrue('pipeline returns a promise');
      p.then(() => {
        resolved = true;
        test(s.listenerCount('error')).isEqualTo(0);
        test(s.listenerCount('end')).isEqualTo(0);
        test(s.listenerCount('finish')).isEqualTo(0);
      });
      s.emit('end');
      Promise.resolve().then(() => {
        test(resolved).shouldBeTrue('pipeline resolved on single stream end');
      });
      """
    }.doesNotFail()
  }

  @Test fun `pipeline rejects if a pipe call throws and does not attach listeners`() {
    executeGuest {
      """
      const { pipeline } = require('node:stream/promises');

      function mkStream() {
        const events = Object.create(null);
        return {
          _events: events,
          readableEnded: false,
          writableFinished: false,
          errored: null,
          once(event, listener) {
            (events[event] || (events[event] = [])).push({ listener, once: true });
            return this;
          },
          off(event, listener) {
            const arr = events[event];
            if (!arr) return this;
            events[event] = arr.filter(e => e.listener !== listener);
            return this;
          },
          emit(event, ...args) {
            const arr = events[event];
            if (!arr) return false;
            for (const e of [...arr]) {
              try { e.listener(...args); } finally {
                if (e.once) this.off(event, e.listener);
              }
            }
            return true;
          },
          listenerCount(event) { return (events[event] ? events[event].length : 0); },
          pipe(dest) { this._dest = dest; return dest; },
        };
      }

      const s1 = mkStream();
      const s2 = mkStream();
      // Force pipe() to throw on the first stream
      s1.pipe = () => { throw new Error('fail'); };

      let rejected = false;
      const p = pipeline(s1, s2).then(
        () => { throw new Error('should not resolve'); },
        () => { rejected = true; },
      );

      Promise.resolve().then(() => {
        test(rejected).shouldBeTrue('pipeline rejected when pipe throws');
        // No listeners should have been attached since rejection happened during piping
        test(s1.listenerCount('error')).isEqualTo(0);
        test(s2.listenerCount('error')).isEqualTo(0);
        test(s2.listenerCount('end')).isEqualTo(0);
        test(s2.listenerCount('finish')).isEqualTo(0);
      });
      """
    }.doesNotFail()
  }
}
