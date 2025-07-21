#!/bin/bash
set -e

echo "GitHub Actions Local Runner"
echo "=========================="
echo ""

# Check if we're running a specific workflow
if [ -n "$WORKFLOW" ]; then
    echo "Running workflow: $WORKFLOW"
    cd /workspace
    act -W ".github/workflows/${WORKFLOW}" $ACT_OPTIONS
else
    echo "Available workflows:"
    ls -la /workspace/.github/workflows/
    echo ""
    echo "To run a specific workflow, set WORKFLOW environment variable"
    echo "Example: docker run -e WORKFLOW=ci.yml ..."
    
    # Keep container running for interactive use
    echo ""
    echo "Container is ready for interactive use."
    echo "You can run workflows with: act -W .github/workflows/ci.yml"
    /bin/bash
fi