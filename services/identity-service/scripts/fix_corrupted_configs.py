#!/usr/bin/env python3

import os
import re
from pathlib import Path

def fix_corrupted_configs():
    """Fix all the corrupted config class names created by the migration script"""
    
    # Known corruptions that need to be fixed
    corruptions = {
        'NewNewBaseNewBaseTestConfig': 'NewBaseTestConfig',
        'UnifiedNewBaseTestConfig': 'NewBaseTestConfig',
        'SecurityNewBaseTestConfig': 'NewBaseTestConfig', 
        'IntegrationNewBaseTestConfig': 'NewIntegrationTestConfig',
        'JpaEntityNewBaseTestConfig': 'NewBaseTestConfig',
        'ResilienceNewBaseTestConfig': 'NewBaseTestConfig',
        'SimpleResilienceNewBaseTestConfig': 'NewBaseTestConfig',
        'SimplifiedGDPRComplianceTest.NewBaseTestConfig': 'NewBaseTestConfig',
        'OAuth2AuthorizationControllerTest.NewBaseTestConfig': 'NewBaseTestConfig'
    }
    
    print("🔧 Fixing corrupted config class names...")
    
    # Find all Java test files
    test_files = list(Path('src/test').glob('**/*.java'))
    fixed_files = []
    
    for test_file in test_files:
        try:
            with open(test_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            original_content = content
            
            # Apply all corruption fixes
            for corrupted_name, correct_name in corruptions.items():
                content = content.replace(corrupted_name, correct_name)
            
            # Only write if content changed
            if content != original_content:
                with open(test_file, 'w', encoding='utf-8') as f:
                    f.write(content)
                
                fixed_files.append(str(test_file))
                print(f"  ✅ Fixed: {os.path.basename(test_file)}")
        
        except Exception as e:
            print(f"  ❌ Error fixing {test_file}: {e}")
    
    print(f"\n🎉 Fixed {len(fixed_files)} files with corrupted config names")
    return fixed_files

if __name__ == "__main__":
    fix_corrupted_configs()