import { FastifyInstance } from 'fastify';
import { UserRole } from '@prisma/client';
import { EventController } from '../controllers/event.controller.js';
import { FormController } from '../controllers/form.controller.js';
import { authenticate } from '../middleware/auth.middleware.js';
import { requireRole } from '../middleware/role.middleware.js';

export async function eventRoutes(fastify: FastifyInstance) {
  // Create event (Organizer only)
  fastify.post(
    '/',
    { preHandler: [authenticate, requireRole(UserRole.ORGANIZER)] },
    EventController.createEvent
  );

  // Validate QR payload and retrieve matching active event
  fastify.post('/validate-qr', EventController.validateQr);

  // Retrieve or save event registration form (Organizer only)
  fastify.get(
    '/:eventId/form',
    { preHandler: [authenticate, requireRole(UserRole.ORGANIZER)] },
    FormController.getForm
  );

  fastify.put(
    '/:eventId/form',
    { preHandler: [authenticate, requireRole(UserRole.ORGANIZER)] },
    FormController.saveForm
  );

  // Retrieve event metadata by slug or UUID
  fastify.get('/:eventId', EventController.getEvent);
}
