'use strict';

console.log('Loading function');

exports.handler = (event, context, callback) => {
    console.log('Received event:', JSON.stringify(event, null, 2));
    console.log('scholarlyItems =', event.scholarlyItems);
    var wfDefString = '{"status": "ACTIVE","exceptions": false,"scholarlyItems": 3,"publicationArtifacts": 6}';
    var jsonContent = JSON.parse(wfDefString);



  var AWS = require('aws-sdk');
    var sqs = new AWS.SQS();
    sqs.sendMessage({
        MessageBody: JSON.stringify(event.Records[0].Sns.Message),
        QueueUrl: 'https://sqs.us-west-2.amazonaws.com/509786517216/ContentServiceResponse'
    }, function(err, data) {
        if (err) {
            console.log(err.stack);
            return;
        }
        console.log('push sent');
        console.log(data);
        context.done(null, 'Function Finished!');
});

    callback(null, jsonContent);  // Echo back the first key value
};
