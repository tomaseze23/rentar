import { startVehiculoMock } from './vehiculo.mock';

export async function startMocks(){
    return Promise.all([startVehiculoMock()]);
}

if(require.main === module) {
    startMocks().then(() => {
        console.log('Mocks gRPC levantados: Vehicle :9091');
    });
}