package com.rentar.rentar.dtos;

import com.rentar.rentar.entities.EstadoReserva;
import com.rentar.rentar.entities.TipoVehiculo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReservaFiltro {
    private Long clienteId;
    private Long vehiculoId;
    private TipoVehiculo tipoVehiculo;
    private EstadoReserva estado;
    private String fechaDesde;
    private String fechaHasta;
}
