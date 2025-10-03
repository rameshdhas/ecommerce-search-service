import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as apigateway from 'aws-cdk-lib/aws-apigateway';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as path from 'path';

export class EcommerceSearchServiceStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // Create IAM role for Lambda function
    const lambdaRole = new iam.Role(this, 'SemanticSearchLambdaRole', {
      assumedBy: new iam.ServicePrincipal('lambda.amazonaws.com'),
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaBasicExecutionRole'),
      ],
      inlinePolicies: {
        BedrockAccess: new iam.PolicyDocument({
          statements: [
            new iam.PolicyStatement({
              effect: iam.Effect.ALLOW,
              actions: [
                'bedrock:InvokeModel',
              ],
              resources: [
                'arn:aws:bedrock:us-east-1::foundation-model/amazon.titan-embed-text-v1'
              ],
            }),
          ],
        }),
      },
    });

    // Create the Java Lambda function
    const semanticSearchFunction = new lambda.Function(this, 'SemanticSearchFunction', {
      runtime: lambda.Runtime.JAVA_17,
      handler: 'org.springframework.cloud.function.adapter.aws.FunctionInvoker::handleRequest',
      code: lambda.Code.fromAsset(path.join(__dirname, '../lambda/semantic-search-service/target/semantic-search-service-1.0.0.jar')),
      memorySize: 1024,
      timeout: cdk.Duration.seconds(30),
      role: lambdaRole,
      environment: {
        SPRING_CLOUD_FUNCTION_DEFINITION: 'semanticSearch',
      },
    });

    // Create the Node.js Lambda function
    const nodeSemanticSearchFunction = new lambda.Function(this, 'NodeSemanticSearchFunction', {
      runtime: lambda.Runtime.NODEJS_18_X,
      handler: 'index.handler',
      code: lambda.Code.fromAsset(path.join(__dirname, '../lambda/nodejs-semantic-search')),
      memorySize: 1024,
      timeout: cdk.Duration.seconds(30),
      role: lambdaRole,
      environment: {
        ELASTICSEARCH_ENDPOINT: 'https://ecommerce-project-a814cd.es.us-east-1.aws.elastic.cloud:443',
        ELASTICSEARCH_APIKEY: 'alBSNG41a0JUamwwcHZqNnoxaUs6Q2I0Ym5iNE14TkRvNEtDUXcyZF83Zw==',
        ELASTICSEARCH_INDEX: 'ecommerce-products',
        NODE_ENV: 'production',
      },
    });

    // Create API Gateway
    const api = new apigateway.RestApi(this, 'SemanticSearchApi', {
      restApiName: 'Ecommerce Semantic Search API',
      description: 'API for semantic search of ecommerce products',
      defaultCorsPreflightOptions: {
        allowOrigins: apigateway.Cors.ALL_ORIGINS,
        allowMethods: ['POST', 'OPTIONS'],
        allowHeaders: ['Content-Type', 'X-Amz-Date', 'Authorization', 'X-Api-Key'],
      },
    });

    // Create Lambda integrations
    const lambdaIntegration = new apigateway.LambdaIntegration(semanticSearchFunction, {
      proxy: true,
    });

    const nodeLambdaIntegration = new apigateway.LambdaIntegration(nodeSemanticSearchFunction, {
      proxy: true,
    });

    // Create the search resource and POST method (Java Lambda)
    const searchResource = api.root.addResource('search');
    searchResource.addMethod('POST', lambdaIntegration);

    // Create the node-search resource and POST method (Node.js Lambda)
    const nodeSearchResource = api.root.addResource('node-search');
    nodeSearchResource.addMethod('POST', nodeLambdaIntegration);

    // Output the API endpoint
    new cdk.CfnOutput(this, 'ApiEndpoint', {
      value: api.url,
      description: 'API Gateway endpoint URL',
    });

    // Output the Lambda function names
    new cdk.CfnOutput(this, 'LambdaFunctionName', {
      value: semanticSearchFunction.functionName,
      description: 'Java Lambda function name',
    });

    new cdk.CfnOutput(this, 'NodeLambdaFunctionName', {
      value: nodeSemanticSearchFunction.functionName,
      description: 'Node.js Lambda function name',
    });

    new cdk.CfnOutput(this, 'NodeSearchEndpoint', {
      value: `${api.url}node-search`,
      description: 'Node.js Lambda API endpoint',
    });
  }
}
