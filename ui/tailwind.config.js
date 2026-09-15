/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        surface: "#0b0d12",
        panel: "#141821",
        border: "#242a36",
      },
    },
  },
  plugins: [],
};
