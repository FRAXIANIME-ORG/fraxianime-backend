import { home, chapter, animeInfo } from '../services/animeService.mjs';

export const homeController = async (req, res) => {
  try {
    const animeList = await home();
    res.json(animeList);
  } catch (error) {
    console.error('Controller error:', error);
    res.status(500).json({ error: 'There was a problem fetching anime data.' });
  }
};

export const chapterController = async (req, res) => {
  try {
    const { animeName, chapter: chapterNum } = req.params;
    const chapterData = await chapter(animeName, chapterNum);
    
    // Devolver los datos procesados como JSON
    res.json(chapterData);
  } catch (error) {
    console.error('Controller error:', error);
    res.status(500).json({ error: 'There was a problem fetching the chapter.' });
  }
};

export const animeInfoController = async (req, res) => {
  try {
    const { animeName } = req.params;
    const page = req.query.page ? parseInt(req.query.page, 10) : 1;
    
    const infoData = await animeInfo(animeName, page);
    
    // Devolver los datos procesados como JSON
    res.json(infoData);
  } catch (error) {
    console.error('Controller error:', error);
    res.status(500).json({ error: 'There was a problem fetching the anime information.' });
  }
}; 