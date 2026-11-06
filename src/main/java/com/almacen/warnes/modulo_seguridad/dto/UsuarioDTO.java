package com.almacen.warnes.modulo_seguridad.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioDTO {
    @JsonProperty("usuarioId")
    private Integer usuarioId;

    @JsonProperty("usuario")
    private String usuario;

    @JsonProperty("nombres")
    private String nombres;

    @JsonProperty("apellidos")
    private String apellidos;

    @JsonProperty("rolId")
    private Integer rolId;

    @JsonProperty("rolNombre")
    private String rolNombre;

    @JsonProperty("nombreCompleto")
    private String nombreCompleto;

    @JsonProperty("marcaBaja")
    private Boolean marcaBaja;

    @JsonProperty("fechaCreacion")
    private LocalDateTime fechaCreacion;

    @JsonProperty("usuarioCreacion")
    private Integer usuarioCreacion;

    @JsonProperty("fechaModificacion")
    private LocalDateTime fechaModificacion;

    @JsonProperty("usuarioModificacion")
    private Integer usuarioModificacion;

    @JsonProperty("usuario_id")
    public Integer getUsuario_id() { return usuarioId; }

    @JsonProperty("rol_id")
    public Integer getRol_id() { return rolId; }

    @JsonProperty("rol_nombre")
    public String getRol_nombre() { return rolNombre; }

    @JsonProperty("nombre_completo")
    public String getNombre_completo() { return nombreCompleto; }
}
