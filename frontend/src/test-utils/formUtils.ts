import { screen } from '@testing-library/react';

// Form testing utilities
export const fillForm = async (formData: Record<string, string>) => {
  const { default: userEvent } = await import('@testing-library/user-event');
  const user = userEvent.setup();

  for (const [field, value] of Object.entries(formData)) {
    const input = screen.getByLabelText(new RegExp(field, 'i'));
    await user.clear(input);
    await user.type(input, value);
  }
};

export const submitForm = async () => {
  const { default: userEvent } = await import('@testing-library/user-event');
  const user = userEvent.setup();

  const submitButton = screen.getByRole('button', { name: /submit|login|register/i });
  await user.click(submitButton);
};

export const clickButton = async (buttonText: string | RegExp) => {
  const { default: userEvent } = await import('@testing-library/user-event');
  const user = userEvent.setup();
  
  const button = screen.getByRole('button', { name: buttonText });
  await user.click(button);
};

export const selectOption = async (label: string | RegExp, option: string) => {
  const { default: userEvent } = await import('@testing-library/user-event');
  const user = userEvent.setup();
  
  const select = screen.getByLabelText(label);
  await user.selectOptions(select, option);
};