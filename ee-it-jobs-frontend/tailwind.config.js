/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        dark: {
          900: '#0f172a',
          800: '#1e293b',
          700: '#334155',
          600: '#475569',
        },
        accent: {
          DEFAULT: '#38bdf8',
          hover: '#7dd3fc',
          dark: '#0284c7',
        }
      }
    },
  },
  plugins: [],
}
