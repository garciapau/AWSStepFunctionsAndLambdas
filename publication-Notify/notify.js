'use strict';

console.log('Loading function');

exports.handler = (event, context, callback) => {
    console.log('Notifying bundleId =', event.bundleId);
    callback(null, event);  // Echo back the first key value
};
