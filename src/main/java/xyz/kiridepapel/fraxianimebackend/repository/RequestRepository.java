package xyz.kiridepapel.fraxianimebackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import xyz.kiridepapel.fraxianimebackend.entity.RequestEntity;

@Repository
public interface RequestRepository extends JpaRepository<RequestEntity, Long>{
  RequestEntity findByEmailAndIp(String email, String ip);
}
