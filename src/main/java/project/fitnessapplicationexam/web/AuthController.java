package project.fitnessapplicationexam.web;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import project.fitnessapplicationexam.user.service.UserService;
import project.fitnessapplicationexam.user.form.RegisterForm;

@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            Model model) {
        if (error != null) {
            log.warn("Login attempt failed");
            model.addAttribute("errorMessage", "Invalid username or password.");
        }
        return "login";
    }
    
    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("form") @Valid RegisterForm form,
                          BindingResult bindingResult,
                          Model model) {
        if (bindingResult.hasErrors()) {
            log.warn("Registration form validation failed");
            return "register";
        }
        
        userService.register(form.getUsername(), form.getPassword(), form.getEmail(),
                form.getFirstName(), form.getLastName());
        log.info("User registered successfully: {}", form.getUsername());
        model.addAttribute("msg", "Registration successful. Please log in.");
        return "login";
    }
}
