/*! (c) Andrea Giammarchi - ISC */
let EventTargetDef;

(function(ctx) {
    var self = {};

    try {
        self.EventTarget = (new EventTarget).constructor;
    } catch(EventTarget) {
        (function (Object, wm) {
            var create = Object.create;
            var defineProperty = Object.defineProperty;
            var proto = EventTarget.prototype;
            define(proto, 'addEventListener', function (type, listener, options) {
                for (var
                         secret = wm.get(this),
                         listeners = secret[type] || (secret[type] = []),
                         i = 0, length = listeners.length; i < length; i++
                ) {
                    if (listeners[i].listener === listener)
                        return;
                }
                listeners.push({target: this, listener: listener, options: options});
            });
            define(proto, 'dispatchEvent', function (event) {
                var secret = wm.get(this);
                var listeners = secret[event.type];
                if (listeners) {
                    define(event, 'target', this);
                    define(event, 'currentTarget', this);
                    listeners.slice(0).some(dispatch, event);
                    delete event.currentTarget;
                    delete event.target;
                }
                return true;
            });
            define(proto, 'removeEventListener', function (type, listener) {
                for (var
                         secret = wm.get(this),
                         /* istanbul ignore next */
                         listeners = secret[type] || (secret[type] = []),
                         i = 0, length = listeners.length; i < length; i++
                ) {
                    if (listeners[i].listener === listener) {
                        listeners.splice(i, 1);
                        return;
                    }
                }
            });
            self.EventTarget = EventTarget;
            function EventTarget() {'use strict';
                wm.set(this, create(null));
            }
            function define(target, name, value) {
                defineProperty(
                    target,
                    name,
                    {
                        configurable: true,
                        writable: true,
                        value: value
                    }
                );
            }
            function dispatch(info) {
                var options = info.options;
                if (options && options.once)
                    info.target.removeEventListener(this.type, info.listener);
                if (typeof info.listener === 'function')
                    info.listener.call(info.target, this);
                else
                    info.listener.handleEvent(this);
                return this._stopImmediatePropagationFlag;
            }
        }(Object, new WeakMap));
    }
    EventTargetDef = self.EventTarget;
    ctx['EventTarget'] = self.EventTarget;

})(globalThis);

export const EventTarget = EventTargetDef;

export default EventTarget;
