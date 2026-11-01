package com.lms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PublicController {

    @GetMapping("/privacidad")
    public String privacidad() {
        return "public/privacidad";
    }

    @GetMapping("/terminos")
    public String terminos() {
        return "public/terminos";
    }

    @GetMapping("/aviso-legal")
    public String avisoLegal() {
        return "public/aviso-legal";
    }
}
