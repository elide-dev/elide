import main, { handleError, handleFinished } from "./crossrunner.mjs";
main().then(handleFinished, handleError);
