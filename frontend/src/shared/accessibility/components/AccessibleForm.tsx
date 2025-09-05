/**
 * Accessible Form Components
 * 
 * WCAG 2.1 AA compliant form components with proper validation,
 * error handling, screen reader support, and keyboard navigation.
 */

import React, { forwardRef } from 'react';
import {
  TextField,
  TextFieldProps,
  FormControl,
  FormControlProps,
  FormLabel,
  FormLabelProps,
  FormHelperText,
  FormHelperTextProps,
  InputLabel,
  Select,
  SelectProps,
  MenuItem,
  Checkbox,
  CheckboxProps,
  Radio,
  RadioProps,
  FormGroup,
  FormControlLabel,
  Switch,
  SwitchProps,
  Autocomplete,
  AutocompleteProps,
  Chip,
  Box,
  Alert,
} from '@mui/material';
import { styled } from '@mui/material/styles';
import { useFormAnnouncement } from '../hooks/useAnnouncement';
import { useLiveRegion } from '../hooks/useLiveRegion';
import type { AccessibleProps } from '../types/accessibility';

// Enhanced TextField with accessibility features
const StyledAccessibleTextField = styled(TextField)(({ theme }) => ({
  '& .MuiInputBase-root': {
    minHeight: '44px',
    
    '&:focus-within': {
      outline: `2px solid ${theme.palette.primary.main}`,
      outlineOffset: '2px',
    },
  },
  
  '& .MuiFormHelperText-root': {
    '&.Mui-error': {
      fontWeight: 500,
      
      '&::before': {
        content: '"Error: "',
        fontWeight: 700,
      },
    },
  },
  
  // High contrast mode support
  '@media (prefers-contrast: high)': {
    '& .MuiInputBase-root': {
      border: '2px solid currentColor',
      
      '&:focus-within': {
        outline: '3px solid currentColor',
        outlineOffset: '3px',
      },
    },
  },
}));

export interface AccessibleTextFieldProps extends Omit<TextFieldProps, 'error'> {
  /**
   * Error state with accessible error message
   */
  error?: boolean;
  
  /**
   * Error message for screen readers
   */
  errorMessage?: string;
  
  /**
   * Success state and message
   */
  success?: boolean;
  successMessage?: string;
  
  /**
   * Instructions for completing the field
   */
  instructions?: string;
  
  /**
   * Character count limit
   */
  maxLength?: number;
  
  /**
   * Show character count
   */
  showCharacterCount?: boolean;
  
  /**
   * Auto-announce validation results
   */
  announceValidation?: boolean;
}

/**
 * Accessible Text Field Component
 */
