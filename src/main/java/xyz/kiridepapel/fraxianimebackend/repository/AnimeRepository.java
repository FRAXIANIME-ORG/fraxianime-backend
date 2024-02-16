package xyz.kiridepapel.fraxianimebackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import xyz.kiridepapel.fraxianimebackend.entity.AnimeEntity;

@Repository
public interface AnimeRepository extends JpaRepository<AnimeEntity, Long>{
  AnimeEntity findByName(String name);
}
