
export const Runtime = {
    BUN: 'bun',
    DENO: 'deno',
    ELIDE: 'elide',
    NODE: 'node',
}

export const BuiltinImportPrefix = {
    [`${Runtime.BUN}`]: 'bun:',
    [`${Runtime.DENO}`]: 'npm:',
    [`${Runtime.ELIDE}`]: 'elide:',
    [`${Runtime.NODE}`]: 'node:',
}
