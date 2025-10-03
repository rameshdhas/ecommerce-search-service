const { Client } = require('@elastic/elasticsearch');
const { BedrockRuntimeClient, InvokeModelCommand } = require('@aws-sdk/client-bedrock-runtime');

// Initialize clients
let elasticsearchClient;
let bedrockClient;

const initializeClients = () => {
    if (!elasticsearchClient) {
        const esEndpoint = process.env.ELASTICSEARCH_ENDPOINT;
        if (!esEndpoint) {
            console.log('ELASTICSEARCH_ENDPOINT not set');
            return;
        }

        try {
            const apiKey = process.env.ELASTICSEARCH_APIKEY;

            const clientConfig = {
                node: esEndpoint
            };

            if (apiKey) {
                clientConfig.auth = {
                    apiKey: apiKey
                };
            }

            elasticsearchClient = new Client(clientConfig);
            console.log('Elasticsearch client initialized successfully');
        } catch (error) {
            console.error('Failed to initialize Elasticsearch client:', error);
        }
    }

    if (!bedrockClient) {
        bedrockClient = new BedrockRuntimeClient({
            region: 'us-east-1'
        });
    }
};

const generateEmbedding = async (text) => {
    try {
        const modelId = 'amazon.titan-embed-text-v1';
        const input = {
            modelId,
            contentType: 'application/json',
            accept: 'application/json',
            body: JSON.stringify({
                inputText: text
            })
        };

        const command = new InvokeModelCommand(input);
        const response = await bedrockClient.send(command);

        const responseBody = JSON.parse(new TextDecoder().decode(response.body));
        return responseBody.embedding;
    } catch (error) {
        console.error('Error generating embedding:', error);
        throw error;
    }
};

const buildVectorSearchQuery = async (query, limit = 10, offset = 0, filters = {}) => {
    const queryVector = await generateEmbedding(query);

    const searchQuery = {
        index: process.env.ELASTICSEARCH_INDEX || 'ecommerce-products',
        size: limit,
        from: offset,
        body: {
            knn: {
                field: 'embeddings',
                query_vector: queryVector,
                k: limit + offset,
                num_candidates: Math.max(100, (limit + offset) * 2)
            },
            _source: {
                includes: ['id', 'title', 'url', 'image_url', 'description', 'metadata']
            }
        }
    };

    // Add filters if provided
    const filterQueries = buildFilterQueries(filters);
    if (filterQueries.length > 0) {
        searchQuery.body.knn.filter = {
            bool: {
                must: filterQueries
            }
        };
    }

    return searchQuery;
};

const buildTextSearchQuery = (query, limit = 10, offset = 0, filters = {}) => {
    const mustQueries = [];

    // Add text search
    if (query && query.trim()) {
        mustQueries.push({
            multi_match: {
                query: query,
                fields: ['title^2', 'description'],
                type: 'best_fields'
            }
        });
    }

    // Add filters
    const filterQueries = buildFilterQueries(filters);
    mustQueries.push(...filterQueries);

    return {
        index: process.env.ELASTICSEARCH_INDEX || 'ecommerce-products',
        size: limit,
        from: offset,
        body: {
            query: mustQueries.length > 0 ? {
                bool: {
                    must: mustQueries
                }
            } : {
                match_all: {}
            },
            _source: {
                includes: ['id', 'title', 'url', 'image_url', 'description', 'metadata']
            }
        }
    };
};

