import { FastifyInstance } from 'fastify';
import { UserRole } from '@prisma/client';
import { OrganizerController } from '../controllers/organizer.controller.js';
import { EventController } from '../controllers/event.controller.js';
import { authenticate } from '../middleware/auth.middleware.js';
import { requireRole } from '../middleware/role.middleware.js';

export async function organizerRoutes(fastify: FastifyInstance) {
  fastify.get(
    '/me',
    { preHandler: [authenticate, requireRole(UserRole.ORGANIZER)] },
    OrganizerController.getMe
  );

  fastify.get(
    '/events',
    { preHandler: [authenticate, requireRole(UserRole.ORGANIZER)] },
    EventController.getOrganizerEvents
  );

  fastify.get(
    '/events/:eventId/registrations',
    { preHandler: [authenticate, requireRole(UserRole.ORGANIZER)] },
    OrganizerController.getEventRegistrations
  );

  fastify.get(
    '/registrations/:registrationId',
    { preHandler: [authenticate, requireRole(UserRole.ORGANIZER)] },
    OrganizerController.getRegistrationDetails
  );
}
