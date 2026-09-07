import { FormQuestionType } from '@prisma/client';
import { z } from 'zod';

export const formQuestionInputSchema = z
  .object({
    id: z.string().optional(),
    label: z
      .string({ required_error: 'Question label is required' })
      .trim()
      .min(1, 'Question label cannot be empty')
      .max(200, 'Question label cannot exceed 200 characters'),
    type: z.nativeEnum(FormQuestionType, {
      errorMap: () => ({
        message: 'Invalid question type. Allowed types: SHORT_TEXT, LONG_TEXT, MULTIPLE_CHOICE, CHECKBOX',
      }),
    }),
    isRequired: z.boolean().default(false),
    options: z
      .array(z.string().trim().min(1, 'Option cannot be empty').max(100, 'Option cannot exceed 100 characters'))
      .default([]),
    isDefaultField: z.boolean().default(false),
    orderIndex: z.number().int().min(0).default(0),
  })
  .superRefine((data, ctx) => {
    // Validate options for choice-based questions
    if (data.type === FormQuestionType.MULTIPLE_CHOICE || data.type === FormQuestionType.CHECKBOX) {
      const cleanOptions = Array.from(new Set(data.options.map((o) => o.trim()))).filter((o) => o.length > 0);
      if (cleanOptions.length < 2) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: `${data.type} questions require at least 2 distinct non-empty options`,
          path: ['options'],
        });
      }
    }
  });

export const saveFormSchema = z
  .object({
    questions: z
      .array(formQuestionInputSchema, { required_error: 'Questions array is required' })
      .min(1, 'At least 1 question is required in the form'),
  })
  .superRefine((data, ctx) => {
    // Verify default identity fields: Full Name & Email are mandatory and must be required SHORT_TEXT
    const fullNameField = data.questions.find(
      (q) => q.isDefaultField && (q.label.toLowerCase() === 'full name' || q.id === 'default-full-name')
    );
    if (!fullNameField) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: 'Registration form must include the verified "Full Name" default field',
        path: ['questions'],
      });
    } else {
      if (fullNameField.type !== FormQuestionType.SHORT_TEXT) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: '"Full Name" default field must be of type SHORT_TEXT',
          path: ['questions'],
        });
      }
      if (!fullNameField.isRequired) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: '"Full Name" default field is mandatory and must be required',
          path: ['questions'],
        });
      }
    }

    const emailField = data.questions.find(
      (q) =>
        q.isDefaultField &&
        (q.label.toLowerCase() === 'email' ||
          q.label.toLowerCase() === 'email address' ||
          q.id === 'default-email')
    );
    if (!emailField) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: 'Registration form must include the verified "Email Address" default field',
        path: ['questions'],
      });
    } else {
      if (emailField.type !== FormQuestionType.SHORT_TEXT) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: '"Email Address" default field must be of type SHORT_TEXT',
          path: ['questions'],
        });
      }
      if (!emailField.isRequired) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: '"Email Address" default field is mandatory and must be required',
          path: ['questions'],
        });
      }
    }
  });

export type FormQuestionInput = z.infer<typeof formQuestionInputSchema>;
export type SaveFormInput = z.infer<typeof saveFormSchema>;
