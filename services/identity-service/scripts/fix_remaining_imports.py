#!/usr/bin/env python3

import os
import re
from pathlib import Path

def fix_remaining_imports():
    """Fix remaining import issues and class references"""
    
    fixes_to_apply = [
        # Fix one more corruption
        ('SimpleNewBaseTestConfig', 'NewBaseTestConfig'),
        
        # Add missing imports where needed - files using NewBaseTestConfig but missing import
        ('AuditLogTest.java', 'import com.focushive.identity.config.NewBaseTestConfig;'),
        ('SimplifiedGDPRComplianceTest.java', 'import com.focushive.identity.config.NewBaseTestConfig;'),
        ('OAuth2AuthorizationControllerTest.java', 'import com.focushive.identity.config.NewBaseTestConfig;'),
        
        # Add missing imports where needed - files using NewIntegrationTestConfig but missing import
        ('CookieAuthenticationTest.java', 'import com.focushive.identity.config.NewIntegrationTestConfig;'),
        ('RegistrationDebugTest.java', 'import com.focushive.identity.config.NewIntegrationTestConfig;'),
        ('OAuth2TokenOperationsTest.java', 'import com.focushive.identity.config.NewIntegrationTestConfig;'),
        ('OAuth2AuthorizationFlowTest.java', 'import com.focushive.identity.config.NewIntegrationTestConfig;'),
        ('SimplePerformanceTestControllerTest.java', 'import com.focushive.identity.config.NewIntegrationTestConfig;'),
        
        # Add missing NewBaseTestConfig imports to resilience tests
        ('GracefulShutdownResilienceTest.java', 'import com.focushive.identity.config.NewBaseTestConfig;'),
        ('StartupHealthChecksResilienceTest.java', 'import com.focushive.identity.config.NewBaseTestConfig;'),
        ('CascadingFailurePreventionResilienceTest.java', 'import com.focushive.identity.config.NewBaseTestConfig;'),
        ('PoisonMessageHandlingResilienceTest.java', 'import com.focushive.identity.config.NewBaseTestConfig;'),
        ('SimpleResilienceTest.java', 'import com.focushive.identity.config.NewBaseTestConfig;'),
        ('CircuitBreakerRecoveryResilienceTest.java', 'import com.focushive.identity.config.NewBaseTestConfig;'),
    ]
    
    print("🔧 Applying final fixes for imports and references...")
    
    # Find all Java test files
    test_files = list(Path('src/test').glob('**/*.java'))
    fixed_files = []
    
    for test_file in test_files:
        try:
            with open(test_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            original_content = content
            filename = os.path.basename(test_file)
            
            # Apply simple string replacements
            content = content.replace('SimpleNewBaseTestConfig', 'NewBaseTestConfig')
            
            # Add missing imports where needed
            for target_file, import_statement in [
                ('AuditLogTest.java', 'import com.focushive.identity.config.NewBaseTestConfig;'),
                ('SimplifiedGDPRComplianceTest.java', 'import com.focushive.identity.config.NewBaseTestConfig;'),
                ('OAuth2AuthorizationControllerTest.java', 'import com.focushive.identity.config.NewBaseTestConfig;'),
                ('CookieAuthenticationTest.java', 'import com.focushive.identity.config.NewIntegrationTestConfig;'),
                ('RegistrationDebugTest.java', 'import com.focushive.identity.config.NewIntegrationTestConfig;'),
                ('OAuth2TokenOperationsTest.java', 'import com.focushive.identity.config.NewIntegrationTestConfig;'),
                ('OAuth2AuthorizationFlowTest.java', 'import com.focushive.identity.config.NewIntegrationTestConfig;'),
                ('SimplePerformanceTestControllerTest.java', 'import com.focushive.identity.config.NewIntegrationTestConfig;'),
                ('GracefulShutdownResilienceTest.java', 'import com.focushive.identity.config.NewBaseTestConfig;'),
                ('StartupHealthChecksResilienceTest.java', 'import com.focushive.identity.config.NewBaseTestConfig;'),
                ('CascadingFailurePreventionResilienceTest.java', 'import com.focushive.identity.config.NewBaseTestConfig;'),
                ('PoisonMessageHandlingResilienceTest.java', 'import com.focushive.identity.config.NewBaseTestConfig;'),
                ('SimpleResilienceTest.java', 'import com.focushive.identity.config.NewBaseTestConfig;'),
                ('CircuitBreakerRecoveryResilienceTest.java', 'import com.focushive.identity.config.NewBaseTestConfig;'),
            ]:
                if filename == target_file:
                    # Check if import already exists
                    if import_statement not in content:
                        # Find the last import statement and add after it
                        import_pattern = r'(import [^;]+;)\s*\n'
                        imports = re.findall(import_pattern, content)
                        if imports:
                            # Add after last import
                            last_import = imports[-1]
                            content = content.replace(
                                last_import + '\n',
                                last_import + '\n' + import_statement + '\n'
                            )
                        else:
                            # Add after package declaration
                            package_pattern = r'(package [^;]+;)\s*\n'
                            match = re.search(package_pattern, content)
                            if match:
                                package_line = match.group(1)
                                content = content.replace(
                                    package_line + '\n',
                                    package_line + '\n\n' + import_statement + '\n'
                                )
            
            # Only write if content changed
            if content != original_content:
                with open(test_file, 'w', encoding='utf-8') as f:
                    f.write(content)
                
                fixed_files.append(str(test_file))
                print(f"  ✅ Fixed: {filename}")
        
        except Exception as e:
            print(f"  ❌ Error fixing {test_file}: {e}")
    
    print(f"\n🎉 Applied final fixes to {len(fixed_files)} files")
    return fixed_files

if __name__ == "__main__":
    fix_remaining_imports()