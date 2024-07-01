import { readFileSync } from "node:fs"
import { join } from "node:path"
import * as tsvfs from "@typescript/vfs"
import ts from "typescript"

const getLib = name => {
  const lib = dirname(require.resolve("typescript"))
  return readFileSync(join(lib, name), "utf8")
}

const addLib = (name, map) => {
  map.set(`/${name}`, getLib(name))
}

const createDefaultMap2015 = () => {
  const fsMap = new Map()
  addLib("lib.es2015.d.ts", fsMap)
  addLib("lib.es2015.collection.d.ts", fsMap)
  addLib("lib.es2015.core.d.ts", fsMap)
  addLib("lib.es2015.generator.d.ts", fsMap)
  addLib("lib.es2015.iterable.d.ts", fsMap)
  addLib("lib.es2015.promise.d.ts", fsMap)
  addLib("lib.es2015.proxy.d.ts", fsMap)
  addLib("lib.es2015.reflect.d.ts", fsMap)
  addLib("lib.es2015.symbol.d.ts", fsMap)
  addLib("lib.es2015.symbol.wellknown.d.ts", fsMap)
  addLib("lib.es5.d.ts", fsMap)
  return fsMap
}

function compile(fsMap, compilerOptions) {
  const baseMap = createDefaultMap2015()
  for (const key of fsMap.keys) {
    baseMap.set(key, fsMap.get(key))
  }

  const system = tsvfs.createSystem(fsMap)
  const host = tsvfs.createVirtualCompilerHost(system, compilerOptions, ts)

  const program = ts.createProgram({
    rootNames: [...fsMap.keys()],
    options: compilerOptions,
    host: host.compilerHost,
  })

  const emitResult = program.emit()

  const allDiagnostics = ts.getPreEmitDiagnostics(program).concat(emitResult.diagnostics)

  for (const diagnostic of allDiagnostics) {
    if (diagnostic.file) {
      const { line, character } = ts.getLineAndCharacterOfPosition(diagnostic.file, diagnostic.start)
      const message = ts.flattenDiagnosticMessageText(diagnostic.messageText, "\n")
      console.log(`${diagnostic.file.fileName} (${line + 1},${character + 1}): ${message}`)
    } else {
      console.log(ts.flattenDiagnosticMessageText(diagnostic.messageText, "\n"))
    }
  }

  const exitCode = emitResult.emitSkipped ? 1 : 0
  console.log(`Process exiting with code '${exitCode}'.`)
  return exitCode
}

export default function entry(args) {
  console.log("Running `tsc`...", JSON.stringify({ args: args[0] }))
  const fsMap = new Map()
  const exitCode = compile(args[0], {})
  console.log("Result of `tsc`:", { exitCode })
  return exitCode
}

entry
