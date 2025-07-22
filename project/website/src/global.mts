// @ts-ignore
import hljs from 'https://unpkg.com/@highlightjs/cdn-assets@11.11.1/es/highlight.min.js';

async function loadHighlightsFor(lang: string): Promise<any> {
  const mod = (await import(`https://unpkg.com/@highlightjs/cdn-assets@11.11.1/es/languages/${lang}.min.js`)).default;
  hljs.registerLanguage(lang, mod);
  return mod;
}

function loadDeferred() {
  setTimeout(async () => {
    await Promise.all(allLangs.map(loadHighlightsFor))
  }, 0)
}

// deferred loads
const allLangs = [
  'javascript',
  'python',
  'bash',
  'typescript',
  'kotlin',
  'java',
  'markdown',
  'css',
  'scss'
]

document.addEventListener("DOMContentLoaded", () => {
  console.log("initializing elide page");
  loadDeferred();

  document.querySelectorAll('pre code').forEach((el) => {
    hljs.highlightElement(el);
  });
})
