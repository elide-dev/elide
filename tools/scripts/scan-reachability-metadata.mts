import * as fs from "node:fs"
import * as path from "node:path"

interface Method {
  name: string
  parameterTypes?: string[]
}

interface TypeEntry {
  type: string | { proxy: string[] }
  methods?: Method[]
  fields?: any[]
  allDeclaredMethods?: boolean
  allPublicMethods?: boolean
  allDeclaredConstructors?: boolean
  allPublicConstructors?: boolean
  allDeclaredFields?: boolean
  allPublicFields?: boolean
  unsafeAllocated?: boolean
}

interface ReachabilityMetadata {
  reflection?: TypeEntry[]
  jni?: TypeEntry[]
  resources?: any[]
  bundles?: any[]
  serialization?: any[]
}

interface MethodWithoutParamTypes {
  typeName: string
  methodName: string
  section: "reflection" | "jni"
}

function getTypeName(type: string | { proxy: string[] }): string {
  if (typeof type === "string") {
    return type
  } else if (type.proxy && Array.isArray(type.proxy)) {
    return `proxy: [${type.proxy.join(", ")}]`
  }
  return "unknown"
}

function findMethodsWithoutParamTypes(filePath: string): MethodWithoutParamTypes[] {
  // Read and parse the JSON file
  const fileContent = fs.readFileSync(filePath, "utf-8")
  const metadata: ReachabilityMetadata = JSON.parse(fileContent)

  const methodsWithoutParamTypes: MethodWithoutParamTypes[] = []

  // Check both reflection and jni sections
  const sections: Array<{ name: "reflection" | "jni"; data?: TypeEntry[] }> = [
    { name: "reflection", data: metadata.reflection },
    { name: "jni", data: metadata.jni },
  ]

  for (const section of sections) {
    if (!section.data) continue

    for (const typeEntry of section.data) {
      // Only check if the type has methods declared
      if (typeEntry.methods && typeEntry.methods.length > 0) {
        const typeName = getTypeName(typeEntry.type)

        for (const method of typeEntry.methods) {
          // Check if parameterTypes is missing or undefined
          if (!method.hasOwnProperty("parameterTypes") || method.parameterTypes === undefined) {
            methodsWithoutParamTypes.push({
              typeName,
              methodName: method.name,
              section: section.name,
            })
          }
        }
      }
    }
  }

  return methodsWithoutParamTypes
}

function generateReport(methodsWithoutParamTypes: MethodWithoutParamTypes[]): void {
  if (methodsWithoutParamTypes.length === 0) {
    console.log("✅ All methods have parameterTypes defined!")
    return
  }

  console.log(`⚠️  Found ${methodsWithoutParamTypes.length} methods without parameterTypes:\n`)

  // Group by section
  const bySection = methodsWithoutParamTypes.reduce(
    (acc, item) => {
      if (!acc[item.section]) {
        acc[item.section] = []
      }
      acc[item.section].push(item)
      return acc
    },
    {} as Record<string, MethodWithoutParamTypes[]>,
  )

  // Print results grouped by section and type
  for (const [section, items] of Object.entries(bySection)) {
    console.log(`\n=== ${section.toUpperCase()} SECTION ===`)

    // Group by type within section
    const byType = items.reduce(
      (acc, item) => {
        if (!acc[item.typeName]) {
          acc[item.typeName] = []
        }
        acc[item.typeName].push(item.methodName)
        return acc
      },
      {} as Record<string, string[]>,
    )

    for (const [typeName, methods] of Object.entries(byType)) {
      console.log(`\n  ${typeName}:`)
      for (const method of methods) {
        console.log(`    - ${method}`)
      }
    }
  }
}

// Main execution
function main() {
  const filePath = process.argv[2]

  if (!filePath) {
    console.error("Usage: ts-node script.ts <path-to-metadata-json>")
    process.exit(1)
  }

  if (!fs.existsSync(filePath)) {
    console.error(`File not found: ${filePath}`)
    process.exit(1)
  }

  try {
    console.log(`Analyzing: ${path.basename(filePath)}\n`)
    const methodsWithoutParamTypes = findMethodsWithoutParamTypes(filePath)
    generateReport(methodsWithoutParamTypes)

    // Export results to JSON if needed
    if (process.argv[3] === "--export") {
      const outputPath = filePath.replace(".json", "-missing-params.json")
      fs.writeFileSync(outputPath, JSON.stringify(methodsWithoutParamTypes, null, 2))
      console.log(`\n📄 Results exported to: ${outputPath}`)
    }
  } catch (error) {
    console.error("Error processing file:", error)
    process.exit(1)
  }
}

// Run the script
main()
