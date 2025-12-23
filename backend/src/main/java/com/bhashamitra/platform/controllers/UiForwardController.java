package com.bhashamitra.platform.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UiForwardController {

    // Serve the SPA entry point for /ui and any /ui/<something> (one segment)
    @GetMapping({"/ui", "/ui/", "/ui/{path:[^\\.]*}"})
    public String forwardUiShallow() {
        return "forward:/ui/index.html";
    }

    // Serve SPA entry point for deeper routes, e.g. /ui/a/b/c
    // This catches everything under /ui/** that doesn't look like a file.
    @GetMapping("/ui/**")
    public String forwardUiDeep() {
        return "forward:/ui/index.html";
    }
}
