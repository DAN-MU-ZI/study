import { readFileSync, writeFileSync } from 'node:fs';
import { renderMermaidSVG, THEMES } from 'beautiful-mermaid';

const inputPath = new URL('./erd.mmd', import.meta.url);
const outputPath = new URL('./erd.svg', import.meta.url);
const source = readFileSync(inputPath, 'utf8');

const svg = renderMermaidSVG(source, {
  ...THEMES['github-light'],
  font: 'Inter',
  padding: 48,
  nodeSpacing: 32,
  layerSpacing: 48,
  componentSpacing: 32,
  thoroughness: 6,
});

writeFileSync(outputPath, svg, 'utf8');
