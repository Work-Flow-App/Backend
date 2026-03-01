#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { BackendStack } from '../lib/stacks/backend-stack';
import { devConfig, prodConfig, EnvironmentConfig } from '../lib/config/environment-config';

const app = new cdk.App();

// Single account — both dev and prod deploy here
const awsEnv: cdk.Environment = {
  account: process.env.CDK_DEFAULT_ACCOUNT,
  region: 'eu-central-1',
};

// Environment-specific configuration
const envConfigs: Record<string, EnvironmentConfig> = {
  dev: devConfig,
  prod: prodConfig,
};

// Deploy a single env with: cdk deploy -c env=dev
// Deploy both with: cdk deploy --all
const targetEnv = app.node.tryGetContext('env');

if (targetEnv && !['dev', 'prod'].includes(targetEnv)) {
  throw new Error(`Invalid environment: ${targetEnv}. Must be 'dev' or 'prod'.`);
}

const envsToDeploy = targetEnv ? [targetEnv] : ['dev', 'prod'];

for (const env of envsToDeploy) {
  new BackendStack(app, `WorkflowBackend-${env}`, {
    env: awsEnv,
    envConfig: envConfigs[env],
    description: `Workflow Backend Infrastructure - ${env} environment`,
    tags: {
      Environment: env,
      Project: 'workflow',
      ManagedBy: 'cdk',
    },
  });
}

app.synth();
