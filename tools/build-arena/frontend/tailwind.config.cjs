/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        elide: {
          primary: '#6366f1',
          secondary: '#8b5cf6',
          dark: '#1e1b4b',
        },
      },
    },
  },
  plugins: [],
}
