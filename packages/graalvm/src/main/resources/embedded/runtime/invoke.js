/**
 * Invoke the user's `renderContent` entrypoint function after setting up runtime state.
 */
function embeddedExecute() {
  // noinspection JSUnresolvedFunction,JSUnresolvedVariable
  return embedded.renderContent;
}
embeddedExecute();
