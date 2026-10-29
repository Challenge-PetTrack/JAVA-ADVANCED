package com.fiap.pettrack;

import com.fiap.pettrack.config.SecurityConfig;
import com.fiap.pettrack.dto.*;
import com.fiap.pettrack.model.*;
import com.fiap.pettrack.model.enums.PerfilEnum;
import com.fiap.pettrack.repository.IUsuarioRepository;
import com.fiap.pettrack.service.*;
import com.fiap.pettrack.service.security.CustomUserDetailsService;
import com.fiap.pettrack.web.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthReportWebController.class)
@ContextConfiguration(classes={HealthReportWebController.class,HealthReportView.class,SecurityConfig.class,CustomUserDetailsService.class})
class HealthReportWebControllerTest {
    @Autowired MockMvc mvc;
    @MockBean IUsuarioRepository usuarios;
    @MockBean HealthReportService service;
    @MockBean HealthContextService contextos;
    private Usuario usuario;
    private Pet pet;
    private HealthReportDTO report;
    private HealthReportHistoryDTO.Detalhe detalhe;

    @BeforeEach void setup() {
        usuario=new Usuario();usuario.setId(1L);usuario.setEmail("tutor@example.invalid");usuario.setRole(PerfilEnum.ROLE_TUTOR);
        pet=new Pet();pet.setId(1L);pet.setNome("Pet de teste");pet.setEspecie("Cão");
        report=HealthReportFixtures.resultado().comoPersistido();
        detalhe=new HealthReportHistoryDTO.Detalhe(1L,report.geradoEm(),report,HealthReportFixtures.contexto());
        when(usuarios.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(contextos.autorizar(1L,usuario)).thenReturn(pet);
        when(service.listar(1L,0,10,usuario)).thenReturn(new HealthReportHistoryDTO.Pagina(List.of(),0,10,0));
        when(service.buscar(1L,report.idGeracao(),usuario)).thenReturn(detalhe);
        when(service.ultimo(1L,usuario)).thenReturn(detalhe);
        when(service.gerar(1L,usuario)).thenReturn(report);
    }

    @Test void historicoVazioRenderizaFormularioCsrf() throws Exception {
        mvc.perform(get("/relatorios/pets/1").with(user(usuario))).andExpect(status().isOk())
                .andExpect(view().name("health-report/historico"))
                .andExpect(content().string(containsString("O primeiro relatório começa aqui")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("/relatorios/pets/1/gerar")))
                .andExpect(header().string("Cache-Control","no-store"));
        verify(service,never()).gerar(any(),any());
    }

    @ParameterizedTest @ValueSource(strings={"ROLE_TUTOR","ROLE_VET","ROLE_ADMIN"})
    void perfisAutorizadosRenderizamDetalheComFontesELimitacoes(String role) throws Exception {
        usuario.setRole(PerfilEnum.valueOf(role));
        mvc.perform(get("/relatorios/pets/1/"+report.idGeracao()).with(user(usuario)))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Próximos cuidados")))
                .andExpect(content().string(containsString("Condição corporal")))
                .andExpect(content().string(containsString("Limites desta análise")))
                .andExpect(content().string(containsString("Dados usados nesta análise")))
                .andExpect(content().string(containsString("Salvo no histórico")))
                .andExpect(content().string(containsString("27/10/2026 às 12:00")));
    }

    @Test void historicoRenderizaSimulacaoEPaginacao() throws Exception {
        var resumo=new HealthReportHistoryDTO.Resumo(report.idGeracao(),1L,report.geradoEm(),report.geradoEm(),"mock","simulacao",true,HealthReportDTO.NivelAtencao.INDETERMINADO);
        when(service.listar(1L,1,10,usuario)).thenReturn(new HealthReportHistoryDTO.Pagina(List.of(resumo),1,10,25));
        mvc.perform(get("/relatorios/pets/1?pagina=1").with(user(usuario))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Simulação")))
                .andExpect(content().string(containsString("Indeterminado")))
                .andExpect(content().string(containsString("pagina=0")))
                .andExpect(content().string(containsString("pagina=2")));
    }

    @Test void sucessoPostRedirecionaParaGeracaoSalvaSemReenvioNoRefresh() throws Exception {
        mvc.perform(post("/relatorios/pets/1/gerar").with(user(usuario)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/relatorios/pets/1/"+report.idGeracao()))
                .andExpect(flash().attribute("mensagemSucesso","Relatório gerado e salvo no histórico."));
        verify(service,times(1)).gerar(1L,usuario);
    }

    @Test void semCsrfNaoGera() throws Exception {
        mvc.perform(post("/relatorios/pets/1/gerar").with(user(usuario))).andExpect(status().isForbidden());
        verifyNoInteractions(service,contextos);
    }

    @Test void semSessaoNaoAcessaHistoricoOuDetalhe() throws Exception {
        mvc.perform(get("/relatorios/pets/1")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/relatorios/pets/1/"+report.idGeracao())).andExpect(status().is3xxRedirection());
        mvc.perform(post("/relatorios/pets/1/gerar").with(csrf())).andExpect(status().is3xxRedirection());
        verifyNoInteractions(service,contextos);
    }

    @ParameterizedTest @ValueSource(ints={429,502,503,504,409})
    void falhasDeGeracaoExibemOrientacaoSemVazarErro(int status) throws Exception {
        when(service.gerar(1L,usuario)).thenThrow(new ResponseStatusException(HttpStatus.valueOf(status),"SEGREDO_PROVEDOR"));
        var resultado=mvc.perform(post("/relatorios/pets/1/gerar").with(user(usuario)).with(csrf()))
                .andExpect(redirectedUrl("/relatorios/pets/1"))
                .andExpect(flash().attribute("mensagemErro",not(containsString("SEGREDO")))).andReturn();
        mvc.perform(get("/relatorios/pets/1").with(user(usuario)).flashAttrs(resultado.getFlashMap()))
                .andExpect(status().isOk()).andExpect(content().string(not(containsString("SEGREDO_PROVEDOR"))));
        verify(service,times(1)).gerar(any(),any());
    }

    @Test void acessoNegadoNaoExpoeNomeOuHistorico() throws Exception {
        when(contextos.autorizar(1L,usuario)).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        mvc.perform(get("/relatorios/pets/1").with(user(usuario))).andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Você não tem acesso")))
                .andExpect(content().string(not(containsString("Pet de teste"))));
        verifyNoInteractions(service);
    }

    @Test void detalheInexistenteRetornaPagina404() throws Exception {
        when(service.buscar(1L,report.idGeracao(),usuario)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mvc.perform(get("/relatorios/pets/1/"+report.idGeracao()).with(user(usuario))).andExpect(status().isNotFound())
                .andExpect(content().string(containsString("não foi encontrado")));
    }

    @Test void ultimoRedirecionaParaRegistroSalvo() throws Exception {
        mvc.perform(get("/relatorios/pets/1/latest").with(user(usuario)))
                .andExpect(redirectedUrl("/relatorios/pets/1/"+report.idGeracao()));
    }

    @Test void htmlDaIaENomeSaoEscapados() throws Exception {
        var ataque="<script>alert('xss')</script>";
        pet.setNome(ataque);
        mvc.perform(get("/relatorios/pets/1").with(user(usuario))).andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(not(containsString(ataque))));
        var r=report.relatorio();
        var malicioso=new HealthReportDTO(report.idGeracao(),1L,report.geradoEm(),report.contextoGeradoEm(),"mock","simulacao",report.versaoPrompt(),true,true,
                new HealthReportDTO.Relatorio(ataque,r.nivelAtencao(),r.motivos(),r.recomendacoes(),r.limitacoes()));
        when(service.buscar(1L,report.idGeracao(),usuario)).thenReturn(new HealthReportHistoryDTO.Detalhe(1L,report.geradoEm(),malicioso,detalhe.contexto()));
        mvc.perform(get("/relatorios/pets/1/"+report.idGeracao()).with(user(usuario)))
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(not(containsString(ataque))))
                .andExpect(content().string(containsString("Relatório de demonstração")));
    }

    @Test void dadosAusentesNaoViraramZero() throws Exception {
        var tree=HealthReportFixtures.mapper().valueToTree(detalhe.contexto());
        ((com.fasterxml.jackson.databind.node.ObjectNode)tree.get("bcs")).putNull("ultima");
        ((com.fasterxml.jackson.databind.node.ObjectNode)tree.get("iotRecente")).putNull("temperaturaMedia");
        var contexto=HealthReportFixtures.mapper().treeToValue(tree,HealthContextDTO.class);
        when(service.buscar(1L,report.idGeracao(),usuario)).thenReturn(new HealthReportHistoryDTO.Detalhe(1L,report.geradoEm(),report,contexto));
        mvc.perform(get("/relatorios/pets/1/"+report.idGeracao()).with(user(usuario)))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Sem avaliação registrada")))
                .andExpect(content().string(containsString("Não informado")));
    }
}
