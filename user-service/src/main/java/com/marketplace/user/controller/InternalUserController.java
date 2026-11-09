
package com.marketplace.user.controller;

import com.marketplace.user.dto.InternalUserResponse;
import com.marketplace.user.model.User;
import com.marketplace.user.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public InternalUserResponse obtenerUsuarioInterno(@PathVariable Long id) {

        User user = userService.obtenerUsuarioPorId(id);

        return new InternalUserResponse(
                user.getNombre(),
                user.getEmail()
        );
    }
}
