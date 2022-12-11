/**
 * Invoke the user's `renderContent` entrypoint function after setting up runtime state.
 */
function embeddedExecute() {
  // noinspection JSUnresolvedFunction,JSUnresolvedVariable
  const renderContent = embedded.renderContent;
  // noinspection JSUnresolvedFunction,JSUnresolvedVariable
  const renderStream = embedded.renderStream;
  return {
    renderContent,
    renderStream,
  };
}
embeddedExecute();
