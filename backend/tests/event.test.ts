import { FastifyInstance } from 'fastify';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { buildApp } from '../src/app.js';
import { prisma } from '../src/config/prisma.js';

describe('Event & QR Validation API (Phase 2C)', () => {
  let app: FastifyInstance;

  beforeAll(async () => {
    app = await buildApp();
    await app.ready();

    // Create an inactive test event to test inactive event handling
    await prisma.event.upsert({
      where: { slug: 'inactive-test-event' },
      update: { isActive: false },
      create: {
        slug: 'inactive-test-event',
        title: 'Inactive Past Event 2020',
        overline: 'PAST EVENT',
        subtitle: 'This event has concluded.',
        startDate: new Date('2020-01-01T09:00:00Z'),
        endDate: new Date('2020-01-02T18:00:00Z'),
        isActive: false,
      },
    });
  });

  afterAll(async () => {
    await prisma.event.deleteMany({
      where: { slug: 'inactive-test-event' },
    });
    await app.close();
  });

  describe('1. GET /api/v1/events/:eventId', () => {
    it('should retrieve existing active event by slug (200 OK)', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/events/techconf-2024',
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);

      expect(body.success).toBe(true);
      expect(body.data.event).toBeDefined();
      expect(body.data.event.slug).toBe('techconf-2024');
      expect(body.data.event.title).toBe('TechConf 2024');
      expect(body.data.event.overline).toBe('EVENT REGISTRATION');
      expect(body.data.event.subtitle).toBe('Complete your registration to secure your spot.');
      expect(body.data.event.maxDuration).toBe(5);
      expect(body.data.event.isActive).toBe(true);
      expect(body.data.event.location).toBeDefined();
      expect(body.data.event.startDate).toBeDefined();
      expect(body.data.event.endDate).toBeDefined();
    });

    it('should retrieve existing active event by UUID id (200 OK)', async () => {
      const techConf = await prisma.event.findUnique({
        where: { slug: 'techconf-2024' },
      });
      expect(techConf).not.toBeNull();

      const res = await app.inject({
        method: 'GET',
        url: `/api/v1/events/${techConf!.id}`,
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);
      expect(body.data.event.slug).toBe('techconf-2024');
    });

    it('should return 404 for unknown event identifier', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/events/non-existent-event-999',
      });

      expect(res.statusCode).toBe(404);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
      expect(body.error).toBe('Not Found');
      expect(body.message).toContain('not found');
    });

    it('should reject inactive event with 410 Gone / Inactive status', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/events/inactive-test-event',
      });

      expect(res.statusCode).toBe(410);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
      expect(body.message).toContain('inactive');
    });
  });

  describe('2. POST /api/v1/events/validate-qr', () => {
    it('should validate TechConf 2024 standard URL and return event data', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/events/validate-qr',
        payload: {
          qrContent: 'https://propass.id/event/techconf-2024',
        },
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);

      expect(body.success).toBe(true);
      expect(body.data.parsedSlug).toBe('techconf-2024');
      expect(body.data.event.title).toBe('TechConf 2024');
      expect(body.data.event.overline).toBe('EVENT REGISTRATION');
    });

    it('should validate Google Office Visit QR', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/events/validate-qr',
        payload: {
          qrContent: 'https://propass.id/event/google-office-visit',
        },
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);
      expect(body.data.parsedSlug).toBe('google-office-visit');
      expect(body.data.event.title).toBe('Google Office Visit');
      expect(body.data.event.overline).toBe('VISITOR PASS');
    });

    it('should validate Android Conf 2026 QR', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/events/validate-qr',
        payload: {
          qrContent: 'https://propass.id/event/android-conf-2026',
        },
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);
      expect(body.data.parsedSlug).toBe('android-conf-2026');
      expect(body.data.event.title).toBe('Android Conf 2026');
      expect(body.data.event.overline).toBe('CONFERENCE PASS');
    });

    it('should accept QR with trailing slash', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/events/validate-qr',
        payload: {
          qrContent: 'https://propass.id/event/techconf-2024/',
        },
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);
      expect(body.data.parsedSlug).toBe('techconf-2024');
    });

    it('should accept QR with http:// protocol', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/events/validate-qr',
        payload: {
          qrContent: 'http://propass.id/event/techconf-2024',
        },
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);
      expect(body.data.parsedSlug).toBe('techconf-2024');
    });

    it('should accept QR with www. subdomain', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/events/validate-qr',
        payload: {
          qrContent: 'https://www.propass.id/event/techconf-2024',
        },
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);
      expect(body.data.parsedSlug).toBe('techconf-2024');
    });

    it('should accept uppercase and case variations', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/events/validate-qr',
        payload: {
          qrContent: 'HTTPS://WWW.PROPASS.ID/EVENT/TECHCONF-2024/',
        },
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);
      expect(body.data.parsedSlug).toBe('techconf-2024');
    });

    it('should reject random unrelated URL (400 Bad Request)', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/events/validate-qr',
        payload: {
          qrContent: 'https://google.com',
        },
      });

      expect(res.statusCode).toBe(400);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
      expect(body.message).toContain('Invalid ProPass QR code format');
    });

    it('should reject ChatGPT URL (400 Bad Request)', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/events/validate-qr',
        payload: {
          qrContent: 'https://chatgpt.com/share/6789-abcd',
        },
      });

      expect(res.statusCode).toBe(400);
    });

    it('should reject non-ProPass domain event URL (400 Bad Request)', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/events/validate-qr',
        payload: {
          qrContent: 'https://example.com/event/techconf-2024',
        },
      });

      expect(res.statusCode).toBe(400);
    });

    it('should reject empty QR content (400 Bad Request)', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/events/validate-qr',
        payload: {
          qrContent: '   ',
        },
      });

      expect(res.statusCode).toBe(400);
    });

    it('should reject malformed QR payload (400 Bad Request)', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/events/validate-qr',
        payload: {
          qrContent: 'propass.id/not-an-event-url',
        },
      });

      expect(res.statusCode).toBe(400);
    });

    it('should reject unknown ProPass event ID in QR code (404 Not Found)', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/events/validate-qr',
        payload: {
          qrContent: 'https://propass.id/event/not-real-999',
        },
      });

      expect(res.statusCode).toBe(404);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
      expect(body.message).toContain('not found');
    });

    it('should reject inactive event in QR code (410 Gone / Inactive)', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/events/validate-qr',
        payload: {
          qrContent: 'https://propass.id/event/inactive-test-event',
        },
      });

      expect(res.statusCode).toBe(410);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
      expect(body.message).toContain('inactive');
    });
  });
});
