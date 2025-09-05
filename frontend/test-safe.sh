#!/bin/bash
# Safe test runner with resource monitoring
echo "🔍 Starting Vitest with resource monitoring..."
echo "📊 Current memory usage before tests:"
free -h | grep "Mem:"

# Run tests with limited resources
echo "🧪 Running Vitest with worker limits..."
npm test -- --run --reporter=basic --poolOptions.threads.maxThreads=4

echo "📊 Memory usage after tests:"
free -h | grep "Mem:"
echo "✅ Test run complete!"
