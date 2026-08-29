import { FastifyInstance } from 'fastify';
import { RegistrationController } from '../controllers/registration.controller.js';
import { authenticate } from '../middleware/auth.middleware.js';

export async function registrationRoutes(fastify: FastifyInstance) {
  // Submit new event registration (Authenticated)
  fastify.post('/', { preHandler: authenticate }, RegistrationController.createRegistration);

  // Retrieve current user's event registrations (Authenticated)
  fastify.get('/my', { preHandler: authenticate }, RegistrationController.getMyRegistrations);
}
