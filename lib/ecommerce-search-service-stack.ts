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

    // Create the Lambda function
    const semanticSearchFunction = new lambda.Function(this, 'SemanticSearchFunction', {
      runtime: lambda.Runtime.JAVA_17,
      handler: 'org.springframework.cloud.function.adapter.aws.FunctionInvoker::handleRequest',
      code: lambda.Code.fromAsset(path.join(__dirname, '../lambda/semantic-search-service'), {
        bundling: {
          image: lambda.Runtime.JAVA_17.bundlingImage,
          command: [
            '/bin/sh',
            '-c',
            'mvn clean package -DskipTests && cp target/semantic-search-service-1.0.0.jar /asset-output/',
          ],
          user: 'root',
        },
      }),
      memorySize: 1024,
      timeout: cdk.Duration.seconds(30),
      role: lambdaRole,
      environment: {
        SPRING_CLOUD_FUNCTION_DEFINITION: 'semanticSearch',
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

    // Create Lambda integration
    const lambdaIntegration = new apigateway.LambdaIntegration(semanticSearchFunction, {
      requestTemplates: { 'application/json': '{ "statusCode": "200" }' },
    });

    // Create the search resource and POST method
    const searchResource = api.root.addResource('search');
    searchResource.addMethod('POST', lambdaIntegration, {
      requestValidator: new apigateway.RequestValidator(this, 'SearchRequestValidator', {
        restApi: api,
        validateRequestBody: true,
        requestValidatorName: 'search-request-validator',
      }),
      requestModels: {
        'application/json': new apigateway.Model(this, 'SearchRequestModel', {
          restApi: api,
          contentType: 'application/json',
          modelName: 'SearchRequest',
          schema: {
            type: apigateway.JsonSchemaType.OBJECT,
            properties: {
              query: {
                type: apigateway.JsonSchemaType.STRING,
                minLength: 1,
              },
              limit: {
                type: apigateway.JsonSchemaType.INTEGER,
                minimum: 1,
                maximum: 100,
              },
              offset: {
                type: apigateway.JsonSchemaType.INTEGER,
                minimum: 0,
              },
              filters: {
                type: apigateway.JsonSchemaType.OBJECT,
                properties: {
                  category: { type: apigateway.JsonSchemaType.STRING },
                  priceMin: { type: apigateway.JsonSchemaType.NUMBER },
                  priceMax: { type: apigateway.JsonSchemaType.NUMBER },
                  brand: { type: apigateway.JsonSchemaType.STRING },
                },
              },
            },
            required: ['query'],
          },
        }),
      },
    });

    // Output the API endpoint
    new cdk.CfnOutput(this, 'ApiEndpoint', {
      value: api.url,
      description: 'API Gateway endpoint URL',
    });

    // Output the Lambda function name
    new cdk.CfnOutput(this, 'LambdaFunctionName', {
      value: semanticSearchFunction.functionName,
      description: 'Lambda function name',
    });
  }
}
