const {
  Readable,
  Writable,
  Transform,
  Duplex,
  pipeline,
  finished,
} = require("readable-stream");

module.exports.Readable = Readable;
module.exports.Writable = Writable;
module.exports.Transform = Transform;
module.exports.Duplex = Duplex;
module.exports.pipeline = pipeline;
module.exports.finished = finished;
