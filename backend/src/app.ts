import cors from '@fastify/cors';
import helmet from '@fastify/helmet';
import Fastify, { FastifyInstance } from 'fastify';
import { healthRoutes } from './routes/health.routes.js';

export async function buildApp(): Promise<FastifyInstance> {
  const app = Fastify({
    logger: {
      level: process.env.NODE_ENV === 'test' ? 'silent' : 'info',
    },
  });

  // Security headers & CORS
  await app.register(helmet, {
    contentSecurityPolicy: false,
  });

  await app.register(cors, {
    origin: true,
    credentials: true,
  });

  // Root welcome endpoint
  app.get('/', async () => {
    return {
      name: 'ProPass Digital Identity System API',
      version: '1.0.0',
      status: 'operational',
      docs: '/api/docs',
    };
  });

  // Register Routes
  await app.register(healthRoutes);

  return app;
}
