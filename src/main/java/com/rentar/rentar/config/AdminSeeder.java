package com.rentar.rentar.config;

import com.rentar.rentar.entities.Usuario;
import com.rentar.rentar.repositories.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }
    @Override
    public void run(String... args) {
        if (usuarioRepository.findByEmail("admin@rentar.com").isEmpty()){
            Usuario admin = new Usuario();
            admin.setEmail("admin@rentar.com");
            admin.setPassword(passwordEncoder.encode("admin1234"));
            admin.setRol("ADMINISTRADOR");
            admin.setActivo(true);
            usuarioRepository.save(admin);
            System.out.println(">>> Admin sembrado: admin@rentar.com");
        }
    }

}
