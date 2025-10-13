module.exports = {
  root: true,
  parser: '@typescript-eslint/parser',
  parserOptions: {
    project: ['./tsconfig.json'],
    tsconfigRootDir: __dirname
  },
  env: {
    browser: true,
    es2022: true
  },
  plugins: ['@typescript-eslint', 'react', 'react-hooks', '@tanstack/query', 'tailwindcss'],
  extends: [
    'eslint:recommended',
    'plugin:react/recommended',
    'plugin:react-hooks/recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:@typescript-eslint/recommended-type-checked',
    'plugin:@tanstack/query/recommended',
    'plugin:tailwindcss/recommended',
    'plugin:react/jsx-runtime',
    'prettier'
  ],
  settings: {
    react: {
      version: 'detect'
    },
    tailwindcss: {
      callees: ['clsx']
    }
  },
  rules: {
    'react/prop-types': 'off'
  }
};

