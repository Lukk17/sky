export interface ThreadSource {
  id: number;
  senderEmail?: string | null;
  receiverEmail?: string | null;
  createdTime: string;
  text: string;
  read?: boolean;
}

export interface ThreadGroup<T extends ThreadSource> {
  email: string;
  messages: T[];
  latest: T;
  unread: number;
}

export function groupThreads<T extends ThreadSource>(received: T[], sent: T[], ownEmail: string | null, isRead: (m: T) => boolean): ThreadGroup<T>[] {
  const receivedIds = new Set(received.map((m) => m.id));
  const byOther = new Map<string, T[]>();
  const newestFirst = [...received, ...sent].sort((a, b) => new Date(b.createdTime).getTime() - new Date(a.createdTime).getTime());
  for (const m of newestFirst) {
    const other = receivedIds.has(m.id) ? (m.senderEmail ?? null) : (m.receiverEmail ?? null);
    if (!other) continue;
    if (!byOther.has(other)) byOther.set(other, []);
    byOther.get(other)!.push(m);
  }
  void ownEmail;
  const out: ThreadGroup<T>[] = [];
  for (const [email, msgs] of byOther) {
    const ordered = [...msgs].sort((a, b) => new Date(a.createdTime).getTime() - new Date(b.createdTime).getTime());
    const latest = [...msgs].sort((a, b) => new Date(b.createdTime).getTime() - new Date(a.createdTime).getTime())[0];
    const unread = msgs.filter((m) => receivedIds.has(m.id) && !isRead(m)).length;
    out.push({email, messages: ordered, latest, unread});
  }
  return out.sort((a, b) => new Date(b.latest.createdTime).getTime() - new Date(a.latest.createdTime).getTime());
}
