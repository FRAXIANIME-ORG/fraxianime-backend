package xyz.kiridepapel.fraxianimebackend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import xyz.kiridepapel.fraxianimebackend.entity.SpecialCaseEntity;
import xyz.kiridepapel.fraxianimebackend.repository.SpecialCaseRepository;

@Service
public class ScheduleService {
  @Autowired
  private SpecialCaseRepository specialCaseRepository;
  
  @Cacheable(value = "specialCases", key = "#type")
  public List<SpecialCaseEntity> getSpecialCases(Character type) {
    List<SpecialCaseEntity> specialCases = specialCaseRepository.findAll();
    List<SpecialCaseEntity> specialCasesSolicited = new ArrayList<SpecialCaseEntity>();
    
    specialCases.stream().filter(specialCase -> specialCase.getType().equals(type))
        .forEach(specialCasesSolicited::add);

    return specialCasesSolicited;
  }
}
