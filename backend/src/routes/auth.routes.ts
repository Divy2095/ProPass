import { FastifyInstance } from 'fastify';
import { AuthController } from '../controllers/auth.controller.js';
import { authenticate } from '../middleware/auth.middleware.js';

export async function authRoutes(fastify: FastifyInstance) {
  // Public authentication endpoints
  fastify.post('/register', AuthController.register);
  fastify.post('/login', AuthController.login);
  fastify.post('/refresh', AuthController.refresh);
  fastify.post('/logout', AuthController.logout);

  // Protected user session endpoint
  fastify.get('/me', { preHandler: authenticate }, AuthController.getMe);
}
