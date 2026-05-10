import { defineConfig } from 'cypress';
import codeCoverage from '@cypress/code-coverage/task';

export default defineConfig({
  component: {
    devServer: {
      framework: 'angular',
      bundler: 'webpack',
      webpackConfig: {
        module: {
          rules: [
            {
              test: /\.(ts|js)$/,
              use: {
                loader: 'babel-loader',
                options: {
                  plugins: ['babel-plugin-istanbul'],
                  presets: [],
                },
              },
              enforce: 'post',
              exclude: /\.(cy|spec)\.(ts|js)$|node_modules/,
            },
          ],
        },
      },
    },
    specPattern: 'src/**/*.cy.ts',
    supportFile: 'cypress/support/component.ts',
    indexHtmlFile: 'cypress/support/component-index.html',
    setupNodeEvents(on, config) {
      codeCoverage(on, config);
      return config;
    },
  },
});
