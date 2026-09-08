import { FastifyInstance } from 'fastify';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { buildApp } from '../src/app.js';
import { prisma } from '../src/config/prisma.js';

describe('Registration API (Phase 2D)', () => {
  let app: FastifyInstance;

  const userA = {
    email: 'user.a.registration@propass.id',
    password: 'Password123!',
  };

  const userB = {
    email: 'user.b.registration@propass.id',
    password: 'Password123!',
  };

  let tokenA: string;
  let userIdA: string;
  let tokenB: string;
  let userIdB: string;

  beforeAll(async () => {
    app = await buildApp();
    await app.ready();

    // Clean up test users
    await prisma.user.deleteMany({
      where: {
        email: { in: [userA.email, userB.email] },
      },
    });

    // Create User A
    const resA = await app.inject({
      method: 'POST',
      url: '/api/v1/auth/register',
      payload: userA,
    });
    const bodyA = JSON.parse(resA.body);
    tokenA = bodyA.data.tokens.accessToken;
    userIdA = bodyA.data.user.id;

    // Create User B
    const resB = await app.inject({
      method: 'POST',
      url: '/api/v1/auth/register',
      payload: userB,
    });
    const bodyB = JSON.parse(resB.body);
    tokenB = bodyB.data.tokens.accessToken;
    userIdB = bodyB.data.user.id;

    // Ensure inactive event exists for testing
    await prisma.event.upsert({
      where: { slug: 'inactive-registration-event' },
      update: { isActive: false },
      create: {
        slug: 'inactive-registration-event',
        title: 'Inactive Registration Event',
        overline: 'CLOSED EVENT',
        subtitle: 'Registrations are closed.',
        startDate: new Date('2020-01-01T09:00:00Z'),
        endDate: new Date('2020-01-02T18:00:00Z'),
        maxDuration: 2,
        isActive: false,
      },
    });
  });

  afterAll(async () => {
    await prisma.user.deleteMany({
      where: {
        email: { in: [userA.email, userB.email] },
      },
    });
    await prisma.event.deleteMany({
      where: { slug: 'inactive-registration-event' },
    });
    await app.close();
  });

  describe('1. POST /api/v1/registrations (Create Registration)', () => {
    it('1. Authenticated user can register for an active event (201 Created)', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/registrations',
        headers: { authorization: `Bearer ${tokenA}` },
        payload: {
          eventId: 'techconf-2024',
          fullName: 'Alex Morgan',
          email: userA.email,
          institution: 'University of Technology',
          purpose: 'GENERAL_ATTENDEE',
          durationDays: 3,
          vehicleNumber: 'MH12AB1234',
        },
      });

      expect(res.statusCode).toBe(201);
      const body = JSON.parse(res.body);

      expect(body.success).toBe(true);
      expect(body.data.registration).toBeDefined();
      expect(body.data.registration.fullName).toBe('Alex Morgan');
      expect(body.data.registration.email).toBe(userA.email);
      expect(body.data.registration.institution).toBe('University of Technology');
      expect(body.data.registration.purpose).toBe('GENERAL_ATTENDEE');
      expect(body.data.registration.durationDays).toBe(3);
      expect(body.data.registration.vehicleNumber).toBe('MH12AB1234');
      expect(body.data.registration.status).toBe('CONFIRMED');
      expect(body.data.registration.event.slug).toBe('techconf-2024');
    });

    it('2. Registration is persisted in PostgreSQL', async () => {
      const dbRegistration = await prisma.registration.findFirst({
        where: {
          userId: userIdA,
          event: { slug: 'techconf-2024' },
        },
      });

      expect(dbRegistration).not.toBeNull();
      expect(dbRegistration?.fullName).toBe('Alex Morgan');
      expect(dbRegistration?.durationDays).toBe(3);
    });

    it('3. Correct authenticated user owns the registration', async () => {
      const dbRegistration = await prisma.registration.findFirst({
        where: {
          userId: userIdA,
          event: { slug: 'techconf-2024' },
        },
      });

      expect(dbRegistration?.userId).toBe(userIdA);
    });

    it('4. Valid purpose variations are accepted (e.g. "Speaker" -> SPEAKER)', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/registrations',
        headers: { authorization: `Bearer ${tokenA}` },
        payload: {
          eventId: 'google-office-visit',
          fullName: 'Alex Morgan',
          email: userA.email,
          institution: 'University of Technology',
          purpose: 'Speaker',
          durationDays: 1,
        },
      });

      expect(res.statusCode).toBe(201);
      const body = JSON.parse(res.body);
      expect(body.data.registration.purpose).toBe('SPEAKER');
    });

    it('5. Invalid purpose is rejected (400 Bad Request)', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/registrations',
        headers: { authorization: `Bearer ${tokenA}` },
        payload: {
          eventId: 'android-conf-2026',
          fullName: 'Alex Morgan',
          email: userA.email,
          institution: 'University of Technology',
          purpose: 'INVALID_PURPOSE_HERE',
          durationDays: 2,
        },
      });

      expect(res.statusCode).toBe(400);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
      expect(body.error).toBe('Validation Error');
    });

    it('6. Invalid email is rejected (400 Bad Request)', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/registrations',
        headers: { authorization: `Bearer ${tokenA}` },
        payload: {
          eventId: 'android-conf-2026',
          fullName: 'Alex Morgan',
          email: 'invalid-email-format',
          institution: 'University of Technology',
          purpose: 'SPEAKER',
          durationDays: 2,
        },
      });

      expect(res.statusCode).toBe(400);
    });

    it('7. Blank required fields are rejected (400 Bad Request)', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/registrations',
        headers: { authorization: `Bearer ${tokenA}` },
        payload: {
          eventId: 'android-conf-2026',
          fullName: '   ',
          email: userA.email,
          institution: '',
          purpose: 'SPEAKER',
          durationDays: 2,
        },
      });

      expect(res.statusCode).toBe(400);
    });

    it('8. durationDays = 1 works (201 Created)', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/registrations',
        headers: { authorization: `Bearer ${tokenA}` },
        payload: {
          eventId: 'android-conf-2026',
          fullName: 'Alex Morgan',
          email: userA.email,
          institution: 'University of Technology',
          purpose: 'MEDIA_PRESS',
          durationDays: 1,
        },
      });

      expect(res.statusCode).toBe(201);
      const body = JSON.parse(res.body);
      expect(body.data.registration.durationDays).toBe(1);
    });

    it('9. durationDays = event.maxDuration works (201 Created)', async () => {
      const techConf = await prisma.event.findUnique({
        where: { slug: 'techconf-2024' },
      });
      expect(techConf?.maxDuration).toBe(5);

      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/registrations',
        headers: { authorization: `Bearer ${tokenB}` },
        payload: {
          eventId: 'techconf-2024',
          fullName: 'Sarah Jenkins',
          email: userB.email,
          institution: 'TechFlow Inc.',
          purpose: 'SPONSOR_EXHIBITOR',
          durationDays: 5,
        },
      });

      expect(res.statusCode).toBe(201);
      const body = JSON.parse(res.body);
      expect(body.data.registration.durationDays).toBe(5);
    });

    it('10. durationDays > event.maxDuration is rejected (400 Bad Request)', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/registrations',
        headers: { authorization: `Bearer ${tokenB}` },
        payload: {
          eventId: 'google-office-visit', // maxDuration = 1
          fullName: 'Sarah Jenkins',
          email: userB.email,
          institution: 'TechFlow Inc.',
          purpose: 'GENERAL_ATTENDEE',
          durationDays: 3, // Exceeds maxDuration 1
        },
      });

      expect(res.statusCode).toBe(400);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
      expect(body.message).toContain('exceeds event maximum allowed duration');
    });

    it('11. durationDays = 0 is rejected (400 Bad Request)', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/registrations',
        headers: { authorization: `Bearer ${tokenB}` },
        payload: {
          eventId: 'google-office-visit',
          fullName: 'Sarah Jenkins',
          email: userB.email,
          institution: 'TechFlow Inc.',
          purpose: 'GENERAL_ATTENDEE',
          durationDays: 0,
        },
      });

      expect(res.statusCode).toBe(400);
    });

    it('12. Invalid/nonexistent event is rejected (404 Not Found)', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/registrations',
        headers: { authorization: `Bearer ${tokenB}` },
        payload: {
          eventId: 'completely-fake-event-id-999',
          fullName: 'Sarah Jenkins',
          email: userB.email,
          institution: 'TechFlow Inc.',
          purpose: 'GENERAL_ATTENDEE',
          durationDays: 1,
        },
      });

      expect(res.statusCode).toBe(404);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
      expect(body.message).toContain('not found');
    });

    it('13. Inactive event is rejected (410 Gone / Inactive)', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/registrations',
        headers: { authorization: `Bearer ${tokenB}` },
        payload: {
          eventId: 'inactive-registration-event',
          fullName: 'Sarah Jenkins',
          email: userB.email,
          institution: 'TechFlow Inc.',
          purpose: 'GENERAL_ATTENDEE',
          durationDays: 1,
        },
      });

      expect(res.statusCode).toBe(410);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
      expect(body.message).toContain('inactive');
    });

    it('14. Duplicate registration is rejected (409 Conflict)', async () => {
      // User A is already registered for techconf-2024 from test 1
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/registrations',
        headers: { authorization: `Bearer ${tokenA}` },
        payload: {
          eventId: 'techconf-2024',
          fullName: 'Alex Morgan',
          email: userA.email,
          institution: 'University of Technology',
          purpose: 'SPEAKER',
          durationDays: 2,
        },
      });

      expect(res.statusCode).toBe(409);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
      expect(body.error).toBe('Conflict');
      expect(body.message).toContain('already registered');
    });

    it('15. Unauthenticated registration request returns 401 Unauthorized', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/registrations',
        payload: {
          eventId: 'techconf-2024',
          fullName: 'Hacker User',
          email: 'hacker@example.com',
          institution: 'Dark Web',
          purpose: 'GENERAL_ATTENDEE',
          durationDays: 1,
        },
      });

      expect(res.statusCode).toBe(401);
    });

    it('16. Sensitive fields are never returned in response', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/registrations',
        headers: { authorization: `Bearer ${tokenB}` },
        payload: {
          eventId: 'google-office-visit',
          fullName: 'Sarah Jenkins',
          email: userB.email,
          institution: 'TechFlow Inc.',
          purpose: 'GENERAL_ATTENDEE',
          durationDays: 1,
        },
      });

      expect(res.statusCode).toBe(201);
      const body = JSON.parse(res.body);
      expect(body.data.registration.passwordHash).toBeUndefined();
      expect(body.data.registration.tokenHash).toBeUndefined();
    });
  });

  describe('2. GET /api/v1/registrations/my (Get My Registrations)', () => {
    it('17. Authenticated user receives their own registrations', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/registrations/my',
        headers: { authorization: `Bearer ${tokenA}` },
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);

      expect(body.success).toBe(true);
      expect(body.data.registrations).toBeDefined();
      expect(body.data.count).toBeGreaterThanOrEqual(3); // TechConf, Google Visit, Android Conf

      // Verify all returned registrations belong to User A
      for (const reg of body.data.registrations) {
        expect(reg.userId).toBe(userIdA);
      }
    });

    it('18. Registrations include appropriate event information', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/registrations/my',
        headers: { authorization: `Bearer ${tokenA}` },
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);
      const firstReg = body.data.registrations[0];

      expect(firstReg.event).toBeDefined();
      expect(firstReg.event.title).toBeDefined();
      expect(firstReg.event.overline).toBeDefined();
      expect(firstReg.event.subtitle).toBeDefined();
    });

    it('19. Another user registrations are never returned (Isolation check)', async () => {
      const resB = await app.inject({
        method: 'GET',
        url: '/api/v1/registrations/my',
        headers: { authorization: `Bearer ${tokenB}` },
      });

      expect(resB.statusCode).toBe(200);
      const bodyB = JSON.parse(resB.body);

      for (const reg of bodyB.data.registrations) {
        expect(reg.userId).toBe(userIdB);
        expect(reg.userId).not.toBe(userIdA);
      }
    });

    it('20. Unauthenticated get my registrations request returns 401 Unauthorized', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/registrations/my',
      });

      expect(res.statusCode).toBe(401);
    });

    it('21. After registering for an event, user has an active Digital Pass via GET /api/v1/passes/me', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/passes/me',
        headers: { authorization: `Bearer ${tokenA}` },
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(true);
      expect(body.data.pass).toBeDefined();
      expect(body.data.pass.isActive).toBe(true);
      expect(body.data.pass.passNumber).toMatch(/^PP-\d{4}-\d{4}$/);
      expect(body.data.pass.tier).toBe('PREMIUM');
      expect(body.data.holder.fullName).toBe('Alex Morgan');
      expect(body.data.holder.organization).toBe('University of Technology');
    });

    it('22. Registration without explicit purpose or durationDays defaults cleanly and issues active pass', async () => {
      const uniqueSlug = `default-reg-event-${Date.now()}`;
      const event = await prisma.event.create({
        data: {
          slug: uniqueSlug,
          title: 'Custom Organizer Event',
          overline: 'DEFAULT REG',
          subtitle: 'Subtitle',
          startDate: new Date('2026-11-20T09:00:00Z'),
          endDate: new Date('2026-11-21T18:00:00Z'),
          maxDuration: 2,
          isActive: true,
        },
      });

      const regRes = await app.inject({
        method: 'POST',
        url: '/api/v1/registrations',
        headers: { authorization: `Bearer ${tokenB}` },
        payload: {
          eventId: uniqueSlug,
          fullName: 'Jordan Lee',
          email: 'jordan.lee@example.com',
          institution: 'MIT Lab',
        },
      });

      expect(regRes.statusCode).toBe(201);
      const regBody = JSON.parse(regRes.body);
      expect(regBody.success).toBe(true);
      expect(regBody.data.registration.purpose).toBe('GENERAL_ATTENDEE');
      expect(regBody.data.registration.durationDays).toBe(1);

      // Verify Digital Pass is active for user B
      const passRes = await app.inject({
        method: 'GET',
        url: '/api/v1/passes/me',
        headers: { authorization: `Bearer ${tokenB}` },
      });
      expect(passRes.statusCode).toBe(200);
      const passBody = JSON.parse(passRes.body);
      expect(passBody.data.pass.isActive).toBe(true);
      expect(passBody.data.holder.fullName).toBe('Jordan Lee');

      await prisma.registration.deleteMany({ where: { eventId: event.id } });
      await prisma.event.delete({ where: { id: event.id } });
    });
  });
});
