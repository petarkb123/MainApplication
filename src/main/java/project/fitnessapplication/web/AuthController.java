package project.fitnessapplication.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import project.fitnessapplication.user.service.UserService;
import project.fitnessapplication.user.form.RegisterForm;

@Controller
@Validated
public class AuthController {

    private final UserService users;
    public AuthController(UserService users){ this.users = users; }

    @GetMapping("/login")    public String loginPage(){ return "login"; }
    @GetMapping("/register") public String registerForm(){ return "register"; }

    @PostMapping("/register")
    public String register(@ModelAttribute RegisterForm f, Model model) {
        try {
            users.register(f.getUsername(), f.getPassword(), f.getEmail(), f.getFirstName(), f.getLastName());
            model.addAttribute("msg", "Registration successful. Please log in.");
            return "login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "register";
        }
    }
}
