package com.bhashamitra.platform.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaForwardController {

    // Forward all routes (except those containing a period) to index.html
    // so that React Router can handle them.
    @RequestMapping(value = {
            "/{path:[^\\.]*}",
            "/**/{path:[^\\.]*}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
