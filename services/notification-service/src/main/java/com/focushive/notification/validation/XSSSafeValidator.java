package com.focushive.notification.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validator implementation for XSSSafe annotation.
 * Uses pattern matching to detect potentially dangerous XSS content.
 */
public class XSSSafeValidator implements ConstraintValidator<XSSSafe, String> {

    private boolean allowBasicHtml;
    
    // Common XSS attack patterns
    private static final List<Pattern> XSS_PATTERNS = Arrays.asList(
            // Script tags
            Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("<script[^>]*>", Pattern.CASE_INSENSITIVE),
            
            // JavaScript protocols
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("data:", Pattern.CASE_INSENSITIVE),
            
            // Event handlers
            Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
            
            // HTML entities that could be used for XSS
            Pattern.compile("&#[x]?[0-9a-f]+;?", Pattern.CASE_INSENSITIVE),
            
            // Style with expression (IE specific)
            Pattern.compile("style\\s*=.*?expression\\s*\\(", Pattern.CASE_INSENSITIVE),
            
            // Object, embed, iframe tags
            Pattern.compile("<(object|embed|iframe|form|meta|link)", Pattern.CASE_INSENSITIVE)
    );
    
    // Basic HTML patterns that are generally safe
    private static final List<Pattern> SAFE_HTML_PATTERNS = Arrays.asList(
            Pattern.compile("</?[bi]>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("</?strong>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("</?em>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("</?p>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("</?br/??>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("</?h[1-6]>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("</?ul>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("</?ol>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("</?li>", Pattern.CASE_INSENSITIVE)
    );

    @Override
    public void initialize(XSSSafe annotation) {
        this.allowBasicHtml = annotation.allowBasicHtml();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Null values are handled by @NotNull
        if (value == null) {
            return true;
        }
        
        // Check for XSS patterns
        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(value).find()) {
                return false;
            }
        }
        
        // If basic HTML is not allowed, check for any HTML tags
        if (!allowBasicHtml) {
            Pattern htmlPattern = Pattern.compile("<[^>]+>", Pattern.CASE_INSENSITIVE);
            if (htmlPattern.matcher(value).find()) {
                // Check if it's only safe HTML
                String testValue = value;
                for (Pattern safePattern : SAFE_HTML_PATTERNS) {
                    testValue = safePattern.matcher(testValue).replaceAll("");
                }
                
                // If there are still HTML tags after removing safe ones, it's not valid
                if (htmlPattern.matcher(testValue).find()) {
                    return false;
                }
            }
        }
        
        return true;
    }
}