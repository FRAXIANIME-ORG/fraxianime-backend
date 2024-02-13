package xyz.kiridepapel.fraxianimebackend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import xyz.kiridepapel.fraxianimebackend.entity.AnimeEntity;

@Repository
public interface AnimeRepository extends JpaRepository<AnimeEntity, UUID>{
  AnimeEntity findByName(String name);
}
