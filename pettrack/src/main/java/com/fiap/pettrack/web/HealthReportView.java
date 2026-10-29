package com.fiap.pettrack.web;

import com.fiap.pettrack.dto.HealthReportDTO.NivelAtencao;
import org.springframework.stereotype.Component;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class HealthReportView {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR"));
    public String data(OffsetDateTime data) { return data == null ? "Não informado" : DATA.format(data.atZoneSameInstant(ZoneId.of("America/Sao_Paulo"))); }
    public String local(LocalDateTime data) { return data == null ? "Não informado" : DATA.format(data); }
    public String nivel(NivelAtencao nivel) {
        return switch(nivel) { case BAIXO -> "Baixo"; case MODERADO -> "Moderado"; case ALTO -> "Alto"; case INDETERMINADO -> "Indeterminado"; };
    }
    public String fonte(String fonte) {
        return switch(fonte) { case "pet" -> "Cadastro"; case "iotRecente" -> "Sensores IoT"; case "bcs" -> "Condição corporal"; case "historico" -> "Histórico clínico"; case "clima" -> "Clima"; default -> "Não informado"; };
    }
    public String tendencia(String tendencia) {
        return switch(tendencia) { case "SUBINDO" -> "Em aumento"; case "DESCENDO" -> "Em redução"; case "ESTAVEL" -> "Estável"; default -> "Histórico insuficiente"; };
    }
    public String medida(Number valor, String unidade) {
        return valor == null ? "Não informado" : String.format(Locale.forLanguageTag("pt-BR"), "%.1f%s", valor.doubleValue(), unidade);
    }
}
