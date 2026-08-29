import { FastifyInstance } from 'fastify';
import { EventController } from '../controllers/event.controller.js';

export async function eventRoutes(fastify: FastifyInstance) {
  // Validate QR payload and retrieve matching active event
  fastify.post('/validate-qr', EventController.validateQr);

  // Retrieve event metadata by slug or UUID
  fastify.get('/:eventId', EventController.getEvent);
}
