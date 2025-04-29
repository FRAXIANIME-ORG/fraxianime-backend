import { chromium } from 'playwright';

/**
 * Navigate to a specific URL and execute a function to extract data
 * @param {string} url - URL to navigate to
 * @param {Function} extractFunction - Function to execute for data extraction (optional)
 * @param {Object} params - Parameters to pass to the extraction function
 * @param {Object} options - Additional options
 * @returns {Promise<any>} - Result of extraction or HTML content
 */
export const navigateTo = async (url, extractFunction = null, params = {}, options = {}) => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    userAgent: options.userAgent || 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
    viewport: options.viewport || { width: 1280, height: 800 }
  });

  const page = await context.newPage();
  
  await page.goto(url, { waitUntil: 'domcontentloaded' });
  
  if (options.waitForJs) {
    await page.waitForTimeout(options.waitTime || 500);
  }
  
  if (options.autoScroll) {
    await autoScroll(page, options.scrollSpeed || 50);
  }
  
  let result;
  
  if (extractFunction) {
    result = await page.evaluate(extractFunction, params);
  } else {
    result = await page.content();
  }
  
  await browser.close();
  return result;
};

/**
 * Función para hacer scroll automático hasta el final de la página
 * para activar la carga de elementos lazy
 */
async function autoScroll(page, scrollSpeed = 100) {
  await page.evaluate(async (speed) => {
    await new Promise((resolve) => {
      let totalHeight = 0;
      let maxScroll = Math.min(document.body.scrollHeight, 3000); // 3000px máximo
      const distance = speed;
      const timer = setInterval(() => {
        window.scrollBy(0, distance);
        totalHeight += distance;

        if (totalHeight >= maxScroll - window.innerHeight) {
          clearInterval(timer);
          resolve();
        }
      }, 50); // Intervalo más corto para scroll más rápido
    });
  }, scrollSpeed);
} 