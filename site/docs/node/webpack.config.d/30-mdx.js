
config.module.rules.push({
  test: /\.(mdx|md)$/,
  use: [
    {
      loader: "@mdx-js/loader",

      /** @type {import('@mdx-js/loader').Options} */
      options: {
        jsxImportSource: '@emotion/react'
      },
    },
  ]
});
