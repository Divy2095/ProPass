import { FastifyInstance } from 'fastify';
import { UserController } from '../controllers/user.controller.js';
import { authenticate } from '../middleware/auth.middleware.js';

export async function userRoutes(fastify: FastifyInstance) {
  fastify.get('/profile', { preHandler: authenticate }, UserController.getProfile);
  fastify.put('/profile', { preHandler: authenticate }, UserController.updateProfile);
}