export const AccessibleTextField = forwardRef<HTMLDivElement, AccessibleTextFieldProps>(({
  label,
  helperText,
  error = false,
  errorMessage,
  success = false,
  successMessage,
  instructions,
  maxLength,
  showCharacterCount = false,
  announceValidation = true,
  onChange,
  ...props
}, ref) => {
  const [value, setValue] = React.useState(props.value || props.defaultValue || '');
  const [hasInteracted, setHasInteracted] = React.useState(false);
  
  const { announceValidationError, announceValidationSuccess } = useFormAnnouncement();
  
  // Generate unique IDs
  const errorId = React.useId();
  const successId = React.useId();
  const instructionsId = React.useId();
  const countId = React.useId();

  const characterCount = typeof value === 'string' ? value.length : 0;
  const remainingChars = maxLength ? maxLength - characterCount : null;
  const isOverLimit = maxLength && characterCount > maxLength;

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const newValue = event.target.value;
    setValue(newValue);
    setHasInteracted(true);
    
    // Announce validation results
    if (announceValidation && hasInteracted) {
      if (error && errorMessage) {
        announceValidationError(errorMessage);
      } else if (success && successMessage) {
        announceValidationSuccess(successMessage);
      }
    }
    
    onChange?.(event);
  };

  const handleBlur = (event: React.FocusEvent<HTMLInputElement>) => {
    setHasInteracted(true);
    props.onBlur?.(event);
  };

  // Construct aria-describedby
  const describedByIds = [
    props['aria-describedby'],
    instructions ? instructionsId : null,
    error && errorMessage ? errorId : null,
    success && successMessage ? successId : null,
    showCharacterCount && maxLength ? countId : null,
  ].filter(Boolean).join(' ') || undefined;

  const combinedHelperText = helperText || 
    (error && errorMessage ? errorMessage : '') || 
    (success && successMessage ? successMessage : '');

  return (
    <Box>
      <StyledAccessibleTextField
        {...props}
        ref={ref}
        label={label}
        value={value}
        onChange={handleChange}
        onBlur={handleBlur}
        error={error || isOverLimit}
        helperText={combinedHelperText}
        aria-describedby={describedByIds}
        aria-invalid={error || isOverLimit}
        inputProps={{
          ...props.inputProps,
          maxLength,
          'aria-required': props.required,
          'aria-invalid': error || isOverLimit,
        }}
      />

      {/* Hidden instructions for screen readers */}
      {instructions && (
        <FormHelperText id={instructionsId} sx={{ 
          position: 'absolute', 
          width: '1px', 
          height: '1px', 
          padding: 0, 
          margin: '-1px', 
          overflow: 'hidden', 
          clip: 'rect(0, 0, 0, 0)', 
          whiteSpace: 'nowrap', 
          border: 0 
        }}>
          {instructions}
        </FormHelperText>
      )}

      {/* Character count */}
      {showCharacterCount && maxLength && (
        <FormHelperText 
          id={countId}
          error={isOverLimit}
          sx={{ textAlign: 'right', mt: 0.5 }}
        >
          {characterCount} / {maxLength} characters
          {remainingChars !== null && remainingChars < 10 && (
            <span> ({remainingChars} remaining)</span>
          )}
        </FormHelperText>
      )}

      {/* Success message */}
      {success && successMessage && (
        <Alert severity="success" id={successId} sx={{ mt: 1 }}>
          {successMessage}
        </Alert>
      )}
    </Box>
  );
});

AccessibleTextField.displayName = 'AccessibleTextField';

/**
 * Accessible Select Component
 */
export interface AccessibleSelectProps<T = unknown> extends Omit<SelectProps<T>, 'error'> {
  /**
   * Required label for the select
   */
  label: string;
  
  /**
   * Error state and message
   */
  error?: boolean;
  errorMessage?: string;
  
  /**
   * Instructions for the select
   */
  instructions?: string;
  
  /**
   * Options for the select
   */
  options: Array<{ value: T; label: string; disabled?: boolean }>;
}

export const AccessibleSelect = <T extends unknown>({
  label,
  error = false,
  errorMessage,
  instructions,
  options,
  ...props
}: AccessibleSelectProps<T>) => {
  const labelId = React.useId();
  const errorId = React.useId();
  const instructionsId = React.useId();

  const describedByIds = [
    props['aria-describedby'],
    instructions ? instructionsId : null,
    error && errorMessage ? errorId : null,
  ].filter(Boolean).join(' ') || undefined;

  return (
    <FormControl error={error} fullWidth={props.fullWidth}>
      <InputLabel id={labelId} required={props.required}>
        {label}
      </InputLabel>
      
      <Select
        {...props}
        labelId={labelId}
        label={label}
        aria-describedby={describedByIds}
        aria-invalid={error}
        MenuProps={{
          ...props.MenuProps,
          PaperProps: {
            ...props.MenuProps?.PaperProps,
            role: 'listbox',
          },
        }}
      >
        {options.map((option) => (
          <MenuItem 
            key={String(option.value)} 
            value={option.value}
            disabled={option.disabled}
            role="option"
          >
            {option.label}
          </MenuItem>
        ))}
      </Select>

      {/* Instructions */}
      {instructions && (
        <FormHelperText id={instructionsId}>
          {instructions}
        </FormHelperText>
      )}

      {/* Error message */}
      {error && errorMessage && (
        <FormHelperText id={errorId} error>
          {errorMessage}
        </FormHelperText>
      )}
    </FormControl>
  );
};

/**
 * Accessible Checkbox Component
 */
export interface AccessibleCheckboxProps extends Omit<CheckboxProps, 'inputProps'> {
  /**
   * Label for the checkbox
   */
  label: string;
  
  /**
   * Description or help text
   */
  description?: string;
  
  /**
   * Error state and message
   */
  error?: boolean;
  errorMessage?: string;
}

