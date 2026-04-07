import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as route53 from 'aws-cdk-lib/aws-route53';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';
import * as ssm from 'aws-cdk-lib/aws-ssm';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as rds from 'aws-cdk-lib/aws-rds';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as backup from 'aws-cdk-lib/aws-backup';
import * as events from 'aws-cdk-lib/aws-events';
import { Construct } from 'constructs';
import { EnvironmentConfig } from '../config/environment-config';

export interface BackendStackProps extends cdk.StackProps {
  envConfig: EnvironmentConfig;
}

export class BackendStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props: BackendStackProps) {
    super(scope, id, props);

    const config = props.envConfig;
    const prefix = `workflow-${config.envName}`;

    // ──────────────────────────────────────────────
    // VPC — 1 public subnet, no NAT (cost saving)
    // ──────────────────────────────────────────────
    const vpc = new ec2.Vpc(this, 'Vpc', {
      vpcName: `${prefix}-vpc`,
      maxAzs: config.maxAzs,
      natGateways: config.natGateways,
      subnetConfiguration: [
        {
          name: 'Public',
          subnetType: ec2.SubnetType.PUBLIC,
          cidrMask: 24,
        },
      ],
    });

    // ──────────────────────────────────────────────
    // Security Group
    // ──────────────────────────────────────────────
    const sg = new ec2.SecurityGroup(this, 'InstanceSg', {
      vpc,
      securityGroupName: `${prefix}-ec2-sg`,
      description: `Security group for ${config.envName} EC2 instance`,
      allowAllOutbound: true,
    });

