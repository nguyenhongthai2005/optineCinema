package com.opticine.controller;

import com.opticine.dto.combo.ComboResponse;
import com.opticine.service.ComboService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/combos")
@RequiredArgsConstructor
public class ComboController {
    private final ComboService comboService;

    @GetMapping("/active")
    public ResponseEntity<List<ComboResponse>> activeCombos() {
        return ResponseEntity.ok(comboService.activeCombos());
    }
}
