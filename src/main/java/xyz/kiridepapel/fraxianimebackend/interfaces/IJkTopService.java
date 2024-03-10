package xyz.kiridepapel.fraxianimebackend.interfaces;

import xyz.kiridepapel.fraxianimebackend.dtos.PageDTO.TopDTO;

public interface IJkTopService {
  public TopDTO pastYearsCacheTop(String year, String season);
  public TopDTO actualYearCacheTop(String year, String season);
}
