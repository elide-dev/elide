
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
 * @param {!Document} doc Document to attach to.
 */
function setupGTM(ctx, doc) {
  ctx['dataLayer'] = ctx['dataLayer'] || [];

  ctx['dataLayer'].push({
    'gtm.start': start,
    'event': 'gtm.js'
  });

  const scriptElement = doc.createElement('script');
  scriptElement.defer = true;
  scriptElement.type = "text/javascript";
  scriptElement.src = `https://www.googletagmanager.com/gtm.js?id=${gtmID}`;
  doc.body.appendChild(scriptElement);
}

/**
 * Boot the analytics layer on page load.
 */
function bootAnalytics() {
  setupGTM(window, document);
}

// attach on load
window.addEventListener('load', bootAnalytics);
