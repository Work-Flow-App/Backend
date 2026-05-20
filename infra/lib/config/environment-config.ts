import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';

export interface EnvironmentConfig {
  envName: 'dev' | 'prod';

  // Networking
  maxAzs: number;
  natGateways: number;

  // EC2
  instanceType: ec2.InstanceType;
  ebsVolumeSize: number;

  // Domain
  domainName: string;
  hostedZoneName: string;

  // RDS
  dbInstanceClass: ec2.InstanceType;
  dbAllocatedStorage: number;
  dbName: string;
  dbMultiAz: boolean;
  dbDeletionProtection: boolean;

  // Existing resources (looked up, not created)
  ecrRepositoryName: string;

  // Removal policy
  removalPolicy: cdk.RemovalPolicy;

  // CloudWatch
  logRetentionDays: number;

  // Backup
  backupRetentionDays: number;
  backupEnabled: boolean;
}

export const devConfig: EnvironmentConfig = {
  envName: 'dev',

  maxAzs: 2,
  natGateways: 0,

  instanceType: ec2.InstanceType.of(ec2.InstanceClass.T4G, ec2.InstanceSize.SMALL),
  ebsVolumeSize: 20,

  domainName: 'api.dev2.workfloow.app',
  hostedZoneName: 'workfloow.app',

  dbInstanceClass: ec2.InstanceType.of(ec2.InstanceClass.T4G, ec2.InstanceSize.MICRO),
  dbAllocatedStorage: 20,
  dbName: 'workflow_dev',
  dbMultiAz: false,
  dbDeletionProtection: false,

  ecrRepositoryName: 'work-flow-dev',

  removalPolicy: cdk.RemovalPolicy.DESTROY,

  logRetentionDays: 7,

  backupRetentionDays: 1,
  backupEnabled: false,
};

export const prodConfig: EnvironmentConfig = {
  envName: 'prod',

  maxAzs: 2,
  natGateways: 0,

  instanceType: ec2.InstanceType.of(ec2.InstanceClass.T4G, ec2.InstanceSize.SMALL),
  ebsVolumeSize: 30,

  domainName: 'api.workfloow.app',
  hostedZoneName: 'workfloow.app',

  dbInstanceClass: ec2.InstanceType.of(ec2.InstanceClass.T4G, ec2.InstanceSize.MICRO),
  dbAllocatedStorage: 20,
  dbName: 'workflow_prod',
  dbMultiAz: false,
  dbDeletionProtection: true,

  ecrRepositoryName: 'work-flow-prod',

  removalPolicy: cdk.RemovalPolicy.RETAIN,

  logRetentionDays: 30,

  backupRetentionDays: 14,
  backupEnabled: true,
};