    sg.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(22), 'SSH');
    sg.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(80), 'HTTP');
    sg.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(443), 'HTTPS');

    // ──────────────────────────────────────────────
    // IAM Role for EC2
    // ──────────────────────────────────────────────
    const role = new iam.Role(this, 'InstanceRole', {
      roleName: `${prefix}-ec2-role`,
      assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com'),
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonSSMManagedInstanceCore'),
      ],
    });

    // ECR pull
    role.addToPolicy(new iam.PolicyStatement({
      actions: [
        'ecr:GetAuthorizationToken',
        'ecr:BatchCheckLayerAvailability',
        'ecr:GetDownloadUrlForLayer',
        'ecr:BatchGetImage',
      ],
      resources: ['*'],
    }));

    // CloudWatch Logs
    role.addToPolicy(new iam.PolicyStatement({
      actions: [
        'logs:CreateLogStream',
        'logs:PutLogEvents',
        'logs:DescribeLogStreams',
      ],
      resources: [`arn:aws:logs:${this.region}:${this.account}:log-group:/workflow/${config.envName}/*`],
    }));

    // Secrets Manager — scoped to /workflow/<env>/
    role.addToPolicy(new iam.PolicyStatement({
      actions: [
        'secretsmanager:GetSecretValue',
        'secretsmanager:DescribeSecret',
      ],
      resources: [`arn:aws:secretsmanager:${this.region}:${this.account}:secret:/workflow/${config.envName}/*`],
    }));

    // SSM Parameter Store — scoped to /workflow/<env>/
    role.addToPolicy(new iam.PolicyStatement({
      actions: [
        'ssm:GetParameter',
        'ssm:GetParameters',
        'ssm:GetParametersByPath',
      ],
      resources: [`arn:aws:ssm:${this.region}:${this.account}:parameter/workflow/${config.envName}/*`],
    }));

    // ──────────────────────────────────────────────
    // S3 Bucket for uploads
    // ──────────────────────────────────────────────
    const uploadsBucket = new s3.Bucket(this, 'UploadsBucket', {
      bucketName: `${prefix}-uploads`,
      blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
      removalPolicy: config.removalPolicy,
      autoDeleteObjects: config.removalPolicy === cdk.RemovalPolicy.DESTROY,
      encryption: s3.BucketEncryption.S3_MANAGED,
    });

    uploadsBucket.grantReadWrite(role);

    // ──────────────────────────────────────────────
    // RDS Security Group
    // ──────────────────────────────────────────────
    const dbSg = new ec2.SecurityGroup(this, 'DbSg', {
      vpc,
      securityGroupName: `${prefix}-db-sg`,
      description: `Security group for ${config.envName} RDS instance`,
      allowAllOutbound: false,
    });

    dbSg.addIngressRule(sg, ec2.Port.tcp(3306), 'MySQL from EC2');

    // ──────────────────────────────────────────────
    // RDS MySQL — db.t4g.micro, single AZ, public (no NAT)
    // ──────────────────────────────────────────────
    const dbCredentials = rds.Credentials.fromGeneratedSecret('workflow_admin', {
      secretName: `/workflow/${config.envName}/database`,
    });

    const dbInstance = new rds.DatabaseInstance(this, 'Database', {
      instanceIdentifier: `${prefix}-db`,
      engine: rds.DatabaseInstanceEngine.mysql({
        version: rds.MysqlEngineVersion.VER_8_0,
      }),
      instanceType: config.dbInstanceClass,
      credentials: dbCredentials,
      databaseName: config.dbName,
      vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
      securityGroups: [dbSg],
      publiclyAccessible: true,
      multiAz: config.dbMultiAz,
      allocatedStorage: config.dbAllocatedStorage,
      storageType: rds.StorageType.GP3,
      storageEncrypted: true,
      deletionProtection: config.dbDeletionProtection,
      removalPolicy: config.removalPolicy,
      backupRetention: cdk.Duration.days(config.backupRetentionDays),
    });

    // ──────────────────────────────────────────────
    // Key Pair — CDK creates it, private key stored in SSM
    // ──────────────────────────────────────────────
    const keyPair = new ec2.KeyPair(this, 'KeyPair', {
      keyPairName: `${prefix}-key`,
      type: ec2.KeyPairType.ED25519,
    });

    // ──────────────────────────────────────────────
    // EC2 Instance — Ubuntu 22.04 ARM64
    // ──────────────────────────────────────────────
    const userData = ec2.UserData.forLinux();
    userData.addCommands(
      'set -euxo pipefail',
      '',
      '# System updates',
      'apt-get update -y',
      'apt-get upgrade -y',
      '',
      '# Install Docker',
      'apt-get install -y docker.io unzip curl jq',
      'systemctl start docker',
      'systemctl enable docker',
      'usermod -aG docker ubuntu',
      '',
      '# Install Docker Compose',
      'curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose',
      'chmod +x /usr/local/bin/docker-compose',
      '',
      '# Install AWS CLI v2 (ARM64)',
      'curl "https://awscli.amazonaws.com/awscli-exe-linux-aarch64.zip" -o "awscliv2.zip"',
      'unzip -q awscliv2.zip',
      './aws/install',
      'rm -rf aws awscliv2.zip',
      '',
      '# Create app directory',
      'mkdir -p /home/ubuntu/app',
      'chown ubuntu:ubuntu /home/ubuntu/app',
      '',
      '# ── setup-env.sh is managed in the repo (scripts/setup-env.sh) ──',
      '# The CI/CD deploy workflow pulls and executes it on every deployment.',
    );

    const instance = new ec2.Instance(this, 'Instance', {
      instanceName: `${prefix}-server`,
      vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
      instanceType: config.instanceType,
      machineImage: ec2.MachineImage.fromSsmParameter(
        '/aws/service/canonical/ubuntu/server/22.04/stable/current/arm64/hvm/ebs-gp2/ami-id',
        { os: ec2.OperatingSystemType.LINUX },
      ),
      securityGroup: sg,
      role,
      userData,
      userDataCausesReplacement: false,
      blockDevices: [
        {
          deviceName: '/dev/sda1',
          volume: ec2.BlockDeviceVolume.ebs(config.ebsVolumeSize, {
            volumeType: ec2.EbsDeviceVolumeType.GP3,
            encrypted: true,
          }),
        },
      ],
      keyPair,
    });

    // ──────────────────────────────────────────────
    // Elastic IP
    // ──────────────────────────────────────────────
    const eip = new ec2.CfnEIP(this, 'Eip', {
      tags: [{ key: 'Name', value: `${prefix}-eip` }],
    });

    new ec2.CfnEIPAssociation(this, 'EipAssoc', {
      allocationId: eip.attrAllocationId,
      instanceId: instance.instanceId,
    });

    // ──────────────────────────────────────────────
    // Route53 A Record
    // ──────────────────────────────────────────────
    const hostedZone = route53.HostedZone.fromLookup(this, 'HostedZone', {
      domainName: config.hostedZoneName,
    });

    new route53.ARecord(this, 'ApiRecord', {
      zone: hostedZone,
      recordName: config.domainName,
      target: route53.RecordTarget.fromIpAddresses(eip.ref),
      ttl: cdk.Duration.minutes(5),
    });

    // ──────────────────────────────────────────────
    // Secrets Manager
    // ──────────────────────────────────────────────
    const jwtSecret = new secretsmanager.Secret(this, 'JwtSecret', {
      secretName: `/workflow/${config.envName}/jwt`,
      description: 'JWT signing secret',
      generateSecretString: {
        passwordLength: 64,
        excludePunctuation: true,
      },
      removalPolicy: config.removalPolicy,
    });

    const mailSecret = new secretsmanager.Secret(this, 'MailSecret', {
      secretName: `/workflow/${config.envName}/mail`,
      description: 'Mail credentials — populate manually after deploy',
      secretStringValue: cdk.SecretValue.unsafePlainText(JSON.stringify({
        host: 'POPULATE_ME',
        port: '587',
        username: 'POPULATE_ME',
        password: 'POPULATE_ME',
        from: 'POPULATE_ME',
        fromName: 'POPULATE_ME',
      })),
      removalPolicy: config.removalPolicy,
    });

    // ──────────────────────────────────────────────
    // SSM Parameter Store
    // ──────────────────────────────────────────────
    new ssm.StringParameter(this, 'CorsOrigins', {
      parameterName: `/workflow/${config.envName}/cors-allowed-origins`,
      stringValue: config.envName === 'dev'
        ? 'https://dev.workfloow.app,http://localhost:5173'
        : 'https://workfloow.app',
      description: 'CORS allowed origins',
    });

    new ssm.StringParameter(this, 'WorkerInvitationUrl', {
      parameterName: `/workflow/${config.envName}/worker-invitation-frontend-url`,
      stringValue: config.envName === 'dev'
        ? 'https://dev.workfloow.app/worker/invitation'
        : 'https://workfloow.app/worker/invitation',
      description: 'Worker invitation frontend URL',
    });

    new ssm.StringParameter(this, 'AppDomain', {
      parameterName: `/workflow/${config.envName}/app-domain`,
      stringValue: config.domainName,
      description: 'Application domain — used by nginx template',
    });

    new ssm.StringParameter(this, 'S3BucketNameParam', {
      parameterName: `/workflow/${config.envName}/s3-bucket-name`,
      stringValue: uploadsBucket.bucketName,
      description: 'S3 uploads bucket name',
    });

    new ssm.StringParameter(this, 'S3RegionParam', {
      parameterName: `/workflow/${config.envName}/s3-region`,
      stringValue: this.region,
      description: 'AWS region for S3',
    });


    // ──────────────────────────────────────────────
    // CloudWatch Log Group
    // ──────────────────────────────────────────────
    new logs.LogGroup(this, 'AppLogGroup', {
      logGroupName: `/workflow/${config.envName}/app`,
      retention: config.logRetentionDays === 7
        ? logs.RetentionDays.ONE_WEEK
        : logs.RetentionDays.ONE_MONTH,
      removalPolicy: config.removalPolicy,
    });

    // ──────────────────────────────────────────────
    // AWS Backup (prod only)
    // ──────────────────────────────────────────────
    if (config.backupEnabled) {
      const backupVault = new backup.BackupVault(this, 'BackupVault', {
        backupVaultName: `${prefix}-vault`,
        removalPolicy: cdk.RemovalPolicy.RETAIN,
      });

      const backupPlan = new backup.BackupPlan(this, 'BackupPlan', {
        backupPlanName: `${prefix}-backup-plan`,
        backupVault,
        backupPlanRules: [
          // Daily at 2AM UTC — keep 30 days
          new backup.BackupPlanRule({
            ruleName: 'daily-30d',
            scheduleExpression: events.Schedule.cron({ hour: '2', minute: '0' }),
            deleteAfter: cdk.Duration.days(30),
            startWindow: cdk.Duration.hours(1),
            completionWindow: cdk.Duration.hours(2),
          }),
          // Weekly Sunday at 2AM UTC — keep 90 days
          new backup.BackupPlanRule({
            ruleName: 'weekly-90d',
            scheduleExpression: events.Schedule.cron({ hour: '2', minute: '0', weekDay: 'SUN' }),
            deleteAfter: cdk.Duration.days(90),
            startWindow: cdk.Duration.hours(1),
            completionWindow: cdk.Duration.hours(3),
          }),
        ],
      });

      backupPlan.addSelection('RdsSelection', {
        resources: [
          backup.BackupResource.fromRdsDbInstance(dbInstance),
        ],
      });

      new cdk.CfnOutput(this, 'BackupVaultName', {
        value: backupVault.backupVaultName,
        description: 'AWS Backup vault name',
      });
    }

    // ──────────────────────────────────────────────
    // CfnOutputs
    // ──────────────────────────────────────────────
    new cdk.CfnOutput(this, 'ElasticIp', {
      value: eip.ref,
      description: 'Elastic IP address — set as EC2_HOST in GitHub Secrets',
    });

    new cdk.CfnOutput(this, 'InstanceId', {
      value: instance.instanceId,
      description: 'EC2 Instance ID',
    });

    new cdk.CfnOutput(this, 'S3BucketName', {
      value: uploadsBucket.bucketName,
      description: 'S3 uploads bucket name',
    });

    new cdk.CfnOutput(this, 'JwtSecretArn', {
      value: jwtSecret.secretArn,
      description: 'JWT secret ARN',
    });

    new cdk.CfnOutput(this, 'DbEndpoint', {
      value: dbInstance.dbInstanceEndpointAddress,
      description: 'RDS MySQL endpoint',
    });

    new cdk.CfnOutput(this, 'DbSecretArn', {
      value: dbInstance.secret?.secretArn ?? 'N/A',
      description: 'RDS-generated credentials secret ARN',
    });

    new cdk.CfnOutput(this, 'MailSecretArn', {
      value: mailSecret.secretArn,
      description: 'Mail secret ARN — populate with real credentials',
    });

    new cdk.CfnOutput(this, 'SecurityGroupId', {
      value: sg.securityGroupId,
      description: 'EC2 Security Group ID — allow in RDS security group for port 3306',
    });

    new cdk.CfnOutput(this, 'SshKeyParam', {
      value: keyPair.privateKey.parameterName,
      description: 'SSM param holding SSH private key — retrieve with: aws ssm get-parameter --name <value> --with-decryption --query Parameter.Value --output text > key.pem',
    });

    new cdk.CfnOutput(this, 'SetupEnvScript', {
      value: '/home/ubuntu/app/setup-env.sh',
      description: 'Run this on EC2 to pull all secrets/params from AWS and write .env.dev',
    });
  }
}
