# Postman Collection - Maintenance Guide

## Quick Start

### First Time Setup

The collection has already been split into modules. Just build it:

```bash
npm run postman:build
```

Then import `docs/Work-Flow-App-API.postman_collection.json` into Postman.

### Regular Workflow

1. **Update an endpoint** - Edit the module file (e.g., `modules/auth.json`)
2. **Build** - Run `npm run postman:build`
3. **Import** - Re-import the collection to Postman

## File Structure

```
docs/
├── Work-Flow-App-API.postman_collection.json  # ← Import this to Postman
└── postman/
    ├── base.json           # Collection metadata & config
    ├── build.js            # Merge script
    ├── split.js            # Split script (one-time use)
    ├── modules/            # Individual controller files
    │   ├── auth.json                    (8 endpoints)
    │   ├── company.json                 (3 endpoints)
    │   ├── workers.json                 (8 endpoints)
    │   ├── clients.json                 (5 endpoints)
    │   ├── job-templates.json          (12 endpoints)
    │   ├── jobs.json                    (6 endpoints)
    │   ├── assets.json                  (6 endpoints)
    │   ├── asset-assignments.json       (4 endpoints)
    │   ├── workflows.json              (11 endpoints)
    │   └── job-workflows.json           (8 endpoints)
    ├── README.md
    └── GUIDE.md            # ← You are here
```

## Common Tasks

### Adding a New Endpoint

1. Open the appropriate module file (e.g., `modules/auth.json`)
2. Add your endpoint to the `item` array:

```json
{
  "name": "New Endpoint",
  "request": {
    "method": "POST",
    "header": [
      {
        "key": "Content-Type",
        "value": "application/json"
      }
    ],
    "body": {
      "mode": "raw",
      "raw": "{\n    \"field\": \"value\"\n}"
    },
    "url": {
      "raw": "{{base_url}}/api/v1/path/to/endpoint",
      "host": ["{{base_url}}"],
      "path": ["api", "v1", "path", "to", "endpoint"]
    }
  }
}
```

3. Build: `npm run postman:build`
4. Re-import to Postman

### Updating an Existing Endpoint

Example: Update the Worker Signup endpoint

1. Open `modules/auth.json`
2. Find the endpoint (search for "Worker Signup" or the URL)
3. Update the request body:

```json
{
  "body": {
    "mode": "raw",
    "raw": "{\n    \"invitationToken\": \"your-token-here\",\n    \"email\": \"worker@example.com\",\n    \"name\": \"John Doe\",\n    \"username\": \"johndoe\",\n    \"password\": \"password123\"\n}"
  }
}
```

4. Build: `npm run postman:build`
5. Re-import to Postman

### Adding a New Controller

1. Create a new module file: `modules/my-controller.json`
2. Use this template:

```json
{
  "name": "My Controller",
  "item": [
    {
      "name": "First Endpoint",
      "request": {
        "method": "GET",
        "url": {
          "raw": "{{base_url}}/api/v1/my-controller",
          "host": ["{{base_url}}"],
          "path": ["api", "v1", "my-controller"]
        }
      }
    }
  ]
}
```

3. Add to MODULE_ORDER in `build.js`:

```javascript
const MODULE_ORDER = [
  'auth',
  'company',
  // ... existing modules
  'my-controller'  // ← Add here
];
```

4. Build: `npm run postman:build`

### Removing a Controller

1. Delete the module file from `modules/`
2. Remove from MODULE_ORDER in `build.js`
3. Build: `npm run postman:build`

## Module File Format

Each module file follows this structure:

```json
{
  "name": "Controller Name",           // Folder name in Postman
  "item": [                             // Array of endpoints
    {
      "name": "Endpoint Name",          // Request name
      "event": [...],                    // Optional: Test scripts
      "request": {
        "method": "GET|POST|PUT|DELETE",
        "header": [...],                 // Headers
        "body": {...},                   // Request body (for POST/PUT)
        "url": {...},                    // URL configuration
        "description": "..."             // Endpoint documentation
      },
      "response": [...]                  // Optional: Example responses
    }
  ]
}
```

## For AI Assistants (Claude)

When updating endpoints, you can now:

1. **Read only the relevant module** instead of a 4000-line file
2. **Make targeted updates** to specific controllers
3. **See clearer git diffs** when changes are committed

### Example: Updating Worker Signup

Instead of searching through 3892 lines, you can:

```bash
# Read the auth module (only ~500 lines)
Read: docs/postman/modules/auth.json

# Edit the specific endpoint
Edit: docs/postman/modules/auth.json
      Update the Worker Signup request body

# Build the collection
Bash: npm run postman:build
```

Much easier! 🎉

## Troubleshooting

### Build Fails

- Check that all module files have valid JSON
- Ensure each module has `name` and `item` fields
- Run `npm run postman:build` to see error details

### Missing Endpoints After Build

- Check that the module is in `MODULE_ORDER` in `build.js`
- Verify the module file exists in `modules/` directory

### Collection Not Updating in Postman

- Make sure you're re-importing after building
- Or use Postman's sync feature to auto-update

## Git Workflow

### What to Commit

```bash
# Always commit module files
git add docs/postman/modules/*.json

# Always commit base.json if changed
git add docs/postman/base.json

# Commit the built collection
git add docs/Work-Flow-App-API.postman_collection.json

# Build scripts
git add docs/postman/build.js
git add docs/postman/split.js
```

### .gitignore

Add this to your `.gitignore` if you add npm dependencies:

```
node_modules/
```

## Benefits of This Structure

✅ **Smaller Files**: ~200-500 lines per module vs 4000+ lines
✅ **Easier to Find**: Jump directly to the controller you need
✅ **Better Diffs**: Git shows only the changed controller
✅ **AI-Friendly**: Claude can update specific modules efficiently
✅ **Maintainable**: Add/remove controllers independently
✅ **Team-Friendly**: Multiple people can edit different controllers
✅ **Automated**: Build script ensures consistency

## Questions?

Check the main [README.md](./README.md) or the module files for examples.
