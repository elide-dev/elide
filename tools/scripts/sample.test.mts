/// <reference path="../../packages/types/index.d.ts" />
import { describe, test, expect } from "elide:test";

console.log('running sample tests')

test('basic test', () => {
  expect(1).toBe(1);
  expect(true).toBeTrue();
})

describe('sample suite', () => {
  test('sample test', () => {
    expect("hello").toBe("hello");
  })
})
