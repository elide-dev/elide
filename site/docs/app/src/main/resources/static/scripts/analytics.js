
/**
 * Sanitizer function.
 *
 * @typedef {function(this:TrustedTypePolicy, string, ...*): TrustedHTML|function(this:*, string): string}
 */
let TrustedTypesSanitizer;

/**
 * GTM container ID.
 *
 * @const
 */
const gtmID = 'GTM-5Q5G8XW';

/**
 * Start time of the script.
 *
 * @const
 */
const start = (new Date).getTime();

/**
 * Setup Google Tag Manager.
 *
 * @param {?} ctx Context to attach data to.
 * @param {TrustedTypesSanitizer} sanitizer HTML trusted-types sanitizer.
 * @param {!Document} doc Document to attach to.
 */
function setupGTM(ctx, sanitizer, doc) {
  ctx['dataLayer'] = ctx['dataLayer'] || [];

  ctx['dataLayer'].push({
    'gtm.start': start,
    event: 'gtm.js'
  });

  const frag = doc.createDocumentFragment();
  const scriptElement = doc.createElement('script');
  scriptElement.async = true;
  scriptElement.defer = true;
  scriptElement.type = "text/javascript";
  scriptElement.src = `https://www.googletagmanager.com/gtm.js?id=${gtmID}`;
  frag.innertHTML = sanitizer(scriptElement.outerHTML);
  doc.body.appendChild(frag);
}

/**
 * Boot the analytics layer on page load.
 */
function bootAnalytics() {
  /** @type {TrustedTypesSanitizer} */
  let sanitizer = (input) => input;

  // trusted types for GTM/GA
  if (typeof window['trustedTypes'] !== 'undefined') {
    const trustedPolicy = trustedTypes.createPolicy('glib', {
      createHTML: (input) => input  // trusted
    });
    sanitizer = trustedPolicy.createHTML;
  }
  setupGTM(window, sanitizer, document);
}

// attach on load
document.addEventListener('load', bootAnalytics);