export const AccessibleCheckbox = forwardRef<HTMLButtonElement, AccessibleCheckboxProps>(({
  label,
  description,
  error = false,
  errorMessage,
  ...props
}, ref) => {
  const descriptionId = React.useId();
  const errorId = React.useId();

  const describedByIds = [
    description ? descriptionId : null,
    error && errorMessage ? errorId : null,
  ].filter(Boolean).join(' ') || undefined;

  return (
    <Box>
      <FormControlLabel
        control={
          <Checkbox
            {...props}
            ref={ref}
            inputProps={{
              'aria-describedby': describedByIds,
              'aria-invalid': error,
            }}
            sx={{
              '&:focus': {
                outline: '2px solid',
                outlineColor: 'primary.main',
                outlineOffset: '2px',
              },
              '@media (prefers-contrast: high)': {
                border: '2px solid currentColor',
                '&:focus': {
                  outline: '3px solid currentColor',
                  outlineOffset: '3px',
                },
              },
            }}
          />
        }
        label={label}
      />

      {/* Description */}
      {description && (
        <FormHelperText id={descriptionId} sx={{ ml: 4 }}>
          {description}
        </FormHelperText>
      )}

      {/* Error message */}
      {error && errorMessage && (
        <FormHelperText id={errorId} error sx={{ ml: 4 }}>
          {errorMessage}
        </FormHelperText>
      )}
    </Box>
  );
});

AccessibleCheckbox.displayName = 'AccessibleCheckbox';

/**
 * Accessible Radio Group Component
 */
export interface AccessibleRadioGroupProps {
  /**
   * Required label for the radio group
   */
  label: string;
  
  /**
   * Radio options
   */
  options: Array<{ value: string; label: string; description?: string; disabled?: boolean }>;
  
  /**
   * Selected value
   */
  value?: string;
  
  /**
   * Change handler
   */
  onChange?: (value: string) => void;
  
  /**
   * Required field
   */
  required?: boolean;
  
  /**
   * Error state and message
   */
  error?: boolean;
  errorMessage?: string;
  
  /**
   * Instructions for the radio group
   */
  instructions?: string;
  
  /**
   * Orientation
   */
  row?: boolean;
}

export const AccessibleRadioGroup: React.FC<AccessibleRadioGroupProps> = ({
  label,
  options,
  value,
  onChange,
  required = false,
  error = false,
  errorMessage,
  instructions,
  row = false,
}) => {
  const groupId = React.useId();
  const instructionsId = React.useId();
  const errorId = React.useId();

  const describedByIds = [
    instructions ? instructionsId : null,
    error && errorMessage ? errorId : null,
  ].filter(Boolean).join(' ') || undefined;

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    onChange?.(event.target.value);
  };

  return (
    <FormControl error={error} required={required}>
      <FormLabel 
        component="legend" 
        id={groupId}
        sx={{ fontWeight: 'medium', color: 'text.primary' }}
      >
        {label}
        {required && <span aria-label="required"> *</span>}
      </FormLabel>

      {/* Instructions */}
      {instructions && (
        <FormHelperText id={instructionsId} sx={{ mt: 1, mb: 1 }}>
          {instructions}
        </FormHelperText>
      )}

      <FormGroup
        role="radiogroup"
        aria-labelledby={groupId}
        aria-describedby={describedByIds}
        aria-required={required}
        aria-invalid={error}
        row={row}
        sx={{ mt: 1 }}
      >
        {options.map((option, index) => (
          <FormControlLabel
            key={option.value}
            control={
              <Radio
                value={option.value}
                checked={value === option.value}
                onChange={handleChange}
                disabled={option.disabled}
                inputProps={{
                  'aria-describedby': option.description ? `${option.value}-desc` : undefined,
                }}
                sx={{
                  '&:focus': {
                    outline: '2px solid',
                    outlineColor: 'primary.main',
                    outlineOffset: '2px',
                  },
                }}
              />
            }
            label={
              <Box>
                {option.label}
                {option.description && (
                  <FormHelperText id={`${option.value}-desc`} sx={{ mt: 0, ml: 0 }}>
                    {option.description}
                  </FormHelperText>
                )}
              </Box>
            }
          />
        ))}
      </FormGroup>

      {/* Error message */}
      {error && errorMessage && (
        <FormHelperText id={errorId} error>
          {errorMessage}
        </FormHelperText>
      )}
    </FormControl>
  );
};

