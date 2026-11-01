package com.fiap.pettrack.config;

import com.fiap.pettrack.model.Clinica;
import com.fiap.pettrack.model.Tutor;
import com.fiap.pettrack.model.Usuario;
import com.fiap.pettrack.model.enums.PerfilEnum;
import com.fiap.pettrack.repository.IClinicaRepository;
import com.fiap.pettrack.repository.ITutorRepository;
import com.fiap.pettrack.repository.IUsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Lazy
    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Lazy
    @Autowired
    private ITutorRepository tutorRepository;

    @Lazy
    @Autowired
    private IClinicaRepository clinicaRepository;

    @Lazy
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        CompletableFuture.runAsync(this::inicializarDadosSeNecessario);
    }

    private void inicializarDadosSeNecessario() {
        try {
            if (usuarioRepository.count() > 0) {
                log.info("Dados iniciais ja existentes no banco. Inicializacao concluida.");
                return;
            }

            if (!usuarioRepository.existsByEmail("admin@pettrack.com")) {
                criarUsuario("admin@pettrack.com", "admin123", "Administrador PetTrack", PerfilEnum.ROLE_ADMIN, null, null);
            }

            if (!usuarioRepository.existsByEmail("vet@pettrack.com")) {
                Clinica clinica = clinicaRepository.findAll(PageRequest.of(0, 1)).stream().findFirst().orElse(null);
                criarUsuario("vet@pettrack.com", "vet123", "Dra. Camila Veterinária", PerfilEnum.ROLE_VET, null, clinica);
            }

            if (!usuarioRepository.existsByEmail("ana.silva@icloud.com")) {
                Tutor tutor = tutorRepository.findAll(PageRequest.of(0, 1)).stream().findFirst().orElse(null);
                criarUsuario("ana.silva@icloud.com", "tutor123", "Ana Paula Silva (Tutor)", PerfilEnum.ROLE_TUTOR, tutor, null);
            }
            log.info("Carga inicial de usuarios concluida com sucesso.");
        } catch (Exception e) {
            log.warn("Aviso ao verificar dados iniciais no startup: {}", e.getMessage());
        }
    }

    private void criarUsuario(String email, String senhaPlana, String nome, PerfilEnum role, Tutor tutor, Clinica clinica) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setNome(nome);
        usuario.setSenha(passwordEncoder.encode(senhaPlana));
        usuario.setRole(role);
        usuario.setAtivo("S");
        if (tutor != null) {
            usuario.setTutor(tutor);
        }
        if (clinica != null) {
            usuario.setClinica(clinica);
        }
        usuarioRepository.save(usuario);
    }
}
