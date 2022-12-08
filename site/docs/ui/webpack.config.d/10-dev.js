config.devServer = Object.assign(
  {},
  config.devServer || {},
  {
    open: true,
    port: 8443,
    https: true,
    http2: true,
    historyApiFallback: true,
    setupExitSignals: true,
    proxy: {
      '/docs': {
        target: 'https://beta.elide.dev',
        changeOrigin: true,
        secure: true
      }
    }
  }
);
