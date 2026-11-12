import type { Config } from 'tailwindcss'
import defaultTheme from 'tailwindcss/defaultTheme'

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    screens: { md: '768px', lg: '1200px' },
    extend: {
      colors: {
        canvas: '#F4F6F8',
        surface: '#FFFFFF',
        ink: { DEFAULT: '#0E1A2B', soft: '#34404F' },
        muted: '#5A6576',
        line: '#DDE2E8',
        accent: { DEFAULT: '#C2410C', soft: '#FFF4EC', light: '#FFD3B8' },
        danger: { DEFAULT: '#A11B1B', soft: '#FDE8E8', line: '#F3C4C4' },
        warn: { DEFAULT: '#8A4B00', soft: '#FFF4E0' },
        skeleton: '#E6EAEF',
        divider: '#EDF0F3',
        'best-row': '#F1FAF5',
        mist: '#C9D1DC',
        cover: {
          0: '#FCE3D3',
          1: '#DCE8FB',
          2: '#E2F1E6',
          3: '#EDE5F7',
          4: '#F7EDD0',
          5: '#E3E7F0',
        },
        deal: { DEFAULT: '#0F6B45', soft: '#E7F5EE' },
        link: { DEFAULT: '#1D4ED8', hover: '#1E3A8A' },
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
      maxWidth: { page: '1248px' },
      boxShadow: {
        menu: '0 12px 32px rgba(14,26,43,0.12)',
        toast: '0 8px 24px rgba(14,26,43,0.18)',
      },
      minHeight: { touch: '44px' },
      minWidth: { touch: '44px' },
    },
  },
  plugins: [],
} satisfies Config
