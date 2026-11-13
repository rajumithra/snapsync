/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        dark: {
          bg: "#0B0F17",
          surface: "#131926",
          card: "#182032",
          border: "#28344D",
        },
        brand: {
          blue: "#3B82F6",
          blueHover: "#2563EB",
          cyan: "#06B6D4",
        }
      },
    },
  },
  plugins: [],
};

