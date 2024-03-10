package xyz.kiridepapel.fraxianimebackend.interfaces;

import xyz.kiridepapel.fraxianimebackend.dtos.PageDTO.ScheduleDTO;

public interface IJkScheduleService {
  public ScheduleDTO getSchedule(String keyName);
}
