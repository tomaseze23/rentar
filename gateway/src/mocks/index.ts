import { startVehiculoMock } from './vehiculo.mock';
import { startClienteMock } from './cliente.mock';
import { startAlquilerMock } from './alquiler.mock';

// MOCKS_OMITIR=cliente,vehiculo,alquiler  -> no levanta esos mocks (útil cuando el servicio real ya corre en ese puerto)
const omitir = (process.env.MOCKS_OMITIR ?? '').split(',').map((s) => s.trim());

export async function startMocks() {
  const arrancados: string[] = [];
  const tareas: Promise<unknown>[] = [];

  if (!omitir.includes('vehiculo')) { tareas.push(startVehiculoMock()); arrancados.push('Vehicle :9091'); }
  if (!omitir.includes('cliente')) { tareas.push(startClienteMock()); arrancados.push('Customer :9092'); }
  if (!omitir.includes('alquiler')) { tareas.push(startAlquilerMock()); arrancados.push('Rental :9093'); }

  await Promise.all(tareas);
  return arrancados;
}

if (require.main === module) {
  startMocks().then((arrancados) => {
    console.log(`Mocks gRPC levantados: ${arrancados.join(', ') || 'ninguno'}`);
  });
}