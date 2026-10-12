import fs from 'node:fs';
import path from 'node:path';
import { buildSchema } from 'graphql';
import { createHandler } from 'graphql-http/lib/use/express';
import { leerUsuario } from '../auth';
import { Contexto, rootValue } from './resolvers';

// El schema es el mismo del Hito 1. Se lee de src/ (desde src/graphql o desde dist/graphql la ruta relativa es la misma).
const sdl = fs.readFileSync(path.resolve(__dirname, '../../src/graphql/schema.graphqls'), 'utf8');
const schema = buildSchema(sdl);

export const graphqlHandler = createHandler<Contexto>({
  schema,
  rootValue,
  // El contexto dice quién llama. Si no hay token (o es inválido) queda en null y cada operación
  // protegida responde con un error GraphQL UNAUTHORIZED.
  context: (req): Contexto => {
    try {
      return { usuario: leerUsuario(req.raw.headers.authorization) };
    } catch {
      return { usuario: null };
    }
  },
});