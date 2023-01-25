
config.module.rules.push({
  test: /\.(scss|sass)$/,
  use: [
    "style-loader",   // translates CSS into CommonJS
    "css-loader",   // translates CSS into CommonJS
    "sass-loader"   // compiles Sass to CSS, using Node Sass by default
  ]
});

config.module.rules.push({
  test: /\.css$/i,
  use: [
    "style-loader",
    "css-loader",
    {
      loader: "postcss-loader",
      options: {
        postcssOptions: {
          plugins: [
            [
              "postcss-preset-env",
              {
                // Options
              },
            ],
          ],
        },
      },
    },
  ]
});
