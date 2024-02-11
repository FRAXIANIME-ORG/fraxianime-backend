package xyz.kiridepapel.fraxianimebackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.LinkDTO;
import xyz.kiridepapel.fraxianimebackend.service.MegaService;

@RestController
@RequestMapping("/api/v1/mega")
@CrossOrigin(
  origins = { "https://fraxianime.vercel.app", "http://localhost:4200" },
  allowedHeaders = "**"
)
public class MegaController {
  @Autowired
  private MegaService megaService;

  @GetMapping("/")
  public ResponseEntity<?> isValidMegaLink(@RequestBody LinkDTO data) {
    Boolean response = this.megaService.isValidMegaLink(data.getUrl());
    return new ResponseEntity<>(response, HttpStatus.OK);
  }
}
