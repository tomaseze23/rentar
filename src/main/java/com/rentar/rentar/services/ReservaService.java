package com.rentar.rentar.services;

import com.rentar.rentar.dtos.ReservaRequest;
import com.rentar.rentar.dtos.ReservaResponse;

public interface ReservaService {
    ReservaResponse crearReserva(ReservaRequest request);
    ReservaResponse cancelarReserva(Long id);
}
