package com.rentar.rentar.services;


import com.rentar.rentar.entities.Vehiculo;

import java.util.List;


public interface VehiculoService {
    Vehiculo crear(Vehiculo v);
    Vehiculo modificar(Long id, Vehiculo datos);
    void bajaLogica(Long id);
    List<Vehiculo> listar();
    Vehiculo consultar(Long id);

}
