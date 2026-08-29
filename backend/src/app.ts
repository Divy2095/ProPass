import cors from '@fastify/cors';
import helmet from '@fastify/helmet';
import Fastify, { FastifyInstance } from 'fastify';
import { authRoutes } from './routes/auth.routes.js';
import { dashboardRoutes } from './routes/dashboard.routes.js';
import { healthRoutes } from './routes/health.routes.js';
import { userRoutes } from './routes/user.routes.js';

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

  // Register Health Routes
  await app.register(healthRoutes);

  // Register Authentication Routes
  await app.register(authRoutes, { prefix: '/api/v1/auth' });
  await app.register(authRoutes, { prefix: '/api/auth' });

  // Register User / Profile Routes
  await app.register(userRoutes, { prefix: '/api/v1/users' });
  await app.register(userRoutes, { prefix: '/api/users' });

  // Register Dashboard Routes
  await app.register(dashboardRoutes, { prefix: '/api/v1/dashboard' });
  await app.register(dashboardRoutes, { prefix: '/api/dashboard' });

  return app;
}
