package xyz.kiridepapel.fraxianimebackend.interfaces;

import xyz.kiridepapel.fraxianimebackend.dtos.PageDTO.AnimeInfoDTO;

public interface ILfAnimeService {
  public AnimeInfoDTO anime(String search);
}
