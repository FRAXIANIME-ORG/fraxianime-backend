package xyz.kiridepapel.fraxianimebackend.interfaces;

import java.util.List;

import xyz.kiridepapel.fraxianimebackend.dtos.IndividualDTO.AnimeDataDTO;
import xyz.kiridepapel.fraxianimebackend.dtos.PageDTO.DirectoryOptionsDTO;

public interface ILfDirectoryService {
  public DirectoryOptionsDTO directoryOptions(String options);
  public List<AnimeDataDTO> saveLongDirectoryAnimes(String uri);
  public List<AnimeDataDTO> constructDirectoryAnimes(String uri);
}