const buildFilterQueries = (filters) => {
    const filterQueries = [];

    if (filters.category) {
        filterQueries.push({
            bool: {
                should: [
                    {
                        match: {
                            'metadata.categories': filters.category
                        }
                    },
                    {
                        wildcard: {
                            'metadata.categories': {
                                value: `*${filters.category.toLowerCase()}*`,
                                case_insensitive: true
                            }
                        }
                    }
                ]
            }
        });
    }

    if (filters.brand) {
        filterQueries.push({
            bool: {
                should: [
                    {
                        match: {
                            'metadata.brand': filters.brand
                        }
                    },
                    {
                        wildcard: {
                            'metadata.brand': {
                                value: `*${filters.brand.toLowerCase()}*`,
                                case_insensitive: true
                            }
                        }
                    }
                ]
            }
        });
    }

    if (filters.priceMin !== undefined || filters.priceMax !== undefined) {
        const rangeQuery = {
            range: {
                'metadata.final_price': {}
            }
        };

        if (filters.priceMin !== undefined) {
            rangeQuery.range['metadata.final_price'].gte = filters.priceMin;
        }

        if (filters.priceMax !== undefined) {
            rangeQuery.range['metadata.final_price'].lte = filters.priceMax;
        }

        filterQueries.push(rangeQuery);
    }

    return filterQueries;
};

const parseSearchResults = (response) => {
    console.log('Elasticsearch response structure:', JSON.stringify(response, null, 2));

    // Handle different response structures
    const hits = response?.hits?.hits || response?.body?.hits?.hits;

    if (!hits || !Array.isArray(hits)) {
        console.log('No hits found in response');
        return [];
    }

    return hits.map(hit => {
        const source = hit._source;
        const metadata = source.metadata || {};

        return {
            id: source.id || source.asin,
            name: source.title || source.name,
            description: source.description,
            price: metadata.final_price || source.price || 0,
            category: metadata.categories || source.category,
            brand: metadata.brand || source.brand,
            imageUrl: source.image_url || source.imageUrl,
            score: hit._score || 0
        };
    });
};

const getTotalCount = async (query, filters = {}) => {
    try {
        if (!elasticsearchClient) {
            return 2; // Mock total for fallback
        }

        const searchQuery = buildTextSearchQuery(query, 0, 0, filters);
        searchQuery.body.track_total_hits = true;

        const response = await elasticsearchClient.search(searchQuery);

        // Handle different response structures
        const total = response?.hits?.total?.value ||
                     response?.body?.hits?.total?.value ||
                     response?.hits?.total ||
                     response?.body?.hits?.total || 0;

        return typeof total === 'object' ? total.value : total;
    } catch (error) {
        console.error('Failed to get total count:', error);
        return 0;
    }
};

const semanticSearch = async (searchRequest) => {
    const startTime = Date.now();

    try {
        const { query, limit = 10, offset = 0, filters = {} } = searchRequest;

        let searchQuery;
        let response;

        // Try vector search first, fallback to text search
        try {
            console.log('Attempting vector search for query:', query);
            searchQuery = await buildVectorSearchQuery(query, limit, offset, filters);
            response = await elasticsearchClient.search(searchQuery);
            console.log('Vector search successful');
        } catch (vectorError) {
            console.log('Vector search failed, falling back to text search:', vectorError.message);
            searchQuery = buildTextSearchQuery(query, limit, offset, filters);
            response = await elasticsearchClient.search(searchQuery);
            console.log('Text search successful');
        }

        const products = parseSearchResults(response);
        const total = await getTotalCount(query, filters);
        const processingTimeMs = Date.now() - startTime;

        return {
            products,
            total,
            limit,
            offset,
            processingTimeMs
        };

    } catch (error) {
        console.error('Search failed:', error);
        throw new Error(`Search failed: ${error.message}`);
    }
};

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

        // Initialize clients
        initializeClients();

        // Parse request body
        let requestBody;
        try {
            requestBody = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
        } catch (parseError) {
            return createCorsResponse(400, { error: 'Invalid JSON in request body' });
        }

        // Validate required fields
        if (!requestBody.query) {
            return createCorsResponse(400, { error: 'Query parameter is required' });
        }

        // Perform search
        const searchResult = await semanticSearch(requestBody);

        return createCorsResponse(200, searchResult);

    } catch (error) {
        console.error('Lambda execution error:', error);

        return createCorsResponse(500, {
            error: 'Internal server error',
            message: error.message
        });
    }
};