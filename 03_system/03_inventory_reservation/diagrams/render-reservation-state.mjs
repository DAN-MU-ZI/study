import { readFileSync, writeFileSync } from 'node:fs';
import { renderMermaidSVG, THEMES } from 'beautiful-mermaid';

const inputPath = new URL('./reservation-state.mmd', import.meta.url);
const outputPath = new URL('./reservation-state.svg', import.meta.url);
const source = readFileSync(inputPath, 'utf8');

const svg = renderMermaidSVG(source, {
  ...THEMES['github-light'],
  font: 'Inter',
  padding: 48,
  nodeSpacing: 36,
  layerSpacing: 52,
  componentSpacing: 32,
  thoroughness: 6,
});

writeFileSync(outputPath, svg, 'utf8');
