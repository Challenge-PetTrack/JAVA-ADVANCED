package com.fiap.pettrack.web;

import com.fiap.pettrack.model.Usuario;
import com.fiap.pettrack.model.enums.PerfilEnum;
import com.fiap.pettrack.repository.IUsuarioRepository;
import com.fiap.pettrack.service.HealthContextService;
import com.fiap.pettrack.service.HealthReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.UUID;

@Controller
@RequestMapping("/relatorios/pets/{idPet}")
@RequiredArgsConstructor
public class HealthReportWebController {
    private final HealthReportService relatorios;
    private final HealthContextService contextos;
    private final IUsuarioRepository usuarios;
    private final HealthReportView view;

    @ModelAttribute
    public void comum(Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        model.addAttribute("view",view);
    }

    @GetMapping
    public String historico(@PathVariable Long idPet, @RequestParam(defaultValue="0") int pagina,
                            Authentication auth, Model model) {
        var usuario=usuario(auth);
        model.addAttribute("pet",contextos.autorizar(idPet,usuario));
        model.addAttribute("historico",relatorios.listar(idPet,pagina,10,usuario));
        model.addAttribute("voltar",usuario.getRole()==PerfilEnum.ROLE_TUTOR ? "/portal-tutor/dashboard" : "/admin/pets");
        return "health-report/historico";
    }

    @GetMapping("/{idGeracao}")
    public String detalhe(@PathVariable Long idPet, @PathVariable UUID idGeracao, Authentication auth, Model model) {
        var detalhe=relatorios.buscar(idPet,idGeracao,usuario(auth));
        model.addAttribute("detalhe",detalhe);
        model.addAttribute("geracao",detalhe.relatorio());
        model.addAttribute("analise",detalhe.relatorio().relatorio());
        model.addAttribute("contexto",detalhe.contexto());
        return "health-report/detalhe";
    }

    @GetMapping("/latest")
    public String ultimo(@PathVariable Long idPet, Authentication auth) {
        var ultimo=relatorios.ultimo(idPet,usuario(auth));
        return "redirect:/relatorios/pets/"+idPet+"/"+ultimo.relatorio().idGeracao();
    }

    @PostMapping("/gerar")
    public String gerar(@PathVariable Long idPet, Authentication auth, RedirectAttributes flash) {
        try {
            var gerado=relatorios.gerar(idPet,usuario(auth));
            flash.addFlashAttribute("mensagemSucesso","Relatório gerado e salvo no histórico.");
            return "redirect:/relatorios/pets/"+idPet+"/"+gerado.idGeracao();
        } catch(ResponseStatusException ex) {
            int status=ex.getStatusCode().value();
            String mensagem=switch(status) {
                case 429 -> "Já há gerações em andamento. Aguarde antes de tentar novamente.";
                case 504 -> "A geração demorou mais que o esperado. Consulte o histórico antes de tentar novamente.";
                case 503, 502 -> "Não foi possível concluir a geração. Consulte o histórico antes de tentar novamente; se o problema continuar, contate a clínica.";
                case 409 -> "Esta geração já foi registrada. Consulte o histórico.";
                default -> null;
            };
            if(mensagem==null) throw ex;
            flash.addFlashAttribute("mensagemErro",mensagem);
            return "redirect:/relatorios/pets/"+idPet;
        }
    }

    @ExceptionHandler(ResponseStatusException.class)
    public String erro(ResponseStatusException ex, Model model, HttpServletResponse response) {
        int status=ex.getStatusCode().value();response.setStatus(status);
        model.addAttribute("codigo",status);
        model.addAttribute("mensagem",switch(status) {
            case 401 -> "Entre novamente para acessar os relatórios.";
            case 403 -> "Você não tem acesso aos relatórios deste pet.";
            case 404 -> "O pet ou relatório solicitado não foi encontrado.";
            case 400 -> "Confira o pet e a página solicitados.";
            default -> "Os relatórios estão indisponíveis no momento. Tente consultar novamente mais tarde.";
        });
        return "health-report/erro";
    }

    private Usuario usuario(Authentication auth) {
        if(auth==null || !auth.isAuthenticated()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return usuarios.findByEmail(auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
