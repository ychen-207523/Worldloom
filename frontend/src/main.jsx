import React, { useEffect, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { emptyDraft, readDraft, toDraft, toWorld } from './world.mjs';
import './styles.css';

async function request(path, options = {}) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 25000);
  try {
    const response = await fetch(path, { ...options, signal: controller.signal });
    const text = await response.text();
    let result;
    try { result = JSON.parse(text); }
    catch { throw new Error('The API did not return JSON. Check that the Java backend is running on port 8080.'); }
    if (!response.ok) throw new Error(result.message || `Request failed (${response.status}).`);
    return result;
  } catch (error) {
    if (error.name === 'AbortError') throw new Error('The request timed out. Check the backend and try again.');
    if (error instanceof TypeError) throw new Error('Cannot reach the API. Start the Java backend and try again.');
    throw error;
  } finally { clearTimeout(timeout); }
}

function App() {
  const [draft, setDraft] = useState(() => {
    try { return readDraft(window.localStorage); } catch { return emptyDraft(); }
  });
  const [lookup, setLookup] = useState('');
  const [busy, setBusy] = useState('');
  const [notice, setNotice] = useState(null);
  const [connected, setConnected] = useState('checking');
  const [dirty, setDirty] = useState(() => !!(draft.id || draft.name || draft.background || draft.tone || draft.rulesText || draft.detailsText !== emptyDraft().detailsText));
  const [loadedId, setLoadedId] = useState('');
  const [draftStored, setDraftStored] = useState(true);
  const noticeRef = useRef(null);

  useEffect(() => {
    if (notice) noticeRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }, [notice]);

  useEffect(() => {
    request('/api/health').then(() => setConnected('up')).catch(() => setConnected('down'));
  }, []);

  useEffect(() => {
    try { localStorage.setItem('worldloom-draft', JSON.stringify(draft)); setDraftStored(true); }
    catch { setDraftStored(false); }
  }, [draft]);

  function change(key, value) {
    setDraft(current => ({ ...current, [key]: value }));
    setDirty(true);
    setNotice(null);
  }

  function canReplace() {
    return !dirty || window.confirm('Replace the world currently in the editor? Unsaved changes will be lost.');
  }

  async function load(event) {
    event.preventDefault();
    if (!canReplace()) return;
    setBusy('loading'); setNotice(null);
    try {
      const world = await request(`/api/worlds/${encodeURIComponent(lookup.trim())}`);
      setDraft(toDraft(world)); setLoadedId(world.id); setDirty(false); setConnected('up');
      setNotice({ kind: 'success', text: `Loaded “${world.name}” from DynamoDB.` });
    } catch (error) { setNotice({ kind: 'error', text: error.message }); }
    finally { setBusy(''); }
  }

  async function save(event) {
    event.preventDefault();
    setNotice(null);
    let world;
    try { world = toWorld(draft); }
    catch (error) { setNotice({ kind: 'error', text: error.message }); return; }
    setBusy('saving');
    try {
      const saved = await request('/api/worlds', {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(world),
      });
      setLoadedId(saved.id); setLookup(saved.id); setDirty(false); setConnected('up');
      setNotice({ kind: 'success', text: `“${saved.name}” is saved in DynamoDB. Load it anytime with ID: ${saved.id}.` });
    } catch (error) { setNotice({ kind: 'error', text: error.message }); }
    finally { setBusy(''); }
  }

  function startFresh() {
    if (!canReplace()) return;
    setDraft(emptyDraft()); setLoadedId(''); setDirty(false); setNotice(null);
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#"><span className="brand-mark" aria-hidden="true">W</span>WorldLoom</a>
        <p className="sidebar-label">YOUR UNIVERSE, UNWRITTEN</p>
        <div className="active-section"><span aria-hidden="true">✧</span> World builder <span className="step">01</span></div>
        <div className="sidebar-note"><span className="note-star" aria-hidden="true">✦</span><h2>Every life starts<br />with a world.</h2><p>Give yours its own rules, ambitions, and impossible things.</p></div>
        <div className="connection"><span className={`dot ${connected}`} /><span>{connected === 'up' ? 'Local API connected' : connected === 'down' ? 'Local API unavailable' : 'Connecting to local API…'}</span></div>
      </aside>

      <main>
        <header className="topbar"><span>WORKSPACE <span className="slash">/</span> WORLD BUILDER</span><span className="local-badge">LOCAL DEVELOPMENT</span></header>
        <div className="page-content">
          <div className="page-heading"><div><p className="eyebrow">THE FIRST THREAD</p><h1>Imagine a world.<br /><em>Make it yours.</em></h1><p className="intro">A forgotten kingdom. A city above the clouds. Start with an idea.<br className="wide-break" /> Give it a history, then decide what makes it different.</p></div><button className="button secondary" disabled={!!busy} onClick={startFresh}>＋ New world</button></div>

          <section className="load-panel" aria-labelledby="load-heading">
            <div><h2 id="load-heading">Return to a world</h2><p>Load a saved definition by its world ID.</p></div>
            <form onSubmit={load}><label className="sr-only" htmlFor="lookup">Saved world ID</label><input id="lookup" value={lookup} onChange={e => setLookup(e.target.value)} placeholder="e.g. ashen-empire" required pattern="[a-z0-9][a-z0-9-]{0,63}" disabled={!!busy} /><button className="button secondary" disabled={!!busy}>{busy === 'loading' ? 'Loading…' : 'Load world →'}</button></form>
          </section>

          {notice && <div ref={noticeRef} className={`notice ${notice.kind}`} role={notice.kind === 'error' ? 'alert' : 'status'}>{notice.text}</div>}

          <form onSubmit={save} className="world-form">
            <fieldset disabled={!!busy}>
              <section className="card">
                <div className="section-heading"><span className="section-number">01</span><div><h2>The foundations</h2><p>What is this world, and what does it feel like to live here?</p></div></div>
                <div className="fields two-columns"><label>World name<input value={draft.name} onChange={e => change('name', e.target.value)} placeholder="The Ashen Empire" required /></label><label>World ID <span className="optional">UNIQUE</span><input value={draft.id} onChange={e => change('id', e.target.value)} placeholder="ashen-empire" required pattern="[a-z0-9][a-z0-9-]{0,63}" title="1–64 lowercase letters, digits, or hyphens; start with a letter or digit" /><span className="help">Use this ID to load your world later.</span></label></div>
                <label>Background<textarea className="background" value={draft.background} onChange={e => change('background', e.target.value)} placeholder="What shaped this world? What tensions, mysteries, or possibilities define its present?" required /></label>
                <label>Tone<input value={draft.tone} onChange={e => change('tone', e.target.value)} placeholder="Political intrigue, strange discoveries, and fragile hope" required /><span className="help">Describe the mood in your own words. There are no preset genres.</span></label>
              </section>

              <section className="card">
                <div className="section-heading"><span className="section-number">02</span><div><h2>The rules of existence</h2><p>What must always be true? What is possible here?</p></div></div>
                <label htmlFor="rules">World rules <span className="optional">OPTIONAL</span></label><textarea id="rules" rows="4" value={draft.rulesText} onChange={e => change('rulesText', e.target.value)} placeholder={'Memories can be traded, but each transfer loses a detail.\nNobody can prove whether the emperor is still alive.'} /><p className="help">One rule per line. These describe your setting; simulation mechanics come later.</p>
              </section>

              <section className="card details-card">
                <div className="section-heading"><span className="section-number">03</span><div><h2>Make it unlike any other</h2><p>Define factions, locations, resources, attributes, and anything beyond.</p></div><span className="json-badge">JSON</span></div>
                <p className="details-hint">Each entry in the four lists needs an <code>id</code> and <code>name</code>. Add any extra fields you need. Use <code>custom</code> for concepts unique to your setting.</p>
                <label className="sr-only" htmlFor="details">Custom world details JSON</label><textarea id="details" className="code-editor" spellCheck="false" rows="15" value={draft.detailsText} onChange={e => change('detailsText', e.target.value)} />
                <details className="example"><summary>See a faction example</summary><pre>{'{\n  "id": "keepers",\n  "name": "The Keepers",\n  "goal": "Protect the last library",\n  "relationships": { "collectors": "distrust" }\n}'}</pre><p>Add objects like this inside the <code>factions</code> array.</p></details>
              </section>
            </fieldset>
            <footer className="save-bar"><div><strong>{loadedId ? `Saved ID: ${loadedId}` : 'Your world begins here.'}</strong><p>{loadedId ? 'Change the ID to save an edited copy. Existing worlds stay intact.' : 'Save this definition to your AWS DynamoDB table.'}</p><small>{draftStored ? 'Draft kept in this browser.' : 'Browser draft storage unavailable. Keep this tab open until saved.'}</small></div><button className="button primary" disabled={!!busy}>{busy === 'saving' ? 'Saving…' : 'Save world →'}</button></footer>
          </form>
          <p className="page-footnote">WORLDLOOM <span>✦</span> A world shaped by the lives within it.</p>
        </div>
      </main>
    </div>
  );
}

createRoot(document.getElementById('root')).render(<React.StrictMode><App /></React.StrictMode>);
