'use strict';

console.log('Loading function');

exports.handler = (event, context, callback) => {
    console.log('publicationArtifacts =', event.publicationArtifacts);
    event.service="SIMS";
    callback(null, event);  // Echo back the first key value
};
