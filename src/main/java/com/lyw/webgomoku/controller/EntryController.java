package com.lyw.webgomoku.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class EntryController {

    @GetMapping("/chess")
    public String chess() {
        return "gomoku";
    }

}
