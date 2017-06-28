'use strict';

console.log('Loading function');

exports.handler = (event, context, callback) => {
  var wfDefString = '{"status": "ACTIVE","exceptions": false,"scholarlyItems": 3,"publicationArtifacts": 6}';
  var jsonContent = JSON.parse(wfDefString);
  if (event.bundleId == "bundle000002") jsonContent.status = "INACTIVE";
  if (event.bundleId == "bundle000003") jsonContent.exceptions = true;
  if (event.bundleId == "bundle000004") jsonContent.scholarlyItems = 0;
  if (event.bundleId == "bundle000005") jsonContent.publicationArtifacts = 0;
  jsonContent.bundleId = event.bundleId;

  var AWS = require('aws-sdk');
    var sqs = new AWS.SQS();
    sqs.sendMessage({
        MessageBody: '<Journal journalId="0000001"><article title="11 Neurological surgery"><category>Cells</category><author>Orson Scott Card</author><city>London</city></article><article title="12 Emerging Sources"><category>Pharmaceutics</category><author>H.G. Wells</author><city>London</city></article><article title="13 Physical Biology"><category>Biomolecules</category><author>Ray Bradbury</author><city>London</city></article></Journal>',
        QueueUrl: 'https://sqs.us-west-2.amazonaws.com/509786517216/PipsResponse'
    }, function(err, data) {
        if (err) {
            console.log(err.stack);
            return;
        }
        console.log('push sent');
        console.log(data);
        context.done(null, 'Function Finished!');
});

  callback(null, jsonContent);

};
