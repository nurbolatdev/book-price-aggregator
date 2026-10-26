import type { Config } from 'tailwindcss'
import defaultTheme from 'tailwindcss/defaultTheme'

export default {
  content: ['./index.html', './src/**/*.{js,jsx,ts,tsx}'],
  theme: {
    extend: {
      colors: {
        canvas: '#F4F6F8',
        surface: '#FFFFFF',
        ink: '#0E1A2B',
        muted: '#5A6576',
        line: '#DDE2E8',
        accent: '#C2410C',
        deal: { DEFAULT: '#0F6B45', soft: '#E7F5EE' },
        link: '#1D4ED8',
      },
      fontFamily: {
        display: ['Unbounded', ...defaultTheme.fontFamily.sans],
        sans: ['Onest', ...defaultTheme.fontFamily.sans],
      },
      borderRadius: {
        control: '10px',
        card: '16px',
        block: '20px',
      },
      minHeight: { touch: '44px' },
      minWidth: { touch: '44px' },
    },
  },
  plugins: [],
} satisfies Config
