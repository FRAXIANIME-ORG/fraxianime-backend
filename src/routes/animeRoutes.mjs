import express from 'express';
import { homeController, chapterController, animeInfoController } from '../controllers/animeController.mjs';
import { proxyController } from '../controllers/proxyController.mjs';

const router = express.Router();

// Route to get latest animes
router.get('/', homeController);

// Route to get anime information
router.get('/anime/:animeName', animeInfoController);

// Route to get chapter content
router.get('/anime/:animeName/:chapter', chapterController);

// Route to proxy video
router.get('/video', proxyController);

export default router; 