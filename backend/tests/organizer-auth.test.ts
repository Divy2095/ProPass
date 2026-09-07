import { FastifyInstance } from 'fastify';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { buildApp } from '../src/app.js';
import { prisma } from '../src/config/prisma.js';
import { UserRole } from '@prisma/client';
import { hashPassword } from '../src/utils/crypto.js';

describe('Role-Based Authentication & Authorization (Phase 4A)', () => {
  let app: FastifyInstance;
  let attendeeToken: string;
  let organizerToken: string;

  const attendeeUser = {
    email: 'test.attendee.role@propass.id',
    password: 'AttendeePassword123!',
  };

  const organizerUser = {
    email: 'test.organizer.role@propass.id',
    password: 'OrganizerPassword123!',
  };

  beforeAll(async () => {
    app = await buildApp();
    await app.ready();

    // Clean up test data
    await prisma.user.deleteMany({
      where: { email: { in: [attendeeUser.email, organizerUser.email] } },
    });

    // 1. Register attendee user
    const attendeeRegisterRes = await app.inject({
      method: 'POST',
      url: '/api/v1/auth/register',
      payload: attendeeUser,
    });
    expect(attendeeRegisterRes.statusCode).toBe(201);
    const attendeeBody = JSON.parse(attendeeRegisterRes.body);
    attendeeToken = attendeeBody.data.tokens.accessToken;
    expect(attendeeBody.data.user.role).toBe(UserRole.ATTENDEE);

    // 2. Create organizer user directly with ORGANIZER role
    const organizerPasswordHash = await hashPassword(organizerUser.password);
    const createdOrganizer = await prisma.user.create({
      data: {
        email: organizerUser.email,
        passwordHash: organizerPasswordHash,
        role: UserRole.ORGANIZER,
        profile: {
          create: {
            fullName: 'Test Organizer Lead',
            title: 'Head of Operations',
            organization: 'ProPass Global',
            phone: '+1 (555) 789-0123',
            isVerified: true,
            completionScore: 100,
          },
        },
      },
    });

    // Log in as organizer to get organizer token
    const organizerLoginRes = await app.inject({
      method: 'POST',
      url: '/api/v1/auth/login',
      payload: organizerUser,
    });
    expect(organizerLoginRes.statusCode).toBe(200);
    const organizerBody = JSON.parse(organizerLoginRes.body);
    organizerToken = organizerBody.data.tokens.accessToken;
    expect(organizerBody.data.user.role).toBe(UserRole.ORGANIZER);
  });

  afterAll(async () => {
    await prisma.user.deleteMany({
      where: { email: { in: [attendeeUser.email, organizerUser.email] } },
    });
    await app.close();
  });

  describe('1. Protected Organizer Endpoint (GET /api/v1/organizer/me)', () => {
    it('should reject unauthenticated request with 401 Unauthorized', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/organizer/me',
      });

      expect(res.statusCode).toBe(401);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
      expect(body.error).toBe('Unauthorized');
    });

    it('should reject invalid Bearer token with 401 Unauthorized', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/organizer/me',
        headers: {
          authorization: 'Bearer invalid.token.payload',
        },
      });

      expect(res.statusCode).toBe(401);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
      expect(body.error).toBe('Unauthorized');
    });

    it('should reject ATTENDEE role with 403 Forbidden', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/organizer/me',
        headers: {
          authorization: `Bearer ${attendeeToken}`,
        },
      });

      expect(res.statusCode).toBe(403);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
      expect(body.error).toBe('Forbidden');
      expect(body.message).toContain('Organizer access required.');
    });

    it('should allow ORGANIZER role with 200 OK and safe organizer profile', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/organizer/me',
        headers: {
          authorization: `Bearer ${organizerToken}`,
        },
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(true);
      expect(body.data.organizer).toBeDefined();
      expect(body.data.organizer.email).toBe(organizerUser.email);
      expect(body.data.organizer.role).toBe(UserRole.ORGANIZER);
      expect(body.data.organizer.passwordHash).toBeUndefined();
      expect(body.data.organizer.profile.fullName).toBe('Test Organizer Lead');
      expect(body.data.organizer.profile.title).toBe('Head of Operations');
    });

    it('should also work on unversioned alias GET /api/organizer/me', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/organizer/me',
        headers: {
          authorization: `Bearer ${organizerToken}`,
        },
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(true);
      expect(body.data.organizer.role).toBe(UserRole.ORGANIZER);
    });
  });

  describe('2. Seeded Organizer Account Verification', () => {
    it('should authenticate seeded dev organizer account (organizer@propass.id)', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/auth/login',
        payload: {
          email: 'organizer@propass.id',
          password: 'Organizer123!',
        },
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(true);
      expect(body.data.user.email).toBe('organizer@propass.id');
      expect(body.data.user.role).toBe(UserRole.ORGANIZER);

      // Access organizer protected endpoint with seeded account token
      const checkRes = await app.inject({
        method: 'GET',
        url: '/api/v1/organizer/me',
        headers: {
          authorization: `Bearer ${body.data.tokens.accessToken}`,
        },
      });
      expect(checkRes.statusCode).toBe(200);
      const checkBody = JSON.parse(checkRes.body);
      expect(checkBody.data.organizer.role).toBe(UserRole.ORGANIZER);
      expect(checkBody.data.organizer.profile.fullName).toBe('Dev Organizer');
    });
  });
});
