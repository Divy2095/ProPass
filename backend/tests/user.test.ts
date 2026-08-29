import { FastifyInstance } from 'fastify';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { buildApp } from '../src/app.js';
import { prisma } from '../src/config/prisma.js';

describe('User & Profile API (Phase 2B)', () => {
  let app: FastifyInstance;

  const testUser = {
    email: 'profile.test.user@propass.id',
    password: 'Password123!',
  };

  let accessToken: string;
  let userId: string;

  beforeAll(async () => {
    app = await buildApp();
    await app.ready();

    // Clean up test user if present
    await prisma.user.deleteMany({
      where: { email: testUser.email },
    });

    // Register test user
    const regRes = await app.inject({
      method: 'POST',
      url: '/api/v1/auth/register',
      payload: testUser,
    });

    const regBody = JSON.parse(regRes.body);
    accessToken = regBody.data.tokens.accessToken;
    userId = regBody.data.user.id;
  });

  afterAll(async () => {
    await prisma.user.deleteMany({
      where: { email: testUser.email },
    });
    await app.close();
  });

  describe('1. GET /api/v1/users/profile', () => {
    it('should retrieve authenticated user profile (200 OK)', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/users/profile',
        headers: {
          authorization: `Bearer ${accessToken}`,
        },
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);

      expect(body.success).toBe(true);
      expect(body.data.user.id).toBe(userId);
      expect(body.data.user.email).toBe(testUser.email);
      expect(body.data.user.passwordHash).toBeUndefined(); // Security check
    });

    it('should reject unauthenticated request (401 Unauthorized)', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/users/profile',
      });

      expect(res.statusCode).toBe(401);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
    });

    it('should retrieve seeded demo user profile correctly (Sarah Jenkins)', async () => {
      const sarah = await prisma.user.findUnique({
        where: { email: 'sarah.jenkins@example.com' },
      });
      expect(sarah).not.toBeNull();

      // Log in as Sarah
      // Create access token for Sarah directly via AuthService or test
      const loginRes = await app.inject({
        method: 'POST',
        url: '/api/v1/auth/register',
        payload: {
          email: 'sarah.direct.test@example.com',
          password: 'Password123!',
        },
      });
      const sarahToken = JSON.parse(loginRes.body).data.tokens.accessToken;

      // Update Sarah's profile
      await app.inject({
        method: 'PUT',
        url: '/api/v1/users/profile',
        headers: { authorization: `Bearer ${sarahToken}` },
        payload: {
          fullName: 'Sarah Jenkins',
          title: 'Senior UX Researcher',
          organization: 'TechFlow Inc.',
          phone: '+1 (555) 018-9234',
          linkedinUrl: 'https://linkedin.com/in/sarahjenkins',
        },
      });

      const profileRes = await app.inject({
        method: 'GET',
        url: '/api/v1/users/profile',
        headers: { authorization: `Bearer ${sarahToken}` },
      });

      expect(profileRes.statusCode).toBe(200);
      const profileBody = JSON.parse(profileRes.body);
      expect(profileBody.data.profile.fullName).toBe('Sarah Jenkins');
      expect(profileBody.data.profile.title).toBe('Senior UX Researcher');
      expect(profileBody.data.profile.organization).toBe('TechFlow Inc.');
      expect(profileBody.data.profile.completionScore).toBe(90); // 20+20+20+15+15

      // Clean up Sarah test user
      await prisma.user.deleteMany({
        where: { email: 'sarah.direct.test@example.com' },
      });
    });
  });

  describe('2. PUT /api/v1/users/profile', () => {
    it('should update profile and calculate completion score (200 OK)', async () => {
      const updatePayload = {
        fullName: 'Alex Morgan',
        title: 'Senior Android Architect',
        organization: 'University of Technology',
        phone: '+1 (555) 012-3456',
        linkedinUrl: 'https://linkedin.com/in/alexmorgan',
        avatarUrl: 'https://propass.id/avatars/alex.jpg',
      };

      const res = await app.inject({
        method: 'PUT',
        url: '/api/v1/users/profile',
        headers: {
          authorization: `Bearer ${accessToken}`,
        },
        payload: updatePayload,
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);

      expect(body.success).toBe(true);
      expect(body.data.profile.fullName).toBe(updatePayload.fullName);
      expect(body.data.profile.title).toBe(updatePayload.title);
      expect(body.data.profile.organization).toBe(updatePayload.organization);
      expect(body.data.profile.phone).toBe(updatePayload.phone);
      expect(body.data.profile.linkedinUrl).toBe(updatePayload.linkedinUrl);
      expect(body.data.profile.avatarUrl).toBe(updatePayload.avatarUrl);
      // Completion score should be 100% (20+20+20+15+15+10)
      expect(body.data.profile.completionScore).toBe(100);

      // Verify persistence in PostgreSQL
      const dbProfile = await prisma.profile.findUnique({
        where: { userId },
      });
      expect(dbProfile?.fullName).toBe('Alex Morgan');
      expect(dbProfile?.title).toBe('Senior Android Architect');
      expect(dbProfile?.completionScore).toBe(100);
    });

    it('should reject invalid URLs in linkedinUrl (400 Bad Request)', async () => {
      const res = await app.inject({
        method: 'PUT',
        url: '/api/v1/users/profile',
        headers: {
          authorization: `Bearer ${accessToken}`,
        },
        payload: {
          linkedinUrl: 'not-a-valid-url',
        },
      });

      expect(res.statusCode).toBe(400);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
      expect(body.error).toBe('Validation Error');
    });

    it('should reject unauthenticated profile update (401 Unauthorized)', async () => {
      const res = await app.inject({
        method: 'PUT',
        url: '/api/v1/users/profile',
        payload: {
          fullName: 'Unauthorized Hacker',
        },
      });

      expect(res.statusCode).toBe(401);
    });
  });

  describe('3. GET /api/v1/dashboard', () => {
    it('should return aggregated live dashboard data (200 OK)', async () => {
      // Seed a sample pass and registration for test user
      const techConf = await prisma.event.findUnique({
        where: { slug: 'techconf-2024' },
      });

      if (techConf) {
        await prisma.registration.upsert({
          where: {
            userId_eventId: {
              userId,
              eventId: techConf.id,
            },
          },
          update: {},
          create: {
            userId,
            eventId: techConf.id,
            fullName: 'Alex Morgan',
            email: testUser.email,
            institution: 'University of Technology',
            purpose: 'GENERAL_ATTENDEE',
            durationDays: 3,
            vehicleNumber: 'MH12AB1234',
          },
        });
      }

      await prisma.digitalPass.upsert({
        where: { userId },
        update: {},
        create: {
          userId,
          passNumber: 'PP-2024-9999',
          qrPayload: 'propass:pass:PP-2024-9999',
          tier: 'PREMIUM',
        },
      });

      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/dashboard',
        headers: {
          authorization: `Bearer ${accessToken}`,
        },
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);

      expect(body.success).toBe(true);
      expect(body.data.greeting).toBe('Hello, Alex');
      expect(body.data.profile.fullName).toBe('Alex Morgan');
      expect(body.data.pass.passNumber).toBe('PP-2024-9999');
      expect(body.data.pass.tier).toBe('PREMIUM');
      expect(body.data.recentActivity.length).toBeGreaterThan(0);
      expect(body.data.recentActivity[0].eventSlug).toBe('techconf-2024');
      expect(body.data.recentActivity[0].eventTitle).toBe('TechConf 2024');
      expect(body.data.recentActivity[0].status).toBe('CONFIRMED');
    });

    it('should reject unauthenticated dashboard request (401 Unauthorized)', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/dashboard',
      });

      expect(res.statusCode).toBe(401);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
    });
  });
});
