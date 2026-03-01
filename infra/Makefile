.PHONY: install bootstrap synth-dev synth-prod synth-all deploy-dev deploy-prod deploy-all destroy-dev destroy-prod destroy-all diff-dev diff-prod diff-all list

# AWS Configuration
AWS_ACCOUNT ?= 768102298608
REGION ?= eu-central-1

# Install dependencies
install:
	npm install

# Bootstrap CDK (one-time per account/region)
bootstrap:
	@echo "Bootstrapping account $(AWS_ACCOUNT) in region $(REGION)..."
	npx cdk bootstrap aws://$(AWS_ACCOUNT)/$(REGION)

# Synthesize stacks
synth-dev:
	npx cdk synth -c env=dev

synth-prod:
	npx cdk synth -c env=prod

synth-all:
	npx cdk synth

# Deploy stacks
deploy-dev:
	npx cdk deploy -c env=dev --require-approval never

deploy-prod:
	npx cdk deploy -c env=prod

deploy-all:
	npx cdk deploy --all

# Destroy stacks
destroy-dev:
	npx cdk destroy -c env=dev --force

destroy-prod:
	npx cdk destroy -c env=prod

destroy-all:
	npx cdk destroy --all

# Show diff before deploying
diff-dev:
	npx cdk diff -c env=dev

diff-prod:
	npx cdk diff -c env=prod

diff-all:
	npx cdk diff

# List all stacks
list:
	npx cdk list -c env=dev
	npx cdk list -c env=prod

# Quick dev workflow
dev: synth-dev deploy-dev

# Help
help:
	@echo "Available commands:"
	@echo "  make install      - Install npm dependencies"
	@echo "  make bootstrap    - Bootstrap CDK (one-time setup)"
	@echo "  make synth-dev    - Synthesize dev stack"
	@echo "  make synth-prod   - Synthesize prod stack"
	@echo "  make synth-all    - Synthesize both stacks"
	@echo "  make deploy-dev   - Deploy dev stack"
	@echo "  make deploy-prod  - Deploy prod stack (with approval)"
	@echo "  make deploy-all   - Deploy both stacks"
	@echo "  make destroy-dev  - Destroy dev stack"
	@echo "  make destroy-prod - Destroy prod stack"
	@echo "  make destroy-all  - Destroy both stacks"
	@echo "  make diff-dev     - Show changes before deploying dev"
	@echo "  make diff-prod    - Show changes before deploying prod"
	@echo "  make diff-all     - Show changes for both stacks"
	@echo "  make list         - List all stacks"
	@echo "  make dev          - Synth and deploy dev (quick)"
	@echo ""
	@echo "Override: make bootstrap AWS_ACCOUNT=123456789 REGION=eu-west-1"
