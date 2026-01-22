# Postman Collection Structure

This directory contains a modular Postman collection structure that makes it easier to maintain and update individual API endpoints.

## Structure

```
postman/
├── modules/           # Individual controller collections
│   ├── auth.json
│   ├── company.json
│   ├── workers.json
│   ├── clients.json
│   ├── job-templates.json
│   ├── jobs.json
│   ├── assets.json
│   ├── asset-assignments.json
│   ├── workflows.json
│   └── job-workflows.json
├── base.json         # Collection metadata and shared config
├── build.js          # Script to merge modules
└── README.md         # This file
```

## Usage

### Building the Collection

```bash
# From the Backend directory
npm run postman:build
```

This generates the complete collection at:
`docs/Work-Flow-App-API.postman_collection.json`

### Importing to Postman

1. Build the collection (see above)
2. Open Postman
3. Click Import → Upload Files
4. Select `docs/Work-Flow-App-API.postman_collection.json`

### Updating Endpoints

To update endpoints for a specific controller:

1. Edit the corresponding module file in `docs/postman/modules/`
2. Run `npm run postman:build` to regenerate the collection
3. Re-import to Postman (or use Postman's sync feature)

## Benefits

- **Easier Maintenance**: Each controller is in its own file
- **Better Organization**: Find endpoints quickly
- **Smaller Files**: ~200-400 lines per module vs 4000+ lines
- **AI-Friendly**: Claude can update individual modules more easily
- **Git Diffs**: Changes are clearer in version control
- **Modular**: Add/remove controllers easily

## Module Structure

Each module file contains:
- Controller name
- Array of endpoint items
- Each endpoint includes:
  - Request details (method, URL, headers, body)
  - Test scripts (for auto-saving tokens, etc.)
  - Example responses
  - Documentation

See `modules/auth.json` for an example.