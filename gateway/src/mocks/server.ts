import * as grpc from '@grpc/grpc-js';

// error gRPC con código y mensaje, igual que lo devolvería un servicio real.
export function grpcError(code: grpc.status, message: string): grpc.ServiceError {
    return Object.assign(new Error(message), { code, details: message, metadata: new grpc.Metadata() });
}

//levanta un servidor gRPC en la dirección indicada
export function serve(addr: string, service: grpc.ServiceDefinition, impl: grpc.UntypedServiceImplementation) {
    return new Promise<grpc.Server>((resolve, reject) => {
        const server = new grpc.Server();
        server.addService(service, impl);
        server.bindAsync(addr, grpc.ServerCredentials.createInsecure(), (err) => {
            if(err) return reject(err);
            resolve(server);
        });
    });
}
