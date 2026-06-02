import { readFileSync, writeFileSync } from 'node:fs';
import { renderMermaidSVG, THEMES } from 'beautiful-mermaid';

const [, , inputFile, outputFile] = process.argv;

if (!inputFile) {
  throw new Error('Usage: node diagrams/render-diagram.mjs <input.mmd> [output.svg]');
}

const inputPath = new URL(`./${inputFile}`, import.meta.url);
const outputPath = new URL(`./${outputFile ?? inputFile.replace(/\.mmd$/, '.svg')}`, import.meta.url);
const source = readFileSync(inputPath, 'utf8');

const svg = renderMermaidSVG(source, {
  ...THEMES['github-light'],
  font: 'Inter',
  padding: 48,
  nodeSpacing: 40,
  layerSpacing: 56,
  componentSpacing: 36,
  thoroughness: 6,
});

writeFileSync(outputPath, svg, 'utf8');
