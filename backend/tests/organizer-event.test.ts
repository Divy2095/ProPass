import { FastifyInstance } from 'fastify';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { buildApp } from '../src/app.js';
import { prisma } from '../src/config/prisma.js';
import { UserRole } from '@prisma/client';
import { hashPassword } from '../src/utils/crypto.js';

describe('Organizer Event Creation & Persistence (Phase 4B)', () => {
  let app: FastifyInstance;
  let attendeeToken: string;
  let organizerToken1: string;
  let organizerToken2: string;
  let organizer1Id: string;
  let organizer2Id: string;

  const attendee = {
    email: 'event.attendee.test@propass.id',
    password: 'Password123!',
  };

  const organizer1 = {
    email: 'event.organizer1.test@propass.id',
    password: 'Password123!',
  };

  const organizer2 = {
    email: 'event.organizer2.test@propass.id',
    password: 'Password123!',
  };

  beforeAll(async () => {
    app = await buildApp();
    await app.ready();

    // Clean up test data
    await prisma.event.deleteMany({
      where: {
        OR: [
          { slug: { startsWith: 'test-hackathon' } },
          { slug: { startsWith: 'unique-slug-test' } },
        ],
      },
    });

    await prisma.user.deleteMany({
      where: {
        email: { in: [attendee.email, organizer1.email, organizer2.email] },
      },
    });

    const pwdHash = await hashPassword('Password123!');

    // 1. Create attendee user
    const uAttendee = await prisma.user.create({
      data: {
        email: attendee.email,
        passwordHash: pwdHash,
        role: UserRole.ATTENDEE,
      },
    });

    // 2. Create organizer 1
    const uOrg1 = await prisma.user.create({
      data: {
        email: organizer1.email,
        passwordHash: pwdHash,
        role: UserRole.ORGANIZER,
      },
    });
    organizer1Id = uOrg1.id;

    // 3. Create organizer 2
    const uOrg2 = await prisma.user.create({
      data: {
        email: organizer2.email,
        passwordHash: pwdHash,
        role: UserRole.ORGANIZER,
      },
    });
    organizer2Id = uOrg2.id;

    // Login each to acquire JWT tokens
    const loginAttendee = await app.inject({
      method: 'POST',
      url: '/api/v1/auth/login',
      payload: attendee,
    });
    attendeeToken = JSON.parse(loginAttendee.body).data.tokens.accessToken;

    const loginOrg1 = await app.inject({
      method: 'POST',
      url: '/api/v1/auth/login',
      payload: organizer1,
    });
    organizerToken1 = JSON.parse(loginOrg1.body).data.tokens.accessToken;

    const loginOrg2 = await app.inject({
      method: 'POST',
      url: '/api/v1/auth/login',
      payload: organizer2,
    });
    organizerToken2 = JSON.parse(loginOrg2.body).data.tokens.accessToken;
  });

  afterAll(async () => {
    await prisma.event.deleteMany({
      where: {
        OR: [
          { slug: { startsWith: 'test-hackathon' } },
          { slug: { startsWith: 'unique-slug-test' } },
        ],
      },
    });

    await prisma.user.deleteMany({
      where: {
        email: { in: [attendee.email, organizer1.email, organizer2.email] },
      },
    });

    await app.close();
  });

  describe('1. POST /api/v1/events (Event Creation)', () => {
    it('should reject unauthenticated request with 401 Unauthorized', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/events',
        payload: {
          name: 'Test Hackathon 2026',
          date: '2026-11-20',
          location: 'San Francisco, CA',
        },
      });

      expect(res.statusCode).toBe(401);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
    });

    it('should reject attendee with 403 Forbidden', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/events',
        headers: {
          authorization: `Bearer ${attendeeToken}`,
        },
        payload: {
          name: 'Test Hackathon 2026',
          date: '2026-11-20',
          location: 'San Francisco, CA',
        },
      });

      expect(res.statusCode).toBe(403);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
      expect(body.message).toContain('Organizer access required.');
    });

    it('should reject invalid input with 400 Validation Error', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/events',
        headers: {
          authorization: `Bearer ${organizerToken1}`,
        },
        payload: {
          name: '', // Empty name
          date: '',
          location: '',
        },
      });

      expect(res.statusCode).toBe(400);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
      expect(body.error).toBe('Validation Error');
    });

    it('should successfully create and persist an event for authenticated organizer (201 Created)', async () => {
      const eventPayload = {
        name: 'Test Hackathon 2026',
        description: 'A 2-day hackathon building future tech',
        date: '2026-11-20',
        startTime: '09:00 AM',
        endTime: '06:00 PM',
        location: 'Pier 27, San Francisco, CA',
        maxDuration: 2,
      };

      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/events',
        headers: {
          authorization: `Bearer ${organizerToken1}`,
        },
        payload: eventPayload,
      });

      expect(res.statusCode).toBe(201);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(true);
      expect(body.data.event).toBeDefined();

      const event = body.data.event;
      expect(event.id).toBeDefined();
      expect(event.title).toBe('Test Hackathon 2026');
      expect(event.slug).toBe('test-hackathon-2026');
      expect(event.qrPayload).toBe('https://propass.id/event/test-hackathon-2026');
      expect(event.location).toBe('Pier 27, San Francisco, CA');
      expect(event.date).toBe('2026-11-20');
      expect(event.startTime).toBe('09:00 AM');
      expect(event.endTime).toBe('06:00 PM');
      expect(event.maxDuration).toBe(2);
      expect(event.organizerId).toBe(organizer1Id);

      // Verify persistence directly in PostgreSQL
      const dbEvent = await prisma.event.findUnique({
        where: { id: event.id },
      });
      expect(dbEvent).not.toBeNull();
      expect(dbEvent?.organizerId).toBe(organizer1Id);
      expect(dbEvent?.title).toBe('Test Hackathon 2026');
      expect(dbEvent?.startDate).toBeDefined();
      expect(dbEvent?.endDate).toBeDefined();
    });

    it('should generate unique slug when creating event with duplicate title', async () => {
      const eventPayload = {
        name: 'Test Hackathon 2026', // Duplicate title
        description: 'Second edition',
        date: '2026-12-01',
        location: 'Virtual',
      };

      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/events',
        headers: {
          authorization: `Bearer ${organizerToken1}`,
        },
        payload: eventPayload,
      });

      expect(res.statusCode).toBe(201);
      const body = JSON.parse(res.body);
      const event = body.data.event;

      expect(event.slug).toBe('test-hackathon-2026-2');
      expect(event.qrPayload).toBe('https://propass.id/event/test-hackathon-2026-2');
    });
  });

  describe('2. GET /api/v1/organizer/events (Organizer Event List)', () => {
    it('should return list of events belonging exclusively to authenticated organizer', async () => {
      // Organizer 1 should see their 2 created events
      const res1 = await app.inject({
        method: 'GET',
        url: '/api/v1/organizer/events',
        headers: {
          authorization: `Bearer ${organizerToken1}`,
        },
      });

      expect(res1.statusCode).toBe(200);
      const body1 = JSON.parse(res1.body);
      expect(body1.success).toBe(true);
      expect(body1.data.events.length).toBe(2);
      expect(body1.data.events.every((e: any) => e.organizerId === organizer1Id)).toBe(true);
      expect(body1.data.events[0].qrPayload).toBeDefined();
    });

    it('should return empty list when organizer has no events', async () => {
      // Organizer 2 hasn't created any events yet
      const res2 = await app.inject({
        method: 'GET',
        url: '/api/v1/organizer/events',
        headers: {
          authorization: `Bearer ${organizerToken2}`,
        },
      });

      expect(res2.statusCode).toBe(200);
      const body2 = JSON.parse(res2.body);
      expect(body2.success).toBe(true);
      expect(body2.data.events).toEqual([]);
    });

    it('should reject attendee access to organizer events list with 403 Forbidden', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/organizer/events',
        headers: {
          authorization: `Bearer ${attendeeToken}`,
        },
      });

      expect(res.statusCode).toBe(403);
    });
  });

  describe('3. Attendee Integration & Compatibility', () => {
    it('should allow attendee to look up organizer-created event via GET /api/v1/events/:eventId', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/events/test-hackathon-2026',
        headers: {
          authorization: `Bearer ${attendeeToken}`,
        },
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(true);
      expect(body.data.event.title).toBe('Test Hackathon 2026');
      expect(body.data.event.location).toBe('Pier 27, San Francisco, CA');
    });

    it('should allow attendee to validate QR code for organizer-created event', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/events/validate-qr',
        headers: {
          authorization: `Bearer ${attendeeToken}`,
        },
        payload: {
          qrContent: 'https://propass.id/event/test-hackathon-2026',
        },
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(true);
      expect(body.data.event.title).toBe('Test Hackathon 2026');
      expect(body.data.parsedSlug).toBe('test-hackathon-2026');
    });

    it('should allow attendee to register for organizer-created event', async () => {
      const event = await prisma.event.findUnique({
        where: { slug: 'test-hackathon-2026' },
      });
      expect(event).not.toBeNull();

      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/registrations',
        headers: {
          authorization: `Bearer ${attendeeToken}`,
        },
        payload: {
          eventId: event!.id,
          fullName: 'Event Attendee Tester',
          email: attendee.email,
          institution: 'Tech Community',
          purpose: 'GENERAL_ATTENDEE',
          durationDays: 2,
        },
      });

      expect(res.statusCode).toBe(201);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(true);
      expect(body.data.registration.eventId).toBe(event!.id);
    });
  });
});
