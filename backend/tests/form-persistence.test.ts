import { FastifyInstance } from 'fastify';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { buildApp } from '../src/app.js';
import { prisma } from '../src/config/prisma.js';
import { UserRole } from '@prisma/client';
import { hashPassword } from '../src/utils/crypto.js';

describe('Phase 4C — Registration Form Persistence & Dynamic Answers', () => {
  let app: FastifyInstance;
  let organizerToken: string;
  let organizerId: string;
  let otherOrganizerToken: string;
  let otherOrganizerId: string;
  let attendeeToken: string;
  let attendeeId: string;
  let eventId: string;
  let eventSlug: string;

  const orgEmail = `org-form-${Date.now()}@propass.id`;
  const otherOrgEmail = `other-org-form-${Date.now()}@propass.id`;
  const attEmail = `att-form-${Date.now()}@propass.id`;
  const pwd = 'Password123!';

  beforeAll(async () => {
    app = await buildApp();
    await app.ready();

    const pwdHash = await hashPassword(pwd);

    // 1. Create main organizer user
    const uOrg = await prisma.user.create({
      data: {
        email: orgEmail,
        passwordHash: pwdHash,
        role: UserRole.ORGANIZER,
      },
    });
    organizerId = uOrg.id;

    // 2. Create second organizer user
    const uOtherOrg = await prisma.user.create({
      data: {
        email: otherOrgEmail,
        passwordHash: pwdHash,
        role: UserRole.ORGANIZER,
      },
    });
    otherOrganizerId = uOtherOrg.id;

    // 3. Create attendee user
    const uAtt = await prisma.user.create({
      data: {
        email: attEmail,
        passwordHash: pwdHash,
        role: UserRole.ATTENDEE,
      },
    });
    attendeeId = uAtt.id;

    // Logins to obtain tokens
    const loginOrg = await app.inject({
      method: 'POST',
      url: '/api/v1/auth/login',
      payload: { email: orgEmail, password: pwd },
    });
    organizerToken = JSON.parse(loginOrg.body).data.tokens.accessToken;

    const loginOtherOrg = await app.inject({
      method: 'POST',
      url: '/api/v1/auth/login',
      payload: { email: otherOrgEmail, password: pwd },
    });
    otherOrganizerToken = JSON.parse(loginOtherOrg.body).data.tokens.accessToken;

    const loginAtt = await app.inject({
      method: 'POST',
      url: '/api/v1/auth/login',
      payload: { email: attEmail, password: pwd },
    });
    attendeeToken = JSON.parse(loginAtt.body).data.tokens.accessToken;

    // 4. Create an event by main organizer
    const createEventRes = await app.inject({
      method: 'POST',
      url: '/api/v1/events',
      headers: { authorization: `Bearer ${organizerToken}` },
      payload: {
        name: `Dynamic Form Summit ${Date.now()}`,
        description: 'Conference testing dynamic forms',
        date: '2026-11-20',
        startTime: '09:00 AM',
        endTime: '05:00 PM',
        location: 'Hall B',
        maxDuration: 2,
      },
    });

    expect(createEventRes.statusCode).toBe(201);
    const eventData = JSON.parse(createEventRes.body).data.event;
    eventId = eventData.id;
    eventSlug = eventData.slug;
  });

  afterAll(async () => {
    if (eventId) {
      await prisma.event.delete({ where: { id: eventId } }).catch(() => {});
    }
    await prisma.user.deleteMany({
      where: {
        id: { in: [organizerId, otherOrganizerId, attendeeId] },
      },
    }).catch(() => {});
    await app.close();
  });

  it('1. should reject form retrieval without authentication (401)', async () => {
    const res = await app.inject({
      method: 'GET',
      url: `/api/v1/events/${eventId}/form`,
    });
    expect(res.statusCode).toBe(401);
  });

  it('2. should reject form retrieval for non-organizer (403)', async () => {
    const res = await app.inject({
      method: 'GET',
      url: `/api/v1/events/${eventId}/form`,
      headers: { authorization: `Bearer ${attendeeToken}` },
    });
    expect(res.statusCode).toBe(403);
  });

  it('3. should reject form save without authentication (401)', async () => {
    const res = await app.inject({
      method: 'PUT',
      url: `/api/v1/events/${eventId}/form`,
      payload: { questions: [] },
    });
    expect(res.statusCode).toBe(401);
  });

  it('4. should reject form save for non-organizer (403)', async () => {
    const res = await app.inject({
      method: 'PUT',
      url: `/api/v1/events/${eventId}/form`,
      headers: { authorization: `Bearer ${attendeeToken}` },
      payload: { questions: [] },
    });
    expect(res.statusCode).toBe(403);
  });

  it('5. should reject form save if organizer does not own the event (403)', async () => {
    const res = await app.inject({
      method: 'PUT',
      url: `/api/v1/events/${eventId}/form`,
      headers: { authorization: `Bearer ${otherOrganizerToken}` },
      payload: {
        questions: [
          { label: 'Full Name', type: 'SHORT_TEXT', isRequired: true, isDefaultField: true },
          { label: 'Email Address', type: 'SHORT_TEXT', isRequired: true, isDefaultField: true },
        ],
      },
    });
    expect(res.statusCode).toBe(403);
  });

  it('6. should reject invalid question type (400)', async () => {
    const res = await app.inject({
      method: 'PUT',
      url: `/api/v1/events/${eventId}/form`,
      headers: { authorization: `Bearer ${organizerToken}` },
      payload: {
        questions: [
          { label: 'Full Name', type: 'SHORT_TEXT', isRequired: true, isDefaultField: true },
          { label: 'Email Address', type: 'SHORT_TEXT', isRequired: true, isDefaultField: true },
          { label: 'Custom', type: 'INVALID_TYPE', isRequired: false },
        ],
      },
    });
    expect(res.statusCode).toBe(400);
  });

  it('7. should reject MULTIPLE_CHOICE question with fewer than 2 options (400)', async () => {
    const res = await app.inject({
      method: 'PUT',
      url: `/api/v1/events/${eventId}/form`,
      headers: { authorization: `Bearer ${organizerToken}` },
      payload: {
        questions: [
          { label: 'Full Name', type: 'SHORT_TEXT', isRequired: true, isDefaultField: true },
          { label: 'Email Address', type: 'SHORT_TEXT', isRequired: true, isDefaultField: true },
          {
            label: 'T-Shirt Size',
            type: 'MULTIPLE_CHOICE',
            isRequired: true,
            options: ['Only One Option'],
          },
        ],
      },
    });
    expect(res.statusCode).toBe(400);
    const body = JSON.parse(res.body);
    expect(body.message).toContain('at least 2 distinct non-empty options');
  });

  it('8. should reject form if mandatory Full Name or Email default fields are missing (400)', async () => {
    const res = await app.inject({
      method: 'PUT',
      url: `/api/v1/events/${eventId}/form`,
      headers: { authorization: `Bearer ${organizerToken}` },
      payload: {
        questions: [
          { label: 'T-Shirt Size', type: 'SHORT_TEXT', isRequired: false },
        ],
      },
    });
    expect(res.statusCode).toBe(400);
    const body = JSON.parse(res.body);
    expect(body.message).toContain('Full Name');
  });

  let savedFormQuestionId1: string;
  let savedFormQuestionId2: string;

  it('9. should successfully save a customized form with custom questions (200)', async () => {
    const res = await app.inject({
      method: 'PUT',
      url: `/api/v1/events/${eventId}/form`,
      headers: { authorization: `Bearer ${organizerToken}` },
      payload: {
        questions: [
          {
            id: 'default-full-name',
            label: 'Full Name',
            type: 'SHORT_TEXT',
            isRequired: true,
            isDefaultField: true,
            orderIndex: 0,
          },
          {
            id: 'default-email',
            label: 'Email Address',
            type: 'SHORT_TEXT',
            isRequired: true,
            isDefaultField: true,
            orderIndex: 1,
          },
          {
            id: 'default-phone',
            label: 'Phone Number',
            type: 'SHORT_TEXT',
            isRequired: true,
            isDefaultField: true,
            orderIndex: 2,
          },
          {
            label: 'Dietary Preference',
            type: 'MULTIPLE_CHOICE',
            isRequired: true,
            options: ['Vegetarian', 'Vegan', 'Standard'],
            isDefaultField: false,
            orderIndex: 3,
          },
          {
            label: 'GitHub Username',
            type: 'SHORT_TEXT',
            isRequired: false,
            options: [],
            isDefaultField: false,
            orderIndex: 4,
          },
        ],
      },
    });

    expect(res.statusCode).toBe(200);
    const body = JSON.parse(res.body);
    expect(body.success).toBe(true);
    expect(body.data.form.questions.length).toBe(5);

    const questions = body.data.form.questions;
    expect(questions[0].label).toBe('Full Name');
    expect(questions[3].label).toBe('Dietary Preference');
    expect(questions[3].options).toEqual(['Vegetarian', 'Vegan', 'Standard']);
    expect(questions[4].label).toBe('GitHub Username');

    savedFormQuestionId1 = questions[3].id;
    savedFormQuestionId2 = questions[4].id;
  });

  it('10. should retrieve the saved form via GET /api/v1/events/:eventId/form (200)', async () => {
    const res = await app.inject({
      method: 'GET',
      url: `/api/v1/events/${eventId}/form`,
      headers: { authorization: `Bearer ${organizerToken}` },
    });

    expect(res.statusCode).toBe(200);
    const body = JSON.parse(res.body);
    expect(body.success).toBe(true);
    expect(body.data.form.questions.length).toBe(5);
  });

  it('11. should include form and questions in GET /api/v1/events/:eventId for attendee', async () => {
    const res = await app.inject({
      method: 'GET',
      url: `/api/v1/events/${eventSlug}`,
    });

    expect(res.statusCode).toBe(200);
    const body = JSON.parse(res.body);
    expect(body.data.event.form).toBeDefined();
    expect(body.data.event.form.questions.length).toBe(5);
  });

  it('12. should reject attendee registration when required custom question answer is missing (400)', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/api/v1/registrations',
      headers: { authorization: `Bearer ${attendeeToken}` },
      payload: {
        eventId,
        fullName: 'Jane Doe',
        email: 'jane.doe@example.com',
        institution: 'Tech Corp',
        purpose: 'GENERAL_ATTENDEE',
        durationDays: 1,
        answers: [
          // Missing required Dietary Preference (savedFormQuestionId1)
          { questionId: savedFormQuestionId2, value: 'janedoe' },
        ],
      },
    });

    expect(res.statusCode).toBe(400);
    const body = JSON.parse(res.body);
    expect(body.message).toContain('Dietary Preference');
  });

  it('13. should reject attendee registration with invalid option for MULTIPLE_CHOICE (400)', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/api/v1/registrations',
      headers: { authorization: `Bearer ${attendeeToken}` },
      payload: {
        eventId,
        fullName: 'Jane Doe',
        email: 'jane.doe@example.com',
        institution: 'Tech Corp',
        purpose: 'GENERAL_ATTENDEE',
        durationDays: 1,
        answers: [
          { questionId: savedFormQuestionId1, value: 'Gluten-Free Non-Existent' },
        ],
      },
    });

    expect(res.statusCode).toBe(400);
    const body = JSON.parse(res.body);
    expect(body.message).toContain('Allowed options');
  });

  it('14. should reject answer referencing an invalid question ID (400)', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/api/v1/registrations',
      headers: { authorization: `Bearer ${attendeeToken}` },
      payload: {
        eventId,
        fullName: 'Jane Doe',
        email: 'jane.doe@example.com',
        institution: 'Tech Corp',
        purpose: 'GENERAL_ATTENDEE',
        durationDays: 1,
        answers: [
          { questionId: '11111111-2222-3333-4444-555555555555', value: 'Any' },
        ],
      },
    });

    expect(res.statusCode).toBe(400);
    const body = JSON.parse(res.body);
    expect(body.message).toContain('does not belong to this event');
  });

  it('15. should successfully register attendee with valid custom answers (201)', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/api/v1/registrations',
      headers: { authorization: `Bearer ${attendeeToken}` },
      payload: {
        eventId,
        fullName: 'Jane Doe',
        email: 'jane.doe@example.com',
        institution: 'Tech Corp',
        purpose: 'GENERAL_ATTENDEE',
        durationDays: 1,
        vehicleNumber: 'KA01AB1234',
        answers: [
          { questionId: savedFormQuestionId1, value: 'Vegetarian' },
          { questionId: savedFormQuestionId2, value: 'janedoe' },
        ],
      },
    });

    expect(res.statusCode).toBe(201);
    const body = JSON.parse(res.body);
    expect(body.success).toBe(true);
    expect(body.data.registration.answers.length).toBe(2);
    expect(body.data.registration.answers[0].value).toBe('Vegetarian');
  });

  it('16. should reject duplicate registration on the same event (409)', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/api/v1/registrations',
      headers: { authorization: `Bearer ${attendeeToken}` },
      payload: {
        eventId,
        fullName: 'Jane Doe',
        email: 'jane.doe@example.com',
        institution: 'Tech Corp',
        purpose: 'GENERAL_ATTENDEE',
        durationDays: 1,
      },
    });

    expect(res.statusCode).toBe(409);
  });
});
