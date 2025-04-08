declare module "elide" {
  // Nothing at this time.
}

interface Crypto {}

declare var Crypto: {
  prototype: Crypto
  new (): Crypto
}

declare var crypto: Crypto
