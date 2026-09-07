import { FastifyInstance } from 'fastify';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { buildApp } from '../src/app.js';
import { prisma } from '../src/config/prisma.js';
import { UserRole } from '@prisma/client';
import { hashPassword } from '../src/utils/crypto.js';

describe('Phase 4D — Organizer Registration Management', () => {
  let app: FastifyInstance;
  let organizerAToken: string;
  let organizerAId: string;
  let organizerBToken: string;
  let organizerBId: string;
  let attendee1Token: string;
  let attendee1Id: string;
  let attendee2Token: string;
  let attendee2Id: string;

  let eventAId: string;
  let eventASlug: string;
  let eventBId: string;
  let eventBSlug: string;

  let tshirtQId: string;
  let dietQId: string;
  let notesQId: string;

  let reg1Id: string;
  let reg2Id: string;

  const orgAEmail = `org-a-${Date.now()}@propass.id`;
  const orgBEmail = `org-b-${Date.now()}@propass.id`;
  const att1Email = `att-1-${Date.now()}@propass.id`;
  const att2Email = `att-2-${Date.now()}@propass.id`;
  const pwd = 'Password123!';

  beforeAll(async () => {
    app = await buildApp();
    await app.ready();

    const pwdHash = await hashPassword(pwd);

    // 1. Create Organizer A
    const userA = await prisma.user.create({
      data: {
        email: orgAEmail,
        passwordHash: pwdHash,
        role: UserRole.ORGANIZER,
      },
    });
    organizerAId = userA.id;

    // 2. Create Organizer B
    const userB = await prisma.user.create({
      data: {
        email: orgBEmail,
        passwordHash: pwdHash,
        role: UserRole.ORGANIZER,
      },
    });
    organizerBId = userB.id;

    // 3. Create Attendee 1 with Profile
    const userAtt1 = await prisma.user.create({
      data: {
        email: att1Email,
        passwordHash: pwdHash,
        role: UserRole.ATTENDEE,
        profile: {
          create: {
            fullName: 'Attendee One',
            phone: '+1 555-0101',
            organization: 'Acme Corp',
            title: 'Lead Developer',
          },
        },
      },
    });
    attendee1Id = userAtt1.id;

    // 4. Create Attendee 2 without phone in Profile
    const userAtt2 = await prisma.user.create({
      data: {
        email: att2Email,
        passwordHash: pwdHash,
        role: UserRole.ATTENDEE,
        profile: {
          create: {
            fullName: 'Attendee Two',
            organization: 'Innovate Labs',
            title: 'Product Manager',
          },
        },
      },
    });
    attendee2Id = userAtt2.id;

    // Logins to obtain JWT tokens
    const loginOrgA = await app.inject({
      method: 'POST',
      url: '/api/v1/auth/login',
      payload: { email: orgAEmail, password: pwd },
    });
    organizerAToken = JSON.parse(loginOrgA.body).data.tokens.accessToken;

    const loginOrgB = await app.inject({
      method: 'POST',
      url: '/api/v1/auth/login',
      payload: { email: orgBEmail, password: pwd },
    });
    organizerBToken = JSON.parse(loginOrgB.body).data.tokens.accessToken;

    const loginAtt1 = await app.inject({
      method: 'POST',
      url: '/api/v1/auth/login',
      payload: { email: att1Email, password: pwd },
    });
    attendee1Token = JSON.parse(loginAtt1.body).data.tokens.accessToken;

    const loginAtt2 = await app.inject({
      method: 'POST',
      url: '/api/v1/auth/login',
      payload: { email: att2Email, password: pwd },
    });
    attendee2Token = JSON.parse(loginAtt2.body).data.tokens.accessToken;

    // Create Event A (owned by Organizer A)
    const createEventARes = await app.inject({
      method: 'POST',
      url: '/api/v1/events',
      headers: { authorization: `Bearer ${organizerAToken}` },
      payload: {
        name: `Summit Alpha ${Date.now()}`,
        description: 'Organizer A Event',
        date: '2026-11-10',
        location: 'Hall Alpha',
        maxDuration: 3,
      },
    });
    const eventAData = JSON.parse(createEventARes.body).data.event;
    eventAId = eventAData.id;
    eventASlug = eventAData.slug;

    // Create Event B (owned by Organizer B)
    const createEventBRes = await app.inject({
      method: 'POST',
      url: '/api/v1/events',
      headers: { authorization: `Bearer ${organizerBToken}` },
      payload: {
        name: `Summit Beta ${Date.now()}`,
        description: 'Organizer B Event',
        date: '2026-11-15',
        location: 'Hall Beta',
        maxDuration: 2,
      },
    });
    const eventBData = JSON.parse(createEventBRes.body).data.event;
    eventBId = eventBData.id;
    eventBSlug = eventBData.slug;

    // Save custom form on Event A with multiple question types
    const saveFormRes = await app.inject({
      method: 'PUT',
      url: `/api/v1/events/${eventAId}/form`,
      headers: { authorization: `Bearer ${organizerAToken}` },
      payload: {
        questions: [
          {
            id: 'default-full-name',
            label: 'Full Name',
            type: 'SHORT_TEXT',
            isRequired: true,
            isDefaultField: true,
          },
          {
            id: 'default-email',
            label: 'Email Address',
            type: 'SHORT_TEXT',
            isRequired: true,
            isDefaultField: true,
          },
          {
            id: 'q-tshirt',
            label: 'T-Shirt Size',
            type: 'MULTIPLE_CHOICE',
            isRequired: true,
            options: ['S', 'M', 'L', 'XL'],
            isDefaultField: false,
          },
          {
            id: 'q-diet',
            label: 'Dietary Restrictions',
            type: 'CHECKBOX',
            isRequired: false,
            options: ['Vegetarian', 'Vegan', 'Gluten-Free', 'Halal'],
            isDefaultField: false,
          },
          {
            id: 'q-notes',
            label: 'Special Requests',
            type: 'LONG_TEXT',
            isRequired: false,
            isDefaultField: false,
          },
        ],
      },
    });

    const savedQuestions = JSON.parse(saveFormRes.body).data.form.questions;
    tshirtQId = savedQuestions.find((q: any) => q.label === 'T-Shirt Size').id;
    dietQId = savedQuestions.find((q: any) => q.label === 'Dietary Restrictions').id;
    notesQId = savedQuestions.find((q: any) => q.label === 'Special Requests').id;
  });

  afterAll(async () => {
    if (eventAId) {
      await prisma.event.delete({ where: { id: eventAId } }).catch(() => {});
    }
    if (eventBId) {
      await prisma.event.delete({ where: { id: eventBId } }).catch(() => {});
    }
    await prisma.user.deleteMany({
      where: {
        id: { in: [organizerAId, organizerBId, attendee1Id, attendee2Id] },
      },
    }).catch(() => {});
    await app.close();
  });

  it('1. should reject unauthenticated request to event registrations (401)', async () => {
    const res = await app.inject({
      method: 'GET',
      url: `/api/v1/organizer/events/${eventAId}/registrations`,
    });
    expect(res.statusCode).toBe(401);
  });

  it('2. should reject attendee accessing organizer event registrations (403)', async () => {
    const res = await app.inject({
      method: 'GET',
      url: `/api/v1/organizer/events/${eventAId}/registrations`,
      headers: { authorization: `Bearer ${attendee1Token}` },
    });
    expect(res.statusCode).toBe(403);
    const body = JSON.parse(res.body);
    expect(body.message).toContain('Organizer access required');
  });

  it('3. should return 404 for non-existent event', async () => {
    const res = await app.inject({
      method: 'GET',
      url: '/api/v1/organizer/events/non-existent-event-uuid/registrations',
      headers: { authorization: `Bearer ${organizerAToken}` },
    });
    expect(res.statusCode).toBe(404);
    const body = JSON.parse(res.body);
    expect(body.message).toContain('Event not found');
  });

  it('4. should reject Organizer B accessing Organizer A event registrations (403)', async () => {
    const res = await app.inject({
      method: 'GET',
      url: `/api/v1/organizer/events/${eventAId}/registrations`,
      headers: { authorization: `Bearer ${organizerBToken}` },
    });
    expect(res.statusCode).toBe(403);
    const body = JSON.parse(res.body);
    expect(body.message).toContain('permission');
  });

  it('5. should return empty list with count 0 when no attendees have registered yet', async () => {
    const res = await app.inject({
      method: 'GET',
      url: `/api/v1/organizer/events/${eventAId}/registrations`,
      headers: { authorization: `Bearer ${organizerAToken}` },
    });
    expect(res.statusCode).toBe(200);
    const body = JSON.parse(res.body);
    expect(body.success).toBe(true);
    expect(body.data.count).toBe(0);
    expect(body.data.registrations).toEqual([]);
    expect(body.data.event.id).toBe(eventAId);
  });

  it('6. should register attendee 1 with custom answers on Event A', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/api/v1/registrations',
      headers: { authorization: `Bearer ${attendee1Token}` },
      payload: {
        eventId: eventAId,
        fullName: 'Attendee One',
        email: att1Email,
        institution: 'Acme Corp',
        purpose: 'GENERAL_ATTENDEE',
        durationDays: 2,
        vehicleNumber: 'ABC-1234',
        answers: [
          { questionId: tshirtQId, value: 'L' },
          { questionId: dietQId, value: ['Vegetarian', 'Gluten-Free'] },
          { questionId: notesQId, value: 'Wheelchair accessible seating requested.' },
        ],
      },
    });
    expect(res.statusCode).toBe(201);
    const body = JSON.parse(res.body);
    reg1Id = body.data.registration.id;
    expect(reg1Id).toBeDefined();
  });

  it('7. should register attendee 2 with minimal required answers on Event A', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/api/v1/registrations',
      headers: { authorization: `Bearer ${attendee2Token}` },
      payload: {
        eventId: eventAId,
        fullName: 'Attendee Two',
        email: att2Email,
        institution: 'Innovate Labs',
        purpose: 'SPEAKER',
        durationDays: 1,
        answers: [
          { questionId: tshirtQId, value: 'M' },
        ],
      },
    });
    expect(res.statusCode).toBe(201);
    const body = JSON.parse(res.body);
    reg2Id = body.data.registration.id;
    expect(reg2Id).toBeDefined();
  });

  it('8. should return registrations ordered newest first with complete identity and answers', async () => {
    const res = await app.inject({
      method: 'GET',
      url: `/api/v1/organizer/events/${eventAId}/registrations`,
      headers: { authorization: `Bearer ${organizerAToken}` },
    });
    expect(res.statusCode).toBe(200);
    const body = JSON.parse(res.body);
    expect(body.success).toBe(true);
    expect(body.data.count).toBe(2);
    expect(body.data.registrations.length).toBe(2);

    // Newest first: Attendee 2 was registered second
    const firstItem = body.data.registrations[0];
    const secondItem = body.data.registrations[1];

    expect(firstItem.id).toBe(reg2Id);
    expect(firstItem.fullName).toBe('Attendee Two');
    expect(firstItem.email).toBe(att2Email);
    expect(firstItem.status).toBe('CONFIRMED');
    expect(firstItem.purpose).toBe('SPEAKER');
    expect(firstItem.durationDays).toBe(1);
    expect(firstItem.answers.length).toBe(1);
    expect(firstItem.answers[0].questionLabel).toBe('T-Shirt Size');
    expect(firstItem.answers[0].value).toBe('M');

    // Second item is Attendee 1
    expect(secondItem.id).toBe(reg1Id);
    expect(secondItem.fullName).toBe('Attendee One');
    expect(secondItem.email).toBe(att1Email);
    expect(secondItem.institution).toBe('Acme Corp');
    expect(secondItem.phone).toBe('+1 555-0101');
    expect(secondItem.vehicleNumber).toBe('ABC-1234');
    expect(secondItem.attendee.phone).toBe('+1 555-0101');
    expect(secondItem.attendee.organization).toBe('Acme Corp');

    // Verify answers for Attendee 1
    expect(secondItem.answers.length).toBe(3);
    const answersMap = new Map(secondItem.answers.map((a: any) => [a.questionLabel, a.value]));
    expect(answersMap.get('T-Shirt Size')).toBe('L');
    expect(answersMap.get('Dietary Restrictions')).toContain('Vegetarian');
    expect(answersMap.get('Dietary Restrictions')).toContain('Gluten-Free');
    expect(answersMap.get('Special Requests')).toBe('Wheelchair accessible seating requested.');
  });

  it('9. should also resolve registrations by event slug', async () => {
    const res = await app.inject({
      method: 'GET',
      url: `/api/v1/organizer/events/${eventASlug}/registrations`,
      headers: { authorization: `Bearer ${organizerAToken}` },
    });
    expect(res.statusCode).toBe(200);
    const body = JSON.parse(res.body);
    expect(body.data.count).toBe(2);
  });

  it('10. should NOT leak password hashes or refresh tokens in registration list', async () => {
    const res = await app.inject({
      method: 'GET',
      url: `/api/v1/organizer/events/${eventAId}/registrations`,
      headers: { authorization: `Bearer ${organizerAToken}` },
    });
    expect(res.statusCode).toBe(200);
    const text = res.body;
    expect(text).not.toContain('passwordHash');
    expect(text).not.toContain('refreshToken');
    expect(text).not.toContain(pwd);
  });

  it('11. should reject unauthenticated request to single registration details (401)', async () => {
    const res = await app.inject({
      method: 'GET',
      url: `/api/v1/organizer/registrations/${reg1Id}`,
    });
    expect(res.statusCode).toBe(401);
  });

  it('12. should reject attendee accessing single registration details (403)', async () => {
    const res = await app.inject({
      method: 'GET',
      url: `/api/v1/organizer/registrations/${reg1Id}`,
      headers: { authorization: `Bearer ${attendee1Token}` },
    });
    expect(res.statusCode).toBe(403);
  });

  it('13. should return 404 for non-existent registration details', async () => {
    const res = await app.inject({
      method: 'GET',
      url: '/api/v1/organizer/registrations/non-existent-reg-id',
      headers: { authorization: `Bearer ${organizerAToken}` },
    });
    expect(res.statusCode).toBe(404);
  });

  it('14. should reject Organizer B accessing registration belonging to Organizer A (403)', async () => {
    const res = await app.inject({
      method: 'GET',
      url: `/api/v1/organizer/registrations/${reg1Id}`,
      headers: { authorization: `Bearer ${organizerBToken}` },
    });
    expect(res.statusCode).toBe(403);
    const body = JSON.parse(res.body);
    expect(body.message).toContain('permission');
  });

  it('15. should return full registration detail for Organizer A (200)', async () => {
    const res = await app.inject({
      method: 'GET',
      url: `/api/v1/organizer/registrations/${reg1Id}`,
      headers: { authorization: `Bearer ${organizerAToken}` },
    });
    expect(res.statusCode).toBe(200);
    const body = JSON.parse(res.body);
    expect(body.success).toBe(true);
    const reg = body.data.registration;
    expect(reg.id).toBe(reg1Id);
    expect(reg.fullName).toBe('Attendee One');
    expect(reg.email).toBe(att1Email);
    expect(reg.event.id).toBe(eventAId);
    expect(reg.event.title).toContain('Summit Alpha');
    expect(reg.answers.length).toBe(3);
  });

  it('16. should reflect registrationCount in GET /api/v1/organizer/events', async () => {
    const res = await app.inject({
      method: 'GET',
      url: '/api/v1/organizer/events',
      headers: { authorization: `Bearer ${organizerAToken}` },
    });
    expect(res.statusCode).toBe(200);
    const body = JSON.parse(res.body);
    const foundEventA = body.data.events.find((e: any) => e.id === eventAId);
    expect(foundEventA).toBeDefined();
    expect(foundEventA.registrationCount).toBe(2);
  });

  it('17. should reflect registrationCount in GET /api/v1/events/:eventId', async () => {
    const res = await app.inject({
      method: 'GET',
      url: `/api/v1/events/${eventAId}`,
    });
    expect(res.statusCode).toBe(200);
    const body = JSON.parse(res.body);
    expect(body.data.event.registrationCount).toBe(2);
  });
});
