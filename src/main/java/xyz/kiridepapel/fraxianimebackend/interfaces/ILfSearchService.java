package xyz.kiridepapel.fraxianimebackend.interfaces;

import xyz.kiridepapel.fraxianimebackend.dtos.PageDTO.SearchDTO;

public interface ILfSearchService {
  public SearchDTO searchAnimes(String anime, Integer page);
}
