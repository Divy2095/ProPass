import { FastifyInstance } from 'fastify';
import { PassController } from '../controllers/pass.controller.js';
import { authenticate } from '../middleware/auth.middleware.js';

export async function passRoutes(fastify: FastifyInstance) {
  // Retrieve authenticated user's digital pass
  fastify.get('/me', { preHandler: authenticate }, PassController.getMyPass);
}
