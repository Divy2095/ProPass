import { FormQuestionType, RegistrationStatus } from '@prisma/client';
import { prisma } from '../config/prisma.js';
import { CreateRegistrationInput } from '../models/registration.schema.js';
import { EventService } from './event.service.js';

export class RegistrationService {
  /**
   * Submits and persists a new event registration for the authenticated user,
   * including any answers to organizer-defined dynamic form questions.
   */
  static async createRegistration(userId: string, input: CreateRegistrationInput) {
    // 1. Resolve and validate the target event
    const event = await EventService.getEventByIdOrSlug(input.eventId);

    // 2. Validate durationDays against event's maxDuration
    if (input.durationDays > event.maxDuration) {
      const error: any = new Error(
        `Duration (${input.durationDays} days) exceeds event maximum allowed duration (${event.maxDuration} days)`
      );
      error.statusCode = 400;
      throw error;
    }

    // 3. Check for existing duplicate registration
    const existingRegistration = await prisma.registration.findUnique({
      where: {
        userId_eventId: {
          userId,
          eventId: event.id,
        },
      },
    });

    if (existingRegistration) {
      const error: any = new Error(`You are already registered for "${event.title}"`);
      error.statusCode = 409;
      throw error;
    }

    // 4. Retrieve event registration form and validate answers if form exists
    const form = await prisma.registrationForm.findUnique({
      where: { eventId: event.id },
      include: {
        questions: true,
      },
    });

    const validatedAnswers: { questionId: string; value: string }[] = [];

    if (form && form.questions.length > 0) {
      const questionMap = new Map(form.questions.map((q) => [q.id, q]));
      const submittedAnswersMap = new Map(input.answers.map((a) => [a.questionId, a.value]));

      // Validate each submitted answer
      for (const ans of input.answers) {
        const question = questionMap.get(ans.questionId);
        if (!question) {
          const error: any = new Error(
            `Invalid question ID: "${ans.questionId}" does not belong to this event registration form`
          );
          error.statusCode = 400;
          throw error;
        }

        // Validate by question type
        if (question.type === FormQuestionType.MULTIPLE_CHOICE) {
          if (typeof ans.value !== 'string' || !question.options.includes(ans.value.trim())) {
            const error: any = new Error(
              `Invalid option for question "${question.label}". Allowed options: ${question.options.join(', ')}`
            );
            error.statusCode = 400;
            throw error;
          }
          validatedAnswers.push({
            questionId: question.id,
            value: ans.value.trim(),
          });
        } else if (question.type === FormQuestionType.CHECKBOX) {
          const selected = Array.isArray(ans.value)
            ? ans.value.map((v) => v.trim()).filter((v) => v.length > 0)
            : ans.value
                .split(',')
                .map((v) => v.trim())
                .filter((v) => v.length > 0);

          for (const item of selected) {
            if (!question.options.includes(item)) {
              const error: any = new Error(
                `Invalid option "${item}" for question "${question.label}". Allowed options: ${question.options.join(', ')}`
              );
              error.statusCode = 400;
              throw error;
            }
          }

          validatedAnswers.push({
            questionId: question.id,
            value: selected.join(', '),
          });
        } else if (question.type === FormQuestionType.SHORT_TEXT) {
          const strVal = Array.isArray(ans.value) ? ans.value.join(', ') : ans.value.trim();
          if (strVal.length > 500) {
            const error: any = new Error(
              `Answer for question "${question.label}" cannot exceed 500 characters`
            );
            error.statusCode = 400;
            throw error;
          }
          validatedAnswers.push({
            questionId: question.id,
            value: strVal,
          });
        } else if (question.type === FormQuestionType.LONG_TEXT) {
          const strVal = Array.isArray(ans.value) ? ans.value.join('\n') : ans.value.trim();
          if (strVal.length > 2000) {
            const error: any = new Error(
              `Answer for question "${question.label}" cannot exceed 2000 characters`
            );
            error.statusCode = 400;
            throw error;
          }
          validatedAnswers.push({
            questionId: question.id,
            value: strVal,
          });
        }
      }

      // Check mandatory required questions
      for (const q of form.questions) {
        if (q.isRequired && !q.isDefaultField) {
          const submittedVal = submittedAnswersMap.get(q.id);
          const hasValue =
            submittedVal !== undefined &&
            (Array.isArray(submittedVal) ? submittedVal.length > 0 : submittedVal.trim().length > 0);

          if (!hasValue) {
            const error: any = new Error(`Answer is required for field "${q.label}"`);
            error.statusCode = 400;
            throw error;
          }
        }
      }
    }

    // 5. Create the registration and answer records in PostgreSQL transaction
    return await prisma.$transaction(async (tx) => {
      const registration = await tx.registration.create({
        data: {
          userId,
          eventId: event.id,
          fullName: input.fullName,
          email: input.email,
          institution: input.institution,
          purpose: input.purpose,
          durationDays: input.durationDays,
          vehicleNumber: input.vehicleNumber,
          status: RegistrationStatus.CONFIRMED,
        },
        include: {
          event: {
            select: {
              id: true,
              slug: true,
              title: true,
              overline: true,
              subtitle: true,
              location: true,
              startDate: true,
              endDate: true,
            },
          },
        },
      });

      if (validatedAnswers.length > 0) {
        await tx.registrationAnswer.createMany({
          data: validatedAnswers.map((va) => ({
            registrationId: registration.id,
            questionId: va.questionId,
            value: va.value,
          })),
        });
      }

      // Issue or update attendee Digital Pass upon confirmed registration
      let tier = 'STANDARD';
      if (input.purpose === 'SPEAKER') {
        tier = 'VIP';
      } else if (input.purpose === 'SPONSOR_EXHIBITOR') {
        tier = 'ALL-ACCESS';
      } else {
        tier = 'PREMIUM';
      }

      const existingPass = await tx.digitalPass.findUnique({
        where: { userId },
      });

      if (!existingPass) {
        const currentYear = new Date().getFullYear();
        let passNumber = '';
        let passUnique = false;
        while (!passUnique) {
          const randomNum = Math.floor(1000 + Math.random() * 9000);
          passNumber = `PP-${currentYear}-${randomNum}`;
          const clash = await tx.digitalPass.findUnique({ where: { passNumber } });
          if (!clash) passUnique = true;
        }

        const qrPayload = `propass:pass:${passNumber}`;

        const oneYearFromNow = new Date();
        oneYearFromNow.setFullYear(oneYearFromNow.getFullYear() + 1);
        const eventEnd = event.endDate ? new Date(event.endDate) : null;
        const passExpiresAt = (eventEnd && eventEnd > oneYearFromNow) ? eventEnd : oneYearFromNow;

        await tx.digitalPass.create({
          data: {
            userId,
            passNumber,
            qrPayload,
            tier,
            isActive: true,
            expiresAt: passExpiresAt,
          },
        });
      } else {
        const oneYearFromNow = new Date();
        oneYearFromNow.setFullYear(oneYearFromNow.getFullYear() + 1);
        const eventEnd = event.endDate ? new Date(event.endDate) : null;
        const passExpiresAt = (eventEnd && eventEnd > oneYearFromNow) ? eventEnd : oneYearFromNow;

        await tx.digitalPass.update({
          where: { userId },
          data: {
            isActive: true,
            tier: existingPass.tier || tier,
            expiresAt: (existingPass.expiresAt && existingPass.expiresAt > new Date()) ? existingPass.expiresAt : passExpiresAt,
          },
        });
      }

      // Sync attendee profile with registered name and institution
      await tx.profile.upsert({
        where: { userId },
        update: {
          fullName: input.fullName,
          organization: input.institution,
        },
        create: {
          userId,
          fullName: input.fullName,
          organization: input.institution,
        },
      });

      const completeRegistration = await tx.registration.findUnique({
        where: { id: registration.id },
        include: {
          event: {
            select: {
              id: true,
              slug: true,
              title: true,
              overline: true,
              subtitle: true,
              location: true,
              startDate: true,
              endDate: true,
            },
          },
          answers: {
            include: {
              question: {
                select: {
                  id: true,
                  label: true,
                  type: true,
                },
              },
            },
          },
        },
      });

      return completeRegistration!;
    });
  }

