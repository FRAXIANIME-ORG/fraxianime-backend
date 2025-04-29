import { handleVideoProxy } from '../utils/proxy.mjs';

/**
 * Controller to proxy video requests to avoid CORS and referrer issues
 */
export const proxyController = async (req, res) => {
  const videoUrl = req.query.url;
  await handleVideoProxy(videoUrl, req, res);
}; 