package com.sunrise.dental.web;

import com.sunrise.dental.config.ClinicProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Requirement 5 - the help section, with step-by-step instructions for new staff.
 */
@Controller
public class HelpController {

    private final ClinicProperties clinic;

    public HelpController(ClinicProperties clinic) {
        this.clinic = clinic;
    }

    @ModelAttribute("currentUser")
    public SessionUser currentUser(HttpServletRequest request) {
        return CurrentUser.require(request);
    }

    @ModelAttribute("clinic")
    public ClinicProperties clinic() {
        return clinic;
    }

    @GetMapping("/help")
    public String help() {
        return "help";
    }
}
