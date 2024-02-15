package xyz.kiridepapel.fraxianimebackend.service;

import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Service
public class DatabaseManageService {
  @PersistenceContext
  private EntityManager entityManager;
  
  @Transactional
  public void resetTable(String tableName) {
    entityManager.createNativeQuery("TRUNCATE TABLE " + tableName + " RESTART IDENTITY").executeUpdate();
  }
}
