# Ecommerce Semantic Search Service

This project contains an AWS CDK application that deploys a semantic search service for ecommerce products using Java Spring Boot Lambda functions and OpenSearch Serverless.

## Architecture

- **API Gateway**: REST API with POST `/search` endpoint
- **Lambda Function**: Java 17 Spring Boot function for semantic search
- **OpenSearch Serverless**: Vector database for storing and searching product embeddings
- **IAM Roles**: Proper permissions for Lambda to access OpenSearch

## Prerequisites

1. AWS CLI configured with appropriate permissions
2. Node.js (18.x or later)
3. Maven (for Java Lambda function)
4. CDK CLI installed globally: `npm install -g aws-cdk`

## Deployment Steps

1. **Install dependencies**:
   ```bash
   npm install
   ```

2. **Bootstrap CDK** (if first time using CDK in this region):
   ```bash
   cdk bootstrap
   ```

3. **Build and deploy**:
   ```bash
   npm run build
   cdk deploy
   ```

4. **Note the outputs**: After deployment, note the API Gateway endpoint URL and OpenSearch endpoint.

## API Usage

### POST /search

Request body:
```json
{
  "query": "wireless headphones",
  "limit": 10,
  "offset": 0,
  "filters": {
    "category": "Electronics",
    "priceMin": 50.0,
    "priceMax": 200.0,
    "brand": "Sony"
  }
}
```

Response:
```json
{
  "products": [
    {
      "id": "123",
      "name": "Sony Wireless Headphones",
      "description": "High quality wireless headphones...",
      "price": 149.99,
      "category": "Electronics",
      "brand": "Sony",
      "imageUrl": "https://example.com/image.jpg",
      "score": 0.95
    }
  ],
  "total": 1,
  "limit": 10,
  "offset": 0,
  "processingTimeMs": 150
}
```

## Setting up the Vector Database

After deployment, you'll need to:

1. **Create the products index** in OpenSearch with proper vector field mapping
2. **Index your product data** with embeddings
3. **Configure access policies** for the OpenSearch collection

Example index mapping:
```json
{
  "mappings": {
    "properties": {
      "id": { "type": "keyword" },
      "name": { "type": "text" },
      "description": { "type": "text" },
      "description_vector": {
        "type": "knn_vector",
        "dimension": 768,
        "method": {
          "name": "hnsw",
          "space_type": "cosinesimil"
        }
      },
      "price": { "type": "double" },
      "category": {
        "type": "text",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "brand": {
        "type": "text",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "image_url": { "type": "keyword" }
    }
  }
}
```

## Development

### Local Testing

The Lambda function can be tested locally using Spring Boot:

```bash
cd lambda/semantic-search-service
mvn spring-boot:run
```

### Building the Lambda

```bash
cd lambda/semantic-search-service
mvn clean package
```

## Environment Variables

The Lambda function uses these environment variables:

- `VECTOR_DB_ENDPOINT`: OpenSearch collection endpoint (set automatically by CDK)
- `VECTOR_DB_INDEX`: Index name (defaults to "products")
- `SPRING_CLOUD_FUNCTION_DEFINITION`: Function name (set to "semanticSearch")

## Clean Up

To remove all resources:

```bash
cdk destroy
```

## Notes

- The vector generation in `VectorDatabaseService.java` is currently using a mock implementation. Replace with actual embedding model calls.
- OpenSearch Serverless collection requires proper access policies to be configured separately.
- Consider implementing caching for frequently searched queries.
- Monitor Lambda cold starts and adjust memory/timeout as needed.