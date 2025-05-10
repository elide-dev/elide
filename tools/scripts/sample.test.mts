/// <reference path="../../packages/types/index.d.ts" />
import { describe, test, expect } from "elide:test"
import * as sample from "./sample.mts"

console.log("running sample tests")

test("basic test", () => {
  expect(1).toBe(1)
  expect(true).toBeTrue()
  expect(false).not.toBeTrue()
})

describe("sample mod", () => {
  test('message is "Hello"', () => {
    expect(sample.message).toBe("Hello")
  })
  test("render message works", () => {
    const rendered = sample.render()
    expect(rendered).toBe("Hello, World!")
  })
})

describe("sample suite", () => {
  test("sample test", () => {
    expect("hello").toBe("hello")
  })
})
