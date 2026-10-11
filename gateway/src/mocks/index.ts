import { startVehiculoMock } from './vehiculo.mock';
import { startClienteMock } from './cliente.mock';
import { startAlquilerMock } from './alquiler.mock';

export async function startMocks(){
    return Promise.all([startVehiculoMock(), startClienteMock(), startAlquilerMock()]);
}

if(require.main === module) {
    startMocks().then(() => {
        console.log('Mocks gRPC levantados: Vehicle :9091, Customer :9092, Rental :9093');
    });
}