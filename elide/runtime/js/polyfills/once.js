/*! (c) Andrea Giammarchi - ISC */

const {apply, construct} = Reflect;

Function.prototype.once || Object.defineProperty(
    Function.prototype,
    'once',
    {
        writable: true,
        configurable: true,
        value() {
            let fn = this, execute = true, returned;
            return function once() {
                if (execute) {
                    execute = false;
                    returned = this instanceof once ?
                        construct(fn, arguments) :
                        apply(fn, this, arguments);
                }
                return returned;
            };
        }
    }
);
