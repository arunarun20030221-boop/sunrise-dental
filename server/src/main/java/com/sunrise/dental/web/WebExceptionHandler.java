package com.sunrise.dental.web;

import com.sunrise.dental.exception.BillAlreadyIssuedException;
import com.sunrise.dental.exception.DoubleBookingException;
import com.sunrise.dental.exception.NotFoundException;
import com.sunrise.dental.exception.OutsideOpeningHoursException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Renders a readable page when something goes wrong in the browser channel, instead of a
 * Tomcat stack trace.
 *
 * <p>Scoped to the {@code web} package so the API keeps its own JSON handler - the console
 * client needs a status code and a JSON body, not HTML.</p>
 */
@ControllerAdvice(basePackages = "com.sunrise.dental.web")
public class WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(WebExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public String notFound(NotFoundException e, Model model) {
        model.addAttribute("heading", "Not found");
        model.addAttribute("detail", e.getMessage());
        return "error";
    }

    @ExceptionHandler({DoubleBookingException.class, OutsideOpeningHoursException.class,
            BillAlreadyIssuedException.class})
    public String businessRule(RuntimeException e, Model model) {
        model.addAttribute("heading", "That action was not allowed");
        model.addAttribute("detail", e.getMessage());
        return "error";
    }

    /**
     * Last resort. The message shown to staff is deliberately generic - an internal exception
     * message can disclose table names or file paths - while the full detail goes to the log
     * where support can find it.
     */
    @ExceptionHandler(Exception.class)
    public String unexpected(Exception e, Model model) {
        log.error("Unhandled error in the web channel", e);
        model.addAttribute("heading", "Something went wrong");
        model.addAttribute("detail",
                "The clinic system hit an unexpected problem. Please try again, and tell IT "
                        + "if it keeps happening.");
        return "error";
    }
}
