package com.rentar.rentar.services;


import com.rentar.rentar.dtos.VehiculoRequest;
import com.rentar.rentar.dtos.VehiculoResponse;
import com.rentar.rentar.dtos.VehiculoUpdateRequest;

import java.util.List;


public interface VehiculoService {
    VehiculoResponse crear(VehiculoRequest request);
    VehiculoResponse modificar(Long id, VehiculoUpdateRequest request);
    void bajaLogica(Long id);
    VehiculoResponse reactivar(Long id);
    List<VehiculoResponse> listar(Boolean activo);
    VehiculoResponse consultar(Long id);

}
