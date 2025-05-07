/**
 * Elide Testing built-in
 *
 * Modeled after Jest's test API
 *
 * @example
 * ```ts
 * import { test } from 'elide:sqltest';
 *
 * test('example', () => { ... })
 * ```
 */
declare module "elide:test" {
  type SuiteFn = () => void | Promise<void>;
  type TestFn = () => void | Promise<void>;
  type ExpectValue = any;
  interface Expect {
    toBe(value: ExpectValue);
    toBeTrue();
  }

  function suite(name?: string, fn?: SuiteFn): void;
  function describe(name?: string, fn?: SuiteFn): void;
  function test(name?: string, fn?: TestFn): void;
  function expect(value: ExpectValue): Expect;
}
