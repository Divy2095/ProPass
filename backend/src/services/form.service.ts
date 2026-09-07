import { FormQuestionType } from '@prisma/client';
import { prisma } from '../config/prisma.js';
import { FormQuestionInput } from '../models/form.schema.js';

export class FormService {
  /**
   * Default verified identity questions returned when an event has no customized form.
   */
  static getDefaultQuestions() {
    return [
      {
        id: 'default-full-name',
        label: 'Full Name',
        type: FormQuestionType.SHORT_TEXT,
        isRequired: true,
        options: [],
        isDefaultField: true,
        orderIndex: 0,
      },
      {
        id: 'default-email',
        label: 'Email Address',
        type: FormQuestionType.SHORT_TEXT,
        isRequired: true,
        options: [],
        isDefaultField: true,
        orderIndex: 1,
      },
      {
        id: 'default-phone',
        label: 'Phone Number',
        type: FormQuestionType.SHORT_TEXT,
        isRequired: false,
        options: [],
        isDefaultField: true,
        orderIndex: 2,
      },
    ];
  }

  /**
   * Retrieves the registration form for an event.
   * If organizerId is provided, ownership is strictly enforced (403 if not owner).
   */
  static async getFormByEventId(eventIdOrSlug: string, organizerId?: string) {
    const cleanId = eventIdOrSlug.trim();

    const event = await prisma.event.findFirst({
      where: {
        OR: [{ id: cleanId }, { slug: cleanId.toLowerCase() }],
      },
      include: {
        form: {
          include: {
            questions: {
              orderBy: { orderIndex: 'asc' },
            },
          },
        },
      },
    });

    if (!event) {
      const error: any = new Error(`Event "${cleanId}" not found`);
      error.statusCode = 404;
      throw error;
    }

    if (organizerId && event.organizerId && event.organizerId !== organizerId) {
      const error: any = new Error('You do not have permission to view or manage this event form');
      error.statusCode = 403;
      throw error;
    }

    if (!event.form) {
      return {
        id: `default-form-${event.id}`,
        eventId: event.id,
        questions: FormService.getDefaultQuestions(),
        createdAt: event.createdAt,
        updatedAt: event.updatedAt,
      };
    }

    return event.form;
  }

  /**
   * Persists or updates the complete registration form definition for an event.
   * Atomic operation replacing the questions and preserving ordering.
   */
  static async saveForm(eventIdOrSlug: string, organizerId: string, questions: FormQuestionInput[]) {
    const cleanId = eventIdOrSlug.trim();

    const event = await prisma.event.findFirst({
      where: {
        OR: [{ id: cleanId }, { slug: cleanId.toLowerCase() }],
      },
      include: {
        form: true,
      },
    });

    if (!event) {
      const error: any = new Error(`Event "${cleanId}" not found`);
      error.statusCode = 404;
      throw error;
    }

    if (event.organizerId && event.organizerId !== organizerId) {
      const error: any = new Error('You do not have permission to modify this event form');
      error.statusCode = 403;
      throw error;
    }

    return await prisma.$transaction(async (tx) => {
      // 1. Find or create registration form
      let form = event.form;
      if (!form) {
        form = await tx.registrationForm.create({
          data: {
            eventId: event.id,
          },
        });
      } else {
        await tx.registrationForm.update({
          where: { id: form.id },
          data: { updatedAt: new Date() },
        });
      }

      // 2. Clear previous questions for this form
      await tx.formQuestion.deleteMany({
        where: { formId: form.id },
      });

      // 3. Insert updated questions with deterministic orderIndex
      for (let i = 0; i < questions.length; i++) {
        const q = questions[i];
        const options =
          q.type === FormQuestionType.MULTIPLE_CHOICE || q.type === FormQuestionType.CHECKBOX
            ? Array.from(new Set(q.options.map((o) => o.trim()))).filter((o) => o.length > 0)
            : [];

        await tx.formQuestion.create({
          data: {
            formId: form.id,
            label: q.label,
            type: q.type,
            isRequired: q.isRequired,
            options,
            isDefaultField: q.isDefaultField,
            orderIndex: q.orderIndex ?? i,
          },
        });
      }

      // 4. Retrieve saved form with ordered questions
      const savedForm = await tx.registrationForm.findUnique({
        where: { id: form.id },
        include: {
          questions: {
            orderBy: { orderIndex: 'asc' },
          },
        },
      });

      return savedForm!;
    });
  }
}
