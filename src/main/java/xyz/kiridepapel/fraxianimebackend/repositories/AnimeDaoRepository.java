package xyz.kiridepapel.fraxianimebackend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import xyz.kiridepapel.fraxianimebackend.entities.AnimeEntity;

@Repository
public interface AnimeDaoRepository extends JpaRepository<AnimeEntity, Long>{
  AnimeEntity findByName(String name);
}
