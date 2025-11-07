/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  transpilePackages: ['@xterm/xterm', '@xterm/addon-fit'],
}

module.exports = nextConfig
