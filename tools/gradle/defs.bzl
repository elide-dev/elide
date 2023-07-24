"""Defines Bazel rules for interacting with Gradle."""

GRADLE_WRAPPER = "//gradle/wrapper"
GRADLE_TOOL_RUNNER = "//tools/gradle:runner"

def _gradle_task_impl(ctx):
    """Run a Gradle task and capture a build output."""

    # resolve the runner, declare a file to capture run output
    runner = ctx.attr._runner
    inputs, _, _ = ctx.resolve_command(tools = [runner])
    tool_bin = inputs[0]
    tool_path = tool_bin.path

    # resolve project and wrapper inputs
    project = ctx.attr.project.files.to_list()
    wrapper = ctx.attr._wrapper.files.to_list()
    tasklist = ctx.attr.tasks
    args_file = ctx.actions.declare_file(ctx.label.name + ".args")
    project_root = project[0].dirname

    env = {}
    args = ctx.actions.args()
    args.add(project_root)
    [args.add(i) for i in tasklist.split(" ")]

    # write to args file
    ctx.actions.write(
        args_file,
        args,
    )

    outputs = []
    outfiles = [args_file]

    if ctx.outputs.output != None:
        outputs.append(ctx.outputs.output)
        outfiles.append(ctx.outputs.output)

    ctx.actions.run(
        inputs = project + wrapper,
        outputs = outputs,
        executable = ctx.executable._runner,
        progress_message = "Gradle %s: %s" % (ctx.label, tasklist),
        mnemonic = "GradleTask",
        arguments = [args],
    )

    return [DefaultInfo(
        files = depset(outfiles),
        runfiles = ctx.runfiles(
            collect_data = True,
            collect_default = True,
            files = [],
        ),
    )]

_gradle_task_rule = rule(
    implementation = _gradle_task_impl,
    attrs = {
        "_runner": attr.label(
            cfg = "host",
            default = GRADLE_TOOL_RUNNER,
            allow_files = True,
            executable = True,
        ),
        "_wrapper": attr.label(
            default = GRADLE_WRAPPER,
            allow_files = True,
        ),
        "project": attr.label(
            allow_files = True,
        ),
        "output": attr.output(
            mandatory = True,
        ),
        "tasks": attr.string(
            mandatory = False,
            default = "build",
        ),
        "debug": attr.bool(
            default = False,
        ),
    },
)

def _gradle_task(name, tags = [], **kwargs):
    """ Macro to wrap a Gradle task target. """

    _gradle_task_rule(
        name = name,
        tags = tags + [
            "requires-network",
        ],
        **kwargs
    )

gradle_task = _gradle_task