  /**
   * Retrieves all event registrations belonging to the authenticated user.
   */
  static async getMyRegistrations(userId: string) {
    const registrations = await prisma.registration.findMany({
      where: { userId },
      orderBy: { registeredAt: 'desc' },
      include: {
        event: {
          select: {
            id: true,
            slug: true,
            title: true,
            overline: true,
            subtitle: true,
            location: true,
            startDate: true,
            endDate: true,
          },
        },
        answers: {
          include: {
            question: {
              select: {
                id: true,
                label: true,
                type: true,
              },
            },
          },
        },
      },
    });

    return registrations;
  }

  /**
   * Formats a raw database registration record into a safe, structured organizer representation.
   */
  private static formatRegistrationForOrganizer(reg: any) {
    const phoneAnswer = reg.answers?.find(
      (a: any) => a.question?.label?.toLowerCase().includes('phone') || a.question?.id === 'default-phone'
    );
    const phone = phoneAnswer?.value || reg.user?.profile?.phone || null;

    return {
      id: reg.id,
      eventId: reg.eventId,
      userId: reg.userId,
      status: reg.status,
      purpose: reg.purpose,
      durationDays: reg.durationDays,
      vehicleNumber: reg.vehicleNumber,
      registeredAt: reg.registeredAt ? reg.registeredAt.toISOString() : reg.createdAt.toISOString(),
      createdAt: reg.registeredAt ? reg.registeredAt.toISOString() : reg.createdAt.toISOString(),
      updatedAt: reg.updatedAt.toISOString(),
      fullName: reg.fullName,
      email: reg.email,
      institution: reg.institution,
      phone,
      attendee: {
        id: reg.userId,
        fullName: reg.fullName,
        email: reg.email,
        institution: reg.institution,
        phone,
        organization: reg.user?.profile?.organization || reg.institution,
        title: reg.user?.profile?.title || null,
        avatarUrl: reg.user?.profile?.avatarUrl || null,
      },
      answers: (reg.answers || []).map((ans: any) => ({
        id: ans.id,
        questionId: ans.questionId,
        questionLabel: ans.question?.label || '',
        questionType: ans.question?.type || 'SHORT_TEXT',
        value: ans.value,
        options: ans.question?.options || [],
        orderIndex: ans.question?.orderIndex ?? 0,
        isDefaultField: ans.question?.isDefaultField ?? false,
      })),
    };
  }

