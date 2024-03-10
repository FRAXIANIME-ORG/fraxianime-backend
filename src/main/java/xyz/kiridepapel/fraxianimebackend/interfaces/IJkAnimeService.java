package xyz.kiridepapel.fraxianimebackend.interfaces;

import org.jsoup.nodes.Document;

import xyz.kiridepapel.fraxianimebackend.dtos.PageDTO.AnimeInfoDTO;

public interface IJkAnimeService {
  public AnimeInfoDTO getAnimeInfo(AnimeInfoDTO animeInfo, Document docJkanime, String search, boolean isMinDateInAnimeLf);
}
