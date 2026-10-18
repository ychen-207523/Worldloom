import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { emptyDraft, readDraft, toDraft, toWorld } from './world.mjs';

test('both settings retain every field through the editor', () => {
  for (const name of ['ashen-empire', 'orbital-garden']) {
    const world = JSON.parse(readFileSync(new URL(`../../examples/${name}.json`, import.meta.url)));
    assert.deepEqual(toWorld(toDraft(world)), world);
  }
});

test('custom details cannot silently replace form fields', () => {
  const draft = { ...emptyDraft(), detailsText: '{"name":"hidden override"}' };
  assert.throws(() => toWorld(draft), /form above/);
});

test('invalid JSON remains an editable error rather than a discarded draft', () => {
  const draft = { ...emptyDraft(), detailsText: '{' };
  assert.throws(() => toWorld(draft), /valid JSON/);
  assert.equal(readDraft({ getItem: () => JSON.stringify(draft) }).detailsText, '{');
});

test('invalid browser storage falls back to an empty draft', () => {
  assert.deepEqual(readDraft({ getItem: () => '{' }), emptyDraft());
  assert.deepEqual(readDraft({ getItem: () => '{"id":42}' }), emptyDraft());
});
