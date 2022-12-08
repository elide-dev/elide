
config.module.rules.push({
  test: /\.(scss|sass)$/,
  use: [
    "style-loader",   // translates CSS into CommonJS
    "css-loader",   // translates CSS into CommonJS
    "sass-loader"   // compiles Sass to CSS, using Node Sass by default
  ]
});

config.module.rules.push({
  test: /\.css$/,
  use: [
    "style-loader",   // translates CSS into CommonJS
    "css-loader",   // translates CSS into CommonJS
  ]
});
