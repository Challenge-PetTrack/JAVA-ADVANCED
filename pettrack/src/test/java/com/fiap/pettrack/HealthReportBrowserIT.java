package com.fiap.pettrack;

import com.fiap.pettrack.config.SecurityConfig;
import com.fiap.pettrack.dto.*;
import com.fiap.pettrack.model.*;
import com.fiap.pettrack.model.enums.PerfilEnum;
import com.fiap.pettrack.repository.IUsuarioRepository;
import com.fiap.pettrack.service.*;
import com.fiap.pettrack.service.security.CustomUserDetailsService;
import com.fiap.pettrack.web.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named="PETTRACK_BROWSER_TESTS",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,classes=HealthReportBrowserIT.Config.class,
        properties={"server.address=127.0.0.1","spring.main.banner-mode=off","spring.thymeleaf.cache=false"})
class HealthReportBrowserIT {
    @LocalServerPort int port;

    @Test void fluxoNoNavegadorComScreenshots() throws Exception {
        String node=System.getProperty("pettrack.browser.node","node");
        var processo=new ProcessBuilder(node,"src/test/browser/health-report.cjs","http://127.0.0.1:"+port,
                "target/screenshots").redirectErrorStream(true)
                .redirectOutput(Path.of("target/health-report-browser.log").toFile());
        String modules=System.getProperty("pettrack.browser.modules");
        if(modules!=null) processo.environment().put("NODE_PATH",modules);
        var p=processo.start();
        if(!p.waitFor(90,TimeUnit.SECONDS)){p.destroyForcibly();fail("Tempo de teste do navegador esgotado");}
        assertEquals(0,p.exitValue(),"Consulte target/health-report-browser.log");
    }

    @Configuration(proxyBeanMethods=false)
    @EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class,HibernateJpaAutoConfiguration.class,
            JpaRepositoriesAutoConfiguration.class,FlywayAutoConfiguration.class})
    @Import({HealthReportWebController.class,HealthReportView.class,SecurityConfig.class,
            CustomUserDetailsService.class,AuthController.class,Home.class})
    static class Config {
        @Bean IUsuarioRepository usuarios() {
            var repo=mock(IUsuarioRepository.class);
            var u=new Usuario();u.setId(1L);u.setNome("Tutor de demonstração");u.setEmail("tutor@example.invalid");
            u.setRole(PerfilEnum.ROLE_TUTOR);u.setSenha(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("somente-teste"));
            when(repo.findByEmail(u.getEmail())).thenReturn(Optional.of(u));return repo;
        }
        @Bean HealthContextService contextos() {
            var s=mock(HealthContextService.class);
            when(s.autorizar(anyLong(),any())).thenAnswer(inv->{
                long id=inv.getArgument(0);
                if(id<1 || id>3) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                var pet=new Pet();pet.setId(id);pet.setNome(id==1 ? "Pet de teste" : id==2 ? "Pet sem relatórios" : "Pet com serviço indisponível");
                return pet;
            });return s;
        }
        @Bean HealthReportService relatorios() {
            var service=mock(HealthReportService.class);
            Map<UUID,HealthReportHistoryDTO.Detalhe> dados=new ConcurrentHashMap<>();
            for(int i=0;i<12;i++) {
                var r=exemplo(new UUID(0,i+1),false);
                dados.put(r.idGeracao(),new HealthReportHistoryDTO.Detalhe(1L,r.geradoEm().minusDays(12-i),r,HealthReportFixtures.contexto()));
            }
            when(service.listar(anyLong(),anyInt(),anyInt(),any())).thenAnswer(inv->{
                long pet=inv.getArgument(0);int page=inv.getArgument(1),size=inv.getArgument(2);
                if(pet<1 || pet>3) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                var all=pet==1 ? dados.values().stream().sorted(Comparator.comparing(HealthReportHistoryDTO.Detalhe::salvoEm).reversed()).toList() : List.<HealthReportHistoryDTO.Detalhe>of();
                var itens=all.stream().skip((long)page*size).limit(size).map(d->{var r=d.relatorio();return new HealthReportHistoryDTO.Resumo(r.idGeracao(),1L,r.geradoEm(),d.salvoEm(),r.provedor(),r.modelo(),r.simulado(),r.relatorio().nivelAtencao());}).toList();
                return new HealthReportHistoryDTO.Pagina(itens,page,size,all.size());
            });
            when(service.buscar(anyLong(),any(),any())).thenAnswer(inv->{
                if(!Long.valueOf(1).equals(inv.getArgument(0))) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                var d=dados.get((UUID)inv.getArgument(1));if(d==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);return d;
            });
            when(service.ultimo(anyLong(),any())).thenAnswer(inv->dados.values().stream().max(Comparator.comparing(HealthReportHistoryDTO.Detalhe::salvoEm)).orElseThrow());
            when(service.gerar(anyLong(),any())).thenAnswer(inv->{
                if(!Long.valueOf(1).equals(inv.getArgument(0))) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
                Thread.sleep(900);
                var r=exemplo(UUID.randomUUID(),true);
                dados.put(r.idGeracao(),new HealthReportHistoryDTO.Detalhe(1L,java.time.OffsetDateTime.now(),r,HealthReportFixtures.contexto()));return r;
            });return service;
        }
        private static HealthReportDTO exemplo(UUID id,boolean mock) {
            var r=HealthReportFixtures.resultado();
            return new HealthReportDTO(id,1L,r.geradoEm(),r.contextoGeradoEm(),mock?"mock":"gemini",mock?"simulacao":r.modelo(),r.versaoPrompt(),mock,true,
                new HealthReportDTO.Relatorio("As duas leituras disponíveis registraram temperatura média de 39 °C e máxima de 40 °C. O BCS passou de 5 para 6, com tendência de aumento. Os registros merecem revisão com o veterinário. Dados fictícios para teste da interface.",HealthReportDTO.NivelAtencao.MODERADO,
                    List.of(new HealthReportDTO.Motivo("O BCS registrado aumentou em relação à avaliação anterior.",List.of("bcs")),new HealthReportDTO.Motivo("A amostragem dos sensores é pequena e precisa ser contextualizada.",List.of("iotRecente"))),
                    List.of("Discutir a evolução da condição corporal na próxima avaliação veterinária.","Conferir as leituras do sensor com a clínica.","Manter os registros de saúde atualizados."),
                    List.of("Apenas duas leituras foram fornecidas.","Clima indisponível e histórico clínico sem eventos no período.","Dados sintéticos; não representam paciente real.","Não substitui avaliação veterinária.")));
        }
    }
    @Controller static class Home {
        @GetMapping("/dashboard") String home(){return "redirect:/relatorios/pets/1";}
    }
}