/**
 * Accessible Switch Component
 */
export interface AccessibleSwitchProps extends Omit<SwitchProps, 'inputProps'> {
  /**
   * Label for the switch
   */
  label: string;
  
  /**
   * Description or help text
   */
  description?: string;
  
  /**
   * Labels for on/off states
   */
  onLabel?: string;
  offLabel?: string;
}

export const AccessibleSwitch = forwardRef<HTMLButtonElement, AccessibleSwitchProps>(({
  label,
  description,
  onLabel = 'On',
  offLabel = 'Off',
  checked,
  ...props
}, ref) => {
  const { announcePolite } = useFormAnnouncement();
  const descriptionId = React.useId();

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const newChecked = event.target.checked;
    announcePolite(`${label} ${newChecked ? onLabel : offLabel}`);
    props.onChange?.(event, newChecked);
  };

  return (
    <Box>
      <FormControlLabel
        control={
          <Switch
            {...props}
            ref={ref}
            checked={checked}
            onChange={handleChange}
            inputProps={{
              'aria-describedby': description ? descriptionId : undefined,
              'aria-label': `${label}. Currently ${checked ? onLabel : offLabel}`,
            }}
            sx={{
              '&:focus-within': {
                outline: '2px solid',
                outlineColor: 'primary.main',
                outlineOffset: '2px',
                borderRadius: 1,
              },
            }}
          />
        }
        label={label}
      />

      {description && (
        <FormHelperText id={descriptionId} sx={{ ml: 4 }}>
          {description}
        </FormHelperText>
      )}
    </Box>
  );
});

AccessibleSwitch.displayName = 'AccessibleSwitch';

/**
 * Form Validation Summary Component
 */
export interface FormValidationSummaryProps {
  /**
   * List of validation errors
   */
  errors: Array<{ field: string; message: string; focusId?: string }>;
  
  /**
   * Title for the error summary
   */
  title?: string;
  
  /**
   * Auto-focus the summary when errors appear
   */
  autoFocus?: boolean;
}

export const FormValidationSummary: React.FC<FormValidationSummaryProps> = ({
  errors,
  title = 'Please correct the following errors:',
  autoFocus = true,
}) => {
  const summaryRef = React.useRef<HTMLDivElement>(null);
  const { announceAssertive } = useFormAnnouncement();

  React.useEffect(() => {
    if (errors.length > 0 && autoFocus) {
      summaryRef.current?.focus();
      announceAssertive(`${errors.length} validation ${errors.length === 1 ? 'error' : 'errors'} found.`);
    }
  }, [errors.length, autoFocus, announceAssertive]);

  if (errors.length === 0) return null;

  const handleErrorClick = (focusId?: string) => {
    if (focusId) {
      const element = document.getElementById(focusId);
      element?.focus();
    }
  };

  return (
    <Alert 
      severity="error" 
      ref={summaryRef}
      tabIndex={-1}
      role="alert"
      aria-live="assertive"
      sx={{ mb: 2 }}
    >
      <Box component="div">
        <Box component="h3" sx={{ margin: 0, mb: 1, fontSize: '1rem', fontWeight: 'bold' }}>
          {title}
        </Box>
        <Box component="ul" sx={{ margin: 0, pl: 2 }}>
          {errors.map((error, index) => (
            <Box component="li" key={`${error.field}-${index}`} sx={{ mb: 0.5 }}>
              {error.focusId ? (
                <Box
                  component="button"
                  onClick={() => handleErrorClick(error.focusId)}
                  sx={{
                    background: 'none',
                    border: 'none',
                    color: 'inherit',
                    textDecoration: 'underline',
                    cursor: 'pointer',
                    padding: 0,
                    font: 'inherit',
                    '&:hover': {
                      textDecoration: 'none',
                    },
                    '&:focus': {
                      outline: '2px solid currentColor',
                      outlineOffset: '2px',
                    },
                  }}
                >
                  {error.field}: {error.message}
                </Box>
              ) : (
                <span>{error.field}: {error.message}</span>
              )}
            </Box>
          ))}
        </Box>
      </Box>
    </Alert>
  );
};

export default AccessibleTextField;