import { db } from '../db/index.js';
import { jobs, buildResults, raceStatistics } from '../db/schema.js';
import { eq } from 'drizzle-orm';

interface BuildResultData {
  buildType: 'elide' | 'standard';
  duration: number;
  status: string;
}

/**
 * Determine race winner based on build durations
 */
export function determineWinner(
  elideDuration: number,
  standardDuration: number
): 'elide' | 'standard' | 'tie' {
  if (Math.abs(elideDuration - standardDuration) < 1) {
    return 'tie';
  } else if (elideDuration < standardDuration) {
    return 'elide';
  } else {
    return 'standard';
  }
}

/**
 * Update race statistics when a race completes
 */
export async function updateRaceStatistics(jobId: string): Promise<void> {
  try {
    const job = await db.select().from(jobs).where(eq(jobs.id, jobId)).limit(1);
    if (job.length === 0) return;

    const results = await db.select().from(buildResults).where(eq(buildResults.jobId, jobId));

    const elideResult = results.find((r) => r.buildType === 'elide') as BuildResultData | undefined;
    const standardResult = results.find((r) => r.buildType === 'standard') as
      | BuildResultData
      | undefined;

    if (!elideResult || !standardResult) return;

    // Determine winner
    const winner = determineWinner(elideResult.duration, standardResult.duration);

    // Get or create statistics record
    const existingStats = await db
      .select()
      .from(raceStatistics)
      .where(eq(raceStatistics.repositoryUrl, job[0].repositoryUrl))
      .limit(1);

    if (existingStats.length > 0) {
      // Update existing statistics
      const stats = existingStats[0];
      const newTotalRaces = stats.totalRaces + 1;
      const newElideWins = stats.elideWins + (winner === 'elide' ? 1 : 0);
      const newStandardWins = stats.standardWins + (winner === 'standard' ? 1 : 0);
      const newTies = stats.ties + (winner === 'tie' ? 1 : 0);

      // Calculate new averages
      const newElideAvg = Math.round(
        ((stats.elideAvgDuration || 0) * stats.totalRaces + elideResult.duration) / newTotalRaces
      );
      const newStandardAvg = Math.round(
        ((stats.standardAvgDuration || 0) * stats.totalRaces + standardResult.duration) /
          newTotalRaces
      );

      await db
        .update(raceStatistics)
        .set({
          totalRaces: newTotalRaces,
          elideWins: newElideWins,
          standardWins: newStandardWins,
          ties: newTies,
          elideAvgDuration: newElideAvg,
          standardAvgDuration: newStandardAvg,
          lastRaceAt: new Date(),
          lastRaceJobId: jobId,
          updatedAt: new Date(),
        })
        .where(eq(raceStatistics.id, stats.id));
    } else {
      // Create new statistics record
      await db.insert(raceStatistics).values({
        repositoryUrl: job[0].repositoryUrl,
        repositoryName: job[0].repositoryName,
        totalRaces: 1,
        elideWins: winner === 'elide' ? 1 : 0,
        standardWins: winner === 'standard' ? 1 : 0,
        ties: winner === 'tie' ? 1 : 0,
        elideAvgDuration: elideResult.duration,
        standardAvgDuration: standardResult.duration,
        lastRaceAt: new Date(),
        lastRaceJobId: jobId,
        createdAt: new Date(),
        updatedAt: new Date(),
      });
    }
  } catch (error) {
    console.error('Error updating race statistics:', error);
  }
}
