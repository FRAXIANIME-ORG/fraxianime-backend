package xyz.kiridepapel.fraxianimebackend.utils;

import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Service
public class DatabaseUtils {
  @PersistenceContext
  private EntityManager entityManager;
  
  @Transactional
  public void resetTable(String tableName) {
    entityManager.createNativeQuery("TRUNCATE TABLE " + tableName + " RESTART IDENTITY").executeUpdate();
  }
}
