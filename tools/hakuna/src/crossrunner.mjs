import { Command } from "commander";
import { sync as which } from "which";
import pkgInfo from "../package.json";
import { existsSync } from "node:fs";
import { basename } from "node:path";
import { spawnSync } from "node:child_process";

const resolvedBinMap = {}
const runtimeKeyMap = {}

const cmd = new Command("hakuna")
  .name("hakuna")
  .description(pkgInfo.description)
  .version(pkgInfo.version)

const bench = new Command("crossbench")
  .alias("bench")
  .argument('[runtimes]')
  .argument('[benchmarks...]')
  .action(crossrunner)

cmd.addCommand(bench);

export function handleFinished() {
    console.info("Done.");
}

export function handleError(err) {
    console.error(err);
    process.exit(1);
}

async function resolveBin(name) {
    return which(name);
}

async function obtainVersion(path) {
    const { stdout } = spawnSync(path, ["--version"]);
    return stdout.toString().replace("\n", "");
}

export async function crossrunner(runtimesAll, files) {
    const runtimes = runtimesAll.split(",");
    const benchmarks = Array.isArray(files) && files.length > 0 ? files : ["./**/*.bench.mjs"];
    for (const runtime of runtimes) {
        // is it a valid file path?
        if (existsSync(runtime)) {
            const key = basename(runtime)
            runtimeKeyMap[runtime] = key;
            resolvedBinMap[key] = runtime;
        } else {
            runtimeKeyMap[runtime] = runtime;
            resolvedBinMap[runtime] = await resolveBin(runtime);
        }
    }
    const resolvedVersionMap = {}
    for (const runtime of Object.keys(resolvedBinMap)) {
        resolvedVersionMap[runtime] = await obtainVersion(resolvedBinMap[runtime]);
    }

    const benchMatrix = `
Cross-Runtime Benchmark Matrix:
- Hakuna: ${pkgInfo.version}
- Runtimes: ${runtimes.join(", ")}
- Benchmarks: ${benchmarks.join(", ")}
- Options: ${JSON.stringify(bench.opts(), null, 2)}

Resolved binaries:
${JSON.stringify(resolvedBinMap, null, 2)}

Versions:
${JSON.stringify(resolvedVersionMap, null, 2)}`

    console.info(benchMatrix)

    for (const runtime of Object.keys(resolvedBinMap)) {
        console.info(`-------- ${runtime}`)
        for (const bench of benchmarks) {
            await runBenchWithRuntime(runtime, resolvedBinMap[runtime], bench)
        }
    }
}

export async function runBenchAndPipe(runtime, executable, args) {
    try {
        console.log(`[${runtime}]`, executable, args.join(' '));
        const { stdout } = spawnSync(executable, args);
        console.log(stdout.toString());
        return stdout.toString();
    } catch (err) {
        console.error(`Error running ${runtime} with ${executable} and ${args}`);
        console.error(err);
        return "";
    }
}

export async function runBenchWithRuntime(runtime, path, benchmark) {
    switch (runtime) {
        case 'bun':
            return await runBenchAndPipe(runtime, path, ['run', '--preload', benchmark, './tools/hakuna/src/runner.mjs']);
        case 'elide':
            return await runBenchAndPipe(runtime, path, ['run', '--js', '--host:allow-all', './tools/hakuna/src/runner.mjs', benchmark]);
        case 'node':
            return await runBenchAndPipe(runtime, path, ['./tools/hakuna/src/runner.mjs', benchmark]);
        case 'deno':
            return await runBenchAndPipe(runtime, path, ['./tools/hakuna/src/runner.mjs', benchmark]);
        default:
            throw new Error(`Unknown runtime: ${runtime}`);
    }
}

export default async function main() {
    const args = process.argv.slice(1).filter((arg) => !(
      arg.includes("hakuna/src/entry.mjs") ||
      arg.includes("$bunfs")
    ))
    await cmd.parseAsync(args, { from: 'user' });
}
