const basicKeys = new Set(['id', 'name', 'background', 'tone', 'rules']);

export function emptyDraft() {
  return {
    id: '', name: '', background: '', tone: '', rulesText: '',
    detailsText: JSON.stringify({ factions: [], locations: [], resources: [], attributes: [], custom: {} }, null, 2),
  };
}

export function toDraft(world) {
  const details = Object.fromEntries(Object.entries(world).filter(([key]) => !basicKeys.has(key)));
  return {
    id: world.id, name: world.name, background: world.background, tone: world.tone,
    rulesText: world.rules.join('\n'), detailsText: JSON.stringify(details, null, 2),
  };
}

export function toWorld(draft) {
  let details;
  try { details = JSON.parse(draft.detailsText); }
  catch { throw new Error('Custom details must be valid JSON. Check commas, brackets, and quotation marks.'); }
  if (!details || Array.isArray(details) || typeof details !== 'object')
    throw new Error('Custom details must be a JSON object.');
  if (Object.keys(details).some(key => basicKeys.has(key)))
    throw new Error('Set id, name, background, tone, and rules in the form above, rather than custom details.');
  if (!/^[a-z0-9][a-z0-9-]{0,63}$/.test(draft.id))
    throw new Error('World ID must be 1–64 lowercase letters, digits, or hyphens, starting with a letter or digit.');
  for (const key of ['name', 'background', 'tone']) {
    if (!draft[key].trim()) throw new Error(`Please enter the world's ${key}.`);
  }
  return {
    ...details, id: draft.id, name: draft.name, background: draft.background, tone: draft.tone,
    rules: draft.rulesText.split('\n').filter(rule => rule.trim()),
  };
}

export function readDraft(storage) {
  try {
    const value = JSON.parse(storage.getItem('worldloom-draft'));
    if (value && Object.keys(emptyDraft()).every(key => typeof value[key] === 'string')) return value;
  } catch { /* An unavailable browser store should not prevent world creation. */ }
  return emptyDraft();
}
