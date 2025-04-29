import express from 'express';
import dotenv from 'dotenv';
import cors from 'cors';
import animeRoutes from './src/routes/animeRoutes.mjs';
import path from 'path';
import { fileURLToPath } from 'url';
import { corsOptions } from './src/config/cors.mjs';

// Load environment variables
dotenv.config();

// Get __dirname equivalent in ESM
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const port = process.env.PORT || 3000;

// Middleware
app.use(express.json());
app.use(cors(corsOptions)); // Aplicar CORS a todas las rutas

// Middleware para manejar solicitudes OPTIONS preflight
app.options('*', (req, res) => {
  res.status(204).end();
});

// Serve static files
app.use(express.static(__dirname));

// Routes
app.use('/api/v2', animeRoutes);

// Start the server
app.listen(port, () => {
  console.log(`Server running at http://localhost:${port}`);
});