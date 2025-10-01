#!/usr/bin/env node
import * as cdk from 'aws-cdk-lib';
import { EcommerceSearchServiceStack } from '../lib/ecommerce-search-service-stack';

const app = new cdk.App();

// Deploy the search service stack
new EcommerceSearchServiceStack(app, 'EcommerceSearchServiceStack', {
   env: { account: process.env.CDK_DEFAULT_ACCOUNT, region: process.env.CDK_DEFAULT_REGION },
});