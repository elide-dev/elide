module.exports = {
    charset: "utf8",
    drop: ["console", "debugger"],
    minify: true,
    target: "es2021",
    treeShaking: false,
    legalComments: "external",
    platform: "neutral",
    footer: {
        js: (
            "// Elide JS Runtime. Copyright (c) 2022, Sam Gammon and the Elide Project Authors. All rights reserved." +
            "\n// Components of this software are licensed separately. See https://github.com/elide-dev/v3 for more."
        ),
    }
};