  /**
   * Retrieves all registrations for an event owned by the authenticated organizer.
   * Enforces event existence and ownership check.
   */
  static async getEventRegistrationsForOrganizer(eventId: string, organizerId: string) {
    const cleanId = eventId.trim();

    const event = await prisma.event.findFirst({
      where: {
        OR: [{ id: cleanId }, { slug: cleanId }],
      },
      select: {
        id: true,
        title: true,
        slug: true,
        organizerId: true,
      },
    });

    if (!event) {
      const error: any = new Error('Event not found');
      error.statusCode = 404;
      throw error;
    }

    if (event.organizerId !== organizerId) {
      const error: any = new Error('You do not have permission to view registrations for this event');
      error.statusCode = 403;
      throw error;
    }

    const registrations = await prisma.registration.findMany({
      where: { eventId: event.id },
      orderBy: { registeredAt: 'desc' },
      include: {
        user: {
          select: {
            id: true,
            email: true,
            profile: {
              select: {
                phone: true,
                organization: true,
                title: true,
                avatarUrl: true,
              },
            },
          },
        },
        answers: {
          include: {
            question: {
              select: {
                id: true,
                label: true,
                type: true,
                options: true,
                orderIndex: true,
                isDefaultField: true,
              },
            },
          },
          orderBy: {
            question: {
              orderIndex: 'asc',
            },
          },
        },
      },
    });

    const formatted = registrations.map((r) => RegistrationService.formatRegistrationForOrganizer(r));

    return {
      event: {
        id: event.id,
        title: event.title,
        slug: event.slug,
      },
      registrations: formatted,
      count: formatted.length,
    };
  }

  /**
   * Retrieves detail for a single registration for an event owned by the authenticated organizer.
   */
  static async getRegistrationDetailForOrganizer(registrationId: string, organizerId: string) {
    const cleanId = registrationId.trim();

    const reg = await prisma.registration.findUnique({
      where: { id: cleanId },
      include: {
        event: {
          select: {
            id: true,
            title: true,
            slug: true,
            organizerId: true,
          },
        },
        user: {
          select: {
            id: true,
            email: true,
            profile: {
              select: {
                phone: true,
                organization: true,
                title: true,
                avatarUrl: true,
              },
            },
          },
        },
        answers: {
          include: {
            question: {
              select: {
                id: true,
                label: true,
                type: true,
                options: true,
                orderIndex: true,
                isDefaultField: true,
              },
            },
          },
          orderBy: {
            question: {
              orderIndex: 'asc',
            },
          },
        },
      },
    });

    if (!reg) {
      const error: any = new Error('Registration not found');
      error.statusCode = 404;
      throw error;
    }

    if (reg.event.organizerId !== organizerId) {
      const error: any = new Error('You do not have permission to view this registration');
      error.statusCode = 403;
      throw error;
    }

    const formatted = RegistrationService.formatRegistrationForOrganizer(reg);

    return {
      registration: {
        ...formatted,
        event: {
          id: reg.event.id,
          title: reg.event.title,
          slug: reg.event.slug,
        },
      },
    };
  }
}
