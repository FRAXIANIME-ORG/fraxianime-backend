import dotenv from 'dotenv';
import { navigateTo } from '../utils/playwright.mjs';

dotenv.config();

const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:3000/api/v2';
const PROVIDER_URL = process.env.PROVIDER_URL || 'https://jkanime.net';

export const home = async () => {
  try {
    const extractLatestAnimes = (params) => {
      const animeItems = document.querySelectorAll('.anime__sidebar__comment__item');
      
      return Array.from(animeItems).map(item => {
        // Information
        let title = item.querySelector('h5')?.textContent?.trim() || '';
        let chapter = item.querySelector('h6')?.textContent?.trim() || '';
        let date = item.querySelector('span')?.textContent?.trim() || '';
        const imageElement = item.querySelector('.anime__sidebar__comment__item__pic img');
        let image = imageElement ? imageElement.getAttribute('src') : null;
        const linkElement = item.closest('a');
        let link = linkElement ? linkElement.getAttribute('href') : null;

        // Format chapter
        try {
          chapter = Number(chapter.split('\n')[1].replaceAll(' ', ''));
        } catch (error) {
          chapter = 1;
        }

        // Format date
        switch (date) {
          case 'Hoy':
            date = new Date().toLocaleDateString();
            break;
          case 'Ayer':
            date = new Date(new Date().setDate(new Date().getDate() - 1)).toLocaleDateString();
            break;
          default:
            date = date + '/' + new Date().getFullYear();
            date = date.split('/').reverse().join('-');
            date = new Date(date).toLocaleDateString();
        }

        // Format link
        if (link) {
          const segments = link.split('/');
          if (segments.length >= 5) {
            link = params.backendUrl + '/' + segments[3] + '/' + segments[4];
          }
        }
        
        return { title, chapter, date, image, link };
      });
    };

    return await navigateTo(
      PROVIDER_URL, 
      extractLatestAnimes, 
      { backendUrl: BACKEND_URL },
      { waitForJs: false }
    );
  } catch (error) {
    console.error('Error during scraping:', error);
    throw new Error('There was a problem scraping anime data');
  }
};

