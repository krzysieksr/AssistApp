'use strict';
var https = require('https');

// Close dialog with the customer, reporting fulfillmentState of Failed or Fulfilled ("Thanks")
function close(sessionAttributes, fulfillmentState, message) {
    return {
        sessionAttributes,
        dialogAction: {
            type: 'Close',
            fulfillmentState,
            message,
        },
    };
}
 
function buildResponse(intentName, attribute, userData, callback, sessionAttributes) {
    let value = userData.attributeValue;
    let response = "Unknown context";
    switch(intentName.toLowerCase()) {
        case "what":
            response = `Okay, my ${attribute} is ${value} `;
            break;
        case "travel":
            response = `${value} is ${attribute} of my travel.`;
            break;
        case "flight": 
            response = `The Flight ${attribute} is ${value}`;
            break;
    }
    
    callback(close(sessionAttributes, 'Fulfilled',
    {
        'contentType': 'PlainText', 
        'content': response}));

}
// --------------- Events -----------------------
 
function dispatch(intentRequest, callback) {
    console.log("input: " + JSON.stringify(intentRequest));
    console.log(`request received for userId=${intentRequest.userId}, intentName=${intentRequest.currentIntent.name}`);
    const sessionAttributes = intentRequest.sessionAttributes;
    const slots = intentRequest.currentIntent.slots;
    const attribute = slots[Object.keys(slots)[0]];
    let intentName = intentRequest.currentIntent.name;
    if(intentName.toLowerCase() == 'name') {
        intentName = 'what';
    }
    var url = `https://c85d0367.ngrok.io/api/lex/v1/intents/${intentName}?attribute=${attribute}`;
    //var url = `https://c85d0367.ngrok.io/api/lex/v1/intents/${intentName}?attribute=${attribute}`;
    https.get(url, function(res) {
          res.on("data", function(chunk) {
            console.log("BODY: " + chunk);
            let body = JSON.parse(chunk);
            console.log("Got response body: " + body);
            buildResponse(intentName,attribute,body,callback, sessionAttributes);
          });
    }).on('error', function(e) {
        console.log("Got error: " + e.message);
        callback(close(sessionAttributes, 'Fulfilled',
        {
            'contentType': 'PlainText', 
            'content': `Uh oh, I have problem accessing data.`}));
    });

    // if(attribute == 'name') {
    //         value = "jarek";
    // }
}
 
// --------------- Main handler -----------------------
 
// Route the incoming request based on intent.
// The JSON body of the request is provided in the event slot.
exports.handler = (event, context, callback) => {
    try {
        dispatch(event,
            (response) => {
                callback(null, response);
            });
    } catch (err) {
        callback(err);
    }
};