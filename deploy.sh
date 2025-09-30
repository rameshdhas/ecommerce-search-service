#!/bin/bash

set -e

echo "🚀 Starting deployment of Ecommerce Semantic Search Service..."

# Check if AWS CLI is configured
if ! aws sts get-caller-identity &> /dev/null; then
    echo "❌ AWS CLI not configured. Please run 'aws configure' first."
    exit 1
fi

# Install Node.js dependencies
echo "📦 Installing Node.js dependencies..."
npm install

# Build TypeScript
echo "🔨 Building TypeScript..."
npm run build

# Build Java Lambda function
echo "☕ Building Java Lambda function..."
cd lambda/semantic-search-service
mvn clean package -DskipTests
cd ../..

# Deploy with CDK
echo "🌩️ Deploying to AWS..."
cdk deploy --require-approval never

echo "✅ Deployment completed successfully!"
echo ""
echo "📋 Next steps:"
echo "1. Configure OpenSearch collection access policies"
echo "2. Create the products index with vector field mapping"
echo "3. Index your product data with embeddings"
echo "4. Test the API endpoint"