FROM node:20-bookworm-slim

WORKDIR /app

COPY package.json ./
RUN npm install

COPY . .

EXPOSE 4173

CMD ["npm", "run", "start", "--", "--host", "0.0.0.0", "--port", "4173"]