export const animeInfo = async (animeName, page = 1) => {
  try {
    const url = `${PROVIDER_URL}/${animeName}/`;
    
    const extractAnimeInfo = (params) => {
      // Información básica del anime
      const title = document.querySelector('.anime__details__title h3')?.textContent?.trim() || '';
      const titleEnglish = document.querySelector('.anime__details__title span')?.textContent?.trim() || '';
      const description = document.querySelector('.tab.sinopsis')?.textContent?.trim() || '';
      const image = document.querySelector('.anime__details__pic')?.getAttribute('data-setbg') || null;
      
      // Información adicional
      const infoElements = document.querySelectorAll('.anime__details__widget .aninfo li');
      const info = {};
      
      infoElements.forEach(element => {
        const label = element.querySelector('span')?.textContent?.trim().replace(':', '') || '';
        if (label) {
          // Para los géneros, extraer todos los enlaces
          if (label === 'Genero') {
            const genres = Array.from(element.querySelectorAll('a')).map(a => a.textContent.trim());
            info['genres'] = genres;
          } else {
            // Para otros elementos, extraer el texto después del span
            const content = element.innerHTML.split('</span>')[1]?.trim() || '';
            const cleanContent = content.replace(/<[^>]*>/g, '').trim();
            
            if (label === 'Episodios') {
              const episodesMatch = cleanContent.match(/(\d+)/);
              if (episodesMatch) {
                info['episodes'] = parseInt(episodesMatch[1], 10);
              } else if (cleanContent === 'Desconocido') {
                info['episodes'] = null;
              } else {
                info['episodes'] = cleanContent;
              }
            } else {
              info[label.toLowerCase()] = cleanContent;
            }
          }
        }
      });
      
      const altTitles = {};
      const altTitleElements = document.querySelectorAll('#c .t');
      altTitleElements.forEach(element => {
        const category = element.textContent.trim();
        let content = '';
        let sibling = element.nextSibling;
        
        while (sibling && sibling.nodeType === Node.TEXT_NODE || (sibling.nodeType === Node.ELEMENT_NODE && !sibling.classList.contains('t'))) {
          if (sibling.textContent) {
            content += sibling.textContent.trim();
          }
          sibling = sibling.nextSibling;
          if (!sibling) break;
        }
        
        if (content) {
          altTitles[category.toLowerCase()] = content.trim();
        }
      });
      
      const additionalLinks = Array.from(document.querySelectorAll('h5#aditional + a')).map(a => {
        return {
          title: a.textContent.trim(),
          url: a.getAttribute('href').replace(params.providerUrl, params.backendUrl).replace(/\/$/, '')
        };
      });
      
      const trailerElement = document.querySelector('.animeTrailer');
      let trailerUrl = null;
      if (trailerElement) {
        const ytId = trailerElement.getAttribute('data-yt');
        if (ytId) {
          trailerUrl = `https://www.youtube.com/watch?v=${ytId}`;
        }
      }
      
      const stats = {};
      const statElements = document.querySelectorAll('.stats .it');
      statElements.forEach(element => {
        const countElement = element.querySelector('.count b');
        if (countElement) {
          const count = countElement.textContent.trim();
          const label = element.textContent.replace(countElement.textContent, '').replace(/<[^>]*>/g, '').trim();
          if (count && label) {
            stats[label.toLowerCase()] = count ? parseInt(count, 10) : 0;
          }
        }
      });
      
      const lastEpisodeElement = document.querySelector('#proxep #uep');
      let lastEpisode = null;
      if (lastEpisodeElement) {
        const lastEpisodeTitle = lastEpisodeElement.textContent.trim().replace('Nuevo', '').trim();
        const lastEpisodeUrl = lastEpisodeElement.getAttribute('href');
        const isNew = lastEpisodeElement.querySelector('.new') !== null;
        
        const titleNumberMatch = lastEpisodeTitle.match(/(.*)\s*-\s*(\d+)$/);
        
        let title = lastEpisodeTitle;
        let episodeNumber = null;
        
        if (titleNumberMatch && titleNumberMatch.length >= 3) {
          title = titleNumberMatch[1].trim();
          episodeNumber = parseInt(titleNumberMatch[2], 10);
        }
        
        lastEpisode = {
          title: title,
          number: episodeNumber,
          url: lastEpisodeUrl ? lastEpisodeUrl.replace(params.providerUrl, params.backendUrl).replace(/\/$/, '') : null,
          isNew: isNew
        };
      }
      
      // Capítulos
      const chapters = Array.from(document.querySelectorAll('#episodes-content .epcontent')).map(item => {
        const link = item.querySelector('a')?.getAttribute('href') || '';
        const title = item.querySelector('.anime__item__text span')?.textContent?.trim() || '';
        const image = item.querySelector('.anime__item__pic')?.getAttribute('data-setbg') || null;
        
        const chapterMatch = title.match(/Capitulo (\d+)/i);
        const chapterNumber = chapterMatch ? parseInt(chapterMatch[1], 10) : null;
        
        return {
          title,
          number: chapterNumber,
          image,
          url: link ? link.replace(params.providerUrl, params.backendUrl).replace(/\/$/, '') : null
        };
      });
      
      const pagination = Array.from(document.querySelectorAll('.anime__pagination .numbers')).map(item => {
        const paginationText = item.textContent.trim();
        const pageMatch = item.getAttribute('href')?.match(/#pag(\d+)/) || [];
        const rangeMatch = paginationText.match(/(\d+)\s*-\s*(\d+)/);
        
        return {
          page: pageMatch[1] ? parseInt(pageMatch[1], 10) : null,
          range: rangeMatch ? {
            start: parseInt(rangeMatch[1], 10),
            end: parseInt(rangeMatch[2], 10)
          } : null
        };
      });
      
      const recommendations = Array.from(document.querySelectorAll('#relacionados .anime__item')).map(item => {
        const linkElement = item.querySelector('a');
        const titleElement = item.querySelector('.anime__item__text h5 a');
        const imageElement = item.querySelector('.anime__item__pic');
        
        const link = linkElement ? linkElement.getAttribute('href') : null;
        const title = titleElement ? titleElement.textContent.trim() : '';
        const image = imageElement ? imageElement.getAttribute('data-setbg') : null;
        
        return {
          title,
          image,
          url: link ? link.replace(params.providerUrl, params.backendUrl).replace(/\/$/, '') : null
        };
      });
      
      return {
        title,
        titleEnglish,
        description,
        image,
        info,
        alternativeTitles: altTitles,
        additionalLinks,
        trailerUrl,
        stats,
        lastEpisode,
        chapters,
        pagination,
        recommendations
      };
    };
    
    const options = {
      waitForJs: true,
      waitTime: 1000
    };
    
    return await navigateTo(
      url, 
      extractAnimeInfo, 
      { animeName, page, backendUrl: BACKEND_URL, providerUrl: PROVIDER_URL }, 
      options
    );
  } catch (error) {
    console.error('Error fetching anime info:', error);
    throw new Error('There was a problem fetching the anime information');
  }
};

export const chapter = async (animeName, chapterNum) => {
  try {
    const url = `${PROVIDER_URL}/${animeName}/${chapterNum}`;
    
    const extractChapterData = (params) => {
      const episodeTitle = document.querySelector('.breadcrumb__links h1')?.textContent?.trim() || '';
      const titleParts = episodeTitle.split('-');
      const title = titleParts.length > 1 ? titleParts[titleParts.length - 1].trim() : episodeTitle;
      
      const videoIframe = document.querySelector('.player_conte');
      const videoUrl = videoIframe ? videoIframe.getAttribute('src') : null;
      
      const downloadLinks = Array.from(document.querySelectorAll('.download table tr:not(:first-child)')).map(row => {
        const columns = row.querySelectorAll('td');
        if (columns.length >= 4) {
          return {
            server: columns[0]?.textContent?.trim() || '',
            size: columns[1]?.textContent?.trim() || '',
            link: columns[3]?.querySelector('a')?.getAttribute('href') || ''
          };
        }
        return null;
      }).filter(item => item !== null);
      
      const animeImage = document.querySelector('.video_t img')?.getAttribute('src') || null;
      const animeTitleElement = document.querySelector('.video_i a');
      const animeTitle = animeTitleElement?.textContent?.trim() || '';
      
      const animeChaptersText = document.querySelector('.video_i span')?.textContent?.trim() || '';
      const chaptersMatch = animeChaptersText.match(/(\d+)/);
      const totalChapters = chaptersMatch ? parseInt(chaptersMatch[1], 10) : null;
      
      const prevChapterElement = document.querySelector('.items_list .vt + .tin h3');
      let prevChapter = null;
      if (prevChapterElement) {
        const prevNumber = parseInt(prevChapterElement.textContent.split('-').pop().trim(), 10);
        let link = document.querySelector('.items_list a')?.getAttribute('href') || null;
        
        if (link && link.startsWith(params.providerUrl)) {
          link = link.replace(params.providerUrl, params.backendUrl).replace(/\/$/, '');
        }
        
        prevChapter = {
          number: prevNumber,
          link: link
        };
      }
      
      const nextChapterElement = document.querySelector('.items_list + .items_list .vt + .tin h3');
      let nextChapter = null;
      if (nextChapterElement && !document.querySelector('.finalcaps')) {
        const nextNumber = parseInt(nextChapterElement.textContent.split('-').pop().trim(), 10);
        let link = document.querySelector('.items_list + .items_list a')?.getAttribute('href') || null;
        
        if (link && link.startsWith(params.providerUrl)) {
          link = link.replace(params.providerUrl, params.backendUrl).replace(/\/$/, '');
        }
        
        nextChapter = {
          number: nextNumber,
          link: link
        };
      }
      
      return {
        chapterInfo: {
          title,
          chapter: parseInt(params.chapterNum, 10),
          animeName: params.animeName
        },
        videoUrl,
        downloadLinks,
        animeInfo: {
          title: animeTitle,
          image: animeImage,
          chapters: totalChapters,
          url: `${params.backendUrl}/${params.animeName}`
        },
        navigation: {
          previous: prevChapter,
          next: nextChapter
        }
      };
    };
    
    return await navigateTo(url, extractChapterData, { 
      animeName, 
      chapterNum, 
      backendUrl: BACKEND_URL, 
      providerUrl: PROVIDER_URL 
    }, { waitForJs: false });
  } catch (error) {
    console.error('Error fetching chapter:', error);
    throw new Error('There was a problem fetching the chapter');
  }
};
