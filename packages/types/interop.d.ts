// Python imports for TypeScript.
declare module "*.py" {
  const mod: any;
  export default mod;
}

// JSON imports for TypeScript, which surface an object as the default export.
declare module "*.json" {
  const value: any;
  export default value;
}
