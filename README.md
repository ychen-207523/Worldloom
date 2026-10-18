# Worldloom

A persistent world shaped by successive lives. Create a custom world in a **React** editor and save/load its definition through a local **Spring Boot** API connected to **real AWS DynamoDB**. The original Java command-line tools also remain available.

This step covers world definitions. Characters, simulation, AI calls, and Lambda come later. The editor keeps an unfinished draft in browser storage; saved worlds live in DynamoDB. No cloud resources are created automatically.

## Run the browser app

Requirements: Java 17+, Maven 3.9+, Node.js 20.17+ (or a supported newer LTS), and npm. The frontend uses Vite 6 to support the installed Node 20 runtime.

In the first PowerShell terminal:

```powershell
cd D:\Project\Worldloom\backend
$env:JAVA_HOME = "C:\Program Files\Java\jdk-22"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:AWS_REGION = "us-east-1"
$env:WORLDLOOM_TABLE = "WorldLoomWorlds"
mvn spring-boot:run
```

Your default AWS profile is used automatically. If `AWS_PROFILE` is already set to another profile, remove it with `Remove-Item Env:AWS_PROFILE -ErrorAction SilentlyContinue`. Change `JAVA_HOME` if your JDK is installed elsewhere.

In a second PowerShell terminal:

```powershell
cd D:\Project\Worldloom\frontend
npm install
npm run dev
```

Open **http://127.0.0.1:5173**. Both processes must remain running. Stop either with Ctrl+C. On the first run, Maven and npm download dependencies.

The backend binds to loopback at port 8080. Vite proxies `/api` to it, so AWS credentials stay in Java and CORS configuration is unnecessary. The connection badge checks API availability, not DynamoDB permissions. This is a local development setup without public authentication; it is not ready to expose to the internet. The frontend production bundle can be built with `npm run build`; production API routing/hosting is a later step.

### Use the editor

1. Enter a unique world ID, name, background, and tone.
2. Add narrative rules, one per line (optional).
3. Edit the JSON lists for factions, locations, resources, and attributes. Extra fields, including custom concepts, are retained when loading/saving.
4. Select **Save world** to create the world in DynamoDB.
5. Enter its ID under **Return to a world** and select **Load world** to read it back. You can try `ashen-empire` if you saved that sample previously.

Saving is create-only. To save an edited copy of an existing world, change its ID. Saving under an existing ID returns a conflict and leaves the original intact. Editing existing saved worlds in place is a later step.

### Verification

```powershell
cd D:\Project\Worldloom\backend
mvn test
cd D:\Project\Worldloom\frontend
npm test
npm run build
```

Automated tests use a mocked DynamoDB boundary and make no AWS calls. API tests cover successful create/read, preserved custom fields, invalid input, missing worlds, duplicate IDs, and AWS connection errors. Editor data tests check full sample-world round trips and draft/error handling.

### Local API

- `GET /api/health`: API health only; does not call AWS.
- `POST /api/worlds`: create a world from a JSON body (201; duplicate ID 409; invalid world 400).
- `GET /api/worlds/{id}`: load a world (200; unknown ID 404).
- AWS connectivity/configuration failures return 503 with a message to check backend configuration.

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

Use your existing default AWS CLI profile. Do not put credentials in source files. The SDK uses the standard AWS credential chain; named and SSO profiles are also supported if you explicitly select one.

```powershell
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

- `id`: 1–64 lowercase letters, digits, or hyphens; starts with a letter or digit.
- `name`, `background`, `tone`: nonempty text.
- `rules`: array of nonempty narrative descriptions.
- `locations`, `factions`, `resources`, `attributes`: arrays, possibly empty. Each entry has a nonempty `id` and `name`; IDs are unique within each array.

Additional fields anywhere in the document are preserved, including a `custom` object for setting-specific concepts. Rules and attribute metadata describe the world; they do not implement mechanics yet. Referenced relationships and numeric attribute bounds are not semantically validated in this step.

Definitions are limited to 350 KB of compact UTF-8 JSON to leave room under DynamoDB's item limit. Each definition is stored as one item (`worldId`, `definition`, `createdAt`). Future characters and event histories will be separate records, not appended to this document.

## Next small step

Improve faction and resource editing with dedicated form controls, then add saved-world updates before starting characters or events.
