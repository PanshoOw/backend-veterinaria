package com.duoc.backend.Care;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/care")
public class CareController {

    private CareRepository careRepository;

    public CareController(CareRepository careRepository) {
        this.careRepository = careRepository;
    }

    @GetMapping
    public List<Care> getAllCares() {
        return (List<Care>) careRepository.findAll();
    }

    @GetMapping("/{id}")
    public Care getCareById(@PathVariable Long id) {
        return careRepository.findById(id).orElse(null);
    }

    @PostMapping
    public Care saveCare(@RequestBody Care service) {
        return careRepository.save(service);
    }

    @DeleteMapping("/{id}")
    public void deleteCare(@PathVariable Long id) {
        careRepository.deleteById(id);
    }
}