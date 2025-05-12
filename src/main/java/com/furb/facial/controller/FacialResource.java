package com.furb.facial.controller;

import com.furb.facial.service.FacialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/facial")
public class FacialResource {

    @Autowired
    @Lazy
    private FacialService facialService;

    @GetMapping("/analisar")
    public void analisarImagem() {
        facialService.getFacialInformationByAws();
    }

    @GetMapping("/comparar")
    public void compararImagens() {
        facialService.comparingFacialImages();
    }
}
