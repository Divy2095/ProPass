-- CreateEnum
CREATE TYPE "UserRole" AS ENUM ('ATTENDEE', 'ORGANIZER');

-- AlterTable
ALTER TABLE "users" ALTER COLUMN "role" DROP DEFAULT;

ALTER TABLE "users" ALTER COLUMN "role" TYPE "UserRole" USING (
    CASE
        WHEN "role"::text = 'ORGANIZER' THEN 'ORGANIZER'::"UserRole"
        ELSE 'ATTENDEE'::"UserRole"
    END
);

ALTER TABLE "users" ALTER COLUMN "role" SET DEFAULT 'ATTENDEE';

-- DropEnum
DROP TYPE "Role";
