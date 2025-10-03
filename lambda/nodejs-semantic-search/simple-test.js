const createCorsResponse = (statusCode, body) => {
    return {
        statusCode,
        headers: {
            'Content-Type': 'application/json',
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Headers': 'Content-Type,X-Amz-Date,Authorization,X-Api-Key',
            'Access-Control-Allow-Methods': 'POST,OPTIONS'
        },
        body: JSON.stringify(body)
    };
};

exports.handler = async (event) => {
    console.log('Event:', JSON.stringify(event, null, 2));

    try {
        // Handle OPTIONS preflight request
        if (event.httpMethod === 'OPTIONS') {
            return createCorsResponse(200, {});
        }

        // Parse request body
        let requestBody;
        try {
            requestBody = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
        } catch (parseError) {
            return createCorsResponse(400, { error: 'Invalid JSON in request body' });
        }

        // Return mock response with CORS headers
        const mockResponse = {
            products: [
                {
                    id: "mock-001",
                    name: "Sample Node.js Product",
                    description: "This is a sample product returned by the Node.js Lambda function with CORS headers",
                    price: 29.99,
                    category: "Test Category",
                    brand: "NodeJS Brand",
                    imageUrl: "https://example.com/sample.jpg",
                    score: 1.0
                }
            ],
            total: 1,
            limit: requestBody.limit || 10,
            offset: requestBody.offset || 0,
            processingTimeMs: 50,
            source: "Node.js Lambda with CORS"
        };

        return createCorsResponse(200, mockResponse);

    } catch (error) {
        console.error('Lambda execution error:', error);

        return createCorsResponse(500, {
            error: 'Internal server error',
            message: error.message
        });
    }
};