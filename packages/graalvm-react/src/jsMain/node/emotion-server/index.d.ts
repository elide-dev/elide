import type { EmotionCache } from "@emotion/utils";

interface EmotionCriticalToChunks {
  html: string;
  styles: { key: string; ids: string[]; css: string }[];
}

interface EmotionServer {
  constructStyleTagsFromChunks: (
    criticalData: EmotionCriticalToChunks
  ) => string;
  extractCriticalToChunks: (html: string) => EmotionCriticalToChunks;
}

export function createEmotionServer(cache: EmotionCache): EmotionServer;
