# GitHub Actions CI/CD

This repository now includes two GitHub Actions workflows:

- `CI`: builds and validates the backend and frontend on `push` and `pull_request` for `dev` and `main`
- `Docker Publish`: builds and pushes Docker images to GitHub Container Registry on `push` to `dev` or `main`, and on version tags like `v1.0.0`

## Published Images

The workflows publish these images to GHCR:

- `ghcr.io/senyfish/hrms-backend`
- `ghcr.io/senyfish/hrms-frontend`

If the repository owner changes, the image namespace changes with it.

## Tags

The publish workflow generates tags with these rules:

- `main` branch: `latest`, `main-<short-sha>`, `sha-<short-sha>`
- `dev` branch: `dev`, `dev-<short-sha>`, `sha-<short-sha>`
- Git tag `v*`: the Git tag itself, for example `v1.0.0`

## Required Repository Settings

No extra Docker registry secret is required when publishing to GHCR with the built-in `GITHUB_TOKEN`, but the workflow depends on:

- repository Actions being enabled
- workflow permissions allowing `Read and write permissions`
- package visibility being configured in GitHub Packages if you want to pull images without authentication

Recommended first-run checks:

1. Open the repository Actions tab and verify the workflows can run.
2. After the first successful publish, open the package page in GHCR and set visibility as needed.
3. If another environment needs to pull private images, use a Personal Access Token with `read:packages`.

## Trigger Summary

- Open a pull request to `dev` or `main`: runs `CI`
- Push to `dev` or `main`: runs `CI` and `Docker Publish`
- Push a tag like `v1.0.0`: runs `Docker Publish`

## Example Pull Commands

```bash
docker pull ghcr.io/senyfish/hrms-backend:dev
docker pull ghcr.io/senyfish/hrms-frontend:dev
```

For a stable release:

```bash
docker pull ghcr.io/senyfish/hrms-backend:v1.0.0
docker pull ghcr.io/senyfish/hrms-frontend:v1.0.0
```
