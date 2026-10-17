# Worldloom

A persistent world shaped by successive lives. Step 1 is deliberately small: define a custom world, validate it, and create/load its definition in **real AWS DynamoDB** from a local Java command-line app.

No characters, simulation, web server, AI calls, or Lambda yet. There is no local storage fallback and no automatic cloud provisioning.

## Requirements

- Java 17+ and Maven 3.9+.
- For cloud commands: AWS credentials, a region, and a DynamoDB table.

From `backend/`:

```powershell
mvn test
mvn exec:java "-Dexec.args=validate ../examples/ashen-empire.json"
mvn exec:java "-Dexec.args=validate ../examples/orbital-garden.json"
```

Validation and unit tests make no AWS calls. Maven downloads dependencies on the first run.

## Create and load a world in AWS

Use your existing AWS CLI profile (SSO profiles are supported). Do not put credentials in source files. The SDK uses the standard AWS credential chain.

```powershell
$env:AWS_PROFILE = "your-profile"
$env:AWS_REGION = "us-east-1"
$env:WORLDLOOM_TABLE = "WorldLoomWorlds"
```

Run the following explicitly when ready to provision a billable, on-demand table. It is not run by the app or tests:

```powershell
aws dynamodb create-table --table-name $env:WORLDLOOM_TABLE --attribute-definitions AttributeName=worldId,AttributeType=S --key-schema AttributeName=worldId,KeyType=HASH --billing-mode PAY_PER_REQUEST --region $env:AWS_REGION
aws dynamodb wait table-exists --table-name $env:WORLDLOOM_TABLE --region $env:AWS_REGION
```

Then, from `backend/`:

```powershell
mvn exec:java "-Dexec.args=create ../examples/ashen-empire.json"
mvn exec:java "-Dexec.args=show ashen-empire"
```

The application needs `dynamodb:PutItem` and `dynamodb:GetItem` on this table. Provisioning additionally needs `dynamodb:CreateTable` and `dynamodb:DescribeTable`. A repeated `create` fails instead of overwriting an existing world. Reads are strongly consistent.

## Customize a world

Copy either example and change its `id`. Required fields:

- `schemaVersion`: currently `1`.
- `id`: 1–64 lowercase letters, digits, or hyphens; starts with a letter or digit.
- `name`, `background`, `tone`: nonempty text.
- `rules`: array of nonempty narrative descriptions.
- `locations`, `factions`, `resources`, `attributes`: arrays, possibly empty. Each entry has a nonempty `id` and `name`; IDs are unique within each array.

Additional fields anywhere in the document are preserved, including a `custom` object for setting-specific concepts. Rules and attribute metadata describe the world; they do not implement mechanics yet. Referenced relationships and numeric attribute bounds are not semantically validated in this step.

Definitions are limited to 350 KB of compact UTF-8 JSON to leave room under DynamoDB's item limit. Each definition is stored as one item (`worldId`, `definition`, `createdAt`). Future characters and event histories will be separate records, not appended to this document.

## Next small step

Review the two sample world definitions and adjust the format around the worlds you actually want to build. Then add editing with version checks before starting characters or events.
