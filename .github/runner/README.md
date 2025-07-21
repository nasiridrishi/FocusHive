# GitHub Actions Local Runner

Run GitHub Actions workflows locally in a Docker container for testing before pushing to GitHub.

## Features

- Run complete GitHub Actions workflows locally
- Test individual jobs from workflows
- Full environment with Java 21, Node.js 20, and Docker
- Caching support for faster subsequent runs
- Interactive shell for debugging
- Local Docker registry for testing image builds

## Prerequisites

- Docker and Docker Compose installed
- Docker daemon running
- Sufficient disk space for caching

## Quick Start

### 1. Build the Runner Container

```bash
./.github/runner/run-local.sh build
```

### 2. Run the CI Workflow

```bash
./.github/runner/run-local.sh run
```

### 3. Run Specific Jobs

```bash
# Run only backend tests
./.github/runner/run-local.sh run -j backend-test

# Run only identity service tests
./.github/runner/run-local.sh run -j identity-service-test

# Run only frontend tests
./.github/runner/run-local.sh run -j frontend-test
```

### 4. Interactive Shell

```bash
./.github/runner/run-local.sh shell
```

Inside the shell, you can run act commands directly:

```bash
# List all workflows
act -l

# Run specific workflow
act -W .github/workflows/ci.yml

# Run specific job with verbose output
act -j backend-test -v

# Dry run to see what would be executed
act -n
```

## Advanced Usage

### Custom Workflow Files

```bash
# Run a different workflow file
./.github/runner/run-local.sh run -w my-workflow.yml
```

### Environment Variables

Create a `.env` file in the `.github/runner/` directory:

```env
# Custom act options
ACT_OPTIONS=-P ubuntu-latest=catthehacker/ubuntu:act-latest --container-architecture linux/amd64

# Workflow to run by default
WORKFLOW=ci.yml
```

### Using Local Registry

The setup includes a local Docker registry for testing image builds:

```yaml
# In your workflow, use:
localhost:5000/your-image:tag
```

## Troubleshooting

### Permission Denied

If you get permission errors with Docker socket:

```bash
# On Linux, add your user to docker group
sudo usermod -aG docker $USER
# Log out and back in
```

### Out of Space

Clean up Docker resources:

```bash
# Clean up our containers and volumes
./.github/runner/run-local.sh clean

# Clean all Docker resources
docker system prune -a
```

### Workflow Fails Locally but Works on GitHub

- Check if you're using GitHub-specific features (like secrets)
- Ensure all required environment variables are set
- Use `-v` flag for verbose output

### Slow Performance

- The first run will be slow due to image downloads
- Subsequent runs use cached layers
- Consider allocating more resources to Docker

## Container Structure

The runner container includes:

- Ubuntu 22.04 base
- Java 21 (OpenJDK)
- Node.js 20
- Python 3
- Docker CLI (for Docker-in-Docker operations)
- act (GitHub Actions local runner)
- Common build tools

## Differences from GitHub Actions

- No access to GitHub secrets (use local env vars)
- No access to GitHub API without token
- Some GitHub-specific actions may not work
- Local paths instead of workspace paths
- No matrix strategy support (run jobs individually)

## Tips

1. **Test Incrementally**: Run individual jobs first before full workflow
2. **Use Verbose Mode**: Add `-v` to act options for debugging
3. **Check Logs**: Container logs are available via `docker-compose logs`
4. **Cache Wisely**: Volumes persist between runs for speed
5. **Clean Regularly**: Run clean command to free up space

## Security Notes

- Don't commit sensitive data in `.env` files
- The local registry is not secured (development only)
- Docker socket mounting gives container full Docker access
- Use only for local development, not production

## Support

For issues or questions:
1. Check act documentation: https://github.com/nektos/act
2. Review GitHub Actions docs: https://docs.github.com/en/actions
3. Check container logs: `docker-compose logs github-runner`