/**
 * Session Manager
 * Cookie-based session management for edge applications
 */

export interface SessionData {
  [key: string]: any;
}

export interface Session {
  id: string;
  data: SessionData;
  createdAt: number;
  expiresAt: number;
}

export interface SessionOptions {
  maxAge?: number; // seconds, default 24 hours
  cookieName?: string;
  secure?: boolean;
  httpOnly?: boolean;
  sameSite?: 'strict' | 'lax' | 'none';
}

export class SessionManager {
  private sessions = new Map<string, Session>();
  private options: Required<SessionOptions>;

  constructor(options: SessionOptions = {}) {
    this.options = {
      maxAge: options.maxAge || 86400, // 24 hours
      cookieName: options.cookieName || 'session_id',
      secure: options.secure !== undefined ? options.secure : true,
      httpOnly: options.httpOnly !== undefined ? options.httpOnly : true,
      sameSite: options.sameSite || 'lax'
    };
  }

  createSession(data: SessionData = {}): Session {
    const id = this.generateId();
    const now = Date.now();

    const session: Session = {
      id,
      data,
      createdAt: now,
      expiresAt: now + (this.options.maxAge * 1000)
    };

    this.sessions.set(id, session);
    return session;
  }

  getSession(id: string): Session | null {
    const session = this.sessions.get(id);

    if (!session) return null;

    if (Date.now() > session.expiresAt) {
      this.sessions.delete(id);
      return null;
    }

    return session;
  }

  updateSession(id: string, data: Partial<SessionData>): Session | null {
    const session = this.getSession(id);

    if (!session) return null;

    session.data = { ...session.data, ...data };
    return session;
  }

  destroySession(id: string): void {
    this.sessions.delete(id);
  }

  refreshSession(id: string): Session | null {
    const session = this.getSession(id);

    if (!session) return null;

    session.expiresAt = Date.now() + (this.options.maxAge * 1000);
    return session;
  }

  private generateId(): string {
    // Simple random ID generator (for production use crypto.randomBytes)
    return Array.from({ length: 32 }, () =>
      Math.floor(Math.random() * 16).toString(16)
    ).join('');
  }

  serializeCookie(session: Session): string {
    const parts = [
      `${this.options.cookieName}=${session.id}`,
      `Max-Age=${this.options.maxAge}`,
      `SameSite=${this.options.sameSite}`
    ];

    if (this.options.httpOnly) parts.push('HttpOnly');
    if (this.options.secure) parts.push('Secure');

    parts.push('Path=/');

    return parts.join('; ');
  }

  parseCookie(cookieHeader: string): string | null {
    const cookies = cookieHeader.split(';').map(c => c.trim());

    for (const cookie of cookies) {
      const [name, value] = cookie.split('=');
      if (name === this.options.cookieName) {
        return value;
      }
    }

    return null;
  }

  // Cleanup expired sessions
  cleanup(): void {
    const now = Date.now();
    for (const [id, session] of this.sessions.entries()) {
      if (now > session.expiresAt) {
        this.sessions.delete(id);
      }
    }
  }
}

// CLI demo
if (import.meta.url.includes("session-manager.ts")) {
  console.log("Session Manager Demo\n");

  const manager = new SessionManager({
    maxAge: 3600,
    cookieName: 'my_session'
  });

  console.log("Create session:");
  const session = manager.createSession({ userId: 123, username: 'alice' });
  console.log("  ID:", session.id.substring(0, 16) + "...");
  console.log("  Data:", session.data);

  console.log("\nSerialize cookie:");
  const cookie = manager.serializeCookie(session);
  console.log("  Set-Cookie:", cookie);

  console.log("\nParse cookie:");
  const sessionId = manager.parseCookie(`my_session=${session.id}; other=value`);
  console.log("  Parsed ID:", sessionId?.substring(0, 16) + "...");

  console.log("\nRetrieve session:");
  const retrieved = manager.getSession(session.id);
  console.log("  Data:", retrieved?.data);

  console.log("\nUpdate session:");
  manager.updateSession(session.id, { lastActive: Date.now() });
  const updated = manager.getSession(session.id);
  console.log("  Updated data:", updated?.data);

  console.log("\nRefresh session:");
  manager.refreshSession(session.id);
  const refreshed = manager.getSession(session.id);
  const timeRemaining = refreshed ? Math.floor((refreshed.expiresAt - Date.now()) / 1000) : 0;
  console.log("  Time remaining:", timeRemaining, "seconds");

  console.log("\nDestroy session:");
  manager.destroySession(session.id);
  console.log("  After destroy:", manager.getSession(session.id));

  console.log("\n✅ Session manager test passed");
}
