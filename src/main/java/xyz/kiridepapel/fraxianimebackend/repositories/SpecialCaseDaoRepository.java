package xyz.kiridepapel.fraxianimebackend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import xyz.kiridepapel.fraxianimebackend.entities.SpecialCaseEntity;

@Repository
public interface SpecialCaseDaoRepository extends JpaRepository<SpecialCaseEntity, Long>{
  @Query("SELECT sc.mapped FROM SpecialCaseEntity sc WHERE sc.original = ?1 AND sc.type = ?2")
  String getMappedByOriginalAndType(String original, Character type);

  @Query("SELECT sc FROM SpecialCaseEntity sc WHERE sc.type = ?1")
  List<SpecialCaseEntity> findByType(Character type);

  @Query("SELECT sc FROM SpecialCaseEntity sc WHERE sc.type = ?1 OR sc.type = ?2")
  List<SpecialCaseEntity> findByTypes(Character type1, Character type2);
}
