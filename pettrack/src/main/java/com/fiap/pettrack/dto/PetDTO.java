package com.fiap.pettrack.dto;

import com.fiap.pettrack.model.enums.SexoPetEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "DTO da entidade Pet.")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PetDTO {

    @NotBlank(message = "O nome é obrigatório")
    @Schema(description = "Nome do pet.", example = "Thor")
    private String nome;

    @NotBlank(message = "A espécie é obrigatória")
    @Schema(description = "Espécie do pet.", example = "Cão")
    private String especie;

    @Schema(description = "Raça do pet.", example = "Golden Retriever")
    private String raca;

    @Schema(description = "Sexo do pet: M ou F.", example = "M")
    private SexoPetEnum sexo;

    @Schema(description = "Idade do pet em anos.", example = "3.0")
    private Double idade;

    @Schema(description = "Peso do pet em kg.", example = "12.5")
    private Double peso;

    @NotNull(message = "O ID do tutor é obrigatório")
    @Schema(description = "ID do tutor.", example = "1")
    private Long idTutor;

    @NotNull(message = "O ID da clínica é obrigatório")
    @Schema(description = "ID da clínica.", example = "1")
    private Long idClinica;
}