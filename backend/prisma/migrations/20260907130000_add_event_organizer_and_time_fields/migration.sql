-- AlterTable
ALTER TABLE "events" ADD COLUMN "date" TEXT,
ADD COLUMN "startTime" TEXT,
ADD COLUMN "endTime" TEXT,
ADD COLUMN "organizerId" TEXT;

-- AddForeignKey
ALTER TABLE "events" ADD CONSTRAINT "events_organizerId_fkey" FOREIGN KEY ("organizerId") REFERENCES "users"("id") ON DELETE SET NULL ON UPDATE CASCADE;
