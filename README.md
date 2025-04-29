# Fraxianime Backend

A Node.js API for anime information, built with Express.js and Playwright for scraping data.

## Features

- Get latest anime updates from the provider
- Redirect to specific anime episodes

## API Endpoints

- `GET /api/animes` - Get a list of latest anime updates
- `GET /api/animes/:animeName/:episode` - Redirect to a specific anime episode

## Installation

1. Clone the repository
2. Install dependencies:
   ```
   npm install
   ```
3. Create a `.env` file with the following variables:
   ```
   PORT=3000
   PROVIDER_URL=https://jkanime.net
   ```

## Usage

### Development
```
npm run dev
```

### Production
```
npm start
```

## Project Structure

```
fraxianime-backend/
├── src/
│   ├── controllers/
│   │   └── animeController.mjs
│   ├── services/
│   │   └── animeService.mjs
│   └── routes/
│       └── animeRoutes.mjs
├── .env
├── package.json
├── README.md
└── server.mjs
``` 