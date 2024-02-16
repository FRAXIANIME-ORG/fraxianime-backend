package xyz.kiridepapel.fraxianimebackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import xyz.kiridepapel.fraxianimebackend.entity.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long>{
  Optional<UserEntity> findByUsername(String username);

  Boolean existsByUsername(String username);
}
