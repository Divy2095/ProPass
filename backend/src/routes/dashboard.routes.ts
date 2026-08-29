import { FastifyInstance } from 'fastify';
import { UserController } from '../controllers/user.controller.js';
import { authenticate } from '../middleware/auth.middleware.js';

export async function dashboardRoutes(fastify: FastifyInstance) {
  fastify.get('/', { preHandler: authenticate }, UserController.getDashboard);
}
