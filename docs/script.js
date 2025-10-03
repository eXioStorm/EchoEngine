const toggleButton = document.getElementById('toggle-btn')
const sidebar = document.getElementById('sidebar')

function show(shown, hidden) {
  document.getElementById(shown).style.display='block';
  document.getElementById(hidden).style.display='none';
  return false;
}

function toggleSidebar(){
  sidebar.classList.toggle('close')
  toggleButton.classList.toggle('rotate')

  closeAllSubMenus()
}

function toggleSubMenu(button){

  if(!button.nextElementSibling.classList.contains('show')){
    closeAllSubMenus()
  }

  button.nextElementSibling.classList.toggle('show')
  button.classList.toggle('rotate')

  if(sidebar.classList.contains('close')){
    sidebar.classList.toggle('close')
    toggleButton.classList.toggle('rotate')
  }
}

function closeAllSubMenus(){
  Array.from(sidebar.getElementsByClassName('show')).forEach(ul => {
    ul.classList.remove('show')
    ul.previousElementSibling.classList.remove('rotate')
  })
}

/*
  Dynamic README loader:
  - Put a container with id="readme-container"
  - Provide either data-url (template) or data-baseurl
  - If you want page-flipping enable data-index (numeric). Omit or leave empty to disable flipping.
*/

function buildReadmeUrl(container) {
  // dataset props:
  // data-url   -> full template or full URL (optional)
  // data-baseurl -> base (optional)
  // data-index -> optional index string (empty or missing = no index / no flipping)
  const template = container.dataset.url;
  const base = container.dataset.baseurl;
  const rawIndex = container.dataset.index;
  const index = (rawIndex === undefined || rawIndex === null || rawIndex === '') ? null : String(rawIndex);

  // Helper - ensure we don't accidentally create double ".md"
  const ensureMd = (u) => u.endsWith('.md') ? u : (u + '.md');

  if (template) {
    let url = template;

    // placeholder replacement if present
    if (/\{i\}|\{index\}/.test(url)) {
      const replacement = index === null ? '' : index;
      url = url.replace(/\{i\}|\{index\}/g, replacement);
      // if placeholder removed and there's no .md, ensure .md present
      if (!url.endsWith('.md')) url = ensureMd(url);
      return url;
    }

    // no placeholder: if index present, insert index before .md or append index + .md
    if (index !== null) {
      if (url.endsWith('.md')) {
        return url.replace(/\.md$/, index + '.md');
      } else {
        return url + index + '.md';
      }
    } else {
      // no index → ensure .md
      return ensureMd(url);
    }
  }

  // fallback to base (if provided)
  if (base) {
    if (index !== null) {
      if (base.endsWith('.md')) {
        return base.replace(/\.md$/, index + '.md');
      } else {
        return base + index + '.md';
      }
    } else {
      return ensureMd(base);
    }
  }

  // TODO change url to failed to fetch url? / file
  const fallback = 'https://raw.githubusercontent.com/eXioStorm/EchoEngine/refs/heads/main/README.md';
  return fallback;
}

async function loadReadme() {
  const container = document.getElementById("readme-container");
  if (!container) return; // nothing to do

  const url = buildReadmeUrl(container);
  // small UI kickoff
  container.textContent = "Loading README…";

  try {
    const resp = await fetch(url);
    if (!resp.ok) throw new Error(`HTTP ${resp.status} ${resp.statusText}`);
    const md = await resp.text();
    // marked must be available (load it before this script)
    container.innerHTML = (typeof marked !== 'undefined') ? marked.parse(md) : md;
  } catch (err) {
    container.textContent = "Failed to load README: " + err.message;
    console.error("Failed to fetch README URL:", url, err);
  }
}

/* Page-flipping controls
   - changePage(delta): increments the numeric data-index by delta (only if data-index present and numeric/parsable)
   - setPage(n): sets numeric page directly
*/
function changePage(delta) {
  const container = document.getElementById("readme-container");
  if (!container) return;

  const rawIndex = container.dataset.index;
  if (rawIndex === undefined || rawIndex === null || rawIndex === '') {
    console.warn('Page flipping disabled: no data-index set on #readme-container.');
    return;
  }

  let idx = parseInt(rawIndex, 10);
  if (isNaN(idx)) idx = 0;
  idx += delta;
  container.dataset.index = String(idx);
  loadReadme();
}

function setPage(n) {
  const container = document.getElementById("readme-container");
  if (!container) return;

  // if data-index is not present, treat this as enabling flipping
  container.dataset.index = String(Number(n) || 0);
  loadReadme();
}

/* Kick things off after DOM ready.
   Note: script must be included *after* marked.js (or ensure marked is loaded beforehand).
*/
document.addEventListener("DOMContentLoaded", () => {
  loadReadme();
});