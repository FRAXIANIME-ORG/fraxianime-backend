import https from 'https';
import http from 'http';
import url from 'url';

/**
 * Función para manejar solicitudes proxy de video para evitar problemas de CORS y referrer
 * @param {string} videoUrl - URL del video a proxear
 * @param {object} req - Objeto de solicitud de Express
 * @param {object} res - Objeto de respuesta de Express
 */
export const handleVideoProxy = async (videoUrl, req, res) => {
  try {
    if (!videoUrl) {
      return res.status(400).json({ error: 'URL de video no proporcionada' });
    }

    res.header('Access-Control-Allow-Origin', '*');
    res.header('Access-Control-Allow-Methods', 'GET, OPTIONS');
    res.header('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-Requested-With, Accept, Origin');
    
    if (req.method === 'OPTIONS') {
      return res.status(204).send();
    }
    
    const parsedUrl = url.parse(videoUrl);
    const protocol = parsedUrl.protocol === 'https:' ? https : http;
    
    const options = {
      hostname: parsedUrl.hostname,
      path: parsedUrl.path,
      method: 'GET',
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
        'Referer': `https://${parsedUrl.hostname}/`,
        'Origin': `https://${parsedUrl.hostname}`,
        'Accept': 'video/webm,video/ogg,video/*;q=0.9,application/ogg;q=0.7,audio/*;q=0.6,*/*;q=0.5',
        'Accept-Language': 'es-ES,es;q=0.9,en;q=0.8',
        'Cache-Control': 'no-cache',
        'Connection': 'keep-alive'
      }
    };
    
    if (req.headers.range) {
      options.headers.Range = req.headers.range;
    }
    
    const proxyReq = protocol.request(options, (proxyRes) => {
      res.setHeader('Content-Type', proxyRes.headers['content-type'] || 'video/mp4');
      
      Object.keys(proxyRes.headers).forEach(header => {
        if (!['connection', 'transfer-encoding'].includes(header.toLowerCase())) {
          res.setHeader(header, proxyRes.headers[header]);
        }
      });
      
      res.status(proxyRes.statusCode);
      
      proxyRes.pipe(res);
    });
    
    proxyReq.on('error', (error) => {
      console.error('Error proxying video:', error);
      res.status(500).json({ error: 'Error al recuperar el video' });
    });
    
    proxyReq.end();
    
  } catch (error) {
    console.error('Video proxy error:', error);
    res.status(500).json({ error: 'Hubo un problema al servir el video' });
  }
}; 