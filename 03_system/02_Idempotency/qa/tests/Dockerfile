FROM mcr.microsoft.com/playwright:v1.55.0-noble

WORKDIR /workspace

COPY package*.json tsconfig.json playwright.config.ts ./
RUN npm ci || npm install

COPY . .
RUN ln -s /ms-playwright .playwright

CMD ["npm", "run", "test:e2e"]
