package xyz.kiridepapel.fraxianimebackend.interfaces;

import xyz.kiridepapel.fraxianimebackend.dtos.PageDTO.ChapterDTO;

public interface ILfChapterService {
  public ChapterDTO saveLongCacheChapter(String url, String chapter);
  public ChapterDTO constructChapter(String url, String chapter);
}
