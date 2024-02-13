package xyz.kiridepapel.fraxianimebackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "anime")
public class AnimeEntity {
  @Id
  @GeneratedValue(generator = "uuid2")
  @Column(columnDefinition = "UUID")
  private UUID id;

  private String name;

  @Column(columnDefinition = "TEXT")
  private String synopsisEnglish;
}
