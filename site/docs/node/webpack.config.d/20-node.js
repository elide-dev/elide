const webpack = require('webpack');

config.resolve.fallback = Object.assign(
  {},
  config.resolve.fallback || {},
  {
    buffer: require.resolve("buffer/"),
    stream: require.resolve("readable-stream"),
  }
);

config.plugins.push(new webpack.ProvidePlugin({
  Buffer: ['buffer', 'Buffer'],
}));
