package com.marketplace.user.controller;

import com.marketplace.user.dto.UserRequest;
import com.marketplace.user.model.User;
import com.marketplace.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public User crearUsuario(@Valid @RequestBody UserRequest request) {
        User user = new User();

        user.setNombre(request.getNombre());
        user.setEmail(request.getEmail());
        user.setRol(request.getRol());

        return userService.crearUsuario(user);
    }

    @GetMapping
    public List<User> obtenerUsuarios() {
        return userService.obtenerUsuarios();
    }
    @GetMapping("/{id}")
    public User obtenerUsuarioPorId(@PathVariable Long id) {
        return userService.obtenerUsuarioPorId(id);
    }
}